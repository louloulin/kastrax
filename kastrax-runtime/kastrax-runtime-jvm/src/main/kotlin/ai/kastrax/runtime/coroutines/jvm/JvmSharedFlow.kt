package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxFlow
import ai.kastrax.runtime.coroutines.KastraxSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * JVM共享流实现
 */
class JvmSharedFlow<T>(private val flow: MutableSharedFlow<T>) : KastraxSharedFlow<T> {
    override suspend fun collect(collector: suspend (T) -> Unit) {
        flow.collect(collector)
    }

    override fun <R> map(transform: suspend (T) -> R): KastraxFlow<R> {
        return JvmFlow(flow.map(transform))
    }

    override fun filter(predicate: suspend (T) -> Boolean): KastraxFlow<T> {
        return JvmFlow(flow.filter(predicate))
    }

    override fun catch(action: suspend (Throwable) -> Unit): KastraxFlow<T> {
        return JvmFlow(flow.catch { action(it) })
    }

    override suspend fun emit(value: T) {
        flow.emit(value)
    }

    override fun tryEmit(value: T): Boolean {
        return flow.tryEmit(value)
    }
}
