package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor

/**
 * JVM调度器实现
 */
class JvmDispatcher(private val dispatcher: CoroutineDispatcher) : KastraxDispatcher {
    override suspend fun <T> withContext(block: suspend () -> T): T {
        return withContext(dispatcher) { block() }
    }

    override fun <T> dispatchContext(continuation: Continuation<T>): Continuation<T> {
        // 在JVM环境中，使用调度器的interceptContinuation方法
        return dispatcher.interceptContinuation(continuation)
    }
}
