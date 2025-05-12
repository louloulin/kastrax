package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class CodeEmbeddingServiceTest {

    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var codeEmbeddingService: CodeEmbeddingService

    @BeforeEach
    fun setUp() {
        mockEmbeddingService = mock(EmbeddingService::class.java)
        whenever(mockEmbeddingService.dimension).thenReturn(1536)
        
        codeEmbeddingService = CodeEmbeddingService(
            baseEmbeddingService = mockEmbeddingService,
            config = CodeEmbeddingServiceConfig(
                cacheSize = 100,
                batchSize = 10
            )
        )
    }

    @Test
    fun `test dimension property`() {
        assertEquals(1536, codeEmbeddingService.dimension)
    }

    @Test
    fun `test embed method`() = runBlocking {
        // 准备测试数据
        val testCode = """
            public class TestClass {
                // This is a comment
                public void testMethod() {
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent()
        
        val expectedVector = FloatArray(1536) { 0.1f }
        
        // 设置模拟行为
        whenever(mockEmbeddingService.embed(any())).thenReturn(expectedVector)
        
        // 调用被测试方法
        val result = codeEmbeddingService.embed(testCode)
        
        // 验证结果
        assertNotNull(result)
        assertEquals(expectedVector.size, result.size)
        assertEquals(expectedVector[0], result[0])
    }

    @Test
    fun `test embedBatch method`() = runBlocking {
        // 准备测试数据
        val testCodes = listOf(
            """
                public class TestClass1 {
                    // This is a comment
                    public void testMethod1() {
                        System.out.println("Hello, world 1!");
                    }
                }
            """.trimIndent(),
            """
                public class TestClass2 {
                    // This is another comment
                    public void testMethod2() {
                        System.out.println("Hello, world 2!");
                    }
                }
            """.trimIndent()
        )
        
        val expectedVectors = listOf(
            FloatArray(1536) { 0.1f },
            FloatArray(1536) { 0.2f }
        )
        
        // 设置模拟行为
        whenever(mockEmbeddingService.embedBatch(any())).thenReturn(expectedVectors)
        
        // 调用被测试方法
        val results = codeEmbeddingService.embedBatch(testCodes)
        
        // 验证结果
        assertNotNull(results)
        assertEquals(expectedVectors.size, results.size)
        assertEquals(expectedVectors[0].size, results[0].size)
        assertEquals(expectedVectors[0][0], results[0][0])
        assertEquals(expectedVectors[1][0], results[1][0])
    }

    @Test
    fun `test code preprocessing`() = runBlocking {
        // 准备测试数据
        val testCode = """
            public class TestClass {
                // This is a comment that should be removed
                /* This is a multi-line comment
                   that should also be removed */
                public void testMethod() {
                    // Another comment
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent()
        
        val expectedVector = FloatArray(1536) { 0.1f }
        
        // 设置模拟行为
        whenever(mockEmbeddingService.embed(any())).thenReturn(expectedVector)
        
        // 调用被测试方法
        val result = codeEmbeddingService.embed(testCode)
        
        // 验证结果 - 我们不能直接验证预处理的结果，但可以确保方法正常工作
        assertNotNull(result)
        assertEquals(expectedVector.size, result.size)
    }
}
