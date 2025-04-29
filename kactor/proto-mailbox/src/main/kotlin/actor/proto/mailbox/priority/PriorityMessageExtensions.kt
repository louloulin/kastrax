package actor.proto.mailbox.priority

/**
 * 消息优先级常量
 */
object MessagePriorities {
    /**
     * 高优先级
     */
    const val HIGH = 0

    /**
     * 中优先级
     */
    const val MEDIUM = 1

    /**
     * 低优先级
     */
    const val LOW = 2

    /**
     * 默认优先级
     */
    const val DEFAULT = MEDIUM
}

/**
 * 将消息包装为优先级消息
 * @param priority 消息优先级
 * @return 包装后的优先级消息
 */
fun Any.withPriority(priority: Int): PriorityMessage {
    return PriorityMessage(this, priority)
}

/**
 * 将消息包装为高优先级消息
 * @return 包装后的高优先级消息
 */
fun Any.withHighPriority(): PriorityMessage {
    return withPriority(MessagePriorities.HIGH)
}

/**
 * 将消息包装为中优先级消息
 * @return 包装后的中优先级消息
 */
fun Any.withMediumPriority(): PriorityMessage {
    return withPriority(MessagePriorities.MEDIUM)
}

/**
 * 将消息包装为低优先级消息
 * @return 包装后的低优先级消息
 */
fun Any.withLowPriority(): PriorityMessage {
    return withPriority(MessagePriorities.LOW)
}


