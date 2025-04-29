package ai.kastrax.actor.network.protocols

/**
 * 协作结果
 *
 * 表示协作任务的执行结果
 *
 * @property success 是否成功
 * @property result 结果内容
 * @property participants 参与者列表
 * @property steps 协作步骤
 */
data class CollaborationResult(
    val success: Boolean,
    val result: String,
    val participants: List<String>,
    val steps: List<CollaborationStep>
)
