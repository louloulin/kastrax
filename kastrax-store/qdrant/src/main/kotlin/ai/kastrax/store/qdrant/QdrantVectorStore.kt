package ai.kastrax.store.qdrant

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.QueryResult
import ai.kastrax.store.SimilarityMetric
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Qdrant 向量存储实现。
 * 参考 mastra 的 QdrantVector 实现。
 *
 * @property host Qdrant 服务器主机
 * @property port Qdrant 服务器端口
 * @property apiKey Qdrant API 密钥
 * @property useTls 是否使用 TLS
 */
class QdrantVectorStore(
    private val host: String = "localhost",
    private val port: Int = 6333,
    private val apiKey: String? = null,
    private val useTls: Boolean = false
) : BaseVectorStore() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
    }

    private val protocol = if (useTls) "https" else "http"
    private val baseUrl = "$protocol://$host:$port"

    /**
     * 创建索引。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @param metric 相似度度量方式，默认为余弦相似度
     * @return 是否成功创建
     */
    override suspend fun createIndex(
        indexName: String,
        dimension: Int,
        metric: SimilarityMetric
    ): Boolean {
        try {
            // 检查集合是否已存在
            val collections = listIndexes()
            if (collections.contains(indexName)) {
                logger.debug { "Collection $indexName already exists" }
                return false
            }

            // 将 Kastrax 相似度度量方式转换为 Qdrant 相似度度量方式
            val qdrantMetric = when (metric) {
                SimilarityMetric.COSINE -> "Cosine"
                SimilarityMetric.EUCLIDEAN -> "Euclid"
                SimilarityMetric.DOT_PRODUCT -> "Dot"
            }

            // 创建集合
            val response = client.put("$baseUrl/collections/$indexName") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("vectors", buildJsonObject {
                            put("size", dimension)
                            put("distance", qdrantMetric)
                        })
                    }
                )
            }

            if (response.status.isSuccess()) {
                logger.debug { "Created collection $indexName with dimension $dimension and metric $metric" }
                return true
            } else {
                logger.error { "Failed to create collection $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating collection $indexName" }
            throw e
        }
    }

    /**
     * 向索引中添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表，如果为 null 则自动生成
     * @return 向量 ID 列表
     */
    override suspend fun upsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>>,
        ids: List<String>?
    ): List<String> {
        if (vectors.isEmpty()) {
            return emptyList()
        }

        try {
            // 生成或使用提供的 ID
            val vectorIds = ids ?: List(vectors.size) { UUID.randomUUID().toString() }

            // 确保元数据列表长度与向量列表长度相同
            val normalizedMetadata = if (metadata.size == vectors.size) {
                metadata
            } else {
                List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
            }

            // 构建请求体
            val requestBody = buildJsonObject {
                put("points", JsonArray(vectors.indices.map { i ->
                    buildJsonObject {
                        put("id", vectorIds[i])
                        put("vector", JsonArray(vectors[i].map { JsonPrimitive(it) }))
                        if (normalizedMetadata[i].isNotEmpty()) {
                            put("payload", buildJsonObject {
                                normalizedMetadata[i].forEach { (key, value) ->
                                    when (value) {
                                        is String -> put(key, value)
                                        is Number -> put(key, value.toDouble())
                                        is Boolean -> put(key, value)
                                        else -> put(key, value.toString())
                                    }
                                }
                            })
                        }
                    }
                }))
            }

            // 发送请求
            val response = client.put("$baseUrl/collections/$indexName/points") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Upserted ${vectors.size} vectors to collection $indexName" }
                return vectorIds
            } else {
                logger.error { "Failed to upsert vectors to collection $indexName: ${response.status}" }
                throw RuntimeException("Failed to upsert vectors: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error upserting vectors to collection $indexName" }
            throw e
        }
    }

    /**
     * 查询向量。
     *
     * @param indexName 索引名称
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @param filter 过滤条件
     * @param includeVectors 是否包含向量
     * @return 查询结果列表
     */
    override suspend fun query(
        indexName: String,
        queryVector: FloatArray,
        topK: Int,
        filter: Map<String, Any>?,
        includeVectors: Boolean
    ): List<QueryResult> {
        try {
            // 构建过滤器
            val filterJson = if (filter != null && filter.isNotEmpty()) {
                buildJsonObject {
                    put("must", JsonArray(filter.map { (key, value) ->
                        buildJsonObject {
                            put("key", key)
                            when (value) {
                                is String -> {
                                    put("match", buildJsonObject {
                                        put("value", value)
                                    })
                                }
                                is Number -> {
                                    put("range", buildJsonObject {
                                        put("gte", value.toDouble())
                                        put("lte", value.toDouble())
                                    })
                                }
                                is Boolean -> {
                                    put("match", buildJsonObject {
                                        put("value", value)
                                    })
                                }
                                is List<*> -> {
                                    put("match", buildJsonObject {
                                        put("any", JsonArray(value.map { JsonPrimitive(it.toString()) }))
                                    })
                                }
                                else -> {
                                    put("match", buildJsonObject {
                                        put("value", value.toString())
                                    })
                                }
                            }
                        }
                    }))
                }
            } else {
                null
            }

            // 构建请求体
            val requestBody = buildJsonObject {
                put("vector", JsonArray(queryVector.map { JsonPrimitive(it) }))
                put("limit", topK)
                put("with_payload", true)
                if (includeVectors) {
                    put("with_vector", true)
                }
                if (filterJson != null) {
                    put("filter", filterJson)
                }
            }

            // 发送请求
            val response = client.post("$baseUrl/collections/$indexName/points/search") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val results = responseBody["result"]?.jsonArray?.map { point ->
                    val id = point.jsonObject["id"]?.jsonPrimitive?.content ?: ""
                    val score = point.jsonObject["score"]?.jsonPrimitive?.double ?: 0.0
                    val payload = point.jsonObject["payload"]?.jsonObject?.map { (key, value) ->
                        key to when {
                            value.jsonPrimitive.isString -> value.jsonPrimitive.content
                            value.jsonPrimitive.isBoolean -> value.jsonPrimitive.boolean
                            else -> value.jsonPrimitive.double
                        }
                    }?.toMap() ?: emptyMap()
                    
                    val vector = if (includeVectors) {
                        point.jsonObject["vector"]?.jsonArray?.map { 
                            it.jsonPrimitive.float 
                        }?.toFloatArray()
                    } else {
                        null
                    }

                    QueryResult(
                        id = id,
                        score = score,
                        metadata = payload,
                        vector = vector
                    )
                } ?: emptyList()

                logger.debug { "Query returned ${results.size} results from collection $indexName" }
                return results
            } else {
                logger.error { "Failed to query collection $indexName: ${response.status}" }
                throw RuntimeException("Failed to query collection: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error querying collection $indexName" }
            throw e
        }
    }

    /**
     * 删除向量。
     *
     * @param indexName 索引名称
     * @param ids ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteVectors(indexName: String, ids: List<String>): Boolean {
        if (ids.isEmpty()) {
            return true
        }

        try {
            // 构建请求体
            val requestBody = buildJsonObject {
                put("points", JsonArray(ids.map { JsonPrimitive(it) }))
            }

            // 发送请求
            val response = client.post("$baseUrl/collections/$indexName/points/delete") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Deleted ${ids.size} vectors from collection $indexName" }
                return true
            } else {
                logger.error { "Failed to delete vectors from collection $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting vectors from collection $indexName" }
            throw e
        }
    }

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    override suspend fun deleteIndex(indexName: String): Boolean {
        try {
            // 发送请求
            val response = client.delete("$baseUrl/collections/$indexName") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Deleted collection $indexName" }
                return true
            } else {
                logger.error { "Failed to delete collection $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting collection $indexName" }
            throw e
        }
    }

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    override suspend fun describeIndex(indexName: String): IndexStats {
        try {
            // 发送请求
            val response = client.get("$baseUrl/collections/$indexName") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val result = responseBody["result"]?.jsonObject
                val config = result?.get("config")?.jsonObject
                val params = config?.get("params")?.jsonObject
                val vectors = params?.get("vectors")?.jsonObject
                
                val dimension = vectors?.get("size")?.jsonPrimitive?.int ?: 0
                val metricStr = vectors?.get("distance")?.jsonPrimitive?.content ?: "Cosine"
                val count = result?.get("vectors_count")?.jsonPrimitive?.int ?: 0
                
                // 将 Qdrant 相似度度量方式转换为 Kastrax 相似度度量方式
                val metric = when (metricStr) {
                    "Cosine" -> SimilarityMetric.COSINE
                    "Euclid" -> SimilarityMetric.EUCLIDEAN
                    "Dot" -> SimilarityMetric.DOT_PRODUCT
                    else -> SimilarityMetric.COSINE
                }

                logger.debug { "Retrieved stats for collection $indexName: dimension=$dimension, count=$count, metric=$metric" }
                return IndexStats(dimension, count, metric)
            } else {
                logger.error { "Failed to get stats for collection $indexName: ${response.status}" }
                throw RuntimeException("Failed to get collection stats: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting stats for collection $indexName" }
            throw e
        }
    }

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    override suspend fun listIndexes(): List<String> {
        try {
            // 发送请求
            val response = client.get("$baseUrl/collections") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val collections = responseBody["result"]?.jsonObject?.get("collections")?.jsonArray?.map { 
                    it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                }?.filter { it.isNotEmpty() } ?: emptyList()

                logger.debug { "Listed ${collections.size} collections" }
                return collections
            } else {
                logger.error { "Failed to list collections: ${response.status}" }
                throw RuntimeException("Failed to list collections: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error listing collections" }
            throw e
        }
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
        if (vector == null && metadata == null) {
            return true
        }

        try {
            // 构建请求体
            val requestBody = buildJsonObject {
                if (vector != null) {
                    put("vectors", buildJsonObject {
                        put(id, JsonArray(vector.map { JsonPrimitive(it) }))
                    })
                }
                
                if (metadata != null && metadata.isNotEmpty()) {
                    put("payload", buildJsonObject {
                        put(id, buildJsonObject {
                            metadata.forEach { (key, value) ->
                                when (value) {
                                    is String -> put(key, value)
                                    is Number -> put(key, value.toDouble())
                                    is Boolean -> put(key, value)
                                    else -> put(key, value.toString())
                                }
                            }
                        })
                    })
                }
            }

            // 发送请求
            val response = client.post("$baseUrl/collections/$indexName/points") {
                if (apiKey != null) {
                    header("api-key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Updated vector $id in collection $indexName" }
                return true
            } else {
                logger.error { "Failed to update vector $id in collection $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating vector $id in collection $indexName" }
            throw e
        }
    }

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
    ): List<String> = coroutineScope {
        if (vectors.isEmpty()) {
            return@coroutineScope emptyList()
        }

        // 生成或使用提供的 ID
        val vectorIds = ids ?: List(vectors.size) { UUID.randomUUID().toString() }

        // 确保元数据列表长度与向量列表长度相同
        val normalizedMetadata = if (metadata.size == vectors.size) {
            metadata
        } else {
            List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
        }

        // 将向量分批处理
        val batches = vectors.indices.chunked(batchSize)

        // 并行处理每个批次
        val results = batches.map { batchIndices ->
            async {
                val batchVectors = batchIndices.map { vectors[it] }
                val batchMetadata = batchIndices.map { normalizedMetadata[it] }
                val batchIds = batchIndices.map { vectorIds[it] }

                upsert(indexName, batchVectors, batchMetadata, batchIds)
            }
        }.awaitAll()

        // 合并结果
        results.flatten()
    }

    /**
     * 关闭客户端。
     */
    fun close() {
        client.close()
    }
}
