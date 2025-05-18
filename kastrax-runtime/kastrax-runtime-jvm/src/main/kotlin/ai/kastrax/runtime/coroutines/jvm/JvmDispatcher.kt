package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * JVM调度器实现
 */
class JvmDispatcher(private val dispatcher: CoroutineDispatcher) : KastraxDispatcher {
    override suspend fun <T> withContext(block: suspend () -> T): T {
        return withContext(dispatcher) { block() }
    }
}
