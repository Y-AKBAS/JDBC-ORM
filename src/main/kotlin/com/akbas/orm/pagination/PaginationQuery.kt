package com.akbas.orm.pagination


data class PaginationQuery<K: Comparable<K>?>(
    val key: K?,
    val limit: Int,
    val offset: Int,
    val components: PaginationQueryComponents<K>
) {
    lateinit var sql: String
}
