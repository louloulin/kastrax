package ai.kastrax.mcp.protocol

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PromptTest {
    @Test
    fun `test Prompt serialization`() {
        val prompt = Prompt(
            id = "test",
            name = "Test Prompt",
            description = "A test prompt",
            parameters = PromptParameters(
                type = "object",
                required = listOf("param1"),
                properties = mapOf(
                    "param1" to PromptParameterProperty(
                        type = "string",
                        description = "Parameter 1"
                    )
                )
            ),
            metadata = mapOf("key" to "value")
        )
        
        assertEquals("test", prompt.id)
        assertEquals("Test Prompt", prompt.name)
        assertEquals("A test prompt", prompt.description)
        assertEquals("object", prompt.parameters?.type)
        assertEquals(listOf("param1"), prompt.parameters?.required)
        assertEquals(1, prompt.parameters?.properties?.size)
        assertEquals("string", prompt.parameters?.properties?.get("param1")?.type)
        assertEquals("Parameter 1", prompt.parameters?.properties?.get("param1")?.description)
        assertEquals(mapOf("key" to "value"), prompt.metadata)
    }
    
    @Test
    fun `test StaticPromptContentProvider`() = runBlocking {
        val provider = StaticPromptContentProvider(
            mapOf("test" to "Hello, {{name}}!")
        )
        
        val content = provider.getPromptContent("test")
        assertEquals("Hello, {{name}}!", content)
        
        val rendered = provider.renderPrompt("test", mapOf("name" to "World"))
        assertEquals("Hello, World!", rendered)
        
        assertThrows<PromptNotFoundException> {
            runBlocking {
                provider.getPromptContent("nonexistent")
            }
        }
    }
    
    @Test
    fun `test DynamicPromptContentProvider`() = runBlocking {
        val provider = DynamicPromptContentProvider(
            contentProvider = { promptId ->
                if (promptId == "test") {
                    "Hello, {{name}}!"
                } else {
                    throw PromptNotFoundException(promptId)
                }
            },
            renderer = { template, parameters ->
                var result = template
                for ((key, value) in parameters) {
                    result = result.replace("{{$key}}", value)
                }
                result
            }
        )
        
        val content = provider.getPromptContent("test")
        assertEquals("Hello, {{name}}!", content)
        
        val rendered = provider.renderPrompt("test", mapOf("name" to "World"))
        assertEquals("Hello, World!", rendered)
        
        assertThrows<PromptNotFoundException> {
            runBlocking {
                provider.getPromptContent("nonexistent")
            }
        }
        
        val errorProvider = DynamicPromptContentProvider(
            contentProvider = { "{{invalid" },
            renderer = { _, _ -> throw RuntimeException("Test error") }
        )
        
        assertThrows<PromptRenderException> {
            runBlocking {
                errorProvider.renderPrompt("test", emptyMap())
            }
        }
    }
}
