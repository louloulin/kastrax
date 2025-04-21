package ai.kastrax.datasource.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * MySQL 数据库连接器，提供了与 MySQL 数据库交互的功能。
 */
class MySqlConnector(
    name: String,
    override val url: String,
    override val username: String,
    override val password: String,
    override val driverClassName: String = "com.mysql.cj.jdbc.Driver"
) : DatabaseConnectorBase(name) {
    
    private var connection: Connection? = null
    
    override suspend fun doConnect(): Boolean {
        return try {
            Class.forName(driverClassName)
            connection = DriverManager.getConnection(url, username, password)
            true
        } catch (e: Exception) {
            logger.error(e) { "Error connecting to MySQL database: $url" }
            false
        }
    }
    
    override suspend fun doDisconnect(): Boolean {
        return try {
            connection?.close()
            true
        } catch (e: Exception) {
            logger.error(e) { "Error disconnecting from MySQL database: $url" }
            false
        } finally {
            connection = null
        }
    }
    
    override fun doExecuteQuery(sql: String, params: Map<String, Any>): List<Map<String, Any>> {
        val conn = connection ?: throw IllegalStateException("Not connected to database")
        
        return conn.prepareStatement(sql).use { statement ->
            bindParameters(statement, params)
            
            statement.executeQuery().use { resultSet ->
                val metadata = resultSet.metaData
                val columnCount = metadata.columnCount
                val result = mutableListOf<Map<String, Any>>()
                
                while (resultSet.next()) {
                    val row = mutableMapOf<String, Any>()
                    
                    for (i in 1..columnCount) {
                        val columnName = metadata.getColumnName(i)
                        val value = resultSet.getObject(i)
                        row[columnName] = value ?: "null"
                    }
                    
                    result.add(row)
                }
                
                result
            }
        }
    }
    
    override fun doExecuteUpdate(sql: String, params: Map<String, Any>): Int {
        val conn = connection ?: throw IllegalStateException("Not connected to database")
        
        return conn.prepareStatement(sql).use { statement ->
            bindParameters(statement, params)
            statement.executeUpdate()
        }
    }
    
    override fun doGetTables(): List<String> {
        val conn = connection ?: throw IllegalStateException("Not connected to database")
        
        return conn.metaData.getTables(null, null, "%", arrayOf("TABLE")).use { resultSet ->
            val tables = mutableListOf<String>()
            
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"))
            }
            
            tables
        }
    }
    
    override fun doGetColumns(table: String): List<Map<String, Any>> {
        val conn = connection ?: throw IllegalStateException("Not connected to database")
        
        return conn.metaData.getColumns(null, null, table, "%").use { resultSet ->
            val columns = mutableListOf<Map<String, Any>>()
            
            while (resultSet.next()) {
                val column = mutableMapOf<String, Any>()
                
                column["name"] = resultSet.getString("COLUMN_NAME")
                column["type"] = resultSet.getString("TYPE_NAME")
                column["size"] = resultSet.getInt("COLUMN_SIZE")
                column["nullable"] = resultSet.getBoolean("IS_NULLABLE")
                
                columns.add(column)
            }
            
            columns
        }
    }
    
    /**
     * 绑定参数到 PreparedStatement。
     *
     * @param statement PreparedStatement。
     * @param params 参数映射。
     */
    private fun bindParameters(statement: PreparedStatement, params: Map<String, Any>) {
        // 简单实现，仅支持命名参数
        val parameterMetaData = statement.parameterMetaData
        val parameterCount = parameterMetaData.parameterCount
        
        if (parameterCount > 0 && params.isNotEmpty()) {
            var index = 1
            
            for ((_, value) in params) {
                if (index > parameterCount) break
                
                when (value) {
                    is String -> statement.setString(index, value)
                    is Int -> statement.setInt(index, value)
                    is Long -> statement.setLong(index, value)
                    is Double -> statement.setDouble(index, value)
                    is Boolean -> statement.setBoolean(index, value)
                    is ByteArray -> statement.setBytes(index, value)
                    null -> statement.setNull(index, java.sql.Types.NULL)
                    else -> statement.setObject(index, value)
                }
                
                index++
            }
        }
    }
}
