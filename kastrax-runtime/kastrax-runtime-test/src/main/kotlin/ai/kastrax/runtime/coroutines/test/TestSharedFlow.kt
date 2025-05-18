package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 测试共享流实现
 */
class TestSharedFlow<T>(private val flow: kotlinx.coroutines.flow.MutableSharedFlow<T>) : KastraxSharedFlow<T> {
    override suspend fun collect(collector: suspend (T) -> Unit) {
        flow.collect(collector)
    }

    override fun <R> map(transform: suspend (T) -> R): KastraxFlow<R> {
        return TestFlow(flow.map(transform))
    }

    override fun filter(predicate: suspend (T) -> Boolean): KastraxFlow<T> {
        return TestFlow(flow.filter(predicate))
    }

    override fun catch(action: suspend (Throwable) -> Unit): KastraxFlow<T> {
        return TestFlow(flow.catch { action(it) })
    }

    override suspend fun emit(value: T) {
        flow.emit(value)
    }

    override fun tryEmit(value: T): Boolean {
        return flow.tryEmit(value)
    }
}
