package ai.kastrax.memory.api

import kotlinx.datetime.Instant

/**
 * 内存系统接口，用于存储和检索对话历史。
 */
interface Memory {
    /**
     * 保存消息到指定的线程。
     *
     * @param message 要保存的消息
     * @param threadId 线程ID
     * @return 保存的消息ID
     */
    suspend fun saveMessage(message: Message, threadId: String): String

    /**
     * 获取指定线程的消息。
     *
     * @param threadId 线程ID
     * @param limit 返回的最大消息数量
     * @param processors 要应用的处理器列表
     * @return 消息列表
     */
    suspend fun getMessages(threadId: String, limit: Int = 10, processors: List<MemoryProcessor>? = null): List<MemoryMessage>

    /**
     * 搜索指定线程中与查询相关的消息。
     *
     * @param query 搜索查询
     * @param threadId 线程ID
     * @param limit 返回的最大消息数量
     * @return 相关消息列表
     */
    suspend fun searchMessages(query: String, threadId: String, limit: Int = 5): List<MemoryMessage>

    /**
     * 语义搜索指定线程中与查询相关的消息。
     *
     * @param query 搜索查询
     * @param threadId 线程ID
     * @param config 语义召回配置
     * @return 相关消息列表
     */
    suspend fun semanticSearch(query: String, threadId: String, config: SemanticRecallConfig = SemanticRecallConfig()): List<SemanticSearchResult>

    /**
     * 创建新的线程。
     *
     * @param title 线程标题（可选）
     * @return 新线程的ID
     */
    suspend fun createThread(title: String? = null): String

    /**
     * 删除指定的线程。
     *
     * @param threadId 要删除的线程ID
     * @return 是否成功删除
     */
    suspend fun deleteThread(threadId: String): Boolean

    /**
     * 获取线程信息。
     *
     * @param threadId 线程ID
     * @return 线程信息
     */
    suspend fun getThread(threadId: String): MemoryThread?

    /**
     * 列出所有线程。
     *
     * @param limit 返回的最大线程数量
     * @param offset 分页偏移量
     * @return 线程列表
     */
    suspend fun listThreads(limit: Int = 20, offset: Int = 0): List<MemoryThread>
}

/**
 * 消息角色枚举。
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * 消息接口。
 */
interface Message {
    /**
     * 消息角色。
     */
    val role: MessageRole

    /**
     * 消息内容。
     */
    val content: String

    /**
     * 消息发送者的名称（可选）。
     */
    val name: String?

    /**
     * 消息中的工具调用（可选）。
     */
    val toolCalls: List<ToolCall>

    /**
     * 如果这是工具响应，则为工具调用ID（可选）。
     */
    val toolCallId: String?
}

/**
 * 工具调用接口。
 */
interface ToolCall {
    /**
     * 工具调用ID。
     */
    val id: String

    /**
     * 工具名称。
     */
    val name: String

    /**
     * 工具参数（JSON字符串）。
     */
    val arguments: String
}

/**
 * 内存中存储的消息。
 *
 * @property id 消息ID
 * @property threadId 线程ID
 * @property message 消息
 * @property createdAt 创建时间
 */
data class MemoryMessage(
    val id: String,
    val threadId: String,
    val message: Message,
    val createdAt: Instant
)

/**
 * 内存中的线程信息。
 *
 * @property id 线程ID
 * @property title 线程标题
 * @property createdAt 创建时间
 * @property updatedAt 最后更新时间
 * @property messageCount 消息数量
 */
data class MemoryThread(
    val id: String,
    val title: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messageCount: Int = 0
)

/**
 * 内存构建器接口，用于创建内存实例。
 */
interface MemoryBuilder {
    /**
     * 设置存储实现。
     */
    fun storage(storage: Any): MemoryBuilder

    /**
     * 设置最近消息数量。
     */
    fun lastMessages(count: Int): MemoryBuilder

    /**
     * 启用或禁用语义召回。
     */
    fun semanticRecall(enabled: Boolean): MemoryBuilder

    /**
     * 设置嵌入生成器。
     */
    fun embeddingGenerator(generator: EmbeddingGenerator): MemoryBuilder

    /**
     * 设置向量存储。
     */
    fun vectorStorage(storage: VectorStorage): MemoryBuilder

    /**
     * 添加内存处理器。
     */
    fun processor(processor: MemoryProcessor): MemoryBuilder

    /**
     * 启用或禁用工作内存。
     */
    fun workingMemory(config: WorkingMemoryConfig): MemoryBuilder

    /**
     * 构建内存实例。
     */
    fun build(): Memory
}
