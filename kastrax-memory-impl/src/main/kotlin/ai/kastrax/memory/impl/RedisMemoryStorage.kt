package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryThread
import ai.kastrax.memory.impl.SimpleMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.ScanParams
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Redis实现的内存存储。
 *
 * @property jedisPool Redis连接池
 * @property keyPrefix Redis键前缀
 * @property expireTime 过期时间（秒）
 */
class RedisMemoryStorage(
    private val jedisPool: JedisPool,
    private val keyPrefix: String = "kastrax:memory:",
    private val expireTime: Long = TimeUnit.DAYS.toSeconds(90) // 默认90天过期
) : MemoryStorage, KastraXBase(component = "MEMORY_STORAGE", name = "redis") {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 键名构建
    private fun threadKey(threadId: String) = "${keyPrefix}thread:$threadId"
    private fun threadListKey() = "${keyPrefix}threads"
    private fun messageKey(messageId: String) = "${keyPrefix}message:$messageId"
    private fun threadMessagesKey(threadId: String) = "${keyPrefix}thread:$threadId:messages"

    override suspend fun saveMessage(message: MemoryMessage): String {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 生成消息ID（如果没有）
                    val messageId = message.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

                    // 序列化消息
                    val messageJson = json.encodeToString(
                        mapOf(
                            "id" to messageId,
                            "threadId" to message.threadId,
                            "role" to message.message.role.name,
                            "content" to message.message.content,
                            "name" to (message.message.name ?: ""),
                            "toolCalls" to (message.message.toolCalls.map { it.toString() }),
                            "toolCallId" to (message.message.toolCallId ?: ""),
                            "createdAt" to message.createdAt.toString(),
                            "priority" to (message.priority?.name ?: MemoryPriority.MEDIUM.name),
                            "lastAccessedAt" to (message.lastAccessedAt?.toString() ?: message.createdAt.toString()),
                            "accessCount" to (message.accessCount.toString())
                        )
                    )

                    // 保存消息
                    val pipeline = jedis.pipelined()
                    pipeline.setex(messageKey(messageId), expireTime, messageJson)
                    pipeline.zadd(threadMessagesKey(message.threadId), message.createdAt.toEpochMilliseconds().toDouble(), messageId)
                    pipeline.sync()

                    // 更新线程的最后更新时间
                    updateThread(message.threadId, mapOf("updatedAt" to message.createdAt))

                    messageId
                }
            } catch (e: Exception) {
                logger.error("保存消息到Redis失败: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val threadMessagesKey = threadMessagesKey(threadId)

                    // 获取消息ID列表（按时间倒序）
                    val messageIds = jedis.zrevrange(threadMessagesKey, 0, (limit - 1).toLong())

                    if (messageIds.isEmpty()) {
                        emptyList()
                    } else {
                        // 获取消息内容
                        val pipeline = jedis.pipelined()
                        val responses = messageIds.map { messageId ->
                            pipeline.get(messageKey(messageId))
                        }
                        pipeline.sync()

                        // 解析消息
                        val result = mutableListOf<MemoryMessage>()
                        for (response in responses) {
                            val messageJson = response.get() ?: continue
                            val message = parseMemoryMessage(messageJson)
                            if (message != null) {
                                result.add(message)
                            }
                        }
                        result
                    }
                }
            } catch (e: Exception) {
                logger.error("从Redis获取消息失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        // 基础实现：获取所有消息并在内存中过滤
        // 注意：这不是最高效的实现，但对于简单场景足够
        // 更高级的实现可以使用Redis的全文搜索功能或外部搜索引擎
        return withContext(Dispatchers.IO) {
            try {
                val allMessages = getMessages(threadId, 100) // 获取较多消息以便搜索
                allMessages
                    .filter { it.message.content.contains(query, ignoreCase = true) }
                    .take(limit)
            } catch (e: Exception) {
                logger.error("在Redis中搜索消息失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun createThread(thread: MemoryThread): String {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 序列化线程
                    val threadJson = json.encodeToString(
                        mapOf(
                            "id" to thread.id,
                            "title" to (thread.title ?: ""),
                            "createdAt" to thread.createdAt.toString(),
                            "updatedAt" to thread.updatedAt.toString(),
                            "metadata" to (thread.metadata ?: emptyMap<String, String>()),
                            "messageCount" to (thread.messageCount.toString())
                        )
                    )

                    // 保存线程
                    val pipeline = jedis.pipelined()
                    pipeline.setex(threadKey(thread.id), expireTime, threadJson)
                    pipeline.zadd(threadListKey(), thread.updatedAt.toEpochMilliseconds().toDouble(), thread.id)
                    pipeline.sync()

                    thread.id
                }
            } catch (e: Exception) {
                logger.error("创建Redis线程失败: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun deleteThread(threadId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取线程的所有消息
                    val messageIds = jedis.zrange(threadMessagesKey(threadId), 0, -1)

                    // 删除所有消息和线程
                    val pipeline = jedis.pipelined()
                    messageIds.forEach { messageId ->
                        pipeline.del(messageKey(messageId))
                    }
                    pipeline.del(threadMessagesKey(threadId))
                    pipeline.del(threadKey(threadId))
                    pipeline.zrem(threadListKey(), threadId)
                    pipeline.sync()

                    true
                }
            } catch (e: Exception) {
                logger.error("删除Redis线程失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun deleteMessage(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取消息信息
                    val messageKey = messageKey(messageId)
                    val messageJson = jedis.get(messageKey) ?: return@withContext false
                    val message = parseMemoryMessage(messageJson) ?: return@withContext false

                    // 删除消息
                    val pipeline = jedis.pipelined()
                    pipeline.del(messageKey)
                    pipeline.zrem(threadMessagesKey(message.threadId), messageId)
                    pipeline.sync()

                    true
                }
            } catch (e: Exception) {
                logger.error("删除Redis消息失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun updateMessagePriority(messageId: String, priority: MemoryPriority): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取消息信息
                    val messageKey = messageKey(messageId)
                    val messageJson = jedis.get(messageKey) ?: return@withContext false

                    // 解析消息
                    val data = json.decodeFromString<Map<String, String>>(messageJson).toMutableMap()

                    // 更新优先级
                    data["priority"] = priority.name

                    // 保存更新后的消息
                    val updatedJson = json.encodeToString(data)
                    jedis.setex(messageKey, expireTime, updatedJson)

                    true
                }
            } catch (e: Exception) {
                logger.error("更新Redis消息优先级失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getMessagePriority(messageId: String): MemoryPriority? {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取消息信息
                    val messageJson = jedis.get(messageKey(messageId)) ?: return@withContext null

                    // 解析消息
                    val data = json.decodeFromString<Map<String, String>>(messageJson)

                    // 获取优先级
                    val priorityName = data["priority"] ?: return@withContext null

                    try {
                        MemoryPriority.valueOf(priorityName)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            } catch (e: Exception) {
                logger.error("获取Redis消息优先级失败: ${e.message}")
                null
            }
        }
    }

    override suspend fun updateMessageAccess(messageId: String, lastAccessedAt: Instant, accessCount: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取消息信息
                    val messageKey = messageKey(messageId)
                    val messageJson = jedis.get(messageKey) ?: return@withContext false

                    // 解析消息
                    val data = json.decodeFromString<Map<String, String>>(messageJson).toMutableMap()

                    // 更新访问信息
                    data["lastAccessedAt"] = lastAccessedAt.toString()
                    data["accessCount"] = accessCount.toString()

                    // 保存更新后的消息
                    val updatedJson = json.encodeToString(data)
                    jedis.setex(messageKey, expireTime, updatedJson)

                    true
                }
            } catch (e: Exception) {
                logger.error("更新Redis消息访问信息失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getAllMessagesWithPriority(): List<MessagePriorityInfo> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val result = mutableListOf<MessagePriorityInfo>()
                    val scanParams = ScanParams().count(100)
                    var cursor = "0"

                    // 扫描所有消息键
                    do {
                        val scanResult = jedis.scan(cursor, scanParams.match("${keyPrefix}message:*"))
                        cursor = scanResult.cursor
                        val keys = scanResult.result

                        if (keys.isNotEmpty()) {
                            // 批量获取消息内容
                            val pipeline = jedis.pipelined()
                            val responses = keys.map { key -> pipeline.get(key) }
                            pipeline.sync()

                            // 解析消息
                            for (i in keys.indices) {
                                val messageJson = responses[i].get() ?: continue
                                val data = json.decodeFromString<Map<String, String>>(messageJson)

                                val messageId = data["id"] ?: continue
                                val priorityName = data["priority"] ?: MemoryPriority.MEDIUM.name
                                val lastAccessedAtStr = data["lastAccessedAt"] ?: data["createdAt"] ?: continue
                                val createdAtStr = data["createdAt"] ?: continue

                                val priority = try {
                                    MemoryPriority.valueOf(priorityName)
                                } catch (e: IllegalArgumentException) {
                                    MemoryPriority.MEDIUM
                                }

                                val lastAccessedAt = try {
                                    Instant.parse(lastAccessedAtStr)
                                } catch (e: Exception) {
                                    null
                                }

                                val createdAt = try {
                                    Instant.parse(createdAtStr)
                                } catch (e: Exception) {
                                    null
                                }

                                if (lastAccessedAt != null && createdAt != null) {
                                    result.add(MessagePriorityInfo(
                                        messageId = messageId,
                                        priority = priority,
                                        lastAccessedAt = lastAccessedAt,
                                        createdAt = createdAt
                                    ))
                                }
                            }
                        }
                    } while (cursor != "0")

                    result
                }
            } catch (e: Exception) {
                logger.error("获取Redis所有消息优先级失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun getThread(threadId: String): MemoryThread? {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val threadKey = threadKey(threadId)
                    val threadJson = jedis.get(threadKey) ?: return@withContext null

                    parseMemoryThread(threadJson)
                }
            } catch (e: Exception) {
                logger.error("从Redis获取线程失败: ${e.message}")
                null
            }
        }
    }

    override suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取线程ID列表（按更新时间倒序）
                    val threadIds = jedis.zrevrange(threadListKey(), offset.toLong(), (offset + limit - 1).toLong())

                    if (threadIds.isEmpty()) {
                        return@withContext emptyList<MemoryThread>()
                    }

                    // 获取线程内容
                    val pipeline = jedis.pipelined()
                    val responses = threadIds.map { threadId ->
                        pipeline.get(threadKey(threadId))
                    }
                    pipeline.sync()

                    // 解析线程
                    responses.mapNotNull { response ->
                        val threadJson = response.get() ?: return@mapNotNull null
                        parseMemoryThread(threadJson)
                    }
                }
            } catch (e: Exception) {
                logger.error("从Redis列出线程失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun updateThread(threadId: String, updates: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    // 获取当前线程
                    val threadKey = threadKey(threadId)
                    val threadJson = jedis.get(threadKey) ?: return@withContext false

                    val thread = parseMemoryThread(threadJson)

                    // 更新线程
                    val updatedThread = thread.copy(
                        title = updates["title"] as? String ?: thread.title,
                        updatedAt = updates["updatedAt"] as? Instant ?: Clock.System.now(),
                        metadata = updates["metadata"] as? Map<String, String> ?: thread.metadata,
                        messageCount = updates["messageCount"] as? Int ?: thread.messageCount
                    )

                    // 序列化更新后的线程
                    val updatedJson = json.encodeToString(
                        mapOf(
                            "id" to updatedThread.id,
                            "title" to (updatedThread.title ?: ""),
                            "createdAt" to updatedThread.createdAt.toString(),
                            "updatedAt" to updatedThread.updatedAt.toString(),
                            "metadata" to (updatedThread.metadata ?: emptyMap<String, String>()),
                            "messageCount" to (updatedThread.messageCount.toString())
                        )
                    )

                    // 保存更新后的线程
                    val pipeline = jedis.pipelined()
                    pipeline.setex(threadKey, expireTime, updatedJson)
                    pipeline.zadd(threadListKey(), updatedThread.updatedAt.toEpochMilliseconds().toDouble(), threadId)
                    pipeline.sync()

                    true
                }
            } catch (e: Exception) {
                logger.error("更新Redis线程失败: ${e.message}")
                false
            }
        }
    }

    /**
     * 清理过期数据。
     *
     * @param olderThan 清理早于此时间的数据
     * @return 清理的记录数
     */
    suspend fun cleanup(olderThan: Instant): Int {
        return withContext(Dispatchers.IO) {
            try {
                var count = 0
                jedisPool.resource.use { jedis ->
                    // 清理过期线程
                    val expiredThreads = jedis.zrangeByScore(
                        threadListKey(),
                        0.0,
                        olderThan.toEpochMilliseconds().toDouble()
                    )

                    for (threadId in expiredThreads) {
                        if (deleteThread(threadId)) {
                            count++
                        }
                    }
                }
                count
            } catch (e: Exception) {
                logger.error("清理Redis过期数据失败: ${e.message}")
                0
            }
        }
    }

    // 辅助方法：解析消息JSON
    private fun parseMemoryMessage(messageJson: String): MemoryMessage? {
        return try {
            val data = json.decodeFromString<Map<String, String>>(messageJson)

            MemoryMessage(
                id = data["id"] ?: "",
                threadId = data["threadId"] ?: "",
                message = SimpleMessage(
                    role = ai.kastrax.memory.api.MessageRole.valueOf(data["role"] ?: "USER"),
                    content = data["content"] ?: "",
                    name = data["name"]?.takeIf { it.isNotBlank() },
                    toolCalls = emptyList(), // 简化实现，实际应解析工具调用
                    toolCallId = data["toolCallId"]?.takeIf { it.isNotBlank() }
                ),
                createdAt = data["createdAt"]?.let { Instant.parse(it) } ?: Clock.System.now(),
                priority = data["priority"]?.let { MemoryPriority.valueOf(it) },
                lastAccessedAt = data["lastAccessedAt"]?.let { Instant.parse(it) },
                accessCount = data["accessCount"]?.toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            logger.error("Failed to parse memory message: ${e.message}")
            null
        }
    }

    // 辅助方法：解析线程JSON
    private fun parseMemoryThread(threadJson: String): MemoryThread {
        val data = json.decodeFromString<Map<String, String>>(threadJson)

        return MemoryThread(
            id = data["id"] ?: "",
            title = data["title"]?.takeIf { it.isNotBlank() },
            createdAt = data["createdAt"]?.let { Instant.parse(it) } ?: Clock.System.now(),
            updatedAt = data["updatedAt"]?.let { Instant.parse(it) } ?: Clock.System.now(),
            metadata = data["metadata"]?.let { json.decodeFromString<Map<String, String>>(it) },
            messageCount = data["messageCount"]?.toIntOrNull() ?: 0
        )
    }
}
