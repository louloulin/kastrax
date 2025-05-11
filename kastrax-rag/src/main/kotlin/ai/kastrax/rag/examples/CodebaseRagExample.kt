package ai.kastrax.rag.examples

import ai.kastrax.rag.codebase.CodebaseRAG
import ai.kastrax.rag.codebase.CodebaseRagConfig
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.store.embedding.FastEmbeddingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

/**
 * 代码库 RAG 示例
 */
object CodebaseRagExample {
    
    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取要索引的目录路径
        val directoryPath = if (args.isNotEmpty()) {
            Path(args[0])
        } else {
            // 默认使用当前目录
            Path(".")
        }
        
        println("开始索引目录: $directoryPath")
        
        // 创建嵌入服务
        val embeddingService = FastEmbeddingService.create()
        
        // 创建向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
        
        // 创建文档向量存储适配器
        val documentStore = DocumentVectorStoreAdapter(
            vectorStore = vectorStore,
            indexName = "codebase",
            dimension = embeddingService.dimension
        )
        
        // 创建代码库 RAG 配置
        val config = CodebaseRagConfig(
            // 使用默认配置
        )
        
        // 创建代码库 RAG
        val codebaseRag = CodebaseRAG.create(
            documentStore = documentStore,
            embeddingService = embeddingService,
            rootPath = directoryPath,
            config = config
        )
        
        try {
            // 启动代码库 RAG
            codebaseRag.start()
            
            // 等待索引完成
            println("等待索引完成...")
            delay(10.seconds)
            
            // 执行查询
            val query = "如何使用 CodebaseRAG？"
            println("\n查询: $query")
            
            // 搜索代码库
            val searchResults = codebaseRag.search(query, limit = 3)
            println("\n搜索结果:")
            searchResults.forEachIndexed { index, result ->
                println("${index + 1}. 文件: ${result.document.metadata["path"]}")
                println("   语言: ${result.document.metadata["language"]}")
                println("   相似度: ${result.score}")
                println("   内容片段: ${result.document.content.take(200)}...")
                println()
            }
            
            // 生成上下文
            val context = codebaseRag.generateContext(query, limit = 3)
            println("\n生成的上下文:")
            println(context.take(500) + "...")
            
            // 等待一段时间
            delay(5.seconds)
            
        } finally {
            // 停止代码库 RAG
            codebaseRag.stop()
            println("代码库 RAG 示例完成")
        }
    }
}
