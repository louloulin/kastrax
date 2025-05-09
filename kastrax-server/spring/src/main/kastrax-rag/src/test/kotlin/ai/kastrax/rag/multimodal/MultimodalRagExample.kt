package ai.kastrax.rag.multimodal

import ai.kastrax.rag.RagProcessOptions
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * 多模态 RAG 示例。
 */
fun main() = runBlocking {
    // 创建 OpenAI 多模态 RAG
    val rag = MultimodalRagFactory.createOpenAIMultimodalRag(
        apiKey = "your-openai-api-key" // 替换为你的 OpenAI API 密钥
    )
    
    // 创建多模态文档
    val documents = listOf(
        // 文本文档
        MultimodalDocument.text(
            id = UUID.randomUUID().toString(),
            content = "Kotlin 是一种在 Java 虚拟机上运行的静态类型编程语言，由 JetBrains 开发。",
            metadata = mapOf("source" to "kotlin-intro.txt")
        ),
        
        // 图像文档
        MultimodalDocument.image(
            id = UUID.randomUUID().toString(),
            imageUrl = "https://example.com/kotlin-logo.png",
            caption = "Kotlin 编程语言的标志",
            metadata = mapOf("source" to "kotlin-logo.png")
        ),
        
        // 文本和图像混合文档
        MultimodalDocument.textAndImage(
            id = UUID.randomUUID().toString(),
            content = "Kotlin 协程提供了一种简单的方式来处理异步编程。",
            imageUrl = "https://example.com/kotlin-coroutines.png",
            metadata = mapOf("source" to "kotlin-coroutines.md")
        )
    )
    
    // 加载文档
    val success = rag.loadMultimodalDocuments(documents)
    println("加载文档: ${if (success) "成功" else "失败"}")
    
    // 使用文本查询
    val textQuery = "Kotlin 协程是什么？"
    val textResults = rag.search(textQuery)
    println("\n文本查询: $textQuery")
    println("找到 ${textResults.size} 个结果:")
    textResults.forEachIndexed { index, result ->
        println("${index + 1}. ${result.document.content} (分数: ${result.score})")
    }
    
    // 使用多模态查询（文本和图像）
    val multimodalQuery = "Kotlin 标志"
    val imageUrl = "https://example.com/kotlin-logo-query.png"
    val multimodalResults = rag.multimodalSearch(
        textQuery = multimodalQuery,
        imageUrl = imageUrl
    )
    println("\n多模态查询: $multimodalQuery + 图像")
    println("找到 ${multimodalResults.size} 个结果:")
    multimodalResults.forEachIndexed { index, result ->
        println("${index + 1}. ${result.document.content} (分数: ${result.score})")
    }
    
    // 生成上下文
    val context = rag.generateContext(textQuery)
    println("\n生成的上下文:")
    println(context)
    
    // 生成多模态上下文
    val multimodalContext = rag.generateMultimodalContext(
        textQuery = multimodalQuery,
        imageUrl = imageUrl
    )
    println("\n生成的多模态上下文:")
    println(multimodalContext)
    
    // 检索上下文
    val retrieveResult = rag.retrieveContext(textQuery)
    println("\n检索上下文:")
    println("找到 ${retrieveResult.results.size} 个结果")
    println("上下文: ${retrieveResult.context}")
    
    // 检索多模态上下文
    val retrieveMultimodalResult = rag.retrieveMultimodalContext(
        textQuery = multimodalQuery,
        imageUrl = imageUrl
    )
    println("\n检索多模态上下文:")
    println("找到 ${retrieveMultimodalResult.results.size} 个结果")
    println("上下文: ${retrieveMultimodalResult.context}")
}
