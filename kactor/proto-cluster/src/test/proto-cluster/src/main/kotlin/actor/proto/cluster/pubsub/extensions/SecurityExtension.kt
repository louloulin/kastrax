package actor.proto.cluster.pubsub.extensions

import actor.proto.PID
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * SecurityExtension 是一个实现安全控制的扩展
 * 它可以限制哪些 PID 可以发布和订阅特定主题
 */
class SecurityExtension : PubSubExtension {
    private val publisherRoles = ConcurrentHashMap<String, MutableSet<String>>()
    private val subscriberRoles = ConcurrentHashMap<String, MutableSet<String>>()
    private val pidRoles = ConcurrentHashMap<String, MutableSet<String>>()
    
    /**
     * 为主题添加发布者角色
     * @param topic 主题
     * @param role 角色
     */
    fun addPublisherRole(topic: String, role: String) {
        publisherRoles.computeIfAbsent(topic) { ConcurrentHashMap.newKeySet() }.add(role)
        logger.info { "Added publisher role '$role' for topic '$topic'" }
    }
    
    /**
     * 为主题添加订阅者角色
     * @param topic 主题
     * @param role 角色
     */
    fun addSubscriberRole(topic: String, role: String) {
        subscriberRoles.computeIfAbsent(topic) { ConcurrentHashMap.newKeySet() }.add(role)
        logger.info { "Added subscriber role '$role' for topic '$topic'" }
    }
    
    /**
     * 为 PID 添加角色
     * @param pid PID
     * @param role 角色
     */
    fun addPidRole(pid: PID, role: String) {
        pidRoles.computeIfAbsent(pid.id) { ConcurrentHashMap.newKeySet() }.add(role)
        logger.info { "Added role '$role' for PID '${pid.id}'" }
    }
    
    /**
     * 移除主题的发布者角色
     * @param topic 主题
     * @param role 角色
     */
    fun removePublisherRole(topic: String, role: String) {
        publisherRoles[topic]?.remove(role)
        logger.info { "Removed publisher role '$role' for topic '$topic'" }
    }
    
    /**
     * 移除主题的订阅者角色
     * @param topic 主题
     * @param role 角色
     */
    fun removeSubscriberRole(topic: String, role: String) {
        subscriberRoles[topic]?.remove(role)
        logger.info { "Removed subscriber role '$role' for topic '$topic'" }
    }
    
    /**
     * 移除 PID 的角色
     * @param pid PID
     * @param role 角色
     */
    fun removePidRole(pid: PID, role: String) {
        pidRoles[pid.id]?.remove(role)
        logger.info { "Removed role '$role' for PID '${pid.id}'" }
    }
    
    /**
     * 检查 PID 是否有发布者角色
     * @param topic 主题
     * @param pid PID
     * @return 是否有发布者角色
     */
    private fun hasPublisherRole(topic: String, pid: PID): Boolean {
        val requiredRoles = publisherRoles[topic] ?: return true // 如果没有设置角色，则允许所有人发布
        val pidRoles = pidRoles[pid.id] ?: return false
        return pidRoles.any { it in requiredRoles }
    }
    
    /**
     * 检查 PID 是否有订阅者角色
     * @param topic 主题
     * @param pid PID
     * @return 是否有订阅者角色
     */
    private fun hasSubscriberRole(topic: String, pid: PID): Boolean {
        val requiredRoles = subscriberRoles[topic] ?: return true // 如果没有设置角色，则允许所有人订阅
        val pidRoles = pidRoles[pid.id] ?: return false
        return pidRoles.any { it in requiredRoles }
    }
    
    override fun beforePublish(topic: String, message: Any): Boolean {
        // 在实际应用中，我们需要知道发布者的 PID
        // 这里简化处理，假设消息中包含发布者信息
        // 在实际应用中，可以通过上下文或其他方式获取发布者 PID
        return true
    }
    
    override fun beforeSubscribe(topic: String, subscriber: PID): Boolean {
        val allowed = hasSubscriberRole(topic, subscriber)
        if (!allowed) {
            logger.warn { "Subscription denied for PID '${subscriber.id}' to topic '$topic': insufficient permissions" }
        }
        return allowed
    }
}
