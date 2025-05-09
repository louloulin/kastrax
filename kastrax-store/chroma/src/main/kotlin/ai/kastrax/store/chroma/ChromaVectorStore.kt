package ai.kastrax.store.chroma

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
 * Chroma 向量存储实现。
 * 参考 mastra 的 ChromaVector 实现。
 *
 * @property host Chroma 服务器主机
 * @property port Chroma 服务器端口
 * @property apiPath API 路径
 * @property tenant 租户
 * @property database 数据库
 */
class ChromaVectorStore(
    private val host: String = "localhost",
    private val port: Int = 8000,
    private val apiPath: String = "",
    private val tenant: String = "default_tenant",
    private val database: String = "default_database"
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

    private val baseUrl = "http://$host:$port$apiPath"

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

            // 将 Kastrax 相似度度量方式转换为 Chroma 相似度度量方式
            val chromaMetric = when (metric) {
                SimilarityMetric.COSINE -> "cosine"
                SimilarityMetric.EUCLIDEAN -> "l2"
                SimilarityMetric.DOT_PRODUCT -> "dot"
            }

            // 创建集合
            val response = client.post("$baseUrl/api/v1/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("name", indexName)
                        put("metadata", buildJsonObject {
                            put("dimension", dimension)
                            put("metric", chromaMetric)
                        })
                        put("tenant", tenant)
                        put("database", database)
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
            // 获取集合 ID
            val collectionId = getCollectionId(indexName)
                ?: throw IllegalArgumentException("Collection $indexName does not exist")

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
                put("ids", JsonArray(vectorIds.map { JsonPrimitive(it) }))
                put("embeddings", JsonArray(vectors.map { vector ->
                    JsonArray(vector.map { JsonPrimitive(it) })
                }))
                put("metadatas", JsonArray(normalizedMetadata.map { meta ->
                    buildJsonObject {
                        meta.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Number -> put(key, value.toDouble())
                                is Boolean -> put(key, value)
                                else -> put(key, value.toString())
                            }
                        }
                    }
                }))
            }

            // 发送请求
            val response = client.post("$baseUrl/api/v1/collections/$collectionId/upsert") {
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
            // 获取集合 ID
            val collectionId = getCollectionId(indexName)
                ?: throw IllegalArgumentException("Collection $indexName does not exist")

            // 构建过滤器
            val filterJson = if (filter != null && filter.isNotEmpty()) {
                buildJsonObject {
                    filter.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value.toDouble())
                            is Boolean -> put(key, value)
                            is List<*> -> put(key, buildJsonObject {
                                put("\$in", JsonArray(value.map { JsonPrimitive(it.toString()) }))
                            })
                            else -> put(key, value.toString())
                        }
                    }
                }
            } else {
                null
            }

            // 构建请求体
            val requestBody = buildJsonObject {
                put("query_embeddings", JsonArray(listOf(JsonArray(queryVector.map { JsonPrimitive(it) }))))
                put("n_results", topK)
                if (filterJson != null) {
                    put("where", filterJson)
                }
                put("include", buildJsonArray {
                    add("metadatas")
                    add("distances")
                    add("documents")
                    if (includeVectors) {
                        add("embeddings")
                    }
                })
            }

            // 发送请求
            val response = client.post("$baseUrl/api/v1/collections/$collectionId/query") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val ids = responseBody["ids"]?.jsonArray?.get(0)?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val distances = responseBody["distances"]?.jsonArray?.get(0)?.jsonArray?.map { it.jsonPrimitive.double } ?: emptyList()
                val metadatas = responseBody["metadatas"]?.jsonArray?.get(0)?.jsonArray?.map { 
                    it.jsonObject.map { (key, value) ->
                        key to when {
                            value.jsonPrimitive.isString -> value.jsonPrimitive.content
                            value.jsonPrimitive.isBoolean -> value.jsonPrimitive.boolean
                            else -> value.jsonPrimitive.double
                        }
                    }.toMap()
                } ?: emptyList()
                
                val embeddings = if (includeVectors) {
                    responseBody["embeddings"]?.jsonArray?.get(0)?.jsonArray?.map { 
                        it.jsonArray.map { value -> value.jsonPrimitive.float }.toFloatArray()
                    }
                } else {
                    null
                }

                // 构建查询结果
                val results = ids.mapIndexed { index, id ->
                    QueryResult(
                        id = id,
                        score = 1.0 - distances.getOrElse(index) { 0.0 }, // 转换距离为相似度
                        metadata = metadatas.getOrElse(index) { emptyMap() },
                        vector = embeddings?.getOrElse(index) { null }
                    )
                }

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
            // 获取集合 ID
            val collectionId = getCollectionId(indexName)
                ?: throw IllegalArgumentException("Collection $indexName does not exist")

            // 构建请求体
            val requestBody = buildJsonObject {
                put("ids", JsonArray(ids.map { JsonPrimitive(it) }))
            }

            // 发送请求
            val response = client.post("$baseUrl/api/v1/collections/$collectionId/delete") {
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
            // 获取集合 ID
            val collectionId = getCollectionId(indexName) ?: return false

            // 发送请求
            val response = client.delete("$baseUrl/api/v1/collections/$collectionId") {
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
            // 获取集合 ID
            val collectionId = getCollectionId(indexName)
                ?: throw IllegalArgumentException("Collection $indexName does not exist")

            // 发送请求
            val response = client.get("$baseUrl/api/v1/collections/$collectionId") {
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val metadata = responseBody["metadata"]?.jsonObject
                val dimension = metadata?.get("dimension")?.jsonPrimitive?.int ?: 0
                val count = responseBody["count"]?.jsonPrimitive?.int ?: 0
                val metricStr = metadata?.get("metric")?.jsonPrimitive?.content ?: "cosine"
                
                // 将 Chroma 相似度度量方式转换为 Kastrax 相似度度量方式
                val metric = when (metricStr) {
                    "cosine" -> SimilarityMetric.COSINE
                    "l2" -> SimilarityMetric.EUCLIDEAN
                    "dot" -> SimilarityMetric.DOT_PRODUCT
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
            val response = client.get("$baseUrl/api/v1/collections") {
                contentType(ContentType.Application.Json)
                parameter("tenant", tenant)
                parameter("database", database)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val collections = responseBody["collections"]?.jsonArray?.map { 
                    it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                } ?: emptyList()

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
        try {
            // 获取集合 ID
            val collectionId = getCollectionId(indexName)
                ?: throw IllegalArgumentException("Collection $indexName does not exist")

            // 构建请求体
            val requestBody = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(id))))
                
                if (vector != null) {
                    put("embeddings", JsonArray(listOf(JsonArray(vector.map { JsonPrimitive(it) }))))
                }
                
                if (metadata != null) {
                    put("metadatas", JsonArray(listOf(buildJsonObject {
                        metadata.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Number -> put(key, value.toDouble())
                                is Boolean -> put(key, value)
                                else -> put(key, value.toString())
                            }
                        }
                    })))
                }
            }

            // 发送请求
            val response = client.post("$baseUrl/api/v1/collections/$collectionId/update") {
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
     * 获取集合 ID。
     *
     * @param collectionName 集合名称
     * @return 集合 ID，如果不存在则返回 null
     */
    private suspend fun getCollectionId(collectionName: String): String? {
        try {
            // 发送请求
            val response = client.get("$baseUrl/api/v1/collections") {
                contentType(ContentType.Application.Json)
                parameter("tenant", tenant)
                parameter("database", database)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()
                
                // 解析响应
                val collections = responseBody["collections"]?.jsonArray ?: return null
                
                // 查找匹配的集合
                for (collection in collections) {
                    val name = collection.jsonObject["name"]?.jsonPrimitive?.content ?: continue
                    if (name == collectionName) {
                        return collection.jsonObject["id"]?.jsonPrimitive?.content
                    }
                }
                
                return null
            } else {
                logger.error { "Failed to get collection ID for $collectionName: ${response.status}" }
                return null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting collection ID for $collectionName" }
            throw e
        }
    }

    /**
     * 关闭客户端。
     */
    fun close() {
        client.close()
    }
}
