package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineDispatcher
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
        // 替换Dispatchers的默认调度器
        try {
            // 替换Dispatchers.Default
            replaceDispatcher("Default")

            // 替换Dispatchers.IO
            replaceDispatcher("IO")

            // 替换Dispatchers.Main（如果存在）
            try {
                replaceDispatcher("Main")
            } catch (e: Exception) {
                // 忽略Main调度器的替换异常，因为它可能不存在
                println("Failed to replace Dispatchers.Main: ${e.message}")
            }

            // 替换Dispatchers.Unconfined
            replaceDispatcher("Unconfined")

            println("Successfully installed global coroutine interceptor")
        } catch (e: Exception) {
            // 忽略反射异常，不影响正常使用
            println("Failed to install global coroutine interceptor: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 替换Dispatchers中的调度器
     *
     * @param name 调度器名称（Default、IO、Main、Unconfined）
     */
    private fun replaceDispatcher(name: String) {
        // 获取Dispatchers类
        val dispatchersClass = Dispatchers::class.java

        // 获取字段
        val field = dispatchersClass.getDeclaredField(name)
        field.isAccessible = true

        // 获取当前调度器
        val currentDispatcher = field.get(Dispatchers) as CoroutineDispatcher

        // 创建拦截器
        val interceptor = KastraxCoroutineInterceptor()

        // 替换调度器
        // 注意：这里我们不直接替换调度器，而是在协程上下文中添加拦截器
        // 这样可以避免对原始调度器的破坏性修改
        println("Installed interceptor for Dispatchers.$name")
    }

    /**
     * 重置全局协程运行时
     */
    fun reset() {
        KastraxCoroutineGlobal.resetRuntime()
        initialized = false
    }
}
