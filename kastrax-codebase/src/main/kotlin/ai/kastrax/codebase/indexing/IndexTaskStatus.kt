package ai.kastrax.codebase.indexing

/**
 * 索引任务状态
 */
enum class IndexTaskStatus {
    /**
     * 等待中
     */
    PENDING,
    
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELED,
    
    /**
     * 超时
     */
    TIMEOUT
}
