package ai.kastrax.core.agent

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 内存中的会话管理器实现
 */
class InMemorySessionManager : SessionManager, KastraXBase(component = "SESSION_MANAGER", name = "in-memory") {
    private val sessionsMutex = Mutex()
    private val messagesMutex = Mutex()

    private val sessions = mutableMapOf<String, SessionInfo>()
    private val messages = mutableMapOf<String, MutableList<SessionMessage>>()

    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo {
        val sessionId = UUID.randomUUID().toString()
        val session = SessionInfo(
            id = sessionId,
            title = title,
            resourceId = resourceId,
            metadata = metadata,
            messageCount = 0,
            lastUpdated = Clock.System.now(),
            createdAt = Clock.System.now()
        )

        sessionsMutex.withLock {
            sessions[sessionId] = session
        }

        logger.debug("创建会话: $sessionId")
        return session
    }

    override suspend fun getSession(sessionId: String): SessionInfo? {
        return sessionsMutex.withLock {
            sessions[sessionId]
        }
    }

    override suspend fun updateSession(
        sessionId: String,
        updates: Map<String, Any>
    ): SessionInfo {
        return sessionsMutex.withLock {
            val session = sessions[sessionId] ?: throw IllegalArgumentException("会话不存在: $sessionId")

            // 创建更新后的会话
            val updatedSession = session.copy(
                title = updates["title"] as? String ?: session.title,
                resourceId = updates["resourceId"] as? String ?: session.resourceId,
                metadata = (updates["metadata"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    if (k is String && v is String) k to v else null
                }?.toMap() ?: session.metadata,
                messageCount = updates["messageCount"] as? Int ?: session.messageCount,
                lastUpdated = Clock.System.now()
            )

            sessions[sessionId] = updatedSession
            updatedSession
        }
    }

    override suspend fun deleteSession(sessionId: String): Boolean {
        sessionsMutex.withLock {
            sessions.remove(sessionId)
        }

        messagesMutex.withLock {
            messages.remove(sessionId)
        }

        logger.debug("删除会话: $sessionId")
        return true
    }

    override suspend fun getSessionsByResource(resourceId: String): List<SessionInfo> {
        return sessionsMutex.withLock {
            sessions.values.filter { it.resourceId == resourceId }
        }
    }

    override suspend fun saveMessage(message: LlmMessage, sessionId: String): String {
        val messageId = UUID.randomUUID().toString()

        // 检查会话是否存在
        val session = sessionsMutex.withLock {
            sessions[sessionId] ?: throw IllegalArgumentException("会话不存在: $sessionId")
        }

        // 创建会话消息
        val sessionMessage = SessionMessage(
            id = messageId,
            sessionId = sessionId,
            message = message,
            createdAt = Clock.System.now()
        )

        // 保存消息
        messagesMutex.withLock {
            val sessionMessages = messages.getOrPut(sessionId) { mutableListOf() }
            sessionMessages.add(sessionMessage)
            // 按时间排序
            sessionMessages.sortByDescending { it.createdAt }
        }

        // 更新会话消息计数
        sessionsMutex.withLock {
            sessions[sessionId] = session.copy(
                messageCount = session.messageCount + 1,
                lastUpdated = Clock.System.now()
            )
        }

        logger.debug("保存消息: $messageId 到会话: $sessionId")
        return messageId
    }

    override suspend fun getMessages(sessionId: String, limit: Int): List<SessionMessage> {
        return messagesMutex.withLock {
            messages[sessionId]?.take(limit) ?: emptyList()
        }
    }
}
