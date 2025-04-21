package ai.kastrax.datasource

import ai.kastrax.datasource.common.ApiConnector
import ai.kastrax.datasource.api.RestApiConnector
import ai.kastrax.datasource.common.DatabaseConnector
import ai.kastrax.datasource.database.MySqlConnector
import ai.kastrax.datasource.common.FileSystemConnector
import ai.kastrax.datasource.filesystem.LocalFileSystemConnector
import ai.kastrax.datasource.common.NoSqlConnector
import ai.kastrax.datasource.nosql.MongoDbConnector
import ai.kastrax.datasource.common.GraphQlConnector
import ai.kastrax.datasource.api.GraphQlConnectorImpl
import ai.kastrax.datasource.common.CloudStorageConnector
import ai.kastrax.datasource.cloud.S3Connector

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

    /**
     * 创建 MongoDB 连接器。
     *
     * @param name 连接器名称。
     * @param host 数据库主机。
     * @param port 数据库端口。
     * @param database 数据库名称。
     * @param username 用户名，可选。
     * @param password 密码，可选。
     * @param connectTimeoutMs 连接超时时间（毫秒）。
     * @param socketTimeoutMs 套接字超时时间（毫秒）。
     * @param maxPoolSize 最大连接池大小。
     * @return MongoDB 连接器。
     */
    fun createMongoDbConnector(
        name: String,
        host: String = "localhost",
        port: Int = 27017,
        database: String,
        username: String? = null,
        password: String? = null,
        connectTimeoutMs: Int = 5000,
        socketTimeoutMs: Int = 10000,
        maxPoolSize: Int = 100
    ): NoSqlConnector {
        val connectionString = if (username != null && password != null) {
            "mongodb://$username:$password@$host:$port"
        } else {
            "mongodb://$host:$port"
        }

        return MongoDbConnector(
            name = name,
            connectionString = connectionString,
            databaseName = database,
            connectTimeoutMs = connectTimeoutMs,
            socketTimeoutMs = socketTimeoutMs,
            maxPoolSize = maxPoolSize
        )
    }

    /**
     * 创建 GraphQL 连接器。
     *
     * @param name 连接器名称。
     * @param endpoint GraphQL 端点 URL。
     * @param defaultHeaders 默认请求头。
     * @param authType 认证类型。
     * @param authToken 认证令牌。
     * @param username 用户名。
     * @param password 密码。
     * @return GraphQL 连接器。
     */
    fun createGraphQlConnector(
        name: String,
        endpoint: String,
        defaultHeaders: Map<String, String> = emptyMap(),
        authType: AuthType = AuthType.NONE,
        authToken: String = "",
        username: String = "",
        password: String = ""
    ): GraphQlConnector {
        return GraphQlConnectorImpl(
            name = name,
            baseUrl = endpoint,
            defaultHeaders = defaultHeaders,
            authType = authType,
            authToken = authToken,
            username = username,
            password = password
        )
    }

    /**
     * 创建 S3 连接器。
     *
     * @param name 连接器名称。
     * @param bucketName 存储桶名称。
     * @param region 区域。
     * @param accessKey 访问密钥。
     * @param secretKey 秘密密钥。
     * @param endpoint 自定义端点，可选。
     * @param pathStyleAccessEnabled 是否启用路径样式访问，可选。
     * @return S3 连接器。
     */
    fun createS3Connector(
        name: String,
        bucketName: String,
        region: String,
        accessKey: String,
        secretKey: String,
        endpoint: String? = null,
        pathStyleAccessEnabled: Boolean = false
    ): CloudStorageConnector {
        return S3Connector(
            name = name,
            bucketName = bucketName,
            region = region,
            accessKey = accessKey,
            secretKey = secretKey,
            endpoint = endpoint,
            pathStyleAccessEnabled = pathStyleAccessEnabled
        )
    }
}
