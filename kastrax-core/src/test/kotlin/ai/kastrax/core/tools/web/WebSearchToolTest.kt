package ai.kastrax.core.tools.web

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebSearchToolTest {

    @Test
    fun `test web search tool with mock search engine`() = runBlocking {
        // 创建 Web 搜索工具
        val webSearchTool = WebSearchTool(
            searchEngine = WebSearchTool.SearchEngine.MOCK,
            maxResults = 3
        )
        
        // 创建输入
        val input = buildJsonObject {
            put("query", "Kotlin programming")
            put("num_results", 3)
        }
        
        // 执行搜索
        val result = webSearchTool.execute(input)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result is JsonObject)
        val resultObj = result as JsonObject
        
        assertTrue(resultObj.containsKey("results"))
        assertTrue(resultObj.containsKey("query"))
        assertTrue(resultObj.containsKey("total_results"))
        
        val results = resultObj["results"] as JsonArray
        assertEquals(3, results.size)
        
        val firstResult = results[0].jsonObject
        assertTrue(firstResult.containsKey("title"))
        assertTrue(firstResult.containsKey("url"))
        assertTrue(firstResult.containsKey("snippet"))
        
        assertEquals("Kotlin programming", resultObj["query"].toString().replace("\"", ""))
        assertEquals("3", resultObj["total_results"].toString())
    }
    
    @Test
    fun `test web search tool with default number of results`() = runBlocking {
        // 创建 Web 搜索工具
        val webSearchTool = WebSearchTool(
            searchEngine = WebSearchTool.SearchEngine.MOCK,
            maxResults = 5
        )
        
        // 创建输入（不指定结果数量）
        val input = buildJsonObject {
            put("query", "Kotlin programming")
        }
        
        // 执行搜索
        val result = webSearchTool.execute(input)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result is JsonObject)
        val resultObj = result as JsonObject
        
        assertTrue(resultObj.containsKey("results"))
        
        val results = resultObj["results"] as JsonArray
        assertEquals(5, results.size) // 应该使用默认值 5
    }
}
