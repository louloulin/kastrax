package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.*
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 内存系统实现。
 */
class MemoryImpl(
    private val storage: MemoryStorage,
    @Suppress("unused") private val lastMessages: Int = 10, // 目前未使用，但保留以备将来需要
    private val semanticRecall: Boolean = false,
    private val priorityConfig: MemoryPriorityConfig = MemoryPriorityConfig(),
    private val structuredMemoryConfig: StructuredMemoryConfig = StructuredMemoryConfig()
) : Memory, KastraXBase(component = "MEMORY", name = "memory") {

    // 优先级处理器
    private val priorityProcessor = BasicMemoryPriorityProcessor(storage)

    // 结构化记忆
    private val structuredMemory = InMemoryStructuredMemory(structuredMemoryConfig)

    override suspend fun saveMessage(message: Message, threadId: String, priority: MemoryPriority?, metadata: Map<String, Any>?): String {
        // 检查线程是否存在
        val thread = storage.getThread(threadId) ?: throw IllegalArgumentException("Thread not found: $threadId")

        // 计算消息优先级
        val messagePriority = priority ?: if (priorityConfig.enablePriority) {
            priorityProcessor.calculatePriority(message, priorityConfig)
        } else {
            priorityConfig.defaultPriority
        }

        // 创建内存消息
        val memoryMessage = MemoryMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            message = message,
            createdAt = Clock.System.now(),
            priority = messagePriority,
            lastAccessedAt = Clock.System.now(),
            accessCount = 0,
            metadata = metadata
        )

        // 保存消息
        val messageId = storage.saveMessage(memoryMessage)

        // 更新线程
        storage.updateThread(threadId, mapOf(
            "updatedAt" to Clock.System.now(),
            "messageCount" to (thread.messageCount + 1)
        ))

        return messageId
    }

    override suspend fun getMessages(
        threadId: String,
        limit: Int,
        processors: List<MemoryProcessor>?
    ): List<MemoryMessage> {
        // 获取消息
        var messages = storage.getMessages(threadId, limit)

        // 如果有处理器，应用处理器
        if (!processors.isNullOrEmpty()) {
            for (processor in processors) {
                messages = processor.process(
                    messages,
                    MemoryProcessorOptions()
                )
            }
        }

        return messages
    }

    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        return if (semanticRecall) {
            // 如果启用了语义召回，使用搜索功能
            storage.searchMessages(query, threadId, limit)
        } else {
            // 否则，只返回最近的消息
            storage.getMessages(threadId, limit)
        }
    }

    override suspend fun createThread(title: String?): String {
        val thread = MemoryThread(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        return storage.createThread(thread)
    }

    override suspend fun deleteThread(threadId: String): Boolean {
        return storage.deleteThread(threadId)
    }

    override suspend fun getThread(threadId: String): MemoryThread? {
        return storage.getThread(threadId)
    }

    override suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread> {
        return storage.listThreads(limit, offset)
    }

    override suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        // 基础实现不支持语义搜索，返回空列表
        return emptyList()
    }

    override suspend fun updateMessagePriority(messageId: String, priority: MemoryPriority): Boolean {
        return storage.updateMessagePriority(messageId, priority)
    }

    override suspend fun getMessagePriority(messageId: String): MemoryPriority? {
        return storage.getMessagePriority(messageId)
    }

    override suspend fun applyPriorityDecay(config: MemoryPriorityConfig): Int {
        return priorityProcessor.applyPriorityDecay(config)
    }

    override suspend fun cleanupLowPriorityMessages(config: MemoryPriorityConfig): Int {
        return priorityProcessor.cleanupLowPriorityMessages(config)
    }

    override fun getStructuredMemory(): StructuredMemory? {
        return structuredMemory
    }
}
