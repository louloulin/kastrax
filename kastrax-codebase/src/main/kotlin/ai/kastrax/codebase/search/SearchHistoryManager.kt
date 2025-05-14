package ai.kastrax.codebase.search

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * 搜索历史管理器配置
 *
 * @property maxHistorySize 最大历史记录大小
 * @property historyFilePath 历史记录文件路径
 * @property enablePersistence 是否启用持久化
 * @property autoSave 是否自动保存
 */
data class SearchHistoryManagerConfig(
    val maxHistorySize: Int = 100,
    val historyFilePath: String = "search_history.json",
    val enablePersistence: Boolean = true,
    val autoSave: Boolean = true
)

/**
 * 搜索历史条目
 *
 * @property id 标识符
 * @property query 查询
 * @property timestamp 时间戳
 * @property resultCount 结果数量
 * @property searchType 搜索类型
 * @property filters 过滤器
 * @property executionTimeMs 执行时间（毫秒）
 */
@Serializable
data class SearchHistoryEntry(
    val id: String,
    val query: String,
    val timestamp: Long,
    val resultCount: Int,
    val searchType: String,
    val filters: Map<String, String> = emptyMap(),
    val executionTimeMs: Long = 0
) {
    /**
     * 格式化时间戳
     *
     * @param pattern 模式
     * @param zoneId 时区ID
     * @return 格式化后的时间戳
     */
    fun formatTimestamp(pattern: String = "yyyy-MM-dd HH:mm:ss", zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern(pattern).withZone(zoneId)
        return formatter.format(instant)
    }
}

/**
 * 搜索历史管理器
 *
 * 管理搜索历史记录
 *
 * @property config 配置
 */
class SearchHistoryManager(
    private val config: SearchHistoryManagerConfig = SearchHistoryManagerConfig()
) {
    private val history = mutableListOf<SearchHistoryEntry>()
    private val mutex = Mutex()
    private val json = Json { prettyPrint = true }

    init {
        if (config.enablePersistence) {
            loadHistory()
        }
    }

    /**
     * 添加历史记录
     *
     * @param query 查询
     * @param resultCount 结果数量
     * @param searchType 搜索类型
     * @param filters 过滤器
     * @param executionTimeMs 执行时间（毫秒）
     * @return 历史记录条目
     */
    suspend fun addHistory(
        query: String,
        resultCount: Int,
        searchType: String,
        filters: Map<String, String> = emptyMap(),
        executionTimeMs: Long = 0
    ): SearchHistoryEntry = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entry = SearchHistoryEntry(
                id = generateId(),
                query = query,
                timestamp = System.currentTimeMillis(),
                resultCount = resultCount,
                searchType = searchType,
                filters = filters,
                executionTimeMs = executionTimeMs
            )

            history.add(0, entry)

            // 限制历史记录大小
            if (history.size > config.maxHistorySize) {
                history.removeAt(history.size - 1)
            }

            // 自动保存
            if (config.enablePersistence && config.autoSave) {
                saveHistory()
            }

            return@withLock entry
        }
    }

    /**
     * 获取历史记录
     *
     * @param limit 限制数量
     * @return 历史记录列表
     */
    suspend fun getHistory(limit: Int = config.maxHistorySize): List<SearchHistoryEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            return@withLock history.take(limit)
        }
    }

    /**
     * 清除历史记录
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        mutex.withLock {
            history.clear()

            // 自动保存
            if (config.enablePersistence && config.autoSave) {
                saveHistory()
            }
        }
    }

    /**
     * 删除历史记录
     *
     * @param id 标识符
     * @return 是否成功
     */
    suspend fun deleteHistory(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val removed = history.removeIf { it.id == id }

            // 自动保存
            if (removed && config.enablePersistence && config.autoSave) {
                saveHistory()
            }

            return@withLock removed
        }
    }

    /**
     * 搜索历史记录
     *
     * @param query 查询
     * @param limit 限制数量
     * @return 历史记录列表
     */
    suspend fun searchHistory(query: String, limit: Int = config.maxHistorySize): List<SearchHistoryEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (query.isBlank()) {
                return@withLock history.take(limit)
            }

            val queryLower = query.lowercase()
            return@withLock history.filter { it.query.lowercase().contains(queryLower) }.take(limit)
        }
    }

    /**
     * 保存历史记录
     */
    suspend fun saveHistory() = withContext(Dispatchers.IO) {
        if (!config.enablePersistence) {
            return@withContext
        }

        mutex.withLock {
            try {
                val file = File(config.historyFilePath)
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(history))
                logger.info { "搜索历史记录已保存到 ${file.absolutePath}" }
            } catch (e: Exception) {
                logger.error(e) { "保存搜索历史记录时发生错误: ${e.message}" }
            }
        }
    }

    /**
     * 加载历史记录
     */
    private fun loadHistory() {
        try {
            val file = File(config.historyFilePath)
            if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val loadedHistory = json.decodeFromString<List<SearchHistoryEntry>>(content)
                    history.clear()
                    history.addAll(loadedHistory)
                    logger.info { "从 ${file.absolutePath} 加载了 ${history.size} 条搜索历史记录" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "加载搜索历史记录时发生错误: ${e.message}" }
        }
    }

    /**
     * 生成标识符
     *
     * @return 标识符
     */
    private fun generateId(): String {
        return System.currentTimeMillis().toString() + "-" + (0..9999).random().toString().padStart(4, '0')
    }
}
