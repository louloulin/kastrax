package ai.kastrax.codebase.search

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.ZoneId

class SearchHistoryManagerTest {

    private lateinit var historyManager: SearchHistoryManager

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // 创建历史管理器
        historyManager = SearchHistoryManager(
            SearchHistoryManagerConfig(
                maxHistorySize = 10,
                historyFilePath = tempDir.resolve("search_history.json").toString(),
                enablePersistence = true,
                autoSave = true
            )
        )
    }

    @Test
    fun testAddHistory() = runBlocking {
        // 添加历史记录
        val entry = historyManager.addHistory(
            query = "test query",
            resultCount = 10,
            searchType = "HYBRID",
            filters = mapOf("type" to "CLASS"),
            executionTimeMs = 100
        )

        // 验证结果
        assertEquals("test query", entry.query)
        assertEquals(10, entry.resultCount)
        assertEquals("HYBRID", entry.searchType)
        assertEquals("CLASS", entry.filters["type"])
        assertEquals(100, entry.executionTimeMs)

        // 获取历史记录
        val history = historyManager.getHistory()

        // 验证历史记录
        assertEquals(1, history.size)
        assertEquals(entry.id, history.first().id)
    }

    @Test
    fun testGetHistory() = runBlocking {
        // 添加多个历史记录
        for (i in 1..5) {
            historyManager.addHistory(
                query = "test query $i",
                resultCount = i * 10,
                searchType = "HYBRID",
                filters = mapOf("type" to "CLASS"),
                executionTimeMs = (i * 100).toLong()
            )
        }

        // 获取历史记录
        val history = historyManager.getHistory(3)

        // 验证历史记录
        assertEquals(3, history.size)
        assertEquals("test query 5", history[0].query)
        assertEquals("test query 4", history[1].query)
        assertEquals("test query 3", history[2].query)
    }

    @Test
    fun testClearHistory() = runBlocking {
        // 添加历史记录
        for (i in 1..5) {
            historyManager.addHistory(
                query = "test query $i",
                resultCount = i * 10,
                searchType = "HYBRID"
            )
        }

        // 清除历史记录
        historyManager.clearHistory()

        // 获取历史记录
        val history = historyManager.getHistory()

        // 验证历史记录
        assertTrue(history.isEmpty())
    }

    @Test
    fun testDeleteHistory() = runBlocking {
        // 添加历史记录
        val entries = mutableListOf<SearchHistoryEntry>()
        for (i in 1..5) {
            val entry = historyManager.addHistory(
                query = "test query $i",
                resultCount = i * 10,
                searchType = "HYBRID"
            )
            entries.add(entry)
        }

        // 删除历史记录
        val deleted = historyManager.deleteHistory(entries[2].id)

        // 验证结果
        assertTrue(deleted)

        // 获取历史记录
        val history = historyManager.getHistory()

        // 验证历史记录
        assertEquals(4, history.size)
        assertFalse(history.any { it.id == entries[2].id })
    }

    @Test
    fun testSearchHistory() = runBlocking {
        // 添加历史记录
        historyManager.addHistory(query = "kotlin class", resultCount = 10, searchType = "HYBRID")
        historyManager.addHistory(query = "java interface", resultCount = 5, searchType = "TEXT")
        historyManager.addHistory(query = "kotlin function", resultCount = 15, searchType = "VECTOR")

        // 搜索历史记录
        val results = historyManager.searchHistory("kotlin")

        // 验证结果
        assertEquals(2, results.size)
        assertTrue(results.all { it.query.contains("kotlin") })
    }

    @Test
    fun testFormatTimestamp() = runBlocking {
        // 添加历史记录
        val entry = historyManager.addHistory(
            query = "test query",
            resultCount = 10,
            searchType = "HYBRID"
        )

        // 格式化时间戳
        val formatted = entry.formatTimestamp("yyyy-MM-dd", ZoneId.systemDefault())

        // 验证结果
        assertNotNull(formatted)
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testSaveAndLoadHistory() = runBlocking {
        // 添加历史记录
        for (i in 1..5) {
            historyManager.addHistory(
                query = "test query $i",
                resultCount = i * 10,
                searchType = "HYBRID"
            )
        }

        // 保存历史记录
        historyManager.saveHistory()

        // 创建新的历史管理器
        val newHistoryManager = SearchHistoryManager(
            SearchHistoryManagerConfig(
                maxHistorySize = 10,
                historyFilePath = tempDir.resolve("search_history.json").toString(),
                enablePersistence = true,
                autoSave = true
            )
        )

        // 获取历史记录
        val history = newHistoryManager.getHistory()

        // 验证历史记录
        assertEquals(5, history.size)
        assertEquals("test query 5", history[0].query)
    }

    @Test
    fun testMaxHistorySize() = runBlocking {
        // 添加超过最大大小的历史记录
        for (i in 1..15) {
            historyManager.addHistory(
                query = "test query $i",
                resultCount = i * 10,
                searchType = "HYBRID"
            )
        }

        // 获取历史记录
        val history = historyManager.getHistory()

        // 验证历史记录
        assertEquals(10, history.size) // 应该限制为最大大小
        assertEquals("test query 15", history[0].query)
        assertEquals("test query 6", history[9].query)
    }
}
