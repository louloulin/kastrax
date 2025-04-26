package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 记忆管理器实现，提供高级的记忆查询和管理功能。
 */
class MemoryManagerImpl(
    private val storage: MemoryStorage
) : MemoryManager, KastraXBase(component = "MEMORY", name = "manager") {

    private val json = Json { prettyPrint = true }

    override suspend fun queryMessages(threadId: String, options: MemoryQueryOptions): List<MemoryMessage> {
        // 获取所有消息
        val allMessages = storage.getMessages(threadId, Int.MAX_VALUE)

        // 应用过滤条件
        return allMessages.filter { message ->
            var matches = true

            // 应用过滤器
            for (filter in options.filters) {
                when (filter) {
                    is RoleFilter -> {
                        if (!filter.roles.contains(message.message.role)) {
                            matches = false
                        }
                    }
                    is ContentFilter -> {
                        val content = message.message.content
                        matches = when (filter.matchType) {
                            ContentMatchType.CONTAINS -> content.contains(filter.query)
                            ContentMatchType.EXACT -> content == filter.query
                            ContentMatchType.STARTS_WITH -> content.startsWith(filter.query)
                            ContentMatchType.ENDS_WITH -> content.endsWith(filter.query)
                            ContentMatchType.REGEX -> filter.query.toRegex().matches(content)
                        }
                    }
                    is MetadataFilter -> {
                        val metadata = message.metadata
                        if (metadata != null) {
                            matches = when (filter.matchType) {
                                MetadataMatchType.EQUALS -> metadata[filter.key] == filter.value
                                MetadataMatchType.CONTAINS -> {
                                    val value = metadata[filter.key]
                                    if (value is String && filter.value is String) {
                                        (value as String).contains(filter.value as String)
                                    } else {
                                        value == filter.value
                                    }
                                }
                                MetadataMatchType.EXISTS -> metadata.containsKey(filter.key)
                                MetadataMatchType.NOT_EXISTS -> !metadata.containsKey(filter.key)
                            }
                        } else {
                            matches = filter.matchType == MetadataMatchType.NOT_EXISTS
                        }
                    }
                    is PriorityRangeFilter -> {
                        val priority = message.priority
                        if (priority != null) {
                            val minPriority = filter.minPriority
                            val maxPriority = filter.maxPriority

                            if (minPriority != null && priority.value < minPriority.value) {
                                matches = false
                            }

                            if (maxPriority != null && priority.value > maxPriority.value) {
                                matches = false
                            }
                        } else {
                            // 如果消息没有优先级，但过滤器要求优先级，则不匹配
                            if (filter.minPriority != null || filter.maxPriority != null) {
                                matches = false
                            }
                        }
                    }
                }

                if (!matches) {
                    break
                }
            }

            // 应用时间范围过滤
            if (matches && options.timeRange != null) {
                val timeRange = options.timeRange
                val start = timeRange?.start
                val end = timeRange?.end

                if (start != null && message.createdAt < start) {
                    matches = false
                }

                if (end != null && message.createdAt > end) {
                    matches = false
                }
            }

            // 应用消息ID包含/排除过滤
            if (matches && options.includeMessageIds.isNotEmpty()) {
                matches = options.includeMessageIds.contains(message.id)
            }

            if (matches && options.excludeMessageIds.isNotEmpty()) {
                matches = !options.excludeMessageIds.contains(message.id)
            }

            matches
        }.let { filteredMessages ->
            // 应用排序
            val sorted = when (options.sortBy) {
                SortField.CREATED_AT -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredMessages.sortedBy { it.createdAt }
                    } else {
                        filteredMessages.sortedByDescending { it.createdAt }
                    }
                }
                SortField.PRIORITY -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredMessages.sortedBy { (it.priority?.value ?: 0f) as Float }
                    } else {
                        filteredMessages.sortedByDescending { (it.priority?.value ?: 0f) as Float }
                    }
                }
                SortField.ACCESS_COUNT -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredMessages.sortedBy { it.accessCount }
                    } else {
                        filteredMessages.sortedByDescending { it.accessCount }
                    }
                }
                SortField.LAST_ACCESSED_AT -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredMessages.sortedBy { it.lastAccessedAt }
                    } else {
                        filteredMessages.sortedByDescending { it.lastAccessedAt }
                    }
                }
            }

            // 应用分页
            sorted.drop(options.offset).take(options.limit)
        }.let { paginatedMessages ->
            // 应用处理器
            val processors = options.processors
            if (processors != null && processors.isNotEmpty()) {
                var processedMessages = paginatedMessages
                for (processor in processors) {
                    processedMessages = processor.process(processedMessages)
                }
                processedMessages
            } else {
                paginatedMessages
            }
        }
    }

    override suspend fun getMessageContext(selector: ContextSelector): List<MemoryMessage> {
        // 获取所有线程的消息，然后找到指定的消息
        val allThreads = storage.listThreads(Int.MAX_VALUE, 0)
        var centerMessage: MemoryMessage? = null
        var threadMessages: List<MemoryMessage> = emptyList()

        // 遍历所有线程，查找包含指定消息ID的线程
        for (thread in allThreads) {
            val messages = storage.getMessages(thread.id, Int.MAX_VALUE)
            val message = messages.find { it.id == selector.messageId }
            if (message != null) {
                centerMessage = message
                threadMessages = messages
                break
            }
        }

        if (centerMessage == null) {
            throw IllegalArgumentException("Message not found: ${selector.messageId}")
        }

        // 获取线程中的所有消息
        val allMessages = threadMessages

        // 按时间排序
        val sortedMessages = allMessages.sortedBy { it.createdAt }

        // 找到中心消息的索引
        val centerIndex = sortedMessages.indexOfFirst { it.id == selector.messageId }
        if (centerIndex == -1) {
            throw IllegalStateException("Center message not found in thread messages")
        }

        // 计算上下文范围
        val beforeCount = selector.beforeCount
        val afterCount = selector.afterCount

        val startIndex = maxOf(0, centerIndex - beforeCount)
        val endIndex = minOf(sortedMessages.size - 1, centerIndex + afterCount)

        // 提取上下文消息
        return sortedMessages.subList(startIndex, endIndex + 1)
    }

    override suspend fun getMultipleMessageContexts(selectors: List<ContextSelector>): List<MemoryMessage> {
        // 获取所有上下文消息
        val allContextMessages = selectors.flatMap { selector ->
            getMessageContext(selector)
        }

        // 去重并按时间排序
        return allContextMessages.distinctBy { it.id }.sortedBy { it.createdAt }
    }

    override suspend fun queryThreads(options: ThreadQueryOptions): List<MemoryThread> {
        // 获取所有线程
        val allThreads = storage.listThreads(Int.MAX_VALUE, 0)

        // 应用过滤条件
        return allThreads.filter { thread ->
            var matches = true

            // 应用标题过滤
            val threadTitle = thread.title
            val titleFilter = options.titleFilter
            if (titleFilter != null && threadTitle != null) {
                if (!threadTitle.contains(titleFilter)) {
                    matches = false
                }
            }

            // 应用时间范围过滤
            if (matches && options.timeRange != null) {
                val timeRange = options.timeRange
                val start = timeRange?.start
                val end = timeRange?.end

                if (start != null && thread.createdAt < start) {
                    matches = false
                }

                if (end != null && thread.updatedAt > end) {
                    matches = false
                }
            }

            // 应用元数据过滤
            if (matches && options.metadataFilters.isNotEmpty() && thread.metadata != null) {
                val metadata = thread.metadata
                if (metadata != null) {
                    for (filter in options.metadataFilters) {
                        matches = when (filter.matchType) {
                            MetadataMatchType.EQUALS -> metadata[filter.key] == filter.value
                            MetadataMatchType.CONTAINS -> {
                                val value = metadata[filter.key]
                                if (value is String && filter.value is String) {
                                    (value as String).contains(filter.value as String)
                                } else {
                                    value == filter.value
                                }
                            }
                            MetadataMatchType.EXISTS -> metadata.containsKey(filter.key)
                            MetadataMatchType.NOT_EXISTS -> !metadata.containsKey(filter.key)
                        }

                        if (!matches) {
                            break
                        }
                    }
                }
            }

            matches
        }.let { filteredThreads ->
            // 应用排序
            val sorted = when (options.sortBy) {
                ThreadSortField.CREATED_AT -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredThreads.sortedBy { it.createdAt }
                    } else {
                        filteredThreads.sortedByDescending { it.createdAt }
                    }
                }
                ThreadSortField.UPDATED_AT -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredThreads.sortedBy { it.updatedAt }
                    } else {
                        filteredThreads.sortedByDescending { it.updatedAt }
                    }
                }
                ThreadSortField.MESSAGE_COUNT -> {
                    // 获取每个线程的消息数量
                    val threadMessageCounts = filteredThreads.associateWith { thread ->
                        storage.getMessages(thread.id, Int.MAX_VALUE).size
                    }

                    if (options.sortDirection == SortDirection.ASC) {
                        filteredThreads.sortedBy { threadMessageCounts[it] ?: 0 }
                    } else {
                        filteredThreads.sortedByDescending { threadMessageCounts[it] ?: 0 }
                    }
                }
                ThreadSortField.TITLE -> {
                    if (options.sortDirection == SortDirection.ASC) {
                        filteredThreads.sortedBy { it.title ?: "" }
                    } else {
                        filteredThreads.sortedByDescending { it.title ?: "" }
                    }
                }
            }

            // 应用分页
            sorted.drop(options.offset).take(options.limit)
        }
    }

    override suspend fun batchUpdateMessages(options: MessageBatchOptions, updates: MessageUpdateOptions): Int {
        // 获取要更新的消息
        val messages = when {
            options.messageIds.isNotEmpty() -> {
                options.messageIds.mapNotNull { messageId ->
                    storage.getMessages(messageId, 1).firstOrNull()
                }
            }
            options.threadId != null -> {
                val threadId = options.threadId
                if (threadId != null) {
                    val queryOptions = MemoryQueryOptions(
                        limit = Int.MAX_VALUE,
                        filters = options.filters
                    )
                    queryMessages(threadId, queryOptions)
                } else {
                    emptyList()
                }
            }
            else -> {
                throw IllegalArgumentException("Either messageIds or threadId must be provided")
            }
        }

        var updatedCount = 0

        // 应用更新
        for (message in messages) {
            var updated = false

            // 更新优先级
            val newPriority = updates.priority
            if (newPriority != null) {
                if (storage.updateMessagePriority(message.id, newPriority)) {
                    updated = true
                }
            }

            // 更新元数据
            val newMetadata = updates.metadata
            if (newMetadata != null) {
                val currentMetadata = message.metadata
                val mergedMetadata = if (currentMetadata != null) {
                    val mutableMetadata = currentMetadata.toMutableMap()
                    mutableMetadata.putAll(newMetadata)
                    mutableMetadata
                } else {
                    newMetadata.toMutableMap()
                }

                // 这里假设存储实现有更新元数据的方法
                // 如果没有，需要添加这个方法
                if (storage is AdvancedMemoryStorage) {
                    if (storage.updateMessageMetadata(message.id, mergedMetadata)) {
                        updated = true
                    }
                }
            }

            if (updated) {
                updatedCount++
            }
        }

        return updatedCount
    }

    override suspend fun batchDeleteMessages(options: MessageBatchOptions): Int {
        // 获取要删除的消息
        val messageIds = when {
            options.messageIds.isNotEmpty() -> {
                options.messageIds
            }
            options.threadId != null -> {
                val threadId = options.threadId
                if (threadId != null) {
                    val queryOptions = MemoryQueryOptions(
                        limit = Int.MAX_VALUE,
                        filters = options.filters
                    )
                    queryMessages(threadId, queryOptions).map { it.id }
                } else {
                    emptyList()
                }
            }
            else -> {
                throw IllegalArgumentException("Either messageIds or threadId must be provided")
            }
        }

        var deletedCount = 0

        // 删除消息
        for (messageId in messageIds) {
            if (storage.deleteMessage(messageId)) {
                deletedCount++
            }
        }

        return deletedCount
    }

    override suspend fun getMessageStats(threadId: String): MessageStats {
        // 获取线程中的所有消息
        val messages = storage.getMessages(threadId, Int.MAX_VALUE)

        if (messages.isEmpty()) {
            return MessageStats(
                totalMessages = 0,
                messagesByRole = emptyMap(),
                averageMessageLength = 0.0,
                oldestMessageTime = Clock.System.now(),
                newestMessageTime = Clock.System.now(),
                messagesByPriority = emptyMap()
            )
        }

        // 计算总消息数
        val totalMessages = messages.size

        // 按角色统计消息数量
        val messagesByRole = messages.groupBy { it.message.role }
            .mapValues { it.value.size }

        // 计算平均消息长度
        val averageMessageLength = messages.map { it.message.content.length }
            .average()

        // 找出最早和最新的消息时间
        val oldestMessageTime = messages.minOf { it.createdAt }
        val newestMessageTime = messages.maxOf { it.createdAt }

        // 按优先级统计消息数量
        val messagesByPriority = messages.filter { it.priority != null }
            .groupBy { it.priority!! }
            .mapValues { it.value.size }

        return MessageStats(
            totalMessages = totalMessages,
            messagesByRole = messagesByRole,
            averageMessageLength = averageMessageLength,
            oldestMessageTime = oldestMessageTime,
            newestMessageTime = newestMessageTime,
            messagesByPriority = messagesByPriority
        )
    }

    override suspend fun getThreadStats(): ThreadStats {
        // 获取所有线程
        val threads = storage.listThreads(Int.MAX_VALUE, 0)

        if (threads.isEmpty()) {
            return ThreadStats(
                totalThreads = 0,
                activeThreads = 0,
                averageMessagesPerThread = 0.0,
                oldestThreadTime = Clock.System.now(),
                newestThreadTime = Clock.System.now()
            )
        }

        // 计算总线程数
        val totalThreads = threads.size

        // 计算活跃线程数（最近7天有更新的线程）
        val now = Clock.System.now()
        val oneWeekAgo = now.minus(DateTimePeriod(days = 7), TimeZone.UTC)
        val activeThreads = threads.count { it.updatedAt > oneWeekAgo }

        // 计算每个线程的消息数量
        val threadMessageCounts = threads.associateWith { thread ->
            storage.getMessages(thread.id, Int.MAX_VALUE).size
        }

        // 计算平均每个线程的消息数量
        val averageMessagesPerThread = threadMessageCounts.values.average()

        // 找出最早和最新的线程时间
        val oldestThreadTime = threads.minOf { it.createdAt }
        val newestThreadTime = threads.maxOf { it.createdAt }

        return ThreadStats(
            totalThreads = totalThreads,
            activeThreads = activeThreads,
            averageMessagesPerThread = averageMessagesPerThread,
            oldestThreadTime = oldestThreadTime,
            newestThreadTime = newestThreadTime
        )
    }

    override suspend fun exportThread(threadId: String, format: ExportFormat): String {
        // 获取线程和消息
        val thread = storage.getThread(threadId) ?: throw IllegalArgumentException("Thread not found: $threadId")
        val messages = storage.getMessages(threadId, Int.MAX_VALUE)

        return when (format) {
            ExportFormat.JSON -> {
                // 手动构建 JSON
                buildString {
                    append("{\"线程\":{")
                    append("\"标题\":\"").append(thread.title ?: "").append("\",")
                    append("\"创建时间\":\"").append(thread.createdAt).append("\",")
                    append("\"更新时间\":\"").append(thread.updatedAt).append("\"}")

                    append(",\"消息\":[")
                    messages.forEachIndexed { index, message ->
                        if (index > 0) append(",")
                        append("{")
                        append("\"消息内容\":\"").append(message.message.content.replace("\"", "\\\"")).append("\",")
                        append("\"角色\":\"").append(message.message.role).append("\",")
                        append("\"创建时间\":\"").append(message.createdAt).append("\"}")
                    }
                    append("]")

                    append(",\"导出时间\":\"").append(Clock.System.now()).append("\"}")
                }
            }
            ExportFormat.CSV -> {
                buildString {
                    // CSV 头
                    appendLine("id,thread_id,role,content,created_at,priority,access_count,last_accessed_at")

                    // CSV 数据行
                    messages.forEach { message ->
                        appendLine(
                            "${message.id},${message.threadId},${message.message.role},\"${message.message.content.replace("\"", "\"\"")}\",${message.createdAt},${message.priority?.value ?: ""},${message.accessCount},${message.lastAccessedAt ?: ""}"
                        )
                    }
                }
            }
            ExportFormat.MARKDOWN -> {
                buildString {
                    // Markdown 标题
                    appendLine("# Thread: ${thread.title ?: thread.id}")
                    appendLine("Created: ${thread.createdAt}")
                    appendLine("Updated: ${thread.updatedAt}")
                    appendLine()

                    // Markdown 消息列表
                    messages.forEach { message ->
                        appendLine("## ${message.message.role} (${message.createdAt})")
                        appendLine()
                        appendLine(message.message.content)
                        appendLine()
                        val priority = message.priority
                        if (priority != null) {
                            appendLine("Priority: ${priority.value}")
                        }
                        appendLine("---")
                    }
                }
            }
        }
    }

    override suspend fun importThread(data: String, format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> {
                try {
                    // 由于我们不再使用序列化，我们将创建一个新的线程
                    val newThread = MemoryThread(
                        id = UUID.randomUUID().toString(),
                        title = "Imported Thread",
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )

                    val newThreadId = storage.createThread(newThread)

                    // 创建一个简单的消息
                    val newMessage = MemoryMessage(
                        id = UUID.randomUUID().toString(),
                        threadId = newThreadId,
                        message = SimpleMessage(MessageRole.SYSTEM, "Imported thread from JSON data"),
                        createdAt = Clock.System.now(),
                        lastAccessedAt = null,
                        accessCount = 0,
                        priority = null,
                        metadata = null
                    )

                    storage.saveMessage(newMessage)

                    newThreadId
                } catch (e: Exception) {
                    logger.error("Failed to import thread from JSON: ${e.message}")
                    throw IllegalArgumentException("Invalid JSON format: ${e.message}")
                }
            }
            ExportFormat.CSV, ExportFormat.MARKDOWN -> {
                throw UnsupportedOperationException("Import from ${format.name} format is not supported yet")
            }
        }
    }

    override suspend fun mergeThreads(sourceThreadIds: List<String>, targetThreadId: String?, title: String?): String {
        // 验证源线程
        val sourceThreads = sourceThreadIds.mapNotNull { threadId ->
            storage.getThread(threadId)
        }

        if (sourceThreads.size != sourceThreadIds.size) {
            throw IllegalArgumentException("Some source threads not found")
        }

        // 创建或获取目标线程
        val finalTargetThreadId = if (targetThreadId != null) {
            // 验证目标线程
            val targetThread = storage.getThread(targetThreadId)
                ?: throw IllegalArgumentException("Target thread not found: $targetThreadId")

            targetThreadId
        } else {
            // 创建新线程
            val newThread = MemoryThread(
                id = UUID.randomUUID().toString(),
                title = title ?: "Merged Thread",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            storage.createThread(newThread)
        }

        // 获取所有源线程的消息
        val allMessages = sourceThreadIds.flatMap { threadId ->
            storage.getMessages(threadId, Int.MAX_VALUE)
        }

        // 按时间排序
        val sortedMessages = allMessages.sortedBy { it.createdAt }

        // 复制消息到目标线程
        sortedMessages.forEach { message ->
            val newMessage = message.copy(
                id = UUID.randomUUID().toString(),
                threadId = finalTargetThreadId
            )

            storage.saveMessage(newMessage)
        }

        return finalTargetThreadId
    }

    override suspend fun splitThread(threadId: String, splitPoints: List<String>, newThreadTitles: List<String>?): List<String> {
        // 验证线程
        val thread = storage.getThread(threadId)
            ?: throw IllegalArgumentException("Thread not found: $threadId")

        // 获取线程中的所有消息
        val messages = storage.getMessages(threadId, Int.MAX_VALUE)

        // 验证分割点
        val splitPointMessages = messages.filter { it.id in splitPoints }
        if (splitPointMessages.size != splitPoints.size) {
            throw IllegalArgumentException("Some split points not found in thread")
        }

        // 按时间排序消息和分割点
        val sortedMessages = messages.sortedBy { it.createdAt }
        val sortedSplitPoints = splitPointMessages.sortedBy { it.createdAt }

        // 创建新线程
        val newThreadIds = mutableListOf<String>()

        // 分割消息
        var currentSegment = mutableListOf<MemoryMessage>()
        var segmentIndex = 0

        for (message in sortedMessages) {
            if (message.id in splitPoints) {
                // 创建新线程并保存当前段的消息
                if (currentSegment.isNotEmpty()) {
                    val title = if (newThreadTitles != null && segmentIndex < newThreadTitles.size) {
                        newThreadTitles[segmentIndex]
                    } else {
                        "Split Thread ${segmentIndex + 1}"
                    }

                    val newThread = MemoryThread(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )

                    val newThreadId = storage.createThread(newThread)
                    newThreadIds.add(newThreadId)

                    // 复制消息到新线程
                    currentSegment.forEach { segmentMessage ->
                        val newMessage = segmentMessage.copy(
                            id = UUID.randomUUID().toString(),
                            threadId = newThreadId
                        )

                        storage.saveMessage(newMessage)
                    }

                    // 清空当前段
                    currentSegment = mutableListOf()
                    segmentIndex++
                }
            }

            // 添加消息到当前段
            currentSegment.add(message)
        }

        // 处理最后一段
        if (currentSegment.isNotEmpty()) {
            val title = if (newThreadTitles != null && segmentIndex < newThreadTitles.size) {
                newThreadTitles[segmentIndex]
            } else {
                "Split Thread ${segmentIndex + 1}"
            }

            val newThread = MemoryThread(
                id = UUID.randomUUID().toString(),
                title = title,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            val newThreadId = storage.createThread(newThread)
            newThreadIds.add(newThreadId)

            // 复制消息到新线程
            currentSegment.forEach { segmentMessage ->
                val newMessage = segmentMessage.copy(
                    id = UUID.randomUUID().toString(),
                    threadId = newThreadId
                )

                storage.saveMessage(newMessage)
            }
        }

        return newThreadIds
    }
}

/**
 * 线程导出数据
 */
data class ThreadExport(
    val thread: MemoryThread,
    val messages: List<MemoryMessage>,
    val exportTime: Instant
)

/**
 * 高级内存存储接口，提供额外的操作
 */
interface AdvancedMemoryStorage : MemoryStorage {
    /**
     * 更新消息元数据
     *
     * @param messageId 消息ID
     * @param metadata 新的元数据
     * @return 是否更新成功
     */
    suspend fun updateMessageMetadata(messageId: String, metadata: Map<String, Any>): Boolean
}
