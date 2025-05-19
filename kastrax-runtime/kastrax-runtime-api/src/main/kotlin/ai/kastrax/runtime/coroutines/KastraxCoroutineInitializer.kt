package ai.kastrax.runtime.coroutines

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
     * 初始化全局协程运行时
     *
     * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
     */
    fun initialize(runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()) {
        if (initialized) {
            return
        }
        
        // 设置全局协程运行时
        KastraxCoroutineGlobal.setRuntime(runtime)
        
        // 安装全局协程拦截器
        installGlobalInterceptor()
        
        initialized = true
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
