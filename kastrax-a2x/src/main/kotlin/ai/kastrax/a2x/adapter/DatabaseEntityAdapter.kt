package ai.kastrax.a2x.adapter

import ai.kastrax.a2x.A2X
import ai.kastrax.a2x.EntityAdapter
import ai.kastrax.a2x.entity.Entity
import ai.kastrax.a2x.model.*
import ai.kastrax.datasource.common.DatabaseConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据库实体适配器，将数据库连接器适配为 A2X 实体
 */
class DatabaseEntityAdapter : EntityAdapter {
    /**
     * A2X 实例
     */
    private val a2x = A2X.getInstance()

    /**
     * 适配数据库连接器为 A2X 实体
     */
    override fun adapt(obj: Any): Entity {
        if (obj !is DatabaseConnector) {
            throw IllegalArgumentException("Object is not a DatabaseConnector: ${obj.javaClass.name}")
        }

        return DatabaseEntityImpl(obj)
    }

    /**
     * 数据库实体实现
     */
    private inner class DatabaseEntityImpl(
        private val databaseConnector: DatabaseConnector,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) : Entity {
        /**
         * 事件流
         */
        private val _eventFlow = MutableSharedFlow<EventMessage>()

        /**
         * 实体卡片
         */
        private val _entityCard: EntityCard by lazy {
            EntityCard(
                id = databaseConnector.sourceName,
                name = databaseConnector.sourceName,
                description = "Database connector for ${databaseConnector.sourceName}",
                version = "1.0.0",
                type = EntityType.SYSTEM,
                endpoint = "",
                capabilities = listOf(
                    // 查询能力
                    Capability(
                        id = "query",
                        name = "数据库查询",
                        description = "执行数据库查询操作",
                        parameters = listOf(
                            Parameter(
                                name = "sql",
                                type = "string",
                                description = "SQL查询语句",
                                required = true
                            ),
                            Parameter(
                                name = "params",
                                type = "object",
                                description = "查询参数",
                                required = false
                            )
                        ),
                        returnType = "json"
                    ),
                    // 执行能力
                    Capability(
                        id = "execute",
                        name = "数据库执行",
                        description = "执行数据库更新操作",
                        parameters = listOf(
                            Parameter(
                                name = "sql",
                                type = "string",
                                description = "SQL执行语句",
                                required = true
                            ),
                            Parameter(
                                name = "params",
                                type = "object",
                                description = "执行参数",
                                required = false
                            )
                        ),
                        returnType = "json"
                    ),
                    // 元数据能力
                    Capability(
                        id = "metadata",
                        name = "数据库元数据",
                        description = "获取数据库元数据信息",
                        parameters = listOf(
                            Parameter(
                                name = "type",
                                type = "string",
                                description = "元数据类型（tables, columns, indexes等）",
                                required = true
                            ),
                            Parameter(
                                name = "name",
                                type = "string",
                                description = "对象名称",
                                required = false
                            )
                        ),
                        returnType = "json"
                    )
                ),
                authentication = Authentication(
                    type = AuthenticationType.API_KEY,
                    metadata = emptyMap()
                )
            )
        }

        /**
         * 获取实体卡片
         */
        override fun getEntityCard(): EntityCard = _entityCard

        /**
         * 获取实体能力
         */
        override fun getCapabilities(): List<Capability> = _entityCard.capabilities

        /**
         * 调用实体能力
         */
        override suspend fun invoke(request: InvokeRequest): InvokeResponse {
            // 验证请求
            if (request.target.id != _entityCard.id) {
                throw IllegalArgumentException("Invalid target ID: ${request.target.id}")
            }

            // 根据能力ID执行相应操作
            val result = when (request.capabilityId) {
                "query" -> executeQuery(request.parameters)
                "execute" -> executeStatement(request.parameters)
                "metadata" -> getMetadata(request.parameters)
                else -> throw IllegalArgumentException("Unsupported capability: ${request.capabilityId}")
            }

            // 返回响应
            return InvokeResponse(
                id = request.id,
                source = EntityReference(
                    id = _entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = result
            )
        }

        /**
         * 查询数据库
         */
        private suspend fun executeQuery(parameters: Map<String, JsonElement>): JsonElement {
            // 获取查询参数
            val sql = parameters["sql"]?.let {
                if (it is JsonPrimitive) it.content else throw IllegalArgumentException("Invalid sql parameter")
            } ?: throw IllegalArgumentException("Missing sql parameter")

            val params = parameters["params"]?.let {
                if (it is JsonObject) {
                    it.entries.associate { entry ->
                        entry.key to when (val value = entry.value) {
                            is JsonPrimitive -> {
                                when {
                                    value.isString -> value.content
                                    value.intOrNull != null -> value.int
                                    value.longOrNull != null -> value.long
                                    value.doubleOrNull != null -> value.double
                                    value.booleanOrNull != null -> value.boolean
                                    else -> value.content
                                }
                            }
                            else -> value.toString()
                        }
                    }
                } else {
                    emptyMap()
                }
            } ?: emptyMap()

            // 执行查询
            val result = databaseConnector.executeQuery(sql, params)

            // 转换结果为JSON
            return buildJsonArray {
                result.forEach { row ->
                    add(buildJsonObject {
                        row.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, JsonPrimitive(value))
                                is Number -> put(key, JsonPrimitive(value.toDouble()))
                                is Boolean -> put(key, JsonPrimitive(value))
                                null -> put(key, JsonNull)
                                else -> put(key, JsonPrimitive(value.toString()))
                            }
                        }
                    })
                }
            }
        }

        /**
         * 执行数据库语句
         */
        private suspend fun executeStatement(parameters: Map<String, JsonElement>): JsonElement {
            // 获取执行参数
            val sql = parameters["sql"]?.let {
                if (it is JsonPrimitive) it.content else throw IllegalArgumentException("Invalid sql parameter")
            } ?: throw IllegalArgumentException("Missing sql parameter")

            val params = parameters["params"]?.let {
                if (it is JsonObject) {
                    it.entries.associate { entry ->
                        entry.key to when (val value = entry.value) {
                            is JsonPrimitive -> {
                                when {
                                    value.isString -> value.content
                                    value.intOrNull != null -> value.int
                                    value.longOrNull != null -> value.long
                                    value.doubleOrNull != null -> value.double
                                    value.booleanOrNull != null -> value.boolean
                                    else -> value.content
                                }
                            }
                            else -> value.toString()
                        }
                    }
                } else {
                    emptyMap()
                }
            } ?: emptyMap()

            // 执行语句
            val affectedRows = databaseConnector.executeUpdate(sql, params)

            // 返回结果
            return buildJsonObject {
                put("success", JsonPrimitive(true))
                put("affectedRows", JsonPrimitive(affectedRows))
            }
        }

        /**
         * 获取数据库元数据
         */
        private suspend fun getMetadata(parameters: Map<String, JsonElement>): JsonElement {
            // 获取元数据参数
            val type = parameters["type"]?.let {
                if (it is JsonPrimitive) it.content else throw IllegalArgumentException("Invalid type parameter")
            } ?: throw IllegalArgumentException("Missing type parameter")

            val name = parameters["name"]?.let {
                if (it is JsonPrimitive) it.content else null
            }

            // 获取元数据
            return when (type) {
                "tables" -> getTablesMetadata(name)
                "columns" -> getColumnsMetadata(name)
                "indexes" -> getIndexesMetadata(name)
                else -> throw IllegalArgumentException("Unsupported metadata type: $type")
            }
        }

        /**
         * 获取表元数据
         */
        private suspend fun getTablesMetadata(tableName: String?): JsonElement {
            // 这里应该调用数据库连接器的方法获取表元数据
            // 由于 DatabaseConnector 接口可能没有直接提供这些方法，这里使用模拟数据
            return buildJsonArray {
                add(buildJsonObject {
                    put("table_name", JsonPrimitive("users"))
                    put("table_type", JsonPrimitive("TABLE"))
                })
                add(buildJsonObject {
                    put("table_name", JsonPrimitive("orders"))
                    put("table_type", JsonPrimitive("TABLE"))
                })
            }
        }

        /**
         * 获取列元数据
         */
        private suspend fun getColumnsMetadata(tableName: String?): JsonElement {
            // 这里应该调用数据库连接器的方法获取列元数据
            // 由于 DatabaseConnector 接口可能没有直接提供这些方法，这里使用模拟数据
            if (tableName == null) {
                throw IllegalArgumentException("Table name is required for columns metadata")
            }

            return buildJsonArray {
                if (tableName == "users") {
                    add(buildJsonObject {
                        put("column_name", JsonPrimitive("id"))
                        put("data_type", JsonPrimitive("INTEGER"))
                        put("is_nullable", JsonPrimitive(false))
                    })
                    add(buildJsonObject {
                        put("column_name", JsonPrimitive("name"))
                        put("data_type", JsonPrimitive("VARCHAR"))
                        put("is_nullable", JsonPrimitive(true))
                    })
                } else if (tableName == "orders") {
                    add(buildJsonObject {
                        put("column_name", JsonPrimitive("id"))
                        put("data_type", JsonPrimitive("INTEGER"))
                        put("is_nullable", JsonPrimitive(false))
                    })
                    add(buildJsonObject {
                        put("column_name", JsonPrimitive("user_id"))
                        put("data_type", JsonPrimitive("INTEGER"))
                        put("is_nullable", JsonPrimitive(false))
                    })
                }
            }
        }

        /**
         * 获取索引元数据
         */
        private suspend fun getIndexesMetadata(tableName: String?): JsonElement {
            // 这里应该调用数据库连接器的方法获取索引元数据
            // 由于 DatabaseConnector 接口可能没有直接提供这些方法，这里使用模拟数据
            if (tableName == null) {
                throw IllegalArgumentException("Table name is required for indexes metadata")
            }

            return buildJsonArray {
                if (tableName == "users") {
                    add(buildJsonObject {
                        put("index_name", JsonPrimitive("users_pkey"))
                        put("column_name", JsonPrimitive("id"))
                        put("is_unique", JsonPrimitive(true))
                    })
                } else if (tableName == "orders") {
                    add(buildJsonObject {
                        put("index_name", JsonPrimitive("orders_pkey"))
                        put("column_name", JsonPrimitive("id"))
                        put("is_unique", JsonPrimitive(true))
                    })
                    add(buildJsonObject {
                        put("index_name", JsonPrimitive("orders_user_id_idx"))
                        put("column_name", JsonPrimitive("user_id"))
                        put("is_unique", JsonPrimitive(false))
                    })
                }
            }
        }

        /**
         * 查询实体状态
         */
        override suspend fun query(request: QueryRequest): QueryResponse {
            // 返回数据库状态
            return QueryResponse(
                id = request.id,
                source = EntityReference(
                    id = _entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = buildJsonObject {
                    put("status", JsonPrimitive(if (databaseConnector.isConnected()) "connected" else "disconnected"))
                    put("name", JsonPrimitive(databaseConnector.sourceName))
                    put("type", JsonPrimitive("database"))
                }
            )
        }

        /**
         * 处理 A2X 消息
         */
        override suspend fun processMessage(message: A2XMessage): A2XMessage {
            // 处理消息
            return when (message) {
                is InvokeRequest -> invoke(message)
                is QueryRequest -> query(message)
                is EventMessage -> {
                    // 处理事件消息
                    if (message.target.id == _entityCard.id || message.target.id == "*") {
                        // 发送事件到事件流
                        _eventFlow.emit(message)
                    }
                    // 返回事件确认
                    EventAcknowledgement(
                        id = UUID.randomUUID().toString(),
                        source = EntityReference(
                            id = _entityCard.id,
                            type = EntityType.SYSTEM
                        ),
                        target = message.source,
                        eventId = message.id,
                        status = "received"
                    )
                }
                else -> throw UnsupportedOperationException("Unsupported message type: ${message::class.simpleName}")
            }
        }

        /**
         * 发送事件
         */
        override suspend fun sendEvent(event: EventMessage) {
            // 发送事件到事件流
            _eventFlow.emit(event)
        }

        /**
         * 订阅事件
         */
        override fun subscribeToEvents(eventTypes: List<String>): Flow<EventMessage> {
            // 过滤事件类型
            return if (eventTypes.isEmpty()) {
                _eventFlow.asSharedFlow()
            } else {
                _eventFlow.filter { event -> eventTypes.contains(event.eventType) }
            }
        }

        /**
         * 启动实体
         */
        override fun start() {
            // 连接数据库
            runBlocking {
                if (!databaseConnector.isConnected()) {
                    databaseConnector.connect()
                }
            }
        }

        /**
         * 停止实体
         */
        override fun stop() {
            // 关闭数据库连接
            runBlocking {
                if (databaseConnector.isConnected()) {
                    databaseConnector.disconnect()
                }
            }
        }
    }
}
