package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.MemoryCompressor
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.MemoryProcessorOptions
import ai.kastrax.memory.api.MemoryTag
import ai.kastrax.memory.api.MemoryTagManager
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.SemanticMemory
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.api.SemanticSearchResult
import ai.kastrax.memory.api.VectorStorage
import ai.kastrax.memory.api.WorkingMemory
import ai.kastrax.memory.api.WorkingMemoryConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 增强型内存实现，支持工作内存、语义搜索和记忆处理器。
 */
class EnhancedMemory(
    private val storage: Any,
    private val lastMessagesCount: Int = 10,
    private val semanticRecallEnabled: Boolean = false,
    private val embeddingGenerator: EmbeddingGenerator? = null,
    private val vectorStorage: VectorStorage? = null,
    private val processors: List<MemoryProcessor> = emptyList(),
    private val workingMemoryConfig: WorkingMemoryConfig? = null,
    private val memoryCompressor: MemoryCompressor? = null,
    private val compressionConfig: MemoryCompressionConfig = MemoryCompressionConfig(),
    private val tagManagerEnabled: Boolean = false,
    private val threadSharingEnabled: Boolean = false
) : Memory, KastraXBase(component = "MEMORY", name = "enhanced") {
    private val mutex = Mutex()
    private val threads = mutableMapOf<String, String>() // threadId -> title
    private val messages = mutableMapOf<String, MutableList<MemoryMessage>>() // threadId -> messages

    private val workingMemory: WorkingMemory? = if (workingMemoryConfig?.enabled == true) {
        WorkingMemoryFactory.createWorkingMemory(workingMemoryConfig, storage)
    } else {
        null
    }

    private val semanticMemory: SemanticMemory? = if (semanticRecallEnabled &&
        embeddingGenerator != null && vectorStorage != null) {
        InMemorySemanticMemory(embeddingGenerator, vectorStorage)
    } else {
        null
    }

    private val tagManager: MemoryTagManager? = if (tagManagerEnabled) {
        TagManagerFactory.createTagManager(storage)
    } else {
        null
    }

    // 线程共享映射：共享线程ID -> 原始线程ID列表
    private val sharedThreads = mutableMapOf<String, MutableList<String>>()

    // 线程访问控制：线程ID -> 访问控制列表
    private val threadAccessControl = mutableMapOf<String, MutableSet<String>>()

    override suspend fun saveMessage(message: Message, threadId: String): String {
        // 检查线程是否存在
        require(threadExists(threadId)) { "线程不存在: $threadId" }

        val messageId = UUID.randomUUID().toString()

        // 创建内存消息
        val memoryMessage = MemoryMessage(
            id = messageId,
            threadId = threadId,
            message = message,
            createdAt = Clock.System.now()
        )

        // 保存消息
        mutex.withLock {
            val threadMessages = messages.getOrPut(threadId) { mutableListOf() }
            threadMessages.add(memoryMessage)

            // 如果是共享线程，也保存到原始线程
            if (threadSharingEnabled) {
                val originalThreads = sharedThreads[threadId] ?: emptyList()
                for (originalThreadId in originalThreads) {
                    val originalThreadMessages = messages.getOrPut(originalThreadId) { mutableListOf() }
                    originalThreadMessages.add(memoryMessage.copy(threadId = originalThreadId))
                }
            }
        }

        // 如果启用了语义内存，保存到语义内存
        semanticMemory?.saveMessage(message, threadId)

        // 如果启用了标签管理器，注册消息到线程
        if (tagManagerEnabled && tagManager != null) {
            when (tagManager) {
                is InMemoryTagManager -> tagManager.registerMessageToThread(messageId, threadId)
                is RedisTagManager -> tagManager.registerMessageToThread(messageId, threadId)
            }

            // 自动添加角色标签
            tagManager.addTagToMessage(
                messageId,
                MemoryTag(
                    name = "role",
                    value = message.role.toString(),
                    color = when (message.role) {
                        ai.kastrax.memory.api.MessageRole.USER -> "#2196F3"
                        ai.kastrax.memory.api.MessageRole.ASSISTANT -> "#4CAF50"
                        ai.kastrax.memory.api.MessageRole.SYSTEM -> "#FF9800"
                        ai.kastrax.memory.api.MessageRole.TOOL -> "#9E9E9E"
                        else -> "#9E9E9E"
                    }
                )
            )
        }

        return messageId
    }

    override suspend fun getMessages(
        threadId: String,
        limit: Int,
        processors: List<MemoryProcessor>?
    ): List<MemoryMessage> {
        // 获取消息
        val threadMessages = mutex.withLock {
            messages[threadId]?.sortedBy { it.createdAt } ?: emptyList()
        }

        // 应用内存压缩（如果启用）
        var processedMessages = threadMessages
        if (memoryCompressor != null && memoryCompressor.shouldCompress(threadMessages, compressionConfig)) {
            logger.info { "对线程 $threadId 的消息进行压缩，原始消息数: ${threadMessages.size}" }
            processedMessages = memoryCompressor.compress(threadMessages, compressionConfig)

            // 更新内存中的消息
            mutex.withLock {
                messages[threadId] = processedMessages.toMutableList()
            }

            logger.info { "压缩完成，压缩后消息数: ${processedMessages.size}" }
        }

        // 如果没有处理器，直接返回最近的消息
        if (processors.isNullOrEmpty() && this.processors.isEmpty()) {
            return processedMessages.takeLast(limit)
        }

        // 应用处理器
        val allProcessors = (processors ?: emptyList()) + this.processors

        for (processor in allProcessors) {
            processedMessages = processor.process(
                processedMessages,
                MemoryProcessorOptions()
            )
        }

        // 返回处理后的消息
        return processedMessages.takeLast(limit)
    }

    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        // 如果启用了语义内存，使用语义搜索
        if (semanticRecallEnabled && semanticMemory != null) {
            return semanticMemory.getSemanticRecallMessages(
                query = query,
                threadId = threadId,
                config = SemanticRecallConfig(topK = limit)
            )
        }

        // 否则，使用简单的关键词搜索
        return mutex.withLock {
            messages[threadId]?.filter {
                it.message.content.contains(query, ignoreCase = true)
            }?.sortedBy { it.createdAt }?.take(limit) ?: emptyList()
        }
    }

    override suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        return semanticMemory?.semanticSearch(query, threadId, config) ?: emptyList()
    }

    override suspend fun createThread(title: String?): String {
        val threadId = UUID.randomUUID().toString()

        mutex.withLock {
            threads[threadId] = title ?: ""
            messages[threadId] = mutableListOf()
        }

        // 如果启用了工作内存，初始化工作内存
        if (workingMemory != null && workingMemoryConfig?.enabled == true) {
            workingMemory.updateWorkingMemory(threadId, workingMemoryConfig.template)
        }

        return threadId
    }

    /**
     * 获取工作内存系统消息。
     */
    suspend fun getWorkingMemorySystemMessage(threadId: String): String? {
        return workingMemory?.getSystemMessage(threadId, workingMemoryConfig)
    }

    /**
     * 获取工作内存工具。
     */
    fun getWorkingMemoryTools(): Map<String, Any> {
        return workingMemory?.getTools(workingMemoryConfig) ?: emptyMap()
    }

    /**
     * 检查线程是否存在。
     */
    private suspend fun threadExists(threadId: String): Boolean {
        return mutex.withLock {
            threads.containsKey(threadId)
        }
    }

    override suspend fun getThread(threadId: String): ai.kastrax.memory.api.MemoryThread? {
        return mutex.withLock {
            val title = threads[threadId] ?: return@withLock null
            val threadMessages = messages[threadId] ?: emptyList()

            val createdAt = threadMessages.minOfOrNull { it.createdAt } ?: Clock.System.now()
            val updatedAt = threadMessages.maxOfOrNull { it.createdAt } ?: createdAt

            ai.kastrax.memory.api.MemoryThread(
                id = threadId,
                title = title,
                createdAt = createdAt,
                updatedAt = updatedAt,
                messageCount = threadMessages.size
            )
        }
    }

    override suspend fun listThreads(limit: Int, offset: Int): List<ai.kastrax.memory.api.MemoryThread> {
        return mutex.withLock {
            threads.keys.mapNotNull { threadId ->
                getThread(threadId)
            }.sortedByDescending { it.updatedAt }
             .drop(offset)
             .take(limit)
        }
    }

    override suspend fun deleteThread(threadId: String): Boolean {
        return mutex.withLock {
            if (!threads.containsKey(threadId)) {
                return@withLock false
            }

            threads.remove(threadId)
            messages.remove(threadId)

            // 如果启用了语义内存，也删除语义内存中的线程
            semanticMemory?.let {
                // 这里假设语义内存有删除线程的功能
                // 实际实现可能需要调整
            }

            // 如果启用了线程共享，也删除相关的共享映射
            if (threadSharingEnabled) {
                sharedThreads.remove(threadId)
                threadAccessControl.remove(threadId)
            }

            true
        }
    }

    /**
     * 检查用户是否有权限访问线程。
     *
     * @param threadId 线程ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    suspend fun hasAccessToThread(threadId: String, userId: String): Boolean {
        if (!threadSharingEnabled) {
            return true
        }

        return mutex.withLock {
            // 如果没有访问控制列表，则允许访问
            val accessList = threadAccessControl[threadId] ?: return@withLock true

            // 如果访问控制列表为空，则允许访问
            if (accessList.isEmpty()) {
                return@withLock true
            }

            // 检查用户是否在访问控制列表中
            accessList.contains(userId)
        }
    }

    /**
     * 添加用户到线程的访问控制列表。
     *
     * @param threadId 线程ID
     * @param userId 用户ID
     * @return 是否成功
     */
    suspend fun addUserToThreadAccess(threadId: String, userId: String): Boolean {
        if (!threadSharingEnabled) {
            return false
        }

        return mutex.withLock {
            if (!threads.containsKey(threadId)) {
                return@withLock false
            }

            val accessList = threadAccessControl.getOrPut(threadId) { mutableSetOf() }
            accessList.add(userId)
            true
        }
    }

    /**
     * 从线程的访问控制列表中移除用户。
     *
     * @param threadId 线程ID
     * @param userId 用户ID
     * @return 是否成功
     */
    suspend fun removeUserFromThreadAccess(threadId: String, userId: String): Boolean {
        if (!threadSharingEnabled) {
            return false
        }

        return mutex.withLock {
            val accessList = threadAccessControl[threadId] ?: return@withLock false
            val removed = accessList.remove(userId)

            if (accessList.isEmpty()) {
                threadAccessControl.remove(threadId)
            }

            removed
        }
    }

    /**
     * 创建共享线程。
     *
     * @param title 线程标题
     * @param originalThreadIds 要共享的原始线程ID列表
     * @return 共享线程ID
     */
    suspend fun createSharedThread(title: String?, originalThreadIds: List<String>): String {
        if (!threadSharingEnabled) {
            error("线程共享未启用")
        }

        // 检查原始线程是否存在
        mutex.withLock {
            for (threadId in originalThreadIds) {
                if (!threads.containsKey(threadId)) {
                    require(false) { "原始线程不存在: $threadId" }
                }
            }
        }

        // 创建新线程
        val sharedThreadId = createThread(title ?: "共享线程")

        // 添加到共享线程映射
        mutex.withLock {
            sharedThreads[sharedThreadId] = originalThreadIds.toMutableList()
        }

        // 复制原始线程的消息到共享线程
        val allMessages = mutableListOf<MemoryMessage>()

        mutex.withLock {
            for (threadId in originalThreadIds) {
                val threadMessages = messages[threadId] ?: continue
                allMessages.addAll(threadMessages.map { it.copy(threadId = sharedThreadId) })
            }

            // 按时间排序
            allMessages.sortBy { it.createdAt }

            // 保存到共享线程
            messages[sharedThreadId] = allMessages.toMutableList()
        }

        return sharedThreadId
    }

    /**
     * 获取消息的标签。
     *
     * @param messageId 消息ID
     * @return 标签列表
     */
    suspend fun getMessageTags(messageId: String): List<MemoryTag> {
        if (!tagManagerEnabled || tagManager == null) {
            return emptyList()
        }

        return tagManager.getMessageTags(messageId)
    }

    /**
     * 添加标签到消息。
     *
     * @param messageId 消息ID
     * @param tag 标签
     * @return 是否成功
     */
    suspend fun addTagToMessage(messageId: String, tag: MemoryTag): Boolean {
        if (!tagManagerEnabled || tagManager == null) {
            return false
        }

        return tagManager.addTagToMessage(messageId, tag)
    }

    /**
     * 根据标签搜索消息。
     *
     * @param threadId 线程ID
     * @param tagName 标签名称
     * @param tagValue 标签值（可选）
     * @param limit 限制返回的消息数量
     * @return 消息列表
     */
    suspend fun searchMessagesByTag(
        threadId: String,
        tagName: String,
        tagValue: String? = null,
        limit: Int = 10
    ): List<MemoryMessage> {
        if (!tagManagerEnabled || tagManager == null) {
            return emptyList()
        }

        return mutex.withLock {
            // 获取线程消息
            val threadMessagesList = messages[threadId] ?: return@withLock emptyList()

            // 过滤带有指定标签的消息
            filterMessagesByTag(threadMessagesList, tagManager, tagName, tagValue, limit)
        }
    }

    /**
     * 根据标签过滤消息。
     */
    private suspend fun filterMessagesByTag(
        messages: List<MemoryMessage>,
        tagManager: MemoryTagManager,
        tagName: String,
        tagValue: String?,
        limit: Int
    ): List<MemoryMessage> {
        val result = mutableListOf<MemoryMessage>()

        for (message in messages) {
            // 获取消息的标签
            val tags = tagManager.getMessageTags(message.id)

            // 检查是否匹配标签
            val matches = matchesTag(tags, tagName, tagValue)

            if (matches) {
                result.add(message)
                if (result.size >= limit) {
                    break
                }
            }
        }

        return result
    }

    /**
     * 检查标签是否匹配。
     */
    private fun matchesTag(
        tags: List<MemoryTag>,
        tagName: String,
        tagValue: String?
    ): Boolean {
        return if (tagValue != null) {
            tags.any { it.name == tagName && it.value == tagValue }
        } else {
            tags.any { it.name == tagName }
        }
    }
}


