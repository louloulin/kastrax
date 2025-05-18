package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.KastraxDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * IntelliJ IDEA调度器实现
 */
class IdeaDispatcher(private val dispatcher: CoroutineDispatcher) : KastraxDispatcher {
    override suspend fun <T> withContext(block: suspend () -> T): T {
        return withContext(dispatcher) { block() }
    }
}
