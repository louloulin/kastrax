package ai.kastrax.runtime.coroutines

/**
 * kastrax协程运行时工厂
 * 负责创建适合当前环境的协程运行时实现
 */
object KastraxCoroutineRuntimeFactory {
    private var runtime: KastraxCoroutineRuntime? = null
    
    /**
     * 获取当前环境的协程运行时
     * 如果尚未初始化，将自动检测环境并创建合适的实现
     */
    @Synchronized
    fun getRuntime(): KastraxCoroutineRuntime {
        if (runtime == null) {
            runtime = detectRuntime()
        }
        return runtime!!
    }
    
    /**
     * 显式设置协程运行时实现
     * 用于测试或特殊环境
     */
    @Synchronized
    fun setRuntime(customRuntime: KastraxCoroutineRuntime) {
        runtime = customRuntime
    }
    
    /**
     * 自动检测当前环境并创建合适的运行时实现
     */
    private fun detectRuntime(): KastraxCoroutineRuntime {
        return when {
            // 检测IntelliJ IDEA环境
            isIntelliJEnvironment() -> {
                try {
                    Class.forName("ai.kastrax.runtime.coroutines.idea.IdeaCoroutineRuntime")
                        .getDeclaredConstructor()
                        .newInstance() as KastraxCoroutineRuntime
                } catch (e: Exception) {
                    // 回退到JVM实现
                    createJvmRuntime()
                }
            }
            
            // 检测Android环境
            isAndroidEnvironment() -> {
                try {
                    Class.forName("ai.kastrax.runtime.coroutines.android.AndroidCoroutineRuntime")
                        .getDeclaredConstructor()
                        .newInstance() as KastraxCoroutineRuntime
                } catch (e: Exception) {
                    // 回退到JVM实现
                    createJvmRuntime()
                }
            }
            
            // 默认使用JVM实现
            else -> createJvmRuntime()
        }
    }
    
    /**
     * 创建JVM运行时实现
     */
    private fun createJvmRuntime(): KastraxCoroutineRuntime {
        return Class.forName("ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime")
            .getDeclaredConstructor()
            .newInstance() as KastraxCoroutineRuntime
    }
    
    /**
     * 检测是否在IntelliJ IDEA环境中运行
     */
    private fun isIntelliJEnvironment(): Boolean {
        return try {
            Class.forName("com.intellij.openapi.application.Application")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * 检测是否在Android环境中运行
     */
    private fun isAndroidEnvironment(): Boolean {
        return try {
            Class.forName("android.app.Activity")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
