package ai.kastrax.store.pinecone

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.model.SearchResult
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
 * Pinecone 向量存储实现。
 * 参考 mastra 的 PineconeVector 实现。
 *
 * @property apiKey Pinecone API 密钥
 * @property environment Pinecone 环境
 * @property projectId Pinecone 项目 ID
 */
class PineconeVectorStore(
    private val apiKey: String,
    private val environment: String,
    private val projectId: String
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

    private val controllerUrl = "https://controller.$environment.pinecone.io"

    /**
     * 获取索引 API URL。
     *
     * @param indexName 索引名称
     * @return 索引 API URL
     */
    private suspend fun getIndexUrl(indexName: String): String {
        val response = client.get("$controllerUrl/databases/$indexName") {
            header("Api-Key", apiKey)
            contentType(ContentType.Application.Json)
        }

        if (response.status.isSuccess()) {
            val responseBody: JsonObject = response.body()
            val host = responseBody["status"]?.jsonObject?.get("host")?.jsonPrimitive?.content
                ?: throw IllegalStateException("Failed to get host for index $indexName")
            return "https://$host"
        } else {
            throw IllegalStateException("Failed to get index URL for $indexName: ${response.status}")
        }
    }

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
            // 检查索引是否已存在
            val indexes = listIndexes()
            if (indexes.contains(indexName)) {
                logger.debug { "Index $indexName already exists" }
                return false
            }

            // 将 Kastrax 相似度度量方式转换为 Pinecone 相似度度量方式
            val pineconeMetric = when (metric) {
                SimilarityMetric.COSINE -> "cosine"
                SimilarityMetric.EUCLIDEAN -> "euclidean"
                SimilarityMetric.DOT_PRODUCT -> "dotproduct"
            }

            // 创建索引
            val response = client.post("$controllerUrl/databases") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("name", indexName)
                        put("dimension", dimension)
                        put("metric", pineconeMetric)
                        put("spec", buildJsonObject {
                            put("serverless", buildJsonObject {
                                put("cloud", "aws")
                                put("region", "us-west-2")
                            })
                        })
                    }
                )
            }

            if (response.status.isSuccess()) {
                logger.debug { "Created index $indexName with dimension $dimension and metric $metric" }
                return true
            } else {
                logger.error { "Failed to create index $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating index $indexName" }
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
            // 获取索引 URL
            val indexUrl = getIndexUrl(indexName)

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
                put("vectors", JsonArray(vectors.indices.map { i ->
                    buildJsonObject {
                        put("id", vectorIds[i])
                        put("values", JsonArray(vectors[i].map { JsonPrimitive(it) }))
                        if (normalizedMetadata[i].isNotEmpty()) {
                            put("metadata", buildJsonObject {
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
            val response = client.post("$indexUrl/vectors/upsert") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Upserted ${vectors.size} vectors to index $indexName" }
                return vectorIds
            } else {
                logger.error { "Failed to upsert vectors to index $indexName: ${response.status}" }
                throw RuntimeException("Failed to upsert vectors: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error upserting vectors to index $indexName" }
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
    ): List<SearchResult> {
        try {
            // 获取索引 URL
            val indexUrl = getIndexUrl(indexName)

            // 构建过滤器
            val filterJson = if (filter != null && filter.isNotEmpty()) {
                buildJsonObject {
                    filter.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, buildJsonObject {
                                put("\$eq", value)
                            })
                            is Number -> put(key, buildJsonObject {
                                put("\$eq", value.toDouble())
                            })
                            is Boolean -> put(key, buildJsonObject {
                                put("\$eq", value)
                            })
                            is List<*> -> put(key, buildJsonObject {
                                put("\$in", JsonArray(value.map { JsonPrimitive(it.toString()) }))
                            })
                            else -> put(key, buildJsonObject {
                                put("\$eq", value.toString())
                            })
                        }
                    }
                }
            } else {
                null
            }

            // 构建请求体
            val requestBody = buildJsonObject {
                put("vector", JsonArray(queryVector.map { JsonPrimitive(it) }))
                put("topK", topK)
                put("includeMetadata", true)
                if (includeVectors) {
                    put("includeValues", true)
                }
                if (filterJson != null) {
                    put("filter", filterJson)
                }
            }

            // 发送请求
            val response = client.post("$indexUrl/query") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()

                // 解析响应
                val matches = responseBody["matches"]?.jsonArray ?: return emptyList()

                // 构建查询结果
                val results = matches.map { match ->
                    val id = match.jsonObject["id"]?.jsonPrimitive?.content ?: ""
                    val score = match.jsonObject["score"]?.jsonPrimitive?.double ?: 0.0
                    val metadata = match.jsonObject["metadata"]?.jsonObject?.map { (key, value) ->
                        key to when {
                            value.jsonPrimitive.isString -> value.jsonPrimitive.content
                            value.jsonPrimitive.content.equals("true", ignoreCase = true) || value.jsonPrimitive.content.equals("false", ignoreCase = true) -> value.jsonPrimitive.content.toBoolean()
                            else -> value.jsonPrimitive.double
                        }
                    }?.toMap() ?: emptyMap()

                    val vector = if (includeVectors) {
                        match.jsonObject["values"]?.jsonArray?.map {
                            it.jsonPrimitive.float
                        }?.toFloatArray()
                    } else {
                        null
                    }

                    SearchResult(
                        id = id,
                        score = score,
                        vector = vector,
                        metadata = metadata
                    )
                }

                logger.debug { "Query returned ${results.size} results from index $indexName" }
                return results
            } else {
                logger.error { "Failed to query index $indexName: ${response.status}" }
                throw RuntimeException("Failed to query index: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error querying index $indexName" }
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
            // 获取索引 URL
            val indexUrl = getIndexUrl(indexName)

            // 构建请求体
            val requestBody = buildJsonObject {
                put("ids", JsonArray(ids.map { JsonPrimitive(it) }))
            }

            // 发送请求
            val response = client.post("$indexUrl/vectors/delete") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Deleted ${ids.size} vectors from index $indexName" }
                return true
            } else {
                logger.error { "Failed to delete vectors from index $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting vectors from index $indexName" }
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
            val response = client.delete("$controllerUrl/databases/$indexName") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Deleted index $indexName" }
                return true
            } else {
                logger.error { "Failed to delete index $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting index $indexName" }
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
            val response = client.get("$controllerUrl/databases/$indexName") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonObject = response.body()

                // 解析响应
                val database = responseBody["database"]?.jsonObject
                val dimension = database?.get("dimension")?.jsonPrimitive?.int ?: 0
                val metricStr = database?.get("metric")?.jsonPrimitive?.content ?: "cosine"

                // 获取向量数量
                val indexUrl = getIndexUrl(indexName)
                val statsResponse = client.get("$indexUrl/describe_index_stats") {
                    header("Api-Key", apiKey)
                    contentType(ContentType.Application.Json)
                }

                val count = if (statsResponse.status.isSuccess()) {
                    val statsBody: JsonObject = statsResponse.body()
                    statsBody["totalVectorCount"]?.jsonPrimitive?.int ?: 0
                } else {
                    0
                }

                // 将 Pinecone 相似度度量方式转换为 Kastrax 相似度度量方式
                val metric = when (metricStr) {
                    "cosine" -> SimilarityMetric.COSINE
                    "euclidean" -> SimilarityMetric.EUCLIDEAN
                    "dotproduct" -> SimilarityMetric.DOT_PRODUCT
                    else -> SimilarityMetric.COSINE
                }

                logger.debug { "Retrieved stats for index $indexName: dimension=$dimension, count=$count, metric=$metric" }
                return IndexStats(dimension, count, metric)
            } else {
                logger.error { "Failed to get stats for index $indexName: ${response.status}" }
                throw RuntimeException("Failed to get index stats: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting stats for index $indexName" }
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
            val response = client.get("$controllerUrl/databases") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                val responseBody: JsonArray = response.body()

                // 解析响应
                val indexes = responseBody.map {
                    it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                }.filter { it.isNotEmpty() }

                logger.debug { "Listed ${indexes.size} indexes" }
                return indexes
            } else {
                logger.error { "Failed to list indexes: ${response.status}" }
                throw RuntimeException("Failed to list indexes: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error listing indexes" }
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
            // 获取索引 URL
            val indexUrl = getIndexUrl(indexName)

            // 构建请求体
            val requestBody = buildJsonObject {
                put("id", id)

                if (vector != null) {
                    put("values", JsonArray(vector.map { JsonPrimitive(it) }))
                }

                if (metadata != null && metadata.isNotEmpty()) {
                    put("setMetadata", buildJsonObject {
                        metadata.forEach { (key, value) ->
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

            // 发送请求
            val response = client.post("$indexUrl/vectors/update") {
                header("Api-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Updated vector $id in index $indexName" }
                return true
            } else {
                logger.error { "Failed to update vector $id in index $indexName: ${response.status}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating vector $id in index $indexName" }
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
