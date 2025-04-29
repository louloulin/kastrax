package actor.proto

/**
 * 从生产者函数创建Props
 * @param producer 生产者函数
 * @return Props实例
 */
fun fromProducer(producer: () -> Actor): Props {
    val props = Props()
    props.producer = producer
    return props
}
