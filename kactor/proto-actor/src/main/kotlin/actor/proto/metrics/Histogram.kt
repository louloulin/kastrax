package actor.proto.metrics

/**
 * 直方图接口，用于测量值的分布
 */
interface Histogram : Metric {
    /**
     * 记录一个观察值
     * @param value 观察到的值
     */
    fun observe(value: Double)
}
