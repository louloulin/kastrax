package ai.kastrax.datasource.common

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
     * 执行 SQL 批处理。
     *
     * @param sql SQL 语句。
     * @param paramsList 参数列表，每个元素是一组参数。
     * @return 每个批次受影响的行数数组。
     */
    suspend fun executeBatch(sql: String, paramsList: List<Map<String, Any>>): IntArray

    /**
     * 开始事务。
     *
     * @return 如果开始事务成功，则返回 true；否则返回 false。
     */
    suspend fun beginTransaction(): Boolean

    /**
     * 提交事务。
     *
     * @return 如果提交事务成功，则返回 true；否则返回 false。
     */
    suspend fun commitTransaction(): Boolean

    /**
     * 回滚事务。
     *
     * @return 如果回滚事务成功，则返回 true；否则返回 false。
     */
    suspend fun rollbackTransaction(): Boolean

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
