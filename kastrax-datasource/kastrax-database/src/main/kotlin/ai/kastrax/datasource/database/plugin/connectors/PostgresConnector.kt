package ai.kastrax.datasource.database.plugin.connectors

import ai.kastrax.core.plugin.Connector
import ai.kastrax.core.plugin.ConnectorMetadata
import ai.kastrax.core.plugin.ConnectorOperation
import ai.kastrax.core.plugin.ConnectorStatus
import ai.kastrax.core.plugin.ConnectorTestResult
import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.concurrent.atomic.AtomicReference

/**
 * PostgreSQL数据库连接器，提供与PostgreSQL数据库交互的功能。
 */
class PostgresConnector(
    override val id: String,
    override val name: String,
    override val description: String,
    override val config: Map<String, Any?>,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val schema: String
) : Connector, KastraXBase(component = "CONNECTOR", name = "PostgreSQL-$id") {
    
    override val type: String = "postgres"
    private val connectionRef = AtomicReference<Connection?>(null)
    private val statusRef = AtomicReference(ConnectorStatus.CREATED)
    private val createdAt = System.currentTimeMillis()
    private var lastConnectedAt: Long? = null
    
    override val status: ConnectorStatus
        get() = statusRef.get()
    
    override suspend fun connect(): Boolean {
        logger.info { "连接到PostgreSQL数据库: $host:$port/$database" }
        
        return try {
            val url = "jdbc:postgresql://$host:$port/$database?currentSchema=$schema"
            
            withContext(Dispatchers.IO) {
                Class.forName("org.postgresql.Driver")
                val connection = DriverManager.getConnection(url, username, password)
                connectionRef.set(connection)
            }
            
            statusRef.set(ConnectorStatus.CONNECTED)
            lastConnectedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            logger.error(e) { "连接PostgreSQL数据库失败: ${e.message}" }
            statusRef.set(ConnectorStatus.ERROR)
            false
        }
    }
    
    override suspend fun disconnect(): Boolean {
        logger.info { "断开PostgreSQL数据库连接" }
        
        return try {
            withContext(Dispatchers.IO) {
                connectionRef.getAndSet(null)?.close()
            }
            
            statusRef.set(ConnectorStatus.DISCONNECTED)
            true
        } catch (e: Exception) {
            logger.error(e) { "断开PostgreSQL数据库连接失败: ${e.message}" }
            statusRef.set(ConnectorStatus.ERROR)
            false
        }
    }
    
    override suspend fun testConnection(): ConnectorTestResult {
        logger.info { "测试PostgreSQL数据库连接" }
        
        return try {
            val connection = withContext(Dispatchers.IO) {
                val url = "jdbc:postgresql://$host:$port/$database?currentSchema=$schema"
                Class.forName("org.postgresql.Driver")
                DriverManager.getConnection(url, username, password)
            }
            
            withContext(Dispatchers.IO) {
                connection.use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT 1").use { rs ->
                            rs.next()
                            val result = rs.getInt(1)
                            
                            if (result == 1) {
                                ConnectorTestResult(
                                    success = true,
                                    message = "连接成功",
                                    details = mapOf(
                                        "database" to database,
                                        "schema" to schema,
                                        "serverVersion" to (conn.metaData.databaseProductVersion ?: "未知")
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
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "测试PostgreSQL数据库连接失败: ${e.message}" }
            
            ConnectorTestResult(
                success = false,
                message = "连接失败: ${e.message}",
                details = mapOf("error" to (e.message ?: "未知错误"))
            )
        }
    }
    
    override suspend fun execute(operation: String, parameters: Map<String, Any?>): JsonElement {
        logger.debug { "执行操作: $operation, 参数: $parameters" }
        
        val connection = connectionRef.get() ?: throw IllegalStateException("未连接到数据库")
        
        return when (operation) {
            "query" -> executeQuery(connection, parameters)
            "update" -> executeUpdate(connection, parameters)
            "batch" -> executeBatch(connection, parameters)
            "getTables" -> getTables(connection)
            "getColumns" -> getColumns(connection, parameters)
            else -> throw IllegalArgumentException("不支持的操作: $operation")
        }
    }
    
    override suspend fun executeStream(operation: String, parameters: Map<String, Any?>): Flow<JsonElement> = flow {
        logger.debug { "执行流式操作: $operation, 参数: $parameters" }
        
        val connection = connectionRef.get() ?: throw IllegalStateException("未连接到数据库")
        
        when (operation) {
            "query" -> {
                val sql = parameters["sql"] as String
                val params = parameters["params"] as? Map<String, Any?> ?: emptyMap()
                
                withContext(Dispatchers.IO) {
                    connection.prepareStatement(sql).use { statement ->
                        bindParameters(statement, params)
                        
                        statement.executeQuery().use { resultSet ->
                            val metadata = resultSet.metaData
                            val columnCount = metadata.columnCount
                            
                            while (resultSet.next()) {
                                val row = buildJsonObject {
                                    for (i in 1..columnCount) {
                                        val columnName = metadata.getColumnName(i)
                                        val value = resultSet.getObject(i)
                                        
                                        when (value) {
                                            null -> put(columnName, JsonNull)
                                            is String -> put(columnName, value)
                                            is Number -> put(columnName, value.toDouble())
                                            is Boolean -> put(columnName, value)
                                            else -> put(columnName, value.toString())
                                        }
                                    }
                                }
                                
                                emit(row)
                            }
                        }
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
                id = "query",
                name = "执行查询",
                description = "执行SQL查询",
                parameterSchema = mapOf(
                    "sql" to mapOf(
                        "type" to "string",
                        "description" to "SQL查询",
                        "required" to true
                    ),
                    "params" to mapOf(
                        "type" to "object",
                        "description" to "查询参数",
                        "required" to false
                    )
                ),
                supportsStreaming = true
            ),
            ConnectorOperation(
                id = "update",
                name = "执行更新",
                description = "执行SQL更新",
                parameterSchema = mapOf(
                    "sql" to mapOf(
                        "type" to "string",
                        "description" to "SQL更新",
                        "required" to true
                    ),
                    "params" to mapOf(
                        "type" to "object",
                        "description" to "更新参数",
                        "required" to false
                    )
                )
            ),
            ConnectorOperation(
                id = "batch",
                name = "执行批处理",
                description = "执行SQL批处理",
                parameterSchema = mapOf(
                    "sql" to mapOf(
                        "type" to "string",
                        "description" to "SQL语句",
                        "required" to true
                    ),
                    "paramsList" to mapOf(
                        "type" to "array",
                        "description" to "参数列表",
                        "required" to true
                    )
                )
            ),
            ConnectorOperation(
                id = "getTables",
                name = "获取表列表",
                description = "获取数据库中的表列表",
                parameterSchema = mapOf()
            ),
            ConnectorOperation(
                id = "getColumns",
                name = "获取列信息",
                description = "获取表的列信息",
                parameterSchema = mapOf(
                    "table" to mapOf(
                        "type" to "string",
                        "description" to "表名",
                        "required" to true
                    )
                )
            )
        )
    }
    
    override fun getMetadata(): ConnectorMetadata {
        return ConnectorMetadata(
            id = id,
            name = name,
            description = description,
            type = type,
            status = status,
            createdAt = createdAt,
            lastConnectedAt = lastConnectedAt,
            config = config.filterKeys { it != "password" },
            operations = getOperations()
        )
    }
    
    private suspend fun executeQuery(connection: Connection, parameters: Map<String, Any?>): JsonElement {
        val sql = parameters["sql"] as String
        val params = parameters["params"] as? Map<String, Any?> ?: emptyMap()
        
        return withContext(Dispatchers.IO) {
            connection.prepareStatement(sql).use { statement ->
                bindParameters(statement, params)
                
                statement.executeQuery().use { resultSet ->
                    resultSetToJson(resultSet)
                }
            }
        }
    }
    
    private suspend fun executeUpdate(connection: Connection, parameters: Map<String, Any?>): JsonElement {
        val sql = parameters["sql"] as String
        val params = parameters["params"] as? Map<String, Any?> ?: emptyMap()
        
        return withContext(Dispatchers.IO) {
            connection.prepareStatement(sql).use { statement ->
                bindParameters(statement, params)
                
                val updateCount = statement.executeUpdate()
                
                buildJsonObject {
                    put("affectedRows", updateCount)
                }
            }
        }
    }
    
    private suspend fun executeBatch(connection: Connection, parameters: Map<String, Any?>): JsonElement {
        val sql = parameters["sql"] as String
        val paramsList = parameters["paramsList"] as? List<Map<String, Any?>> ?: emptyList()
        
        return withContext(Dispatchers.IO) {
            connection.prepareStatement(sql).use { statement ->
                for (params in paramsList) {
                    bindParameters(statement, params)
                    statement.addBatch()
                }
                
                val updateCounts = statement.executeBatch()
                
                buildJsonObject {
                    put("batchResults", buildJsonArray {
                        updateCounts.forEach { add(it) }
                    })
                }
            }
        }
    }
    
    private suspend fun getTables(connection: Connection): JsonElement {
        return withContext(Dispatchers.IO) {
            connection.metaData.getTables(null, schema, "%", arrayOf("TABLE")).use { resultSet ->
                resultSetToJson(resultSet)
            }
        }
    }
    
    private suspend fun getColumns(connection: Connection, parameters: Map<String, Any?>): JsonElement {
        val table = parameters["table"] as String
        
        return withContext(Dispatchers.IO) {
            connection.metaData.getColumns(null, schema, table, "%").use { resultSet ->
                resultSetToJson(resultSet)
            }
        }
    }
    
    private fun resultSetToJson(resultSet: ResultSet): JsonElement {
        val metadata = resultSet.metaData
        val columnCount = metadata.columnCount
        val rows = buildJsonArray {
            while (resultSet.next()) {
                add(buildJsonObject {
                    for (i in 1..columnCount) {
                        val columnName = metadata.getColumnName(i)
                        val value = resultSet.getObject(i)
                        
                        when (value) {
                            null -> put(columnName, JsonNull)
                            is String -> put(columnName, value)
                            is Number -> put(columnName, value.toDouble())
                            is Boolean -> put(columnName, value)
                            else -> put(columnName, value.toString())
                        }
                    }
                })
            }
        }
        
        return buildJsonObject {
            put("rows", rows)
            put("rowCount", rows.size)
        }
    }
    
    private fun bindParameters(statement: PreparedStatement, params: Map<String, Any?>) {
        var index = 1
        
        for ((_, value) in params) {
            when (value) {
                null -> statement.setNull(index, Types.NULL)
                is String -> statement.setString(index, value)
                is Int -> statement.setInt(index, value)
                is Long -> statement.setLong(index, value)
                is Double -> statement.setDouble(index, value)
                is Boolean -> statement.setBoolean(index, value)
                is ByteArray -> statement.setBytes(index, value)
                else -> statement.setObject(index, value)
            }
            
            index++
        }
    }
}
