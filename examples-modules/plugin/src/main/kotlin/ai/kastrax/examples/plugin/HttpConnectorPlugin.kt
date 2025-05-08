package ai.kastrax.examples.plugin

import ai.kastrax.core.plugin.AbstractConnectorPlugin
import ai.kastrax.core.plugin.ConfigField
import ai.kastrax.core.plugin.ConfigFieldType
import ai.kastrax.core.plugin.Connector
import ai.kastrax.core.plugin.ConnectorMetadata
import ai.kastrax.core.plugin.ConnectorOperation
import ai.kastrax.core.plugin.ConnectorStatus
import ai.kastrax.core.plugin.ConnectorTestResult
import ai.kastrax.core.plugin.ConnectorType
import ai.kastrax.core.plugin.DefaultPluginConfig
import ai.kastrax.core.plugin.PluginContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging

/**
 * HTTP连接器插件，提供与HTTP服务的集成。
 */
class HttpConnectorPlugin : AbstractConnectorPlugin(
    id = "ai.kastrax.plugin.connector.http",
    name = "HTTP Connector",
    description = "Provides integration with HTTP services",
    version = "1.0.0",
    author = "KastraX Team",
    config = DefaultPluginConfig()
) {
    private val pluginLogger = KotlinLogging.logger {}

    override fun initialize(context: PluginContext): Boolean {
        pluginLogger.info { "初始化HTTP连接器插件" }
        return true
    }

    override fun start(context: PluginContext): Boolean {
        pluginLogger.info { "启动HTTP连接器插件" }
        return true
    }

    override fun stop(context: PluginContext): Boolean {
        pluginLogger.info { "停止HTTP连接器插件" }
        return true
    }

    override fun getConnectorTypes(): List<ConnectorType> {
        return listOf(
            ConnectorType(
                id = "http",
                name = "HTTP",
                description = "Connect to HTTP services",
                icon = "http",
                configSchema = mapOf(
                    "baseUrl" to ConfigField(
                        name = "Base URL",
                        type = ConfigFieldType.STRING,
                        description = "Base URL of the HTTP service",
                        required = true
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "HTTP headers to include in requests",
                        required = false
                    ),
                    "timeout" to ConfigField(
                        name = "Timeout",
                        type = ConfigFieldType.NUMBER,
                        description = "Request timeout in milliseconds",
                        required = false,
                        defaultValue = 30000
                    )
                ),
                operations = listOf(
                    ConnectorOperation(
                        id = "get",
                        name = "GET",
                        description = "Perform a GET request",
                        parameterSchema = mapOf(
                            "path" to ConfigField(
                                name = "Path",
                                type = ConfigFieldType.STRING,
                                description = "Request path",
                                required = true
                            ),
                            "queryParams" to ConfigField(
                                name = "Query Parameters",
                                type = ConfigFieldType.OBJECT,
                                description = "Query parameters",
                                required = false
                            )
                        )
                    ),
                    ConnectorOperation(
                        id = "post",
                        name = "POST",
                        description = "Perform a POST request",
                        parameterSchema = mapOf(
                            "path" to ConfigField(
                                name = "Path",
                                type = ConfigFieldType.STRING,
                                description = "Request path",
                                required = true
                            ),
                            "body" to ConfigField(
                                name = "Body",
                                type = ConfigFieldType.OBJECT,
                                description = "Request body",
                                required = false
                            ),
                            "contentType" to ConfigField(
                                name = "Content Type",
                                type = ConfigFieldType.STRING,
                                description = "Content type",
                                required = false,
                                defaultValue = "application/json"
                            )
                        )
                    )
                ),
                category = "Integration",
                tags = listOf("http", "rest", "api")
            )
        )
    }

    override fun createConnector(connectorType: String, config: Map<String, Any?>): Connector? {
        if (connectorType != "http") {
            logger.warn { "不支持的连接器类型: $connectorType" }
            return null
        }

        val baseUrl = config["baseUrl"] as? String
            ?: throw IllegalArgumentException("缺少必需的配置: baseUrl")

        val headers = config["headers"] as? Map<String, String> ?: emptyMap()
        val timeout = (config["timeout"] as? Number)?.toLong() ?: 30000L

        return HttpConnector(
            id = "http-${System.currentTimeMillis()}",
            name = "HTTP Connector",
            description = "HTTP connector to $baseUrl",
            baseUrl = baseUrl,
            headers = headers,
            timeout = timeout
        )
    }
}

/**
 * HTTP连接器，提供与HTTP服务的集成。
 */
class HttpConnector(
    override val id: String,
    override val name: String,
    override val description: String,
    private val baseUrl: String,
    private val headers: Map<String, String>,
    private val timeout: Long
) : Connector {
    private val logger = KotlinLogging.logger {}
    private var client: HttpClient? = null

    override val type: String = "http"
    override val config: Map<String, Any?> = mapOf(
        "baseUrl" to baseUrl,
        "headers" to headers,
        "timeout" to timeout
    )

    override var status: ConnectorStatus = ConnectorStatus.CREATED
        private set

    private val createdAt = System.currentTimeMillis()
    private var lastConnectedAt: Long? = null

    override suspend fun connect(): Boolean {
        try {
            client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
                engine {
                    requestTimeout = timeout
                }
            }

            status = ConnectorStatus.CONNECTED
            lastConnectedAt = System.currentTimeMillis()

            logger.info { "连接到HTTP服务: $baseUrl" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "连接到HTTP服务失败: $baseUrl" }
            status = ConnectorStatus.ERROR
            return false
        }
    }

    override suspend fun disconnect(): Boolean {
        try {
            client?.close()
            client = null

            status = ConnectorStatus.DISCONNECTED

            logger.info { "断开与HTTP服务的连接: $baseUrl" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "断开与HTTP服务的连接失败: $baseUrl" }
            status = ConnectorStatus.ERROR
            return false
        }
    }

    override suspend fun testConnection(): ConnectorTestResult {
        return try {
            val client = this.client ?: HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
                engine {
                    requestTimeout = timeout
                }
            }

            val response = client.get("$baseUrl/")

            if (this.client == null) {
                client.close()
            }

            ConnectorTestResult(
                success = response.status.value in 200..299,
                message = "HTTP status: ${response.status.value}",
                details = mapOf(
                    "status" to response.status.value,
                    "statusText" to response.status.description
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "测试HTTP连接失败: $baseUrl" }

            ConnectorTestResult(
                success = false,
                message = "连接失败: ${e.message}",
                details = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "errorType" to e.javaClass.simpleName
                )
            )
        }
    }

    override suspend fun execute(operation: String, parameters: Map<String, Any?>): JsonElement {
        val client = this.client ?: throw IllegalStateException("连接器未连接")

        return when (operation) {
            "get" -> {
                val path = parameters["path"] as? String
                    ?: throw IllegalArgumentException("缺少必需的参数: path")

                val queryParams = parameters["queryParams"] as? Map<String, String> ?: emptyMap()

                val url = buildUrl(path, queryParams)

                val response = client.get(url) {
                    val headerMap = parameters["headers"] as? Map<String, String> ?: emptyMap()
                    headerMap.forEach { (name, value) ->
                        headers.append(name, value)
                    }
                }

                val responseText = response.bodyAsText()

                try {
                    Json.parseToJsonElement(responseText)
                } catch (e: Exception) {
                    buildJsonObject {
                        put("status", response.status.value)
                        put("statusText", response.status.description)
                        put("body", responseText)
                    }
                }
            }
            "post" -> {
                val path = parameters["path"] as? String
                    ?: throw IllegalArgumentException("缺少必需的参数: path")

                val body = parameters["body"]
                val contentType = parameters["contentType"] as? String ?: "application/json"

                val url = buildUrl(path)

                val response = client.post(url) {
                    val headerMap = parameters["headers"] as? Map<String, String> ?: emptyMap()
                    headerMap.forEach { (name, value) ->
                        headers.append(name, value)
                    }

                    contentType(ContentType.parse(contentType))

                    if (body != null) {
                        when (body) {
                            is JsonElement -> setBody(body)
                            is Map<*, *> -> setBody(body)
                            is String -> setBody(body)
                            else -> setBody(body.toString())
                        }
                    }
                }

                val responseText = response.bodyAsText()

                try {
                    Json.parseToJsonElement(responseText)
                } catch (e: Exception) {
                    buildJsonObject {
                        put("status", response.status.value)
                        put("statusText", response.status.description)
                        put("body", responseText)
                    }
                }
            }
            else -> throw IllegalArgumentException("不支持的操作: $operation")
        }
    }

    override suspend fun executeStream(operation: String, parameters: Map<String, Any?>): Flow<JsonElement> = flow {
        val result = execute(operation, parameters)
        emit(result)
    }

    override fun getOperations(): List<ConnectorOperation> {
        return listOf(
            ConnectorOperation(
                id = "get",
                name = "GET",
                description = "Perform a GET request",
                parameterSchema = mapOf(
                    "path" to ConfigField(
                        name = "Path",
                        type = ConfigFieldType.STRING,
                        description = "Request path",
                        required = true
                    ),
                    "queryParams" to ConfigField(
                        name = "Query Parameters",
                        type = ConfigFieldType.OBJECT,
                        description = "Query parameters",
                        required = false
                    )
                )
            ),
            ConnectorOperation(
                id = "post",
                name = "POST",
                description = "Perform a POST request",
                parameterSchema = mapOf(
                    "path" to ConfigField(
                        name = "Path",
                        type = ConfigFieldType.STRING,
                        description = "Request path",
                        required = true
                    ),
                    "body" to ConfigField(
                        name = "Body",
                        type = ConfigFieldType.OBJECT,
                        description = "Request body",
                        required = false
                    ),
                    "contentType" to ConfigField(
                        name = "Content Type",
                        type = ConfigFieldType.STRING,
                        description = "Content type",
                        required = false,
                        defaultValue = "application/json"
                    )
                )
            )
        )
    }

    override fun getMetadata(): ConnectorMetadata {
        return ConnectorMetadata(
            id = id,
            name = name,
            description = description,
            type = type,
            status = status,
            createdAt = createdAt,
            lastConnectedAt = lastConnectedAt,
            config = mapOf(
                "baseUrl" to baseUrl,
                "timeout" to timeout
                // 不包含headers，因为它可能包含敏感信息
            ),
            operations = getOperations()
        )
    }

    /**
     * 构建URL。
     *
     * @param path 路径
     * @param queryParams 查询参数
     * @return URL
     */
    private fun buildUrl(path: String, queryParams: Map<String, String> = emptyMap()): String {
        val baseUrl = if (this.baseUrl.endsWith("/")) this.baseUrl else "${this.baseUrl}/"
        val pathWithoutLeadingSlash = if (path.startsWith("/")) path.substring(1) else path

        val url = baseUrl + pathWithoutLeadingSlash

        if (queryParams.isEmpty()) {
            return url
        }

        val queryString = queryParams.entries.joinToString("&") { (key, value) ->
            "$key=${value.encodeUrl()}"
        }

        return if (url.contains("?")) {
            "$url&$queryString"
        } else {
            "$url?$queryString"
        }
    }

    /**
     * URL编码。
     */
    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}
