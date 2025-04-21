package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.Embedding
import ai.kastrax.rag.embedding.EmbeddedDocument
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * 使用 FAISS 的向量存储实现。
 *
 * FAISS (Facebook AI Similarity Search) 是一个用于高效相似性搜索和密集向量聚类的库。
 * 这个实现使用 JNI 绑定来访问 FAISS 的功能。
 *
 * 注意：这个类需要 FAISS 库和 JNI 绑定才能正常工作。
 * 请参考文档了解如何安装和配置 FAISS。
 * 在没有 FAISS 的情况下，请使用 InMemoryVectorStore 作为替代。
 *
 * @property dimension 向量维度
 * @property indexType 索引类型，可以是 "Flat"（精确搜索）或 "IVFFlat"（近似搜索）
 * @property metric 距离度量，可以是 "L2"（欧几里得距离）或 "IP"（内积，用于余弦相似度）
 * @property nlist 对于 IVFFlat 索引，聚类中心的数量
 * @property nprobe 对于 IVFFlat 索引，搜索时探测的聚类数量
 */
class FaissVectorStore(
    private val dimension: Int,
    private val indexType: String = "Flat",
    private val metric: String = "IP",
    private val nlist: Int = 100,
    private val nprobe: Int = 10
) : RagVectorStore {

    // 使用 JNI 加载 FAISS 库
    init {
        try {
            System.loadLibrary("faiss_jni")
            logger.info { "FAISS JNI library loaded successfully" }
        } catch (e: UnsatisfiedLinkError) {
            logger.error(e) { "Failed to load FAISS JNI library" }
            throw RuntimeException("Failed to load FAISS JNI library. Make sure it's installed and in the library path.", e)
        }
    }

    // 索引 ID
    private val indexId = AtomicInteger(0)

    // 存储文档和索引的映射
    private val documents = ConcurrentHashMap<Int, EmbeddedDocument>()

    // FAISS 索引指针
    private var indexPointer: Long = 0

    // 创建 FAISS 索引
    init {
        createIndex()
    }

    /**
     * 创建 FAISS 索引。
     */
    private fun createIndex() {
        val metricType = if (metric == "L2") 0 else 1 // 0 for L2, 1 for IP

        indexPointer = when (indexType) {
            "Flat" -> {
                // 创建精确搜索索引
                createFlatIndex(dimension, metricType)
            }
            "IVFFlat" -> {
                // 创建近似搜索索引
                createIVFFlatIndex(dimension, nlist, metricType)
            }
            else -> {
                throw IllegalArgumentException("Unsupported index type: $indexType")
            }
        }

        if (indexPointer == 0L) {
            throw RuntimeException("Failed to create FAISS index")
        }

        // 对于 IVFFlat 索引，设置 nprobe 参数
        if (indexType == "IVFFlat") {
            setNprobe(indexPointer, nprobe)
        }
    }

    /**
     * 添加嵌入文档到向量存储。
     *
     * @param documents 要添加的嵌入文档列表
     * @return 添加的文档数量
     */
    override suspend fun addEmbeddedDocuments(documents: List<EmbeddedDocument>): Int {
        if (documents.isEmpty()) {
            return 0
        }

        return withContext(Dispatchers.IO) {
            var addedCount = 0

            // 检查所有向量的维度是否一致
            for (doc in documents) {
                if (doc.embedding.vector.size != dimension) {
                    logger.warn { "Skipping document with incorrect embedding dimension: ${doc.embedding.vector.size} (expected $dimension)" }
                    continue
                }

                val id = indexId.getAndIncrement()
                this@FaissVectorStore.documents[id] = doc

                // 将向量添加到 FAISS 索引
                val vector = doc.embedding.vector.toFloatArray()
                addVector(indexPointer, vector, id)

                addedCount++
            }

            addedCount
        }
    }

    /**
     * 使用嵌入向量搜索相似文档。
     *
     * @param embedding 查询嵌入向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    override suspend fun similaritySearch(
        embedding: Embedding,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        if (documents.isEmpty()) {
            return emptyList()
        }

        if (embedding.vector.size != dimension) {
            throw IllegalArgumentException("Query vector dimension (${embedding.vector.size}) does not match index dimension ($dimension)")
        }

        return withContext(Dispatchers.IO) {
            val queryVector = embedding.vector.toFloatArray()
            val k = limit

            // 执行搜索
            val results = search(indexPointer, queryVector, k)

            // 处理搜索结果
            val searchResults = mutableListOf<SearchResult>()
            for (i in 0 until results.size / 2) {
                val idx = results[i * 2].toInt()
                val score = results[i * 2 + 1].toDouble()

                // 对于 IP 度量，分数是内积，需要转换为余弦相似度
                val normalizedScore = if (metric == "IP") {
                    // 假设向量已经归一化，内积就是余弦相似度
                    score
                } else {
                    // 对于 L2 度量，分数是距离的平方，需要转换为相似度
                    1.0 / (1.0 + score)
                }

                if (normalizedScore >= minScore && idx >= 0) {
                    val doc = documents[idx]
                    if (doc != null) {
                        searchResults.add(SearchResult(doc.document, normalizedScore))
                    }
                }
            }

            // 按相似度降序排序
            searchResults.sortedByDescending { it.score }
        }
    }

    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    override suspend fun count(): Int {
        return documents.size
    }

    /**
     * 清空向量存储。
     */
    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            documents.clear()
            indexId.set(0)

            // 重置 FAISS 索引
            resetIndex(indexPointer)
        }
    }

    /**
     * 保存索引到文件。
     *
     * @param filePath 文件路径
     */
    suspend fun saveIndex(filePath: String) {
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            file.parentFile?.mkdirs()

            writeIndex(indexPointer, filePath)
            logger.info { "FAISS index saved to $filePath" }
        }
    }

    /**
     * 从文件加载索引。
     *
     * @param filePath 文件路径
     */
    suspend fun loadIndex(filePath: String) {
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalArgumentException("Index file does not exist: $filePath")
            }

            // 释放旧索引
            releaseIndex(indexPointer)

            // 加载新索引
            indexPointer = readIndex(filePath)
            if (indexPointer == 0L) {
                throw RuntimeException("Failed to load FAISS index from $filePath")
            }

            // 对于 IVFFlat 索引，设置 nprobe 参数
            if (indexType == "IVFFlat") {
                setNprobe(indexPointer, nprobe)
            }

            logger.info { "FAISS index loaded from $filePath" }
        }
    }

    /**
     * 释放资源。
     */
    fun close() {
        releaseIndex(indexPointer)
        logger.info { "FAISS index released" }
    }

    /**
     * 将 Float 列表转换为 FloatBuffer。
     */
    private fun List<Float>.toFloatBuffer(): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        for (value in this) {
            buffer.put(value)
        }
        buffer.rewind()

        return buffer
    }

    // 原生方法声明

    /**
     * 创建 Flat 索引。
     */
    private external fun createFlatIndex(dimension: Int, metricType: Int): Long

    /**
     * 创建 IVFFlat 索引。
     */
    private external fun createIVFFlatIndex(dimension: Int, nlist: Int, metricType: Int): Long

    /**
     * 设置 nprobe 参数。
     */
    private external fun setNprobe(indexPointer: Long, nprobe: Int)

    /**
     * 添加向量到索引。
     */
    private external fun addVector(indexPointer: Long, vector: FloatArray, id: Int)

    /**
     * 搜索向量。
     */
    private external fun search(indexPointer: Long, queryVector: FloatArray, k: Int): FloatArray

    /**
     * 重置索引。
     */
    private external fun resetIndex(indexPointer: Long)

    /**
     * 释放索引。
     */
    private external fun releaseIndex(indexPointer: Long)

    /**
     * 写入索引到文件。
     */
    private external fun writeIndex(indexPointer: Long, filePath: String)

    /**
     * 从文件读取索引。
     */
    private external fun readIndex(filePath: String): Long
}
