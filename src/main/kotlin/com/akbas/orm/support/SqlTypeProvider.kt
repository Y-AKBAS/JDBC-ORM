package com.akbas.orm.support

import org.springframework.jdbc.core.StatementCreatorUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object SqlTypeProvider {

    private val extractorCache = ConcurrentHashMap<KClass<*>, Int>()

    fun <T : Any> getForType(clazz: KClass<T>): Int {
        return extractorCache.getOrPut(clazz) {
            val javaType = clazz.java
            val targetType = if (javaType.isEnum) String::class.java else javaType
            StatementCreatorUtils.javaTypeToSqlParameterType(targetType)
        }
    }

    fun <T : Any> addForType(clazz: KClass<T>, type: Int) {
        extractorCache[clazz] = type
    }
}
