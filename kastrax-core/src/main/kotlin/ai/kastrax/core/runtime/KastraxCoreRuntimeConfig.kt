package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * kastrax-core模块的协程运行时配置
 */
object KastraxCoreRuntimeConfig {
    /**
     * 是否自动初始化
     */
    private var autoInitialize = true
    
    /**
     * 获取是否自动初始化
     *
     * @return 是否自动初始化
     */
    fun isAutoInitialize(): Boolean {
        return autoInitialize
    }
    
    /**
     * 设置是否自动初始化
     *
     * @param value 是否自动初始化
     */
    fun setAutoInitialize(value: Boolean) {
        autoInitialize = value
    }
    
    /**
     * 初始化kastrax-core模块的协程运行时
     *
     * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
     */
    fun initialize(runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()) {
        KastraxCoreRuntimeInitializer.initialize(runtime)
    }
    
    /**
     * 重置kastrax-core模块的协程运行时
     */
    fun reset() {
        KastraxCoreRuntimeInitializer.reset()
    }
}
