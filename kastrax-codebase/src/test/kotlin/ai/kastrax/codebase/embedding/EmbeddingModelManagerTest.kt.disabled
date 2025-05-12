package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmbeddingModelManagerTest {
    
    private lateinit var modelManager: EmbeddingModelManager
    private lateinit var mockEmbeddingService1: MockEmbeddingService
    private lateinit var mockEmbeddingService2: MockEmbeddingService
    
    @BeforeEach
    fun setUp() {
        mockEmbeddingService1 = MockEmbeddingService()
        mockEmbeddingService2 = MockEmbeddingService()
        
        modelManager = EmbeddingModelManager(
            config = EmbeddingModelManagerConfig(
                defaultVersion = "v1",
                transitionPeriodMs = 1000, // 1秒，便于测试
                cacheSize = 100
            )
        )
    }
    
    @Test
    fun `test registering model versions`() = runBlocking {
        // 注册模型版本
        val result1 = modelManager.registerModelVersion("v1", mockEmbeddingService1, true)
        val result2 = modelManager.registerModelVersion("v2", mockEmbeddingService2)
        
        // 验证注册结果
        assertTrue(result1)
        assertTrue(result2)
        
        // 验证模型版本
        val modelVersion1 = modelManager.getModelVersion("v1")
        val modelVersion2 = modelManager.getModelVersion("v2")
        
        assertNotNull(modelVersion1)
        assertNotNull(modelVersion2)
        assertEquals("v1", modelVersion1.version)
        assertEquals("v2", modelVersion2.version)
        
        // 验证活动版本
        assertEquals("v1", modelManager.getActiveVersion())
        
        // 验证所有模型版本
        val allVersions = modelManager.getAllModelVersions()
        assertEquals(2, allVersions.size)
        assertTrue(allVersions.any { it.version == "v1" })
        assertTrue(allVersions.any { it.version == "v2" })
    }
    
    @Test
    fun `test setting active version`() = runBlocking {
        // 注册模型版本
        modelManager.registerModelVersion("v1", mockEmbeddingService1, true)
        modelManager.registerModelVersion("v2", mockEmbeddingService2)
        
        // 验证初始活动版本
        assertEquals("v1", modelManager.getActiveVersion())
        
        // 设置活动版本（无平滑过渡）
        val result = modelManager.setActiveVersion("v2", smoothTransition = false)
        
        // 验证设置结果
        assertTrue(result)
        
        // 验证活动版本已更新
        assertEquals("v2", modelManager.getActiveVersion())
        
        // 验证没有过渡版本
        assertNull(modelManager.getTransitionVersion())
    }
    
    @Test
    fun `test smooth transition between versions`() = runBlocking {
        // 注册模型版本
        modelManager.registerModelVersion("v1", mockEmbeddingService1, true)
        modelManager.registerModelVersion("v2", mockEmbeddingService2)
        
        // 验证初始活动版本
        assertEquals("v1", modelManager.getActiveVersion())
        
        // 设置活动版本（平滑过渡）
        val result = modelManager.setActiveVersion("v2", smoothTransition = true)
        
        // 验证设置结果
        assertTrue(result)
        
        // 验证过渡版本
        assertEquals("v2", modelManager.getTransitionVersion())
        
        // 验证过渡进度
        val progress = modelManager.getTransitionProgress()
        assertNotNull(progress)
        assertTrue(progress >= 0.0 && progress <= 1.0)
        
        // 等待过渡期结束
        Thread.sleep(1100) // 等待略多于过渡期
        
        // 验证活动版本已更新
        assertEquals("v2", modelManager.getActiveVersion())
        
        // 验证过渡版本已清除
        assertNull(modelManager.getTransitionVersion())
    }
    
    @Test
    fun `test embedding with specific version`() = runBlocking {
        // 注册模型版本
        modelManager.registerModelVersion("v1", mockEmbeddingService1, true)
        modelManager.registerModelVersion("v2", mockEmbeddingService2)
        
        // 准备测试数据
        val text = "This is a test text."
        
        // 使用特定版本嵌入
        val embedding1 = modelManager.embed(text, "v1")
        val embedding2 = modelManager.embed(text, "v2")
        
        // 验证嵌入维度
        assertEquals(mockEmbeddingService1.dimension, embedding1.size)
        assertEquals(mockEmbeddingService2.dimension, embedding2.size)
        
        // 验证嵌入不为零
        assertTrue(embedding1.any { it != 0f })
        assertTrue(embedding2.any { it != 0f })
        
        // 验证两个版本的嵌入相同（因为都使用 MockEmbeddingService）
        assertTrue(embedding1.contentEquals(embedding2))
    }
    
    @Test
    fun `test batch embedding with caching`() = runBlocking {
        // 注册模型版本
        modelManager.registerModelVersion("v1", mockEmbeddingService1, true)
        
        // 准备测试数据
        val texts = listOf(
            "This is the first test text.",
            "This is the second test text.",
            "This is the third test text."
        )
        
        // 第一次批量嵌入
        val embeddings1 = modelManager.embedBatch(texts)
        
        // 验证嵌入数量
        assertEquals(texts.size, embeddings1.size)
        
        // 验证每个嵌入的维度
        embeddings1.forEach { embedding ->
            assertEquals(mockEmbeddingService1.dimension, embedding.size)
            assertTrue(embedding.any { it != 0f })
        }
        
        // 第二次批量嵌入（应该使用缓存）
        val embeddings2 = modelManager.embedBatch(texts)
        
        // 验证两次嵌入相同
        for (i in texts.indices) {
            assertTrue(embeddings1[i].contentEquals(embeddings2[i]))
        }
        
        // 清除缓存
        modelManager.clearCache()
        
        // 第三次批量嵌入（缓存已清除）
        val embeddings3 = modelManager.embedBatch(texts)
        
        // 验证嵌入仍然相同（因为使用相同的基础嵌入服务）
        for (i in texts.indices) {
            assertTrue(embeddings1[i].contentEquals(embeddings3[i]))
        }
    }
}
