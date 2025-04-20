package ai.kastrax.zod.test

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Simple ZodTool test.
 */
class SimpleZodToolTest {

    /**
     * Test simple string reverse tool.
     */
    @Test
    fun testSimpleStringReverseTool() = runBlocking {
        // Create a simple string reverse tool
        val reverseStringTool = zodTool<String, String> {
            id = "reverse_string"
            name = "Reverse String"
            description = "Reverses the input string"

            @Suppress("UNCHECKED_CAST")
            inputSchema = StringSchema().nullable() as Schema<String, String>
            outputSchema = StringSchema()

            execute = { input ->
                input.reversed()
            }
        }

        // Test valid input
        val input = "Hello, World!"
        val output = kotlinx.coroutines.runBlocking { reverseStringTool.execute(input) }
        assertEquals("!dlroW ,olleH", output)

        // Test empty string
        val emptyInput = ""
        val emptyOutput = kotlinx.coroutines.runBlocking { reverseStringTool.execute(emptyInput) }
        assertEquals("", emptyOutput)
    }
}
