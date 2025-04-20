# 迁移到 ZodTool

本文档提供了从传统 Tool 迁移到 ZodTool 的指南。

## 为什么要迁移到 ZodTool？

ZodTool 提供了以下优势：

1. **类型安全**：使用 kastrax-zod 提供的类型安全模式定义和验证
2. **更好的开发体验**：更清晰的 API 和更好的 IDE 支持
3. **更强的验证**：更强大的数据验证和转换能力
4. **更好的错误处理**：更详细和更有用的错误消息
5. **向后兼容**：与现有工具系统完全兼容

## 迁移步骤

### 1. 从传统 Tool 迁移到 ZodTool

假设您有一个传统的 Tool 实现：

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "Calculator"
    description = "Performs basic arithmetic operations"
    
    inputSchema = jsonObject {
        "type" to "object"
        "required" to jsonArray("operation", "a", "b")
        "properties" to jsonObject {
            "operation" to jsonObject {
                "type" to "string"
                "enum" to jsonArray("add", "subtract", "multiply", "divide")
            }
            "a" to jsonObject {
                "type" to "number"
            }
            "b" to jsonObject {
                "type" to "number"
            }
        }
    }
    
    outputSchema = jsonObject {
        "type" to "object"
        "required" to jsonArray("result")
        "properties" to jsonObject {
            "result" to jsonObject {
                "type" to "number"
            }
        }
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
        
        jsonObject {
            "result" to result
        }
    }
}
```

您可以使用以下方式迁移到 ZodTool：

```kotlin
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
```

### 2. 使用数据类

您还可以使用数据类来进一步改进代码：

```kotlin
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
```

### 3. 转换现有的 Tool

如果您有现有的 Tool 实例，可以使用 `toZodTool` 方法将其转换为 ZodTool：

```kotlin
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
val zodTool = existingTool.toZodTool(inputSchema, outputSchema)
```

### 4. 将 ZodTool 转换为传统 Tool

如果您需要将 ZodTool 转换回传统的 Tool（例如，为了与旧代码兼容），可以使用 `toTool` 方法：

```kotlin
val legacyTool = zodTool.toTool()
```

或者使用 `zodToolAsLegacy` 函数直接创建传统的 Tool：

```kotlin
val legacyTool = zodToolAsLegacy<Map<String, Any?>, Map<String, Any?>> {
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
```

## 最佳实践

1. **使用数据类**：尽可能使用数据类来表示输入和输出，这样可以获得更好的类型安全性和代码可读性。

2. **使用辅助函数**：使用 `stringInput`、`numberInput` 等辅助函数来创建模式，这样可以简化代码并提高可读性。

3. **添加验证**：利用 kastrax-zod 的验证功能，为输入添加验证规则，例如 `minLength`、`maxLength`、`pattern` 等。

4. **处理错误**：使用 `safeParse` 方法来验证输入，并提供有用的错误消息。

5. **文档**：为模式和字段添加描述，这样可以生成更好的文档和错误消息。

## 常见问题

### 如何处理嵌套对象？

```kotlin
val userSchema = objectInput("User data") {
    stringField("name", "User name")
    objectField("address", "User address") {
        stringField("street", "Street address")
        stringField("city", "City")
        stringField("country", "Country")
    }
}
```

### 如何处理数组？

```kotlin
val userSchema = objectInput("User data") {
    stringField("name", "User name")
    arrayField("hobbies", stringInput("Hobby"), "User hobbies")
}
```

### 如何处理枚举？

```kotlin
enum class UserRole { ADMIN, USER, GUEST }

val userSchema = objectInput("User data") {
    stringField("name", "User name")
    enumField("role", UserRole::class.java, "User role")
}
```

### 如何处理可选字段？

```kotlin
val userSchema = objectInput("User data") {
    stringField("name", "User name", required = true)
    stringField("middleName", "User middle name", required = false)
}
```

### 如何处理默认值？

```kotlin
val userSchema = objectInput("User data") {
    stringField("name", "User name")
    numberField("age", "User age", required = false)
}.default(mapOf("age" to 18))
```
