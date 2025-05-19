package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * JVM协程作用域实现
 */
class JvmCoroutineScope(private val scope: CoroutineScope) : KastraxCoroutineScope {
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
        return JvmJob(scope.launch { block() })
    }

    /**
     * 启动协程，带异常处理
     *
     * @param block 要执行的代码块
     * @param onError 异常处理器
     * @return 协程作业
     */
    override fun launchSafe(block: suspend () -> Unit, onError: (Throwable) -> Unit): KastraxJob {
        val handler = CoroutineExceptionHandler { _, throwable -> onError(throwable) }
        return JvmJob(scope.launch(handler) { block() })
    }

    /**
     * 异步执行并返回结果
     *
     * @param block 要执行的代码块
     * @return 延迟结果
     */
    override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        return JvmDeferred(scope.async { block() })
    }

    /**
     * 异步执行并返回结果，带异常处理
     *
     * @param block 要执行的代码块
     * @param onError 异常处理器
     * @return 延迟结果
     */
    override fun <T> asyncSafe(block: suspend () -> T, onError: (Throwable) -> T): KastraxDeferred<T> {
        return JvmDeferred(scope.async {
            try {
                block()
            } catch (e: Throwable) {
                onError(e)
            }
        })
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
        return scope.isActive
    }
}
