package actor.proto.cluster.pubsub.extensions

import actor.proto.PID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * MetricsExtension 是一个收集 PubSub 操作指标的扩展
 * 它统计发布和订阅操作的次数
 */
class MetricsExtension : PubSubExtension {
    private val publishCount = AtomicLong(0)
    private val subscribeCount = AtomicLong(0)
    private val unsubscribeCount = AtomicLong(0)
    private val topicPublishCount = ConcurrentHashMap<String, AtomicLong>()
    private val topicSubscriberCount = ConcurrentHashMap<String, AtomicLong>()
    
    override fun afterPublish(topic: String, message: Any) {
        publishCount.incrementAndGet()
        topicPublishCount.computeIfAbsent(topic) { AtomicLong(0) }.incrementAndGet()
    }
    
    override fun afterSubscribe(topic: String, subscriber: PID) {
        subscribeCount.incrementAndGet()
        topicSubscriberCount.computeIfAbsent(topic) { AtomicLong(0) }.incrementAndGet()
    }
    
    override fun afterUnsubscribe(topic: String, subscriber: PID) {
        unsubscribeCount.incrementAndGet()
        topicSubscriberCount.computeIfAbsent(topic) { AtomicLong(0) }.decrementAndGet()
    }
    
    /**
     * 获取发布消息的总次数
     * @return 发布消息的总次数
     */
    fun getPublishCount(): Long {
        return publishCount.get()
    }
    
    /**
     * 获取订阅的总次数
     * @return 订阅的总次数
     */
    fun getSubscribeCount(): Long {
        return subscribeCount.get()
    }
    
    /**
     * 获取取消订阅的总次数
     * @return 取消订阅的总次数
     */
    fun getUnsubscribeCount(): Long {
        return unsubscribeCount.get()
    }
    
    /**
     * 获取特定主题的发布次数
     * @param topic 主题
     * @return 发布次数
     */
    fun getTopicPublishCount(topic: String): Long {
        return topicPublishCount[topic]?.get() ?: 0
    }
    
    /**
     * 获取特定主题的订阅者数量
     * @param topic 主题
     * @return 订阅者数量
     */
    fun getTopicSubscriberCount(topic: String): Long {
        return topicSubscriberCount[topic]?.get() ?: 0
    }
    
    /**
     * 获取所有主题的发布次数
     * @return 主题到发布次数的映射
     */
    fun getAllTopicPublishCounts(): Map<String, Long> {
        return topicPublishCount.mapValues { it.value.get() }
    }
    
    /**
     * 获取所有主题的订阅者数量
     * @return 主题到订阅者数量的映射
     */
    fun getAllTopicSubscriberCounts(): Map<String, Long> {
        return topicSubscriberCount.mapValues { it.value.get() }
    }
    
    /**
     * 重置所有指标
     */
    fun reset() {
        publishCount.set(0)
        subscribeCount.set(0)
        unsubscribeCount.set(0)
        topicPublishCount.clear()
        topicSubscriberCount.clear()
    }
}
