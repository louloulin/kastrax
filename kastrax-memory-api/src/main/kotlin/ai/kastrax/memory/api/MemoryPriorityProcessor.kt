package ai.kastrax.memory.api

/**
 * 记忆优先级处理器接口，用于处理记忆优先级。
 */
interface MemoryPriorityProcessor {
    /**
     * 计算消息的优先级。
     *
     * @param message 消息
     * @param config 优先级配置
     * @return 计算的优先级
     */
    suspend fun calculatePriority(message: Message, config: MemoryPriorityConfig): MemoryPriority
    
    /**
     * 更新消息的优先级。
     *
     * @param messageId 消息ID
     * @param priority 新的优先级
     * @return 是否更新成功
     */
    suspend fun updatePriority(messageId: String, priority: MemoryPriority): Boolean
    
    /**
     * 获取消息的优先级。
     *
     * @param messageId 消息ID
     * @return 消息的优先级
     */
    suspend fun getPriority(messageId: String): MemoryPriority?
    
    /**
     * 应用优先级衰减。
     *
     * @param config 优先级配置
     * @return 处理的消息数量
     */
    suspend fun applyPriorityDecay(config: MemoryPriorityConfig): Int
    
    /**
     * 清理过期的低优先级消息。
     *
     * @param config 优先级配置
     * @return 清理的消息数量
     */
    suspend fun cleanupLowPriorityMessages(config: MemoryPriorityConfig): Int
}
