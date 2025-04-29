package actor.proto.metrics

/**
 * 度量接口，所有度量类型的基础接口
 */
interface Metric {
    /**
     * 获取度量的名称
     */
    val name: String
    
    /**
     * 获取度量的标签
     */
    val tags: Map<String, String>
}
