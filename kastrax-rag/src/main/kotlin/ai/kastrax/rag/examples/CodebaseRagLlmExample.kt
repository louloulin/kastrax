package ai.kastrax.rag.examples

import ai.kastrax.core.llm.LlmClient
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmRole
import ai.kastrax.integrations.deepseek.DeepseekChatCompletionRequest
import ai.kastrax.integrations.deepseek.DeepseekClient
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.codebase.CodebaseRagConfig
import ai.kastrax.rag.codebase.CodebaseRagFactory
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.store.embedding.FastEmbeddingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

/**
 * 代码库 RAG 与 LLM 集成示例
 */
object CodebaseRagLlmExample {
    
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
            indexName = "llm_integration_example",
            dimension = embeddingService.dimension
        )
        
        // 创建基础 RAG 系统
        val rag = RAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            defaultOptions = RagProcessOptions(
                contextOptions = ContextBuilderConfig(
                    maxTokens = 4000,
                    includeMetadata = true,
                    metadataFields = listOf("path", "language", "type", "topic")
                )
            )
        )
        
        // 创建代码库 RAG 配置
        val config = CodebaseRagConfig(
            // 使用默认配置
        )
        
        // 创建代码库 RAG
        val codebaseRag = CodebaseRagFactory.create(
            rag = rag,
            rootPath = directoryPath,
            config = config
        )
        
        // 创建 LLM 客户端
        val llmClient = DeepseekClient(
            apiKey = "YOUR_DEEPSEEK_API_KEY", // 替换为你的 API 密钥
            baseUrl = "https://api.deepseek.com"
        )
        
        try {
            // 启动代码库 RAG
            codebaseRag.start()
            
            // 等待索引完成
            println("等待索引完成...")
            delay(10.seconds)
            
            // 执行代码相关查询
            val query = "解释一下这个代码库的主要功能和结构"
            println("\n查询: $query")
            
            // 生成上下文
            val context = codebaseRag.generateContext(query, limit = 5)
            println("\n生成的上下文 (截取):")
            println(context.take(500) + "...")
            
            // 创建 LLM 请求
            val messages = listOf(
                LlmMessage(
                    role = LlmRole.SYSTEM,
                    content = """你是一个专业的代码助手，擅长解释代码和回答编程问题。
                        |请基于提供的上下文回答用户的问题。
                        |如果上下文中没有足够的信息，请告诉用户你无法回答这个问题。
                        |""".trimMargin()
                ),
                LlmMessage(
                    role = LlmRole.USER,
                    content = """
                        |上下文信息:
                        |$context
                        |
                        |问题: $query
                        |""".trimMargin()
                )
            )
            
            // 发送请求到 LLM
            val request = DeepseekChatCompletionRequest(
                messages = messages,
                model = "deepseek-coder",
                temperature = 0.3,
                maxTokens = 1000
            )
            
            println("\n发送请求到 LLM...")
            val response = llmClient.chatCompletion(request)
            
            // 打印 LLM 回答
            println("\nLLM 回答:")
            println(response.choices[0].message.content)
            
            // 等待一段时间
            delay(5.seconds)
            
        } finally {
            // 停止代码库 RAG
            codebaseRag.stop()
            println("代码库 RAG 与 LLM 集成示例完成")
        }
    }
}
