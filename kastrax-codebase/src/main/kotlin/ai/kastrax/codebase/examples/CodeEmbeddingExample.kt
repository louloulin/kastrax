package ai.kastrax.codebase.examples

import ai.kastrax.codebase.embedding.CodeChunker
import ai.kastrax.codebase.embedding.CodeChunkerConfig
import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.embedding.CodeEmbeddingServiceConfig
import ai.kastrax.codebase.embedding.EmbeddingModelManager
import ai.kastrax.codebase.embedding.EmbeddingModelManagerConfig
import ai.kastrax.codebase.embedding.FastEmbeddingService
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.math.sqrt

/**
 * 代码嵌入示例
 * TODO: 暂时注释掉，等待修复EmbeddingService相关问题
 */
/*
object CodeEmbeddingExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取要处理的目录路径
        val directoryPath = if (args.isNotEmpty()) {
            Path(args[0])
        } else {
            // 默认使用当前目录
            Path(".")
        }

        println("开始处理目录: $directoryPath")

        // 创建基础嵌入服务
        val baseEmbeddingService = FastEmbeddingService.create()

        // 创建代码嵌入服务
        val codeEmbeddingService = CodeEmbeddingService(
            baseEmbeddingService = baseEmbeddingService,
            config = CodeEmbeddingServiceConfig(
                cacheSize = 1000,
                batchSize = 32
            )
        )

        // 创建嵌入模型管理器
        val modelManager = EmbeddingModelManager(
            config = EmbeddingModelManagerConfig(
                defaultVersion = "v1",
                cacheSize = 1000
            )
        )

        // 注册模型版本
        modelManager.registerModelVersion("v1", codeEmbeddingService, true)

        // 创建代码分块器
        val chunker = CodeChunker(
            config = CodeChunkerConfig(
                maxChunkSize = 1000,
                minChunkSize = 100,
                overlap = 50,
                preserveSemantics = true
            )
        )

        // 查找代码文件
        val codeFiles = findCodeFiles(directoryPath)
        println("找到 ${codeFiles.size} 个代码文件")

        // 处理每个文件
        val allChunks = mutableListOf<Pair<String, FloatArray>>()

        for (file in codeFiles.take(10)) { // 仅处理前 10 个文件作为示例
            println("\n处理文件: ${file.name}")

            // 分割代码
            val chunks = chunker.chunkFile(file)
            println("分割为 ${chunks.size} 个代码块")

            // 生成嵌入
            for ((index, chunk) in chunks.withIndex()) {
                val embedding = modelManager.embed(chunk.content)
                allChunks.add(chunk.content to embedding)

                println("块 ${index + 1}/${chunks.size} 嵌入完成，维度: ${embedding.size}")
            }
        }

        // 演示相似度搜索
        if (allChunks.isNotEmpty()) {
            println("\n演示相似度搜索:")

            // 选择一个查询块
            val queryIndex = allChunks.indices.random()
            val (queryText, queryEmbedding) = allChunks[queryIndex]

            println("\n查询块:")
            println(queryText.take(200) + "...")

            // 计算相似度
            val similarities = allChunks.mapIndexed { index, (text, embedding) ->
                Triple(index, text, cosineSimilarity(queryEmbedding, embedding))
            }

            // 排序并显示最相似的块
            val topSimilar = similarities
                .filter { it.first != queryIndex } // 排除查询块本身
                .sortedByDescending { it.third }
                .take(3)

            println("\n最相似的块:")
            topSimilar.forEachIndexed { index, (_, text, similarity) ->
                println("${index + 1}. 相似度: $similarity")
                println(text.take(200) + "...")
                println()
            }
        }

        // 打印缓存统计
        val cacheStats = codeEmbeddingService.getCacheStats()
        println("\n嵌入缓存命中率: ${cacheStats * 100}%")

        println("\n代码嵌入示例完成")
    }

    /**
     * 查找代码文件
     *
     * @param directory 目录路径
     * @return 代码文件列表
     */
    private fun findCodeFiles(directory: Path): List<Path> {
        val codeExtensions = setOf(
            "java", "kt", "kts", "scala", "groovy",
            "py", "js", "ts", "jsx", "tsx",
            "html", "css", "scss", "less",
            "c", "cpp", "h", "hpp", "cs", "go", "rs",
            "php", "rb", "swift", "m", "mm"
        )

        return Files.walk(directory)
            .filter { it.isRegularFile() }
            .filter { it.extension.lowercase() in codeExtensions }
            .toList()
    }

    /**
     * 计算余弦相似度
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 余弦相似度
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "向量维度不匹配: ${a.size} != ${b.size}" }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0.0
        }
    }
}*/
