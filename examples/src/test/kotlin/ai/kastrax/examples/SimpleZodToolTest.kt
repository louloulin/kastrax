package ai.kastrax.examples

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
     * Unsafe cast extension function for Schema
     * This is used to work around type inference issues
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T, reified R> Schema<*, *>.unsafeCast(): Schema<T, R> = this as Schema<T, R>

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

            inputSchema = stringInput("Input string").unsafeCast<String, String>()
            outputSchema = stringOutput("Output string").unsafeCast<String, String>()

            execute = { input ->
                input.reversed()
            }
        }

        // Test valid input
        val input = "Hello, World!"
        val output = reverseStringTool.execute(input)
        assertEquals("!dlroW ,olleH", output)

        // Test empty string
        val emptyInput = ""
        val emptyOutput = reverseStringTool.execute(emptyInput)
        assertEquals("", emptyOutput)
    }
}
