package ai.kastrax.mcp.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MessageTest {
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `test MCPRequest serialization`() {
        val request = MCPRequest(
            id = "123",
            method = "test",
            params = JsonPrimitive("test")
        )
        
        val serialized = json.encodeToString(MCPRequest.serializer(), request)
        val deserialized = json.decodeFromString<MCPRequest>(serialized)
        
        assertEquals(request.id, deserialized.id)
        assertEquals(request.method, deserialized.method)
        assertEquals(request.params, deserialized.params)
        assertEquals("2.0", deserialized.jsonrpc)
    }
    
    @Test
    fun `test MCPResponse serialization`() {
        val response = MCPResponse(
            id = "123",
            result = JsonPrimitive("test")
        )
        
        val serialized = json.encodeToString(MCPResponse.serializer(), response)
        val deserialized = json.decodeFromString<MCPResponse>(serialized)
        
        assertEquals(response.id, deserialized.id)
        assertEquals(response.result, deserialized.result)
        assertNull(deserialized.error)
        assertEquals("2.0", deserialized.jsonrpc)
    }
    
    @Test
    fun `test MCPResponse with error serialization`() {
        val response = MCPResponse(
            id = "123",
            error = MCPError(
                code = 123,
                message = "test"
            )
        )
        
        val serialized = json.encodeToString(MCPResponse.serializer(), response)
        val deserialized = json.decodeFromString<MCPResponse>(serialized)
        
        assertEquals(response.id, deserialized.id)
        assertNull(deserialized.result)
        assertNotNull(deserialized.error)
        assertEquals(response.error?.code, deserialized.error?.code)
        assertEquals(response.error?.message, deserialized.error?.message)
        assertEquals("2.0", deserialized.jsonrpc)
    }
    
    @Test
    fun `test MCPNotification serialization`() {
        val notification = MCPNotification(
            method = "test",
            params = JsonPrimitive("test")
        )
        
        val serialized = json.encodeToString(MCPNotification.serializer(), notification)
        val deserialized = json.decodeFromString<MCPNotification>(serialized)
        
        assertNull(deserialized.id)
        assertEquals(notification.method, deserialized.method)
        assertEquals(notification.params, deserialized.params)
        assertEquals("2.0", deserialized.jsonrpc)
    }
}
