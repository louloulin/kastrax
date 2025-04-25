package ai.kastrax.mcp.protocol

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ToolTest {
    @Test
    fun `test Tool serialization`() {
        val tool = Tool(
            id = "test",
            name = "Test Tool",
            description = "A test tool",
            parameters = ToolParameters(
                type = "object",
                required = listOf("param1"),
                properties = mapOf(
                    "param1" to ToolParameterProperty(
                        type = "string",
                        description = "Parameter 1"
                    )
                )
            ),
            metadata = mapOf("key" to "value")
        )

        assertEquals("test", tool.id)
        assertEquals("Test Tool", tool.name)
        assertEquals("A test tool", tool.description)
        assertEquals("object", tool.parameters.type)
        assertEquals(listOf("param1"), tool.parameters.required)
        assertEquals(1, tool.parameters.properties.size)
        assertEquals("string", tool.parameters.properties["param1"]?.type)
        assertEquals("Parameter 1", tool.parameters.properties["param1"]?.description)
        assertEquals(mapOf("key" to "value"), tool.metadata)
    }

    @Test
    fun `test FunctionToolHandler`() = runBlocking {
        val handler = FunctionToolHandler(
            mapOf<String, suspend (Map<String, Any>) -> String>(
                "test" to { params ->
                    "Result: ${params["param1"]}"
                }
            )
        )

        val result = handler.handleToolCall("test", mapOf("param1" to "value"))
        assertEquals("Result: value", result)

        assertThrows<ToolNotFoundException> {
            runBlocking {
                handler.handleToolCall("nonexistent", emptyMap())
            }
        }

        val errorHandler = FunctionToolHandler(
            mapOf<String, suspend (Map<String, Any>) -> String>(
                "error" to { _ ->
                    throw RuntimeException("Test error")
                }
            )
        )

        assertThrows<ToolExecutionException> {
            runBlocking {
                errorHandler.handleToolCall("error", emptyMap())
            }
        }
    }
}
