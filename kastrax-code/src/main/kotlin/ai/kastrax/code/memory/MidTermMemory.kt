package ai.kastrax.code.memory

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement

// 使用本地的MemoryType
// import ai.kastrax.memory.api.MemoryType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/**
 * 中期记忆
 *
 * 存储项目特定的知识和用户偏好
 */
@Service(Service.Level.PROJECT)
class MidTermMemory(
    private val project: Project,
    private val config: MidTermMemoryConfig = MidTermMemoryConfig()
) : KastraXCodeBase(component = "MID_TERM_MEMORY") {

    // 使用父类的logger

    // 代码记忆系统
    private val memorySystem by lazy { CodeMemorySystemImpl.getInstance(project) }

    // 项目ID
    private val projectId: String by lazy { project.locationHash }

    /**
     * 存储项目知识
     *
     * @param key 键
     * @param content 内容
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeProjectKnowledge(
        key: String,
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储项目知识: $key" }

            // 创建记忆
            val memory = SimpleMemory(
                content = content,
                metadata = metadata + mapOf(
                    "key" to key,
                    "project_id" to projectId,
                    "type" to MemoryType.PROJECT.name
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeProjectMemory(projectId, memory)
        } catch (e: Exception) {
            logger.error(e) { "存储项目知识时出错: $key" }
            return@withContext false
        }
    }

    /**
     * 检索项目知识
     *
     * @param key 键
     * @return 内容
     */
    suspend fun retrieveProjectKnowledge(key: String): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索项目知识: $key" }

            // 检索记忆
            val memories = memorySystem.retrieveProjectMemory(projectId, MemoryType.PROJECT)

            // 查找匹配的记忆
            val memory = memories.find { it.metadata["key"] == key }

            return@withContext memory?.content
        } catch (e: Exception) {
            logger.error(e) { "检索项目知识时出错: $key" }
            return@withContext null
        }
    }

    /**
     * 存储代码模式
     *
     * @param name 模式名称
     * @param pattern 模式内容
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeCodePattern(
        name: String,
        pattern: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储代码模式: $name" }

            // 创建记忆
            val memory = SimpleMemory(
                content = pattern,
                metadata = metadata + mapOf(
                    "name" to name,
                    "project_id" to projectId,
                    "type" to "CODE_PATTERN"
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeProjectMemory(projectId, memory)
        } catch (e: Exception) {
            logger.error(e) { "存储代码模式时出错: $name" }
            return@withContext false
        }
    }

    /**
     * 检索代码模式
     *
     * @param query 查询字符串
     * @param limit 限制数量
     * @return 代码模式列表
     */
    suspend fun retrieveCodePatterns(query: String, limit: Int = 5): List<CodePattern> = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索代码模式: $query" }

            // 检索记忆
            val memories = memorySystem.retrieveProjectMemory(projectId)

            // 过滤代码模式类型的记忆
            val patternMemories = memories.filter { it.metadata["type"] == "CODE_PATTERN" }

            // 转换为代码模式
            val patterns = patternMemories.map { memory ->
                CodePattern(
                    name = memory.metadata["name"]?.toString() ?: "",
                    pattern = memory.content,
                    metadata = memory.metadata
                )
            }

            // 如果查询为空，则返回所有模式
            if (query.isBlank()) {
                return@withContext patterns.take(limit)
            }

            // 根据查询过滤模式
            val filteredPatterns = patterns.filter { pattern ->
                pattern.name.contains(query, ignoreCase = true) ||
                pattern.pattern.contains(query, ignoreCase = true)
            }

            return@withContext filteredPatterns.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "检索代码模式时出错: $query" }
            return@withContext emptyList()
        }
    }

    /**
     * 存储项目结构
     *
     * @param structure 项目结构
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeProjectStructure(
        structure: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储项目结构" }

            // 创建记忆
            val memory = SimpleMemory(
                content = structure,
                metadata = metadata + mapOf(
                    "project_id" to projectId,
                    "type" to "PROJECT_STRUCTURE"
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeProjectMemory(projectId, memory)
        } catch (e: Exception) {
            logger.error(e) { "存储项目结构时出错" }
            return@withContext false
        }
    }

    /**
     * 检索项目结构
     *
     * @return 项目结构
     */
    suspend fun retrieveProjectStructure(): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索项目结构" }

            // 检索记忆
            val memories = memorySystem.retrieveProjectMemory(projectId)

            // 查找项目结构类型的记忆
            val memory = memories.find { it.metadata["type"] == "PROJECT_STRUCTURE" }

            return@withContext memory?.content
        } catch (e: Exception) {
            logger.error(e) { "检索项目结构时出错" }
            return@withContext null
        }
    }

    /**
     * 存储用户偏好
     *
     * @param key 键
     * @param value 值
     * @return 是否成功存储
     */
    suspend fun storeUserPreference(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储用户偏好: $key" }

            // 获取用户ID
            val userId = System.getProperty("user.name") ?: "unknown"

            // 存储用户偏好
            return@withContext memorySystem.storeUserPreferenceMemory(userId, key, value)
        } catch (e: Exception) {
            logger.error(e) { "存储用户偏好时出错: $key" }
            return@withContext false
        }
    }

    /**
     * 检索用户偏好
     *
     * @param key 键
     * @return 值
     */
    suspend fun retrieveUserPreference(key: String): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索用户偏好: $key" }

            // 获取用户ID
            val userId = System.getProperty("user.name") ?: "unknown"

            // 检索用户偏好
            return@withContext memorySystem.retrieveUserPreferenceMemory(userId, key)
        } catch (e: Exception) {
            logger.error(e) { "检索用户偏好时出错: $key" }
            return@withContext null
        }
    }

    /**
     * 清除项目记忆
     *
     * @return 是否成功清除
     */
    suspend fun clearProjectMemory(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除项目记忆" }

            // 清除项目记忆
            return@withContext memorySystem.clearProjectMemory(projectId)
        } catch (e: Exception) {
            logger.error(e) { "清除项目记忆时出错" }
            return@withContext false
        }
    }

    companion object {
        /**
         * 获取项目的中期记忆实例
         *
         * @param project 项目
         * @return 中期记忆实例
         */
        fun getInstance(project: Project): MidTermMemory {
            return project.service<MidTermMemory>()
        }
    }
}

/**
 * 代码模式
 *
 * @property name 模式名称
 * @property pattern 模式内容
 * @property metadata 元数据
 */
data class CodePattern(
    val name: String,
    val pattern: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 中期记忆配置
 *
 * @property maxPatterns 最大模式数量
 * @property maxKnowledgeItems 最大知识项数量
 * @property memoryExpirationDays 记忆过期天数
 */
data class MidTermMemoryConfig(
    val maxPatterns: Int = 100,
    val maxKnowledgeItems: Int = 1000,
    val memoryExpirationDays: Int = 30
)
