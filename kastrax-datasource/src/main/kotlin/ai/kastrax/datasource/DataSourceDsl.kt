package ai.kastrax.datasource

import ai.kastrax.datasource.common.ApiConnector
import ai.kastrax.datasource.api.RestApiConnector
import ai.kastrax.datasource.common.DatabaseConnector
import ai.kastrax.datasource.common.FileSystemConnector
import ai.kastrax.datasource.common.NoSqlConnector
import ai.kastrax.datasource.common.GraphQlConnector
import ai.kastrax.datasource.common.CloudStorageConnector
import ai.kastrax.datasource.common.AuthType

/**
 * MySQL 数据库连接器配置类，用于 DSL 构建。
 */
class MySqlConnectorConfig {
    var name: String = "mysql"
    var host: String = "localhost"
    var port: Int = 3306
    var database: String = ""
    var username: String = ""
    var password: String = ""

    /**
     * 设置连接器名称。
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * 设置数据库主机。
     */
    fun host(host: String) {
        this.host = host
    }

    /**
     * 设置数据库端口。
     */
    fun port(port: Int) {
        this.port = port
    }

    /**
     * 设置数据库名称。
     */
    fun database(database: String) {
        this.database = database
    }

    /**
     * 设置用户名。
     */
    fun username(username: String) {
        this.username = username
    }

    /**
     * 设置密码。
     */
    fun password(password: String) {
        this.password = password
    }
}

/**
 * RESTful API 连接器配置类，用于 DSL 构建。
 */
class RestApiConnectorConfig {
    var name: String = "rest-api"
    var baseUrl: String = ""
    var defaultHeaders: MutableMap<String, String> = mutableMapOf()
    var authType: RestApiConnector.AuthType = RestApiConnector.AuthType.NONE
    var authToken: String = ""
    var username: String = ""
    var password: String = ""

    /**
     * 设置连接器名称。
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * 设置 API 基础 URL。
     */
    fun baseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    /**
     * 添加默认请求头。
     */
    fun header(key: String, value: String) {
        defaultHeaders[key] = value
    }

    /**
     * 设置 Bearer 认证。
     */
    fun bearerAuth(token: String) {
        authType = RestApiConnector.AuthType.BEARER
        authToken = token
    }

    /**
     * 设置 Basic 认证。
     */
    fun basicAuth(username: String, password: String) {
        authType = RestApiConnector.AuthType.BASIC
        this.username = username
        this.password = password
    }

    /**
     * 设置 API Key 认证。
     */
    fun apiKeyAuth(token: String) {
        authType = RestApiConnector.AuthType.API_KEY
        authToken = token
    }
}

/**
 * 本地文件系统连接器配置类，用于 DSL 构建。
 */
class LocalFileSystemConnectorConfig {
    var name: String = "local-fs"
    var rootPath: String = ""

    /**
     * 设置连接器名称。
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * 设置根目录路径。
     */
    fun rootPath(rootPath: String) {
        this.rootPath = rootPath
    }
}

/**
 * 创建 MySQL 数据库连接器。
 *
 * @param init 配置初始化函数。
 * @return MySQL 数据库连接器。
 */
fun mysql(init: MySqlConnectorConfig.() -> Unit): DatabaseConnector {
    val config = MySqlConnectorConfig().apply(init)

    if (config.database.isEmpty()) {
        throw IllegalArgumentException("Database name is required")
    }

    return DataSourceFactory.createMySqlConnector(
        name = config.name,
        host = config.host,
        port = config.port,
        database = config.database,
        username = config.username,
        password = config.password
    )
}

/**
 * 创建 RESTful API 连接器。
 *
 * @param init 配置初始化函数。
 * @return RESTful API 连接器。
 */
fun restApi(init: RestApiConnectorConfig.() -> Unit): ApiConnector {
    val config = RestApiConnectorConfig().apply(init)

    if (config.baseUrl.isEmpty()) {
        throw IllegalArgumentException("Base URL is required")
    }

    return DataSourceFactory.createRestApiConnector(
        name = config.name,
        baseUrl = config.baseUrl,
        defaultHeaders = config.defaultHeaders,
        authType = config.authType,
        authToken = config.authToken,
        username = config.username,
        password = config.password
    )
}

/**
 * 创建本地文件系统连接器。
 *
 * @param init 配置初始化函数。
 * @return 本地文件系统连接器。
 */
fun localFileSystem(init: LocalFileSystemConnectorConfig.() -> Unit): FileSystemConnector {
    val config = LocalFileSystemConnectorConfig().apply(init)

    if (config.rootPath.isEmpty()) {
        throw IllegalArgumentException("Root path is required")
    }

    return DataSourceFactory.createLocalFileSystemConnector(
        name = config.name,
        rootPath = config.rootPath
    )
}

/**
 * MongoDB 数据库连接器配置类，用于 DSL 构建。
 */
class MongoDbConnectorConfig {
    var name: String = "mongodb"
    var host: String = "localhost"
    var port: Int = 27017
    var database: String = ""
    var username: String? = null
    var password: String? = null
    var connectTimeoutMs: Int = 5000
    var socketTimeoutMs: Int = 10000
    var maxPoolSize: Int = 100

    /**
     * 设置连接器名称。
     *
     * @param name 连接器名称。
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * 设置主机。
     *
     * @param host 主机名或 IP 地址。
     */
    fun host(host: String) {
        this.host = host
    }

    /**
     * 设置端口。
     *
     * @param port 端口号。
     */
    fun port(port: Int) {
        this.port = port
    }

    /**
     * 设置数据库名称。
     *
     * @param database 数据库名称。
     */
    fun database(database: String) {
        this.database = database
    }

    /**
     * 设置用户名和密码。
     *
     * @param username 用户名。
     * @param password 密码。
     */
    fun credentials(username: String, password: String) {
        this.username = username
        this.password = password
    }

    /**
     * 设置连接超时时间。
     *
     * @param timeoutMs 超时时间（毫秒）。
     */
    fun connectTimeout(timeoutMs: Int) {
        this.connectTimeoutMs = timeoutMs
    }

    /**
     * 设置套接字超时时间。
     *
     * @param timeoutMs 超时时间（毫秒）。
     */
    fun socketTimeout(timeoutMs: Int) {
        this.socketTimeoutMs = timeoutMs
    }

    /**
     * 设置最大连接池大小。
     *
     * @param size 连接池大小。
     */
    fun maxPoolSize(size: Int) {
        this.maxPoolSize = size
    }
}

/**
 * 创建 MongoDB 连接器的 DSL 函数。
 *
 * @param init 初始化函数。
 * @return MongoDB 连接器。
 */
fun mongodb(init: MongoDbConnectorConfig.() -> Unit): NoSqlConnector {
    val config = MongoDbConnectorConfig().apply(init)

    if (config.database.isEmpty()) {
        throw IllegalArgumentException("Database name is required")
    }

    return DataSourceFactory.createMongoDbConnector(
        name = config.name,
        host = config.host,
        port = config.port,
        database = config.database,
        username = config.username,
        password = config.password,
        connectTimeoutMs = config.connectTimeoutMs,
        socketTimeoutMs = config.socketTimeoutMs,
        maxPoolSize = config.maxPoolSize
    )
}

/**
 * GraphQL 连接器配置类，用于 DSL 构建。
 */
class GraphQlConnectorConfig {
    var name: String = "graphql"
    var endpoint: String = ""
    var defaultHeaders: Map<String, String> = emptyMap()
    var authType: AuthType = AuthType.NONE
    var authToken: String = ""
    var username: String = ""
    var password: String = ""

    /**
     * 设置连接器名称。
     *
     * @param name 连接器名称。
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * 设置 GraphQL 端点 URL。
     *
     * @param endpoint 端点 URL。
     */
    fun endpoint(endpoint: String) {
        this.endpoint = endpoint
    }

    /**
     * 添加请求头。
     *
     * @param name 请求头名称。
     * @param value 请求头值。
     */
    fun header(name: String, value: String) {
        this.defaultHeaders = this.defaultHeaders + (name to value)
    }

    /**
     * 设置 Bearer 认证。
     *
     * @param token 认证令牌。
     */
    fun bearerAuth(token: String) {
        this.authType = AuthType.BEARER
        this.authToken = token
    }

    /**
     * 设置 Basic 认证。
     *
     * @param username 用户名。
     * @param password 密码。
     */
    fun basicAuth(username: String, password: String) {
        this.authType = AuthType.BASIC
        this.username = username
        this.password = password
    }
}

/**
 * 创建 GraphQL 连接器的 DSL 函数。
 *
 * @param init 初始化函数。
 * @return GraphQL 连接器。
 */
fun graphql(init: GraphQlConnectorConfig.() -> Unit): GraphQlConnector {
    val config = GraphQlConnectorConfig().apply(init)

    if (config.endpoint.isEmpty()) {
        throw IllegalArgumentException("GraphQL endpoint is required")
    }

    return DataSourceFactory.createGraphQlConnector(
        name = config.name,
        endpoint = config.endpoint,
        defaultHeaders = config.defaultHeaders,
        authType = config.authType,
        authToken = config.authToken,
        username = config.username,
        password = config.password
    )
}

/**
 * S3 连接器配置类，用于 DSL 构建。
 */
class S3ConnectorConfig {
    var name: String = "s3"
    var bucketName: String = ""
    var region: String = "us-east-1"
    var accessKey: String = ""
    var secretKey: String = ""
    var endpoint: String? = null
    var pathStyleAccessEnabled: Boolean = false

    /**
     * 设置连接器名称。
     *
     * @param name 连接器名称。
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * 设置存储桶名称。
     *
     * @param bucketName 存储桶名称。
     */
    fun bucket(bucketName: String) {
        this.bucketName = bucketName
    }

    /**
     * 设置区域。
     *
     * @param region 区域。
     */
    fun region(region: String) {
        this.region = region
    }

    /**
     * 设置访问凭证。
     *
     * @param accessKey 访问密钥。
     * @param secretKey 秘密密钥。
     */
    fun credentials(accessKey: String, secretKey: String) {
        this.accessKey = accessKey
        this.secretKey = secretKey
    }

    /**
     * 设置自定义端点。
     *
     * @param endpoint 端点 URL。
     */
    fun endpoint(endpoint: String) {
        this.endpoint = endpoint
    }

    /**
     * 启用路径样式访问。
     *
     * @param enabled 是否启用。
     */
    fun pathStyleAccess(enabled: Boolean) {
        this.pathStyleAccessEnabled = enabled
    }
}

/**
 * 创建 S3 连接器的 DSL 函数。
 *
 * @param init 初始化函数。
 * @return S3 连接器。
 */
fun s3(init: S3ConnectorConfig.() -> Unit): CloudStorageConnector {
    val config = S3ConnectorConfig().apply(init)

    if (config.bucketName.isEmpty()) {
        throw IllegalArgumentException("Bucket name is required")
    }

    if (config.accessKey.isEmpty() || config.secretKey.isEmpty()) {
        throw IllegalArgumentException("Access key and secret key are required")
    }

    return DataSourceFactory.createS3Connector(
        name = config.name,
        bucketName = config.bucketName,
        region = config.region,
        accessKey = config.accessKey,
        secretKey = config.secretKey,
        endpoint = config.endpoint,
        pathStyleAccessEnabled = config.pathStyleAccessEnabled
    )
}
