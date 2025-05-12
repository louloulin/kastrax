package ai.kastrax.codebase.indexing.distributed

/**
 * 系统状态枚举
 */
enum class SystemStatus {
    /**
     * 初始化中
     */
    INITIALIZING,
    
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 停止中
     */
    STOPPING,
    
    /**
     * 已停止
     */
    STOPPED,
    
    /**
     * 错误
     */
    ERROR
}
