package ai.kastrax.codebase.indexing.distributed

import actor.proto.PID
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskStatus

/**
 * 索引协调器消息
 */
sealed class IndexCoordinatorMessage {
    /**
     * 提交任务消息
     *
     * @property task 索引任务
     */
    data class SubmitTask(val task: IndexTask) : IndexCoordinatorMessage()

    /**
     * 批量提交任务消息
     *
     * @property tasks 索引任务列表
     */
    data class SubmitTasks(val tasks: List<IndexTask>) : IndexCoordinatorMessage()

    /**
     * 任务状态更新消息
     *
     * @property taskId 任务ID
     * @property status 任务状态
     * @property error 错误信息
     */
    data class TaskStatusUpdate(val taskId: String, val status: IndexTaskStatus, val error: String? = null) : IndexCoordinatorMessage()

    /**
     * 工作器注册消息
     *
     * @property workerId 工作器ID
     * @property capacity 工作器容量
     * @property pid 工作器PID
     */
    data class RegisterWorker(val workerId: String, val capacity: Int, val pid: PID) : IndexCoordinatorMessage()

    /**
     * 工作器注销消息
     *
     * @property workerId 工作器ID
     */
    data class UnregisterWorker(val workerId: String) : IndexCoordinatorMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : IndexCoordinatorMessage()

    /**
     * 状态响应消息
     *
     * @property pendingTaskCount 待处理任务数量
     * @property runningTaskCount 运行中任务数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     * @property workerCount 工作器数量
     * @property totalCapacity 总容量
     * @property availableCapacity 可用容量
     */
    data class StatusResponse(
        val pendingTaskCount: Int,
        val runningTaskCount: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int,
        val workerCount: Int,
        val totalCapacity: Int,
        val availableCapacity: Int
    ) : IndexCoordinatorMessage()

    /**
     * 工作器心跳消息
     *
     * @property workerId 工作器ID
     * @property activeTaskCount 活动任务数量
     * @property availableSlots 可用槽位数量
     */
    data class WorkerHeartbeat(
        val workerId: String,
        val activeTaskCount: Int,
        val availableSlots: Int
    ) : IndexCoordinatorMessage()
}
