package ai.kastrax.core.runtime

/**
 * kastrax-core模块的协程运行时自动初始化器
 */
class KastraxCoreRuntimeAutoInitializer {
    init {
        // 如果配置为自动初始化，则初始化kastrax-core模块的协程运行时
        if (KastraxCoreRuntimeConfig.isAutoInitialize()) {
            KastraxCoreRuntimeConfig.initialize()
        }
    }
    
    companion object {
        /**
         * 单例实例
         */
        private val INSTANCE = KastraxCoreRuntimeAutoInitializer()
        
        /**
         * 获取单例实例
         *
         * @return 单例实例
         */
        fun getInstance(): KastraxCoreRuntimeAutoInitializer {
            return INSTANCE
        }
    }
}
