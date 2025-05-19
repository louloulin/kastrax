package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor

/**
 * 测试调度器实现
 */
class TestDispatcher(private val dispatcher: kotlinx.coroutines.test.TestDispatcher) : KastraxDispatcher {
    override suspend fun <T> withContext(block: suspend () -> T): T {
        return withContext(dispatcher) { block() }
    }

    override fun <T> dispatchContext(continuation: Continuation<T>): Continuation<T> {
        // 在测试环境中，直接返回原始的continuation
        return continuation
    }
}
