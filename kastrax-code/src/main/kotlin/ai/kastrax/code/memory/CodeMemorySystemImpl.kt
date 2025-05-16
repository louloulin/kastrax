package ai.kastrax.code.memory

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement

import ai.kastrax.memory.api.MemoryId
import ai.kastrax.memory.api.MemoryStore
import ai.kastrax.memory.api.MemoryType
import ai.kastrax.memory.api.query.MemoryQuery
import ai.kastrax.memory.api.query.MemoryQueryType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/**
 * 代码记忆系统实现
 *
 * 基于 kastrax-memory-api 实现代码记忆系统
 */
@Service(Service.Level.PROJECT)
class CodeMemorySystemImpl(
    private val project: Project,
    private val config: CodeMemorySystemConfig = CodeMemorySystemConfig()
) : CodeMemorySystem, KastraXCodeBase(component = "CODE_MEMORY_SYSTEM") {

    override val logger = KotlinLogging.logger {}

    // 记忆存储
    private val memoryStore: MemoryStore by lazy {
        // 从项目服务中获取，如果没有则创建一个新的
        project.getService(MemoryStore::class.java) ?: throw IllegalStateException("MemoryStore not found")
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

            // 创建记忆ID
            val memoryId = MemoryId(
                id = UUID.randomUUID().toString(),
                type = MemoryType.CONVERSATION,
                namespace = "conversation:$conversationId"
            )

            // 存储记忆
            memoryStore.storeMemory(memoryId, memory)

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

            // 创建查询
            val query = MemoryQuery(
                type = MemoryQueryType.NAMESPACE,
                value = "conversation:$conversationId",
                limit = limit
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(query)

            return@withContext memories
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
            logger.info { "存储代码上下文记忆: ${context.query}" }

            // 为每个上下文元素创建记忆
            for (element in context.elements) {
                // 创建记忆ID
                val memoryId = MemoryId(
                    id = UUID.randomUUID().toString(),
                    type = MemoryType.CODE_CONTEXT,
                    namespace = "code_context"
                )

                // 创建记忆
                val memory = SimpleMemory(
                    content = element.content,
                    metadata = mapOf(
                        "query" to context.query,
                        "element_id" to element.id,
                        "element_name" to element.name,
                        "element_type" to element.type,
                        "file_path" to (element.filePath?.toString() ?: ""),
                        "location" to (element.location?.toString() ?: ""),
                        "score" to element.score.toString()
                    ),
                    timestamp = Instant.now()
                )

                // 存储记忆
                memoryStore.storeMemory(memoryId, memory)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "存储代码上下文记忆时出错: ${context.query}" }
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

            // 创建查询
            val memoryQuery = MemoryQuery(
                type = MemoryQueryType.SEMANTIC,
                value = query,
                limit = limit
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(memoryQuery)

            // 转换为上下文元素
            val elements = memories.mapNotNull { memory ->
                try {
                    val metadata = memory.metadata
                    val score = metadata["score"]?.toString()?.toDoubleOrNull() ?: 0.0

                    // 检查分数是否达到最小分数
                    if (score < minScore) {
                        return@mapNotNull null
                    }

                    // 创建上下文元素
                    ContextElement(
                        id = metadata["element_id"]?.toString() ?: UUID.randomUUID().toString(),
                        name = metadata["element_name"]?.toString() ?: "",
                        type = metadata["element_type"]?.toString() ?: "",
                        content = memory.content,
                        filePath = metadata["file_path"]?.toString()?.let { java.nio.file.Paths.get(it) },
                        location = metadata["location"]?.toString()?.let { parseLocation(it) },
                        score = score
                    )
                } catch (e: Exception) {
                    logger.error(e) { "转换记忆为上下文元素时出错" }
                    null
                }
            }

            return@withContext elements
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

            // 创建记忆ID
            val memoryId = MemoryId(
                id = UUID.randomUUID().toString(),
                type = MemoryType.PROJECT,
                namespace = "project:$projectId"
            )

            // 存储记忆
            memoryStore.storeMemory(memoryId, memory)

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

            // 创建查询
            val query = MemoryQuery(
                type = MemoryQueryType.NAMESPACE,
                value = "project:$projectId",
                limit = limit
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(query)

            // 如果指定了记忆类型，则过滤
            return@withContext if (memoryType != null) {
                memories.filter { it.metadata["type"] == memoryType.name }
            } else {
                memories
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

            // 创建记忆ID
            val memoryId = MemoryId(
                id = "preference:$userId:$key",
                type = MemoryType.PREFERENCE,
                namespace = "user:$userId"
            )

            // 创建记忆
            val memory = SimpleMemory(
                content = value,
                metadata = mapOf(
                    "user_id" to userId,
                    "key" to key
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            memoryStore.storeMemory(memoryId, memory)

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

            // 创建记忆ID
            val memoryId = MemoryId(
                id = "preference:$userId:$key",
                type = MemoryType.PREFERENCE,
                namespace = "user:$userId"
            )

            // 获取记忆
            val memory = memoryStore.getMemory(memoryId)

            return@withContext memory?.content
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

            // 创建查询
            val query = MemoryQuery(
                type = MemoryQueryType.NAMESPACE,
                value = "conversation:$conversationId"
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(query)

            // 删除记忆
            for (memory in memories) {
                memoryStore.deleteMemory(memory.id)
            }

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

            // 创建查询
            val query = MemoryQuery(
                type = MemoryQueryType.NAMESPACE,
                value = "code_context"
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(query)

            // 删除记忆
            for (memory in memories) {
                memoryStore.deleteMemory(memory.id)
            }

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

            // 创建查询
            val query = MemoryQuery(
                type = MemoryQueryType.NAMESPACE,
                value = "project:$projectId"
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(query)

            // 删除记忆
            for (memory in memories) {
                memoryStore.deleteMemory(memory.id)
            }

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

            // 创建查询
            val query = MemoryQuery(
                type = MemoryQueryType.NAMESPACE,
                value = "user:$userId"
            )

            // 查询记忆
            val memories = memoryStore.queryMemories(query)

            // 删除记忆
            for (memory in memories) {
                memoryStore.deleteMemory(memory.id)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "清除用户偏好记忆时出错: $userId" }
            return@withContext false
        }
    }

    /**
     * 关闭记忆系统
     */
    override suspend fun close() {
        try {
            logger.info { "关闭记忆系统" }

            // 关闭记忆存储
            memoryStore.close()
        } catch (e: Exception) {
            logger.error(e) { "关闭记忆系统时出错" }
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
            logger.error(e) { "解析位置字符串时出错: $locationString" }
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
