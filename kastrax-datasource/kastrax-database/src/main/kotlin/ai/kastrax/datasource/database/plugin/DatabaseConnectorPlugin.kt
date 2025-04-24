package ai.kastrax.datasource.database.plugin

import ai.kastrax.core.plugin.*
import ai.kastrax.datasource.database.plugin.connectors.PostgresConnector
import ai.kastrax.datasource.database.plugin.connectors.MongoConnector
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import java.util.UUID

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
                "host" to ConfigField("host", "数据库主机", "string", true),
                "port" to ConfigField("port", "数据库端口", "integer", true, "5432"),
                "database" to ConfigField("database", "数据库名称", "string", true),
                "username" to ConfigField("username", "用户名", "string", true),
                "password" to ConfigField("password", "密码", "string", true, null, true),
                "schema" to ConfigField("schema", "模式", "string", false, "public")
            ),
            operations = listOf(
                ConnectorOperation(
                    id = "query",
                    name = "执行查询",
                    description = "执行SQL查询",
                    parameterSchema = mapOf(
                        "sql" to ConfigField("sql", "SQL查询", "string", true),
                        "params" to ConfigField("params", "查询参数", "object", false)
                    )
                ),
                ConnectorOperation(
                    id = "update",
                    name = "执行更新",
                    description = "执行SQL更新",
                    parameterSchema = mapOf(
                        "sql" to ConfigField("sql", "SQL更新", "string", true),
                        "params" to ConfigField("params", "更新参数", "object", false)
                    )
                ),
                ConnectorOperation(
                    id = "batch",
                    name = "执行批处理",
                    description = "执行SQL批处理",
                    parameterSchema = mapOf(
                        "sql" to ConfigField("sql", "SQL语句", "string", true),
                        "paramsList" to ConfigField("paramsList", "参数列表", "array", true)
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
                        "table" to ConfigField("table", "表名", "string", true)
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
                "connectionString" to ConfigField("connectionString", "连接字符串", "string", true),
                "database" to ConfigField("database", "数据库名称", "string", true)
            ),
            operations = listOf(
                ConnectorOperation(
                    id = "find",
                    name = "查找文档",
                    description = "查找匹配条件的文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "filter" to ConfigField("filter", "过滤条件", "object", false, "{}"),
                        "projection" to ConfigField("projection", "投影", "object", false),
                        "limit" to ConfigField("limit", "限制数量", "integer", false, "100"),
                        "skip" to ConfigField("skip", "跳过数量", "integer", false, "0")
                    )
                ),
                ConnectorOperation(
                    id = "findOne",
                    name = "查找单个文档",
                    description = "查找匹配条件的单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "filter" to ConfigField("filter", "过滤条件", "object", false, "{}"),
                        "projection" to ConfigField("projection", "投影", "object", false)
                    )
                ),
                ConnectorOperation(
                    id = "insertOne",
                    name = "插入文档",
                    description = "插入单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "document" to ConfigField("document", "文档", "object", true)
                    )
                ),
                ConnectorOperation(
                    id = "insertMany",
                    name = "批量插入文档",
                    description = "插入多个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "documents" to ConfigField("documents", "文档列表", "array", true)
                    )
                ),
                ConnectorOperation(
                    id = "updateOne",
                    name = "更新文档",
                    description = "更新单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "filter" to ConfigField("filter", "过滤条件", "object", true),
                        "update" to ConfigField("update", "更新操作", "object", true)
                    )
                ),
                ConnectorOperation(
                    id = "updateMany",
                    name = "批量更新文档",
                    description = "更新多个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "filter" to ConfigField("filter", "过滤条件", "object", true),
                        "update" to ConfigField("update", "更新操作", "object", true)
                    )
                ),
                ConnectorOperation(
                    id = "deleteOne",
                    name = "删除文档",
                    description = "删除单个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "filter" to ConfigField("filter", "过滤条件", "object", true)
                    )
                ),
                ConnectorOperation(
                    id = "deleteMany",
                    name = "批量删除文档",
                    description = "删除多个文档",
                    parameterSchema = mapOf(
                        "collection" to ConfigField("collection", "集合名称", "string", true),
                        "filter" to ConfigField("filter", "过滤条件", "object", true)
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
            name = name,
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
            name = name,
            description = "MongoDB数据库连接器",
            config = config,
            connectionString = connectionString,
            database = database
        )
    }
}

/**
 * 配置字段，描述了连接器配置中的一个字段。
 */
data class ConfigField(
    val name: String,
    val description: String,
    val type: String,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val sensitive: Boolean = false
)
