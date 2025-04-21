package ai.kastrax.rag.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.FastEmbedKotlinEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * 使用 FastEmbed Kotlin 嵌入服务的测试应用程序。
 */
fun main() = runBlocking {
    println("FastEmbed Kotlin Embedding Service Test")
    println("=======================================")

    // 设置测试模式，使用模拟实现
    System.setProperty("ai.kastrax.fastembed.test.mode", "true")

    // 创建嵌入服务
    println("\n创建嵌入服务...")
    FastEmbedKotlinEmbeddingService.create(
        model = EmbeddingModel.BGE_SMALL_ZH,
        showDownloadProgress = true
    ).use { embeddingService ->
        println("嵌入服务创建成功，嵌入维度: ${embeddingService.dimensions}")

        // 测试单个文本嵌入
        val text = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。"
        println("\n生成单个文本的嵌入向量:")
        println("文本: $text")

        val time1 = measureTimeMillis {
            val embedding = embeddingService.embed(text)
            println("嵌入维度: ${embedding.vector.size}")
            println("前 5 个值: ${embedding.vector.take(5)}")
        }
        println("耗时: $time1 毫秒")

        // 测试批量文本嵌入
        val texts = listOf(
            "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
            "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。"
        )
        println("\n生成批量文本的嵌入向量:")
        println("文本数量: ${texts.size}")

        val time2 = measureTimeMillis {
            val embeddings = embeddingService.embedBatch(texts)
            println("生成的嵌入向量数量: ${embeddings.size}")
            embeddings.forEachIndexed { index, embedding ->
                println("嵌入 $index 维度: ${embedding.vector.size}")
            }
        }
        println("耗时: $time2 毫秒")

        // 测试相似度
        val text1 = "人工智能是计算机科学的一个分支。"
        val text2 = "AI是计算机科学的一个领域。"
        val text3 = "今天天气真好，阳光明媚。"

        println("\n计算文本相似度:")
        println("文本 1: $text1")
        println("文本 2: $text2")
        println("文本 3: $text3")

        val time3 = measureTimeMillis {
            val embedding1 = embeddingService.embed(text1)
            val embedding2 = embeddingService.embed(text2)
            val embedding3 = embeddingService.embed(text3)

            val similarity12 = cosineSimilarity(embedding1.vector, embedding2.vector)
            val similarity13 = cosineSimilarity(embedding1.vector, embedding3.vector)

            println("文本 1 和文本 2 的相似度: $similarity12")
            println("文本 1 和文本 3 的相似度: $similarity13")
            println("相似度比较: ${if (similarity12 > similarity13) "相关文本的相似度更高" else "不相关文本的相似度更高"}")
        }
        println("耗时: $time3 毫秒")

        // 测试 RAG 系统
        println("\n测试 RAG 系统...")
        val vectorStore = InMemoryVectorStore()
        val rag = RAG(vectorStore, embeddingService)

        // 添加文档
        println("\n添加文档...")
        val documents = listOf(
            Document(
                content = "FastEmbed 是一个用于生成文本和图像嵌入向量的库。它支持多种语言，包括中文和英文。",
                metadata = mapOf(
                    "source" to "documentation",
                    "title" to "FastEmbed 简介"
                )
            ),
            Document(
                content = "向量嵌入是数据的数值表示，它捕获了语义含义，使得可以进行高效的相似性比较。",
                metadata = mapOf(
                    "source" to "documentation",
                    "title" to "向量嵌入概念"
                )
            ),
            Document(
                content = "FastEmbed 提供了一个简单的 API，用于使用最先进的模型生成嵌入向量。它支持多种语言，并针对性能进行了优化。",
                metadata = mapOf(
                    "source" to "documentation",
                    "title" to "FastEmbed API"
                )
            ),
            Document(
                content = "该库包括多个预训练模型，如 BGE（BAAI 通用嵌入）、All-MiniLM-L6-v2 和 E5，每个模型都有不同的特性和性能配置。",
                metadata = mapOf(
                    "source" to "documentation",
                    "title" to "FastEmbed 模型"
                )
            ),
            Document(
                content = "Kotlin 是一种现代编程语言，它使开发人员更加高效和愉快。",
                metadata = mapOf(
                    "source" to "documentation",
                    "title" to "Kotlin 简介"
                )
            )
        )

        val addedCount = vectorStore.addDocuments(documents, embeddingService)
        println("添加了 $addedCount 个文档")

        // 查询 RAG 系统
        val queries = listOf(
            "什么是 FastEmbed？",
            "向量嵌入是什么？",
            "FastEmbed 支持哪些模型？",
            "什么是 Kotlin？"
        )

        for (query in queries) {
            println("\n查询: $query")
            val results = rag.search(query, limit = 2)

            println("相关文档:")
            results.forEachIndexed { index, result ->
                println("${index + 1}. [相似度: ${result.score}] ${result.document.content}")
                println("   来源: ${result.document.metadata["source"]}, 标题: ${result.document.metadata["title"]}")
            }
        }
    }

    println("\n测试完成!")
}

/**
 * 计算两个向量的余弦相似度。
 */
private fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
    require(vec1.size == vec2.size) { "Vectors must have the same dimension" }

    var dotProduct = 0.0f
    var norm1 = 0.0f
    var norm2 = 0.0f

    for (i in vec1.indices) {
        dotProduct += vec1[i] * vec2[i]
        norm1 += vec1[i] * vec1[i]
        norm2 += vec2[i] * vec2[i]
    }

    norm1 = kotlin.math.sqrt(norm1)
    norm2 = kotlin.math.sqrt(norm2)

    return if (norm1 > 0 && norm2 > 0) {
        dotProduct / (norm1 * norm2)
    } else {
        0.0f
    }
}
