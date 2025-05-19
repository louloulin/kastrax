package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.KastraxCoroutineInitializer
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * kastrax-core模块的运行时初始化器
 */
object KastraxCoreRuntimeInitializer {
    /**
     * 是否已初始化
     */
    private var initialized = false
    
    /**
     * 初始化kastrax-core模块的运行时
     *
     * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
     */
    fun initialize(runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()) {
        if (initialized) {
            return
        }
        
        // 初始化全局协程运行时
        KastraxCoroutineInitializer.initialize(runtime)
        
        initialized = true
    }
    
    /**
     * 重置kastrax-core模块的运行时
     */
    fun reset() {
        KastraxCoroutineInitializer.reset()
        initialized = false
    }
}
