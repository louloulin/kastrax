package ai.kastrax.rag.embedding

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CachedEmbeddingServiceTest {

    @Test
    fun `test cache hit for single embedding`() = runBlocking {
        // 创建模拟的嵌入服务
        val mockEmbeddingService = mockk<EmbeddingService>()
        val text = "This is a test text."
        val embedding = floatArrayOf(0.1f, 0.2f, 0.3f)
        
        // 设置模拟行为
        coEvery { mockEmbeddingService.embed(text) } returns embedding
        coEvery { mockEmbeddingService.dimension } returns embedding.size
        
        // 创建缓存嵌入服务
        val cachedService = CachedEmbeddingService(mockEmbeddingService)
        
        // 第一次调用（缓存未命中）
        val result1 = cachedService.embed(text)
        
        // 第二次调用（缓存命中）
        val result2 = cachedService.embed(text)
        
        // 验证结果
        assertTrue(result1.contentEquals(embedding))
        assertTrue(result2.contentEquals(embedding))
        
        // 验证模拟服务只被调用一次
        coVerify(exactly = 1) { mockEmbeddingService.embed(text) }
    }
    
    @Test
    fun `test cache hit for batch embedding`() = runBlocking {
        // 创建模拟的嵌入服务
        val mockEmbeddingService = mockk<EmbeddingService>()
        val texts = listOf("Text 1", "Text 2", "Text 3")
        val embeddings = listOf(
            floatArrayOf(0.1f, 0.2f),
            floatArrayOf(0.3f, 0.4f),
            floatArrayOf(0.5f, 0.6f)
        )
        
        // 设置模拟行为
        coEvery { mockEmbeddingService.embedBatch(texts) } returns embeddings
        coEvery { mockEmbeddingService.embed(any()) } coAnswers {
            val text = firstArg<String>()
            val index = texts.indexOf(text)
            if (index >= 0) embeddings[index] else floatArrayOf()
        }
        coEvery { mockEmbeddingService.dimension } returns 2
        
        // 创建缓存嵌入服务
        val cachedService = CachedEmbeddingService(mockEmbeddingService)
        
        // 第一次调用（缓存未命中）
        val results1 = cachedService.embedBatch(texts)
        
        // 第二次调用（缓存命中）
        val results2 = cachedService.embedBatch(texts)
        
        // 验证结果
        assertEquals(texts.size, results1.size)
        assertEquals(texts.size, results2.size)
        
        for (i in texts.indices) {
            assertTrue(results1[i].contentEquals(embeddings[i]))
            assertTrue(results2[i].contentEquals(embeddings[i]))
        }
        
        // 验证模拟服务只被调用一次
        coVerify(exactly = 1) { mockEmbeddingService.embedBatch(texts) }
    }
    
    @Test
    fun `test cache eviction when cache is full`() = runBlocking {
        // 创建模拟的嵌入服务
        val mockEmbeddingService = mockk<EmbeddingService>()
        val cacheSize = 2
        
        // 设置模拟行为
        coEvery { mockEmbeddingService.embed(any()) } answers {
            val text = firstArg<String>()
            floatArrayOf(text.hashCode().toFloat())
        }
        coEvery { mockEmbeddingService.dimension } returns 1
        
        // 创建缓存嵌入服务
        val cachedService = CachedEmbeddingService(mockEmbeddingService, cacheSize)
        
        // 填充缓存
        val text1 = "Text 1"
        val text2 = "Text 2"
        cachedService.embed(text1)
        cachedService.embed(text2)
        
        // 验证前两个文本的嵌入已缓存
        coVerify(exactly = 1) { mockEmbeddingService.embed(text1) }
        coVerify(exactly = 1) { mockEmbeddingService.embed(text2) }
        
        // 添加第三个文本，应该导致缓存淘汰
        val text3 = "Text 3"
        cachedService.embed(text3)
        
        // 再次请求第一个文本，应该导致缓存未命中
        cachedService.embed(text1)
        
        // 验证第一个文本被调用了两次（缓存淘汰后重新加载）
        coVerify(exactly = 2) { mockEmbeddingService.embed(text1) }
        
        // 验证第二个和第三个文本各被调用一次
        coVerify(exactly = 1) { mockEmbeddingService.embed(text2) }
        coVerify(exactly = 1) { mockEmbeddingService.embed(text3) }
    }
    
    @Test
    fun `test dimension delegates to underlying service`() {
        // 创建模拟的嵌入服务
        val mockEmbeddingService = mockk<EmbeddingService>()
        val dimension = 512
        
        // 设置模拟行为
        coEvery { mockEmbeddingService.dimension } returns dimension
        
        // 创建缓存嵌入服务
        val cachedService = CachedEmbeddingService(mockEmbeddingService)
        
        // 验证维度
        assertEquals(dimension, cachedService.dimension)
    }
}
