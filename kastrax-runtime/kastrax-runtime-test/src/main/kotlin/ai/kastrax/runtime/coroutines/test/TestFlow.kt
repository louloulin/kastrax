package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * 测试流实现
 */
class TestFlow<T>(private val flow: Flow<T>) : KastraxFlow<T> {
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
}
