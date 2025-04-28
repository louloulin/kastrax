package ai.kastrax.a2x.client

import ai.kastrax.a2x.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/**
 * A2X 客户端配置
 */
data class A2XClientConfig(
    /**
     * 服务器 URL
     */
    val serverUrl: String,
    
    /**
     * API 密钥
     */
    val apiKey: String? = null,
    
    /**
     * 客户端 ID
     */
    val clientId: String = "client-${UUID.randomUUID()}",
    
    /**
     * 客户端类型
     */
    val clientType: EntityType = EntityType.AGENT,
    
    /**
     * 客户端名称
     */
    val clientName: String = "A2X Client",
    
    /**
     * 连接超时（毫秒）
     */
    val connectTimeoutMillis: Long = 10000,
    
    /**
     * 请求超时（毫秒）
     */
    val requestTimeoutMillis: Long = 30000
)

/**
 * A2X 客户端
 */
class A2XClient(
    private val config: A2XClientConfig
) : AutoCloseable {
    /**
     * HTTP 客户端
     */
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        engine {
            requestTimeout = config.requestTimeoutMillis
        }
    }
    
    /**
     * 客户端实体引用
     */
    private val clientReference = EntityReference(
        id = config.clientId,
        type = config.clientType,
        endpoint = null
    )
    
    /**
     * 获取实体卡片
     */
    suspend fun getEntityCard(entityId: String? = null): EntityCard {
        val url = if (entityId != null) {
            "${config.serverUrl}/a2x/v1/entities/$entityId"
        } else {
            "${config.serverUrl}/a2x/v1/entities"
        }
        
        return httpClient.get(url) {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header("X-API-Key", apiKey)
            }
        }.body()
    }
    
    /**
     * 获取实体能力
     */
    suspend fun getCapabilities(entityId: String): List<Capability> {
        val url = "${config.serverUrl}/a2x/v1/entities/$entityId/capabilities"
        
        return httpClient.get(url) {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header("X-API-Key", apiKey)
            }
        }.body()
    }
    
    /**
     * 调用实体能力
     */
    suspend fun invoke(
        entityId: String,
        capabilityId: String,
        parameters: Map<String, JsonElement>,
        metadata: Map<String, String> = emptyMap()
    ): JsonElement {
        val url = "${config.serverUrl}/a2x/v1/entities/$entityId/invoke"
        
        val request = InvokeRequest(
            id = UUID.randomUUID().toString(),
            source = clientReference,
            target = EntityReference(
                id = entityId,
                type = EntityType.AGENT,
                endpoint = null
            ),
            capabilityId = capabilityId,
            parameters = parameters,
            metadata = metadata
        )
        
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header("X-API-Key", apiKey)
            }
            setBody(request)
        }.body<InvokeResponse>()
        
        return response.result
    }
    
    /**
     * 查询实体状态
     */
    suspend fun query(
        entityId: String,
        queryType: String,
        parameters: Map<String, JsonElement> = emptyMap(),
        metadata: Map<String, String> = emptyMap()
    ): JsonElement {
        val url = "${config.serverUrl}/a2x/v1/entities/$entityId/query"
        
        val request = QueryRequest(
            id = UUID.randomUUID().toString(),
            source = clientReference,
            target = EntityReference(
                id = entityId,
                type = EntityType.AGENT,
                endpoint = null
            ),
            queryType = queryType,
            parameters = parameters,
            metadata = metadata
        )
        
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header("X-API-Key", apiKey)
            }
            setBody(request)
        }.body<QueryResponse>()
        
        return response.result
    }
    
    /**
     * 发送事件
     */
    suspend fun sendEvent(
        targetId: String,
        eventType: String,
        data: JsonElement,
        metadata: Map<String, String> = emptyMap()
    ) {
        val url = "${config.serverUrl}/a2x/v1/events"
        
        val event = EventMessage(
            id = UUID.randomUUID().toString(),
            source = clientReference,
            target = EntityReference(
                id = targetId,
                type = EntityType.AGENT,
                endpoint = null
            ),
            eventType = eventType,
            data = data,
            metadata = metadata
        )
        
        httpClient.post(url) {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header("X-API-Key", apiKey)
            }
            setBody(event)
        }
    }
    
    /**
     * 发送消息
     */
    suspend fun sendMessage(message: A2XMessage): A2XMessage {
        val url = "${config.serverUrl}/a2x/v1/messages"
        
        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            config.apiKey?.let { apiKey ->
                header("X-API-Key", apiKey)
            }
            setBody(message)
        }.body()
    }
    
    /**
     * 关闭客户端
     */
    override fun close() {
        httpClient.close()
    }
}
