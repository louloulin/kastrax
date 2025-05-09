package ai.kastrax.rag.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.rag.embedding.FastEmbedKotlinEmbeddingService
import ai.kastrax.rag.embedding.cosineSimilarity
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * FastEmbed Kotlin 示例应用程序。
 * 这个示例展示了如何使用 FastEmbedKotlinEmbeddingService 生成文本嵌入。
 */
fun main() {
    println("FastEmbed Kotlin 示例")
    println("====================")

    runBlocking {
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
                println("嵌入数量: ${embeddings.size}")
                embeddings.forEachIndexed { index, embedding ->
                    println("嵌入 $index 维度: ${embedding.size}, 前 3 个值: ${embedding.take(3).toList()}")
                }
            }
            println("耗时: $time2 毫秒")

            // 测试文本相似度
            val text1 = "人工智能正在改变我们的生活方式。"
            val text2 = "AI 技术正在深刻影响我们的日常生活。"
            val text3 = "今天的天气真不错，阳光明媚。"

            println("\n计算文本相似度:")
            println("文本 1: $text1")
            println("文本 2: $text2")
            println("文本 3: $text3")

            val time3 = measureTimeMillis {
                val embedding1 = embeddingService.embed(text1)
                val embedding2 = embeddingService.embed(text2)
                val embedding3 = embeddingService.embed(text3)

                val similarity12 = embedding1.cosineSimilarity(embedding2)
                val similarity13 = embedding1.cosineSimilarity(embedding3)
                val similarity23 = embedding2.cosineSimilarity(embedding3)

                println("文本 1 和文本 2 的相似度: ${similarity12.format(4)}")
                println("文本 1 和文本 3 的相似度: ${similarity13.format(4)}")
                println("文本 2 和文本 3 的相似度: ${similarity23.format(4)}")
            }
            println("耗时: $time3 毫秒")
        }
    }
}

/**
 * 格式化浮点数，保留指定位数的小数。
 *
 * @param digits 小数位数
 * @return 格式化后的字符串
 */
fun Double.format(digits: Int): String = "%.${digits}f".format(this)
