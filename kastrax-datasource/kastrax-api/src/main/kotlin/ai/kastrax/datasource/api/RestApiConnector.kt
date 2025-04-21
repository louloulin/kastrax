package ai.kastrax.datasource.api

import mu.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*

private val logger = KotlinLogging.logger {}

/**
 * RESTful API 连接器，提供了与 RESTful API 交互的功能。
 */
class RestApiConnector(
    name: String,
    override val baseUrl: String,
    override val defaultHeaders: Map<String, String> = emptyMap(),
    private val authType: AuthType = AuthType.NONE,
    private val authToken: String = "",
    private val username: String = "",
    private val password: String = ""
) : ApiConnectorImpl(name) {

    private var isConnected = false

    override suspend fun doConnect(): Boolean {
        return try {
            // 对于 RESTful API，连接通常是无状态的，但我们可以验证 API 是否可访问
            val response = httpClient.get("$baseUrl/") {
                headers {
                    defaultHeaders.forEach { (key, value) -> append(key, value) }
                    appendAuth()
                }
            }

            isConnected = response.status.value in 200..299
            isConnected
        } catch (e: Exception) {
            logger.error(e) { "Error connecting to RESTful API: $baseUrl" }
            false
        }
    }

    override suspend fun doDisconnect(): Boolean {
        // 对于 RESTful API，断开连接通常是无状态的
        isConnected = false
        return true
    }

    /**
     * 向请求头添加认证信息。
     */
    private fun io.ktor.client.request.HttpRequestBuilder.appendAuth() {
        when (authType) {
            AuthType.BEARER -> {
                headers.append("Authorization", "Bearer $authToken")
            }
            AuthType.BASIC -> {
                val credentials = "$username:$password"
                val encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
                headers.append("Authorization", "Basic $encodedCredentials")
            }
            AuthType.API_KEY -> {
                headers.append("X-API-Key", authToken)
            }
            AuthType.NONE -> {
                // 不添加认证信息
            }
        }
    }

    /**
     * 认证类型枚举。
     */
    enum class AuthType {
        NONE,
        BEARER,
        BASIC,
        API_KEY
    }
}
