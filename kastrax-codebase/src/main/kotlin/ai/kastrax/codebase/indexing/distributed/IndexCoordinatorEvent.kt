package ai.kastrax.codebase.indexing.distributed

/**
 * 索引协调者事件
 */
sealed class IndexCoordinatorEvent {
    /**
     * 任务分配事件
     *
     * @property taskId 任务 ID
     * @property workerId 工作者 ID
     */
    data class TaskAssigned(val taskId: String, val workerId: String) : IndexCoordinatorEvent()
    
    /**
     * 任务完成事件
     *
     * @property taskId 任务 ID
     * @property workerId 工作者 ID
     */
    data class TaskCompleted(val taskId: String, val workerId: String) : IndexCoordinatorEvent()
    
    /**
     * 任务失败事件
     *
     * @property taskId 任务 ID
     * @property workerId 工作者 ID
     * @property error 错误信息
     */
    data class TaskFailed(val taskId: String, val workerId: String, val error: String) : IndexCoordinatorEvent()
    
    /**
     * 工作者注册事件
     *
     * @property workerId 工作者 ID
     */
    data class WorkerRegistered(val workerId: String) : IndexCoordinatorEvent()
    
    /**
     * 工作者注销事件
     *
     * @property workerId 工作者 ID
     */
    data class WorkerUnregistered(val workerId: String) : IndexCoordinatorEvent()
}
