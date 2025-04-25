package ai.kastrax.mcp.transport

import ai.kastrax.mcp.protocol.MCPMessage
import ai.kastrax.mcp.protocol.MCPRequest
import ai.kastrax.mcp.protocol.MCPResponse
import ai.kastrax.mcp.protocol.MCPNotification
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于 SSE 的传输层
 * 
 * @param url SSE 服务器 URL
 * @param headers 请求头
 */
class SSETransport(
    private val url: String,
    private val headers: Map<String, String> = emptyMap()
) : Transport {
    private val json = Json { ignoreUnknownKeys = true }
    private val isConnected = AtomicBoolean(false)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(SSE)
    }
    private var sseSession: SSESession? = null
    
    override suspend fun connect() {
        if (isConnected.get()) {
            return
        }
        
        withContext(Dispatchers.IO) {
            sseSession = client.sse(url) {
                headers {
                    this@SSETransport.headers.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            
            isConnected.set(true)
        }
    }
    
    override suspend fun disconnect() {
        if (!isConnected.get()) {
            return
        }
        
        withContext(Dispatchers.IO) {
            sseSession?.cancel()
            sseSession = null
            
            isConnected.set(false)
        }
    }
    
    override suspend fun send(message: MCPMessage) {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }
        
        withContext(Dispatchers.IO) {
            val messageJson = when (message) {
                is MCPRequest -> json.encodeToString(MCPRequest.serializer(), message)
                is MCPResponse -> json.encodeToString(MCPResponse.serializer(), message)
                is MCPNotification -> json.encodeToString(MCPNotification.serializer(), message)
                else -> throw IllegalArgumentException("Unknown message type: ${message::class.java.name}")
            }
            
            client.post(url) {
                headers {
                    this@SSETransport.headers.forEach { (key, value) ->
                        append(key, value)
                    }
                    contentType(ContentType.Application.Json)
                }
                setBody(messageJson)
            }
        }
    }
    
    override fun receive(): Flow<MCPMessage> = flow {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }
        
        sseSession?.incoming?.collect { event ->
            val data = event.data
            
            if (data.isBlank()) {
                return@collect
            }
            
            try {
                // 尝试解析为请求
                val request = json.decodeFromString<MCPRequest>(data)
                emit(request)
                return@collect
            } catch (e: Exception) {
                // 不是请求，继续尝试其他类型
            }
            
            try {
                // 尝试解析为响应
                val response = json.decodeFromString<MCPResponse>(data)
                emit(response)
                return@collect
            } catch (e: Exception) {
                // 不是响应，继续尝试其他类型
            }
            
            try {
                // 尝试解析为通知
                val notification = json.decodeFromString<MCPNotification>(data)
                emit(notification)
                return@collect
            } catch (e: Exception) {
                // 不是通知，忽略
            }
        }
    }.flowOn(Dispatchers.IO)
    
    override fun isConnected(): Boolean {
        return isConnected.get()
    }
}
