package com.akbas.orm.support

import org.springframework.jdbc.core.SqlTypeValue
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private class EntityParamConstants<T>(
    val paramSqlTypes: Map<String, Int>,
    val paramNames: Array<String>
)

class SqlParamSourceImpl<T> private constructor(
    private val params: Map<String, Any?>,
    private val paramConstants: EntityParamConstants<T>,
    private val constructorParams: List<ConstructorParameter>
) : SqlParameterSource {

    companion object {

        private val sqlTypeCache = ConcurrentHashMap<KClass<*>, EntityParamConstants<*>>()

        fun <T : Any> of(constructorParams: List<ConstructorParameter>, item: T): SqlParamSourceImpl<T> {
            val paramMap = constructorParams.fold(HashMap<String, Any?>(constructorParams.size)) { map, param ->
                map[param.name] = param.property.call(item)?.let { if (param.isEnum) (it as Enum<*>).name else it }
                map
            }

            @Suppress("UNCHECKED_CAST")
            val entityParamConstants = getConstants(item::class, constructorParams) as EntityParamConstants<T>
            return SqlParamSourceImpl(paramMap, entityParamConstants, constructorParams)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> getConstants(type: KClass<T>, constructorParams: List<ConstructorParameter>): EntityParamConstants<T> {
            val paramConstants = sqlTypeCache.getOrPut(type) {
                val sqlTypes = constructorParams.fold(HashMap<String, Int>(constructorParams.size)) { map, param ->
                    map[param.name] = param.sqlType
                    map
                }
                val paramNames = constructorParams.map { it.name }.toTypedArray()
                EntityParamConstants<T>(sqlTypes, paramNames)
            } as EntityParamConstants<T>
            return paramConstants
        }
    }

    override fun hasValue(paramName: String): Boolean {
        return params.containsKey(paramName)
    }

    override fun getValue(paramName: String): Any? {
        return params.getValue(paramName)
    }

    override fun getSqlType(paramName: String): Int {
        return paramConstants.paramSqlTypes[paramName] ?: SqlTypeValue.TYPE_UNKNOWN
    }

    override fun getTypeName(paramName: String): String? {
        return constructorParams.find { it.name == paramName }?.type?.java?.name
    }

    override fun getParameterNames(): Array<String> {
        return paramConstants.paramNames
    }
}
