package ai.kastrax.rag.embedding

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RandomEmbeddingServiceTest {

    @Test
    fun `test embed returns vector of correct dimension`() = runBlocking {
        // 创建嵌入服务
        val dimensions = 512
        val embeddingService = RandomEmbeddingService(dimensions = dimensions)
        
        // 嵌入文本
        val text = "This is a test text for embedding."
        val embedding = embeddingService.embed(text)
        
        // 验证维度
        assertEquals(dimensions, embedding.size)
        assertEquals(dimensions, embeddingService.dimension())
    }
    
    @Test
    fun `test embedBatch returns vectors of correct dimension`() = runBlocking {
        // 创建嵌入服务
        val dimensions = 512
        val embeddingService = RandomEmbeddingService(dimensions = dimensions)
        
        // 嵌入文本列表
        val texts = listOf(
            "This is the first test text.",
            "This is the second test text.",
            "This is the third test text."
        )
        val embeddings = embeddingService.embedBatch(texts)
        
        // 验证结果
        assertEquals(texts.size, embeddings.size)
        embeddings.forEach { embedding ->
            assertEquals(dimensions, embedding.size)
        }
    }
    
    @Test
    fun `test same text produces same embedding`() = runBlocking {
        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService()
        
        // 嵌入相同的文本两次
        val text = "This is a test text for embedding."
        val embedding1 = embeddingService.embed(text)
        val embedding2 = embeddingService.embed(text)
        
        // 验证两个嵌入向量相同
        assertTrue(embedding1.contentEquals(embedding2))
    }
    
    @Test
    fun `test embeddings are normalized`() = runBlocking {
        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService()
        
        // 嵌入文本
        val text = "This is a test text for embedding."
        val embedding = embeddingService.embed(text)
        
        // 计算向量的范数
        val norm = Math.sqrt(embedding.sumOf { it * it.toDouble() })
        
        // 验证向量已归一化（范数约为 1）
        assertEquals(1.0, norm, 0.0001)
    }
    
    @Test
    fun `test different texts produce different embeddings`() = runBlocking {
        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService()
        
        // 嵌入不同的文本
        val text1 = "This is the first text."
        val text2 = "This is a completely different text."
        val embedding1 = embeddingService.embed(text1)
        val embedding2 = embeddingService.embed(text2)
        
        // 验证两个嵌入向量不同
        assertTrue(!embedding1.contentEquals(embedding2))
        
        // 计算相似度
        val similarity = embedding1.cosineSimilarity(embedding2)
        
        // 验证相似度在合理范围内（-1 到 1）
        assertTrue(similarity >= -1.0 && similarity <= 1.0)
    }
}
