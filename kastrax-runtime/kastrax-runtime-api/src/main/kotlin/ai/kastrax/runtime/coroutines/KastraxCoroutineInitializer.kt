package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob

/**
 * 协程初始化器
 *
 * 初始化全局协程运行时
 */
object KastraxCoroutineInitializer {
    /**
     * 是否已初始化
     */
    private var initialized = false

    /**
     * 默认异常处理器
     */
    private val defaultExceptionHandler = { throwable: Throwable ->
        println("KastraxCoroutine uncaught exception: ${throwable.message}")
        throwable.printStackTrace()
    }

    /**
     * 初始化全局协程运行时
     *
     * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
     * @param exceptionHandler 全局异常处理器，默认为打印异常
     */
    fun initialize(
        runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime(),
        exceptionHandler: (Throwable) -> Unit = defaultExceptionHandler
    ) {
        if (initialized) {
            return
        }

        // 设置全局协程运行时
        KastraxCoroutineGlobal.setRuntime(runtime)

        // 安装全局协程异常处理器
        installGlobalExceptionHandler(exceptionHandler)

        // 安装全局协程拦截器
        installGlobalInterceptor()

        initialized = true
    }

    /**
     * 安装全局协程异常处理器
     *
     * @param handler 异常处理器
     */
    private fun installGlobalExceptionHandler(handler: (Throwable) -> Unit) {
        // 添加全局异常处理器
        KastraxCoroutineExceptionHandler.addHandler(handler)

        // 替换GlobalScope的异常处理器
        try {
            val context = GlobalScope.coroutineContext
            val newContext = context + KastraxCoroutineExceptionHandler.handler + SupervisorJob()
            val field = GlobalScope::class.java.getDeclaredField("coroutineContext")
            field.isAccessible = true
            field.set(GlobalScope, newContext)
        } catch (e: Exception) {
            // 忽略反射异常，不影响正常使用
            println("Failed to replace GlobalScope's exception handler: ${e.message}")
        }
    }

    /**
     * 安装全局协程拦截器
     */
    private fun installGlobalInterceptor() {
        // 这里可以添加全局协程拦截器的安装代码
        // 例如，可以使用反射修改Dispatchers的默认调度器
        // 或者使用其他方式拦截协程调用
    }

    /**
     * 重置全局协程运行时
     */
    fun reset() {
        KastraxCoroutineGlobal.resetRuntime()
        initialized = false
    }
}
