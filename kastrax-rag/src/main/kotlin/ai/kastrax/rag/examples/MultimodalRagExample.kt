package ai.kastrax.rag.examples

import ai.kastrax.rag.multimodal.MultimodalDocument
import ai.kastrax.rag.multimodal.MultimodalDocumentType
import ai.kastrax.rag.multimodal.MultimodalRagFactory
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import ai.kastrax.store.document.DocumentSearchResult

/**
 * 多模态 RAG 示例，展示如何使用多模态 RAG 系统。
 */
fun main() = runBlocking {
    println("多模态 RAG 示例")
    println("===========")

    // 创建多模态 RAG 实例
    val apiKey = "your-openai-api-key" // 替换为你的 OpenAI API 密钥
    val multimodalRag = MultimodalRagFactory.createOpenAIMultimodalRag(apiKey)

    println("创建了多模态 RAG 实例")

    // 创建多模态文档
    val documents = listOf(
        MultimodalDocument(
            id = "1",
            content = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
            mediaUrls = listOf("https://example.com/images/ai.jpg"),
            mediaType = MultimodalDocumentType.IMAGE,
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        MultimodalDocument(
            id = "2",
            content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            mediaUrls = listOf("https://example.com/images/machine_learning.jpg"),
            mediaType = MultimodalDocumentType.IMAGE,
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        MultimodalDocument(
            id = "3",
            content = "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
            mediaUrls = listOf("https://example.com/images/deep_learning.jpg"),
            mediaType = MultimodalDocumentType.IMAGE,
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        MultimodalDocument(
            id = "4",
            content = "自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
            mediaUrls = listOf("https://example.com/audio/nlp_intro.mp3"),
            mediaType = MultimodalDocumentType.AUDIO,
            metadata = mapOf("source" to "NLP百科", "category" to "技术")
        ),
        MultimodalDocument(
            id = "5",
            content = "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息。",
            mediaUrls = listOf("https://example.com/videos/computer_vision.mp4"),
            mediaType = MultimodalDocumentType.VIDEO,
            metadata = mapOf("source" to "CV百科", "category" to "技术")
        )
    )

    println("创建了 ${documents.size} 个多模态文档")

    // 加载多模态文档
    val loadTime = measureTimeMillis {
        multimodalRag.loadMultimodalDocuments(documents)
    }

    println("加载多模态文档耗时: ${loadTime}ms")

    // 测试纯文本查询
    val textQuery = "人工智能的应用"
    println("\n执行纯文本查询: '$textQuery'")

    val textSearchTime = measureTimeMillis {
        val results = multimodalRag.multimodalSearch(textQuery, limit = 3) as List<DocumentSearchResult>
        println("结果数量: ${results.size}")

        results.forEachIndexed { index, result ->
            println("${index + 1}. ${result.document.content}")
            println("   分数: ${result.score}")
            println("   来源: ${result.document.metadata["source"]}")
            println("   媒体类型: ${result.document.metadata["mediaType"]}")
            println()
        }
    }

    println("纯文本查询耗时: ${textSearchTime}ms")

    // 测试多模态查询（文本 + 图像）
    val multimodalQuery = "深度学习"
    val imageUrl = "https://example.com/images/neural_network.jpg"
    println("\n执行多模态查询: 文本='$multimodalQuery', 图像=$imageUrl")

    val multimodalSearchTime = measureTimeMillis {
        val results = multimodalRag.multimodalSearch(
            textQuery = multimodalQuery,
            imageUrl = imageUrl,
            limit = 3
        )

        println("结果数量: ${results.size}")

        results.forEachIndexed { index, result ->
            println("${index + 1}. ${result.document.content}")
            println("   分数: ${result.score}")
            println("   来源: ${result.document.metadata["source"]}")
            println("   媒体类型: ${result.document.metadata["mediaType"]}")
            println()
        }
    }

    println("多模态查询耗时: ${multimodalSearchTime}ms")

    // 生成多模态上下文
    println("\n生成多模态上下文")

    val contextTime = measureTimeMillis {
        val context = multimodalRag.generateMultimodalContext(
            textQuery = "机器学习和深度学习的区别",
            imageUrl = "https://example.com/images/ml_vs_dl.jpg",
            limit = 3
        )

        println("上下文长度: ${context.length}")
        println("上下文预览: ${context.take(200)}...")
    }

    println("生成多模态上下文耗时: ${contextTime}ms")

    // 检索多模态上下文
    println("\n检索多模态上下文")

    val retrieveTime = measureTimeMillis {
        val result = multimodalRag.retrieveMultimodalContext(
            textQuery = "计算机视觉的应用",
            videoUrl = "https://example.com/videos/cv_applications.mp4",
            limit = 3
        )

        println("上下文长度: ${result.context.length}")
        println("上下文预览: ${result.context.take(200)}...")
        println("文档数量: ${result.documents.size}")

        result.documents.forEachIndexed { index, document ->
            println("${index + 1}. ${document.content.take(100)}...")
            println("   来源: ${document.metadata["source"]}")
            println("   媒体类型: ${document.metadata["mediaType"]}")
            println()
        }
    }

    println("检索多模态上下文耗时: ${retrieveTime}ms")

    println("\n示例完成")
}
