package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.sql.Connection as JavaSqlConnection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types
import java.sql.Timestamp
import java.sql.PreparedStatement
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import com.microsoft.sqlserver.jdbc.SQLServerDataSource

/**
 * SQL Server实现的数据库连接器，适配Micronaut框架
 */
@Singleton
@Requires(classes = [com.microsoft.sqlserver.jdbc.SQLServerDriver::class])
class SQLServerConnector : DatabaseConnector {
    
    private val logger = LoggerFactory.getLogger(SQLServerConnector::class.java)
    
    /**
     * 连接到SQL Server数据库
     */
    override suspend fun connect(config: ConnectionConfig): Connection = withContext(Dispatchers.IO) {
        try {
            val dataSource = createDataSource(config)
            val jdbcConnection = dataSource.connection
            
            // 测试连接
            jdbcConnection.prepareStatement("SELECT 1").use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                }
            }
            
            return@withContext Connection(
                id = UUID.randomUUID().toString(),
                config = config,
                status = ConnectionStatus.CONNECTED,
                connectedAt = Instant.now(),
                dataSource = dataSource,
                jdbcConnection = jdbcConnection
            )
        } catch (e: Exception) {
            logger.error("SQL Server连接失败: {}", config.name, e)
            throw ConnectionException("连接SQL Server失败: ${e.message}", e)
        }
    }
    
    /**
     * 断开SQL Server连接
     */
    override suspend fun disconnect(connection: Connection): Boolean = withContext(Dispatchers.IO) {
        try {
            connection.jdbcConnection?.close()
            logger.info("SQL Server连接已断开: {}", connection.id)
            return@withContext true
        } catch (e: Exception) {
            logger.error("断开SQL Server连接失败: {}", connection.id, e)
            return@withContext false
        }
    }
    
    /**
     * 测试SQL Server连接
     */
    override suspend fun testConnection(config: ConnectionConfig): ConnectionStatus = withContext(Dispatchers.IO) {
        try {
            val dataSource = createDataSource(config)
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT @@VERSION").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            logger.debug("SQL Server版本: {}", rs.getString(1))
                        }
                    }
                }
            }
            return@withContext ConnectionStatus.CONNECTED
        } catch (e: Exception) {
            logger.error("SQL Server连接测试失败: {}", config.name, e)
            return@withContext ConnectionStatus.FAILED
        }
    }
    
    /**
     * 获取SQL Server数据库元数据
     */
    override suspend fun getMetadata(connection: Connection): DatabaseMetadata = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = connection.jdbcConnection
                ?: throw ConnectionException("连接已关闭")
            
            val metaData = jdbcConnection.metaData
            
            // 获取数据库信息
            val databaseInfo = DatabaseInfo(
                productName = metaData.databaseProductName,
                productVersion = metaData.databaseProductVersion,
                driverName = metaData.driverName,
                driverVersion = metaData.driverVersion,
                url = metaData.url,
                username = metaData.userName
            )
            
            // 获取模式列表
            val schemas = mutableListOf<String>()
            metaData.schemas.use { rs ->
                while (rs.next()) {
                    val schemaName = rs.getString("TABLE_SCHEM")
                    if (!isSystemSchema(schemaName)) {
                        schemas.add(schemaName)
                    }
                }
            }
            
            // 获取表列表
            val tables = mutableListOf<TableInfo>()
            for (schema in schemas) {
                metaData.getTables(null, schema, null, arrayOf("TABLE")).use { rs ->
                    while (rs.next()) {
                        val tableName = rs.getString("TABLE_NAME")
                        val tableType = rs.getString("TABLE_TYPE")
                        val remarks = rs.getString("REMARKS") ?: ""
                        
                        tables.add(
                            TableInfo(
                                name = tableName,
                                schema = schema,
                                type = tableType,
                                comment = remarks,
                                columns = getTableColumns(metaData, schema, tableName)
                            )
                        )
                    }
                }
            }
            
            return@withContext DatabaseMetadata(
                databaseInfo = databaseInfo,
                schemas = schemas,
                tables = tables
            )
        } catch (e: Exception) {
            logger.error("获取SQL Server元数据失败", e)
            throw ConnectionException("获取元数据失败: ${e.message}", e)
        }
    }
    
    /**
     * 执行SQL Server查询
     */
    override suspend fun executeQuery(
        connection: Connection,
        query: String,
        parameters: List<Any>,
        timeout: Long
    ): QueryResult = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = connection.jdbcConnection
                ?: throw ConnectionException("连接已关闭")
            
            jdbcConnection.prepareStatement(query).use { stmt ->
                stmt.queryTimeout = (timeout / 1000).toInt()
                
                // 设置参数
                parameters.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                
                val startTime = System.currentTimeMillis()
                stmt.executeQuery().use { rs ->
                    val endTime = System.currentTimeMillis()
                    
                    val columns = getResultSetColumns(rs)
                    val rows = mutableListOf<Map<String, Any?>>()
                    
                    while (rs.next()) {
                        val row = mutableMapOf<String, Any?>()
                        columns.forEach { column ->
                            row[column.name] = getColumnValue(rs, column)
                        }
                        rows.add(row)
                    }
                    
                    return@withContext QueryResult(
                        columns = columns,
                        rows = rows,
                        rowCount = rows.size,
                        executionTime = endTime - startTime
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("SQL Server查询执行失败: {}", query, e)
            throw QueryException("查询执行失败: ${e.message}", e)
        }
    }
    
    /**
     * 执行SQL Server更新操作
     */
    override suspend fun executeUpdate(
        connection: Connection,
        query: String,
        parameters: List<Any>
    ): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = connection.jdbcConnection
                ?: throw ConnectionException("连接已关闭")
            
            jdbcConnection.prepareStatement(query).use { stmt ->
                // 设置参数
                parameters.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                
                val startTime = System.currentTimeMillis()
                val affectedRows = stmt.executeUpdate()
                val endTime = System.currentTimeMillis()
                
                return@withContext UpdateResult(
                    affectedRows = affectedRows,
                    executionTime = endTime - startTime
                )
            }
        } catch (e: Exception) {
            logger.error("SQL Server更新执行失败: {}", query, e)
            throw QueryException("更新执行失败: ${e.message}", e)
        }
    }
    
    /**
     * 开始事务
     */
    override suspend fun beginTransaction(connection: Connection): Transaction = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = connection.jdbcConnection
                ?: throw ConnectionException("连接已关闭")
            
            jdbcConnection.autoCommit = false
            
            return@withContext Transaction(
                id = UUID.randomUUID().toString(),
                connection = connection,
                startTime = Instant.now()
            )
        } catch (e: Exception) {
            logger.error("开始SQL Server事务失败", e)
            throw TransactionException("开始事务失败: ${e.message}", e)
        }
    }
    
    /**
     * 提交事务
     */
    override suspend fun commitTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = transaction.connection.jdbcConnection
                ?: throw ConnectionException("连接已关闭")
            
            jdbcConnection.commit()
            jdbcConnection.autoCommit = true
        } catch (e: Exception) {
            logger.error("提交SQL Server事务失败", e)
            throw TransactionException("提交事务失败: ${e.message}", e)
        }
    }
    
    /**
     * 回滚事务
     */
    override suspend fun rollbackTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = transaction.connection.jdbcConnection
                ?: throw ConnectionException("连接已关闭")
            
            jdbcConnection.rollback()
            jdbcConnection.autoCommit = true
        } catch (e: Exception) {
            logger.error("回滚SQL Server事务失败", e)
            throw TransactionException("回滚事务失败: ${e.message}", e)
        }
    }
    
    /**
     * 创建SQL Server数据源
     */
    private fun createDataSource(config: ConnectionConfig): DataSource {
        val dataSource = SQLServerDataSource()
        dataSource.serverName = config.host
        dataSource.portNumber = config.port
        dataSource.databaseName = config.database
        dataSource.user = config.username
        dataSource.password = config.password
        
        // SSL配置
        if (config.sslEnabled) {
            dataSource.encrypt = "true"
            dataSource.trustServerCertificate = "false"
        }
        
        // 自定义属性
        config.properties.forEach { (key, value) ->
            when (key.lowercase()) {
                "applicationname" -> dataSource.applicationName = value
                "connecttimeout" -> dataSource.loginTimeout = value.toIntOrNull() ?: 30
                "sockettimeout" -> dataSource.socketTimeout = value.toIntOrNull() ?: 0
                "instancename" -> dataSource.instanceName = value
            }
        }
        
        return dataSource
    }
    
    /**
     * 检查是否为系统模式
     */
    private fun isSystemSchema(schemaName: String): Boolean {
        val systemSchemas = setOf(
            "sys",
            "information_schema",
            "db_owner",
            "db_accessadmin",
            "db_securityadmin",
            "db_ddladmin",
            "db_backupoperator",
            "db_datareader",
            "db_datawriter",
            "db_denydatareader",
            "db_denydatawriter",
            "guest"
        )
        return systemSchemas.contains(schemaName.lowercase())
    }
    
    /**
     * 获取表列信息
     */
    private fun getTableColumns(metaData: java.sql.DatabaseMetaData, schema: String, tableName: String): List<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()
        
        metaData.getColumns(null, schema, tableName, null).use { rs ->
            while (rs.next()) {
                val columnName = rs.getString("COLUMN_NAME")
                val dataType = rs.getInt("DATA_TYPE")
                val typeName = rs.getString("TYPE_NAME")
                val columnSize = rs.getInt("COLUMN_SIZE")
                val nullable = rs.getInt("NULLABLE") == java.sql.DatabaseMetaData.columnNullable
                val defaultValue = rs.getString("COLUMN_DEF")
                val remarks = rs.getString("REMARKS") ?: ""
                
                columns.add(
                    ColumnInfo(
                        name = columnName,
                        type = typeName,
                        size = columnSize,
                        nullable = nullable,
                        defaultValue = defaultValue,
                        comment = remarks,
                        isPrimaryKey = false, // 需要额外查询
                        isForeignKey = false  // 需要额外查询
                    )
                )
            }
        }
        
        return columns
    }
    
    /**
     * 获取结果集列信息
     */
    private fun getResultSetColumns(rs: ResultSet): List<ColumnInfo> {
        val metaData = rs.metaData
        val columns = mutableListOf<ColumnInfo>()
        
        for (i in 1..metaData.columnCount) {
            columns.add(
                ColumnInfo(
                    name = metaData.getColumnName(i),
                    type = metaData.getColumnTypeName(i),
                    size = metaData.getColumnDisplaySize(i),
                    nullable = metaData.isNullable(i) == ResultSet.COLUMN_NULLABLE,
                    defaultValue = null,
                    comment = "",
                    isPrimaryKey = false,
                    isForeignKey = false
                )
            )
        }
        
        return columns
    }
    
    /**
     * 获取列值
     */
    private fun getColumnValue(rs: ResultSet, column: ColumnInfo): Any? {
        return try {
            rs.getObject(column.name)
        } catch (e: Exception) {
            null
        }
    }
}