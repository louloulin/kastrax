package ai.kastrax.core.agent

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

/**
 * SQLite实现的会话管理器
 *
 * @property dbPath SQLite数据库文件路径
 * @property json JSON序列化器
 */
class SQLiteSessionManager(
    private val dbPath: String,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false }
) : SessionManager, KastraXBase(component = "SESSION_MANAGER", name = "sqlite") {

    init {
        // 初始化数据库
        initDatabase()
    }

    /**
     * 初始化数据库
     */
    private fun initDatabase() {
        var connection: Connection? = null
        var statement: Statement? = null

        try {
            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC")

            // 创建连接
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            statement = connection.createStatement()

            // 创建会话表
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id TEXT PRIMARY KEY,
                    title TEXT,
                    resource_id TEXT,
                    metadata TEXT NOT NULL,
                    message_count INTEGER NOT NULL,
                    last_updated TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """)

            // 创建消息表
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS session_messages (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    message TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
                )
            """)

            // 创建索引
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_resource_id ON sessions(resource_id)")
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_session_id ON session_messages(session_id)")

            logger.info { "SQLite会话管理器初始化完成: $dbPath" }
        } catch (e: Exception) {
            logger.error(e) { "初始化SQLite数据库失败: ${e.message}" }
            throw e
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    /**
     * 获取数据库连接
     */
    private fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }

    /**
     * 从ResultSet中解析SessionInfo
     */
    private fun parseSessionInfo(rs: ResultSet): SessionInfo {
        val id = rs.getString("id")
        val title = rs.getString("title")
        val resourceId = rs.getString("resource_id")
        val metadata = json.decodeFromString<Map<String, String>>(rs.getString("metadata"))
        val messageCount = rs.getInt("message_count")
        val lastUpdated = Instant.parse(rs.getString("last_updated"))
        val createdAt = Instant.parse(rs.getString("created_at"))

        return SessionInfo(
            id = id,
            title = title,
            resourceId = resourceId,
            metadata = metadata,
            messageCount = messageCount,
            lastUpdated = lastUpdated,
            createdAt = createdAt
        )
    }

    /**
     * 从ResultSet中解析SessionMessage
     */
    private fun parseSessionMessage(rs: ResultSet): SessionMessage {
        val id = rs.getString("id")
        val sessionId = rs.getString("session_id")
        val messageJson = rs.getString("message")

        // 手动解析LlmMessage，避免序列化问题
        val messageObj = json.parseToJsonElement(messageJson).jsonObject
        val role = LlmMessageRole.valueOf(messageObj["role"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> LlmMessageRole.USER.name
            }
        } ?: LlmMessageRole.USER.name)

        val content = messageObj["content"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> ""
            }
        } ?: ""

        val name = messageObj["name"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> null
            }
        }

        val toolCallId = messageObj["toolCallId"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> null
            }
        }

        val toolCalls = messageObj["toolCalls"]?.let {
            when (it) {
                is kotlinx.serialization.json.JsonArray -> {
                    it.mapNotNull { toolCallElement ->
                        val toolCallObj = toolCallElement.jsonObject
                        val toolId = toolCallObj["id"]?.let { idElement ->
                            when (idElement) {
                                is JsonPrimitive -> idElement.content
                                else -> ""
                            }
                        } ?: ""

                        val toolName = toolCallObj["name"]?.let { nameElement ->
                            when (nameElement) {
                                is JsonPrimitive -> nameElement.content
                                else -> ""
                            }
                        } ?: ""

                        val toolArgs = toolCallObj["arguments"]?.let { argsElement ->
                            when (argsElement) {
                                is JsonPrimitive -> argsElement.content
                                else -> ""
                            }
                        } ?: ""

                        LlmToolCall(id = toolId, name = toolName, arguments = toolArgs)
                    }
                }
                else -> emptyList()
            }
        } ?: emptyList()

        val message = LlmMessage(
            role = role,
            content = content,
            name = name,
            toolCalls = toolCalls,
            toolCallId = toolCallId
        )

        val createdAt = Instant.parse(rs.getString("created_at"))

        return SessionMessage(
            id = id,
            sessionId = sessionId,
            message = message,
            createdAt = createdAt
        )
    }

    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val now = Clock.System.now()
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("""
                INSERT INTO sessions (
                    id, title, resource_id, metadata, message_count, last_updated, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, sessionId)
                stmt.setString(2, title)
                stmt.setString(3, resourceId)
                stmt.setString(4, json.encodeToString(metadata))
                stmt.setInt(5, 0)
                stmt.setString(6, now.toString())
                stmt.setString(7, now.toString())
                stmt.executeUpdate()
            }

            logger.debug { "创建会话: $sessionId" }
            return@withContext SessionInfo(
                id = sessionId,
                title = title,
                resourceId = resourceId,
                metadata = metadata,
                messageCount = 0,
                lastUpdated = now,
                createdAt = now
            )
        } catch (e: Exception) {
            logger.error(e) { "创建会话失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun getSession(sessionId: String): SessionInfo? = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("SELECT * FROM sessions WHERE id = ?").use { stmt ->
                stmt.setString(1, sessionId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    return@withContext parseSessionInfo(rs)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            logger.error(e) { "获取会话失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun updateSession(
        sessionId: String,
        updates: Map<String, Any>
    ): SessionInfo = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            // 获取当前会话
            val currentSession = getSession(sessionId)
                ?: throw IllegalArgumentException("会话不存在: $sessionId")

            // 准备更新字段
            val updateFields = mutableListOf<String>()
            val updateValues = mutableListOf<Any>()

            // 处理更新
            updates.forEach { (key, value) ->
                when (key) {
                    "title" -> {
                        updateFields.add("title = ?")
                        updateValues.add(value as String)
                    }
                    "resourceId" -> {
                        updateFields.add("resource_id = ?")
                        updateValues.add(value as String)
                    }
                    "metadata" -> {
                        @Suppress("UNCHECKED_CAST")
                        val newMetadata = value as Map<String, String>
                        updateFields.add("metadata = ?")
                        updateValues.add(json.encodeToString(newMetadata))
                    }
                    "messageCount" -> {
                        updateFields.add("message_count = ?")
                        updateValues.add(value as Int)
                    }
                }
            }

            // 添加最后更新时间
            updateFields.add("last_updated = ?")
            val now = Clock.System.now()
            updateValues.add(now.toString())

            // 构建SQL
            val sql = "UPDATE sessions SET ${updateFields.joinToString(", ")} WHERE id = ?"

            // 执行更新
            connection = getConnection()
            connection.prepareStatement(sql).use { stmt ->
                updateValues.forEachIndexed { index, value ->
                    when (value) {
                        is String -> stmt.setString(index + 1, value)
                        is Int -> stmt.setInt(index + 1, value)
                        else -> throw IllegalArgumentException("不支持的类型: ${value::class.java}")
                    }
                }
                stmt.setString(updateValues.size + 1, sessionId)
                stmt.executeUpdate()
            }

            // 获取更新后的会话
            return@withContext getSession(sessionId)
                ?: throw IllegalStateException("更新会话后无法检索: $sessionId")
        } catch (e: Exception) {
            logger.error(e) { "更新会话失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun deleteSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("DELETE FROM sessions WHERE id = ?").use { stmt ->
                stmt.setString(1, sessionId)
                val count = stmt.executeUpdate()

                logger.debug { "删除会话: $sessionId, 影响行数: $count" }
                return@withContext count > 0
            }
        } catch (e: Exception) {
            logger.error(e) { "删除会话失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun getSessionsByResource(resourceId: String): List<SessionInfo> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        val sessions = mutableListOf<SessionInfo>()

        try {
            connection = getConnection()
            connection.prepareStatement("SELECT * FROM sessions WHERE resource_id = ? ORDER BY last_updated DESC").use { stmt ->
                stmt.setString(1, resourceId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    sessions.add(parseSessionInfo(rs))
                }
                return@withContext sessions
            }
        } catch (e: Exception) {
            logger.error(e) { "获取资源会话失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun saveMessage(message: LlmMessage, sessionId: String): String = withContext(Dispatchers.IO) {
        val messageId = UUID.randomUUID().toString()
        val now = Clock.System.now()
        var connection: Connection? = null

        try {
            // 检查会话是否存在
            val session = getSession(sessionId)
                ?: throw IllegalArgumentException("会话不存在: $sessionId")

            connection = getConnection()

            // 保存消息
            connection.prepareStatement("""
                INSERT INTO session_messages (
                    id, session_id, message, created_at
                ) VALUES (?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, messageId)
                stmt.setString(2, sessionId)
                // 手动序列化LlmMessage，避免序列化问题
                val messageJson = buildJsonObject {
                    put("role", message.role.name)
                    put("content", message.content)
                    message.name?.let { put("name", it) }
                    message.toolCallId?.let { put("toolCallId", it) }

                    if (message.toolCalls.isNotEmpty()) {
                        putJsonArray("toolCalls") {
                            message.toolCalls.forEach { toolCall ->
                                add(buildJsonObject {
                                    put("id", toolCall.id)
                                    put("name", toolCall.name)
                                    put("arguments", toolCall.arguments)
                                })
                            }
                        }
                    }
                }
                stmt.setString(3, messageJson.toString())
                stmt.setString(4, now.toString())
                stmt.executeUpdate()
            }

            // 更新会话消息计数
            updateSession(
                sessionId,
                mapOf("messageCount" to (session.messageCount + 1))
            )

            logger.debug { "保存消息: $messageId 到会话: $sessionId" }
            return@withContext messageId
        } catch (e: Exception) {
            logger.error(e) { "保存消息失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun getMessages(sessionId: String, limit: Int): List<SessionMessage> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        val messages = mutableListOf<SessionMessage>()

        try {
            connection = getConnection()
            connection.prepareStatement(
                "SELECT * FROM session_messages WHERE session_id = ? ORDER BY created_at DESC LIMIT ?"
            ).use { stmt ->
                stmt.setString(1, sessionId)
                stmt.setInt(2, limit)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    messages.add(parseSessionMessage(rs))
                }
                return@withContext messages
            }
        } catch (e: Exception) {
            logger.error(e) { "获取会话消息失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }
}
