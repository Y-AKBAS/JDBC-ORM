package com.akbas.orm.support

import com.akbas.orm.entities.EntityMapper
import com.akbas.orm.entities.EntitySqlGenerator
import com.akbas.orm.entities.EntitySqlParamGenerator
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

class JdbcMappingManager<T : Any> private constructor(
    val idParam: ConstructorParameter,
    val sqlGenerator: EntitySqlGenerator<T>,
    val paramGenerator: EntitySqlParamGenerator<T>,
    val mapper: EntityMapper<T>
) {

    companion object {

        private val managerCache = ConcurrentHashMap<KClass<*>, JdbcMappingManager<*>>()

        fun <T : Any> of(entityOrProjectionClass: KClass<T>): JdbcMappingManager<T> {
            @Suppress("UNCHECKED_CAST")
            return managerCache.getOrPut(entityOrProjectionClass) { createMappingManager(entityOrProjectionClass) } as JdbcMappingManager<T>
        }

        private fun <T : Any> createMappingManager(entityOrProjectionClass: KClass<T>): JdbcMappingManager<T> {
            val tableAnno = entityOrProjectionClass.findAnnotation<Table>()
            val projectionAnno = entityOrProjectionClass.findAnnotation<Projection>()
            require(tableAnno != null || projectionAnno != null) { "${entityOrProjectionClass.simpleName} is neither an an entity nor a projection class! No @Table or @Projection annotation found!" }
            require(tableAnno == null || projectionAnno == null) { "${entityOrProjectionClass.simpleName} has both @Table and @Projection annotations!" }
            return if (tableAnno != null) createForEntity(tableAnno, entityOrProjectionClass) else createForProjection(projectionAnno!!, entityOrProjectionClass)
        }

        private fun <T : Any> createForEntity(tableAnno: Table, entityClass: KClass<T>): JdbcMappingManager<T> {
            val tableName = tableAnno.value.ifEmpty { tableAnno.name }
            require(tableName.isNotBlank()) { "Found no table name for ${entityClass.simpleName}" }
            val idProperty = entityClass.memberProperties.find { prop ->
                prop.javaField!!.annotations.find { it is Id } != null
            }
            requireNotNull(idProperty) { "Found no @Id annotated property for ${entityClass.simpleName}" }
            val constructorParameters = createConstructorParameters(entityClass)
            val idParam = constructorParameters.first { it.property == idProperty }
            val entityMapper = EntityMapper(entityClass, constructorParameters)
            val sqlGenerator = EntitySqlGenerator<T>(tableName, idParam, constructorParameters)
            val sqlParamGenerator = EntitySqlParamGenerator<T>(constructorParameters, idParam)
            return JdbcMappingManager(idParam, sqlGenerator, sqlParamGenerator, entityMapper)
        }

        private fun <T : Any> createForProjection(projectionAnno: Projection, projectionClass: KClass<T>): JdbcMappingManager<T> {
            val entityClass = projectionAnno.ofClass
            val tableAnno = entityClass.findAnnotation<Table>()
            requireNotNull(tableAnno) { "${entityClass.simpleName} has no @Table annotation!" }
            val tableName = tableAnno.value.ifEmpty { tableAnno.name }
            require(tableName.isNotBlank()) { "Found no table name for ${entityClass.simpleName}" }
            val idProperty = entityClass.memberProperties.find { prop ->
                prop.javaField!!.annotations.find { it is Id } != null
            }
            requireNotNull(idProperty) { "Found no @Id annotated property for ${entityClass.simpleName}" }
            val entityProperties = entityClass.memberProperties
            val projectionProperties = projectionClass.memberProperties
            projectionProperties.forEach { proj -> requireNotNull(entityProperties.find { it.name == proj.name }) { "${proj.name} property is not found in ${entityClass.simpleName}. Is this really a projection?" } }

            val projectionIdProperty = projectionProperties.find { it.name == idProperty.name }
            requireNotNull(projectionIdProperty) { "@Id property of ${entityClass.simpleName} is not found in ${projectionClass.simpleName}!" }
            val constructorParameters = createConstructorParameters(projectionClass)
            val idParam = constructorParameters.first { it.property == projectionIdProperty }
            val entityMapper = EntityMapper(projectionClass, constructorParameters)
            val sqlGenerator = EntitySqlGenerator<T>(tableName, idParam, constructorParameters)
            val sqlParamGenerator = EntitySqlParamGenerator<T>(constructorParameters, idParam)
            return JdbcMappingManager(idParam, sqlGenerator, sqlParamGenerator, entityMapper)
        }

        private fun <T : Any> createConstructorParameters(entityClass: KClass<T>): List<ConstructorParameter> {
            val constructor = requireNotNull(entityClass.primaryConstructor) { "${entityClass.simpleName} has no primary constructor!" }
            val valueParams = constructor.valueParameters
            val memberProperties = entityClass.memberProperties
            require(valueParams.size == memberProperties.size) { "There are member properties which are not in the primary constructor!" }
            return valueParams.mapTo(ArrayList()) { param ->
                val name = param.name!!
                val type = param.type.jvmErasure
                val isEnum = type.java.isEnum
                val enumConstants = if (isEnum) type.java.enumConstants.let { enums -> enums.mapTo(HashSet(enums.size)) { it as Enum<*> } } else null
                val property = memberProperties.first { it.name == name }
                ConstructorParameter(
                    name = name,
                    snakeCaseName = toSnakeCase(name),
                    type = type,
                    property = property,
                    isEnum = isEnum,
                    enumConstants = enumConstants,
                    sqlType = SqlTypeProvider.getForType(type),
                    resultSetExtractor = ResultSetExtractorProvider.getForType(type)
                )
            }
        }

        fun toSnakeCase(str: String) = buildString {
            for (c in str) {
                if (c.isUpperCase()) {
                    append('_')
                    append(c.lowercaseChar())
                } else {
                    append(c)
                }
            }
        }

    }
}
