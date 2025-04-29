package actor.proto.metrics

/**
 * 仪表接口，用于测量某个值
 */
interface Gauge : Metric {
    /**
     * 设置仪表的值
     * @param value 要设置的值
     */
    fun set(value: Double)
}
