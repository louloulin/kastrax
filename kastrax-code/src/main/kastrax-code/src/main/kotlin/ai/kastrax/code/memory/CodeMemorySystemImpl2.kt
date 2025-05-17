package ai.kastrax.code.memory

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.ContextLevel
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    // 创建内存系统
    private val memorySystem: Memory = memory {
        storage(inMemoryStorage())
        lastMessages(config.maxMemoryItems)
        semanticRecall(true)
    }

    // 线程映射
    private val threadMap = ConcurrentHashMap<String, String>()

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

            // 获取或创建线程ID
            val threadId = threadMap.computeIfAbsent("conversation:$conversationId") { UUID.randomUUID().toString() }

            // 创建消息
            val message = MemoryMessage(
                role = MessageRole.USER,
                content = memory.content,
                metadata = memory.metadata
            )

            // 添加消息
            memorySystem.addMessage(threadId, message)

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "存储对话记忆时出错: $conversationId" }
            return@withContext false
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
            val threadId = threadMap["conversation:$conversationId"] ?: return@withContext emptyList()

            // 获取消息
            val messages = memorySystem.getMessages(threadId, limit)

            // 转换为简单记忆
            return@withContext messages.map { message ->
                SimpleMemory(
                    id = UUID.randomUUID().toString(),
                    content = message.content,
                    timestamp = message.timestamp.toKotlinInstant(),
                    metadata = message.metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "检索对话记忆时出错: $conversationId" }
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
            logger.info { "存储代码上下文记忆: ${context.elements.size} 个元素" }

            // 获取或创建线程ID
            val threadId = threadMap.computeIfAbsent("code_context") { UUID.randomUUID().toString() }

            // 存储每个上下文元素
            for (element in context.elements) {
                // 创建消息
                val message = MemoryMessage(
                    role = MessageRole.SYSTEM,
                    content = element.content,
                    metadata = mapOf(
                        "type" to "CODE_CONTEXT",
                        "path" to element.path,
                        "level" to element.level.toString(),
                        "relevance" to element.relevance.toString(),
                        "location" to element.location?.let { locationToString(it) } ?: ""
                    )
                )

                // 添加消息
                memorySystem.addMessage(threadId, message)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "存储代码上下文记忆时出错" }
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
            val threadId = threadMap["code_context"] ?: return@withContext emptyList()

            // 获取相似消息
            val messages = memorySystem.getSimilarMessages(threadId, query, limit, minScore)

            // 转换为上下文元素
            return@withContext messages.map { message ->
                val metadata = message.metadata
                ContextElement(
                    content = message.content,
                    path = metadata["path"]?.toString() ?: "",
                    level = parseContextLevel(metadata["level"]?.toString() ?: ""),
                    relevance = metadata["relevance"]?.toString()?.toFloatOrNull() ?: 0.0f,
                    location = metadata["location"]?.toString()?.let { parseModelLocation(it) }
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "检索代码上下文记忆时出错: $query" }
            return@withContext emptyList()
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

            // 获取或创建线程ID
            val threadId = threadMap.computeIfAbsent("project:$projectId") { UUID.randomUUID().toString() }

            // 创建消息
            val message = MemoryMessage(
                role = MessageRole.SYSTEM,
                content = memory.content,
                metadata = memory.metadata
            )

            // 添加消息
            memorySystem.addMessage(threadId, message)

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "存储项目记忆时出错: $projectId" }
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

            // 过滤记忆类型
            val filteredMessages = if (memoryType != null) {
                messages.filter { message ->
                    message.metadata["type"]?.toString() == memoryType.toString()
                }
            } else {
                messages
            }

            // 转换为简单记忆
            return@withContext filteredMessages.map { message ->
                SimpleMemory(
                    id = UUID.randomUUID().toString(),
                    content = message.content,
                    timestamp = message.timestamp.toKotlinInstant(),
                    metadata = message.metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "检索项目记忆时出错: $projectId" }
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

            // 获取或创建线程ID
            val threadId = threadMap.computeIfAbsent("user:$userId") { UUID.randomUUID().toString() }

            // 创建消息
            val message = MemoryMessage(
                role = MessageRole.SYSTEM,
                content = value,
                metadata = mapOf(
                    "type" to "USER_PREFERENCE",
                    "key" to key
                )
            )

            // 添加消息
            memorySystem.addMessage(threadId, message)

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "存储用户偏好记忆时出错: $userId, $key" }
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
            val threadId = threadMap["user:$userId"] ?: return@withContext null

            // 获取消息
            val messages = memorySystem.getMessages(threadId, 100)

            // 查找匹配的消息
            val message = messages.find { message ->
                message.metadata["type"] == "USER_PREFERENCE" && message.metadata["key"] == key
            }

            return@withContext message?.content
        } catch (e: Exception) {
            logger.error(e) { "检索用户偏好记忆时出错: $userId, $key" }
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
            val threadId = threadMap["conversation:$conversationId"] ?: return@withContext true

            // 清除线程
            memorySystem.clearThread(threadId)

            // 移除线程映射
            threadMap.remove("conversation:$conversationId")

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "清除对话记忆时出错: $conversationId" }
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

            // 获取线程ID
            val threadId = threadMap["code_context"] ?: return@withContext true

            // 清除线程
            memorySystem.clearThread(threadId)

            // 移除线程映射
            threadMap.remove("code_context")

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "清除代码上下文记忆时出错" }
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

            // 清除线程
            memorySystem.clearThread(threadId)

            // 移除线程映射
            threadMap.remove("project:$projectId")

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "清除项目记忆时出错: $projectId" }
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
            val threadId = threadMap["user:$userId"] ?: return@withContext true

            // 清除线程
            memorySystem.clearThread(threadId)

            // 移除线程映射
            threadMap.remove("user:$userId")

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "清除用户偏好记忆时出错: $userId" }
            return@withContext false
        }
    }

    /**
     * 关闭记忆系统
     */
    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            logger.info { "关闭记忆系统" }

            // 清空线程映射
            threadMap.clear()
        } catch (e: Exception) {
            logger.error(e) { "关闭记忆系统时出错" }
        }
    }

    /**
     * 位置转字符串
     *
     * @param location 位置
     * @return 字符串
     */
    private fun locationToString(location: Location): String {
        return "${location.filePath}:${location.line}:${location.column}:${location.endLine}:${location.endColumn}"
    }

    /**
     * 解析模型位置字符串
     *
     * @param locationString 位置字符串
     * @return 代码位置
     */
    private fun parseModelLocation(locationString: String): Location? {
        try {
            val parts = locationString.split(":")
            if (parts.size < 5) return null

            return Location(
                filePath = Paths.get(parts[0]),
                line = parts[1].toIntOrNull() ?: 0,
                column = parts[2].toIntOrNull() ?: 0,
                endLine = parts[3].toIntOrNull() ?: 0,
                endColumn = parts[4].toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            logger.error(e) { "解析代码位置字符串时出错: $locationString" }
            return null
        }
    }

    /**
     * 解析上下文级别
     *
     * @param levelString 级别字符串
     * @return 上下文级别
     */
    private fun parseContextLevel(levelString: String): ContextLevel {
        return try {
            ContextLevel.valueOf(levelString)
        } catch (e: Exception) {
            ContextLevel.FILE
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
