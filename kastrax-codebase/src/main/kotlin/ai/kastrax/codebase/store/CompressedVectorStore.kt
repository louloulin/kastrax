package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 压缩向量存储配置
 *
 * @property compressionMethod 压缩方法
 * @property quantizationBits 量化位数（用于标量量化）
 * @property pqDimensions 乘积量化维度（用于乘积量化）
 * @property pqClusters 乘积量化聚类数（用于乘积量化）
 */
data class CompressedVectorStoreConfig(
    val compressionMethod: CompressionMethod = CompressionMethod.SCALAR_QUANTIZATION,
    val quantizationBits: Int = 8,
    val pqDimensions: Int = 16,
    val pqClusters: Int = 256
)

/**
 * 压缩方法
 */
enum class CompressionMethod {
    /**
     * 标量量化
     */
    SCALAR_QUANTIZATION,
    
    /**
     * 乘积量化
     */
    PRODUCT_QUANTIZATION,
    
    /**
     * 二值化
     */
    BINARIZATION
}

/**
 * 压缩向量存储
 *
 * 实现向量压缩技术，减少存储空间需求
 *
 * @property baseVectorStore 基础向量存储
 * @property config 配置
 */
class CompressedVectorStore(
    private val baseVectorStore: VectorStore,
    private val config: CompressedVectorStoreConfig = CompressedVectorStoreConfig()
) {
    // 向量映射
    private val vectors = ConcurrentHashMap<String, ByteArray>()
    
    // 元数据映射
    private val metadata = ConcurrentHashMap<String, Map<String, Any>>()
    
    // 向量统计信息（用于标量量化）
    private var minValues: FloatArray? = null
    private var maxValues: FloatArray? = null
    
    // 乘积量化码本（用于乘积量化）
    private var pqCodebooks: Array<Array<FloatArray>>? = null
    
    // 互斥锁，用于写操作
    private val mutex = Mutex()
    
    /**
     * 添加向量
     *
     * @param vector 向量数据
     * @param metadata 元数据
     * @return 向量ID
     */
    suspend fun addVector(
        vector: FloatArray,
        metadata: Map<String, Any> = emptyMap()
    ): String = mutex.withLock {
        // 压缩向量
        val compressed = compressVector(vector)
        
        // 添加到基础向量存储
        val id = baseVectorStore.addVector(vector, metadata)
        
        // 添加到映射
        vectors[id] = compressed
        this.metadata[id] = metadata
        
        return@withLock id
    }
    
    /**
     * 批量添加向量
     *
     * @param vectors 向量数据列表
     * @param metadata 元数据列表
     * @return 向量ID列表
     */
    suspend fun addVectors(
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>> = List(vectors.size) { emptyMap() }
    ): List<String> = mutex.withLock {
        require(vectors.size == metadata.size) {
            "向量数量与元数据数量不匹配: ${vectors.size} != ${metadata.size}"
        }
        
        // 压缩向量
        val compressed = vectors.map { compressVector(it) }
        
        // 添加到基础向量存储
        val ids = baseVectorStore.addVectors(vectors, metadata)
        
        // 添加到映射
        ids.forEachIndexed { index, id ->
            this.vectors[id] = compressed[index]
            this.metadata[id] = metadata[index]
        }
        
        return@withLock ids
    }
    
    /**
     * 添加嵌入
     *
     * @param text 文本
     * @param metadata 元数据
     * @param embeddingService 嵌入服务
     * @return 向量ID
     */
    suspend fun addEmbedding(
        text: String,
        metadata: Map<String, Any> = emptyMap(),
        embeddingService: EmbeddingService
    ): String = withContext(Dispatchers.Default) {
        // 生成嵌入
        val embedding = embeddingService.embed(text)
        
        // 添加向量
        return@withContext addVector(embedding, metadata)
    }
    
    /**
     * 批量添加嵌入
     *
     * @param texts 文本列表
     * @param metadata 元数据列表
     * @param embeddingService 嵌入服务
     * @return 向量ID列表
     */
    suspend fun addEmbeddings(
        texts: List<String>,
        metadata: List<Map<String, Any>> = List(texts.size) { emptyMap() },
        embeddingService: EmbeddingService
    ): List<String> = withContext(Dispatchers.Default) {
        require(texts.size == metadata.size) {
            "文本数量与元数据数量不匹配: ${texts.size} != ${metadata.size}"
        }
        
        // 生成嵌入
        val embeddings = embeddingService.embedBatch(texts)
        
        // 添加向量
        return@withContext addVectors(embeddings, metadata)
    }
    
    /**
     * 搜索向量
     *
     * @param vector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun searchVector(
        vector: FloatArray,
        limit: Int = 10,
        minScore: Double = 0.0
    ): List<CodeSearchResult> = withContext(Dispatchers.Default) {
        // 使用基础向量存储搜索
        val results = baseVectorStore.searchVector(vector, limit, minScore)
        
        // 转换为代码搜索结果
        return@withContext results.mapNotNull { result ->
            val compressed = vectors[result.id] ?: return@mapNotNull null
            val meta = metadata[result.id] ?: emptyMap()
            
            // 解压向量
            val decompressed = decompressVector(compressed)
            
            // 创建代码向量
            val codeVector = CodeVector(
                id = result.id,
                vector = decompressed,
                metadata = meta
            )
            
            CodeSearchResult(
                vector = codeVector,
                score = result.score,
                distance = result.distance
            )
        }
    }
    
    /**
     * 搜索嵌入
     *
     * @param text 查询文本
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param embeddingService 嵌入服务
     * @return 搜索结果列表
     */
    suspend fun searchEmbedding(
        text: String,
        limit: Int = 10,
        minScore: Double = 0.0,
        embeddingService: EmbeddingService
    ): List<CodeSearchResult> = withContext(Dispatchers.Default) {
        // 生成嵌入
        val embedding = embeddingService.embed(text)
        
        // 搜索向量
        return@withContext searchVector(embedding, limit, minScore)
    }
    
    /**
     * 删除向量
     *
     * @param id 向量ID
     * @return 是否成功删除
     */
    suspend fun deleteVector(id: String): Boolean = mutex.withLock {
        // 从基础向量存储中删除
        val success = baseVectorStore.deleteVector(id)
        
        if (success) {
            // 从映射中移除
            vectors.remove(id)
            metadata.remove(id)
        }
        
        return@withLock success
    }
    
    /**
     * 批量删除向量
     *
     * @param ids 向量ID列表
     * @return 是否成功删除
     */
    suspend fun deleteVectors(ids: List<String>): Boolean = mutex.withLock {
        // 从基础向量存储中删除
        val success = baseVectorStore.deleteVectors(ids)
        
        if (success) {
            // 从映射中移除
            ids.forEach { id ->
                vectors.remove(id)
                metadata.remove(id)
            }
        }
        
        return@withLock success
    }
    
    /**
     * 获取向量
     *
     * @param id 向量ID
     * @return 代码向量，如果不存在则返回 null
     */
    suspend fun getVector(id: String): CodeVector? = withContext(Dispatchers.Default) {
        val compressed = vectors[id] ?: return@withContext null
        val meta = metadata[id] ?: emptyMap()
        
        // 解压向量
        val decompressed = decompressVector(compressed)
        
        return@withContext CodeVector(
            id = id,
            vector = decompressed,
            metadata = meta
        )
    }
    
    /**
     * 批量获取向量
     *
     * @param ids 向量ID列表
     * @return 代码向量列表
     */
    suspend fun getVectors(ids: List<String>): List<CodeVector> = withContext(Dispatchers.Default) {
        return@withContext ids.mapNotNull { id -> getVector(id) }
    }
    
    /**
     * 获取向量数量
     *
     * @return 向量数量
     */
    fun getVectorCount(): Int {
        return vectors.size
    }
    
    /**
     * 清空存储
     *
     * @return 是否成功清空
     */
    suspend fun clear(): Boolean = mutex.withLock {
        // 清空基础向量存储
        val success = baseVectorStore.clear()
        
        if (success) {
            // 清空映射
            vectors.clear()
            metadata.clear()
            
            // 重置统计信息
            minValues = null
            maxValues = null
            pqCodebooks = null
        }
        
        return@withLock success
    }
    
    /**
     * 压缩向量
     *
     * @param vector 向量数据
     * @return 压缩后的字节数组
     */
    private fun compressVector(vector: FloatArray): ByteArray {
        return when (config.compressionMethod) {
            CompressionMethod.SCALAR_QUANTIZATION -> compressWithScalarQuantization(vector)
            CompressionMethod.PRODUCT_QUANTIZATION -> compressWithProductQuantization(vector)
            CompressionMethod.BINARIZATION -> compressWithBinarization(vector)
        }
    }
    
    /**
     * 解压向量
     *
     * @param compressed 压缩后的字节数组
     * @return 解压后的向量数据
     */
    private fun decompressVector(compressed: ByteArray): FloatArray {
        return when (config.compressionMethod) {
            CompressionMethod.SCALAR_QUANTIZATION -> decompressWithScalarQuantization(compressed)
            CompressionMethod.PRODUCT_QUANTIZATION -> decompressWithProductQuantization(compressed)
            CompressionMethod.BINARIZATION -> decompressWithBinarization(compressed)
        }
    }
    
    /**
     * 使用标量量化压缩向量
     *
     * @param vector 向量数据
     * @return 压缩后的字节数组
     */
    private fun compressWithScalarQuantization(vector: FloatArray): ByteArray {
        // 初始化最小值和最大值
        if (minValues == null || maxValues == null) {
            minValues = FloatArray(vector.size) { Float.MAX_VALUE }
            maxValues = FloatArray(vector.size) { Float.MIN_VALUE }
        }
        
        // 更新最小值和最大值
        for (i in vector.indices) {
            minValues!![i] = minOf(minValues!![i], vector[i])
            maxValues!![i] = maxOf(maxValues!![i], vector[i])
        }
        
        // 计算量化步长
        val steps = FloatArray(vector.size) { i ->
            (maxValues!![i] - minValues!![i]) / ((1 shl config.quantizationBits) - 1)
        }
        
        // 量化向量
        val quantized = ByteArray(vector.size)
        for (i in vector.indices) {
            val step = steps[i]
            if (step > 0) {
                val value = ((vector[i] - minValues!![i]) / step).toInt()
                quantized[i] = value.toByte()
            } else {
                quantized[i] = 0
            }
        }
        
        return quantized
    }
    
    /**
     * 使用标量量化解压向量
     *
     * @param compressed 压缩后的字节数组
     * @return 解压后的向量数据
     */
    private fun decompressWithScalarQuantization(compressed: ByteArray): FloatArray {
        // 检查最小值和最大值是否已初始化
        if (minValues == null || maxValues == null) {
            return FloatArray(compressed.size)
        }
        
        // 计算量化步长
        val steps = FloatArray(compressed.size) { i ->
            (maxValues!![i] - minValues!![i]) / ((1 shl config.quantizationBits) - 1)
        }
        
        // 解量化向量
        val decompressed = FloatArray(compressed.size)
        for (i in compressed.indices) {
            val step = steps[i]
            if (step > 0) {
                val value = compressed[i].toInt() and 0xFF
                decompressed[i] = minValues!![i] + value * step
            } else {
                decompressed[i] = minValues!![i]
            }
        }
        
        return decompressed
    }
    
    /**
     * 使用乘积量化压缩向量
     *
     * @param vector 向量数据
     * @return 压缩后的字节数组
     */
    private fun compressWithProductQuantization(vector: FloatArray): ByteArray {
        // 初始化码本
        if (pqCodebooks == null) {
            initializeProductQuantizationCodebooks(vector.size)
        }
        
        // 将向量分成子向量
        val subVectorSize = vector.size / config.pqDimensions
        val subVectors = Array(config.pqDimensions) { i ->
            val start = i * subVectorSize
            val end = (i + 1) * subVectorSize
            vector.sliceArray(start until end)
        }
        
        // 量化子向量
        val quantized = ByteArray(config.pqDimensions)
        for (i in 0 until config.pqDimensions) {
            val subVector = subVectors[i]
            val codebook = pqCodebooks!![i]
            
            // 找到最近的码字
            var minDistance = Float.MAX_VALUE
            var nearestCluster = 0
            
            for (j in 0 until config.pqClusters) {
                val codeword = codebook[j]
                val distance = euclideanDistance(subVector, codeword)
                
                if (distance < minDistance) {
                    minDistance = distance
                    nearestCluster = j
                }
            }
            
            quantized[i] = nearestCluster.toByte()
        }
        
        return quantized
    }
    
    /**
     * 使用乘积量化解压向量
     *
     * @param compressed 压缩后的字节数组
     * @return 解压后的向量数据
     */
    private fun decompressWithProductQuantization(compressed: ByteArray): FloatArray {
        // 检查码本是否已初始化
        if (pqCodebooks == null) {
            return FloatArray(compressed.size * 10) // 假设每个子向量大小为 10
        }
        
        // 计算子向量大小
        val subVectorSize = pqCodebooks!![0][0].size
        
        // 解量化向量
        val decompressed = FloatArray(compressed.size * subVectorSize)
        
        for (i in compressed.indices) {
            val clusterIndex = compressed[i].toInt() and 0xFF
            val codeword = pqCodebooks!![i][clusterIndex]
            
            // 复制码字到结果向量
            val start = i * subVectorSize
            for (j in codeword.indices) {
                decompressed[start + j] = codeword[j]
            }
        }
        
        return decompressed
    }
    
    /**
     * 初始化乘积量化码本
     *
     * @param vectorSize 向量大小
     */
    private fun initializeProductQuantizationCodebooks(vectorSize: Int) {
        // 计算子向量大小
        val subVectorSize = vectorSize / config.pqDimensions
        
        // 初始化码本
        pqCodebooks = Array(config.pqDimensions) { i ->
            Array(config.pqClusters) { j ->
                // 随机初始化码字
                FloatArray(subVectorSize) { k ->
                    (Math.random() * 2 - 1).toFloat()
                }
            }
        }
        
        // 注意：在实际应用中，应该使用 K-means 聚类算法训练码本
        // 这里只是简单地随机初始化
    }
    
    /**
     * 使用二值化压缩向量
     *
     * @param vector 向量数据
     * @return 压缩后的字节数组
     */
    private fun compressWithBinarization(vector: FloatArray): ByteArray {
        // 计算向量的均值
        val mean = vector.average().toFloat()
        
        // 二值化向量
        val bitsPerByte = 8
        val byteCount = (vector.size + bitsPerByte - 1) / bitsPerByte
        val binarized = ByteArray(byteCount)
        
        for (i in vector.indices) {
            val byteIndex = i / bitsPerByte
            val bitIndex = i % bitsPerByte
            
            if (vector[i] > mean) {
                // 设置位
                binarized[byteIndex] = (binarized[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
        }
        
        return binarized
    }
    
    /**
     * 使用二值化解压向量
     *
     * @param compressed 压缩后的字节数组
     * @return 解压后的向量数据
     */
    private fun decompressWithBinarization(compressed: ByteArray): FloatArray {
        val bitsPerByte = 8
        val vectorSize = compressed.size * bitsPerByte
        val decompressed = FloatArray(vectorSize)
        
        for (i in 0 until vectorSize) {
            val byteIndex = i / bitsPerByte
            val bitIndex = i % bitsPerByte
            
            if (byteIndex < compressed.size) {
                val bit = (compressed[byteIndex].toInt() shr bitIndex) and 1
                decompressed[i] = if (bit == 1) 1.0f else -1.0f
            }
        }
        
        return decompressed
    }
    
    /**
     * 计算欧几里得距离
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 欧几里得距离
     */
    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "向量维度不匹配: ${a.size} != ${b.size}" }
        
        var sum = 0.0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        
        return sqrt(sum)
    }
    
    /**
     * 获取压缩率
     *
     * @return 压缩率（原始大小 / 压缩后大小）
     */
    fun getCompressionRatio(): Double {
        if (vectors.isEmpty()) {
            return 0.0
        }
        
        // 计算原始大小（假设每个向量元素为 4 字节）
        val originalSize = vectors.size * 4 * (minValues?.size ?: 0)
        
        // 计算压缩后大小
        val compressedSize = vectors.values.sumOf { it.size }
        
        return if (compressedSize > 0) {
            originalSize.toDouble() / compressedSize
        } else {
            0.0
        }
    }
    
    /**
     * 获取压缩方法
     *
     * @return 压缩方法
     */
    fun getCompressionMethod(): CompressionMethod {
        return config.compressionMethod
    }
}
