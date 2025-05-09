package ai.kastrax.store.postgres

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.model.SearchResult
import ai.kastrax.store.SimilarityMetric
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * PostgreSQL 向量存储实现，使用 pgvector 扩展。
 * 参考 mastra 的 PostgresVector 实现。
 *
 * @property jdbcUrl JDBC URL
 * @property username 用户名
 * @property password 密码
 * @property schema 模式
 */
class PostgresVectorStore(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
    private val schema: String = "public"
) : BaseVectorStore() {

    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@PostgresVectorStore.jdbcUrl
            this.username = this@PostgresVectorStore.username
            this.password = this@PostgresVectorStore.password
            this.schema = this@PostgresVectorStore.schema
            this.maximumPoolSize = 10
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_READ_COMMITTED"
            this.addDataSourceProperty("ApplicationName", "KastraxVectorStore")
        }
        dataSource = HikariDataSource(config)

        // 初始化数据库
        transaction(Database.connect(dataSource)) {
            // 检查 pgvector 扩展是否已安装
            val extensionExists = exec("SELECT 1 FROM pg_extension WHERE extname = 'vector'") { it.next() }
            if (extensionExists == null || !extensionExists) {
                exec("CREATE EXTENSION IF NOT EXISTS vector")
            }

            // 创建索引信息表
            SchemaUtils.createMissingTablesAndColumns(IndexInfoTable)
            commit()
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
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            transaction(Database.connect(dataSource)) {
                // 检查索引是否已存在
                val indexExists = IndexInfoTable.select { IndexInfoTable.name eq indexName }.count() > 0
                if (indexExists) {
                    logger.debug { "Index $indexName already exists" }
                    return@transaction false
                }

                // 创建向量表
                val vectorTableName = "${indexName}_vectors"
                exec("""
                    CREATE TABLE IF NOT EXISTS $schema.$vectorTableName (
                        id VARCHAR(255) PRIMARY KEY,
                        vector VECTOR($dimension) NOT NULL,
                        metadata JSONB
                    )
                """)

                // 创建向量索引
                val indexType = when (metric) {
                    SimilarityMetric.COSINE -> "vector_cosine_ops"
                    SimilarityMetric.EUCLIDEAN -> "vector_l2_ops"
                    SimilarityMetric.DOT_PRODUCT -> "vector_ip_ops"
                }
                exec("CREATE INDEX IF NOT EXISTS ${vectorTableName}_vector_idx ON $schema.$vectorTableName USING ivfflat (vector $indexType)")

                // 记录索引信息
                IndexInfoTable.insert {
                    it[name] = indexName
                    it[this.dimension] = dimension
                    it[this.metric] = metric.name
                    it[vectorTable] = vectorTableName
                    it[count] = 0
                }

                logger.debug { "Created index $indexName with dimension $dimension and metric $metric" }
                return@transaction true
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
    ): List<String> = withContext(Dispatchers.IO) {
        if (vectors.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            transaction(Database.connect(dataSource)) {
                // 获取索引信息
                val indexInfo = getIndexInfo(indexName)
                    ?: throw IllegalArgumentException("Index $indexName does not exist")

                // 验证向量维度
                vectors.forEach { vector ->
                    if (vector.size != indexInfo.dimension) {
                        throw IllegalArgumentException("Vector dimension (${vector.size}) does not match index dimension (${indexInfo.dimension})")
                    }
                }

                // 生成或使用提供的 ID
                val vectorIds = ids ?: List(vectors.size) { UUID.randomUUID().toString() }

                // 确保元数据列表长度与向量列表长度相同
                val normalizedMetadata = if (metadata.size == vectors.size) {
                    metadata
                } else {
                    List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
                }

                // 添加向量到表
                val vectorTableName = indexInfo.vectorTable
                val batchSize = 100
                val batches = vectors.indices.chunked(batchSize)

                for (batchIndices in batches) {
                    val batchValues = batchIndices.joinToString(", ") { i ->
                        val id = vectorIds[i]
                        val vector = vectors[i].joinToString(", ")
                        val metadataJson = normalizedMetadata[i].toJsonString()
                        "('$id', '[$vector]', '$metadataJson'::jsonb)"
                    }

                    exec("""
                        INSERT INTO $schema.$vectorTableName (id, vector, metadata)
                        VALUES $batchValues
                        ON CONFLICT (id) DO UPDATE
                        SET vector = EXCLUDED.vector, metadata = EXCLUDED.metadata
                    """)
                }

                // 更新索引计数
                val newCount = exec("SELECT COUNT(*) FROM $schema.$vectorTableName") { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
                exec("UPDATE $schema.index_info SET count = $newCount WHERE name = '$indexName'")

                logger.debug { "Upserted ${vectors.size} vectors to index $indexName" }
                return@transaction vectorIds
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
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            transaction(Database.connect(dataSource)) {
                // 获取索引信息
                val indexInfo = getIndexInfo(indexName)
                    ?: throw IllegalArgumentException("Index $indexName does not exist")

                // 验证查询向量维度
                if (queryVector.size != indexInfo.dimension) {
                    throw IllegalArgumentException("Query vector dimension (${queryVector.size}) does not match index dimension (${indexInfo.dimension})")
                }

                // 构建查询
                val vectorTableName = indexInfo.vectorTable
                val vectorStr = queryVector.joinToString(", ")
                val operator = when (SimilarityMetric.valueOf(indexInfo.metric)) {
                    SimilarityMetric.COSINE -> "<=>"
                    SimilarityMetric.EUCLIDEAN -> "<->"
                    SimilarityMetric.DOT_PRODUCT -> "<#>"
                }

                val filterClause = if (filter != null && filter.isNotEmpty()) {
                    val conditions = filter.entries.joinToString(" AND ") { (key, value) ->
                        when (value) {
                            is String -> "metadata->>'$key' = '$value'"
                            is Number -> "CAST(metadata->>'$key' AS NUMERIC) = ${value.toDouble()}"
                            is Boolean -> "metadata->>'$key' = '${value.toString()}'"
                            is List<*> -> {
                                val values = value.joinToString(", ") { "'${it.toString()}'" }
                                "metadata->>'$key' IN ($values)"
                            }
                            else -> "metadata->>'$key' = '${value.toString()}'"
                        }
                    }
                    "WHERE $conditions"
                } else {
                    ""
                }

                val vectorSelection = if (includeVectors) ", vector" else ""

                // 执行查询
                val results = exec("""
                    SELECT id, vector $operator '[$vectorStr]' AS score, metadata $vectorSelection
                    FROM $schema.$vectorTableName
                    $filterClause
                    ORDER BY score
                    LIMIT $topK
                """) { rs ->
                    val results = mutableListOf<SearchResult>()
                    while (rs.next()) {
                        val id = rs.getString("id")
                        val score = rs.getDouble("score")
                        val metadataJson = rs.getString("metadata")
                        val metadata = metadataJson.fromJsonString()

                        val vector = if (includeVectors) {
                            val vectorStr = rs.getString("vector")
                                .trim('[', ']')
                                .split(',')
                                .map { it.trim().toFloat() }
                                .toFloatArray()
                            vectorStr
                        } else {
                            null
                        }

                        // 转换分数为相似度（0-1范围）
                        val normalizedScore = when (SimilarityMetric.valueOf(indexInfo.metric)) {
                            SimilarityMetric.COSINE -> 1.0 - min(1.0, score)
                            SimilarityMetric.EUCLIDEAN -> 1.0 / (1.0 + score)
                            SimilarityMetric.DOT_PRODUCT -> score
                        }

                        results.add(
                            SearchResult(
                                id = id,
                                score = normalizedScore,
                                vector = vector,
                                metadata = metadata
                            )
                        )
                    }
                    results
                }

                val resultList = results?.toList() ?: emptyList()
                logger.debug { "Query returned ${resultList.size} results from index $indexName" }
                return@transaction resultList
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
    override suspend fun deleteVectors(indexName: String, ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) {
            return@withContext true
        }

        try {
            transaction(Database.connect(dataSource)) {
                // 获取索引信息
                val indexInfo = getIndexInfo(indexName)
                    ?: throw IllegalArgumentException("Index $indexName does not exist")

                // 删除向量
                val vectorTableName = indexInfo.vectorTable
                val idList = ids.joinToString(", ") { "'$it'" }
                val deletedCount = exec("DELETE FROM $schema.$vectorTableName WHERE id IN ($idList)") { rs ->
                    rs.getInt(1)
                }

                // 更新索引计数
                val newCount = exec("SELECT COUNT(*) FROM $schema.$vectorTableName") { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
                exec("UPDATE $schema.index_info SET count = $newCount WHERE name = '$indexName'")

                logger.debug { "Deleted $deletedCount vectors from index $indexName" }
                return@transaction (deletedCount ?: 0) > 0
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
    override suspend fun deleteIndex(indexName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            transaction(Database.connect(dataSource)) {
                // 获取索引信息
                val indexInfo = getIndexInfo(indexName) ?: return@transaction false

                // 删除向量表
                val vectorTableName = indexInfo.vectorTable
                exec("DROP TABLE IF EXISTS $schema.$vectorTableName")

                // 删除索引信息
                val deletedCount = IndexInfoTable.deleteWhere { IndexInfoTable.name eq indexName }

                logger.debug { "Deleted index $indexName" }
                return@transaction deletedCount > 0
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
    override suspend fun describeIndex(indexName: String): IndexStats = withContext(Dispatchers.IO) {
        try {
            transaction(Database.connect(dataSource)) {
                // 获取索引信息
                val indexInfo = getIndexInfo(indexName)
                    ?: throw IllegalArgumentException("Index $indexName does not exist")

                logger.debug { "Retrieved stats for index $indexName: dimension=${indexInfo.dimension}, count=${indexInfo.count}, metric=${indexInfo.metric}" }
                return@transaction IndexStats(
                    dimension = indexInfo.dimension,
                    count = indexInfo.count,
                    metric = SimilarityMetric.valueOf(indexInfo.metric)
                )
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
    override suspend fun listIndexes(): List<String> = withContext(Dispatchers.IO) {
        try {
            transaction(Database.connect(dataSource)) {
                val indexes = IndexInfoTable.selectAll().map { it[IndexInfoTable.name] }
                logger.debug { "Listed ${indexes.size} indexes" }
                return@transaction indexes
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
    ): Boolean = withContext(Dispatchers.IO) {
        if (vector == null && metadata == null) {
            return@withContext true
        }

        try {
            transaction(Database.connect(dataSource)) {
                // 获取索引信息
                val indexInfo = getIndexInfo(indexName)
                    ?: throw IllegalArgumentException("Index $indexName does not exist")

                // 验证向量维度
                if (vector != null && vector.size != indexInfo.dimension) {
                    throw IllegalArgumentException("Vector dimension (${vector.size}) does not match index dimension (${indexInfo.dimension})")
                }

                // 构建更新语句
                val vectorTableName = indexInfo.vectorTable
                val updates = mutableListOf<String>()

                if (vector != null) {
                    val vectorStr = vector.joinToString(", ")
                    updates.add("vector = '[$vectorStr]'")
                }

                if (metadata != null) {
                    val metadataJson = metadata.toJsonString()
                    updates.add("metadata = metadata || '$metadataJson'::jsonb")
                }

                if (updates.isEmpty()) {
                    return@transaction true
                }

                // 执行更新
                val updateClause = updates.joinToString(", ")
                val updatedCount = exec("""
                    UPDATE $schema.$vectorTableName
                    SET $updateClause
                    WHERE id = '$id'
                """) { rs ->
                    rs.getInt(1)
                }

                logger.debug { "Updated vector $id in index $indexName" }
                return@transaction (updatedCount ?: 0) > 0
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating vector $id in index $indexName" }
            throw e
        }
    }

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    private fun Transaction.getIndexInfo(indexName: String): IndexInfo? {
        return IndexInfoTable.select { IndexInfoTable.name eq indexName }
            .map {
                IndexInfo(
                    name = it[IndexInfoTable.name],
                    dimension = it[IndexInfoTable.dimension],
                    metric = it[IndexInfoTable.metric],
                    vectorTable = it[IndexInfoTable.vectorTable],
                    count = it[IndexInfoTable.count]
                )
            }
            .singleOrNull()
    }

    /**
     * 将 Map 转换为 JSON 字符串。
     *
     * @return JSON 字符串
     */
    private fun Map<String, Any>.toJsonString(): String {
        val entries = this.entries.joinToString(", ") { (key, value) ->
            val valueStr = when (value) {
                is String -> "\"$value\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"${value.toString()}\""
            }
            "\"$key\": $valueStr"
        }
        return "{$entries}"
    }

    /**
     * 将 JSON 字符串转换为 Map。
     *
     * @return Map
     */
    private fun String.fromJsonString(): Map<String, Any> {
        if (this.isBlank() || this == "null" || this == "{}" || this == "[]") {
            return emptyMap()
        }

        val result = mutableMapOf<String, Any>()
        val content = this.trim('{', '}')
        if (content.isBlank()) {
            return result
        }

        val regex = """"([^"]+)"\s*:\s*(?:"([^"]*)"|(true|false|null)|([0-9]+(?:\.[0-9]+)?))""".toRegex()
        regex.findAll(content).forEach { matchResult ->
            val key = matchResult.groupValues[1]
            val stringValue = matchResult.groupValues[2]
            val booleanOrNullValue = matchResult.groupValues[3]
            val numberValue = matchResult.groupValues[4]

            val value: Any = when {
                stringValue.isNotEmpty() -> stringValue
                booleanOrNullValue.isNotEmpty() -> when (booleanOrNullValue) {
                    "true" -> true
                    "false" -> false
                    else -> null
                } ?: ""
                numberValue.isNotEmpty() -> if (numberValue.contains('.')) {
                    numberValue.toDouble()
                } else {
                    numberValue.toInt()
                }
                else -> ""
            }

            result[key] = value
        }

        return result
    }

    /**
     * 关闭数据源。
     */
    fun close() {
        dataSource.close()
    }

    /**
     * 索引信息表。
     */
    private object IndexInfoTable : Table("kastrax_vector_indexes") {
        val name = varchar("name", 255)
        val dimension = integer("dimension")
        val metric = varchar("metric", 50)
        val vectorTable = varchar("vector_table", 255)
        val count = integer("count")

        override val primaryKey = PrimaryKey(name)
    }

    /**
     * 索引信息。
     */
    private data class IndexInfo(
        val name: String,
        val dimension: Int,
        val metric: String,
        val vectorTable: String,
        val count: Int
    )
}
