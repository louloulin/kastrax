# kastrax-zod 与工具系统集成文档

## 概述

kastrax-zod 模块提供了类型安全的模式定义和验证功能，已经成功集成到 kastrax 工具系统中。本文档介绍了集成的主要组件和使用方法。

## 主要组件

### 1. Schema 扩展函数

在 `SchemaJsonExtensions.kt` 中实现了以下扩展函数：

- `toJsonSchema()`: 将 Schema 转换为 JSON Schema
- `parseJson()`: 从 JSON 解析输入
- `toJson()`: 将输出转换为 JSON

这些函数使 Schema 能够与基于 JSON 的工具系统无缝集成。

### 2. ZodTool 接口和 ZodToolBuilder

在 `ZodTool.kt` 中实现了：

- `ZodTool` 接口：使用 kastrax-zod 的工具接口
- `ZodToolBuilder` 类：用于构建 ZodTool 实例
- `zodTool` DSL 函数：创建 ZodTool 的 DSL 函数
- `zodToolAsLegacy` DSL 函数：创建传统 Tool 的 DSL 函数（使用 ZodTool）

### 3. 常见工具模式的辅助函数

在 `SchemaToolHelpers.kt` 中实现了以下辅助函数：

- 输入模式辅助函数：`stringInput`, `numberInput`, `booleanInput`, `objectInput`, `arrayInput`, `enumInput` 等
- 输出模式辅助函数：`stringOutput`, `numberOutput`, `booleanOutput`, `objectOutput`, `arrayOutput`, `enumOutput` 等
- 对象模式构建器的扩展函数：`stringField`, `numberField`, `booleanField`, `objectField`, `arrayField`, `enumField` 等

### 4. 与现有工具系统的集成

在 `ToolExtensions.kt` 中实现了：

- `Tool.toZodTool()`: 将传统的 Tool 转换为 ZodTool
- `ZodTool.toTool()`: 将 ZodTool 转换为传统的 Tool

## 使用示例

### 基本使用

```kotlin
// 定义计算器输入模式
val calculatorInputSchema = objectInput("Calculator input") {
    stringField("operation", "Operation to perform") {
        enum("add", "subtract", "multiply", "divide")
    }
    numberField("a", "First operand")
    numberField("b", "Second operand")
}

// 定义计算器输出模式
val calculatorOutputSchema = objectOutput("Calculator output") {
    numberField("result", "Result of the operation")
}

// 创建计算器工具
val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
    id = "calculator"
    name = "Calculator"
    description = "Performs basic arithmetic operations"
    inputSchema = calculatorInputSchema
    outputSchema = calculatorOutputSchema
    
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

// 使用计算器工具
val input = mapOf(
    "operation" to "add",
    "a" to 5,
    "b" to 3
)

// 验证输入
val validationResult = calculatorTool.inputSchema.safeParse(input)
if (validationResult is SchemaResult.Success) {
    println("Input is valid")
} else {
    println("Input is invalid: ${(validationResult as SchemaResult.Failure).error}")
    return
}

// 执行工具
val output = runBlocking {
    calculatorTool.execute(input)
}

println("Result: ${output["result"]}")
```

### 使用数据类

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

## 优势

1. **类型安全**：使用 kastrax-zod 提供的类型安全模式定义和验证
2. **更好的开发体验**：更清晰的 API 和更好的 IDE 支持
3. **更强的验证**：更强大的数据验证和转换能力
4. **更好的错误处理**：更详细和更有用的错误消息
5. **向后兼容**：与现有工具系统完全兼容

## 注意事项

由于 kastrax-core 和 kastrax-zod 之间存在循环依赖关系，在使用时需要注意：

1. kastrax-zod 依赖于 kastrax-core 中的基本接口
2. kastrax-core 中的 ZodTool 实现依赖于 kastrax-zod 中的 Schema 实现

这种循环依赖是通过在编译时正确设置依赖关系来解决的。在实际使用中，您只需要依赖 kastrax-core，它会自动包含 kastrax-zod。
