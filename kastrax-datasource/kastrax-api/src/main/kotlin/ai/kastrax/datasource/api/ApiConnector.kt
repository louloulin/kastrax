package ai.kastrax.datasource.api

import ai.kastrax.datasource.common.DataSource
import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import mu.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * API 连接器接口，定义了 API 连接器的通用操作。
 */
interface ApiConnector : DataSource {
    /**
     * 发送 GET 请求。
     *
     * @param path 请求路径。
     * @param queryParams 查询参数。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * 发送 POST 请求。
     *
     * @param path 请求路径。
     * @param body 请求体。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun post(
        path: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * 发送 PUT 请求。
     *
     * @param path 请求路径。
     * @param body 请求体。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun put(
        path: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * 发送 DELETE 请求。
     *
     * @param path 请求路径。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun delete(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse
}

/**
 * API 响应类，包含响应状态码、头信息和内容。
 */
data class ApiResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val content: String,
    val json: JsonElement? = null
)

/**
 * API 连接器基类，提供了通用的实现。
 */
abstract class ApiConnectorBase(
    name: String
) : DataSourceBase(name, DataSourceType.API), ApiConnector {

    /**
     * API 基础 URL。
     */
    protected abstract val baseUrl: String

    /**
     * 默认请求头。
     */
    protected abstract val defaultHeaders: Map<String, String>

    /**
     * HTTP 客户端。
     */
    protected val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 30000
        }

        expectSuccess = false
    }

    override suspend fun get(
        path: String,
        queryParams: Map<String, String>,
        headers: Map<String, String>
    ): ApiResponse {
        logger.debug { "Sending GET request to: $path with params: $queryParams" }

        val response = httpClient.get("$baseUrl/$path") {
            headers {
                appendAll(defaultHeaders)
                appendAll(headers)
            }

            url {
                queryParams.forEach { (key, value) ->
                    parameters.append(key, value)
                }
            }
        }

        return processResponse(response)
    }

    override suspend fun post(
        path: String,
        body: Any,
        headers: Map<String, String>
    ): ApiResponse {
        logger.debug { "Sending POST request to: $path" }

        val response = httpClient.post("$baseUrl/$path") {
            headers {
                appendAll(defaultHeaders)
                appendAll(headers)
            }

            contentType(ContentType.Application.Json)
            setBody(body)
        }

        return processResponse(response)
    }

    override suspend fun put(
        path: String,
        body: Any,
        headers: Map<String, String>
    ): ApiResponse {
        logger.debug { "Sending PUT request to: $path" }

        val response = httpClient.put("$baseUrl/$path") {
            headers {
                appendAll(defaultHeaders)
                appendAll(headers)
            }

            contentType(ContentType.Application.Json)
            setBody(body)
        }

        return processResponse(response)
    }

    override suspend fun delete(
        path: String,
        headers: Map<String, String>
    ): ApiResponse {
        logger.debug { "Sending DELETE request to: $path" }

        val response = httpClient.delete("$baseUrl/$path") {
            headers {
                appendAll(defaultHeaders)
                appendAll(headers)
            }
        }

        return processResponse(response)
    }

    /**
     * 处理 HTTP 响应。
     *
     * @param response HTTP 响应。
     * @return API 响应。
     */
    private suspend fun processResponse(response: HttpResponse): ApiResponse {
        val content = response.bodyAsText()
        val json = try {
            Json.parseToJsonElement(content)
        } catch (e: Exception) {
            null
        }

        return ApiResponse(
            statusCode = response.status.value,
            headers = response.headers.toMap(),
            content = content,
            json = json
        )
    }

    /**
     * 将 Map<String, String> 转换为 Headers.Builder。
     */
    private fun HeadersBuilder.appendAll(headers: Map<String, String>) {
        headers.forEach { (key, value) ->
            append(key, value)
        }
    }
}
