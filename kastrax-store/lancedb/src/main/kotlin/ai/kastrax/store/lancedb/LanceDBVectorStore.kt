package ai.kastrax.store.lancedb

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.QueryResult
import ai.kastrax.store.SimilarityMetric
import com.lancedb.lance.Arrow
import com.lancedb.lance.Connection
import com.lancedb.lance.LanceDB
import com.lancedb.lance.Table
import com.lancedb.lance.builder.TableBuilder
import com.lancedb.lance.query.Query
import com.lancedb.lance.schema.Field
import com.lancedb.lance.schema.Schema
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.FieldVector
import org.apache.arrow.vector.Float4Vector
import org.apache.arrow.vector.VarCharVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.complex.ListVector
import org.apache.arrow.vector.types.FloatingPointPrecision
import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.types.pojo.FieldType
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * LanceDB 向量存储实现。
 * 基于 LanceDB Java 客户端实现。
 *
 * @property uri LanceDB URI，可以是本地路径或远程 URI
 */
class LanceDBVectorStore(
    private val uri: String
) : BaseVectorStore() {

    private val connection: Connection by lazy {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // 远程连接
            LanceDB.connect(uri)
        } else {
            // 本地连接
            val directory = File(uri)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            LanceDB.connect(directory.absolutePath)
        }
    }

    private val allocator = RootAllocator()

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
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查表是否已存在
            val tables = listIndexes()
            if (tables.contains(indexName)) {
                logger.debug { "Table $indexName already exists" }
                return@withContext false
            }

            // 将 Kastrax 相似度度量方式转换为 LanceDB 相似度度量方式
            val lanceMetric = when (metric) {
                SimilarityMetric.COSINE -> "cosine"
                SimilarityMetric.EUCLIDEAN -> "l2"
                SimilarityMetric.DOT_PRODUCT -> "dot"
            }

            // 创建 Schema
            val schema = Schema.builder()
                .addField(Field.builder("id", ArrowType.Utf8.INSTANCE).build())
                .addField(Field.builder("vector", ArrowType.List.INSTANCE)
                    .addField(Field.builder("item", ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)).build())
                    .build())
                .addField(Field.builder("metadata", ArrowType.Utf8.INSTANCE).build())
                .build()

            // 创建表
            val tableBuilder = TableBuilder.builder(connection, indexName)
                .schema(schema)
                .metric(lanceMetric)
                .dimension(dimension)

            tableBuilder.build()

            logger.debug { "Created table $indexName with dimension $dimension and metric $metric" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error creating table $indexName" }
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
    ): List<String> = withContext(Dispatchers.IO) {
        if (vectors.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            // 获取表
            val table = connection.openTable(indexName)

            // 生成或使用提供的 ID
            val vectorIds = ids ?: List(vectors.size) { UUID.randomUUID().toString() }

            // 确保元数据列表长度与向量列表长度相同
            val normalizedMetadata = if (metadata.size == vectors.size) {
                metadata
            } else {
                List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
            }

            // 创建 Arrow 向量
            val idVector = VarCharVector("id", allocator)
            val vectorVector = ListVector("vector", allocator, FieldType(true, ArrowType.List.INSTANCE, null), null)
            val metadataVector = VarCharVector("metadata", allocator)

            // 分配内存
            idVector.allocateNew(vectors.size)
            vectorVector.allocateNew()
            metadataVector.allocateNew(vectors.size)

            // 填充数据
            for (i in vectors.indices) {
                // ID
                idVector.setSafe(i, vectorIds[i].toByteArray(StandardCharsets.UTF_8))

                // 向量
                val vector = vectors[i]
                vectorVector.startNewValue(i)
                for (j in vector.indices) {
                    vectorVector.setSafe(vectorVector.valueCount, vector[j])
                }
                vectorVector.endValue(i, vector.size)

                // 元数据
                val metadataJson = normalizedMetadata[i].entries.joinToString(",", "{", "}") { (key, value) ->
                    "\"$key\":\"$value\""
                }
                metadataVector.setSafe(i, metadataJson.toByteArray(StandardCharsets.UTF_8))
            }

            // 设置值计数
            idVector.valueCount = vectors.size
            vectorVector.valueCount = vectors.size
            metadataVector.valueCount = vectors.size

            // 创建 VectorSchemaRoot
            val root = VectorSchemaRoot.of(idVector, vectorVector, metadataVector)

            // 添加数据
            table.add(Arrow.vectorSchemaRoot(root))

            // 释放资源
            root.close()
            idVector.close()
            vectorVector.close()
            metadataVector.close()

            logger.debug { "Upserted ${vectors.size} vectors to table $indexName" }
            return@withContext vectorIds
        } catch (e: Exception) {
            logger.error(e) { "Error upserting vectors to table $indexName" }
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
    ): List<QueryResult> = withContext(Dispatchers.IO) {
        try {
            // 获取表
            val table = connection.openTable(indexName)

            // 构建查询
            var query = table.query()
                .nearest("vector", queryVector)
                .limit(topK)

            // 添加过滤条件
            if (filter != null && filter.isNotEmpty()) {
                val filterStr = buildString {
                    filter.entries.forEachIndexed { index, (key, value) ->
                        if (index > 0) append(" AND ")
                        when (value) {
                            is String -> append("json_extract(metadata, '$.$key') = '$value'")
                            is Number -> append("json_extract(metadata, '$.$key') = $value")
                            is Boolean -> append("json_extract(metadata, '$.$key') = ${value.toString().lowercase()}")
                            is List<*> -> {
                                append("json_extract(metadata, '$.$key') IN (")
                                value.forEachIndexed { i, item ->
                                    if (i > 0) append(", ")
                                    when (item) {
                                        is String -> append("'$item'")
                                        else -> append("$item")
                                    }
                                }
                                append(")")
                            }
                            else -> append("json_extract(metadata, '$.$key') = '${value}'")
                        }
                    }
                }
                query = query.filter(filterStr)
            }

            // 执行查询
            val result = query.execute()

            // 解析结果
            val queryResults = mutableListOf<QueryResult>()
            
            while (result.hasNext()) {
                val batch = result.next()
                val root = batch.getRoot()
                
                val idVector = root.getVector("id") as VarCharVector
                val scoreVector = root.getVector("_distance") as Float4Vector
                val metadataVector = root.getVector("metadata") as VarCharVector
                
                val vectorVector = if (includeVectors) {
                    root.getVector("vector") as ListVector
                } else {
                    null
                }
                
                for (i in 0 until root.rowCount) {
                    // 解析 ID
                    val id = idVector.getObject(i).toString()
                    
                    // 解析分数
                    val score = 1.0 - scoreVector.get(i).toDouble() // 转换为相似度
                    
                    // 解析元数据
                    val metadataStr = metadataVector.getObject(i).toString()
                    val metadata = parseMetadata(metadataStr)
                    
                    // 解析向量
                    val vector = if (includeVectors && vectorVector != null) {
                        val vectorValues = vectorVector.getObject(i) as List<*>
                        vectorValues.map { (it as Number).toFloat() }.toFloatArray()
                    } else {
                        null
                    }
                    
                    queryResults.add(
                        QueryResult(
                            id = id,
                            score = score,
                            metadata = metadata,
                            vector = vector
                        )
                    )
                }
            }
            
            result.close()

            logger.debug { "Query returned ${queryResults.size} results from table $indexName" }
            return@withContext queryResults
        } catch (e: Exception) {
            logger.error(e) { "Error querying table $indexName" }
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
    override suspend fun deleteVectors(indexName: String, ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) {
            return@withContext true
        }

        try {
            // 获取表
            val table = connection.openTable(indexName)

            // 构建 ID 列表
            val idList = ids.joinToString(", ") { "'$it'" }

            // 删除向量
            table.delete("id IN ($idList)")

            logger.debug { "Deleted ${ids.size} vectors from table $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting vectors from table $indexName" }
            throw e
        }
    }

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    override suspend fun deleteIndex(indexName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 删除表
            connection.dropTable(indexName)

            logger.debug { "Deleted table $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting table $indexName" }
            throw e
        }
    }

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    override suspend fun describeIndex(indexName: String): IndexStats = withContext(Dispatchers.IO) {
        try {
            // 获取表
            val table = connection.openTable(indexName)

            // 获取表信息
            val schema = table.schema()
            val vectorField = schema.fields().stream()
                .filter { it.name() == "vector" }
                .findFirst()
                .orElseThrow { IllegalStateException("Vector field not found") }

            // 获取维度
            val dimension = table.dimension()

            // 获取度量方式
            val metricStr = table.metric()

            // 获取向量数量
            val count = table.countRows()

            // 将 LanceDB 相似度度量方式转换为 Kastrax 相似度度量方式
            val metric = when (metricStr) {
                "cosine" -> SimilarityMetric.COSINE
                "l2" -> SimilarityMetric.EUCLIDEAN
                "dot" -> SimilarityMetric.DOT_PRODUCT
                else -> SimilarityMetric.COSINE
            }

            logger.debug { "Retrieved stats for table $indexName: dimension=$dimension, count=$count, metric=$metric" }
            return@withContext IndexStats(dimension, count.toInt(), metric)
        } catch (e: Exception) {
            logger.error(e) { "Error getting stats for table $indexName" }
            throw e
        }
    }

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    override suspend fun listIndexes(): List<String> = withContext(Dispatchers.IO) {
        try {
            // 列出所有表
            val tables = connection.listTables()

            logger.debug { "Listed ${tables.size} tables" }
            return@withContext tables
        } catch (e: Exception) {
            logger.error(e) { "Error listing tables" }
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
    ): Boolean = withContext(Dispatchers.IO) {
        if (vector == null && metadata == null) {
            return@withContext true
        }

        try {
            // 获取表
            val table = connection.openTable(indexName)

            // 删除旧向量
            table.delete("id = '$id'")

            // 如果没有新向量或元数据，则直接返回
            if (vector == null && metadata == null) {
                return@withContext true
            }

            // 获取现有向量
            val existingVector = if (vector == null) {
                val result = table.query()
                    .filter("id = '$id'")
                    .execute()

                if (result.hasNext()) {
                    val batch = result.next()
                    val root = batch.getRoot()
                    val vectorVector = root.getVector("vector") as ListVector
                    
                    if (root.rowCount > 0) {
                        val vectorValues = vectorVector.getObject(0) as List<*>
                        vectorValues.map { (it as Number).toFloat() }.toFloatArray()
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                vector
            }

            // 获取现有元数据
            val existingMetadata = if (metadata == null) {
                val result = table.query()
                    .filter("id = '$id'")
                    .execute()

                if (result.hasNext()) {
                    val batch = result.next()
                    val root = batch.getRoot()
                    val metadataVector = root.getVector("metadata") as VarCharVector
                    
                    if (root.rowCount > 0) {
                        val metadataStr = metadataVector.getObject(0).toString()
                        parseMetadata(metadataStr)
                    } else {
                        emptyMap()
                    }
                } else {
                    emptyMap()
                }
            } else {
                metadata
            }

            // 如果没有现有向量或元数据，则直接返回
            if (existingVector == null && existingMetadata.isEmpty()) {
                return@withContext false
            }

            // 添加新向量
            upsert(
                indexName = indexName,
                vectors = listOf(existingVector ?: FloatArray(0)),
                metadata = listOf(existingMetadata),
                ids = listOf(id)
            )

            logger.debug { "Updated vector $id in table $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error updating vector $id in table $indexName" }
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
     * 创建 ANN 索引。
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param params 索引参数
     * @return 是否成功创建
     */
    suspend fun createAnnIndex(
        indexName: String,
        indexType: String = "ivf_pq",
        params: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取表
            val table = connection.openTable(indexName)

            // 构建索引参数
            val indexParams = mutableMapOf<String, Any>()
            indexParams["type"] = indexType
            indexParams.putAll(params)

            // 创建索引
            table.createIndex("vector", indexParams)

            logger.debug { "Created ANN index for table $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error creating ANN index for table $indexName" }
            throw e
        }
    }

    /**
     * 解析元数据。
     *
     * @param metadataStr 元数据字符串
     * @return 元数据映射
     */
    private fun parseMetadata(metadataStr: String): Map<String, Any> {
        if (metadataStr.isEmpty() || metadataStr == "{}" || metadataStr == "null") {
            return emptyMap()
        }

        val metadata = mutableMapOf<String, Any>()
        
        // 简单的 JSON 解析
        val trimmed = metadataStr.trim().removeSurrounding("{", "}")
        if (trimmed.isEmpty()) {
            return emptyMap()
        }
        
        val pairs = trimmed.split(",")
        for (pair in pairs) {
            val keyValue = pair.split(":", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim().removeSurrounding("\"")
                val valueStr = keyValue[1].trim()
                
                val value: Any = when {
                    valueStr == "null" -> "null"
                    valueStr == "true" || valueStr == "false" -> valueStr.toBoolean()
                    valueStr.startsWith("\"") && valueStr.endsWith("\"") -> 
                        valueStr.removeSurrounding("\"")
                    else -> try {
                        valueStr.toDouble()
                    } catch (e: Exception) {
                        valueStr
                    }
                }
                
                metadata[key] = value
            }
        }
        
        return metadata
    }

    /**
     * 关闭资源。
     */
    fun close() {
        allocator.close()
    }
}
