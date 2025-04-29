package actor.proto.priority

import actor.proto.Props
import actor.proto.mailbox.priority.newPriorityMailboxProducer

/**
 * 使用优先级邮箱
 * @param userMailboxSize 用户邮箱的大小，0 表示无限制
 * @param initialCapacity 优先级队列的初始容量
 * @return 使用优先级邮箱的 Props
 */
fun Props.withPriorityMailbox(
    userMailboxSize: Int = 0,
    initialCapacity: Int = 11
): Props {
    return withMailbox(newPriorityMailboxProducer(userMailboxSize, initialCapacity))
}
