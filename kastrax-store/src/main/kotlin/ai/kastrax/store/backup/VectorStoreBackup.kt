package ai.kastrax.store.backup

import ai.kastrax.store.IndexStats
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable as KotlinxSerializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private val logger = KotlinLogging.logger {}

/**
 * 向量存储备份工具类。
 */
object VectorStoreBackup {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * 备份向量存储。
     *
     * @param vectorStore 向量存储
     * @param backupDir 备份目录
     * @param indexNames 要备份的索引名称列表，如果为空则备份所有索引
     * @param includeVectors 是否包含向量数据
     * @return 备份文件路径
     */
    suspend fun backup(
        vectorStore: VectorStore,
        backupDir: String,
        indexNames: List<String>? = null,
        includeVectors: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        // 创建备份目录
        val backupPath = Paths.get(backupDir)
        Files.createDirectories(backupPath)

        // 获取要备份的索引列表
        val indexes = indexNames ?: vectorStore.listIndexes()
        if (indexes.isEmpty()) {
            throw IllegalStateException("No indexes to backup")
        }

        // 创建备份文件
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = backupPath.resolve("vector_store_backup_$timestamp.zip").toString()

        ZipOutputStream(Files.newOutputStream(Paths.get(backupFile))).use { zipOut ->
            // 备份索引元数据
            val indexMetadata = indexes.mapNotNull { indexName ->
                try {
                    val stats = vectorStore.describeIndex(indexName)
                    IndexMetadata(
                        name = indexName,
                        dimension = stats.dimension,
                        metric = stats.metric?.name ?: "COSINE",
                        count = stats.count
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to get metadata for index $indexName" }
                    null
                }
            }

            // 写入索引元数据
            val metadataJson = json.encodeToString(indexMetadata)
            zipOut.putNextEntry(ZipEntry("index_metadata.json"))
            zipOut.write(metadataJson.toByteArray())
            zipOut.closeEntry()

            // 备份每个索引的数据
            indexes.forEach { indexName ->
                try {
                    // 获取索引数据
                    val indexData = getIndexData(vectorStore, indexName, includeVectors)

                    // 写入索引数据
                    val indexDataJson = json.encodeToString(indexData)
                    zipOut.putNextEntry(ZipEntry("$indexName.json"))
                    zipOut.write(indexDataJson.toByteArray())
                    zipOut.closeEntry()

                    logger.info { "Backed up index $indexName with ${indexData.vectors.size} vectors" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to backup index $indexName" }
                }
            }
        }

        logger.info { "Backup completed: $backupFile" }
        return@withContext backupFile
    }

    /**
     * 恢复向量存储。
     *
     * @param vectorStore 向量存储
     * @param backupFile 备份文件路径
     * @param indexNames 要恢复的索引名称列表，如果为空则恢复所有索引
     * @param recreateIndexes 是否重新创建索引
     * @return 恢复的索引数量
     */
    suspend fun restore(
        vectorStore: VectorStore,
        backupFile: String,
        indexNames: List<String>? = null,
        recreateIndexes: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        var restoredCount = 0

        ZipFile(backupFile).use { zipFile ->
            // 读取索引元数据
            val metadataEntry = zipFile.getEntry("index_metadata.json")
                ?: throw IllegalStateException("Invalid backup file: missing index_metadata.json")

            val metadataJson = zipFile.getInputStream(metadataEntry).bufferedReader().use { it.readText() }
            val indexMetadata = json.decodeFromString<List<IndexMetadata>>(metadataJson)

            // 过滤要恢复的索引
            val indexesToRestore = if (indexNames != null) {
                indexMetadata.filter { it.name in indexNames }
            } else {
                indexMetadata
            }

            if (indexesToRestore.isEmpty()) {
                logger.warn { "No indexes to restore" }
                return@withContext 0
            }

            // 恢复每个索引
            indexesToRestore.forEach { metadata ->
                try {
                    // 检查索引是否存在
                    val indexExists = vectorStore.listIndexes().contains(metadata.name)

                    // 如果需要重新创建索引或索引不存在，则创建索引
                    if (recreateIndexes || !indexExists) {
                        if (indexExists) {
                            // 删除现有索引
                            vectorStore.deleteIndex(metadata.name)
                        }

                        // 创建新索引
                        val metric = SimilarityMetric.valueOf(metadata.metric)
                        vectorStore.createIndex(metadata.name, metadata.dimension, metric)
                    }

                    // 读取索引数据
                    val dataEntry = zipFile.getEntry("${metadata.name}.json")
                        ?: throw IllegalStateException("Invalid backup file: missing ${metadata.name}.json")

                    val dataJson = zipFile.getInputStream(dataEntry).bufferedReader().use { it.readText() }
                    val indexData = json.decodeFromString<IndexData>(dataJson)

                    // 恢复向量数据
                    if (indexData.vectors.isNotEmpty()) {
                        val vectors = indexData.vectors.map { it.vector }
                        val metadata = indexData.vectors.map { it.metadata }
                        val ids = indexData.vectors.map { it.id }

                        // 批量添加向量
                        vectorStore.batchUpsert(indexData.indexName, vectors, metadata, ids)
                    }

                    logger.info { "Restored index ${indexData.indexName} with ${indexData.vectors.size} vectors" }
                    restoredCount++
                } catch (e: Exception) {
                    logger.error(e) { "Failed to restore index ${metadata.name}" }
                }
            }
        }

        logger.info { "Restore completed: $restoredCount indexes restored" }
        return@withContext restoredCount
    }

    /**
     * 获取索引数据。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param includeVectors 是否包含向量数据
     * @return 索引数据
     */
    private suspend fun getIndexData(
        vectorStore: VectorStore,
        indexName: String,
        includeVectors: Boolean
    ): IndexData = coroutineScope {
        // 获取索引信息
        val stats = vectorStore.describeIndex(indexName)

        // 如果索引为空，则返回空数据
        if (stats.count == 0) {
            val metadata = IndexMetadata(
                name = indexName,
                dimension = stats.dimension,
                metric = stats.metric?.name ?: "COSINE",
                count = 0
            )
            return@coroutineScope IndexData(indexName, metadata, emptyList())
        }

        // 查询所有向量
        val batchSize = 1000
        val batches = (0 until stats.count).chunked(batchSize)

        // 创建测试向量
        val testVector = FloatArray(stats.dimension) { 0f }
        testVector[0] = 1f

        // 并行查询所有向量
        val vectorBatches = batches.map { batchIndices ->
            async {
                try {
                    // 查询一批向量
                    val results = vectorStore.query(
                        indexName = indexName,
                        queryVector = testVector,
                        topK = batchSize,
                        filter = null,
                        includeVectors = includeVectors
                    )

                    // 转换为向量数据
                    results.map { result ->
                        VectorData(
                            id = result.id,
                            vector = result.vector ?: FloatArray(0),
                            metadata = result.metadata ?: emptyMap()
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to query vectors for index $indexName, batch $batchIndices" }
                    emptyList()
                }
            }
        }.awaitAll()

        // 合并所有批次
        val vectors = vectorBatches.flatten()

        val metadata = IndexMetadata(
            name = indexName,
            dimension = stats.dimension,
            metric = stats.metric?.name ?: "COSINE",
            count = vectors.size
        )
        return@coroutineScope IndexData(indexName, metadata, vectors)
    }

    /**
     * 列出备份文件。
     *
     * @param backupDir 备份目录
     * @return 备份文件列表
     */
    fun listBackups(backupDir: String): List<BackupInfo> {
        val backupPath = Paths.get(backupDir)
        if (!Files.exists(backupPath)) {
            return emptyList()
        }

        return Files.list(backupPath)
            .filter { it.toString().endsWith(".zip") }
            .map { path ->
                val file = path.toFile()
                val timestamp = try {
                    val fileName = file.name
                    val pattern = "vector_store_backup_(\\d{8}_\\d{6})\\.zip".toRegex()
                    val matchResult = pattern.find(fileName)
                    val timestampStr = matchResult?.groupValues?.get(1)
                    if (timestampStr != null) {
                        LocalDateTime.parse(
                            timestampStr,
                            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        )
                    } else {
                        LocalDateTime.ofEpochSecond(file.lastModified() / 1000, 0, java.time.ZoneOffset.UTC)
                    }
                } catch (e: Exception) {
                    LocalDateTime.ofEpochSecond(file.lastModified() / 1000, 0, java.time.ZoneOffset.UTC)
                }

                // 读取索引元数据
                val indexCount = try {
                    ZipFile(file).use { zipFile ->
                        val metadataEntry = zipFile.getEntry("index_metadata.json")
                        if (metadataEntry != null) {
                            val metadataJson = zipFile.getInputStream(metadataEntry).bufferedReader().use { it.readText() }
                            val indexMetadata = json.decodeFromString<List<IndexMetadata>>(metadataJson)
                            indexMetadata.size
                        } else {
                            0
                        }
                    }
                } catch (e: Exception) {
                    0
                }

                BackupInfo(
                    file = file.absolutePath,
                    timestamp = timestamp,
                    size = file.length(),
                    indexCount = indexCount
                )
            }
            .sorted(compareByDescending { it.timestamp })
            .toList()
    }

    /**
     * 删除备份文件。
     *
     * @param backupFile 备份文件路径
     * @return 是否成功删除
     */
    fun deleteBackup(backupFile: String): Boolean {
        val file = File(backupFile)
        if (!file.exists() || !file.isFile) {
            return false
        }

        return file.delete()
    }
}

/**
 * 索引元数据。
 *
 * @property name 索引名称
 * @property dimension 向量维度
 * @property metric 相似度度量方式
 * @property count 向量数量
 */
@KotlinxSerializable
data class IndexMetadata(
    val name: String,
    val dimension: Int,
    val metric: String,
    val count: Int
)

/**
 * 索引数据。
 *
 * @property indexName 索引名称
 * @property metadata 索引元数据
 * @property vectors 向量数据列表
 */
@KotlinxSerializable
data class IndexData(
    val indexName: String,
    val metadata: IndexMetadata,
    val vectors: List<VectorData>
)

/**
 * 向量数据。
 *
 * @property id 向量 ID
 * @property vector 向量
 * @property metadata 元数据
 */
@KotlinxSerializable
data class VectorData(
    val id: String,
    val vector: FloatArray,
    val metadata: Map<String, @Contextual Any>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorData

        if (id != other.id) return false
        if (!vector.contentEquals(other.vector)) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * 备份信息。
 *
 * @property file 备份文件路径
 * @property timestamp 备份时间
 * @property size 备份文件大小
 * @property indexCount 索引数量
 */
data class BackupInfo(
    val file: String,
    val timestamp: LocalDateTime,
    val size: Long,
    val indexCount: Int
)
