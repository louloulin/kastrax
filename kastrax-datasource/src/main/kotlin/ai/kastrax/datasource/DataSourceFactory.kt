package ai.kastrax.datasource

import ai.kastrax.datasource.api.ApiConnector
import ai.kastrax.datasource.api.RestApiConnector
import ai.kastrax.datasource.database.DatabaseConnector
import ai.kastrax.datasource.database.MySqlConnector
import ai.kastrax.datasource.filesystem.FileSystemConnector
import ai.kastrax.datasource.filesystem.LocalFileSystemConnector

/**
 * 数据源工厂，用于创建各种数据源。
 */
object DataSourceFactory {
    /**
     * 创建 MySQL 数据库连接器。
     *
     * @param name 连接器名称。
     * @param host 数据库主机。
     * @param port 数据库端口。
     * @param database 数据库名称。
     * @param username 用户名。
     * @param password 密码。
     * @return MySQL 数据库连接器。
     */
    fun createMySqlConnector(
        name: String,
        host: String,
        port: Int = 3306,
        database: String,
        username: String,
        password: String
    ): DatabaseConnector {
        val url = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC"
        return MySqlConnector(name, url, username, password)
    }
    
    /**
     * 创建 RESTful API 连接器。
     *
     * @param name 连接器名称。
     * @param baseUrl API 基础 URL。
     * @param defaultHeaders 默认请求头。
     * @param authType 认证类型。
     * @param authToken 认证令牌。
     * @param username 用户名。
     * @param password 密码。
     * @return RESTful API 连接器。
     */
    fun createRestApiConnector(
        name: String,
        baseUrl: String,
        defaultHeaders: Map<String, String> = emptyMap(),
        authType: RestApiConnector.AuthType = RestApiConnector.AuthType.NONE,
        authToken: String = "",
        username: String = "",
        password: String = ""
    ): ApiConnector {
        return RestApiConnector(name, baseUrl, defaultHeaders, authType, authToken, username, password)
    }
    
    /**
     * 创建本地文件系统连接器。
     *
     * @param name 连接器名称。
     * @param rootPath 根目录路径。
     * @return 本地文件系统连接器。
     */
    fun createLocalFileSystemConnector(
        name: String,
        rootPath: String
    ): FileSystemConnector {
        return LocalFileSystemConnector(name, rootPath)
    }
}
