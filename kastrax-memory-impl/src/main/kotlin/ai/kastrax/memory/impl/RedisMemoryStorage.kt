package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryThread
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
                    // 序列化消息
                    val messageJson = json.encodeToString(
                        mapOf(
                            "id" to message.id,
                            "threadId" to message.threadId,
                            "role" to message.message.role.toString(),
                            "content" to message.message.content,
                            "name" to message.message.name,
                            "toolCalls" to message.message.toolCalls,
                            "toolCallId" to message.message.toolCallId,
                            "createdAt" to message.createdAt.toString()
                        )
                    )

                    // 保存消息
                    val messageKey = messageKey(message.id)
                    jedis.setex(messageKey, expireTime, messageJson)

                    // 添加到线程消息列表
                    val threadMessagesKey = threadMessagesKey(message.threadId)
                    jedis.zadd(threadMessagesKey, message.createdAt.toEpochMilliseconds().toDouble(), message.id)
                    jedis.expire(threadMessagesKey, expireTime)

                    message.id
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
                        return@withContext emptyList<MemoryMessage>()
                    }

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
                            "title" to thread.title,
                            "createdAt" to thread.createdAt.toString(),
                            "updatedAt" to thread.updatedAt.toString(),
                            "messageCount" to thread.messageCount
                        )
                    )

                    // 保存线程
                    val threadKey = threadKey(thread.id)
                    jedis.setex(threadKey, expireTime, threadJson)

                    // 添加到线程列表
                    jedis.zadd(threadListKey(), thread.updatedAt.toEpochMilliseconds().toDouble(), thread.id)
                    jedis.expire(threadListKey(), expireTime)

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
                    // 获取线程消息
                    val threadMessagesKey = threadMessagesKey(threadId)
                    val messageIds = jedis.zrange(threadMessagesKey, 0L, -1L)

                    // 删除所有消息
                    val pipeline = jedis.pipelined()
                    for (messageId in messageIds) {
                        pipeline.del(messageKey(messageId))
                    }

                    // 删除线程和线程消息列表
                    pipeline.del(threadKey(threadId))
                    pipeline.del(threadMessagesKey)
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
                    val messageJson = jedis.get(messageKey(messageId)) ?: return@withContext false
                    val message = parseMemoryMessage(messageJson) ?: return@withContext false

                    // 删除消息
                    val pipeline = jedis.pipelined()
                    pipeline.del(messageKey(messageId))
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

                    // 获取所有线程
                    val threadIds = jedis.zrange(threadListKey(), 0, -1)

                    // 遍历每个线程的消息
                    for (threadId in threadIds) {
                        val threadMessagesKey = threadMessagesKey(threadId)
                        val messageIds = jedis.zrange(threadMessagesKey, 0, -1)

                        // 获取每个消息的信息
                        for (messageId in messageIds) {
                            val messageJson = jedis.get(messageKey(messageId)) ?: continue
                            val data = json.decodeFromString<Map<String, String>>(messageJson)

                            // 解析优先级
                            val priority = data["priority"]?.let {
                                try {
                                    MemoryPriority.valueOf(it)
                                } catch (e: IllegalArgumentException) {
                                    null
                                }
                            }

                            // 解析时间
                            val lastAccessedAt = data["lastAccessedAt"]?.let {
                                try {
                                    Instant.parse(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            val createdAt = data["createdAt"]?.let {
                                try {
                                    Instant.parse(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            result.add(MessagePriorityInfo(
                                messageId = messageId,
                                priority = priority,
                                lastAccessedAt = lastAccessedAt,
                                createdAt = createdAt
                            ))
                        }
                    }

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
                        messageCount = updates["messageCount"] as? Int ?: thread.messageCount
                    )

                    // 序列化更新后的线程
                    val updatedThreadJson = json.encodeToString(
                        mapOf(
                            "id" to updatedThread.id,
                            "title" to updatedThread.title,
                            "createdAt" to updatedThread.createdAt.toString(),
                            "updatedAt" to updatedThread.updatedAt.toString(),
                            "messageCount" to updatedThread.messageCount
                        )
                    )

                    // 保存更新后的线程
                    jedis.setex(threadKey, expireTime, updatedThreadJson)

                    // 更新线程列表中的分数（更新时间）
                    jedis.zadd(threadListKey(), updatedThread.updatedAt.toEpochMilliseconds().toDouble(), threadId)

                    true
                }
            } catch (e: Exception) {
                logger.error("更新Redis线程失败: ${e.message}")
                false
            }
        }
    }

    /**
     * 清理过期的内存数据。
     *
     * @param days 过期天数
     * @return 清理的记录数
     */
    suspend fun cleanupExpiredData(days: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    var count = 0
                    val expireTime = Clock.System.now().toEpochMilliseconds() - TimeUnit.DAYS.toMillis(days.toLong())

                    // 清理过期线程
                    val expiredThreads = jedis.zrangeByScore(threadListKey(), 0.0, expireTime.toDouble())
                    if (expiredThreads.isNotEmpty()) {
                        expiredThreads.forEach { threadId ->
                            if (deleteThread(threadId)) {
                                count++
                            }
                        }
                    }

                    count
                }
            } catch (e: Exception) {
                logger.error("清理Redis过期数据失败: ${e.message}")
                0
            }
        }
    }

    // 辅助方法：解析内存消息
    private fun parseMemoryMessage(json: String): MemoryMessage? {
        return try {
            val data = this.json.decodeFromString<Map<String, String>>(json)

            // 创建简单消息
            val message = SimpleMessage(
                role = ai.kastrax.memory.api.MessageRole.valueOf(data["role"] ?: "USER"),
                content = data["content"] ?: "",
                name = data["name"],
                toolCalls = emptyList(), // 简化实现，实际应解析工具调用
                toolCallId = data["toolCallId"]
            )

            MemoryMessage(
                id = data["id"] ?: UUID.randomUUID().toString(),
                threadId = data["threadId"] ?: "",
                message = message,
                createdAt = data["createdAt"]?.let { Instant.parse(it) } ?: Clock.System.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to parse memory message: ${e.message}")
            null
        }
    }

    // 辅助方法：解析内存线程
    private fun parseMemoryThread(json: String): MemoryThread {
        val data = this.json.decodeFromString<Map<String, String>>(json)

        return MemoryThread(
            id = data["id"] ?: UUID.randomUUID().toString(),
            title = data["title"],
            createdAt = data["createdAt"]?.let { Instant.parse(it) } ?: Clock.System.now(),
            updatedAt = data["updatedAt"]?.let { Instant.parse(it) } ?: Clock.System.now(),
            messageCount = data["messageCount"]?.toIntOrNull() ?: 0
        )
    }
}
