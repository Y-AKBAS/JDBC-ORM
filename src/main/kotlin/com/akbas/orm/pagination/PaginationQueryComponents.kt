package com.akbas.orm.pagination

import org.springframework.jdbc.core.namedparam.SqlParameterSource

data class PaginationQueryComponents<T: Comparable<T>?>(
    val sortKey: SortingKey<T>,
    val whereClause: String? = null,
    val groupByClause: String? = null,
    val namedParams: SqlParameterSource? = null
)
