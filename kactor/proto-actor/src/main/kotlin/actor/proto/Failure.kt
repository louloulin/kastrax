package actor.proto

import actor.proto.mailbox.SystemMessage

/**
 * Failure 消息在子 Actor 抛出异常时发送给父 Actor
 * @param who 失败的 Actor 的 PID
 * @param reason 失败原因
 * @param restartStatistics 重启统计信息
 * @param message 失败时处理的消息
 */
data class Failure(
    val who: PID,
    val reason: Any,
    val restartStatistics: RestartStatistics,
    val message: Any? = null
) : SystemMessage
