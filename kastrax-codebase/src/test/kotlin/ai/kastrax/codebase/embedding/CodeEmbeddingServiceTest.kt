package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CodeEmbeddingServiceTest {
    
    private lateinit var mockEmbeddingService: MockEmbeddingService
    private lateinit var codeEmbeddingService: CodeEmbeddingService
    
    @BeforeEach
    fun setUp() {
        mockEmbeddingService = MockEmbeddingService()
        codeEmbeddingService = CodeEmbeddingService(
            baseEmbeddingService = mockEmbeddingService,
            config = CodeEmbeddingServiceConfig(
                cacheSize = 100,
                cacheExpirationDuration = kotlin.time.Duration.hours(1),
                batchSize = 10
            )
        )
    }
    
    @Test
    fun `test embedding single code snippet`() = runBlocking {
        // 准备测试数据
        val codeSnippet = """
            public class TestClass {
                /**
                 * This is a test method.
                 */
                public void testMethod() {
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent()
        
        // 生成嵌入
        val embedding = codeEmbeddingService.embed(codeSnippet)
        
        // 验证嵌入维度
        assertEquals(mockEmbeddingService.dimension, embedding.size)
        
        // 验证嵌入不为零
        assertTrue(embedding.any { it != 0f })
    }
    
    @Test
    fun `test embedding batch of code snippets`() = runBlocking {
        // 准备测试数据
        val codeSnippets = listOf(
            """
                public class TestClass1 {
                    public void testMethod1() {
                        System.out.println("Hello, world 1!");
                    }
                }
            """.trimIndent(),
            """
                public class TestClass2 {
                    public void testMethod2() {
                        System.out.println("Hello, world 2!");
                    }
                }
            """.trimIndent(),
            """
                public class TestClass3 {
                    public void testMethod3() {
                        System.out.println("Hello, world 3!");
                    }
                }
            """.trimIndent()
        )
        
        // 生成嵌入
        val embeddings = codeEmbeddingService.embedBatch(codeSnippets)
        
        // 验证嵌入数量
        assertEquals(codeSnippets.size, embeddings.size)
        
        // 验证每个嵌入的维度
        embeddings.forEach { embedding ->
            assertEquals(mockEmbeddingService.dimension, embedding.size)
            assertTrue(embedding.any { it != 0f })
        }
        
        // 验证不同代码片段的嵌入不同
        for (i in 0 until embeddings.size - 1) {
            for (j in i + 1 until embeddings.size) {
                assertNotEquals(
                    embeddings[i].contentToString(),
                    embeddings[j].contentToString(),
                    "嵌入 $i 和 $j 应该不同"
                )
            }
        }
    }
    
    @Test
    fun `test embedding cache`() = runBlocking {
        // 准备测试数据
        val codeSnippet = """
            public class TestClass {
                public void testMethod() {
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent()
        
        // 第一次生成嵌入
        val embedding1 = codeEmbeddingService.embed(codeSnippet)
        
        // 第二次生成嵌入（应该使用缓存）
        val embedding2 = codeEmbeddingService.embed(codeSnippet)
        
        // 验证两次嵌入相同
        assertTrue(embedding1.contentEquals(embedding2))
        
        // 验证缓存命中率
        val cacheStats = codeEmbeddingService.getCacheStats()
        assertEquals(0.5, cacheStats) // 一次缓存未命中，一次命中，命中率为 0.5
        
        // 清除缓存
        codeEmbeddingService.clearCache()
        
        // 第三次生成嵌入（缓存已清除）
        val embedding3 = codeEmbeddingService.embed(codeSnippet)
        
        // 验证嵌入仍然相同（因为使用相同的基础嵌入服务）
        assertTrue(embedding1.contentEquals(embedding3))
        
        // 验证缓存命中率已重置
        val newCacheStats = codeEmbeddingService.getCacheStats()
        assertEquals(0.0, newCacheStats) // 缓存已清除，命中率为 0
    }
    
    @Test
    fun `test code preprocessing`() = runBlocking {
        // 准备测试数据：包含注释的代码
        val codeWithComments = """
            /**
             * This is a class comment.
             */
            public class TestClass {
                // This is a line comment
                public void testMethod() {
                    /* This is a block comment */
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent()
        
        // 准备测试数据：相同代码但没有注释
        val codeWithoutComments = """
            public class TestClass {
                public void testMethod() {
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent()
        
        // 生成嵌入
        val embeddingWithComments = codeEmbeddingService.embed(codeWithComments)
        val embeddingWithoutComments = codeEmbeddingService.embed(codeWithoutComments)
        
        // 验证两个嵌入相似（注释应该被预处理移除）
        // 注意：由于预处理可能不会完全移除所有注释，我们检查相似度而不是完全相等
        val similarity = cosineSimilarity(embeddingWithComments, embeddingWithoutComments)
        assertTrue(similarity > 0.9, "包含注释的代码和不包含注释的代码的嵌入应该相似，相似度: $similarity")
    }
    
    /**
     * 计算余弦相似度
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 余弦相似度
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "向量维度不匹配: ${a.size} != ${b.size}" }
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return if (normA > 0 && normB > 0) {
            dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
        } else {
            0.0
        }
    }
}
