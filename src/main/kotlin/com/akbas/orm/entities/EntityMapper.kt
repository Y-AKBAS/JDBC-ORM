package com.akbas.orm.entities

import com.akbas.orm.support.ConstructorParameter
import com.akbas.orm.support.SqlParamSourceImpl
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


/**
 * An entity mapper which operates based on the public primary constructor value parameters.
 *
 * <P>While mapping columns to the entity class properties, all the standard sql types and
 * enums are supported. However, user defined types are not supported out of the box.
 * To achieve this or to override the behavior of mapping from a resultSet for a certain type,
 * an extractor can be added to {@see com.yakbas.mapper.ResultSetExtractorProvider}
 *
 * <P>Besides that, with the help of this class a SqlParameterSource can be generated to be used
 * in the methods of NamedParameterJdbcTemplate, JdbcClient and JdbcBatchWriter etc.
 *
 * @author Yasin Akbas
 */

class EntityMapper<T : Any>(
    private val entityClass: KClass<T>,
    private val params: List<ConstructorParameter>
) : RowMapper<T> {

    private val constructor = requireNotNull(entityClass.primaryConstructor) { "${entityClass.simpleName} has no primary constructor!" }

    override fun mapRow(rs: ResultSet, rowNum: Int): T {
        check(params.size == rs.metaData.columnCount) { "Inconsistent ResultSet and Entity. Prop size: ${params.size}, Column count: ${rs.metaData.columnCount}" }
        return extract(rs)
    }

    private fun extract(rs: ResultSet): T {
        val result = Array(params.size) {
            val param = params[it]
            param.resultSetExtractor(rs, param.snakeCaseName)
        }

        return constructor.call(*result)
    }

    fun createSqlParameterSource(item: T): SqlParameterSource {
        return SqlParamSourceImpl.of(constructorParams = params, item = item)
    }

}
