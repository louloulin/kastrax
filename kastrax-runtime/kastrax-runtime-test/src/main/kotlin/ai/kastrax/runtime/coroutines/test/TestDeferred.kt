package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxDeferred
import kotlinx.coroutines.Deferred

/**
 * 测试延迟结果实现
 */
class TestDeferred<T>(private val deferred: Deferred<T>) : KastraxDeferred<T>, KastraxJob by TestJob(deferred) {
    override suspend fun await(): T {
        return deferred.await()
    }
}
