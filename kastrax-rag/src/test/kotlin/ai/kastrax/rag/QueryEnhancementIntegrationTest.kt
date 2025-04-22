package ai.kastrax.rag

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryEnhancementIntegrationTest {
    
    private lateinit var rag: RAG
    private lateinit var vectorStore: InMemoryVectorStore
    private lateinit var embeddingService: RandomEmbeddingService
    
    @BeforeEach
    fun setup() {
        // 创建向量存储和嵌入服务
        vectorStore = InMemoryVectorStore()
        embeddingService = RandomEmbeddingService()
        
        // 创建 RAG 系统
        rag = RAG(vectorStore, embeddingService)
        
        // 添加测试文档
        runBlocking {
            val documents = listOf(
                Document(
                    "人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，致力于创建能够模拟人类智能的系统。",
                    mapOf("category" to "AI", "type" to "definition")
                ),
                Document(
                    "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习和改进。",
                    mapOf("category" to "ML", "type" to "definition")
                ),
                Document(
                    "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
                    mapOf("category" to "DL", "type" to "definition")
                ),
                Document(
                    "自然语言处理（NLP）是人工智能的一个分支，专注于使计算机能够理解、解释和生成人类语言。",
                    mapOf("category" to "NLP", "type" to "definition")
                ),
                Document(
                    "计算机视觉是人工智能的一个领域，它使计算机能够从数字图像或视频中获取高级理解。",
                    mapOf("category" to "CV", "type" to "definition")
                )
            )
            
            // 将文档转换为字符串和元数据
            val docContents = documents.map { it.content }
            val docMetadata = documents.map { doc -> doc.metadata.mapValues { it.value.toString() } }
            
            // 添加文档到向量存储
            vectorStore.addDocuments(docContents, embeddingService, docMetadata)
        }
    }
    
    @Test
    fun testBasicSearch() = runBlocking {
        // 不使用查询增强的基本搜索
        val results = rag.search("什么是人工智能", limit = 3)
        
        // 验证结果
        assertEquals(3, results.size)
        
        // 验证第一个结果应该与人工智能相关
        assertTrue(results[0].document.content.contains("人工智能"))
    }
    
    @Test
    fun testQueryEnhancedSearch() = runBlocking {
        // 使用查询增强的搜索
        val results = rag.search(
            "什么是AI",
            limit = 3,
            options = RagProcessOptions(
                useQueryEnhancement = true,
                queryEnhancementOptions = ai.kastrax.rag.retrieval.QueryEnhancedRetrieverConfig(
                    useMultiQuery = true
                )
            )
        )
        
        // 验证结果
        assertEquals(3, results.size)
        
        // 验证结果应该包含与人工智能相关的文档
        val containsAI = results.any { it.document.content.contains("人工智能") }
        assertTrue(containsAI, "结果应该包含与人工智能相关的文档")
    }
    
    @Test
    fun testQueryExpansionWithSynonyms() = runBlocking {
        // 使用同义词扩展的查询增强搜索
        val results = rag.search(
            "机器学习",
            limit = 3,
            options = RagProcessOptions(
                useQueryEnhancement = true,
                queryEnhancementOptions = ai.kastrax.rag.retrieval.QueryEnhancedRetrieverConfig(
                    useMultiQuery = true,
                    mergeStrategy = ai.kastrax.rag.retrieval.MergeStrategy.DIVERSITY
                )
            )
        )
        
        // 验证结果
        assertEquals(3, results.size)
        
        // 验证结果应该包含与机器学习相关的文档
        val containsML = results.any { it.document.content.contains("机器学习") }
        assertTrue(containsML, "结果应该包含与机器学习相关的文档")
        
        // 由于同义词扩展，结果可能还包含与深度学习相关的文档
        val containsDL = results.any { it.document.content.contains("深度学习") }
        assertTrue(containsDL, "结果应该包含与深度学习相关的文档")
    }
}
