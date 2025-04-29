package actor.proto.cluster.pubsub.extensions

import actor.proto.PID
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * FilterExtension 是一个过滤 PubSub 消息的扩展
 * 它可以根据主题和消息内容过滤发布和订阅操作
 */
class FilterExtension : PubSubExtension {
    private val topicFilters = mutableMapOf<String, (Any) -> Boolean>()
    private val subscriberFilters = mutableMapOf<String, (PID) -> Boolean>()
    
    /**
     * 添加主题消息过滤器
     * @param topic 主题
     * @param filter 过滤器函数，返回 true 表示允许发布，false 表示拦截
     */
    fun addTopicFilter(topic: String, filter: (Any) -> Boolean) {
        topicFilters[topic] = filter
        logger.info { "Added filter for topic '$topic'" }
    }
    
    /**
     * 移除主题消息过滤器
     * @param topic 主题
     */
    fun removeTopicFilter(topic: String) {
        topicFilters.remove(topic)
        logger.info { "Removed filter for topic '$topic'" }
    }
    
    /**
     * 添加订阅者过滤器
     * @param topic 主题
     * @param filter 过滤器函数，返回 true 表示允许订阅，false 表示拦截
     */
    fun addSubscriberFilter(topic: String, filter: (PID) -> Boolean) {
        subscriberFilters[topic] = filter
        logger.info { "Added subscriber filter for topic '$topic'" }
    }
    
    /**
     * 移除订阅者过滤器
     * @param topic 主题
     */
    fun removeSubscriberFilter(topic: String) {
        subscriberFilters.remove(topic)
        logger.info { "Removed subscriber filter for topic '$topic'" }
    }
    
    override fun beforePublish(topic: String, message: Any): Boolean {
        val filter = topicFilters[topic] ?: return true
        val allowed = filter(message)
        if (!allowed) {
            logger.debug { "Message filtered for topic '$topic': $message" }
        }
        return allowed
    }
    
    override fun beforeSubscribe(topic: String, subscriber: PID): Boolean {
        val filter = subscriberFilters[topic] ?: return true
        val allowed = filter(subscriber)
        if (!allowed) {
            logger.debug { "Subscriber filtered for topic '$topic': $subscriber" }
        }
        return allowed
    }
}
