package ai.kastrax.memory.api

/**
 * 记忆管理器接口，提供高级的记忆查询和管理功能。
 */
interface MemoryManager {
    /**
     * 高级查询消息。
     *
     * @param threadId 线程ID
     * @param options 查询选项
     * @return 消息列表
     */
    suspend fun queryMessages(threadId: String, options: MemoryQueryOptions): List<MemoryMessage>
    
    /**
     * 获取消息上下文。
     *
     * @param selector 上下文选择器
     * @return 消息列表，包括中心消息及其上下文
     */
    suspend fun getMessageContext(selector: ContextSelector): List<MemoryMessage>
    
    /**
     * 获取多个消息的上下文。
     *
     * @param selectors 上下文选择器列表
     * @return 消息列表，包括所有中心消息及其上下文
     */
    suspend fun getMultipleMessageContexts(selectors: List<ContextSelector>): List<MemoryMessage>
    
    /**
     * 高级查询线程。
     *
     * @param options 查询选项
     * @return 线程列表
     */
    suspend fun queryThreads(options: ThreadQueryOptions): List<MemoryThread>
    
    /**
     * 批量更新消息。
     *
     * @param options 批量操作选项
     * @param updates 更新选项
     * @return 更新的消息数量
     */
    suspend fun batchUpdateMessages(options: MessageBatchOptions, updates: MessageUpdateOptions): Int
    
    /**
     * 批量删除消息。
     *
     * @param options 批量操作选项
     * @return 删除的消息数量
     */
    suspend fun batchDeleteMessages(options: MessageBatchOptions): Int
    
    /**
     * 获取消息统计信息。
     *
     * @param threadId 线程ID
     * @return 消息统计信息
     */
    suspend fun getMessageStats(threadId: String): MessageStats
    
    /**
     * 获取线程统计信息。
     *
     * @return 线程统计信息
     */
    suspend fun getThreadStats(): ThreadStats
    
    /**
     * 导出线程数据。
     *
     * @param threadId 线程ID
     * @param format 导出格式
     * @return 导出的数据
     */
    suspend fun exportThread(threadId: String, format: ExportFormat): String
    
    /**
     * 导入线程数据。
     *
     * @param data 要导入的数据
     * @param format 导入格式
     * @return 导入的线程ID
     */
    suspend fun importThread(data: String, format: ExportFormat): String
    
    /**
     * 合并线程。
     *
     * @param sourceThreadIds 源线程ID列表
     * @param targetThreadId 目标线程ID，如果为null则创建新线程
     * @param title 新线程的标题（如果创建新线程）
     * @return 目标线程ID
     */
    suspend fun mergeThreads(sourceThreadIds: List<String>, targetThreadId: String? = null, title: String? = null): String
    
    /**
     * 拆分线程。
     *
     * @param threadId 要拆分的线程ID
     * @param splitPoints 拆分点（消息ID列表）
     * @param newThreadTitles 新线程的标题列表
     * @return 新线程ID列表
     */
    suspend fun splitThread(threadId: String, splitPoints: List<String>, newThreadTitles: List<String>? = null): List<String>
}

/**
 * 消息统计信息。
 *
 * @property totalMessages 总消息数
 * @property messagesByRole 按角色分组的消息数
 * @property averageMessageLength 平均消息长度
 * @property oldestMessageTime 最早消息时间
 * @property newestMessageTime 最新消息时间
 * @property messagesByPriority 按优先级分组的消息数
 */
data class MessageStats(
    val totalMessages: Int,
    val messagesByRole: Map<MessageRole, Int>,
    val averageMessageLength: Double,
    val oldestMessageTime: kotlinx.datetime.Instant,
    val newestMessageTime: kotlinx.datetime.Instant,
    val messagesByPriority: Map<MemoryPriority, Int>
)

/**
 * 线程统计信息。
 *
 * @property totalThreads 总线程数
 * @property activeThreads 活跃线程数（最近7天有更新）
 * @property averageMessagesPerThread 每个线程的平均消息数
 * @property oldestThreadTime 最早线程创建时间
 * @property newestThreadTime 最新线程创建时间
 */
data class ThreadStats(
    val totalThreads: Int,
    val activeThreads: Int,
    val averageMessagesPerThread: Double,
    val oldestThreadTime: kotlinx.datetime.Instant,
    val newestThreadTime: kotlinx.datetime.Instant
)

/**
 * 导出格式枚举。
 */
enum class ExportFormat {
    JSON,
    CSV,
    MARKDOWN
}
