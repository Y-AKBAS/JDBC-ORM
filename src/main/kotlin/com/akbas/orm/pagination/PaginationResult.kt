package com.akbas.orm.pagination

import java.util.Optional

class PaginationResult<R, K: Comparable<K>?>(
    val maxOrMinKey: Optional<K>,
    val paginationQuery: PaginationQuery<K>,
    val result: Optional<R>,
    val failed: Boolean,
    val exception: Optional<Throwable> = Optional.empty()
)
