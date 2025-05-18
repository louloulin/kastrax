package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 测试调度器实现
 */
class TestDispatcher(private val dispatcher: CoroutineDispatcher) : KastraxDispatcher {
    override suspend fun <T> withContext(block: suspend () -> T): T {
        return withContext(dispatcher) { block() }
    }
}
