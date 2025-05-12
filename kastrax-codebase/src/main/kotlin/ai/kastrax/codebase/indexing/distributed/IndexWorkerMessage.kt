package ai.kastrax.codebase.indexing.distributed

import ai.kastrax.codebase.indexing.IndexTask

/**
 * 索引工作器消息
 */
sealed class IndexWorkerMessage {
    /**
     * 处理任务消息
     *
     * @property task 索引任务
     */
    data class ProcessTask(val task: IndexTask) : IndexWorkerMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : IndexWorkerMessage()

    /**
     * 状态响应消息
     *
     * @property activeTaskCount 活动任务数量
     * @property availableSlots 可用槽位数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     */
    data class StatusResponse(
        val activeTaskCount: Int,
        val availableSlots: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int
    ) : IndexWorkerMessage()

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
    ) : IndexWorkerMessage()
}
