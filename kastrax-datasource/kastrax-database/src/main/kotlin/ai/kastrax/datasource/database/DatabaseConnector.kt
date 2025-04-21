package ai.kastrax.datasource.database

import ai.kastrax.datasource.DataSource
import ai.kastrax.datasource.DataSourceBase
import ai.kastrax.datasource.DataSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据库连接器接口，定义了数据库连接器的通用操作。
 */
interface DatabaseConnector : DataSource {
    /**
     * 执行 SQL 查询。
     *
     * @param sql SQL 查询语句。
     * @param params 查询参数。
     * @return 查询结果，以列表形式返回，每个元素是一个映射，键是列名，值是列值。
     */
    suspend fun executeQuery(sql: String, params: Map<String, Any> = emptyMap()): List<Map<String, Any>>
    
    /**
     * 执行 SQL 更新。
     *
     * @param sql SQL 更新语句。
     * @param params 更新参数。
     * @return 受影响的行数。
     */
    suspend fun executeUpdate(sql: String, params: Map<String, Any> = emptyMap()): Int
    
    /**
     * 获取数据库中的表列表。
     *
     * @return 表名列表。
     */
    suspend fun getTables(): List<String>
    
    /**
     * 获取表的列信息。
     *
     * @param table 表名。
     * @return 列信息列表，每个元素是一个映射，包含列名、类型等信息。
     */
    suspend fun getColumns(table: String): List<Map<String, Any>>
}

/**
 * 数据库连接器基类，提供了通用的实现。
 */
abstract class DatabaseConnectorBase(
    name: String
) : DataSourceBase(name, DataSourceType.DATABASE), DatabaseConnector {
    
    /**
     * 数据库连接 URL。
     */
    protected abstract val url: String
    
    /**
     * 数据库用户名。
     */
    protected abstract val username: String
    
    /**
     * 数据库密码。
     */
    protected abstract val password: String
    
    /**
     * 数据库驱动类名。
     */
    protected abstract val driverClassName: String
    
    override suspend fun executeQuery(sql: String, params: Map<String, Any>): List<Map<String, Any>> {
        logger.debug { "Executing query: $sql with params: $params" }
        return withContext(Dispatchers.IO) {
            doExecuteQuery(sql, params)
        }
    }
    
    override suspend fun executeUpdate(sql: String, params: Map<String, Any>): Int {
        logger.debug { "Executing update: $sql with params: $params" }
        return withContext(Dispatchers.IO) {
            doExecuteUpdate(sql, params)
        }
    }
    
    override suspend fun getTables(): List<String> {
        logger.debug { "Getting tables" }
        return withContext(Dispatchers.IO) {
            doGetTables()
        }
    }
    
    override suspend fun getColumns(table: String): List<Map<String, Any>> {
        logger.debug { "Getting columns for table: $table" }
        return withContext(Dispatchers.IO) {
            doGetColumns(table)
        }
    }
    
    /**
     * 执行实际的 SQL 查询操作。
     *
     * @param sql SQL 查询语句。
     * @param params 查询参数。
     * @return 查询结果。
     */
    protected abstract fun doExecuteQuery(sql: String, params: Map<String, Any>): List<Map<String, Any>>
    
    /**
     * 执行实际的 SQL 更新操作。
     *
     * @param sql SQL 更新语句。
     * @param params 更新参数。
     * @return 受影响的行数。
     */
    protected abstract fun doExecuteUpdate(sql: String, params: Map<String, Any>): Int
    
    /**
     * 执行实际的获取表列表操作。
     *
     * @return 表名列表。
     */
    protected abstract fun doGetTables(): List<String>
    
    /**
     * 执行实际的获取列信息操作。
     *
     * @param table 表名。
     * @return 列信息列表。
     */
    protected abstract fun doGetColumns(table: String): List<Map<String, Any>>
}
