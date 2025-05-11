package ai.kastrax.store

import ai.kastrax.store.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 向量存储基类，提供一些通用实现。
 */
abstract class BaseVectorStore : VectorStore {
    /**
     * 批量添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表
     * @param batchSize 批处理大小
     * @return 向量 ID 列表
     */
    override suspend fun batchUpsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>>,
        ids: List<String>?,
        batchSize: Int
    ): List<String> {
        logger.debug { "Batch upserting ${vectors.size} vectors to index $indexName with batch size $batchSize" }

        val actualIds = ids ?: List(vectors.size) { generateId() }
        val actualMetadata = if (metadata.isEmpty()) List(vectors.size) { emptyMap() } else metadata

        val results = mutableListOf<String>()

        // 分批处理
        vectors.chunked(batchSize).forEachIndexed { batchIndex, batchVectors ->
            val startIdx = batchIndex * batchSize
            val endIdx = minOf(startIdx + batchVectors.size, vectors.size)

            val batchIds = actualIds.subList(startIdx, endIdx)
            val batchMetadata = actualMetadata.subList(startIdx, endIdx)

            logger.debug { "Processing batch ${batchIndex + 1}: ${batchVectors.size} vectors" }

            val batchResults = upsert(indexName, batchVectors, batchMetadata, batchIds)
            results.addAll(batchResults)
        }

        return results
    }

    /**
     * 更新向量。
     *
     * @param indexName 索引名称
     * @param id 向量 ID
     * @param vector 新向量
     * @param metadata 新元数据
     * @return 是否成功更新
     */
    override suspend fun updateVector(
        indexName: String,
        id: String,
        vector: FloatArray?,
        metadata: Map<String, Any>?
    ): Boolean {
        logger.debug { "Updating vector $id in index $indexName" }

        if (vector == null && metadata == null) {
            logger.warn { "Both vector and metadata are null, nothing to update" }
            return false
        }

        // 如果只更新元数据
        if (vector == null && metadata != null) {
            // 获取现有向量
            val results = query(indexName, FloatArray(0), 1, mapOf("id" to id), true)

            if (results.isEmpty()) {
                // 尝试使用空向量和空过滤条件获取所有向量，然后手动过滤
                val allResults = query(indexName, FloatArray(0), 1000, null, true)
                val filteredResults = allResults.filter { it.id == id }

                if (filteredResults.isEmpty()) {
                    logger.warn { "Vector $id not found in index $indexName" }
                    return false
                }

                val existingVector = filteredResults.first().vector

                if (existingVector == null) {
                    logger.warn { "Vector $id found but has no vector data" }
                    return false
                }

                // 获取现有元数据并合并
                val existingMetadata = filteredResults.first().metadata ?: emptyMap()
                val mergedMetadata = existingMetadata.toMutableMap().apply { putAll(metadata) }

                // 更新向量和元数据
                return upsert(indexName, listOf(existingVector), listOf(mergedMetadata), listOf(id)).isNotEmpty()
            }

            val existingVector = results.first().vector

            if (existingVector == null) {
                logger.warn { "Vector $id found but has no vector data" }
                return false
            }

            // 获取现有元数据并合并
            val existingMetadata = results.first().metadata ?: emptyMap()
            val mergedMetadata = existingMetadata.toMutableMap().apply { putAll(metadata) }

            // 更新向量和元数据
            return upsert(indexName, listOf(existingVector), listOf(mergedMetadata), listOf(id)).isNotEmpty()
        }

        // 如果只更新向量
        if (vector != null && metadata == null) {
            // 获取现有元数据
            val results = query(indexName, FloatArray(0), 1, mapOf("id" to id), false)

            if (results.isEmpty()) {
                // 尝试使用空向量和空过滤条件获取所有向量，然后手动过滤
                val allResults = query(indexName, FloatArray(0), 1000, null, false)
                val filteredResults = allResults.filter { it.id == id }

                if (filteredResults.isEmpty()) {
                    logger.warn { "Vector $id not found in index $indexName" }
                    return false
                }

                val existingMetadata = filteredResults.first().metadata

                // 更新向量和元数据
                return upsert(indexName, listOf(vector), listOf(existingMetadata ?: emptyMap()), listOf(id)).isNotEmpty()
            }

            val existingMetadata = results.first().metadata

            // 更新向量和元数据
            return upsert(indexName, listOf(vector), listOf(existingMetadata ?: emptyMap()), listOf(id)).isNotEmpty()
        }

        // 检查向量是否存在
        val results = query(indexName, FloatArray(0), 1, mapOf("id" to id), false)
        if (results.isEmpty()) {
            // 尝试使用空向量和空过滤条件获取所有向量，然后手动过滤
            val allResults = query(indexName, FloatArray(0), 1000, null, false)
            val filteredResults = allResults.filter { it.id == id }

            if (filteredResults.isEmpty()) {
                logger.warn { "Vector $id not found in index $indexName" }
                return false
            }
        }

        // 更新向量和元数据
        return upsert(indexName, listOf(vector!!), listOf(metadata!!), listOf(id)).isNotEmpty()
    }

    /**
     * 生成唯一 ID。
     *
     * @return 唯一 ID
     */
    protected fun generateId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
