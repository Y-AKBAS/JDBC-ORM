package com.akbas.orm.support

import java.math.BigDecimal
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object ResultSetExtractorProvider {

    private val extractorCache = ConcurrentHashMap<KClass<*>, (rs: ResultSet, columnName: String) -> Any?>()

    fun <T : Any> getForType(clazz: KClass<T>): (rs: ResultSet, columnName: String) -> T? {
        @Suppress("UNCHECKED_CAST")
        return extractorCache.getOrPut(clazz) { doGet(clazz) } as (rs: ResultSet, columnName: String) -> T?
    }

    fun <T : Any> addForType(clazz: KClass<T>, extractor: (rs: ResultSet, columnName: String) -> T?) {
        extractorCache[clazz] = extractor
    }

    private fun <T : Any> doGet(clazz: KClass<T>): (rs: ResultSet, columnName: String) -> Any? {
        if (clazz == String::class) {
            return { rs, columnName -> rs.getString(columnName) }
        } else if (clazz == Boolean::class) {
            return { rs, columnName -> rs.getBoolean(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == Byte::class) {
            return { rs, columnName -> rs.getByte(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == Short::class) {
            return { rs, columnName -> rs.getShort(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == Int::class) {
            return { rs, columnName -> rs.getInt(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == Long::class) {
            return { rs, columnName -> rs.getLong(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == Float::class) {
            return { rs, columnName -> rs.getFloat(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == Double::class || clazz == Number::class) {
            return { rs, columnName -> rs.getDouble(columnName).let { if (rs.wasNull()) null else it } }
        } else if (clazz == BigDecimal::class) {
            return { rs, columnName -> rs.getBigDecimal(columnName) }
        } else if (clazz == Date::class) {
            return { rs, columnName -> rs.getDate(columnName) }
        } else if (clazz == Time::class) {
            return { rs, columnName -> rs.getTime(columnName) }
        } else if (clazz == Timestamp::class || clazz == java.util.Date::class) {
            return { rs, columnName -> rs.getTimestamp(columnName) }
        } else if (clazz == ByteArray::class) {
            return { rs, columnName -> rs.getBytes(columnName) }
        } else if (clazz == Blob::class) {
            return { rs, columnName -> rs.getBlob(columnName) }
        } else if (clazz == Clob::class) {
            return { rs, columnName -> rs.getClob(columnName) }
        } else if (clazz == java.time.LocalDate::class) {
            return { rs, columnName -> rs.getDate(columnName)?.toLocalDate() }
        } else if (clazz == java.time.LocalTime::class) {
            return { rs, columnName -> rs.getTime(columnName)?.toLocalTime() }
        } else if (clazz == java.time.LocalDateTime::class) {
            return { rs, columnName -> rs.getTimestamp(columnName)?.toLocalDateTime() }
        } else if (clazz.java.isEnum) {
            val enumConstantsArray = clazz.java.enumConstants
            val enumConstants: Set<Enum<*>> = enumConstantsArray.mapTo(HashSet(enumConstantsArray.size)) { it as Enum<*> }
            return { rs, columnName ->
                val obj: Any? = rs.getObject(columnName)
                if (obj == null) {
                    null
                } else if (obj is String) {
                    enumConstants.find { it.name == obj }
                } else if (obj is Number) {
                    val ordinal = obj.toInt()
                    enumConstants.find { it.ordinal == ordinal }
                } else {
                    val str: String? = rs.getString(columnName)
                    if (str == null) null else enumConstants.find { it.name == str }
                }
            }
        } else {
            return { rs, columnName -> rs.getObject(columnName, clazz.java) }
        }
    }
}
