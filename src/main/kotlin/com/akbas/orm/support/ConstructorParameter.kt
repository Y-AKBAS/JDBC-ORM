package com.akbas.orm.support

import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class ConstructorParameter(
    val name: String,
    val snakeCaseName: String,
    val type: KClass<*>,
    val property: KProperty<*>,
    val isEnum: Boolean = false,
    val enumConstants: Set<Enum<*>>? = null,
    val sqlType: Int,
    val resultSetExtractor: (rs: ResultSet, columnName: String) -> Any?
) {
    init {
        if (isEnum && enumConstants.isNullOrEmpty()) throw IllegalArgumentException("isEnum is true but enum constants are empty or null!")
    }
}

