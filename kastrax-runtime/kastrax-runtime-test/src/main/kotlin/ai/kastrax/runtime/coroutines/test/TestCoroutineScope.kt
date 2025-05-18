package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.test.*

/**
 * 测试协程作用域实现
 */
class TestCoroutineScope(private val scope: kotlinx.coroutines.test.TestScope) : KastraxCoroutineScope {
    override fun launch(block: suspend () -> Unit): KastraxJob {
        val job = kotlinx.coroutines.launch(scope.coroutineContext) { block() }
        return TestJob(job)
    }

    override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        val deferred = kotlinx.coroutines.async(scope.coroutineContext) { block() }
        return TestDeferred(deferred)
    }

    override fun cancel() {
        scope.cancel()
    }

    override fun isActive(): Boolean {
        return scope.coroutineContext.isActive
    }
}
