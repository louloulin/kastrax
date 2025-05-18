package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.KastraxDeferred
import kotlinx.coroutines.Deferred

/**
 * IntelliJ IDEA延迟结果实现
 */
class IdeaDeferred<T>(private val deferred: Deferred<T>) : KastraxDeferred<T>, KastraxJob by IdeaJob(deferred) {
    override suspend fun await(): T {
        return deferred.await()
    }
}
