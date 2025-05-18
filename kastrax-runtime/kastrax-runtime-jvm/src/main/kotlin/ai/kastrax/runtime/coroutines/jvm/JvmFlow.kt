package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * JVM流实现
 */
class JvmFlow<T>(private val flow: Flow<T>) : KastraxFlow<T> {
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
}
