package ai.kastrax.a2x.adapter

import ai.kastrax.a2x.A2X
import ai.kastrax.a2x.EntityAdapter
import ai.kastrax.a2x.entity.Entity
import ai.kastrax.a2x.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.util.Base64
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * API 实体适配器，将 Web API 适配为 A2X 实体
 */
class APIEntityAdapter : EntityAdapter {
    /**
     * A2X 实例
     */
    private val a2x = A2X.getInstance()

    /**
     * 适配 Web API 为 A2X 实体
     */
    override fun adapt(obj: Any): Entity {
        if (obj !is APIConfig) {
            throw IllegalArgumentException("Object is not an APIConfig: ${obj.javaClass.name}")
        }

        return APIEntityImpl(obj)
    }

    /**
     * API 实体实现
     */
    private inner class APIEntityImpl(
        private val apiConfig: APIConfig,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) : Entity {
        /**
         * HTTP 客户端
         */
        private val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = apiConfig.timeout
                connectTimeoutMillis = apiConfig.timeout
                socketTimeoutMillis = apiConfig.timeout
            }
            defaultRequest {
                // 添加默认请求头
                apiConfig.defaultHeaders.forEach { (key, value) ->
                    header(key, value)
                }
                // 添加认证信息
                when (apiConfig.authentication?.type) {
                    AuthenticationType.API_KEY -> {
                        val keyName = apiConfig.authentication.metadata["key_name"] ?: "api_key"
                        val keyValue = apiConfig.authentication.metadata["key_value"] ?: ""
                        val keyLocation = apiConfig.authentication.metadata["key_location"] ?: "header"

                        when (keyLocation) {
                            "header" -> header(keyName, keyValue)
                            "query" -> url.parameters.append(keyName, keyValue)
                            else -> header(keyName, keyValue)
                        }
                    }
                    AuthenticationType.OTHER -> {
                        // 检查认证类型
                        val authType = apiConfig.authentication.metadata["auth_type"] ?: ""
                        when (authType) {
                            "bearer" -> {
                                val token = apiConfig.authentication.metadata["token"] ?: ""
                                header(HttpHeaders.Authorization, "Bearer $token")
                            }
                            "basic" -> {
                                val username = apiConfig.authentication.metadata["username"] ?: ""
                                val password = apiConfig.authentication.metadata["password"] ?: ""
                                header(HttpHeaders.Authorization, "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}")
                            }
                        }
                    }
                    else -> {
                        // 不添加认证信息
                    }
                }
            }
        }

        /**
         * 事件流
         */
        private val _eventFlow = MutableSharedFlow<EventMessage>()

        /**
         * 实体卡片
         */
        private val _entityCard: EntityCard by lazy {
            EntityCard(
                id = apiConfig.id,
                name = apiConfig.name,
                description = apiConfig.description,
                version = "1.0.0",
                type = EntityType.SYSTEM,
                endpoint = apiConfig.baseUrl,
                capabilities = buildCapabilities(),
                authentication = apiConfig.authentication ?: Authentication(type = AuthenticationType.OTHER)
            )
        }

        /**
         * 构建能力列表
         */
        private fun buildCapabilities(): List<Capability> {
            val capabilities = mutableListOf<Capability>()

            // 添加 API 端点能力
            apiConfig.endpoints.forEach { endpoint ->
                capabilities.add(
                    Capability(
                        id = endpoint.id,
                        name = endpoint.name,
                        description = endpoint.description,
                        parameters = endpoint.parameters.map { param ->
                            Parameter(
                                name = param.name,
                                type = param.type,
                                description = param.description,
                                required = param.required
                            )
                        },
                        returnType = "json"
                    )
                )
            }

            return capabilities
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

            // 查找端点
            val endpoint = apiConfig.endpoints.find { it.id == request.capabilityId }
                ?: throw IllegalArgumentException("Unsupported capability: ${request.capabilityId}")

            // 构建请求 URL
            val url = buildRequestUrl(endpoint, request.parameters)

            // 执行请求
            val result = when (endpoint.method.uppercase()) {
                "GET" -> executeGet(url, request.parameters, endpoint)
                "POST" -> executePost(url, request.parameters, endpoint)
                "PUT" -> executePut(url, request.parameters, endpoint)
                "DELETE" -> executeDelete(url, request.parameters, endpoint)
                "PATCH" -> executePatch(url, request.parameters, endpoint)
                else -> throw IllegalArgumentException("Unsupported HTTP method: ${endpoint.method}")
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
         * 构建请求 URL
         */
        private fun buildRequestUrl(endpoint: APIEndpoint, parameters: Map<String, JsonElement>): String {
            var url = apiConfig.baseUrl

            // 添加端点路径
            if (!url.endsWith("/") && !endpoint.path.startsWith("/")) {
                url += "/"
            }
            url += endpoint.path

            // 替换路径参数
            val pathParams = endpoint.parameters.filter { it.location == "path" }
            pathParams.forEach { param ->
                val value = parameters[param.name]?.let {
                    if (it is JsonPrimitive) it.content else it.toString()
                } ?: ""
                url = url.replace("{${param.name}}", value)
            }

            return url
        }

        /**
         * 执行 GET 请求
         */
        private suspend fun executeGet(url: String, parameters: Map<String, JsonElement>, endpoint: APIEndpoint): JsonElement {
            // 添加查询参数
            val queryParams = endpoint.parameters.filter { it.location == "query" }

            val response = httpClient.get(url) {
                // 添加查询参数
                queryParams.forEach { param ->
                    val value = parameters[param.name]
                    if (value != null) {
                        parameter(param.name, when (value) {
                            is JsonPrimitive -> value.content
                            else -> value.toString()
                        })
                    }
                }
            }

            // 解析响应
            return parseResponse(response)
        }

        /**
         * 执行 POST 请求
         */
        private suspend fun executePost(url: String, parameters: Map<String, JsonElement>, endpoint: APIEndpoint): JsonElement {
            // 添加请求体参数
            val bodyParams = endpoint.parameters.filter { it.location == "body" }

            val response = httpClient.post(url) {
                // 设置内容类型
                contentType(ContentType.Application.Json)

                // 添加请求体
                if (bodyParams.isNotEmpty()) {
                    val bodyJson = buildJsonObject {
                        bodyParams.forEach { param ->
                            val value = parameters[param.name]
                            if (value != null) {
                                put(param.name, value)
                            }
                        }
                    }
                    setBody(bodyJson)
                }
            }

            // 解析响应
            return parseResponse(response)
        }

        /**
         * 执行 PUT 请求
         */
        private suspend fun executePut(url: String, parameters: Map<String, JsonElement>, endpoint: APIEndpoint): JsonElement {
            // 添加请求体参数
            val bodyParams = endpoint.parameters.filter { it.location == "body" }

            val response = httpClient.put(url) {
                // 设置内容类型
                contentType(ContentType.Application.Json)

                // 添加请求体
                if (bodyParams.isNotEmpty()) {
                    val bodyJson = buildJsonObject {
                        bodyParams.forEach { param ->
                            val value = parameters[param.name]
                            if (value != null) {
                                put(param.name, value)
                            }
                        }
                    }
                    setBody(bodyJson)
                }
            }

            // 解析响应
            return parseResponse(response)
        }

        /**
         * 执行 DELETE 请求
         */
        private suspend fun executeDelete(url: String, parameters: Map<String, JsonElement>, endpoint: APIEndpoint): JsonElement {
            val response = httpClient.delete(url) {
                // 添加查询参数
                val queryParams = endpoint.parameters.filter { it.location == "query" }
                queryParams.forEach { param ->
                    val value = parameters[param.name]
                    if (value != null) {
                        parameter(param.name, when (value) {
                            is JsonPrimitive -> value.content
                            else -> value.toString()
                        })
                    }
                }
            }

            // 解析响应
            return parseResponse(response)
        }

        /**
         * 执行 PATCH 请求
         */
        private suspend fun executePatch(url: String, parameters: Map<String, JsonElement>, endpoint: APIEndpoint): JsonElement {
            // 添加请求体参数
            val bodyParams = endpoint.parameters.filter { it.location == "body" }

            val response = httpClient.patch(url) {
                // 设置内容类型
                contentType(ContentType.Application.Json)

                // 添加请求体
                if (bodyParams.isNotEmpty()) {
                    val bodyJson = buildJsonObject {
                        bodyParams.forEach { param ->
                            val value = parameters[param.name]
                            if (value != null) {
                                put(param.name, value)
                            }
                        }
                    }
                    setBody(bodyJson)
                }
            }

            // 解析响应
            return parseResponse(response)
        }

        /**
         * 解析响应
         */
        private suspend fun parseResponse(response: HttpResponse): JsonElement {
            // 获取响应状态
            val status = response.status.value

            // 获取响应内容
            val content = response.bodyAsText()

            // 尝试解析为 JSON
            return try {
                Json.parseToJsonElement(content)
            } catch (e: Exception) {
                // 如果解析失败，返回文本内容
                buildJsonObject {
                    put("status", JsonPrimitive(status))
                    put("content", JsonPrimitive(content))
                }
            }
        }

        /**
         * 查询实体状态
         */
        override suspend fun query(request: QueryRequest): QueryResponse {
            // 返回 API 状态
            return QueryResponse(
                id = request.id,
                source = EntityReference(
                    id = _entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = buildJsonObject {
                    put("status", JsonPrimitive("available"))
                    put("baseUrl", JsonPrimitive(apiConfig.baseUrl))
                    put("endpoints", buildJsonArray {
                        apiConfig.endpoints.forEach { endpoint ->
                            add(buildJsonObject {
                                put("id", JsonPrimitive(endpoint.id))
                                put("name", JsonPrimitive(endpoint.name))
                                put("method", JsonPrimitive(endpoint.method))
                                put("path", JsonPrimitive(endpoint.path))
                            })
                        }
                    })
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
            // API 实体不需要特殊启动逻辑
        }

        /**
         * 停止实体
         */
        override fun stop() {
            // 关闭 HTTP 客户端
            runBlocking {
                httpClient.close()
            }
        }
    }
}

/**
 * API 配置
 */
data class APIConfig(
    /**
     * API ID
     */
    val id: String,

    /**
     * API 名称
     */
    val name: String,

    /**
     * API 描述
     */
    val description: String,

    /**
     * 基础 URL
     */
    val baseUrl: String,

    /**
     * API 端点列表
     */
    val endpoints: List<APIEndpoint>,

    /**
     * 默认请求头
     */
    val defaultHeaders: Map<String, String> = emptyMap(),

    /**
     * 认证信息
     */
    val authentication: Authentication? = null,

    /**
     * 超时时间（毫秒）
     */
    val timeout: Long = 30000
)

/**
 * API 端点
 */
data class APIEndpoint(
    /**
     * 端点 ID
     */
    val id: String,

    /**
     * 端点名称
     */
    val name: String,

    /**
     * 端点描述
     */
    val description: String,

    /**
     * HTTP 方法
     */
    val method: String,

    /**
     * 端点路径
     */
    val path: String,

    /**
     * 参数列表
     */
    val parameters: List<APIParameter> = emptyList()
)

/**
 * API 参数
 */
data class APIParameter(
    /**
     * 参数名称
     */
    val name: String,

    /**
     * 参数类型
     */
    val type: String,

    /**
     * 参数描述
     */
    val description: String,

    /**
     * 是否必需
     */
    val required: Boolean = false,

    /**
     * 参数位置（query, path, body, header）
     */
    val location: String = "query"
)
