package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.test.*

/**
 * 测试协程作用域实现
 */
class TestCoroutineScope(private val scope: TestScope) : KastraxCoroutineScope {
    override fun launch(block: suspend () -> Unit): KastraxJob {
        return TestJob(scope.launch { block() })
    }
    
    override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        return TestDeferred(scope.async { block() })
    }
    
    override fun cancel() {
        scope.cancel()
    }
    
    override fun isActive(): Boolean {
        return scope.isActive
    }
}
