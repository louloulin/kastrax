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

/**
 * 系统状态信息
 *
 * @property status 系统状态
 * @property pendingTaskCount 待处理任务数量
 * @property runningTaskCount 运行中任务数量
 * @property completedTaskCount 已完成任务数量
 * @property failedTaskCount 失败任务数量
 * @property workerCount 工作器数量
 * @property isRunning 是否运行中
 */
data class SystemStatusInfo(
    val status: SystemStatus,
    val pendingTaskCount: Int = 0,
    val runningTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val failedTaskCount: Int = 0,
    val workerCount: Int = 0
) {
    /**
     * 是否运行中
     */
    val isRunning: Boolean
        get() = status == SystemStatus.RUNNING
}
