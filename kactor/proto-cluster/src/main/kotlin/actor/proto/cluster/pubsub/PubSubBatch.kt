package actor.proto.cluster.pubsub

import actor.proto.PID
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * PubSubBatch 表示一批要发布到主题的消息
 * @param topic 主题名称
 * @param messages 要发布的消息列表
 */
data class PubSubBatch(
    val topic: String,
    val messages: MutableList<Any> = mutableListOf()
) {
    /**
     * 添加消息到批处理中
     * @param message 要添加的消息
     */
    fun add(message: Any) {
        messages.add(message)
    }
    
    /**
     * 添加多个消息到批处理中
     * @param messages 要添加的消息列表
     */
    fun addAll(messages: Collection<Any>) {
        this.messages.addAll(messages)
    }
    
    /**
     * 获取批处理中的消息数量
     * @return 消息数量
     */
    fun size(): Int {
        return messages.size
    }
    
    /**
     * 检查批处理是否为空
     * @return 如果批处理为空则返回 true，否则返回 false
     */
    fun isEmpty(): Boolean {
        return messages.isEmpty()
    }
    
    /**
     * 清空批处理中的所有消息
     */
    fun clear() {
        messages.clear()
    }
}

/**
 * PubSubBatchResponse 表示批处理发布的响应
 * @param success 是否成功
 * @param errorMessage 错误消息，如果有的话
 */
data class PubSubBatchResponse(
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * BatchPublishRequest 表示批处理发布请求
 * @param batch 要发布的批处理
 * @param sender 发送者 PID，用于响应
 */
data class BatchPublishRequest(
    val batch: PubSubBatch,
    val sender: PID?
)
