package ai.kastrax.datasource.common

/**
 * NoSQL 数据库连接器接口，定义了 NoSQL 数据库连接器的通用操作。
 */
interface NoSqlConnector : DataSource {
    /**
     * 查询文档。
     *
     * @param collection 集合名称。
     * @param filter 查询过滤条件，使用 JSON 格式。
     * @param projection 投影字段，使用 JSON 格式，指定要返回的字段。
     * @param sort 排序条件，使用 JSON 格式。
     * @param limit 限制返回的文档数量，默认为 0（不限制）。
     * @param skip 跳过的文档数量，默认为 0。
     * @return 查询结果，以列表形式返回，每个元素是一个映射，键是字段名，值是字段值。
     */
    suspend fun findDocuments(
        collection: String,
        filter: String = "{}",
        projection: String = "{}",
        sort: String = "{}",
        limit: Int = 0,
        skip: Int = 0
    ): List<Map<String, Any>>

    /**
     * 查询单个文档。
     *
     * @param collection 集合名称。
     * @param filter 查询过滤条件，使用 JSON 格式。
     * @param projection 投影字段，使用 JSON 格式，指定要返回的字段。
     * @return 查询结果，如果找到文档则返回映射，否则返回 null。
     */
    suspend fun findOneDocument(
        collection: String,
        filter: String = "{}",
        projection: String = "{}"
    ): Map<String, Any>?

    /**
     * 插入单个文档。
     *
     * @param collection 集合名称。
     * @param document 要插入的文档，使用 JSON 格式。
     * @return 插入的文档 ID。
     */
    suspend fun insertDocument(
        collection: String,
        document: String
    ): String

    /**
     * 插入多个文档。
     *
     * @param collection 集合名称。
     * @param documents 要插入的文档列表，每个文档使用 JSON 格式。
     * @return 插入的文档 ID 列表。
     */
    suspend fun insertDocuments(
        collection: String,
        documents: List<String>
    ): List<String>

    /**
     * 更新文档。
     *
     * @param collection 集合名称。
     * @param filter 查询过滤条件，使用 JSON 格式。
     * @param update 更新操作，使用 JSON 格式。
     * @param upsert 如果文档不存在，是否插入新文档，默认为 false。
     * @param multi 是否更新多个文档，默认为 false。
     * @return 更新的文档数量。
     */
    suspend fun updateDocuments(
        collection: String,
        filter: String,
        update: String,
        upsert: Boolean = false,
        multi: Boolean = false
    ): Long

    /**
     * 删除文档。
     *
     * @param collection 集合名称。
     * @param filter 查询过滤条件，使用 JSON 格式。
     * @param multi 是否删除多个文档，默认为 false。
     * @return 删除的文档数量。
     */
    suspend fun deleteDocuments(
        collection: String,
        filter: String,
        multi: Boolean = false
    ): Long

    /**
     * 获取集合列表。
     *
     * @param database 数据库名称，如果为 null，则使用当前数据库。
     * @return 集合名称列表。
     */
    suspend fun getCollections(database: String? = null): List<String>

    /**
     * 获取数据库列表。
     *
     * @return 数据库名称列表。
     */
    suspend fun getDatabases(): List<String>

    /**
     * 执行聚合操作。
     *
     * @param collection 集合名称。
     * @param pipeline 聚合管道，使用 JSON 数组格式。
     * @return 聚合结果，以列表形式返回，每个元素是一个映射，键是字段名，值是字段值。
     */
    suspend fun aggregate(
        collection: String,
        pipeline: String
    ): List<Map<String, Any>>

    /**
     * 创建索引。
     *
     * @param collection 集合名称。
     * @param keys 索引键，使用 JSON 格式。
     * @param options 索引选项，使用 JSON 格式。
     * @return 创建的索引名称。
     */
    suspend fun createIndex(
        collection: String,
        keys: String,
        options: String = "{}"
    ): String

    /**
     * 删除索引。
     *
     * @param collection 集合名称。
     * @param indexName 索引名称。
     * @return 如果删除成功，则返回 true；否则返回 false。
     */
    suspend fun dropIndex(
        collection: String,
        indexName: String
    ): Boolean

    /**
     * 获取索引列表。
     *
     * @param collection 集合名称。
     * @return 索引信息列表，每个元素是一个映射，包含索引名称、键等信息。
     */
    suspend fun getIndexes(
        collection: String
    ): List<Map<String, Any>>
}
