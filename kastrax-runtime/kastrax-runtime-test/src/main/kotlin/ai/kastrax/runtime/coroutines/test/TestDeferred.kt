package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxDeferred
import kotlinx.coroutines.Deferred

/**
 * 测试延迟结果实现
 */
class TestDeferred<T>(private val deferred: kotlinx.coroutines.Deferred<T>) : KastraxDeferred<T> {
    override fun cancel() {
        deferred.cancel()
    }

    override suspend fun join() {
        deferred.join()
    }

    override fun isActive(): Boolean {
        return deferred.isActive
    }

    override suspend fun await(): T {
        return deferred.await()
    }
}
