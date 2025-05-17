package ai.kastrax.code.memory

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.Location
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.impl.memory
import ai.kastrax.memory.impl.inMemoryStorage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.datetime.Instant
import ai.kastrax.code.memory.toJavaInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import kotlinx.datetime.toKotlinInstant

/**
 * 代码记忆系统实现
 *
 * 使用 kastrax-memory-api 实现代码记忆系统
 */
@Service(Service.Level.PROJECT)
class CodeMemorySystemImpl(
    private val project: Project,
    private val config: CodeMemorySystemConfig = CodeMemorySystemConfig()
) : CodeMemorySystem, KastraXCodeBase(component = "CODE_MEMORY_SYSTEM") {

    // 使用 KastraXCodeBase 的 logger

    // 创建内存系统
    private val memorySystem: Memory = memory {
        storage(inMemoryStorage())
        lastMessages(config.maxMemoryItems)
        semanticRecall(true)
    }

    // 线程映射
    private val threadMap = mutableMapOf<String, String>()

    /**
     * 内存类型枚举
     */
    enum class MemoryType {
        /**
         * 对话内存
         */
        CONVERSATION,

        /**
         * 代码上下文内存
         */
        CODE_CONTEXT,

        /**
         * 项目内存
         */
        PROJECT,

        /**
         * 用户偏好内存
         */
        PREFERENCE
    }

    /**
     * 存储对话记忆
     *
     * @param conversationId 对话ID
     * @param memory 记忆
     * @return 是否成功存储
     */
    override suspend fun storeConversationMemory(conversationId: String, memory: SimpleMemory): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储对话记忆: $conversationId" }

            // 获取或创建线程
            val threadId = getOrCreateThread(conversationId)

            // 创建消息
            val message = memory.toMessage(memory.metadata["role"]?.toString() ?: "user")

            // 存储消息
            memorySystem.saveMessage(message, threadId, metadata = memory.metadata.mapValues { it.value.toString() })

            return@withContext true
        } catch (e: Exception) {
            logger.error { "存储对话记忆时出错: $conversationId" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 获取或创建线程
     *
     * @param id 标识符
     * @return 线程ID
     */
    private suspend fun getOrCreateThread(id: String): String {
        return threadMap.getOrPut(id) {
            memorySystem.createThread(id)
        }
    }

    /**
     * 检索对话记忆
     *
     * @param conversationId 对话ID
     * @param limit 限制数量
     * @return 记忆列表
     */
    override suspend fun retrieveConversationMemory(conversationId: String, limit: Int): List<SimpleMemory> = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索对话记忆: $conversationId" }

            // 获取线程ID
            val threadId = threadMap[conversationId] ?: return@withContext emptyList()

            // 获取消息
            val messages = memorySystem.getMessages(threadId, limit)

            // 转换为 SimpleMemory
            return@withContext messages.map { memoryMessage ->
                SimpleMemory(
                    content = memoryMessage.message.content,
                    metadata = memoryMessage.metadata?.mapValues { it.value } ?: mapOf(
                        "role" to memoryMessage.message.role.name.lowercase()
                    ),
                    timestamp = memoryMessage.createdAt.toJavaInstant()
                )
            }
        } catch (e: Exception) {
            logger.error { "检索对话记忆时出错: $conversationId" }
            logger.error(e.toString())
            return@withContext emptyList()
        }
    }

    /**
     * 存储代码上下文记忆
     *
     * @param context 上下文
     * @return 是否成功存储
     */
    override suspend fun storeCodeContextMemory(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储代码上下文记忆: ${context.query}" }

            // 获取或创建线程
            val threadId = getOrCreateThread("code_context:${context.query}")

            // 为每个上下文元素创建记忆
            for (element in context.elements) {
                // 创建消息
                val message = ai.kastrax.memory.impl.SimpleMessage(
                    role = MessageRole.SYSTEM,
                    content = element.content
                )

                // 创建元数据
                val metadata = mapOf(
                    "query" to context.query,
                    "element_id" to element.element.id,
                    "element_name" to element.element.name,
                    "element_type" to element.element.type.toString(),
                    "file_path" to (element.element.filePath?.toString() ?: ""),
                    "location" to (element.element.location?.toString() ?: ""),
                    "score" to element.score.toString(),
                    "type" to "CODE_CONTEXT"
                )

                // 存储消息
                memorySystem.saveMessage(message, threadId, metadata = metadata)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error { "存储代码上下文记忆时出错: ${context.query}" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 检索代码上下文记忆
     *
     * @param query 查询字符串
     * @param limit 限制数量
     * @param minScore 最小相似度分数
     * @return 上下文元素列表
     */
    override suspend fun retrieveCodeContextMemory(query: String, limit: Int, minScore: Double): List<ContextElement> = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索代码上下文记忆: $query" }

            // 获取线程ID
            val threadId = threadMap["code_context:$query"] ?: return@withContext emptyList()

            // 语义搜索消息
            val messages = memorySystem.searchMessages(query, threadId, limit)

            // 转换为上下文元素
            val elements = messages.mapNotNull { memoryMessage ->
                try {
                    val metadata = memoryMessage.metadata ?: return@mapNotNull null
                    val score = metadata["score"]?.toString()?.toDoubleOrNull() ?: 0.0

                    // 检查分数是否达到最小分数
                    if (score < minScore) {
                        return@mapNotNull null
                    }

                    // 创建代码元素
                    val codeElement = ai.kastrax.code.model.CodeElement(
                        id = metadata["element_id"]?.toString() ?: UUID.randomUUID().toString(),
                        name = metadata["element_name"]?.toString() ?: "",
                        type = ai.kastrax.code.model.CodeElementType.valueOf(metadata["element_type"]?.toString() ?: "UNKNOWN"),
                        content = memoryMessage.message.content,
                        filePath = metadata["file_path"]?.toString()?.let { Paths.get(it) },
                        location = metadata["location"]?.toString()?.let { parseLocation(it) }
                    )

                    // 创建上下文元素
                    ContextElement(
                        element = codeElement,
                        level = ai.kastrax.code.model.ContextLevel.PRIMARY,
                        relevance = ai.kastrax.code.model.ContextRelevance.HIGH,
                        content = memoryMessage.message.content,
                        score = score
                    )
                } catch (e: Exception) {
                    logger.error { "转换记忆为上下文元素时出错" }
                    logger.error(e.toString())
                    null
                }
            }

            return@withContext elements
        } catch (e: Exception) {
            logger.error { "检索代码上下文记忆时出错: $query" }
            logger.error(e.toString())
            return@withContext emptyList()
        }
    }

    /**
     * 解析位置字符串
     *
     * @param locationString 位置字符串
     * @return 位置
     */
    private fun parseLocation(locationString: String): ai.kastrax.code.model.Location? {
        try {
            // 解析位置字符串，格式为 "line:column-endLine:endColumn" 或 "line:column"
            val parts = locationString.split("-")

            if (parts.size == 1) {
                // 格式为 "line:column"
                val (line, column) = parts[0].split(":").map { it.toInt() }
                return ai.kastrax.code.model.Location(line, column)
            } else if (parts.size == 2) {
                // 格式为 "line:column-endLine:endColumn"
                val (startPart, endPart) = parts
                val (line, column) = startPart.split(":").map { it.toInt() }
                val (endLine, endColumn) = endPart.split(":").map { it.toInt() }
                return ai.kastrax.code.model.Location(line, column, endLine, endColumn)
            }

            return null
        } catch (e: Exception) {
            logger.error { "解析位置字符串时出错: $locationString" }
            logger.error(e.toString())
            return null
        }
    }

    /**
     * 存储项目记忆
     *
     * @param projectId 项目ID
     * @param memory 记忆
     * @return 是否成功存储
     */
    override suspend fun storeProjectMemory(projectId: String, memory: SimpleMemory): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储项目记忆: $projectId" }

            // 获取或创建线程
            val threadId = getOrCreateThread("project:$projectId")

            // 创建消息
            val message = memory.toMessage(memory.metadata["role"]?.toString() ?: "system")

            // 添加项目类型元数据
            val metadata = memory.metadata.toMutableMap()
            metadata["type"] = "PROJECT"

            // 存储消息
            memorySystem.saveMessage(message, threadId, metadata = metadata.mapValues { it.value.toString() })

            return@withContext true
        } catch (e: Exception) {
            logger.error { "存储项目记忆时出错: $projectId" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 检索项目记忆
     *
     * @param projectId 项目ID
     * @param memoryType 记忆类型
     * @param limit 限制数量
     * @return 记忆列表
     */
    override suspend fun retrieveProjectMemory(projectId: String, memoryType: MemoryType?, limit: Int): List<SimpleMemory> = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索项目记忆: $projectId" }

            // 获取线程ID
            val threadId = threadMap["project:$projectId"] ?: return@withContext emptyList()

            // 获取消息
            val messages = memorySystem.getMessages(threadId, limit)

            // 如果指定了记忆类型，则过滤
            val filteredMessages = if (memoryType != null) {
                messages.filter { it.metadata?.get("type") == memoryType.name }
            } else {
                messages
            }

            // 转换为 SimpleMemory
            return@withContext filteredMessages.map { memoryMessage ->
                SimpleMemory(
                    content = memoryMessage.message.content,
                    metadata = memoryMessage.metadata?.mapValues { it.value } ?: mapOf(
                        "role" to memoryMessage.message.role.name.lowercase()
                    ),
                    timestamp = memoryMessage.createdAt.toJavaInstant()
                )
            }
        } catch (e: Exception) {
            logger.error { "检索项目记忆时出错: $projectId" }
            logger.error(e.toString())
            return@withContext emptyList()
        }
    }

    /**
     * 存储用户偏好记忆
     *
     * @param userId 用户ID
     * @param key 键
     * @param value 值
     * @return 是否成功存储
     */
    override suspend fun storeUserPreferenceMemory(userId: String, key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储用户偏好记忆: $userId, $key" }

            // 获取或创建线程
            val threadId = getOrCreateThread("preference:$userId")

            // 创建消息
            val message = ai.kastrax.memory.impl.SimpleMessage(
                role = MessageRole.SYSTEM,
                content = value
            )

            // 创建元数据
            val metadata = mapOf(
                "user_id" to userId,
                "key" to key,
                "type" to "PREFERENCE"
            )

            // 存储消息
            memorySystem.saveMessage(message, threadId, metadata = metadata)

            return@withContext true
        } catch (e: Exception) {
            logger.error { "存储用户偏好记忆时出错: $userId, $key" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 检索用户偏好记忆
     *
     * @param userId 用户ID
     * @param key 键
     * @return 值
     */
    override suspend fun retrieveUserPreferenceMemory(userId: String, key: String): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索用户偏好记忆: $userId, $key" }

            // 获取线程ID
            val threadId = threadMap["preference:$userId"] ?: return@withContext null

            // 获取消息
            val messages = memorySystem.getMessages(threadId, 100)

            // 查找匹配的消息
            val message = messages.find { it.metadata?.get("key") == key }

            return@withContext message?.message?.content
        } catch (e: Exception) {
            logger.error { "检索用户偏好记忆时出错: $userId, $key" }
            logger.error(e.toString())
            return@withContext null
        }
    }

    /**
     * 清除对话记忆
     *
     * @param conversationId 对话ID
     * @return 是否成功清除
     */
    override suspend fun clearConversationMemory(conversationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除对话记忆: $conversationId" }

            // 获取线程ID
            val threadId = threadMap[conversationId] ?: return@withContext true

            // 删除线程
            memorySystem.deleteThread(threadId)

            // 移除线程映射
            threadMap.remove(conversationId)

            return@withContext true
        } catch (e: Exception) {
            logger.error { "清除对话记忆时出错: $conversationId" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 清除代码上下文记忆
     *
     * @return 是否成功清除
     */
    override suspend fun clearCodeContextMemory(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除代码上下文记忆" }

            // 找到所有代码上下文线程
            val codeContextThreads = threadMap.entries.filter { it.key.startsWith("code_context:") }

            // 删除所有代码上下文线程
            for ((key, threadId) in codeContextThreads) {
                memorySystem.deleteThread(threadId)
                threadMap.remove(key)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error { "清除代码上下文记忆时出错" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 清除项目记忆
     *
     * @param projectId 项目ID
     * @return 是否成功清除
     */
    override suspend fun clearProjectMemory(projectId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除项目记忆: $projectId" }

            // 获取线程ID
            val threadId = threadMap["project:$projectId"] ?: return@withContext true

            // 删除线程
            memorySystem.deleteThread(threadId)

            // 移除线程映射
            threadMap.remove("project:$projectId")

            return@withContext true
        } catch (e: Exception) {
            logger.error { "清除项目记忆时出错: $projectId" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 清除用户偏好记忆
     *
     * @param userId 用户ID
     * @return 是否成功清除
     */
    override suspend fun clearUserPreferenceMemory(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除用户偏好记忆: $userId" }

            // 获取线程ID
            val threadId = threadMap["preference:$userId"] ?: return@withContext true

            // 删除线程
            memorySystem.deleteThread(threadId)

            // 移除线程映射
            threadMap.remove("preference:$userId")

            return@withContext true
        } catch (e: Exception) {
            logger.error { "清除用户偏好记忆时出错: $userId" }
            logger.error(e.toString())
            return@withContext false
        }
    }

    /**
     * 关闭记忆系统
     */
    override suspend fun close() {
        try {
            logger.info { "关闭记忆系统" }

            // 清空线程映射
            threadMap.clear()

            // 关闭记忆系统
            memorySystem.close()
        } catch (e: Exception) {
            logger.error { "关闭记忆系统时出错" }
            logger.error(e.toString())
        }
    }

    /**
     * 简单消息类
     */
    private data class CodeMessage(
        val role: MessageRole,
        val content: String
    )

    // 使用 ai.kastrax.memory.impl.SimpleMessage 类型别名
    private typealias SimpleMessage = ai.kastrax.memory.impl.SimpleMessage

    /**
     * 将 SimpleMemory 转换为消息
     *
     * @param role 角色
     * @return 消息
     */
    private fun SimpleMemory.toMessage(role: String): CodeMessage {
        val messageRole = when (role.lowercase()) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            else -> MessageRole.SYSTEM
        }
        return CodeMessage(messageRole, this.content)
    }

    /**
     * 解析位置字符串
     *
     * @param locationString 位置字符串
     * @return 位置
     */
    private fun parseLocation(locationString: String): ai.kastrax.code.model.Location? {
        try {
            // 解析位置字符串，格式为 "line:column-endLine:endColumn" 或 "line:column"
            val parts = locationString.split("-")

            if (parts.size == 1) {
                // 格式为 "line:column"
                val (line, column) = parts[0].split(":").map { it.toInt() }
                return ai.kastrax.code.model.Location(line, column)
            } else if (parts.size == 2) {
                // 格式为 "line:column-endLine:endColumn"
                val (startPart, endPart) = parts
                val (line, column) = startPart.split(":").map { it.toInt() }
                val (endLine, endColumn) = endPart.split(":").map { it.toInt() }
                return ai.kastrax.code.model.Location(line, column, endLine, endColumn)
            }

            return null
        } catch (e: Exception) {
            logger.error { "解析位置字符串时出错: $locationString" }
            logger.error(e.toString())
            return null
        }
    }

    companion object {
        /**
         * 获取项目的代码记忆系统实例
         *
         * @param project 项目
         * @return 代码记忆系统实例
         */
        fun getInstance(project: Project): CodeMemorySystem {
            return project.service<CodeMemorySystemImpl>()
        }
    }
}

/**
 * 代码记忆系统配置
 *
 * @property enableConversationMemory 是否启用对话记忆
 * @property enableCodeContextMemory 是否启用代码上下文记忆
 * @property enableProjectMemory 是否启用项目记忆
 * @property enableUserPreferenceMemory 是否启用用户偏好记忆
 * @property maxMemoryItems 最大记忆项数量
 * @property memoryExpirationDays 记忆过期天数
 */
data class CodeMemorySystemConfig(
    val enableConversationMemory: Boolean = true,
    val enableCodeContextMemory: Boolean = true,
    val enableProjectMemory: Boolean = true,
    val enableUserPreferenceMemory: Boolean = true,
    val maxMemoryItems: Int = 1000,
    val memoryExpirationDays: Int = 30
)
