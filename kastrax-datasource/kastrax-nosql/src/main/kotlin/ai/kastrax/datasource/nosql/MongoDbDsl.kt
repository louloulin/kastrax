package ai.kastrax.datasource.nosql

import ai.kastrax.datasource.common.NoSqlConnector

/**
 * MongoDB 连接器 DSL 构建器。
 */
class MongoDbBuilder {
    private var name: String = "mongodb"
    private var connectionString: String = "mongodb://localhost:27017"
    private var databaseName: String = "test"
    private var connectTimeoutMs: Int = 5000
    private var socketTimeoutMs: Int = 10000
    private var maxPoolSize: Int = 100

    /**
     * 设置连接器名称。
     *
     * @param name 连接器名称。
     * @return 构建器实例。
     */
    fun name(name: String): MongoDbBuilder {
        this.name = name
        return this
    }

    /**
     * 设置连接字符串。
     *
     * @param connectionString 连接字符串。
     * @return 构建器实例。
     */
    fun connectionString(connectionString: String): MongoDbBuilder {
        this.connectionString = connectionString
        return this
    }

    /**
     * 设置主机。
     *
     * @param host 主机名或 IP 地址。
     * @return 构建器实例。
     */
    fun host(host: String): MongoDbBuilder {
        val currentUri = java.net.URI(this.connectionString)
        val userInfo = currentUri.userInfo
        val port = if (currentUri.port == -1) 27017 else currentUri.port
        
        this.connectionString = if (userInfo != null && userInfo.isNotEmpty()) {
            "mongodb://$userInfo@$host:$port"
        } else {
            "mongodb://$host:$port"
        }
        
        return this
    }

    /**
     * 设置端口。
     *
     * @param port 端口号。
     * @return 构建器实例。
     */
    fun port(port: Int): MongoDbBuilder {
        val currentUri = java.net.URI(this.connectionString)
        val userInfo = currentUri.userInfo
        val host = currentUri.host ?: "localhost"
        
        this.connectionString = if (userInfo != null && userInfo.isNotEmpty()) {
            "mongodb://$userInfo@$host:$port"
        } else {
            "mongodb://$host:$port"
        }
        
        return this
    }

    /**
     * 设置数据库名称。
     *
     * @param databaseName 数据库名称。
     * @return 构建器实例。
     */
    fun database(databaseName: String): MongoDbBuilder {
        this.databaseName = databaseName
        return this
    }

    /**
     * 设置用户名和密码。
     *
     * @param username 用户名。
     * @param password 密码。
     * @return 构建器实例。
     */
    fun credentials(username: String, password: String): MongoDbBuilder {
        val currentUri = java.net.URI(this.connectionString)
        val host = currentUri.host ?: "localhost"
        val port = if (currentUri.port == -1) 27017 else currentUri.port
        
        this.connectionString = "mongodb://$username:$password@$host:$port"
        return this
    }

    /**
     * 设置连接超时时间。
     *
     * @param timeoutMs 超时时间（毫秒）。
     * @return 构建器实例。
     */
    fun connectTimeout(timeoutMs: Int): MongoDbBuilder {
        this.connectTimeoutMs = timeoutMs
        return this
    }

    /**
     * 设置套接字超时时间。
     *
     * @param timeoutMs 超时时间（毫秒）。
     * @return 构建器实例。
     */
    fun socketTimeout(timeoutMs: Int): MongoDbBuilder {
        this.socketTimeoutMs = timeoutMs
        return this
    }

    /**
     * 设置最大连接池大小。
     *
     * @param size 连接池大小。
     * @return 构建器实例。
     */
    fun maxPoolSize(size: Int): MongoDbBuilder {
        this.maxPoolSize = size
        return this
    }

    /**
     * 构建 MongoDB 连接器。
     *
     * @return MongoDB 连接器实例。
     */
    fun build(): NoSqlConnector {
        return MongoDbConnector(
            name = name,
            connectionString = connectionString,
            databaseName = databaseName,
            connectTimeoutMs = connectTimeoutMs,
            socketTimeoutMs = socketTimeoutMs,
            maxPoolSize = maxPoolSize
        )
    }
}

/**
 * 创建 MongoDB 连接器的 DSL 函数。
 *
 * @param init 初始化函数。
 * @return MongoDB 连接器实例。
 */
fun mongodb(init: MongoDbBuilder.() -> Unit): NoSqlConnector {
    val builder = MongoDbBuilder()
    builder.init()
    return builder.build()
}
