package ai.kastrax.memory.api

import kotlinx.serialization.Serializable

/**
 * 内存压缩配置。
 *
 * @property enabled 是否启用内存压缩
 * @property threshold 触发压缩的消息数阈值
 * @property targetSize 压缩后的目标消息数
 * @property preserveSystemMessages 是否保留系统消息
 * @property preserveRecentMessages 保留最近的消息数量
 */
@Serializable
data class MemoryCompressionConfig(
    val enabled: Boolean = false,
    val threshold: Int = 100,
    val targetSize: Int = 20,
    val preserveSystemMessages: Boolean = true,
    val preserveRecentMessages: Int = 10
)

/**
 * 内存压缩器接口，用于压缩和摘要长对话。
 */
interface MemoryCompressor {
    /**
     * 压缩消息列表。
     *
     * @param messages 要压缩的消息列表
     * @param config 压缩配置
     * @return 压缩后的消息列表
     */
    suspend fun compress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig = MemoryCompressionConfig()
    ): List<MemoryMessage>
    
    /**
     * 生成对话摘要。
     *
     * @param messages 要摘要的消息列表
     * @param maxLength 摘要的最大长度
     * @return 对话摘要
     */
    suspend fun summarize(
        messages: List<MemoryMessage>,
        maxLength: Int = 500
    ): String
    
    /**
     * 检查是否需要压缩。
     *
     * @param messages 消息列表
     * @param config 压缩配置
     * @return 是否需要压缩
     */
    fun shouldCompress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig = MemoryCompressionConfig()
    ): Boolean {
        return config.enabled && messages.size >= config.threshold
    }
}
