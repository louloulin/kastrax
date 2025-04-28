package ai.kastrax.a2a.client

import ai.kastrax.a2a.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * A2A 客户端配置
 */
data class A2AClientConfig(
    /**
     * 服务器 URL
     */
    val serverUrl: String,
    
    /**
     * API 密钥
     */
    val apiKey: String? = null,
    
    /**
     * 超时时间（毫秒）
     */
    val timeout: Long = 30000,
    
    /**
     * 重试次数
     */
    val retries: Int = 3
)

/**
 * A2A 客户端，用于与 A2A 代理通信
 */
class A2AClient(private val config: A2AClientConfig) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        engine {
            requestTimeout = config.timeout
        }
    }
    
    /**
     * 获取代理卡片
     */
    suspend fun getAgentCard(agentId: String? = null): AgentCard {
        val url = if (agentId != null) {
            "${config.serverUrl}/a2a/v1/agents/$agentId"
        } else {
            "${config.serverUrl}/.well-known/agent.json"
        }
        
        return httpClient.get(url) {
            config.apiKey?.let { apiKey ->
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }.body()
    }
    
    /**
     * 获取代理能力
     */
    suspend fun getCapabilities(agentId: String): List<Capability> {
        return httpClient.get("${config.serverUrl}/a2a/v1/agents/$agentId/capabilities") {
            config.apiKey?.let { apiKey ->
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }.body()
    }
    
    /**
     * 调用代理能力
     */
    suspend fun invoke(
        agentId: String,
        capabilityId: String,
        parameters: Map<String, JsonElement>,
        metadata: Map<String, String> = emptyMap()
    ): InvokeResponse {
        val request = InvokeRequest(
            id = UUID.randomUUID().toString(),
            capabilityId = capabilityId,
            parameters = parameters,
            metadata = metadata
        )
        
        return httpClient.post("${config.serverUrl}/a2a/v1/agents/$agentId/invoke") {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(request)
        }.body()
    }
    
    /**
     * 查询代理状态
     */
    suspend fun queryStatus(agentId: String): QueryResponse {
        val request = QueryRequest(
            id = UUID.randomUUID().toString(),
            queryType = "status"
        )
        
        return httpClient.post("${config.serverUrl}/a2a/v1/agents/$agentId/message") {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(request)
        }.body()
    }
    
    /**
     * 查询代理健康状态
     */
    suspend fun queryHealth(agentId: String): QueryResponse {
        val request = QueryRequest(
            id = UUID.randomUUID().toString(),
            queryType = "health"
        )
        
        return httpClient.post("${config.serverUrl}/a2a/v1/agents/$agentId/message") {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(request)
        }.body()
    }
    
    /**
     * 发送通用 A2A 消息
     */
    suspend fun sendMessage(agentId: String, message: A2AMessage): A2AMessage {
        return httpClient.post("${config.serverUrl}/a2a/v1/agents/$agentId/message") {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(message)
        }.body()
    }
    
    /**
     * 关闭客户端
     */
    fun close() {
        httpClient.close()
    }
}
