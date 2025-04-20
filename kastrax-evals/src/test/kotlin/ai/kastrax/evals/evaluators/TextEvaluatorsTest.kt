package ai.kastrax.evals.evaluators

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TextEvaluatorsTest {

    @Test
    fun `test exact match evaluator`() = runBlocking {
        val evaluator = exactMatchEvaluator()
        
        // 测试完全匹配
        val result1 = evaluator.evaluate("test", "test")
        assertEquals(1.0, result1.score)
        assertTrue(result1.details["isMatch"] as Boolean)
        
        // 测试不匹配
        val result2 = evaluator.evaluate("test", "different")
        assertEquals(0.0, result2.score)
        assertFalse(result2.details["isMatch"] as Boolean)
        
        // 测试忽略大小写
        val result3 = evaluator.evaluate("test", "TEST", mapOf("ignoreCase" to true))
        assertEquals(1.0, result3.score)
        assertTrue(result3.details["isMatch"] as Boolean)
        
        // 测试忽略空白
        val result4 = evaluator.evaluate("test", "  test  ", mapOf("ignoreWhitespace" to true))
        assertEquals(1.0, result4.score)
        assertTrue(result4.details["isMatch"] as Boolean)
    }
    
    @Test
    fun `test contains keywords evaluator`() = runBlocking {
        val evaluator = containsKeywordsEvaluator()
        
        // 测试包含所有关键词
        val result1 = evaluator.evaluate(
            "test",
            "This is a test message",
            mapOf("keywords" to listOf("test", "message"))
        )
        assertEquals(1.0, result1.score)
        assertEquals(2, result1.details["matchedCount"])
        
        // 测试只包含部分关键词
        val result2 = evaluator.evaluate(
            "test",
            "This is a test",
            mapOf("keywords" to listOf("test", "missing"), "requireAll" to false)
        )
        assertEquals(0.5, result2.score)
        assertEquals(1, result2.details["matchedCount"])
        
        // 测试不包含任何关键词
        val result3 = evaluator.evaluate(
            "test",
            "Nothing here",
            mapOf("keywords" to listOf("test", "message"))
        )
        assertEquals(0.0, result3.score)
        assertEquals(0, result3.details["matchedCount"])
    }
    
    @Test
    fun `test regex match evaluator`() = runBlocking {
        val evaluator = regexMatchEvaluator()
        
        // 测试匹配正则表达式
        val result1 = evaluator.evaluate(
            "test",
            "The number is 12345",
            mapOf("pattern" to "\\d+")
        )
        assertEquals(1.0, result1.score)
        assertTrue(result1.details["isMatch"] as Boolean)
        assertEquals(listOf("12345"), result1.details["matches"])
        
        // 测试不匹配正则表达式
        val result2 = evaluator.evaluate(
            "test",
            "No numbers here",
            mapOf("pattern" to "\\d+")
        )
        assertEquals(0.0, result2.score)
        assertFalse(result2.details["isMatch"] as Boolean)
        assertEquals(emptyList<String>(), result2.details["matches"])
        
        // 测试多次匹配
        val result3 = evaluator.evaluate(
            "test",
            "Numbers: 123, 456, 789",
            mapOf("pattern" to "\\d+")
        )
        assertEquals(1.0, result3.score)
        assertTrue(result3.details["isMatch"] as Boolean)
        assertEquals(listOf("123", "456", "789"), result3.details["matches"])
    }
    
    @Test
    fun `test similarity evaluator`() = runBlocking {
        val evaluator = similarityEvaluator()
        
        // 测试 Jaccard 相似度
        val result1 = evaluator.evaluate(
            "This is a test sentence",
            "This is a test sentence",
            mapOf("method" to "jaccard")
        )
        assertEquals(1.0, result1.score)
        
        // 测试部分相似
        val result2 = evaluator.evaluate(
            "This is a test sentence",
            "This is a different sentence",
            mapOf("method" to "jaccard")
        )
        assertTrue(result2.score > 0.0 && result2.score < 1.0)
        
        // 测试 Levenshtein 相似度
        val result3 = evaluator.evaluate(
            "test",
            "test",
            mapOf("method" to "levenshtein")
        )
        assertEquals(1.0, result3.score)
        
        // 测试部分相似
        val result4 = evaluator.evaluate(
            "test",
            "tent",
            mapOf("method" to "levenshtein")
        )
        assertTrue(result4.score > 0.0 && result4.score < 1.0)
    }
}
