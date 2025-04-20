package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import ai.kastrax.zod.SchemaResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * ZodTool 示例测试。
 */
class ZodToolExampleTest {

    /**
     * 测试简单的字符串反转工具。
     */
    @Test
    fun `test simple string reverse tool`() = runBlocking {
        // 创建一个简单的字符串反转工具
        val reverseStringTool = zodTool<String, String> {
            id = "reverse_string"
            name = "Reverse String"
            description = "Reverses the input string"

            inputSchema = stringInput("The string to reverse")
            outputSchema = stringOutput("The reversed string")

            execute = { input ->
                input.reversed()
            }
        }

        // 测试有效输入
        val input = "Hello, World!"
        val validationResult = reverseStringTool.inputSchema.safeParse(input)
        assertTrue(validationResult is SchemaResult.Success)

        // 测试执行
        val output = reverseStringTool.execute(input)
        assertEquals("!dlroW ,olleH", output)

        // 测试空字符串
        val emptyInput = ""
        val emptyOutput = reverseStringTool.execute(emptyInput)
        assertEquals("", emptyOutput)
    }

    /**
     * 测试计算器工具。
     */
    @Test
    fun `test calculator tool`() = runBlocking {
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
            }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

            outputSchema = objectOutput("Calculator output") {
                numberField("result", "Result of the operation")
            }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

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

        // 测试加法
        val addInput = mapOf(
            "operation" to "add",
            "a" to 5,
            "b" to 3
        )
        val addOutput = calculatorTool.execute(addInput)
        assertEquals(8.0, addOutput["result"])

        // 测试减法
        val subtractInput = mapOf(
            "operation" to "subtract",
            "a" to 10,
            "b" to 4
        )
        val subtractOutput = calculatorTool.execute(subtractInput)
        assertEquals(6.0, subtractOutput["result"])

        // 测试乘法
        val multiplyInput = mapOf(
            "operation" to "multiply",
            "a" to 7,
            "b" to 6
        )
        val multiplyOutput = calculatorTool.execute(multiplyInput)
        assertEquals(42.0, multiplyOutput["result"])

        // 测试除法
        val divideInput = mapOf(
            "operation" to "divide",
            "a" to 20,
            "b" to 4
        )
        val divideOutput = calculatorTool.execute(divideInput)
        assertEquals(5.0, divideOutput["result"])

        // 测试无效输入
        val invalidInput = mapOf(
            "operation" to "power", // 不支持的操作
            "a" to 2,
            "b" to 3
        )
        val invalidResult = calculatorTool.inputSchema.safeParse(invalidInput)
        assertTrue(invalidResult is SchemaResult.Failure)

        // 测试缺少参数
        val incompleteInput = mapOf(
            "operation" to "add",
            "a" to 5
            // 缺少 b
        )
        val incompleteResult = calculatorTool.inputSchema.safeParse(incompleteInput)
        assertTrue(incompleteResult is SchemaResult.Failure)
    }

    /**
     * 测试使用数据类的工具。
     */
    @Test
    fun `test tool with data class`() = runBlocking {
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
            inputSchema = calculatorInputSchema.unsafeCast<CalculatorInput, CalculatorInput>()
            outputSchema = calculatorOutputSchema.unsafeCast<CalculatorOutput, CalculatorOutput>()

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

        // 测试乘法
        val input = CalculatorInput(
            operation = "multiply",
            a = 4.0,
            b = 7.0
        )
        val output = calculatorTool.execute(input)
        assertEquals(28.0, output.result)
    }

    /**
     * 测试用户验证工具。
     */
    @Test
    fun `test user validation tool`() = runBlocking {
        // 定义数据类
        data class User(
            val id: String,
            val name: String,
            val email: String,
            val age: Int
        )

        data class UserValidationResult(
            val isValid: Boolean,
            val errors: List<String>
        )

        // 创建用户验证工具
        val userValidatorTool = zodTool<User, UserValidationResult> {
            id = "user_validator"
            name = "User Validator"
            description = "Validates user data"

            // 创建输入模式
            val userSchema = objectInput("User data") {
                stringField("id", "User ID") {
                    minLength = 3
                    maxLength = 50
                    pattern = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$")
                }
                stringField("name", "User name") {
                    minLength = 2
                    maxLength = 100
                }
                stringField("email", "User email") {
                    email = true
                }
                numberField("age", "User age") {
                    min = 0.0
                    max = 120.0
                }
            }.transform { input ->
                User(
                    id = input["id"] as String,
                    name = input["name"] as String,
                    email = input["email"] as String,
                    age = (input["age"] as Number).toInt()
                )
            }.unsafeCast<Map<String, Any?>, User>()

            // 创建输出模式
            val resultSchema = objectOutput("Validation result") {
                booleanField("isValid", "Whether the user data is valid")
                arrayField("errors", stringInput("Error message"), "List of validation errors")
            }.transform { output ->
                UserValidationResult(
                    isValid = output["isValid"] as Boolean,
                    errors = output["errors"] as List<String>
                )
            }.unsafeCast<Map<String, Any?>, UserValidationResult>()

            inputSchema = userSchema.unsafeCast<User, User>()
            outputSchema = resultSchema.unsafeCast<UserValidationResult, UserValidationResult>()

            execute = { user ->
                val errors = mutableListOf<String>()

                // 验证 ID
                if (!user.id.matches(Regex("^[a-zA-Z0-9_-]+\$"))) {
                    errors.add("ID can only contain letters, numbers, underscores, and hyphens")
                }

                // 验证名称
                if (user.name.isBlank()) {
                    errors.add("Name cannot be blank")
                }

                // 验证邮箱
                if (!user.email.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"))) {
                    errors.add("Invalid email format")
                }

                // 验证年龄
                if (user.age < 18) {
                    errors.add("User must be at least 18 years old")
                }

                UserValidationResult(
                    isValid = errors.isEmpty(),
                    errors = errors
                )
            }
        }

        // 测试有效用户
        val validUser = User(
            id = "user123",
            name = "John Doe",
            email = "john.doe@example.com",
            age = 30
        )
        val validResult = userValidatorTool.execute(validUser)
        assertTrue(validResult.isValid)
        assertTrue(validResult.errors.isEmpty())

        // 测试无效用户
        val invalidUser = User(
            id = "user@123", // 包含无效字符
            name = "",       // 名称为空
            email = "invalid-email", // 无效邮箱
            age = 15         // 年龄小于 18
        )
        val invalidResult = userValidatorTool.execute(invalidUser)
        assertFalse(invalidResult.isValid)
        assertEquals(4, invalidResult.errors.size)
        assertTrue(invalidResult.errors.any { it.contains("ID") })
        assertTrue(invalidResult.errors.any { it.contains("Name") })
        assertTrue(invalidResult.errors.any { it.contains("email") })
        assertTrue(invalidResult.errors.any { it.contains("age") })
    }
}
