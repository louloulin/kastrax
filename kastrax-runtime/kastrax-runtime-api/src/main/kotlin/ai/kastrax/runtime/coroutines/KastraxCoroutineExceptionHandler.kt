package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * kastrax协程异常处理器
 *
 * 提供全局的协程异常处理
 */
object KastraxCoroutineExceptionHandler {
    /**
     * 异常处理器列表
     */
    private val handlers = mutableListOf<(Throwable) -> Unit>()

    /**
     * 添加异常处理器
     *
     * @param handler 异常处理器
     */
    fun addHandler(handler: (Throwable) -> Unit) {
        handlers.add(handler)
    }

    /**
     * 移除异常处理器
     *
     * @param handler 异常处理器
     */
    fun removeHandler(handler: (Throwable) -> Unit) {
        handlers.remove(handler)
    }

    /**
     * 处理异常
     *
     * @param throwable 异常
     */
    fun handleException(throwable: Throwable) {
        // 如果没有处理器，打印异常
        if (handlers.isEmpty()) {
            throwable.printStackTrace()
            return
        }

        // 调用所有处理器
        handlers.forEach { handler ->
            try {
                handler(throwable)
            } catch (e: Exception) {
                // 忽略处理器异常
                e.printStackTrace()
            }
        }
    }

    /**
     * 创建协程异常处理器
     *
     * @return 协程异常处理器
     */
    fun createCoroutineExceptionHandler(): CoroutineExceptionHandler {
        return object : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
            override fun handleException(context: CoroutineContext, exception: Throwable) {
                handleException(exception)
            }
        }
    }

    /**
     * 默认协程异常处理器
     */
    val handler = createCoroutineExceptionHandler()
}
