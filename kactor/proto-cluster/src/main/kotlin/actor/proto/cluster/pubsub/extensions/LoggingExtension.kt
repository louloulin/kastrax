package actor.proto.cluster.pubsub.extensions

import actor.proto.PID
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * LoggingExtension 是一个记录 PubSub 操作的扩展
 * 它在发布和订阅操作前后记录日志
 */
class LoggingExtension : PubSubExtension {
    override fun beforePublish(topic: String, message: Any): Boolean {
        logger.debug { "Publishing message to topic '$topic': $message" }
        return true
    }
    
    override fun afterPublish(topic: String, message: Any) {
        logger.debug { "Published message to topic '$topic': $message" }
    }
    
    override fun beforeSubscribe(topic: String, subscriber: PID): Boolean {
        logger.debug { "Subscribing to topic '$topic': $subscriber" }
        return true
    }
    
    override fun afterSubscribe(topic: String, subscriber: PID) {
        logger.debug { "Subscribed to topic '$topic': $subscriber" }
    }
    
    override fun beforeUnsubscribe(topic: String, subscriber: PID): Boolean {
        logger.debug { "Unsubscribing from topic '$topic': $subscriber" }
        return true
    }
    
    override fun afterUnsubscribe(topic: String, subscriber: PID) {
        logger.debug { "Unsubscribed from topic '$topic': $subscriber" }
    }
}
