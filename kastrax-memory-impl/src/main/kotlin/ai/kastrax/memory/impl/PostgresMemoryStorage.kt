package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryThread
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource

/**
 * PostgreSQL实现的内存存储。
 *
 * @property dataSource 数据源
 * @property tablePrefix 表前缀
 */
class PostgresMemoryStorage(
    private val dataSource: DataSource,
    private val tablePrefix: String = "memory_"
) : MemoryStorage, KastraXBase(component = "MEMORY_STORAGE", name = "postgres") {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 表名
    private val threadsTable = "${tablePrefix}threads"
    private val messagesTable = "${tablePrefix}messages"

    init {
        // 初始化数据库
        initDatabase()
    }

    /**
     * 初始化数据库。
     */
    private fun initDatabase() {
        try {
            dataSource.connection.use { connection ->
                // 创建线程表
                connection.createStatement().use { statement ->
                    statement.execute("""
                        CREATE TABLE IF NOT EXISTS $threadsTable (
                            id VARCHAR(255) PRIMARY KEY,
                            title VARCHAR(255),
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            message_count INTEGER DEFAULT 0
                        )
                    """)
                }

                // 创建消息表
                connection.createStatement().use { statement ->
                    statement.execute("""
                        CREATE TABLE IF NOT EXISTS $messagesTable (
                            id VARCHAR(255) PRIMARY KEY,
                            thread_id VARCHAR(255) NOT NULL,
                            role VARCHAR(50) NOT NULL,
                            content TEXT NOT NULL,
                            name VARCHAR(255),
                            tool_calls JSONB,
                            tool_call_id VARCHAR(255),
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            FOREIGN KEY (thread_id) REFERENCES $threadsTable (id) ON DELETE CASCADE
                        )
                    """)
                }

                // 创建索引
                connection.createStatement().use { statement ->
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_${messagesTable}_thread_id ON $messagesTable (thread_id)")
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_${messagesTable}_created_at ON $messagesTable (created_at)")
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_${threadsTable}_updated_at ON $threadsTable (updated_at)")
                }

                // 创建全文搜索索引（如果PostgreSQL版本支持）
                try {
                    connection.createStatement().use { statement ->
                        // 添加全文搜索列
                        statement.execute("ALTER TABLE $messagesTable ADD COLUMN IF NOT EXISTS content_tsv TSVECTOR")

                        // 创建更新触发器
                        statement.execute("""
                            CREATE OR REPLACE FUNCTION ${tablePrefix}messages_trigger() RETURNS trigger AS $$
                            BEGIN
                                NEW.content_tsv := to_tsvector('english', NEW.content);
                                RETURN NEW;
                            END
                            $$ LANGUAGE plpgsql;
                        """)

                        // 创建触发器
                        statement.execute("""
                            DROP TRIGGER IF EXISTS ${tablePrefix}messages_update ON $messagesTable;
                            CREATE TRIGGER ${tablePrefix}messages_update
                            BEFORE INSERT OR UPDATE ON $messagesTable
                            FOR EACH ROW EXECUTE FUNCTION ${tablePrefix}messages_trigger();
                        """)

                        // 创建GIN索引
                        statement.execute("CREATE INDEX IF NOT EXISTS idx_${messagesTable}_content_tsv ON $messagesTable USING GIN(content_tsv)")
                    }
                } catch (e: Exception) {
                    logger.warn("创建全文搜索索引失败，可能是PostgreSQL版本不支持: ${e.message}")
                    // 继续执行，不中断初始化
                }
            }
        } catch (e: Exception) {
            logger.error("初始化PostgreSQL数据库失败: ${e.message}")
            throw IllegalStateException("初始化PostgreSQL数据库失败", e)
        }
    }

    override suspend fun saveMessage(message: MemoryMessage): String {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("""
                        INSERT INTO $messagesTable (id, thread_id, role, content, name, tool_calls, tool_call_id, created_at)
                        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                    """).use { statement ->
                        statement.setString(1, message.id)
                        statement.setString(2, message.threadId)
                        statement.setString(3, message.message.role.toString())
                        statement.setString(4, message.message.content)
                        statement.setString(5, message.message.name)
                        statement.setString(6, if (message.message.toolCalls.isNotEmpty()) {
                            json.encodeToString(message.message.toolCalls)
                        } else null)
                        statement.setString(7, message.message.toolCallId)
                        statement.setTimestamp(8, Timestamp.from(message.createdAt.toJavaInstant()))
                        statement.executeUpdate()
                    }

                    message.id
                }
            } catch (e: Exception) {
                logger.error("保存消息到PostgreSQL失败: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("""
                        SELECT id, thread_id, role, content, name, tool_calls, tool_call_id, created_at
                        FROM $messagesTable
                        WHERE thread_id = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                    """).use { statement ->
                        statement.setString(1, threadId)
                        statement.setInt(2, limit)
                        statement.executeQuery().use { resultSet ->
                            val messages = mutableListOf<MemoryMessage>()
                            while (resultSet.next()) {
                                messages.add(resultSetToMemoryMessage(resultSet))
                            }
                            messages
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("从PostgreSQL获取消息失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    // 尝试使用全文搜索
                    try {
                        connection.prepareStatement("""
                            SELECT id, thread_id, role, content, name, tool_calls, tool_call_id, created_at
                            FROM $messagesTable
                            WHERE thread_id = ? AND content_tsv @@ plainto_tsquery('english', ?)
                            ORDER BY ts_rank(content_tsv, plainto_tsquery('english', ?)) DESC
                            LIMIT ?
                        """).use { statement ->
                            statement.setString(1, threadId)
                            statement.setString(2, query)
                            statement.setString(3, query)
                            statement.setInt(4, limit)
                            statement.executeQuery().use { resultSet ->
                                val messages = mutableListOf<MemoryMessage>()
                                while (resultSet.next()) {
                                    messages.add(resultSetToMemoryMessage(resultSet))
                                }
                                return messages
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("全文搜索失败，回退到LIKE搜索: ${e.message}")
                        // 回退到LIKE搜索
                    }

                    // 使用LIKE搜索（回退方案）
                    connection.prepareStatement("""
                        SELECT id, thread_id, role, content, name, tool_calls, tool_call_id, created_at
                        FROM $messagesTable
                        WHERE thread_id = ? AND content ILIKE ?
                        ORDER BY created_at DESC
                        LIMIT ?
                    """).use { statement ->
                        statement.setString(1, threadId)
                        statement.setString(2, "%$query%")
                        statement.setInt(3, limit)
                        statement.executeQuery().use { resultSet ->
                            val messages = mutableListOf<MemoryMessage>()
                            while (resultSet.next()) {
                                messages.add(resultSetToMemoryMessage(resultSet))
                            }
                            messages
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("在PostgreSQL中搜索消息失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun createThread(thread: MemoryThread): String {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("""
                        INSERT INTO $threadsTable (id, title, created_at, updated_at, message_count)
                        VALUES (?, ?, ?, ?, ?)
                    """).use { statement ->
                        statement.setString(1, thread.id)
                        statement.setString(2, thread.title)
                        statement.setTimestamp(3, Timestamp.from(thread.createdAt.toJavaInstant()))
                        statement.setTimestamp(4, Timestamp.from(thread.updatedAt.toJavaInstant()))
                        statement.setInt(5, thread.messageCount)
                        statement.executeUpdate()
                    }

                    thread.id
                }
            } catch (e: Exception) {
                logger.error("创建PostgreSQL线程失败: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun deleteThread(threadId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("DELETE FROM $threadsTable WHERE id = ?").use { statement ->
                        statement.setString(1, threadId)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (e: Exception) {
                logger.error("删除PostgreSQL线程失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun deleteMessage(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("DELETE FROM $messagesTable WHERE id = ?").use { statement ->
                        statement.setString(1, messageId)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (e: Exception) {
                logger.error("删除PostgreSQL消息失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun updateMessagePriority(messageId: String, priority: MemoryPriority): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    // 使用JSONB更新优先级
                    connection.prepareStatement(
                        "UPDATE $messagesTable SET priority = ?::jsonb WHERE id = ?"
                    ).use { statement ->
                        statement.setString(1, "{\"value\": ${priority.value}, \"name\": \"${priority.name}\"}")
                        statement.setString(2, messageId)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (e: Exception) {
                logger.error("更新PostgreSQL消息优先级失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getMessagePriority(messageId: String): MemoryPriority? {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement(
                        "SELECT priority->>'name' as priority_name FROM $messagesTable WHERE id = ?"
                    ).use { statement ->
                        statement.setString(1, messageId)
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                val priorityName = resultSet.getString("priority_name")
                                if (priorityName != null) {
                                    try {
                                        MemoryPriority.valueOf(priorityName)
                                    } catch (e: IllegalArgumentException) {
                                        null
                                    }
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("获取PostgreSQL消息优先级失败: ${e.message}")
                null
            }
        }
    }

    override suspend fun updateMessageAccess(messageId: String, lastAccessedAt: Instant, accessCount: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement(
                        "UPDATE $messagesTable SET last_accessed_at = ?, access_count = ? WHERE id = ?"
                    ).use { statement ->
                        statement.setTimestamp(1, Timestamp.from(lastAccessedAt.toJavaInstant()))
                        statement.setInt(2, accessCount)
                        statement.setString(3, messageId)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (e: Exception) {
                logger.error("更新PostgreSQL消息访问信息失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getAllMessagesWithPriority(): List<MessagePriorityInfo> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement(
                        """
                        SELECT id, priority->>'name' as priority_name, last_accessed_at, created_at
                        FROM $messagesTable
                        """
                    ).use { statement ->
                        statement.executeQuery().use { resultSet ->
                            val result = mutableListOf<MessagePriorityInfo>()
                            while (resultSet.next()) {
                                val messageId = resultSet.getString("id")
                                val priorityName = resultSet.getString("priority_name")
                                val lastAccessedAt = resultSet.getTimestamp("last_accessed_at")
                                val createdAt = resultSet.getTimestamp("created_at")

                                val priority = if (priorityName != null) {
                                    try {
                                        MemoryPriority.valueOf(priorityName)
                                    } catch (e: IllegalArgumentException) {
                                        null
                                    }
                                } else {
                                    null
                                }

                                result.add(MessagePriorityInfo(
                                    messageId = messageId,
                                    priority = priority,
                                    lastAccessedAt = lastAccessedAt?.let { Instant.fromEpochMilliseconds(it.time) },
                                    createdAt = createdAt?.let { Instant.fromEpochMilliseconds(it.time) }
                                ))
                            }
                            result
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("获取PostgreSQL所有消息优先级失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun getThread(threadId: String): MemoryThread? {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("""
                        SELECT id, title, created_at, updated_at, message_count
                        FROM $threadsTable
                        WHERE id = ?
                    """).use { statement ->
                        statement.setString(1, threadId)
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                resultSetToMemoryThread(resultSet)
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("从PostgreSQL获取线程失败: ${e.message}")
                null
            }
        }
    }

    override suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("""
                        SELECT id, title, created_at, updated_at, message_count
                        FROM $threadsTable
                        ORDER BY updated_at DESC
                        LIMIT ? OFFSET ?
                    """).use { statement ->
                        statement.setInt(1, limit)
                        statement.setInt(2, offset)
                        statement.executeQuery().use { resultSet ->
                            val threads = mutableListOf<MemoryThread>()
                            while (resultSet.next()) {
                                threads.add(resultSetToMemoryThread(resultSet))
                            }
                            threads
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("从PostgreSQL列出线程失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun updateThread(threadId: String, updates: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    // 构建更新SQL
                    val updateFields = mutableListOf<String>()
                    val params = mutableListOf<Any>()

                    updates.forEach { (key, value) ->
                        when (key) {
                            "title" -> {
                                updateFields.add("title = ?")
                                params.add(value)
                            }
                            "updatedAt" -> {
                                updateFields.add("updated_at = ?")
                                params.add(Timestamp.from((value as Instant).toJavaInstant()))
                            }
                            "messageCount" -> {
                                updateFields.add("message_count = ?")
                                params.add(value)
                            }
                        }
                    }

                    if (updateFields.isEmpty()) {
                        return false
                    }

                    val sql = "UPDATE $threadsTable SET ${updateFields.joinToString(", ")} WHERE id = ?"

                    connection.prepareStatement(sql).use { statement ->
                        params.forEachIndexed { index, value ->
                            when (value) {
                                is String -> statement.setString(index + 1, value)
                                is Int -> statement.setInt(index + 1, value)
                                is Timestamp -> statement.setTimestamp(index + 1, value)
                                else -> statement.setObject(index + 1, value)
                            }
                        }
                        statement.setString(params.size + 1, threadId)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (e: Exception) {
                logger.error("更新PostgreSQL线程失败: ${e.message}")
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
                dataSource.connection.use { connection ->
                    // 创建过期时间
                    val now = Clock.System.now()
                    val daysInMillis = days * 24L * 60L * 60L * 1000L
                    val expiredTime = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - daysInMillis)
                    val timestamp = Timestamp.from(expiredTime.toJavaInstant())

                    // 删除过期线程（会级联删除消息）
                    connection.prepareStatement("""
                        DELETE FROM $threadsTable
                        WHERE updated_at < ?
                    """).use { statement ->
                        statement.setTimestamp(1, timestamp)
                        statement.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.error("清理PostgreSQL过期数据失败: ${e.message}")
                0
            }
        }
    }

    // 辅助方法：从ResultSet创建MemoryMessage
    private fun resultSetToMemoryMessage(resultSet: ResultSet): MemoryMessage {
        val id = resultSet.getString("id")
        val threadId = resultSet.getString("thread_id")
        val role = MessageRole.valueOf(resultSet.getString("role"))
        val content = resultSet.getString("content")
        val name = resultSet.getString("name")
        val toolCallsJson = resultSet.getString("tool_calls")
        val toolCallId = resultSet.getString("tool_call_id")
        val createdAt = resultSet.getTimestamp("created_at").toInstant()

        // 解析工具调用
        val toolCalls = if (toolCallsJson != null) {
            try {
                json.decodeFromString<List<SimpleToolCall>>(toolCallsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // 创建消息
        val message = SimpleMessage(
            role = role,
            content = content,
            name = name,
            toolCalls = toolCalls,
            toolCallId = toolCallId
        )

        return MemoryMessage(
            id = id,
            threadId = threadId,
            message = message,
            createdAt = Instant.fromEpochMilliseconds(createdAt.toEpochMilli())
        )
    }

    // 辅助方法：从ResultSet创建MemoryThread
    private fun resultSetToMemoryThread(resultSet: ResultSet): MemoryThread {
        val id = resultSet.getString("id")
        val title = resultSet.getString("title")
        val createdAt = resultSet.getTimestamp("created_at").toInstant()
        val updatedAt = resultSet.getTimestamp("updated_at").toInstant()
        val messageCount = resultSet.getInt("message_count")

        return MemoryThread(
            id = id,
            title = title,
            createdAt = Instant.fromEpochMilliseconds(createdAt.toEpochMilli()),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt.toEpochMilli()),
            messageCount = messageCount
        )
    }
}
