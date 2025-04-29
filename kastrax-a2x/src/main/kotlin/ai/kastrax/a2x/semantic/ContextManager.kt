package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 上下文管理器，负责管理交互上下文
 */
class ContextManager {
    /**
     * 上下文映射
     */
    private val contexts = ConcurrentHashMap<String, Context>()

    /**
     * 会话映射
     */
    private val sessions = ConcurrentHashMap<String, Session>()

    /**
     * 创建上下文
     */
    fun createContext(name: String, description: String, type: String): Context {
        val context = Context(
            id = "ctx-${UUID.randomUUID()}",
            name = name,
            description = description,
            type = type,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        contexts[context.id] = context
        return context
    }

    /**
     * 获取上下文
     */
    fun getContext(contextId: String): Context? {
        return contexts[contextId]
    }

    /**
     * 更新上下文
     */
    fun updateContext(contextId: String, update: (Context) -> Context): Context? {
        val context = contexts[contextId] ?: return null
        val updatedContext = update(context).copy(updatedAt = System.currentTimeMillis())
        contexts[contextId] = updatedContext
        return updatedContext
    }

    /**
     * 更新上下文数据
     */
    fun updateContextData(contextId: String, data: JsonObject): Context? {
        return updateContext(contextId) { context ->
            context.copy(data = data)
        }
    }

    /**
     * 删除上下文
     */
    fun deleteContext(contextId: String) {
        contexts.remove(contextId)

        // 删除关联的会话
        sessions.values.filter { it.contextId == contextId }.forEach { session ->
            sessions.remove(session.id)
        }
    }

    /**
     * 获取所有上下文
     */
    fun getAllContexts(): List<Context> {
        return contexts.values.toList()
    }

    /**
     * 创建会话
     */
    fun createSession(contextId: String, entityId: String): Session? {
        val context = contexts[contextId] ?: return null

        val session = Session(
            id = "sess-${UUID.randomUUID()}",
            contextId = contextId,
            entityId = entityId,
            state = emptyMap(),
            history = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        sessions[session.id] = session
        return session
    }

    /**
     * 获取会话
     */
    fun getSession(sessionId: String): Session? {
        return sessions[sessionId]
    }

    /**
     * 更新会话
     */
    fun updateSession(sessionId: String, update: (Session) -> Session): Session? {
        val session = sessions[sessionId] ?: return null
        val updatedSession = update(session).copy(updatedAt = System.currentTimeMillis())
        sessions[sessionId] = updatedSession
        return updatedSession
    }

    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    /**
     * 获取所有会话
     */
    fun getAllSessions(): List<Session> {
        return sessions.values.toList()
    }

    /**
     * 获取上下文的会话
     */
    fun getContextSessions(contextId: String): List<Session> {
        return sessions.values.filter { it.contextId == contextId }
    }

    /**
     * 获取实体的会话
     */
    fun getEntitySessions(entityId: String): List<Session> {
        return sessions.values.filter { it.entityId == entityId }
    }

    /**
     * 添加会话历史
     */
    fun addSessionHistory(sessionId: String, entry: HistoryEntry): Session? {
        return updateSession(sessionId) { session ->
            val history = session.history.toMutableList()
            history.add(entry)
            session.copy(history = history)
        }
    }

    /**
     * 更新会话状态
     */
    fun updateSessionState(sessionId: String, key: String, value: JsonElement): Session? {
        return updateSession(sessionId) { session ->
            val state = session.state.toMutableMap()
            state[key] = value
            session.copy(state = state)
        }
    }

    /**
     * 获取会话状态
     */
    fun getSessionState(sessionId: String, key: String): JsonElement? {
        val session = sessions[sessionId] ?: return null
        return session.state[key]
    }

    /**
     * 合并上下文
     */
    fun mergeContexts(contextIds: List<String>, name: String, description: String): Context? {
        if (contextIds.isEmpty()) return null

        val contexts = contextIds.mapNotNull { getContext(it) }
        if (contexts.isEmpty()) return null

        // 创建新上下文
        val mergedContext = createContext(
            name = name,
            description = description,
            type = "merged"
        )

        // 合并上下文数据
        val mergedData = buildJsonObject {
            contexts.forEach { context ->
                context.data.forEach { (key, value) ->
                    put(key, value)
                }
            }
        }

        // 更新合并后的上下文
        return updateContext(mergedContext.id) { context ->
            context.copy(data = mergedData)
        }
    }

    /**
     * 导出上下文
     */
    fun exportContext(contextId: String): JsonObject {
        val context = contexts[contextId] ?: return JsonObject(emptyMap())
        val sessions = getContextSessions(contextId)

        return buildJsonObject {
            put("context", kotlinx.serialization.json.Json.encodeToJsonElement(Context.serializer(), context))
            put("sessions", kotlinx.serialization.json.Json.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(Session.serializer()),
                sessions
            ))
        }
    }

    /**
     * 导入上下文
     */
    fun importContext(data: JsonObject): Context? {
        val contextElement = data["context"] ?: return null
        val context = kotlinx.serialization.json.Json.decodeFromJsonElement(Context.serializer(), contextElement)

        // 导入上下文
        contexts[context.id] = context

        // 导入会话
        val sessionsElement = data["sessions"] ?: return context
        val importedSessions = kotlinx.serialization.json.Json.decodeFromJsonElement(
            kotlinx.serialization.builtins.ListSerializer(Session.serializer()),
            sessionsElement
        )

        importedSessions.forEach { session ->
            sessions[session.id] = session
        }

        return context
    }
}

// Context class is now defined in Context.kt

/**
 * 会话
 */
@Serializable
data class Session(
    /**
     * 会话 ID
     */
    val id: String,

    /**
     * 上下文 ID
     */
    val contextId: String,

    /**
     * 实体 ID
     */
    val entityId: String,

    /**
     * 会话状态
     */
    val state: Map<String, JsonElement>,

    /**
     * 会话历史
     */
    val history: List<HistoryEntry>,

    /**
     * 会话元数据
     */
    val metadata: Map<String, String> = emptyMap(),

    /**
     * 创建时间
     */
    val createdAt: Long,

    /**
     * 更新时间
     */
    val updatedAt: Long
)

/**
 * 历史条目
 */
@Serializable
data class HistoryEntry(
    /**
     * 条目 ID
     */
    val id: String,

    /**
     * 条目类型
     */
    val type: String,

    /**
     * 条目内容
     */
    val content: JsonElement,

    /**
     * 条目元数据
     */
    val metadata: Map<String, String> = emptyMap(),

    /**
     * 创建时间
     */
    val timestamp: Long = System.currentTimeMillis()
)
