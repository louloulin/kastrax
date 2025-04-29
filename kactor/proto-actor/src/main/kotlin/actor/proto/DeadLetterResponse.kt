package actor.proto

/**
 * 死信响应，当消息无法发送到目标 Actor 时返回
 * @param target 目标 PID
 */
data class DeadLetterResponse(val target: PID)
