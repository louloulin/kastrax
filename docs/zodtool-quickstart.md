# ZodTool 快速入门指南

本指南将帮助您快速上手 ZodTool，创建类型安全的工具。

## 目录

1. [安装](#安装)
2. [创建第一个 ZodTool](#创建第一个-zodtool)
3. [使用数据类](#使用数据类)
4. [添加验证规则](#添加验证规则)
5. [处理错误](#处理错误)
6. [与现有工具集成](#与现有工具集成)
7. [下一步](#下一步)

## 安装

要使用 ZodTool，您需要在项目中添加 kastrax-core 和 kastrax-zod 依赖：

```kotlin
// build.gradle.kts
dependencies {
    implementation("ai.kastrax:kastrax-core:1.0.0")
    // kastrax-zod 已经作为 kastrax-core 的传递依赖被引入
}
```

## 创建第一个 ZodTool

让我们创建一个简单的字符串反转工具：

```kotlin
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.stringInput
import ai.kastrax.zod.stringOutput
import kotlinx.coroutines.runBlocking

fun main() {
    // 创建一个简单的字符串反转工具
    val reverseStringTool = zodTool<String, String> {
        id = "reverse_string"
        name = "Reverse String"
        description = "Reverses the input string"
        
        // 使用 stringInput 和 stringOutput 辅助函数创建模式
        inputSchema = stringInput("The string to reverse")
        outputSchema = stringOutput("The reversed string")
        
        // 实现执行逻辑
        execute = { input ->
            input.reversed()
        }
    }
    
    // 使用工具
    val input = "Hello, World!"
    
    // 执行工具
    val output = runBlocking {
        reverseStringTool.execute(input)
    }
    
    println("Original: $input")
    println("Reversed: $output")
}
```

这个简单的例子展示了 ZodTool 的基本用法：

1. 使用 `zodTool` 函数创建工具
2. 指定输入和输出类型（这里是 `String` 和 `String`）
3. 设置工具的基本属性（id、name、description）
4. 定义输入和输出模式
5. 实现执行逻辑
6. 使用 `execute` 方法执行工具

## 使用数据类

ZodTool 可以与数据类一起使用，提供更好的类型安全性：

```kotlin
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

// 定义数据类
data class CalculatorInput(
    val operation: String,
    val a: Double,
    val b: Double
)

data class CalculatorOutput(
    val result: Double
)

fun main() {
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
        id = "calculator"
        name = "Calculator"
        description = "Performs basic arithmetic operations"
        inputSchema = calculatorInputSchema
        outputSchema = calculatorOutputSchema
        
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
    
    // 使用工具
    val input = CalculatorInput(
        operation = "add",
        a = 5.0,
        b = 3.0
    )
    
    // 执行工具
    val output = runBlocking {
        calculatorTool.execute(input)
    }
    
    println("Operation: ${input.operation}")
    println("a: ${input.a}")
    println("b: ${input.b}")
    println("Result: ${output.result}")
}
```

这个例子展示了如何使用数据类与 ZodTool：

1. 定义输入和输出数据类
2. 创建输入和输出模式，并使用 `transform` 方法将 JSON 对象转换为数据类
3. 创建工具，指定数据类作为输入和输出类型
4. 实现执行逻辑，直接使用数据类
5. 使用数据类作为输入和输出

## 添加验证规则

ZodTool 提供了强大的验证功能，可以添加各种验证规则：

```kotlin
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

fun main() {
    // 创建用户验证工具
    val userValidatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "user_validator"
        name = "User Validator"
        description = "Validates user data"
        
        // 创建输入模式，添加验证规则
        inputSchema = objectInput("User data") {
            stringField("name", "User name") {
                minLength = 2
                maxLength = 100
            }
            stringField("email", "User email") {
                email = true
            }
            numberField("age", "User age") {
                min = 18.0
                max = 120.0
            }
            stringField("password", "User password") {
                minLength = 8
            }.refine({ password ->
                password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { !it.isLetterOrDigit() }
            }, "Password must contain at least one digit, one uppercase letter, one lowercase letter, and one special character")
        }
        
        // 创建输出模式
        outputSchema = objectOutput("Validation result") {
            booleanField("isValid", "Whether the user data is valid")
            arrayField("errors", stringInput("Error message"), "List of validation errors", required = false)
        }
        
        // 实现执行逻辑
        execute = { input ->
            // 输入已经通过验证，可以安全处理
            mapOf(
                "isValid" to true
            )
        }
    }
    
    // 使用工具
    val validInput = mapOf(
        "name" to "John Doe",
        "email" to "john.doe@example.com",
        "age" to 30,
        "password" to "P@ssw0rd"
    )
    
    val invalidInput = mapOf(
        "name" to "J", // 太短
        "email" to "invalid-email", // 无效邮箱
        "age" to 15, // 年龄太小
        "password" to "password" // 不符合密码规则
    )
    
    // 验证有效输入
    val validResult = userValidatorTool.inputSchema.safeParse(validInput)
    println("Valid input: ${validResult is SchemaResult.Success}")
    
    // 验证无效输入
    val invalidResult = userValidatorTool.inputSchema.safeParse(invalidInput)
    println("Invalid input: ${invalidResult is SchemaResult.Failure}")
    if (invalidResult is SchemaResult.Failure) {
        println("Errors: ${invalidResult.error}")
    }
}
```

这个例子展示了如何添加验证规则：

1. 使用内置验证规则，如 `minLength`、`maxLength`、`email`、`min`、`max` 等
2. 使用 `refine` 方法添加自定义验证规则
3. 使用 `safeParse` 方法验证输入，并获取验证结果

## 处理错误

ZodTool 提供了多种错误处理方式：

```kotlin
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

fun main() {
    // 创建除法工具
    val divisionTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "division"
        name = "Division"
        description = "Divides two numbers"
        
        // 创建输入模式
        inputSchema = objectInput("Division input") {
            numberField("dividend", "Dividend")
            numberField("divisor", "Divisor")
        }
        
        // 创建输出模式
        outputSchema = objectOutput("Division output") {
            numberField("quotient", "Quotient", required = false)
            stringField("error", "Error message", required = false)
        }
        
        // 实现执行逻辑，处理错误
        execute = { input ->
            val dividend = (input["dividend"] as Number).toDouble()
            val divisor = (input["divisor"] as Number).toDouble()
            
            try {
                if (divisor == 0.0) {
                    throw ArithmeticException("Division by zero")
                }
                
                mapOf(
                    "quotient" to (dividend / divisor)
                )
            } catch (e: Exception) {
                mapOf(
                    "error" to e.message
                )
            }
        }
    }
    
    // 使用工具
    val validInput = mapOf(
        "dividend" to 10,
        "divisor" to 2
    )
    
    val invalidInput = mapOf(
        "dividend" to 10,
        "divisor" to 0
    )
    
    // 执行有效输入
    val validOutput = runBlocking {
        divisionTool.execute(validInput)
    }
    println("Valid output: $validOutput")
    
    // 执行无效输入
    val invalidOutput = runBlocking {
        divisionTool.execute(invalidInput)
    }
    println("Invalid output: $invalidOutput")
}
```

这个例子展示了如何处理错误：

1. 使用 try-catch 块捕获异常
2. 返回包含错误信息的输出
3. 使用可选字段表示可能的错误

## 与现有工具集成

ZodTool 可以与现有的工具系统集成：

```kotlin
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import ai.kastrax.core.tools.toZodTool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main() {
    // 创建传统的 Tool
    val legacyTool = tool {
        id = "legacy_tool"
        name = "Legacy Tool"
        description = "A legacy tool"
        
        inputSchema = jsonObject {
            "type" to "object"
            "required" to jsonArray("message")
            "properties" to jsonObject {
                "message" to jsonObject {
                    "type" to "string"
                }
            }
        }
        
        outputSchema = jsonObject {
            "type" to "object"
            "required" to jsonArray("result")
            "properties" to jsonObject {
                "result" to jsonObject {
                    "type" to "string"
                }
            }
        }
        
        execute = { input ->
            val message = input.jsonObject["message"]?.jsonPrimitive?.content ?: ""
            jsonObject {
                "result" to "Legacy tool received: $message"
            }
        }
    }
    
    // 创建输入和输出模式
    val inputSchema = objectInput("Legacy tool input") {
        stringField("message", "Message")
    }
    
    val outputSchema = objectOutput("Legacy tool output") {
        stringField("result", "Result")
    }
    
    // 将传统 Tool 转换为 ZodTool
    val zodTool = legacyTool.toZodTool(inputSchema, outputSchema)
    
    // 使用转换后的 ZodTool
    val input = mapOf(
        "message" to "Hello from ZodTool"
    )
    
    // 执行工具
    val output = runBlocking {
        zodTool.execute(input)
    }
    
    println("Output: ${output["result"]}")
    
    // 将 ZodTool 转换回传统 Tool
    val convertedTool = zodTool.toTool()
    
    // 使用转换后的传统 Tool
    val jsonInput = buildJsonObject {
        put("message", "Hello from converted Tool")
    }
    
    // 执行工具
    val jsonOutput = runBlocking {
        convertedTool.execute(jsonInput)
    }
    
    println("JSON output: ${jsonOutput.jsonObject["result"]?.jsonPrimitive?.content}")
}
```

这个例子展示了如何与现有工具系统集成：

1. 使用 `toZodTool` 方法将传统 Tool 转换为 ZodTool
2. 使用 `toTool` 方法将 ZodTool 转换回传统 Tool
3. 在两种工具之间无缝切换

## 下一步

恭喜！您已经学会了 ZodTool 的基本用法。接下来，您可以：

1. 查看 [ZodTool 高级用法指南](advanced-zodtool-usage.md) 了解更多高级特性
2. 查看 [ZodTool 性能优化指南](zodtool-performance.md) 学习如何优化性能
3. 查看 [ZodTool 安全最佳实践](zodtool-security.md) 学习如何安全地使用 ZodTool
4. 查看 [ZodTool 常见问题解答 (FAQ)](zodtool-faq.md) 解答常见问题
5. 查看示例代码，了解更多用法：
   - [计算器示例](../examples/src/main/kotlin/ai/kastrax/examples/ZodCalculatorExample.kt)
   - [简单字符串反转示例](../examples/src/main/kotlin/ai/kastrax/examples/SimpleZodToolExample.kt)
   - [数据类用户验证示例](../examples/src/main/kotlin/ai/kastrax/examples/DataClassZodToolExample.kt)
   - [高级用户搜索示例](../examples/src/main/kotlin/ai/kastrax/examples/AdvancedZodToolExample.kt)

祝您使用 ZodTool 愉快！
