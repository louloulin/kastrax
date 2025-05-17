package ai.kastrax.code.memory

import ai.kastrax.code.common.KastraXCodeBase

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
 * 长期记忆
 *
 * 存储编码风格、常用模式和跨项目知识
 */
@Service
class LongTermMemory : KastraXCodeBase(component = "LONG_TERM_MEMORY") {

    // 使用父类的logger

    // 代码记忆系统
    private var memorySystem: CodeMemorySystem? = null

    // 用户ID
    private val userId: String by lazy { System.getProperty("user.name") ?: "unknown" }

    /**
     * 初始化
     *
     * @param project 项目
     */
    fun initialize(project: Project) {
        memorySystem = CodeMemorySystemImpl.getInstance(project)
    }

    /**
     * 存储编码风格
     *
     * @param language 编程语言
     * @param style 编码风格
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeCodingStyle(
        language: String,
        style: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储编码风格: $language" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 创建记忆
            val memory = SimpleMemory(
                content = style,
                metadata = metadata + mapOf(
                    "language" to language,
                    "user_id" to userId,
                    "type" to "CODING_STYLE"
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeUserPreferenceMemory(userId, "coding_style:$language", style)
        } catch (e: Exception) {
            logger.error(e) { "存储编码风格时出错: $language" }
            return@withContext false
        }
    }

    /**
     * 检索编码风格
     *
     * @param language 编程语言
     * @return 编码风格
     */
    suspend fun retrieveCodingStyle(language: String): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索编码风格: $language" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 检索记忆
            return@withContext memorySystem.retrieveUserPreferenceMemory(userId, "coding_style:$language")
        } catch (e: Exception) {
            logger.error(e) { "检索编码风格时出错: $language" }
            return@withContext null
        }
    }

    /**
     * 存储常用模式
     *
     * @param name 模式名称
     * @param pattern 模式内容
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeCommonPattern(
        name: String,
        pattern: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储常用模式: $name" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 创建记忆
            val memory = SimpleMemory(
                content = pattern,
                metadata = metadata + mapOf(
                    "name" to name,
                    "user_id" to userId,
                    "type" to "COMMON_PATTERN"
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeUserPreferenceMemory(userId, "common_pattern:$name", pattern)
        } catch (e: Exception) {
            logger.error(e) { "存储常用模式时出错: $name" }
            return@withContext false
        }
    }

    /**
     * 检索常用模式
     *
     * @param name 模式名称
     * @return 模式内容
     */
    suspend fun retrieveCommonPattern(name: String): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索常用模式: $name" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 检索记忆
            return@withContext memorySystem.retrieveUserPreferenceMemory(userId, "common_pattern:$name")
        } catch (e: Exception) {
            logger.error(e) { "检索常用模式时出错: $name" }
            return@withContext null
        }
    }

    /**
     * 存储跨项目知识
     *
     * @param key 键
     * @param content 内容
     * @param metadata 元数据
     * @return 是否成功存储
     */
    suspend fun storeCrossProjectKnowledge(
        key: String,
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "存储跨项目知识: $key" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 创建记忆
            val memory = SimpleMemory(
                content = content,
                metadata = metadata + mapOf(
                    "key" to key,
                    "user_id" to userId,
                    "type" to "CROSS_PROJECT_KNOWLEDGE"
                ),
                timestamp = Instant.now()
            )

            // 存储记忆
            return@withContext memorySystem.storeUserPreferenceMemory(userId, "cross_project:$key", content)
        } catch (e: Exception) {
            logger.error(e) { "存储跨项目知识时出错: $key" }
            return@withContext false
        }
    }

    /**
     * 检索跨项目知识
     *
     * @param key 键
     * @return 内容
     */
    suspend fun retrieveCrossProjectKnowledge(key: String): String? = withContext(Dispatchers.IO) {
        try {
            logger.info { "检索跨项目知识: $key" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 检索记忆
            return@withContext memorySystem.retrieveUserPreferenceMemory(userId, "cross_project:$key")
        } catch (e: Exception) {
            logger.error(e) { "检索跨项目知识时出错: $key" }
            return@withContext null
        }
    }

    /**
     * 清除用户记忆
     *
     * @return 是否成功清除
     */
    suspend fun clearUserMemory(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "清除用户记忆" }

            // 检查记忆系统是否已初始化
            val memorySystem = this@LongTermMemory.memorySystem
                ?: throw IllegalStateException("Memory system not initialized")

            // 清除用户记忆
            return@withContext memorySystem.clearUserPreferenceMemory(userId)
        } catch (e: Exception) {
            logger.error(e) { "清除用户记忆时出错" }
            return@withContext false
        }
    }

    companion object {
        /**
         * 获取长期记忆实例
         *
         * @return 长期记忆实例
         */
        fun getInstance(): LongTermMemory {
            return com.intellij.openapi.application.ApplicationManager.getApplication().getService(LongTermMemory::class.java)
        }
    }
}

/**
 * 长期记忆配置
 *
 * @property maxCodingStyles 最大编码风格数量
 * @property maxCommonPatterns 最大常用模式数量
 * @property maxKnowledgeItems 最大知识项数量
 * @property memoryExpirationDays 记忆过期天数
 */
data class LongTermMemoryConfig(
    val maxCodingStyles: Int = 10,
    val maxCommonPatterns: Int = 100,
    val maxKnowledgeItems: Int = 1000,
    val memoryExpirationDays: Int = 365
)
