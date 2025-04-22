package ai.kastrax.memory.api

/**
 * 内存处理器接口，用于处理和转换内存消息。
 */
interface MemoryProcessor {
    /**
     * 处理消息列表。
     *
     * @param messages 要处理的消息列表
     * @param options 处理选项
     * @return 处理后的消息列表
     */
    suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions = MemoryProcessorOptions()
    ): List<MemoryMessage>
}

/**
 * 内存处理器选项。
 *
 * @property systemMessage 系统消息
 * @property memorySystemMessage 内存系统消息
 * @property newMessages 新消息
 */
data class MemoryProcessorOptions(
    val systemMessage: Message? = null,
    val memorySystemMessage: Message? = null,
    val newMessages: List<Message> = emptyList()
)

/**
 * 令牌限制器，用于限制消息的总令牌数。
 *
 * @property maxTokens 最大令牌数
 */
class TokenLimiter(
    private val maxTokens: Int
) : MemoryProcessor {
    // 每条消息的令牌开销
    private val tokensPerMessage = 3
    // 每个工具调用的令牌开销
    private val tokensPerTool = 2
    // 每个对话的固定令牌开销
    private val tokensPerConversation = 25
    
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        // 计算系统消息的令牌数
        var totalTokens = tokensPerConversation
        
        if (options.systemMessage != null) {
            totalTokens += countTokens(options.systemMessage.content)
            totalTokens += tokensPerMessage
        }
        
        if (options.memorySystemMessage != null) {
            totalTokens += countTokens(options.memorySystemMessage.content)
            totalTokens += tokensPerMessage
        }
        
        // 从最新的消息开始，直到达到令牌限制
        val result = mutableListOf<MemoryMessage>()
        
        // 按时间倒序排序
        val sortedMessages = messages.sortedByDescending { it.createdAt }
        
        for (message in sortedMessages) {
            val messageTokens = countTokens(message.message.content)
            val messageOverhead = tokensPerMessage + 
                (message.message.toolCalls.size * tokensPerTool)
            
            if (totalTokens + messageTokens + messageOverhead <= maxTokens) {
                result.add(message)
                totalTokens += messageTokens + messageOverhead
            } else {
                // 如果这条消息会超出限制，就停止添加
                break
            }
        }
        
        // 按时间正序排序返回
        return result.sortedBy { it.createdAt }
    }
    
    /**
     * 计算文本的令牌数。
     * 这是一个简化的实现，实际应用中应该使用更准确的分词器。
     */
    private fun countTokens(text: String): Int {
        // 简单估算：每个单词约1.3个令牌
        return (text.split(Regex("\\s+")).size * 1.3).toInt()
    }
}

/**
 * 工具调用过滤器，用于过滤掉工具调用和结果。
 *
 * @property exclude 要排除的工具名称列表，如果为null则排除所有工具调用
 */
class ToolCallFilter(
    private val exclude: List<String>? = null
) : MemoryProcessor {
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        return messages.filter { memoryMessage ->
            val message = memoryMessage.message
            
            // 如果排除所有工具调用
            if (exclude == null) {
                message.toolCalls.isEmpty() && message.toolCallId == null
            } else {
                // 如果只排除特定工具
                message.toolCalls.none { it.name in exclude } && 
                    (message.toolCallId == null || 
                     !messages.any { 
                         it.message.toolCalls.any { 
                             call -> call.id == message.toolCallId && call.name in exclude 
                         }
                     })
            }
        }
    }
}

/**
 * 摘要生成器，用于生成长对话的摘要。
 */
class SummaryGenerator : MemoryProcessor {
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        // 这里应该实现摘要生成逻辑
        // 在实际应用中，可能需要使用LLM来生成摘要
        return messages
    }
}
