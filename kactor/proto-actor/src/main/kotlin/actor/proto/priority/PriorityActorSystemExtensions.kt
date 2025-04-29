package actor.proto.priority

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.mailbox.priority.PriorityMessage
import actor.proto.mailbox.priority.MessagePriorities
import actor.proto.mailbox.priority.withPriority
import actor.proto.mailbox.priority.withHighPriority
import actor.proto.mailbox.priority.withMediumPriority
import actor.proto.mailbox.priority.withLowPriority

/**
 * 向 PID 发送优先级消息
 * @param pid 目标 PID
 * @param message 消息
 * @param priority 消息优先级
 */
fun ActorSystem.sendWithPriority(pid: PID, message: Any, priority: Int) {
    send(pid, message.withPriority(priority))
}

/**
 * 向 PID 发送高优先级消息
 * @param pid 目标 PID
 * @param message 消息
 */
fun ActorSystem.sendWithHighPriority(pid: PID, message: Any) {
    send(pid, message.withHighPriority())
}

/**
 * 向 PID 发送中优先级消息
 * @param pid 目标 PID
 * @param message 消息
 */
fun ActorSystem.sendWithMediumPriority(pid: PID, message: Any) {
    send(pid, message.withMediumPriority())
}

/**
 * 向 PID 发送低优先级消息
 * @param pid 目标 PID
 * @param message 消息
 */
fun ActorSystem.sendWithLowPriority(pid: PID, message: Any) {
    send(pid, message.withLowPriority())
}
