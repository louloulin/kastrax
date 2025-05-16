package ai.kastrax.code.memory

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.memory.api.MessageRole
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/**
 * 短期记忆
 *
 * 存储当前会话的上下文和交互
 */
@Service(Service.Level.PROJECT)
class ShortTermMemory(
    private val project: Project,
    private val config: ShortTermMemoryConfig = ShortTermMemoryConfig()
) : KastraXCodeBase(component = "SHORT_TERM_MEMORY") {

    // 使用父类的logger

    // 代码记忆系统
    private val memorySystem by lazy { CodeMemorySystemImpl.getInstance(project) }

    // 当前会话ID
    private var sessionId: String = UUID.randomUUID().toString()

    /**
     * 存储对话消息
     *
     * @param role 角色
     * @param content 内容
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeMessage(
        role: String,
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储对话消息: $role" }

            // 创建记忆
            val memory = SimpleMemory(
                content = content,
                metadata = metadata + mapOf(
                    "role" to role,
                    "session_id" to sessionId,
                    "type" to "message"
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeConversationMemory(sessionId, memory)
        } catch (e: Exception) {
            logger.error(e) { "存储对话消息时出错: $role" }
            return@withContext false
        }
    }

    /**
     * 存储上下文
     *
     * @param context 上下文
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeContext(
        context: Context,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储上下文: ${context.query}" }

            // 创建记忆
            val memory = SimpleMemory(
                content = context.query,
                metadata = metadata + mapOf(
                    "session_id" to sessionId,
                    "type" to "context",
                    "elements_count" to context.elements.size
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            val stored = memorySystem.storeConversationMemory(sessionId, memory)

            // 存储上下文元素
            val contextStored = memorySystem.storeCodeContextMemory(context)

            return@withContext stored && contextStored
        } catch (e: Exception) {
            logger.error(e) { "存储上下文时出错: ${context.query}" }
            return@withContext false
        }
    }

    /**
     * 检索对话历史
     *
     * @param limit 限制数量
     * @return 记忆列表
     */
    suspend fun retrieveConversationHistory(limit: Int = 10): List<SimpleMemory> = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索对话历史" }

            // 检索记忆
            val memories = memorySystem.retrieveConversationMemory(sessionId, limit)

            // 过滤消息类型的记忆
            return@withContext memories.filter { it.metadata["type"] == "message" }
        } catch (e: Exception) {
            logger.error(e) { "检索对话历史时出错" }
            return@withContext emptyList()
        }
    }

    /**
     * 检索最近的上下文
     *
     * @param limit 限制数量
     * @return 上下文列表
     */
    suspend fun retrieveRecentContexts(limit: Int = 5): List<Context> = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索最近的上下文" }

            // 检索记忆
            val memories = memorySystem.retrieveConversationMemory(sessionId, limit * 2)

            // 过滤上下文类型的记忆
            val contextMemories = memories.filter { it.metadata["type"] == "context" }
                .take(limit)

            // 转换为上下文
            val contexts = mutableListOf<Context>()

            for (memory in contextMemories) {
                val query = memory.content
                val elements = memorySystem.retrieveCodeContextMemory(query, 10, 0.0)

                contexts.add(Context(
                    elements = elements,
                    query = query
                ))
            }

            return@withContext contexts
        } catch (e: Exception) {
            logger.error(e) { "检索最近的上下文时出错" }
            return@withContext emptyList()
        }
    }

    /**
     * 清除当前会话
     *
     * @return 是否成功清除
     */
    suspend fun clearSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除当前会话" }

            // 清除对话记忆
            val cleared = memorySystem.clearConversationMemory(sessionId)

            // 创建新会话ID
            sessionId = UUID.randomUUID().toString()

            return@withContext cleared
        } catch (e: Exception) {
            logger.error(e) { "清除当前会话时出错" }
            return@withContext false
        }
    }

    /**
     * 获取当前会话ID
     *
     * @return 会话ID
     */
    fun getSessionId(): String {
        return sessionId
    }

    /**
     * 设置当前会话ID
     *
     * @param id 会话ID
     */
    fun setSessionId(id: String) {
        sessionId = id
    }

    companion object {
        /**
         * 获取项目的短期记忆实例
         *
         * @param project 项目
         * @return 短期记忆实例
         */
        fun getInstance(project: Project): ShortTermMemory {
            return project.service<ShortTermMemory>()
        }
    }
}

/**
 * 短期记忆配置
 *
 * @property maxMessages 最大消息数量
 * @property maxContexts 最大上下文数量
 * @property messageExpirationMinutes 消息过期分钟数
 */
data class ShortTermMemoryConfig(
    val maxMessages: Int = 100,
    val maxContexts: Int = 10,
    val messageExpirationMinutes: Int = 60
)
