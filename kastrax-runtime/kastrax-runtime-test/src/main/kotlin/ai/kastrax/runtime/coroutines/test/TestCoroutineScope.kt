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
    /**
     * 协程上下文
     */
    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    /**
     * 启动协程
     *
     * @param block 要执行的代码块
     * @return 协程作业
     */
    override fun launch(block: suspend () -> Unit): KastraxJob {
        // 使用TestScope的协程上下文创建Job
        val job = Job(scope.coroutineContext[Job])
        // 在测试调度器上启动协程
        CoroutineScope(scope.coroutineContext + job).launch {
            block()
        }
        return TestJob(job)
    }

    /**
     * 启动协程，带异常处理
     *
     * @param block 要执行的代码块
     * @param onError 异常处理器
     * @return 协程作业
     */
    override fun launchSafe(block: suspend () -> Unit, onError: (Throwable) -> Unit): KastraxJob {
        // 使用TestScope的协程上下文创建Job
        val job = Job(scope.coroutineContext[Job])
        // 在测试调度器上启动协程
        CoroutineScope(scope.coroutineContext + job).launch {
            try {
                block()
            } catch (e: Throwable) {
                onError(e)
            }
        }
        return TestJob(job)
    }

    /**
     * 异步执行并返回结果
     *
     * @param block 要执行的代码块
     * @return 延迟结果
     */
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

    /**
     * 异步执行并返回结果，带异常处理
     *
     * @param block 要执行的代码块
     * @param onError 异常处理器
     * @return 延迟结果
     */
    override fun <T> asyncSafe(block: suspend () -> T, onError: (Throwable) -> T): KastraxDeferred<T> {
        // 使用TestScope的协程上下文创建Deferred
        val deferred = CompletableDeferred<T>(scope.coroutineContext[Job])
        // 在测试调度器上启动协程
        CoroutineScope(scope.coroutineContext).launch {
            try {
                deferred.complete(block())
            } catch (e: Throwable) {
                try {
                    deferred.complete(onError(e))
                } catch (e2: Throwable) {
                    deferred.completeExceptionally(e2)
                }
            }
        }
        return TestDeferred(deferred)
    }

    /**
     * 取消作用域中的所有协程
     */
    override fun cancel() {
        scope.cancel()
    }

    /**
     * 检查作用域是否活跃
     *
     * @return 作用域是否活跃
     */
    override fun isActive(): Boolean {
        return scope.coroutineContext.isActive
    }
}
