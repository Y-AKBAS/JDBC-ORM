package com.akbas.orm.stream

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.data.util.CloseableIterator
import java.util.stream.Stream

/**
 * This method creates a cold flow which emits the elements
 * in chunks in an asynchronous and reactive manner.
 * Should be preferred over chunked Sequence<List<T>>
 * if there are backpressure problems etc.
 *
 * Resource management like closing of Stream etc.
 * is handled internally.
 *
 * @param chunkSize should be greater than 1
 * @return the cold flow which emits the chunks
 * @author yakbas
 */

fun <T : Any> Stream<T>.chunked(chunkSize: Int, closeLog: () -> String = { "" }): Flow<List<T>> {
    require(chunkSize > 1) { "chunkSize should be greater than 1!" }
    val iterator = this.toClosableIterator(closeLog)
    return flow {
        iterator.use {
            while (iterator.hasNext()) {
                val chunk = ArrayList<T>(chunkSize)
                var counter = 0

                while (counter < chunkSize && iterator.hasNext()) {
                    chunk.add(iterator.next())
                    counter++
                }

                if (chunk.isNotEmpty()) {
                    emit(chunk)
                }
            }
        }
    }
}

fun <T> Stream<T>.toClosableIterator(closeLog: () -> String): CloseableIterator<T> = JdbcClosableIterator(this, closeLog)

private class JdbcClosableIterator<T>(private val stream: Stream<T>, closeLog: () -> String) : CloseableIterator<T> {

    private val iterator = stream.iterator()
    private val closeLogMessage = closeLog()

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): T {
        return iterator.next()
    }

    override fun close() {
        println(closeLogMessage.ifBlank { "Closing stream in jdbc closable iterator" })
        stream.close()
    }

    override fun remove() {
        iterator.remove()
    }
}
