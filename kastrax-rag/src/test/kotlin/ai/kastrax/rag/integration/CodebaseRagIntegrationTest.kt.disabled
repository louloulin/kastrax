package ai.kastrax.rag.integration

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.codebase.CodebaseRAG
import ai.kastrax.rag.codebase.CodebaseRagConfig
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.store.document.Document
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodebaseRagIntegrationTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var codebaseRag: CodebaseRAG
    private lateinit var rag: RAG
    private lateinit var embeddingService: MockEmbeddingService
    private lateinit var documentStore: DocumentVectorStoreAdapter
    
    @BeforeEach
    fun setUp() = runBlocking {
        // 创建测试文件
        createTestFiles()
        
        // 创建嵌入服务
        embeddingService = MockEmbeddingService()
        
        // 创建向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
        
        // 创建文档向量存储适配器
        documentStore = DocumentVectorStoreAdapter(
            vectorStore = vectorStore,
            indexName = "integration_test",
            dimension = embeddingService.dimension
        )
        
        // 创建基础 RAG 系统
        rag = RAG(
            documentStore = documentStore,
            embeddingService = embeddingService
        )
        
        // 添加一些普通文档到 RAG
        val documents = listOf(
            Document(
                content = "这是一个关于 RAG 的文档，介绍了检索增强生成技术。",
                metadata = mapOf("type" to "article", "topic" to "RAG")
            ),
            Document(
                content = "这是一个关于 LLM 的文档，介绍了大型语言模型技术。",
                metadata = mapOf("type" to "article", "topic" to "LLM")
            )
        )
        
        // 添加文档到 RAG
        documentStore.addDocuments(documents, embeddingService)
        
        // 创建代码库 RAG 配置
        val config = CodebaseRagConfig(
            ragProcessOptions = RagProcessOptions(
                contextOptions = ContextBuilderConfig(
                    maxTokens = 2000,
                    includeMetadata = true,
                    metadataFields = listOf("path", "language", "type", "topic")
                )
            )
        )
        
        // 创建代码库 RAG
        codebaseRag = CodebaseRAG.create(
            documentStore = documentStore,
            embeddingService = embeddingService,
            rootPath = tempDir,
            config = config
        )
        
        // 启动代码库 RAG
        codebaseRag.start()
    }
    
    @AfterEach
    fun tearDown() = runBlocking {
        // 停止代码库 RAG
        codebaseRag.stop()
    }
    
    @Test
    fun `test integration with existing RAG documents`() = runBlocking {
        // 等待索引完成
        Thread.sleep(2000)
        
        // 执行查询 - 应该同时返回代码文件和普通文档
        val query = "RAG 技术"
        val searchResults = codebaseRag.search(query, limit = 5)
        
        // 验证结果
        assertTrue(searchResults.isNotEmpty())
        
        // 检查是否包含普通文档
        val hasRegularDoc = searchResults.any { 
            it.document.metadata.containsKey("topic") && it.document.metadata["topic"] == "RAG"
        }
        assertTrue(hasRegularDoc, "搜索结果应该包含普通 RAG 文档")
        
        // 检查是否包含代码文件
        val hasCodeFile = searchResults.any { 
            it.document.metadata.containsKey("language")
        }
        assertTrue(hasCodeFile, "搜索结果应该包含代码文件")
    }
    
    @Test
    fun `test context generation with mixed content`() = runBlocking {
        // 等待索引完成
        Thread.sleep(2000)
        
        // 执行查询
        val query = "RAG 和代码库"
        val context = codebaseRag.generateContext(query, limit = 5)
        
        // 验证结果
        assertTrue(context.isNotEmpty())
        assertTrue(context.contains("RAG"), "上下文应该包含 RAG 相关内容")
        assertTrue(context.contains("代码"), "上下文应该包含代码相关内容")
    }
    
    /**
     * 创建测试文件
     */
    private fun createTestFiles() {
        // 创建 Kotlin 文件
        val kotlinFilePath = tempDir.resolve("RagIntegration.kt")
        kotlinFilePath.writeText("""
            package ai.kastrax.rag.integration
            
            /**
             * 这个类演示了 RAG 和代码库集成。
             */
            class RagIntegration {
                /**
                 * 使用 RAG 搜索代码库
                 */
                fun searchCodebase(query: String) {
                    // 使用 RAG 搜索代码库
                    println("搜索代码库: $query")
                }
            }
        """.trimIndent())
        
        // 创建 Markdown 文件
        val markdownFilePath = tempDir.resolve("integration.md")
        markdownFilePath.writeText("""
            # RAG 与代码库集成
            
            本文档介绍了如何将 RAG 技术与代码库理解功能集成。
            
            ## 主要功能
            
            1. 代码库索引
            2. 代码语义理解
            3. 与现有 RAG 系统集成
        """.trimIndent())
    }
}
