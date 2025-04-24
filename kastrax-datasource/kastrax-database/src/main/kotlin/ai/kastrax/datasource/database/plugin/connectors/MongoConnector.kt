package ai.kastrax.datasource.database.plugin.connectors

import ai.kastrax.core.plugin.Connector
import ai.kastrax.core.plugin.ConnectorMetadata
import ai.kastrax.core.plugin.ConnectorOperation
import ai.kastrax.core.plugin.ConnectorStatus
import ai.kastrax.core.plugin.ConnectorTestResult
import ai.kastrax.core.common.KastraXBase
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.util.concurrent.atomic.AtomicReference

/**
 * MongoDB数据库连接器，提供与MongoDB数据库交互的功能。
 */
class MongoConnector(
    override val id: String,
    override val name: String,
    override val description: String,
    override val config: Map<String, Any?>,
    val connectionString: String,
    val database: String
) : Connector, KastraXBase(component = "CONNECTOR", name = "MongoDB-$id") {
    
    override val type: String = "mongodb"
    private val clientRef = AtomicReference<MongoClient?>(null)
    private val databaseRef = AtomicReference<MongoDatabase?>(null)
    private val statusRef = AtomicReference(ConnectorStatus.CREATED)
    private val createdAt = System.currentTimeMillis()
    private var lastConnectedAt: Long? = null
    
    override val status: ConnectorStatus
        get() = statusRef.get()
    
    override suspend fun connect(): Boolean {
        logger.info { "连接到MongoDB数据库: $connectionString, 数据库: $database" }
        
        return try {
            withContext(Dispatchers.IO) {
                val settings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(connectionString))
                    .build()
                
                val client = MongoClients.create(settings)
                val db = client.getDatabase(database)
                
                clientRef.set(client)
                databaseRef.set(db)
            }
            
            statusRef.set(ConnectorStatus.CONNECTED)
            lastConnectedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            logger.error(e) { "连接MongoDB数据库失败: ${e.message}" }
            statusRef.set(ConnectorStatus.ERROR)
            false
        }
    }
    
    override suspend fun disconnect(): Boolean {
        logger.info { "断开MongoDB数据库连接" }
        
        return try {
            withContext(Dispatchers.IO) {
                clientRef.getAndSet(null)?.close()
                databaseRef.set(null)
            }
            
            statusRef.set(ConnectorStatus.DISCONNECTED)
            true
        } catch (e: Exception) {
            logger.error(e) { "断开MongoDB数据库连接失败: ${e.message}" }
            statusRef.set(ConnectorStatus.ERROR)
            false
        }
    }
    
    override suspend fun testConnection(): ConnectorTestResult {
        logger.info { "测试MongoDB数据库连接" }
        
        return try {
            withContext(Dispatchers.IO) {
                val settings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(connectionString))
                    .build()
                
                MongoClients.create(settings).use { client ->
                    val db = client.getDatabase(database)
                    val result = db.runCommand(Document("ping", 1))
                    
                    if (result.getDouble("ok") == 1.0) {
                        ConnectorTestResult(
                            success = true,
                            message = "连接成功",
                            details = mapOf(
                                "database" to database,
                                "serverVersion" to (client.getDatabase("admin")
                                    .runCommand(Document("buildInfo", 1))
                                    .getString("version") ?: "未知")
                            )
                        )
                    } else {
                        ConnectorTestResult(
                            success = false,
                            message = "连接测试失败",
                            details = emptyMap()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "测试MongoDB数据库连接失败: ${e.message}" }
            
            ConnectorTestResult(
                success = false,
                message = "连接失败: ${e.message}",
                details = mapOf("error" to (e.message ?: "未知错误"))
            )
        }
    }
    
    override suspend fun execute(operation: String, parameters: Map<String, Any?>): JsonElement {
        logger.debug { "执行操作: $operation, 参数: $parameters" }
        
        val db = databaseRef.get() ?: throw IllegalStateException("未连接到数据库")
        
        return when (operation) {
            "find" -> find(db, parameters)
            "findOne" -> findOne(db, parameters)
            "insertOne" -> insertOne(db, parameters)
            "insertMany" -> insertMany(db, parameters)
            "updateOne" -> updateOne(db, parameters)
            "updateMany" -> updateMany(db, parameters)
            "deleteOne" -> deleteOne(db, parameters)
            "deleteMany" -> deleteMany(db, parameters)
            "getCollections" -> getCollections(db)
            else -> throw IllegalArgumentException("不支持的操作: $operation")
        }
    }
    
    override suspend fun executeStream(operation: String, parameters: Map<String, Any?>): Flow<JsonElement> = flow {
        logger.debug { "执行流式操作: $operation, 参数: $parameters" }
        
        val db = databaseRef.get() ?: throw IllegalStateException("未连接到数据库")
        
        when (operation) {
            "find" -> {
                val collectionName = parameters["collection"] as String
                val filterJson = parameters["filter"] as? String ?: "{}"
                val projectionJson = parameters["projection"] as? String
                val limit = (parameters["limit"] as? Number)?.toInt() ?: 100
                val skip = (parameters["skip"] as? Number)?.toInt() ?: 0
                
                val collection = db.getCollection(collectionName)
                val filter = Document.parse(filterJson)
                
                withContext(Dispatchers.IO) {
                    val findIterable = collection.find(filter)
                        .skip(skip)
                        .limit(limit)
                    
                    if (projectionJson != null) {
                        findIterable.projection(Document.parse(projectionJson))
                    }
                    
                    val cursor = findIterable.cursor()
                    
                    while (cursor.hasNext()) {
                        val document = cursor.next()
                        emit(documentToJson(document))
                    }
                }
            }
            else -> {
                // 对于非流式操作，直接执行并发送结果
                val result = execute(operation, parameters)
                emit(result)
            }
        }
    }
    
    override fun getOperations(): List<ConnectorOperation> {
        return listOf(
            ConnectorOperation(
                id = "find",
                name = "查找文档",
                description = "查找匹配条件的文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "filter" to mapOf(
                        "type" to "object",
                        "description" to "过滤条件",
                        "required" to false,
                        "default" to "{}"
                    ),
                    "projection" to mapOf(
                        "type" to "object",
                        "description" to "投影",
                        "required" to false
                    ),
                    "limit" to mapOf(
                        "type" to "integer",
                        "description" to "限制数量",
                        "required" to false,
                        "default" to 100
                    ),
                    "skip" to mapOf(
                        "type" to "integer",
                        "description" to "跳过数量",
                        "required" to false,
                        "default" to 0
                    )
                ),
                supportsStreaming = true
            ),
            ConnectorOperation(
                id = "findOne",
                name = "查找单个文档",
                description = "查找匹配条件的单个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "filter" to mapOf(
                        "type" to "object",
                        "description" to "过滤条件",
                        "required" to false,
                        "default" to "{}"
                    ),
                    "projection" to mapOf(
                        "type" to "object",
                        "description" to "投影",
                        "required" to false
                    )
                )
            ),
            ConnectorOperation(
                id = "insertOne",
                name = "插入文档",
                description = "插入单个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "document" to mapOf(
                        "type" to "object",
                        "description" to "文档",
                        "required" to true
                    )
                )
            ),
            ConnectorOperation(
                id = "insertMany",
                name = "批量插入文档",
                description = "插入多个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "documents" to mapOf(
                        "type" to "array",
                        "description" to "文档列表",
                        "required" to true
                    )
                )
            ),
            ConnectorOperation(
                id = "updateOne",
                name = "更新文档",
                description = "更新单个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "filter" to mapOf(
                        "type" to "object",
                        "description" to "过滤条件",
                        "required" to true
                    ),
                    "update" to mapOf(
                        "type" to "object",
                        "description" to "更新操作",
                        "required" to true
                    ),
                    "upsert" to mapOf(
                        "type" to "boolean",
                        "description" to "如果不存在则插入",
                        "required" to false,
                        "default" to false
                    )
                )
            ),
            ConnectorOperation(
                id = "updateMany",
                name = "批量更新文档",
                description = "更新多个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "filter" to mapOf(
                        "type" to "object",
                        "description" to "过滤条件",
                        "required" to true
                    ),
                    "update" to mapOf(
                        "type" to "object",
                        "description" to "更新操作",
                        "required" to true
                    ),
                    "upsert" to mapOf(
                        "type" to "boolean",
                        "description" to "如果不存在则插入",
                        "required" to false,
                        "default" to false
                    )
                )
            ),
            ConnectorOperation(
                id = "deleteOne",
                name = "删除文档",
                description = "删除单个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "filter" to mapOf(
                        "type" to "object",
                        "description" to "过滤条件",
                        "required" to true
                    )
                )
            ),
            ConnectorOperation(
                id = "deleteMany",
                name = "批量删除文档",
                description = "删除多个文档",
                parameterSchema = mapOf(
                    "collection" to mapOf(
                        "type" to "string",
                        "description" to "集合名称",
                        "required" to true
                    ),
                    "filter" to mapOf(
                        "type" to "object",
                        "description" to "过滤条件",
                        "required" to true
                    )
                )
            ),
            ConnectorOperation(
                id = "getCollections",
                name = "获取集合列表",
                description = "获取数据库中的集合列表",
                parameterSchema = mapOf()
            )
        )
    }
    
    override fun getMetadata(): ConnectorMetadata {
        // 移除敏感信息
        val safeConfig = config.toMutableMap()
        if (connectionString.contains("@")) {
            safeConfig["connectionString"] = connectionString.replace(Regex("//[^@]+@"), "//****:****@")
        }
        
        return ConnectorMetadata(
            id = id,
            name = name,
            description = description,
            type = type,
            status = status,
            createdAt = createdAt,
            lastConnectedAt = lastConnectedAt,
            config = safeConfig,
            operations = getOperations()
        )
    }
    
    private suspend fun find(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val filterJson = parameters["filter"] as? String ?: "{}"
        val projectionJson = parameters["projection"] as? String
        val limit = (parameters["limit"] as? Number)?.toInt() ?: 100
        val skip = (parameters["skip"] as? Number)?.toInt() ?: 0
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val filter = Document.parse(filterJson)
            
            val findIterable = collection.find(filter)
                .skip(skip)
                .limit(limit)
            
            if (projectionJson != null) {
                findIterable.projection(Document.parse(projectionJson))
            }
            
            val documents = findIterable.into(ArrayList())
            
            buildJsonObject {
                put("documents", buildJsonArray {
                    documents.forEach { add(documentToJson(it)) }
                })
                put("count", documents.size)
            }
        }
    }
    
    private suspend fun findOne(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val filterJson = parameters["filter"] as? String ?: "{}"
        val projectionJson = parameters["projection"] as? String
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val filter = Document.parse(filterJson)
            
            val findIterable = collection.find(filter).limit(1)
            
            if (projectionJson != null) {
                findIterable.projection(Document.parse(projectionJson))
            }
            
            val document = findIterable.first()
            
            if (document != null) {
                documentToJson(document)
            } else {
                JsonNull
            }
        }
    }
    
    private suspend fun insertOne(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val documentJson = parameters["document"] as String
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val document = Document.parse(documentJson)
            
            collection.insertOne(document)
            
            buildJsonObject {
                put("acknowledged", true)
                put("insertedId", document.getObjectId("_id")?.toString() ?: "")
            }
        }
    }
    
    private suspend fun insertMany(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val documentsJson = parameters["documents"] as List<String>
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val documents = documentsJson.map { Document.parse(it) }
            
            val result = collection.insertMany(documents)
            
            buildJsonObject {
                put("acknowledged", result.wasAcknowledged())
                put("insertedCount", result.insertedIds.size)
                put("insertedIds", buildJsonArray {
                    result.insertedIds.values.forEach { 
                        add((it as? ObjectId)?.toString() ?: it.toString())
                    }
                })
            }
        }
    }
    
    private suspend fun updateOne(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val filterJson = parameters["filter"] as String
        val updateJson = parameters["update"] as String
        val upsert = parameters["upsert"] as? Boolean ?: false
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val filter = Document.parse(filterJson)
            val update = Document.parse(updateJson)
            
            val options = UpdateOptions().upsert(upsert)
            val result = collection.updateOne(filter, update, options)
            
            buildJsonObject {
                put("acknowledged", result.wasAcknowledged())
                put("matchedCount", result.matchedCount)
                put("modifiedCount", result.modifiedCount)
                put("upsertedId", result.upsertedId?.toString() ?: JsonNull)
            }
        }
    }
    
    private suspend fun updateMany(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val filterJson = parameters["filter"] as String
        val updateJson = parameters["update"] as String
        val upsert = parameters["upsert"] as? Boolean ?: false
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val filter = Document.parse(filterJson)
            val update = Document.parse(updateJson)
            
            val options = UpdateOptions().upsert(upsert)
            val result = collection.updateMany(filter, update, options)
            
            buildJsonObject {
                put("acknowledged", result.wasAcknowledged())
                put("matchedCount", result.matchedCount)
                put("modifiedCount", result.modifiedCount)
                put("upsertedId", result.upsertedId?.toString() ?: JsonNull)
            }
        }
    }
    
    private suspend fun deleteOne(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val filterJson = parameters["filter"] as String
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val filter = Document.parse(filterJson)
            
            val result = collection.deleteOne(filter)
            
            buildJsonObject {
                put("acknowledged", result.wasAcknowledged())
                put("deletedCount", result.deletedCount)
            }
        }
    }
    
    private suspend fun deleteMany(db: MongoDatabase, parameters: Map<String, Any?>): JsonElement {
        val collectionName = parameters["collection"] as String
        val filterJson = parameters["filter"] as String
        
        return withContext(Dispatchers.IO) {
            val collection = db.getCollection(collectionName)
            val filter = Document.parse(filterJson)
            
            val result = collection.deleteMany(filter)
            
            buildJsonObject {
                put("acknowledged", result.wasAcknowledged())
                put("deletedCount", result.deletedCount)
            }
        }
    }
    
    private suspend fun getCollections(db: MongoDatabase): JsonElement {
        return withContext(Dispatchers.IO) {
            val collections = db.listCollectionNames().into(ArrayList())
            
            buildJsonObject {
                put("collections", buildJsonArray {
                    collections.forEach { add(it) }
                })
            }
        }
    }
    
    private fun documentToJson(document: Document): JsonElement {
        return buildJsonObject {
            for ((key, value) in document) {
                when (value) {
                    null -> put(key, JsonNull)
                    is String -> put(key, value)
                    is Number -> put(key, value.toDouble())
                    is Boolean -> put(key, value)
                    is ObjectId -> put(key, value.toString())
                    is Document -> put(key, documentToJson(value))
                    is List<*> -> put(key, listToJsonArray(value))
                    else -> put(key, value.toString())
                }
            }
        }
    }
    
    private fun listToJsonArray(list: List<*>): JsonArray {
        return buildJsonArray {
            list.forEach { item ->
                when (item) {
                    null -> add(JsonNull)
                    is String -> add(item)
                    is Number -> add(item.toDouble())
                    is Boolean -> add(item)
                    is ObjectId -> add(item.toString())
                    is Document -> add(documentToJson(item))
                    is List<*> -> add(listToJsonArray(item))
                    else -> add(item.toString())
                }
            }
        }
    }
}
