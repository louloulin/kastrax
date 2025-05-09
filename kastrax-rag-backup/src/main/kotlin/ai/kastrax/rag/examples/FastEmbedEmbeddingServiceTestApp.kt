package ai.kastrax.rag.examples

import ai.kastrax.rag.embedding.Embedding
import ai.kastrax.rag.embedding.FastEmbedEmbeddingService
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * 一个简单的测试应用程序，用于验证 FastEmbedEmbeddingService 的功能。
 */
fun main() = runBlocking {
    println("FastEmbed Embedding Service Test")
    println("================================")

    // 创建嵌入服务
    println("\n创建嵌入服务...")
    FastEmbedEmbeddingService(
        modelName = "BAAI/bge-small-zh-v1.5",
        dimensions = 384,
        maxLength = 512,
        normalize = true
    ).use { embeddingService ->
        println("嵌入服务创建成功")

        // 测试单个文本嵌入
        val text = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。"
        println("\n生成单个文本的嵌入向量:")
        println("文本: $text")

        val time1 = measureTimeMillis {
            val embedding = embeddingService.embed(text)
            println("嵌入维度: ${embedding.size}")
            println("前 5 个值: ${embedding.take(5).toList()}")
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
                println("嵌入 $index 维度: ${embedding.size}")
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

            val similarity12 = cosineSimilarity(embedding1, embedding2)
            val similarity13 = cosineSimilarity(embedding1, embedding3)

            println("文本 1 和文本 2 的相似度: $similarity12")
            println("文本 1 和文本 3 的相似度: $similarity13")
            println("相似度比较: ${if (similarity12 > similarity13) "相关文本的相似度更高" else "不相关文本的相似度更高"}")
        }
        println("耗时: $time3 毫秒")
    }

    println("\n测试完成!")
}

/**
 * 计算两个向量的余弦相似度。
 */
private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
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
