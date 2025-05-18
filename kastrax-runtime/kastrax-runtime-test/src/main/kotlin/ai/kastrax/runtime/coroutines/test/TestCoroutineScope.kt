package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.coroutines.CoroutineContext

/**
 * 测试协程作用域实现
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TestCoroutineScope(private val scope: kotlinx.coroutines.test.TestScope) : KastraxCoroutineScope {
    override fun launch(block: suspend () -> Unit): KastraxJob {
        // 使用TestScope的协程上下文创建Job
        val job = Job(scope.coroutineContext[Job])
        // 在测试调度器上启动协程
        CoroutineScope(scope.coroutineContext + job).launch {
            block()
        }
        return TestJob(job)
    }

    override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        // 使用TestScope的协程上下文创建Deferred
        val deferred = CompletableDeferred<T>(scope.coroutineContext[Job])
        // 在测试调度器上启动协程
        CoroutineScope(scope.coroutineContext).launch {
            try {
                deferred.complete(block())
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
            }
        }
        return TestDeferred(deferred)
    }

    override fun cancel() {
        scope.cancel()
    }

    override fun isActive(): Boolean {
        return scope.coroutineContext.isActive
    }
}
