package ai.kastrax.examples.ragx

import ai.kastrax.core.llm.deepseek.DeepSeekChatModel
import ai.kastrax.core.llm.deepseek.DeepSeekChatOptions
import ai.kastrax.ragx.ContextFormat
import ai.kastrax.ragx.RagX
import ai.kastrax.ragx.RagXOptions
import ai.kastrax.ragx.document.TextFileLoader
import ai.kastrax.ragx.document.TextSplitter
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.embedding.OpenAIEmbeddingService
import kotlinx.coroutines.runBlocking

/**
 * 简单的 RagX 示例，展示如何使用 RagX 加载文档、搜索文档和生成上下文。
 */
fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = OpenAIEmbeddingService(
        apiKey = "your-openai-api-key",
        model = "text-embedding-3-small"
    )
    
    // 创建向量存储
    val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
    
    // 创建 RagX 实例
    val ragX = RagX(
        vectorStore = vectorStore,
        embeddingService = embeddingService,
        defaultOptions = RagXOptions(
            contextFormat = ContextFormat.MARKDOWN,
            includeMetadata = true
        )
    )
    
    // 加载文档
    val loader = TextFileLoader("path/to/your/documents")
    val splitter = TextSplitter(chunkSize = 500, chunkOverlap = 100)
    val count = ragX.loadDocuments(loader, splitter)
    println("Loaded $count document chunks")
    
    // 搜索文档
    val query = "What is RAG?"
    val searchResults = ragX.search(query, limit = 3)
    println("Search results:")
    searchResults.forEachIndexed { index, result ->
        println("${index + 1}. ${result.document.content.take(100)}... (score: ${result.score})")
    }
    
    // 生成上下文
    val context = ragX.generateContext(query, limit = 3)
    println("\nGenerated context:")
    println(context)
    
    // 使用 LLM 生成回答
    val llm = DeepSeekChatModel(
        apiKey = "your-deepseek-api-key",
        options = DeepSeekChatOptions(
            model = "deepseek-chat",
            temperature = 0.7
        )
    )
    
    val prompt = """
        You are a helpful assistant. Use the following context to answer the question.
        
        Context:
        $context
        
        Question: $query
        
        Answer:
    """.trimIndent()
    
    val response = llm.chat(prompt)
    println("\nLLM response:")
    println(response)
}
