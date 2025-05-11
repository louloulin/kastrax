package ai.kastrax.codebase.examples

import ai.kastrax.codebase.embedding.CodeChunker
import ai.kastrax.codebase.embedding.CodeChunkerConfig
import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.embedding.CodeEmbeddingServiceConfig
import ai.kastrax.codebase.store.CodeVectorStore
import ai.kastrax.codebase.store.CodeVectorStoreConfig
import ai.kastrax.codebase.store.CompressedVectorStore
import ai.kastrax.codebase.store.CompressedVectorStoreConfig
import ai.kastrax.codebase.store.CompressionMethod
import ai.kastrax.codebase.store.MultiTenantVectorStore
import ai.kastrax.codebase.store.MultiTenantVectorStoreConfig
import ai.kastrax.codebase.store.ShardedVectorStore
import ai.kastrax.codebase.store.ShardedVectorStoreConfig
import ai.kastrax.store.VectorStoreFactory
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
 * 向量存储示例
 */
object VectorStoreExample {

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
        val embeddingService = baseEmbeddingService

        // 创建代码嵌入服务
        val codeEmbeddingService = CodeEmbeddingService(
            baseEmbeddingService = embeddingService,
            config = CodeEmbeddingServiceConfig(
                cacheSize = 1000,
                batchSize = 32
            )
        )

        // 创建代码分块器
        val chunker = CodeChunker(
            config = CodeChunkerConfig(
                maxChunkSize = 1000,
                minChunkSize = 100,
                overlap = 50,
                preserveSemantics = true
            )
        )

        // 创建基础向量存储
        val baseVectorStore = Any()

        // 创建压缩向量存储
        val compressedStore = CompressedVectorStore(
            baseVectorStore = baseVectorStore,
            config = CompressedVectorStoreConfig(
                compressionMethod = CompressionMethod.SCALAR_QUANTIZATION,
                quantizationBits = 8
            )
        )

        // 创建代码向量存储
        val codeVectorStore = CodeVectorStore(
            baseVectorStore = baseVectorStore,
            config = CodeVectorStoreConfig(
                maxVectors = 10000,
                dimension = codeEmbeddingService.dimension,
                distanceThreshold = 0.6
            )
        )

        // 创建分片向量存储
        val shardedStore = ShardedVectorStore(
            shardStoreFactory = { shardId ->
                VectorStoreFactory.createInMemoryVectorStore()
            },
            config = ShardedVectorStoreConfig(
                shardCount = 2,
                replicaCount = 1,
                maxVectorsPerShard = 5000
            )
        )

        // 创建多租户向量存储
        val multiTenantStore = MultiTenantVectorStore(
            baseVectorStoreFactory = { tenantId ->
                VectorStoreFactory.createInMemoryVectorStore()
            },
            config = MultiTenantVectorStoreConfig(
                maxTenantsInMemory = 3,
                maxVectorsPerTenant = 1000
            )
        )

        // 查找代码文件
        val codeFiles = findCodeFiles(directoryPath)
        println("找到 ${codeFiles.size} 个代码文件")

        // 处理每个文件
        val processedFiles = mutableListOf<Path>()

        for (file in codeFiles.take(10)) { // 仅处理前 10 个文件作为示例
            println("\n处理文件: ${file.name}")

            // 分割代码
            val chunks = chunker.chunkFile(file)
            println("分割为 ${chunks.size} 个代码块")

            // 为每个块生成嵌入并添加到不同的存储中
            for ((index, chunk) in chunks.withIndex()) {
                // 生成嵌入
                val embedding = codeEmbeddingService.embed(chunk.content)

                // 添加到代码向量存储
                val id1 = codeVectorStore.addVector(embedding, chunk.metadata)

                // 添加到压缩向量存储
                val id2 = compressedStore.addVector(embedding, chunk.metadata)

                // 添加到分片向量存储
                val id3 = shardedStore.addVector(embedding, chunk.metadata)

                // 添加到多租户向量存储（使用文件扩展名作为租户ID）
                val tenantId = file.extension.ifEmpty { "txt" }
                val id4 = multiTenantStore.addVector(tenantId, embedding, chunk.metadata)

                println("块 ${index + 1}/${chunks.size} 添加到所有存储")
            }

            processedFiles.add(file)
        }

        // 演示搜索
        if (processedFiles.isNotEmpty()) {
            // 选择一个查询文件
            val queryFile = processedFiles.random()
            println("\n使用文件进行查询: ${queryFile.name}")

            // 读取文件内容
            val queryContent = Files.readString(queryFile)

            // 生成嵌入
            val queryEmbedding = codeEmbeddingService.embed(queryContent)

            // 在代码向量存储中搜索
            println("\n在代码向量存储中搜索:")
            val results1 = codeVectorStore.searchVector(queryEmbedding, limit = 3)
            results1.forEachIndexed { index, result ->
                println("${index + 1}. 文件: ${result.vector.metadata["path"]}")
                println("   相似度: ${result.score}")
            }

            // 在压缩向量存储中搜索
            println("\n在压缩向量存储中搜索:")
            val results2 = compressedStore.searchVector(queryEmbedding, limit = 3)
            results2.forEachIndexed { index, result ->
                println("${index + 1}. 文件: ${result.vector.metadata["path"]}")
                println("   相似度: ${result.score}")
            }

            // 在分片向量存储中搜索
            println("\n在分片向量存储中搜索:")
            val results3 = shardedStore.searchVector(queryEmbedding, limit = 3)
            results3.forEachIndexed { index, result ->
                println("${index + 1}. 文件: ${result.vector.metadata["path"]}")
                println("   相似度: ${result.score}")
            }

            // 在多租户向量存储中搜索
            val queryTenantId = queryFile.extension.ifEmpty { "txt" }
            println("\n在多租户向量存储中搜索 (租户: $queryTenantId):")
            val results4 = multiTenantStore.searchVector(queryTenantId, queryEmbedding, limit = 3)
            results4.forEachIndexed { index, result ->
                println("${index + 1}. 文件: ${result.vector.metadata["path"]}")
                println("   相似度: ${result.score}")
            }
        }

        // 打印存储统计
        println("\n存储统计:")
        println("代码向量存储: ${codeVectorStore.getVectorCount()} 个向量")
        println("压缩向量存储: ${compressedStore.getVectorCount()} 个向量")
        println("压缩率: ${compressedStore.getCompressionRatio()}")
        println("分片向量存储: ${shardedStore.getVectorCount()} 个向量")
        println("分片信息: ${shardedStore.getShardInfo()}")
        println("多租户向量存储: ${multiTenantStore.getTenantStats()}")

        println("\n向量存储示例完成")
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
}
