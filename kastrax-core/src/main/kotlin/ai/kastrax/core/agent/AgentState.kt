package ai.kastrax.core.agent

import ai.kastrax.core.llm.LlmMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * Agent状态，用于跟踪Agent的运行状态。
 *
 * @property id 状态ID
 * @property threadId 线程ID
 * @property resourceId 资源ID
 * @property status 状态
 * @property metadata 元数据
 * @property variables 变量
 * @property lastUpdated 最后更新时间
 * @property createdAt 创建时间
 */
@Serializable
data class AgentState(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String? = null,
    val resourceId: String? = null,
    val status: AgentStatus = AgentStatus.IDLE,
    val metadata: Map<String, String> = emptyMap(),
    val variables: Map<String, JsonElement> = emptyMap(),
    val lastUpdated: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now()
) {
    /**
     * 创建新状态
     */
    fun withStatus(status: AgentStatus): AgentState {
        return copy(
            status = status,
            lastUpdated = Clock.System.now()
        )
    }

    /**
     * 添加或更新元数据
     */
    fun withMetadata(key: String, value: String): AgentState {
        return copy(
            metadata = metadata + (key to value),
            lastUpdated = Clock.System.now()
        )
    }

    /**
     * 添加或更新变量
     */
    fun withVariable(key: String, value: JsonElement): AgentState {
        return copy(
            variables = variables + (key to value),
            lastUpdated = Clock.System.now()
        )
    }

    /**
     * 设置线程ID
     */
    fun withThreadId(threadId: String): AgentState {
        return copy(
            threadId = threadId,
            lastUpdated = Clock.System.now()
        )
    }

    /**
     * 设置资源ID
     */
    fun withResourceId(resourceId: String): AgentState {
        return copy(
            resourceId = resourceId,
            lastUpdated = Clock.System.now()
        )
    }
}

/**
 * Agent状态枚举
 */
enum class AgentStatus {
    IDLE,        // 空闲状态
    THINKING,    // 思考中
    EXECUTING,   // 执行工具
    RESPONDING,  // 生成响应
    ERROR,       // 错误状态
    PAUSED,      // 暂停状态
    COMPLETED    // 完成状态
}

/**
 * 会话信息
 *
 * @property id 会话ID
 * @property title 会话标题
 * @property resourceId 资源ID
 * @property metadata 元数据
 * @property messageCount 消息数量
 * @property lastUpdated 最后更新时间
 * @property createdAt 创建时间
 */
@Serializable
data class SessionInfo(
    val id: String,
    val title: String? = null,
    val resourceId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val messageCount: Int = 0,
    val lastUpdated: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now()
)

/**
 * 会话消息
 *
 * @property id 消息ID
 * @property sessionId 会话ID
 * @property message 消息内容
 * @property createdAt 创建时间
 */
@Serializable
data class SessionMessage(
    val id: String,
    val sessionId: String,
    @Contextual val message: LlmMessage,
    val createdAt: Instant = Clock.System.now()
)

/**
 * 会话管理接口
 */
interface SessionManager {
    /**
     * 创建会话
     *
     * @param title 会话标题
     * @param resourceId 资源ID
     * @param metadata 元数据
     * @return 会话信息
     */
    suspend fun createSession(
        title: String? = null,
        resourceId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): SessionInfo

    /**
     * 获取会话
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    suspend fun getSession(sessionId: String): SessionInfo?

    /**
     * 更新会话
     *
     * @param sessionId 会话ID
     * @param updates 更新内容
     * @return 更新后的会话信息
     */
    suspend fun updateSession(
        sessionId: String,
        updates: Map<String, Any>
    ): SessionInfo

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    suspend fun deleteSession(sessionId: String): Boolean

    /**
     * 获取资源的所有会话
     *
     * @param resourceId 资源ID
     * @return 会话列表
     */
    suspend fun getSessionsByResource(resourceId: String): List<SessionInfo>

    /**
     * 保存消息
     *
     * @param message 消息
     * @param sessionId 会话ID
     * @return 消息ID
     */
    suspend fun saveMessage(message: LlmMessage, sessionId: String): String

    /**
     * 获取会话消息
     *
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    suspend fun getMessages(sessionId: String, limit: Int = 100): List<SessionMessage>
}

/**
 * 状态管理接口
 */
interface StateManager {
    /**
     * 保存状态
     *
     * @param state 状态
     * @return 保存的状态
     */
    suspend fun saveState(state: AgentState): AgentState

    /**
     * 获取状态
     *
     * @param stateId 状态ID
     * @return 状态
     */
    suspend fun getState(stateId: String): AgentState?

    /**
     * 获取线程的最新状态
     *
     * @param threadId 线程ID
     * @return 状态
     */
    suspend fun getStateByThread(threadId: String): AgentState?

    /**
     * 获取资源的所有状态
     *
     * @param resourceId 资源ID
     * @return 状态列表
     */
    suspend fun getStatesByResource(resourceId: String): List<AgentState>

    /**
     * 删除状态
     *
     * @param stateId 状态ID
     * @return 是否成功
     */
    suspend fun deleteState(stateId: String): Boolean
}
