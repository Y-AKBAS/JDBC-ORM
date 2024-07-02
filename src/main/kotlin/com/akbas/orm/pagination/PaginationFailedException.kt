package com.akbas.orm.pagination

class PaginationFailedException(
    val causes: List<Throwable>
) : RuntimeException() {
    override val message: String = buildString {
        causes.forEach {
            append(it.message)
            append('\n')
        }
    }
    override val cause: Throwable? = causes.firstOrNull()
}
