package ai.kastrax.actor.network.protocols

/**
 * 协作步骤
 *
 * 表示协作过程中的一个步骤
 *
 * @property agentId 执行步骤的 Agent ID
 * @property input 输入内容
 * @property output 输出内容
 * @property timestamp 时间戳
 */
data class CollaborationStep(
    val agentId: String,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)
