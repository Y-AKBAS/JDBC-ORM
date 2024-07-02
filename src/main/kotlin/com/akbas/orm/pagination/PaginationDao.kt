package com.akbas.orm.pagination

import com.akbas.orm.entities.EntityDao
import com.akbas.orm.support.JdbcMappingManager
import com.akbas.orm.support.SqlTypeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.measureTimeMillis

class PaginationDao(val entityDao: EntityDao) {

    inline fun <reified T : Any, reified K> paginateInParallel(
        numOfPagesPerIteration: Int,
        pageSize: Int,
        queryComponents: PaginationQueryComponents<K>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        crossinline chunkConsumer: EntityDao.(queryResult: List<T>) -> Unit
    ) where K : Any, K : Comparable<K>? = runBlocking(dispatcher) {
        val millis = measureTimeMillis {
            requireGreaterThanZero("numOfPagesPerIteration", numOfPagesPerIteration)
            requireGreaterThanZero("pageSize", pageSize)
            val mappingManager = JdbcMappingManager.of(T::class)

            val keyProp = queryComponents.sortKey.property
            val sortOrder = queryComponents.sortKey.sortOrder
            val paramSource = queryComponents.namedParams

            var queries = createPages(mappingManager, null, numOfPagesPerIteration, 0, pageSize, queryComponents, true)
            var currentKey = Optional.empty<K>()
            var currentParams = Optional.ofNullable(paramSource)

            while (true) {
                val iterationResults = queries.map {
                    async(block = handleChunk<K, T>(it, currentParams, sortOrder, keyProp, chunkConsumer, currentKey))
                }.awaitAll()

                val (successResults, failureResults) = iterationResults.partition { !it.failed }

                handleFailures<K>(failureResults)

                if (successResults.isEmpty()) break

                val candidateKeys: List<K> = successResults.filter { it.maxOrMinKey.isPresent }.map { it.maxOrMinKey.get() }
                if (candidateKeys.isEmpty()) break
                val newKey = if (sortOrder == SortOrder.ASC) {
                    candidateKeys.maxBy { it }
                } else {
                    candidateKeys.minBy { it }
                }

                val newOffset = if (currentKey.isPresent && currentKey.get() == newKey) {
                    successResults.maxByOrNull { it.paginationQuery.offset }!!.paginationQuery.offset
                } else 0

                currentKey = Optional.of(newKey)
                queries = createPages(mappingManager, currentKey.get(), numOfPagesPerIteration, newOffset, pageSize, queryComponents, false)
                currentParams = Optional.of(createParams(paramSource, keyProp, currentKey.get()))
                println("CurrentKey: ${currentKey.get()}")
            }
        }
        println("Passed time: $millis")
    }

    inline fun <reified K, reified T : Any> handleChunk(
        it: PaginationQuery<K>,
        currentParams: Optional<SqlParameterSource>,
        sortOrder: SortOrder,
        keyProp: KProperty<K?>,
        crossinline chunkConsumer: EntityDao.(queryResult: List<T>) -> Unit,
        currentKey: Optional<K>
    ): suspend CoroutineScope.() -> PaginationResult<Any, K> where K : Any, K : Comparable<K>? = {
        try {
            val chunk = entityDao.findAll<T>(it.sql, namedParams = currentParams.orElse(null))
            val lastKey = if (chunk.isEmpty()) {
                Optional.empty<K>()
            } else if (sortOrder == SortOrder.ASC) {
                Optional.of(chunk.maxOf { keyProp.call(it)!! })
            } else {
                Optional.of(chunk.minOf { keyProp.call(it)!! })
            }
            chunkConsumer(entityDao, chunk)
            PaginationResult(lastKey, it, Optional.empty<Any>(), false, Optional.empty())
        } catch (e: Exception) {
            buildErrorMessage(T::class, currentKey, it.offset, it.sql)
            PaginationResult(Optional.empty<K>(), it, Optional.empty<Any?>(), true, Optional.of(e))
        }
    }

    inline fun <reified K> handleFailures(failureResults: List<PaginationResult<Any, K>>) where K : Any, K : Comparable<K>? {
        if (failureResults.isEmpty()) return

        val exceptions = failureResults
            .filter { it.exception.isPresent }
            .map { it.exception.get() }
        throw PaginationFailedException(causes = exceptions)
    }

    fun <K> createParams(sourceParams: SqlParameterSource?, keyProp: KProperty<K>, key: K): MapSqlParameterSource {
        val target = MapSqlParameterSource()
        target.addValue(JdbcMappingManager.toSnakeCase(keyProp.name), key, SqlTypeProvider.getForType(keyProp.returnType.jvmErasure))
        if (sourceParams == null) return target
        val paramNames = sourceParams.parameterNames
        requireNotNull(paramNames) { "Found no param names in provided SqlParameterSource!" }
        for (name in paramNames) {
            val value = sourceParams.getValue(name)
            val type = sourceParams.getSqlType(name)
            target.addValue(name, value, type)
        }
        return target
    }

    fun <T : Any, K : Comparable<K>?> createPages(
        manager: JdbcMappingManager<T>,
        sortKey: K?,
        numOfPagesPerIteration: Int,
        offset: Int,
        pageSize: Int,
        queryComponents: PaginationQueryComponents<K>,
        isInitial: Boolean
    ): List<PaginationQuery<K>> {
        val sqlGenerator = manager.sqlGenerator
        val queries = (0 until numOfPagesPerIteration).map {
            PaginationQuery(sortKey, pageSize, offset + pageSize * it, queryComponents)
        }.onEach {
            it.sql = sqlGenerator.paginationQuery(it, isInitial)
        }

        return queries
    }

    fun requireGreaterThanZero(paramName: String, num: Int) = require(num > 0) { "$paramName must be greater than zero!" }

    fun <T : Any, K : Any> buildErrorMessage(entityClass: KClass<T>, key: Optional<K>, offset: Int, sql: String): String {
        return "Failed at key: $key with offset: $offset for entity: ${entityClass.simpleName}. Sql: $sql"
    }
}
