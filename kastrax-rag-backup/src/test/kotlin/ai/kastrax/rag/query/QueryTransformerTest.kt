package ai.kastrax.rag.query

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryTransformerTest {

    @Test
    fun testNormalizationQueryTransformer() = runBlocking {
        val transformer = NormalizationQueryTransformer()

        // 测试移除多余空格
        assertEquals(
            "hello world",
            transformer.transform("  hello   world  ")
        )

        // 测试移除标点符号
        val result = transformer.transform("hello, world! how are you?")
        assertTrue(result.contains("hello"))
        assertTrue(result.contains("world"))
        assertTrue(result.contains("how"))
        assertTrue(result.contains("are"))
        assertTrue(result.contains("you"))

        // 测试综合情况
        val result2 = transformer.transform("What is artificial intelligence?")
        assertTrue(result2.contains("what"))
        assertTrue(result2.contains("is"))
        assertTrue(result2.contains("artificial"))
        assertTrue(result2.contains("intelligence"))
    }

    @Test
    fun testSynonymQueryTransformer() = runBlocking {
        val synonymMap = mapOf(
            "ai" to listOf("artificial intelligence", "machine learning"),
            "nlp" to listOf("natural language processing")
        )

        val transformer = SynonymQueryTransformer(synonymMap)

        // 测试同义词扩展
        assertEquals(
            "ai (artificial intelligence OR machine learning)",
            transformer.transform("ai")
        )

        // 测试多个词的同义词扩展
        assertEquals(
            "what is ai (artificial intelligence OR machine learning) and nlp (natural language processing)",
            transformer.transform("what is ai and nlp")
        )

        // 测试生成多个查询变体
        val variants = transformer.transformToMultiple("ai and nlp")

        // 应该生成 6 个变体 (2 * 3)
        assertEquals(6, variants.size)

        // 验证变体包含所有可能的组合
        assertTrue(variants.contains("ai and nlp"))
        assertTrue(variants.contains("artificial intelligence and nlp"))
        assertTrue(variants.contains("machine learning and nlp"))
        assertTrue(variants.contains("ai and natural language processing"))
        assertTrue(variants.contains("artificial intelligence and natural language processing"))
        assertTrue(variants.contains("machine learning and natural language processing"))
    }

    @Test
    fun testDecompositionQueryTransformer() = runBlocking {
        val transformer = DecompositionQueryTransformer()

        // 测试句子分解
        val sentences = transformer.transformToMultiple(
            "What is artificial intelligence? How does it work? What are its applications?"
        )

        assertEquals(3, sentences.size)
        assertTrue(sentences.contains("What is artificial intelligence"))
        assertTrue(sentences.contains("How does it work"))
        assertTrue(sentences.contains("What are its applications"))

        // 测试短语分解
        val phrases = transformer.transformToMultiple(
            "Define AI, explain machine learning, describe neural networks"
        )

        assertEquals(3, phrases.size)
        assertTrue(phrases.contains("Define AI"))
        assertTrue(phrases.contains("explain machine learning"))
        assertTrue(phrases.contains("describe neural networks"))
    }

    @Test
    fun testCompositeQueryTransformer() = runBlocking {
        val synonymMap = mapOf(
            "ai" to listOf("artificial intelligence")
        )

        val transformers = listOf(
            NormalizationQueryTransformer(),
            SynonymQueryTransformer(synonymMap)
        )

        val transformer = CompositeQueryTransformer(transformers)

        // 测试组合转换
        assertEquals(
            "what is ai (artificial intelligence)",
            transformer.transform("What is AI?")
        )

        // 测试生成多个查询变体
        val variants = transformer.transformToMultiple("What is AI?")

        assertEquals(2, variants.size)
        assertTrue(variants.contains("what is ai"))
        assertTrue(variants.contains("what is artificial intelligence"))
    }
}
