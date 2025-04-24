package ai.kastrax.examples.plugin

import ai.kastrax.core.plugin.AbstractStepPlugin
import ai.kastrax.core.plugin.ConfigField
import ai.kastrax.core.plugin.ConfigFieldType
import ai.kastrax.core.plugin.DefaultPluginConfig
import ai.kastrax.core.plugin.PluginContext
import ai.kastrax.core.plugin.StepType
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging

/**
 * HTTP步骤插件，提供HTTP请求步骤。
 */
class HttpStepPlugin : AbstractStepPlugin(
    id = "ai.kastrax.plugin.step.http",
    name = "HTTP Step",
    description = "Provides steps for making HTTP requests",
    version = "1.0.0",
    author = "KastraX Team",
    config = DefaultPluginConfig()
) {
    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    override fun initialize(context: PluginContext): Boolean {
        logger.info { "初始化HTTP步骤插件" }
        return true
    }
    
    override fun start(context: PluginContext): Boolean {
        logger.info { "启动HTTP步骤插件" }
        return true
    }
    
    override fun stop(context: PluginContext): Boolean {
        logger.info { "停止HTTP步骤插件" }
        httpClient.close()
        return true
    }
    
    override fun getStepTypes(): List<StepType> {
        return listOf(
            StepType(
                id = "http-get",
                name = "HTTP GET",
                description = "Make an HTTP GET request",
                icon = "http",
                configSchema = mapOf(
                    "url" to ConfigField(
                        name = "URL",
                        type = ConfigFieldType.STRING,
                        description = "URL to request",
                        required = true
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "HTTP headers",
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
                inputSchema = mapOf(
                    "url" to ConfigField(
                        name = "URL",
                        type = ConfigFieldType.STRING,
                        description = "URL to request (overrides config)",
                        required = false
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "HTTP headers (merged with config)",
                        required = false
                    )
                ),
                outputSchema = mapOf(
                    "status" to ConfigField(
                        name = "Status",
                        type = ConfigFieldType.NUMBER,
                        description = "HTTP status code"
                    ),
                    "body" to ConfigField(
                        name = "Body",
                        type = ConfigFieldType.OBJECT,
                        description = "Response body"
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "Response headers"
                    )
                ),
                category = "Integration",
                tags = listOf("http", "rest", "api")
            ),
            StepType(
                id = "http-post",
                name = "HTTP POST",
                description = "Make an HTTP POST request",
                icon = "http",
                configSchema = mapOf(
                    "url" to ConfigField(
                        name = "URL",
                        type = ConfigFieldType.STRING,
                        description = "URL to request",
                        required = true
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "HTTP headers",
                        required = false
                    ),
                    "contentType" to ConfigField(
                        name = "Content Type",
                        type = ConfigFieldType.STRING,
                        description = "Content type",
                        required = false,
                        defaultValue = "application/json"
                    ),
                    "timeout" to ConfigField(
                        name = "Timeout",
                        type = ConfigFieldType.NUMBER,
                        description = "Request timeout in milliseconds",
                        required = false,
                        defaultValue = 30000
                    )
                ),
                inputSchema = mapOf(
                    "url" to ConfigField(
                        name = "URL",
                        type = ConfigFieldType.STRING,
                        description = "URL to request (overrides config)",
                        required = false
                    ),
                    "body" to ConfigField(
                        name = "Body",
                        type = ConfigFieldType.OBJECT,
                        description = "Request body",
                        required = true
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "HTTP headers (merged with config)",
                        required = false
                    ),
                    "contentType" to ConfigField(
                        name = "Content Type",
                        type = ConfigFieldType.STRING,
                        description = "Content type (overrides config)",
                        required = false
                    )
                ),
                outputSchema = mapOf(
                    "status" to ConfigField(
                        name = "Status",
                        type = ConfigFieldType.NUMBER,
                        description = "HTTP status code"
                    ),
                    "body" to ConfigField(
                        name = "Body",
                        type = ConfigFieldType.OBJECT,
                        description = "Response body"
                    ),
                    "headers" to ConfigField(
                        name = "Headers",
                        type = ConfigFieldType.OBJECT,
                        description = "Response headers"
                    )
                ),
                category = "Integration",
                tags = listOf("http", "rest", "api")
            )
        )
    }
    
    override fun createStep(stepType: String, config: Map<String, Any?>): WorkflowStep? {
        return when (stepType) {
            "http-get" -> {
                val url = config["url"] as? String
                    ?: throw IllegalArgumentException("缺少必需的配置: url")
                
                val headers = config["headers"] as? Map<String, String> ?: emptyMap()
                val timeout = (config["timeout"] as? Number)?.toLong() ?: 30000L
                
                HttpGetStep(
                    id = "http-get-${System.currentTimeMillis()}",
                    name = "HTTP GET",
                    description = "HTTP GET request to $url",
                    after = emptyList(),
                    variables = emptyMap(),
                    url = url,
                    headers = headers,
                    timeout = timeout
                )
            }
            "http-post" -> {
                val url = config["url"] as? String
                    ?: throw IllegalArgumentException("缺少必需的配置: url")
                
                val headers = config["headers"] as? Map<String, String> ?: emptyMap()
                val contentType = config["contentType"] as? String ?: "application/json"
                val timeout = (config["timeout"] as? Number)?.toLong() ?: 30000L
                
                HttpPostStep(
                    id = "http-post-${System.currentTimeMillis()}",
                    name = "HTTP POST",
                    description = "HTTP POST request to $url",
                    after = emptyList(),
                    variables = emptyMap(),
                    url = url,
                    headers = headers,
                    contentType = contentType,
                    timeout = timeout
                )
            }
            else -> {
                logger.warn { "不支持的步骤类型: $stepType" }
                null
            }
        }
    }
}

/**
 * HTTP GET步骤，执行HTTP GET请求。
 */
class HttpGetStep(
    override val id: String,
    override val name: String,
    override val description: String,
    override val after: List<String>,
    override val variables: Map<String, VariableReference>,
    private val url: String,
    private val headers: Map<String, String>,
    private val timeout: Long
) : WorkflowStep {
    private val logger = KotlinLogging.logger {}
    
    override val config: StepConfig? = object : StepConfig {
        override fun getProperty(key: String): Any? {
            return when (key) {
                "url" -> url
                "headers" -> headers
                "timeout" -> timeout
                else -> null
            }
        }
        
        override fun getAllProperties(): Map<String, Any?> {
            return mapOf(
                "url" to url,
                "headers" to headers,
                "timeout" to timeout
            )
        }
    }
    
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        try {
            // 获取输入参数
            val inputUrl = context.input["url"] as? String ?: url
            val inputHeaders = context.input["headers"] as? Map<String, String> ?: emptyMap()
            
            // 合并headers
            val mergedHeaders = headers + inputHeaders
            
            // 创建HTTP客户端
            val client = HttpClient(CIO) {
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
            
            // 执行请求
            val response = client.get(inputUrl) {
                mergedHeaders.forEach { (name, value) ->
                    headers {
                        append(name, value)
                    }
                }
            }
            
            // 解析响应
            val responseText = response.bodyAsText()
            val responseHeaders = response.headers.entries().associate { it.key to it.value }
            
            // 解析JSON响应
            val responseBody = try {
                Json.parseToJsonElement(responseText)
            } catch (e: Exception) {
                JsonPrimitive(responseText)
            }
            
            // 关闭客户端
            client.close()
            
            // 返回结果
            return WorkflowStepResult(
                stepId = id,
                success = response.status.value in 200..299,
                output = mapOf(
                    "status" to response.status.value,
                    "body" to responseBody,
                    "headers" to responseHeaders
                ),
                executionTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            logger.error(e) { "执行HTTP GET请求失败: $url" }
            
            return WorkflowStepResult(
                stepId = id,
                success = false,
                error = e.message ?: "Unknown error",
                output = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "errorType" to e.javaClass.simpleName
                ),
                executionTime = System.currentTimeMillis()
            )
        }
    }
}

/**
 * HTTP POST步骤，执行HTTP POST请求。
 */
class HttpPostStep(
    override val id: String,
    override val name: String,
    override val description: String,
    override val after: List<String>,
    override val variables: Map<String, VariableReference>,
    private val url: String,
    private val headers: Map<String, String>,
    private val contentType: String,
    private val timeout: Long
) : WorkflowStep {
    private val logger = KotlinLogging.logger {}
    
    override val config: StepConfig? = object : StepConfig {
        override fun getProperty(key: String): Any? {
            return when (key) {
                "url" -> url
                "headers" -> headers
                "contentType" -> contentType
                "timeout" -> timeout
                else -> null
            }
        }
        
        override fun getAllProperties(): Map<String, Any?> {
            return mapOf(
                "url" to url,
                "headers" to headers,
                "contentType" to contentType,
                "timeout" to timeout
            )
        }
    }
    
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        try {
            // 获取输入参数
            val inputUrl = context.input["url"] as? String ?: url
            val inputBody = context.input["body"]
                ?: throw IllegalArgumentException("缺少必需的输入: body")
            val inputHeaders = context.input["headers"] as? Map<String, String> ?: emptyMap()
            val inputContentType = context.input["contentType"] as? String ?: contentType
            
            // 合并headers
            val mergedHeaders = headers + inputHeaders
            
            // 创建HTTP客户端
            val client = HttpClient(CIO) {
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
            
            // 执行请求
            val response = client.post(inputUrl) {
                mergedHeaders.forEach { (name, value) ->
                    headers {
                        append(name, value)
                    }
                }
                
                contentType(ContentType.parse(inputContentType))
                
                when (inputBody) {
                    is JsonElement -> setBody(inputBody)
                    is Map<*, *> -> setBody(inputBody)
                    is String -> setBody(inputBody)
                    else -> setBody(inputBody.toString())
                }
            }
            
            // 解析响应
            val responseText = response.bodyAsText()
            val responseHeaders = response.headers.entries().associate { it.key to it.value }
            
            // 解析JSON响应
            val responseBody = try {
                Json.parseToJsonElement(responseText)
            } catch (e: Exception) {
                JsonPrimitive(responseText)
            }
            
            // 关闭客户端
            client.close()
            
            // 返回结果
            return WorkflowStepResult(
                stepId = id,
                success = response.status.value in 200..299,
                output = mapOf(
                    "status" to response.status.value,
                    "body" to responseBody,
                    "headers" to responseHeaders
                ),
                executionTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            logger.error(e) { "执行HTTP POST请求失败: $url" }
            
            return WorkflowStepResult(
                stepId = id,
                success = false,
                error = e.message ?: "Unknown error",
                output = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "errorType" to e.javaClass.simpleName
                ),
                executionTime = System.currentTimeMillis()
            )
        }
    }
}
