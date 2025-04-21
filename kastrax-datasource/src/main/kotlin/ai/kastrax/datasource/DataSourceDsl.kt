package ai.kastrax.datasource

import ai.kastrax.datasource.common.ApiConnector
import ai.kastrax.datasource.api.RestApiConnector
import ai.kastrax.datasource.common.DatabaseConnector
import ai.kastrax.datasource.common.FileSystemConnector

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
