package ai.kastrax.core.tools

import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZodToolTest {

    @Test
    fun `ZodTool should execute with Map input and output`() = runBlocking {
        // 创建计算器工具
        val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
            id = "calculator"
            name = "Calculator"
            description = "Performs basic arithmetic operations"

            inputSchema = objectInput("Calculator input") {
                stringField("operation", "Operation to perform") {
                    enum("add", "subtract", "multiply", "divide")
                }
                numberField("a", "First operand")
                numberField("b", "Second operand")
            }

            outputSchema = objectOutput("Calculator output") {
                numberField("result", "Result of the operation")
            }

            execute = { input ->
                val operation = input["operation"] as String
                val a = (input["a"] as Number).toDouble()
                val b = (input["b"] as Number).toDouble()

                val result = when (operation) {
                    "add" -> a + b
                    "subtract" -> a - b
                    "multiply" -> a * b
                    "divide" -> a / b
                    else -> throw IllegalArgumentException("Unknown operation: $operation")
                }

                mapOf("result" to result)
            }
        }

        // 执行工具
        val input = mapOf(
            "operation" to "add",
            "a" to 5,
            "b" to 3
        )

        val output = calculatorTool.execute(input)

        assertEquals(8.0, output["result"])
    }

    @Test
    fun `ZodTool should execute with data class input and output`() = runBlocking {
        // 定义数据类
        data class CalculatorInput(
            val operation: String,
            val a: Double,
            val b: Double
        )

        data class CalculatorOutput(
            val result: Double
        )

        // 创建输入模式
        val calculatorInputSchema = objectInput("Calculator input") {
            stringField("operation", "Operation to perform") {
                enum("add", "subtract", "multiply", "divide")
            }
            numberField("a", "First operand")
            numberField("b", "Second operand")
        }.transform { input ->
            CalculatorInput(
                operation = input["operation"] as String,
                a = (input["a"] as Number).toDouble(),
                b = (input["b"] as Number).toDouble()
            )
        }

        // 创建输出模式
        val calculatorOutputSchema = objectOutput("Calculator output") {
            numberField("result", "Result of the operation")
        }.transform { output ->
            CalculatorOutput(
                result = (output["result"] as Number).toDouble()
            )
        }

        // 创建计算器工具
        val calculatorTool = zodTool<CalculatorInput, CalculatorOutput> {
            id = "calculator_data_class"
            name = "Calculator (Data Class)"
            description = "Performs basic arithmetic operations using data classes"
            inputSchema = calculatorInputSchema as Schema<CalculatorInput, CalculatorInput>
            outputSchema = calculatorOutputSchema as Schema<CalculatorOutput, CalculatorOutput>

            execute = { input ->
                val result = when (input.operation) {
                    "add" -> input.a + input.b
                    "subtract" -> input.a - input.b
                    "multiply" -> input.a * input.b
                    "divide" -> input.a / input.b
                    else -> throw IllegalArgumentException("Unknown operation: ${input.operation}")
                }

                CalculatorOutput(result)
            }
        }

        // 执行工具
        val input = CalculatorInput(
            operation = "multiply",
            a = 4.0,
            b = 7.0
        )

        val output = calculatorTool.execute(input)

        assertEquals(28.0, output.result)
    }

    @Test
    fun `ZodTool should convert to Tool`() = runBlocking {
        // 创建 ZodTool
        val zodTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
            id = "calculator"
            name = "Calculator"
            description = "Performs basic arithmetic operations"

            inputSchema = objectInput("Calculator input") {
                stringField("operation", "Operation to perform") {
                    enum("add", "subtract", "multiply", "divide")
                }
                numberField("a", "First operand")
                numberField("b", "Second operand")
            }

            outputSchema = objectOutput("Calculator output") {
                numberField("result", "Result of the operation")
            }

            execute = { input ->
                val operation = input["operation"] as String
                val a = (input["a"] as Number).toDouble()
                val b = (input["b"] as Number).toDouble()

                val result = when (operation) {
                    "add" -> a + b
                    "subtract" -> a - b
                    "multiply" -> a * b
                    "divide" -> a / b
                    else -> throw IllegalArgumentException("Unknown operation: $operation")
                }

                mapOf("result" to result)
            }
        }

        // 转换为 Tool
        val tool = zodTool.toTool()

        // 验证 Tool 属性
        assertEquals("calculator", tool.id)
        assertEquals("Calculator", tool.name)
        assertEquals("Performs basic arithmetic operations", tool.description)

        // 验证 inputSchema
        assertTrue(tool.inputSchema is JsonObject)
        assertEquals("object", tool.inputSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // 验证 outputSchema
        assertTrue(tool.outputSchema is JsonObject)
        assertEquals("object", tool.outputSchema?.jsonObject?.get("type")?.jsonPrimitive?.content)

        // 执行 Tool
        val input = buildJsonObject {
            put("operation", "add")
            put("a", 5)
            put("b", 3)
        }

        val output = tool.execute(input)

        assertTrue(output is JsonObject)
        assertEquals(8.0, output.jsonObject["result"]?.jsonPrimitive?.double)
    }

    @Test
    fun `Tool should convert to ZodTool`() = runBlocking {
        // 创建 Tool
        val tool = tool {
            id = "calculator"
            name = "Calculator"
            description = "Performs basic arithmetic operations"

            inputSchema = buildJsonObject {
                put("type", "object")
                put("required", buildJsonArray {
                    add("operation")
                    add("a")
                    add("b")
                })
                put("properties", buildJsonObject {
                    put("operation", buildJsonObject {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add("add")
                            add("subtract")
                            add("multiply")
                            add("divide")
                        })
                    })
                    put("a", buildJsonObject {
                        put("type", "number")
                    })
                    put("b", buildJsonObject {
                        put("type", "number")
                    })
                })
            }

            outputSchema = buildJsonObject {
                put("type", "object")
                put("required", buildJsonArray {
                    add("result")
                })
                put("properties", buildJsonObject {
                    put("result", buildJsonObject {
                        put("type", "number")
                    })
                })
            }

            execute = { input ->
                val operation = input.jsonObject["operation"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing operation")
                val a = input.jsonObject["a"]?.jsonPrimitive?.double
                    ?: throw IllegalArgumentException("Missing or invalid a")
                val b = input.jsonObject["b"]?.jsonPrimitive?.double
                    ?: throw IllegalArgumentException("Missing or invalid b")

                val result = when (operation) {
                    "add" -> a + b
                    "subtract" -> a - b
                    "multiply" -> a * b
                    "divide" -> a / b
                    else -> throw IllegalArgumentException("Unknown operation: $operation")
                }

                buildJsonObject {
                    put("result", result)
                }
            }
        }

        // 创建输入和输出模式
        val inputSchema = objectInput("Calculator input") {
            stringField("operation", "Operation to perform") {
                enum("add", "subtract", "multiply", "divide")
            }
            numberField("a", "First operand")
            numberField("b", "Second operand")
        }

        val outputSchema = objectOutput("Calculator output") {
            numberField("result", "Result of the operation")
        }

        // 转换为 ZodTool
        val zodTool = tool.toZodTool(inputSchema, outputSchema)

        // 验证 ZodTool 属性
        assertEquals("calculator", zodTool.id)
        assertEquals("Calculator", zodTool.name)
        assertEquals("Performs basic arithmetic operations", zodTool.description)

        // 执行 ZodTool
        val input = mapOf(
            "operation" to "add",
            "a" to 5,
            "b" to 3
        )

        val output = zodTool.execute(input)

        assertEquals(8.0, output["result"])
    }
}
