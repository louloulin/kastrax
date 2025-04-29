package actor.proto.metrics

/**
 * 计数器接口，用于计数事件发生的次数
 */
interface Counter : Metric {
    /**
     * 增加计数器的值
     * @param value 增加的值
     */
    fun inc(value: Long = 1)
}
