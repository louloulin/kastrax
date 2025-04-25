package ai.kastrax.mcp.protocol

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ResourceTest {
    @Test
    fun `test Resource serialization`() {
        val resource = Resource(
            id = "test",
            name = "Test Resource",
            description = "A test resource",
            type = ResourceType.TEXT,
            metadata = mapOf("key" to "value")
        )
        
        assertEquals("test", resource.id)
        assertEquals("Test Resource", resource.name)
        assertEquals("A test resource", resource.description)
        assertEquals(ResourceType.TEXT, resource.type)
        assertEquals(mapOf("key" to "value"), resource.metadata)
    }
    
    @Test
    fun `test StaticResourceContentProvider`() = runBlocking {
        val provider = StaticResourceContentProvider(
            mapOf("test" to "Test content")
        )
        
        val content = provider.getResourceContent("test")
        assertEquals("Test content", content)
        
        assertThrows<ResourceNotFoundException> {
            runBlocking {
                provider.getResourceContent("nonexistent")
            }
        }
    }
    
    @Test
    fun `test DynamicResourceContentProvider`() = runBlocking {
        val provider = DynamicResourceContentProvider { resourceId ->
            if (resourceId == "test") {
                "Dynamic content for $resourceId"
            } else {
                throw ResourceNotFoundException(resourceId)
            }
        }
        
        val content = provider.getResourceContent("test")
        assertEquals("Dynamic content for test", content)
        
        assertThrows<ResourceNotFoundException> {
            runBlocking {
                provider.getResourceContent("nonexistent")
            }
        }
    }
}
