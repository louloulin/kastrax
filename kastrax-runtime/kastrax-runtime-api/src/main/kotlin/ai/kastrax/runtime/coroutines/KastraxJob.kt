package ai.kastrax.runtime.coroutines

/**
 * kastrax协程作业抽象
 */
interface KastraxJob {
    /**
     * 取消作业
     */
    fun cancel()
    
    /**
     * 等待作业完成
     */
    suspend fun join()
    
    /**
     * 检查作业是否活跃
     */
    fun isActive(): Boolean
}
