package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxDeferred
import kotlinx.coroutines.Deferred

/**
 * JVM延迟结果实现
 */
class JvmDeferred<T>(private val deferred: Deferred<T>) : KastraxDeferred<T>, KastraxJob by JvmJob(deferred) {
    override suspend fun await(): T {
        return deferred.await()
    }
}
