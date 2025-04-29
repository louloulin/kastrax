package actor.proto.cluster.pubsub.extensions

import actor.proto.PID
import actor.proto.cluster.PubSubMessage
import actor.proto.cluster.Topic
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * PubSubExtension 是 PubSub 扩展的基本接口
 * 它允许在发布和订阅操作前后执行自定义逻辑
 */
interface PubSubExtension {
    /**
     * 在发布消息前执行
     * @param topic 主题
     * @param message 消息
     * @return 是否继续发布消息
     */
    fun beforePublish(topic: String, message: Any): Boolean = true
    
    /**
     * 在发布消息后执行
     * @param topic 主题
     * @param message 消息
     */
    fun afterPublish(topic: String, message: Any) {}
    
    /**
     * 在订阅主题前执行
     * @param topic 主题
     * @param subscriber 订阅者
     * @return 是否继续订阅
     */
    fun beforeSubscribe(topic: String, subscriber: PID): Boolean = true
    
    /**
     * 在订阅主题后执行
     * @param topic 主题
     * @param subscriber 订阅者
     */
    fun afterSubscribe(topic: String, subscriber: PID) {}
    
    /**
     * 在取消订阅主题前执行
     * @param topic 主题
     * @param subscriber 订阅者
     * @return 是否继续取消订阅
     */
    fun beforeUnsubscribe(topic: String, subscriber: PID): Boolean = true
    
    /**
     * 在取消订阅主题后执行
     * @param topic 主题
     * @param subscriber 订阅者
     */
    fun afterUnsubscribe(topic: String, subscriber: PID) {}
}

/**
 * PubSubExtensionRegistry 是 PubSub 扩展的注册表
 * 它管理所有注册的扩展，并在发布和订阅操作时调用它们
 */
class PubSubExtensionRegistry {
    private val extensions = mutableListOf<PubSubExtension>()
    
    /**
     * 注册扩展
     * @param extension 要注册的扩展
     */
    fun register(extension: PubSubExtension) {
        extensions.add(extension)
        logger.info { "Registered PubSub extension: ${extension.javaClass.simpleName}" }
    }
    
    /**
     * 注销扩展
     * @param extension 要注销的扩展
     */
    fun unregister(extension: PubSubExtension) {
        extensions.remove(extension)
        logger.info { "Unregistered PubSub extension: ${extension.javaClass.simpleName}" }
    }
    
    /**
     * 在发布消息前执行所有扩展
     * @param topic 主题
     * @param message 消息
     * @return 是否继续发布消息
     */
    fun beforePublish(topic: String, message: Any): Boolean {
        return extensions.all { it.beforePublish(topic, message) }
    }
    
    /**
     * 在发布消息后执行所有扩展
     * @param topic 主题
     * @param message 消息
     */
    fun afterPublish(topic: String, message: Any) {
        extensions.forEach { it.afterPublish(topic, message) }
    }
    
    /**
     * 在订阅主题前执行所有扩展
     * @param topic 主题
     * @param subscriber 订阅者
     * @return 是否继续订阅
     */
    fun beforeSubscribe(topic: String, subscriber: PID): Boolean {
        return extensions.all { it.beforeSubscribe(topic, subscriber) }
    }
    
    /**
     * 在订阅主题后执行所有扩展
     * @param topic 主题
     * @param subscriber 订阅者
     */
    fun afterSubscribe(topic: String, subscriber: PID) {
        extensions.forEach { it.afterSubscribe(topic, subscriber) }
    }
    
    /**
     * 在取消订阅主题前执行所有扩展
     * @param topic 主题
     * @param subscriber 订阅者
     * @return 是否继续取消订阅
     */
    fun beforeUnsubscribe(topic: String, subscriber: PID): Boolean {
        return extensions.all { it.beforeUnsubscribe(topic, subscriber) }
    }
    
    /**
     * 在取消订阅主题后执行所有扩展
     * @param topic 主题
     * @param subscriber 订阅者
     */
    fun afterUnsubscribe(topic: String, subscriber: PID) {
        extensions.forEach { it.afterUnsubscribe(topic, subscriber) }
    }
}
