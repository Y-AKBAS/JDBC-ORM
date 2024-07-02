package com.akbas.orm.pagination

import com.akbas.orm.support.JdbcMappingManager
import kotlin.reflect.KProperty


data class SortingKey<T: Comparable<T>?>(val property: KProperty<T?>, val sortOrder: SortOrder) {
    val snakeCaseName = JdbcMappingManager.toSnakeCase(property.name)
}
