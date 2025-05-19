package ai.kastrax.runtime.coroutines

import kotlin.coroutines.CoroutineContext

/**
 * kastrax协程作用域抽象
 */
interface KastraxCoroutineScope {
    /**
     * 协程上下文
     */
    val coroutineContext: CoroutineContext

    /**
     * 启动协程
     *
     * @param block 要执行的代码块
     * @return 协程作业
     */
    fun launch(block: suspend () -> Unit): KastraxJob

    /**
     * 启动协程，带异常处理
     *
     * @param block 要执行的代码块
     * @param onError 异常处理器
     * @return 协程作业
     */
    fun launchSafe(block: suspend () -> Unit, onError: (Throwable) -> Unit): KastraxJob

    /**
     * 异步执行并返回结果
     *
     * @param block 要执行的代码块
     * @return 延迟结果
     */
    fun <T> async(block: suspend () -> T): KastraxDeferred<T>

    /**
     * 异步执行并返回结果，带异常处理
     *
     * @param block 要执行的代码块
     * @param onError 异常处理器
     * @return 延迟结果
     */
    fun <T> asyncSafe(block: suspend () -> T, onError: (Throwable) -> T): KastraxDeferred<T>

    /**
     * 取消作用域中的所有协程
     */
    fun cancel()

    /**
     * 检查作用域是否活跃
     *
     * @return 作用域是否活跃
     */
    fun isActive(): Boolean
}
