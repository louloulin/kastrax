package ai.kastrax.datasource.api

import ai.kastrax.datasource.common.DataSource
import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.ApiConnector
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
 * API 响应对象，包含响应状态码和响应内容。
 */
data class ApiResponse(
    val statusCode: Int,
    val content: String,
    val headers: Map<String, List<String>> = emptyMap()
) {
    val isSuccess: Boolean
        get() = statusCode in 200..299
}

/**
 * API 连接器实现类，提供了通用的实现。
 */
abstract class ApiConnectorImpl(
    name: String
) : DataSourceBase(name, DataSourceType.API), ApiConnector {

    /**
     * API 基础 URL。
     */
    abstract override val baseUrl: String

    /**
     * 默认请求头。
     */
    abstract override val defaultHeaders: Map<String, String>

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
    }

    /**
     * 处理响应。
     */
    protected suspend fun processResponse(response: HttpResponse): ApiResponse {
        val content = response.bodyAsText()
        val headers = response.headers.entries().associate { it.key to it.value }

        return ApiResponse(
            statusCode = response.status.value,
            content = content,
            headers = headers
        )
    }

    /**
     * 发送 GET 请求。
     */
    override suspend fun get(path: String, params: Map<String, String>, headers: Map<String, String>): String {
        logger.debug { "Sending GET request to: $path with params: $params" }

        val response = httpClient.get("$baseUrl/$path") {
            headers {
                defaultHeaders.forEach { (key, value) -> append(key, value) }
                headers.forEach { (key, value) -> append(key, value) }
            }

            url {
                params.forEach { (key, value) -> parameters.append(key, value) }
            }
        }

        return response.bodyAsText()
    }

    /**
     * 发送 POST 请求。
     */
    override suspend fun post(path: String, body: String, headers: Map<String, String>): String {
        logger.debug { "Sending POST request to: $path" }

        val response = httpClient.post("$baseUrl/$path") {
            headers {
                defaultHeaders.forEach { (key, value) -> append(key, value) }
                headers.forEach { (key, value) -> append(key, value) }
            }

            setBody(body)
        }

        return response.bodyAsText()
    }

    /**
     * 发送 PUT 请求。
     */
    override suspend fun put(path: String, body: String, headers: Map<String, String>): String {
        logger.debug { "Sending PUT request to: $path" }

        val response = httpClient.put("$baseUrl/$path") {
            headers {
                defaultHeaders.forEach { (key, value) -> append(key, value) }
                headers.forEach { (key, value) -> append(key, value) }
            }

            setBody(body)
        }

        return response.bodyAsText()
    }

    /**
     * 发送 DELETE 请求。
     */
    override suspend fun delete(path: String, headers: Map<String, String>): String {
        logger.debug { "Sending DELETE request to: $path" }

        val response = httpClient.delete("$baseUrl/$path") {
            headers {
                defaultHeaders.forEach { (key, value) -> append(key, value) }
                headers.forEach { (key, value) -> append(key, value) }
            }
        }

        return response.bodyAsText()
    }

    /**
     * 发送 PATCH 请求。
     */
    override suspend fun patch(path: String, body: String, headers: Map<String, String>): String {
        logger.debug { "Sending PATCH request to: $path" }

        val response = httpClient.patch("$baseUrl/$path") {
            headers {
                defaultHeaders.forEach { (key, value) -> append(key, value) }
                headers.forEach { (key, value) -> append(key, value) }
            }

            setBody(body)
        }

        return response.bodyAsText()
    }

    // The getBaseUrl() and getDefaultHeaders() methods are implemented by the properties
    // baseUrl and defaultHeaders with the override keyword

    /**
     * 连接到 API。
     */
    override suspend fun doConnect(): Boolean {
        return true
    }

    /**
     * 断开与 API 的连接。
     */
    override suspend fun doDisconnect(): Boolean {
        httpClient.close()
        return true
    }
}
