package ai.kastrax.datasource.nosql

import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.NoSqlConnector
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.json.JsonParseException
import org.bson.types.ObjectId
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * MongoDB 连接器，提供了与 MongoDB 数据库交互的功能。
 */
class MongoDbConnector(
    name: String,
    val connectionString: String,
    val databaseName: String,
    val connectTimeoutMs: Int = 5000,
    val socketTimeoutMs: Int = 10000,
    val maxPoolSize: Int = 100
) : DataSourceBase(name, DataSourceType.NOSQL), NoSqlConnector {

    private var client: MongoClient? = null
    private var database: MongoDatabase? = null

    override suspend fun doConnect(): Boolean {
        return try {
            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .applyToSocketSettings { builder ->
                    builder.connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                    builder.readTimeout(socketTimeoutMs, TimeUnit.MILLISECONDS)
                }
                .applyToConnectionPoolSettings { builder ->
                    builder.maxSize(maxPoolSize)
                }
                .build()

            client = MongoClient.create(settings)
            database = client?.getDatabase(databaseName)
            true
        } catch (e: Exception) {
            logger.error(e) { "Error connecting to MongoDB: $connectionString" }
            false
        }
    }

    override suspend fun doDisconnect(): Boolean {
        return try {
            client?.close()
            client = null
            database = null
            true
        } catch (e: Exception) {
            logger.error(e) { "Error disconnecting from MongoDB: $connectionString" }
            false
        }
    }

    /**
     * 获取集合。
     *
     * @param collectionName 集合名称。
     * @return 集合对象。
     */
    private fun getCollection(collectionName: String): MongoCollection<Document> {
        val db = database ?: throw IllegalStateException("Not connected to MongoDB")
        return db.getCollection(collectionName)
    }

    /**
     * 将 JSON 字符串解析为 Bson 文档。
     *
     * @param json JSON 字符串。
     * @return Bson 文档。
     */
    private fun parseJson(json: String): Bson {
        return try {
            BsonDocument.parse(json)
        } catch (e: JsonParseException) {
            logger.error(e) { "Error parsing JSON: $json" }
            throw IllegalArgumentException("Invalid JSON: ${e.message}")
        }
    }

    /**
     * 将 Document 转换为 Map<String, Any>。
     *
     * @param document Document 对象。
     * @return Map 对象。
     */
    private fun documentToMap(document: Document): Map<String, Any> {
        return document.mapValues { (_, value) ->
            when (value) {
                is ObjectId -> value.toString()
                is Document -> documentToMap(value)
                is List<*> -> value.map { item ->
                    if (item is Document) documentToMap(item) else item
                }
                else -> value
            }
        }
    }

    override suspend fun findDocuments(
        collection: String,
        filter: String,
        projection: String,
        sort: String,
        limit: Int,
        skip: Int
    ): List<Map<String, Any>> {
        logger.debug { "Finding documents in collection: $collection with filter: $filter" }

        val filterDoc = parseJson(filter)
        val projectionDoc = parseJson(projection)
        val sortDoc = parseJson(sort)

        val results = mutableListOf<Map<String, Any>>()

        try {
            val col = getCollection(collection)
            val findFlow = col.find(filterDoc)
                .projection(projectionDoc)
                .sort(sortDoc)

            if (skip > 0) {
                findFlow.skip(skip)
            }

            if (limit > 0) {
                findFlow.limit(limit)
            }

            findFlow.collect { document ->
                results.add(documentToMap(document))
            }

            return results
        } catch (e: Exception) {
            logger.error(e) { "Error finding documents in collection: $collection" }
            throw e
        }
    }

    override suspend fun findOneDocument(
        collection: String,
        filter: String,
        projection: String
    ): Map<String, Any>? {
        logger.debug { "Finding one document in collection: $collection with filter: $filter" }

        val filterDoc = parseJson(filter)
        val projectionDoc = parseJson(projection)

        try {
            val col = getCollection(collection)
            val documents = col.find(filterDoc)
                .projection(projectionDoc)
                .limit(1)
                .toList()

            return if (documents.isNotEmpty()) {
                documentToMap(documents[0])
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error finding one document in collection: $collection" }
            throw e
        }
    }

    override suspend fun insertDocument(
        collection: String,
        document: String
    ): String {
        logger.debug { "Inserting document into collection: $collection" }

        val doc = Document.parse(document)

        try {
            val col = getCollection(collection)
            val result = col.insertOne(doc)

            return result.insertedId?.asObjectId()?.value?.toString() ?: ""
        } catch (e: Exception) {
            logger.error(e) { "Error inserting document into collection: $collection" }
            throw e
        }
    }

    override suspend fun insertDocuments(
        collection: String,
        documents: List<String>
    ): List<String> {
        logger.debug { "Inserting ${documents.size} documents into collection: $collection" }

        val docs = documents.map { Document.parse(it) }

        try {
            val col = getCollection(collection)
            val result = col.insertMany(docs)

            return result.insertedIds.values
                .mapNotNull { it.asObjectId()?.value?.toString() }
        } catch (e: Exception) {
            logger.error(e) { "Error inserting documents into collection: $collection" }
            throw e
        }
    }

    override suspend fun updateDocuments(
        collection: String,
        filter: String,
        update: String,
        upsert: Boolean,
        multi: Boolean
    ): Long {
        logger.debug { "Updating documents in collection: $collection with filter: $filter" }

        val filterDoc = parseJson(filter)
        val updateDoc = parseJson(update)

        try {
            val col = getCollection(collection)

            return if (multi) {
                val result = col.updateMany(
                    filterDoc,
                    updateDoc,
                    com.mongodb.client.model.UpdateOptions().upsert(upsert)
                )
                result.modifiedCount
            } else {
                val result = col.updateOne(
                    filterDoc,
                    updateDoc,
                    com.mongodb.client.model.UpdateOptions().upsert(upsert)
                )
                result.modifiedCount
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating documents in collection: $collection" }
            throw e
        }
    }

    override suspend fun deleteDocuments(
        collection: String,
        filter: String,
        multi: Boolean
    ): Long {
        logger.debug { "Deleting documents from collection: $collection with filter: $filter" }

        val filterDoc = parseJson(filter)

        try {
            val col = getCollection(collection)

            return if (multi) {
                val result = col.deleteMany(filterDoc)
                result.deletedCount
            } else {
                val result = col.deleteOne(filterDoc)
                result.deletedCount
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting documents from collection: $collection" }
            throw e
        }
    }

    override suspend fun getCollections(database: String?): List<String> {
        logger.debug { "Getting collections from database: ${database ?: databaseName}" }

        try {
            val db = if (database != null) {
                client?.getDatabase(database)
            } else {
                this.database
            } ?: throw IllegalStateException("Not connected to MongoDB")

            return db.listCollectionNames().toList()
        } catch (e: Exception) {
            logger.error(e) { "Error getting collections from database: ${database ?: databaseName}" }
            throw e
        }
    }

    override suspend fun getDatabases(): List<String> {
        logger.debug { "Getting databases" }

        try {
            val client = this.client ?: throw IllegalStateException("Not connected to MongoDB")
            return client.listDatabaseNames().toList()
        } catch (e: Exception) {
            logger.error(e) { "Error getting databases" }
            throw e
        }
    }

    override suspend fun aggregate(
        collection: String,
        pipeline: String
    ): List<Map<String, Any>> {
        logger.debug { "Aggregating documents in collection: $collection with pipeline: $pipeline" }

        try {
            val pipelineList = Json.parseToJsonElement(pipeline).let {
                if (it.toString().startsWith('[')) {
                    it.toString()
                } else {
                    "[$it]"
                }
            }

            val pipelineDocs = BsonDocument.parse(pipelineList).map { doc ->
                BsonDocument.parse(doc.toString())
            }

            val col = getCollection(collection)
            val results = mutableListOf<Map<String, Any>>()

            col.aggregate<Document>(pipelineDocs).collect { document ->
                results.add(documentToMap(document))
            }

            return results
        } catch (e: Exception) {
            logger.error(e) { "Error aggregating documents in collection: $collection" }
            throw e
        }
    }

    override suspend fun createIndex(
        collection: String,
        keys: String,
        options: String
    ): String {
        logger.debug { "Creating index in collection: $collection with keys: $keys" }

        val keysDoc = parseJson(keys)

        try {
            val col = getCollection(collection)
            // 创建索引选项
            val indexOptions = com.mongodb.client.model.IndexOptions()

            // 使用默认索引选项
            return col.createIndex(keysDoc, indexOptions)
        } catch (e: Exception) {
            logger.error(e) { "Error creating index in collection: $collection" }
            throw e
        }
    }

    override suspend fun dropIndex(
        collection: String,
        indexName: String
    ): Boolean {
        logger.debug { "Dropping index: $indexName from collection: $collection" }

        try {
            val col = getCollection(collection)
            col.dropIndex(indexName)
            return true
        } catch (e: Exception) {
            logger.error(e) { "Error dropping index: $indexName from collection: $collection" }
            return false
        }
    }

    override suspend fun getIndexes(
        collection: String
    ): List<Map<String, Any>> {
        logger.debug { "Getting indexes from collection: $collection" }

        try {
            val col = getCollection(collection)
            val results = mutableListOf<Map<String, Any>>()

            col.listIndexes().collect { document ->
                results.add(documentToMap(document))
            }

            return results
        } catch (e: Exception) {
            logger.error(e) { "Error getting indexes from collection: $collection" }
            throw e
        }
    }
}
