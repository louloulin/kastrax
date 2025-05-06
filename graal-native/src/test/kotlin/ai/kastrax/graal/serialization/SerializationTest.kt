package ai.kastrax.graal.serialization

import ai.kastrax.integrations.deepseek.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 序列化测试类
 * 
 * 用于验证序列化和反序列化功能在 GraalVM 原生镜像中的正确性
 */
class SerializationTest {
    
    private lateinit var json: kotlinx.serialization.json.Json
    
    @BeforeEach
    fun setup() {
        // 初始化序列化模块
        SerializationInitializer.initialize()
        json = SerializationInitializer.json
    }
    
    @Test
    fun `test DeepSeekChatCompletionRequest serialization`() {
        // 创建请求对象
        val request = DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "user",
                    content = "Hello, how are you?"
                )
            ),
            temperature = 0.7,
            maxTokens = 100
        )
        
        // 序列化
        val jsonString = json.encodeToString(DeepSeekChatCompletionRequest.serializer(), request)
        println("Serialized request: $jsonString")
        
        // 反序列化
        val deserializedRequest = json.decodeFromString(DeepSeekChatCompletionRequest.serializer(), jsonString)
        
        // 验证
        assertEquals(request.model, deserializedRequest.model)
        assertEquals(request.messages.size, deserializedRequest.messages.size)
        assertEquals(request.messages[0].role, deserializedRequest.messages[0].role)
        assertEquals(request.messages[0].content, deserializedRequest.messages[0].content)
        assertEquals(request.temperature, deserializedRequest.temperature)
        assertEquals(request.maxTokens, deserializedRequest.maxTokens)
    }
    
    @Test
    fun `test DeepSeekChatCompletionResponse serialization`() {
        // 创建响应对象
        val response = DeepSeekChatCompletionResponse(
            id = "resp-123456",
            model = "deepseek-chat",
            choices = listOf(
                DeepSeekChoice(
                    index = 0,
                    message = DeepSeekMessage(
                        role = "assistant",
                        content = "I'm doing well, thank you for asking!"
                    ),
                    finishReason = "stop"
                )
            ),
            usage = DeepSeekUsage(
                promptTokens = 10,
                completionTokens = 15,
                totalTokens = 25
            )
        )
        
        // 序列化
        val jsonString = json.encodeToString(DeepSeekChatCompletionResponse.serializer(), response)
        println("Serialized response: $jsonString")
        
        // 反序列化
        val deserializedResponse = json.decodeFromString(DeepSeekChatCompletionResponse.serializer(), jsonString)
        
        // 验证
        assertEquals(response.id, deserializedResponse.id)
        assertEquals(response.model, deserializedResponse.model)
        assertEquals(response.choices.size, deserializedResponse.choices.size)
        assertEquals(response.choices[0].index, deserializedResponse.choices[0].index)
        assertEquals(response.choices[0].message.role, deserializedResponse.choices[0].message.role)
        assertEquals(response.choices[0].message.content, deserializedResponse.choices[0].message.content)
        assertEquals(response.choices[0].finishReason, deserializedResponse.choices[0].finishReason)
        assertEquals(response.usage?.promptTokens, deserializedResponse.usage?.promptTokens)
        assertEquals(response.usage?.completionTokens, deserializedResponse.usage?.completionTokens)
        assertEquals(response.usage?.totalTokens, deserializedResponse.usage?.totalTokens)
    }
    
    @Test
    fun `test DeepSeekEmbeddingRequest serialization`() {
        // 创建嵌入请求对象
        val request = DeepSeekEmbeddingRequest(
            model = "deepseek-embedding",
            input = listOf("Hello, world!")
        )
        
        // 序列化
        val jsonString = json.encodeToString(DeepSeekEmbeddingRequest.serializer(), request)
        println("Serialized embedding request: $jsonString")
        
        // 反序列化
        val deserializedRequest = json.decodeFromString(DeepSeekEmbeddingRequest.serializer(), jsonString)
        
        // 验证
        assertEquals(request.model, deserializedRequest.model)
        assertEquals(request.input.size, deserializedRequest.input.size)
        assertEquals(request.input[0], deserializedRequest.input[0])
    }
    
    @Test
    fun `test DeepSeekEmbeddingResponse serialization`() {
        // 创建嵌入响应对象
        val response = DeepSeekEmbeddingResponse(
            data = listOf(
                DeepSeekEmbedding(
                    embedding = listOf(0.1f, 0.2f, 0.3f),
                    index = 0,
                    object_ = "embedding"
                )
            ),
            model = "deepseek-embedding",
            usage = DeepSeekEmbeddingUsage(
                promptTokens = 5,
                totalTokens = 5
            )
        )
        
        // 序列化
        val jsonString = json.encodeToString(DeepSeekEmbeddingResponse.serializer(), response)
        println("Serialized embedding response: $jsonString")
        
        // 反序列化
        val deserializedResponse = json.decodeFromString(DeepSeekEmbeddingResponse.serializer(), jsonString)
        
        // 验证
        assertEquals(response.model, deserializedResponse.model)
        assertEquals(response.data.size, deserializedResponse.data.size)
        assertEquals(response.data[0].index, deserializedResponse.data[0].index)
        assertEquals(response.data[0].object_, deserializedResponse.data[0].object_)
        assertEquals(response.data[0].embedding.size, deserializedResponse.data[0].embedding.size)
        assertEquals(response.usage?.promptTokens, deserializedResponse.usage?.promptTokens)
        assertEquals(response.usage?.totalTokens, deserializedResponse.usage?.totalTokens)
    }
    
    @Test
    fun `test DeepSeekErrorResponse serialization`() {
        // 创建错误响应对象
        val errorResponse = DeepSeekErrorResponse(
            error = DeepSeekError(
                message = "Invalid API key",
                type = "authentication_error",
                param = "api_key",
                code = "invalid_api_key"
            )
        )
        
        // 序列化
        val jsonString = json.encodeToString(DeepSeekErrorResponse.serializer(), errorResponse)
        println("Serialized error response: $jsonString")
        
        // 反序列化
        val deserializedErrorResponse = json.decodeFromString(DeepSeekErrorResponse.serializer(), jsonString)
        
        // 验证
        assertNotNull(deserializedErrorResponse.error)
        assertEquals(errorResponse.error.message, deserializedErrorResponse.error.message)
        assertEquals(errorResponse.error.type, deserializedErrorResponse.error.type)
        assertEquals(errorResponse.error.param, deserializedErrorResponse.error.param)
        assertEquals(errorResponse.error.code, deserializedErrorResponse.error.code)
    }
    
    @Test
    fun `test JSON basic types serialization`() {
        // 测试 JSON 基本类型的序列化
        val jsonObject = JsonObject(
            mapOf(
                "string" to JsonPrimitive("value"),
                "number" to JsonPrimitive(42),
                "boolean" to JsonPrimitive(true),
                "array" to JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))),
                "nested" to JsonObject(mapOf("key" to JsonPrimitive("value")))
            )
        )
        
        // 序列化
        val jsonString = json.encodeToString(JsonObject.serializer(), jsonObject)
        println("Serialized JSON object: $jsonString")
        
        // 反序列化
        val deserializedJsonObject = json.decodeFromString(JsonObject.serializer(), jsonString)
        
        // 验证
        assertEquals(jsonObject.size, deserializedJsonObject.size)
        assertEquals(jsonObject["string"], deserializedJsonObject["string"])
        assertEquals(jsonObject["number"], deserializedJsonObject["number"])
        assertEquals(jsonObject["boolean"], deserializedJsonObject["boolean"])
        assertEquals((jsonObject["array"] as JsonArray).size, (deserializedJsonObject["array"] as JsonArray).size)
        assertEquals((jsonObject["nested"] as JsonObject).size, (deserializedJsonObject["nested"] as JsonObject).size)
    }
}
