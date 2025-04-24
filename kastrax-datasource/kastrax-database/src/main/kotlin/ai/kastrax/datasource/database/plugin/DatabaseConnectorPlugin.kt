package ai.kastrax.datasource.database.plugin

import ai.kastrax.core.plugin.*
import ai.kastrax.datasource.database.plugin.connectors.PostgresConnector
import ai.kastrax.datasource.database.plugin.connectors.MongoConnector
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import java.util.UUID
import ai.kastrax.core.plugin.ConfigField
import ai.kastrax.core.plugin.ConfigFieldType

/**
 * 数据库连接器插件，提供与各种数据库系统的集成。
 */
class DatabaseConnectorPlugin : AbstractConnectorPlugin(
    id = "ai.kastrax.plugins.database",
    name = "Database Connector Plugin",
    description = "提供与各种数据库系统的集成，包括PostgreSQL和MongoDB",
    version = "1.0.0",
    author = "KastraX Team"
) {
    private val connectorTypes = listOf(
        ConnectorType(
            id = "postgres",
            name = "PostgreSQL",
            description = "PostgreSQL数据库连接器",
            icon = "database",
            configSchema = mapOf(
                "host" to ConfigField(
                    name = "host",
                    type = ConfigFieldType.STRING,
                    description = "数据库主机",
                    required = true
                ),
                "port" to ConfigField(
                    name = "port",
                    type = ConfigFieldType.NUMBER,
                    description = "数据库端口",
                    required = true,
                    defaultValue = 5432
                ),
                "database" to ConfigField(
                    name = "database",
                    type = ConfigFieldType.STRING,
                    description = "数据库名称",
                    required = true
                ),
                "username" to ConfigField(
                    name = "username",
                    type = ConfigFieldType.STRING,
                    description = "用户名",
                    required = true
                ),
                "password" to ConfigField(
                    name = "password",
                    type = ConfigFieldType.STRING,
                    description = "密码",
                    required = true
                ),
                "schema" to ConfigField(
                    name = "schema",
                    type = ConfigFieldType.STRING,
                    description = "模式",
                    required = false,
                    defaultValue = "public"
                )
            ),
            operations = listOf(
                ConnectorOperation(
                    id = "query",
                    name = "执行查询",
                    description = "执行SQL查询",
                    parameterSchema = mapOf(
                        "sql" to ConfigField(
                            name = "sql",
                            type = ConfigFieldType.STRING,
                            description = "SQL查询",
                            required = true
                        ),
                        "params" to ConfigField(
                            name = "params",
                            type = ConfigFieldType.OBJECT,
                            description = "查询参数",
                            required = false
                        )
                    )
                ),
                ConnectorOperation(
                    id = "update",
                    name = "执行更新",
                    description = "执行SQL更新",
                    parameterSchema = mapOf(
                        "sql" to ConfigField(
                            name = "sql",
                            type = ConfigFieldType.STRING,
                            description = "SQL更新",
                            required = true
                        ),
                        "params" to ConfigField(
                            name = "params",
                            type = ConfigFieldType.OBJECT,
                            description = "更新参数",
                            required = false
                        )
                    )
                ),
                ConnectorOperation(
                    id = "batch",
                    name = "执行批处理",
                    description = "执行SQL批处理",
                    parameterSchema = mapOf(
                        "sql" to ConfigField(
                            name = "sql",
                            type = ConfigFieldType.STRING,
                            description = "SQL语句",
                            required = true
                        ),
                        "paramsList" to ConfigField(
                            name = "paramsList",
                            type = ConfigFieldType.ARRAY,
                            description = "参数列表",
                            required = true
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
                        "table" to ConfigField(
                            name = "table",
                            type = ConfigFieldType.STRING,
                            description = "表名",
                            required = true
                        )
                    )
                )
            ),
            category = "数据库",
            tags = listOf("SQL", "关系型数据库", "PostgreSQL")
        ),
        ConnectorType(
            id = "mongodb",
            name = "MongoDB",
            description = "MongoDB数据库连接器",
            icon = "database",
            configSchema = mapOf(
                "connectionString" to ConfigField(
                    name = "connectionString",
                    type = ConfigFieldType.STRING,
                    description = "连接字符串",
                    required = true
                ),
                "database" to ConfigField(
                    name = "database",
                    type = ConfigFieldType.STRING,
                    description = "数据库名称",
                    required = true
                )
            ),
            operations = listOf(
                ConnectorOperation(
                    id = "find",
                    name = "查找文档",
                    description = "查找匹配条件的文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "filter" to ConfigField(
                            name = "filter",
                            type = ConfigFieldType.OBJECT,
                            description = "过滤条件",
                            required = false,
                            defaultValue = "{}"
                        ),
                        "projection" to ConfigField(
                            name = "projection",
                            type = ConfigFieldType.OBJECT,
                            description = "投影",
                            required = false
                        ),
                        "limit" to ConfigField(
                            name = "limit",
                            type = ConfigFieldType.NUMBER,
                            description = "限制数量",
                            required = false,
                            defaultValue = 100
                        ),
                        "skip" to ConfigField(
                            name = "skip",
                            type = ConfigFieldType.NUMBER,
                            description = "跳过数量",
                            required = false,
                            defaultValue = 0
                        )
                    )
                ),
                ConnectorOperation(
                    id = "findOne",
                    name = "查找单个文档",
                    description = "查找匹配条件的单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "filter" to ConfigField(
                            name = "filter",
                            type = ConfigFieldType.OBJECT,
                            description = "过滤条件",
                            required = false,
                            defaultValue = "{}"
                        ),
                        "projection" to ConfigField(
                            name = "projection",
                            type = ConfigFieldType.OBJECT,
                            description = "投影",
                            required = false
                        )
                    )
                ),
                ConnectorOperation(
                    id = "insertOne",
                    name = "插入文档",
                    description = "插入单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "document" to ConfigField(
                            name = "document",
                            type = ConfigFieldType.OBJECT,
                            description = "文档",
                            required = true
                        )
                    )
                ),
                ConnectorOperation(
                    id = "insertMany",
                    name = "批量插入文档",
                    description = "插入多个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "documents" to ConfigField(
                            name = "documents",
                            type = ConfigFieldType.ARRAY,
                            description = "文档列表",
                            required = true
                        )
                    )
                ),
                ConnectorOperation(
                    id = "updateOne",
                    name = "更新文档",
                    description = "更新单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "filter" to ConfigField(
                            name = "filter",
                            type = ConfigFieldType.OBJECT,
                            description = "过滤条件",
                            required = true
                        ),
                        "update" to ConfigField(
                            name = "update",
                            type = ConfigFieldType.OBJECT,
                            description = "更新操作",
                            required = true
                        )
                    )
                ),
                ConnectorOperation(
                    id = "updateMany",
                    name = "批量更新文档",
                    description = "更新多个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "filter" to ConfigField(
                            name = "filter",
                            type = ConfigFieldType.OBJECT,
                            description = "过滤条件",
                            required = true
                        ),
                        "update" to ConfigField(
                            name = "update",
                            type = ConfigFieldType.OBJECT,
                            description = "更新操作",
                            required = true
                        )
                    )
                ),
                ConnectorOperation(
                    id = "deleteOne",
                    name = "删除文档",
                    description = "删除单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "filter" to ConfigField(
                            name = "filter",
                            type = ConfigFieldType.OBJECT,
                            description = "过滤条件",
                            required = true
                        )
                    )
                ),
                ConnectorOperation(
                    id = "deleteMany",
                    name = "批量删除文档",
                    description = "删除多个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField(
                            name = "collection",
                            type = ConfigFieldType.STRING,
                            description = "集合名称",
                            required = true
                        ),
                        "filter" to ConfigField(
                            name = "filter",
                            type = ConfigFieldType.OBJECT,
                            description = "过滤条件",
                            required = true
                        )
                    )
                ),
                ConnectorOperation(
                    id = "getCollections",
                    name = "获取集合列表",
                    description = "获取数据库中的集合列表",
                    parameterSchema = mapOf()
                )
            ),
            category = "数据库",
            tags = listOf("NoSQL", "文档数据库", "MongoDB")
        )
    )

    override fun getConnectorTypes(): List<ConnectorType> {
        return connectorTypes
    }

    override fun createConnector(connectorType: String, config: Map<String, Any?>): Connector? {
        return when (connectorType) {
            "postgres" -> createPostgresConnector(config)
            "mongodb" -> createMongoConnector(config)
            else -> {
                logger.warn { "不支持的连接器类型: $connectorType" }
                null
            }
        }
    }

    private fun createPostgresConnector(config: Map<String, Any?>): Connector {
        val host = config["host"] as String
        val port = (config["port"] as? String)?.toInt() ?: 5432
        val database = config["database"] as String
        val username = config["username"] as String
        val password = config["password"] as String
        val schema = config["schema"] as? String ?: "public"

        val id = config["id"] as? String ?: "postgres-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "PostgreSQL Connector"

        return PostgresConnector(
            id = id,
            connectorName = name,
            description = "PostgreSQL数据库连接器",
            config = config,
            host = host,
            port = port,
            database = database,
            username = username,
            password = password,
            schema = schema
        )
    }

    private fun createMongoConnector(config: Map<String, Any?>): Connector {
        val connectionString = config["connectionString"] as String
        val database = config["database"] as String

        val id = config["id"] as? String ?: "mongodb-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "MongoDB Connector"

        return MongoConnector(
            id = id,
            connectorName = name,
            description = "MongoDB数据库连接器",
            config = config,
            connectionString = connectionString,
            database = database
        )
    }
}

// Using ConfigField from core module
