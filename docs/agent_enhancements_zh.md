# KastraX Agent系统增强功能文档

本文档详细介绍了KastraX Agent系统的增强功能，包括配置选项扩展、工具集管理、默认选项配置和工具选择策略等。

## 1. Agent配置选项扩展

我们扩展了Agent的配置选项，增加了更多参数和功能，使Agent更加灵活和强大。

### 1.1 AgentGenerateOptions增强

`AgentGenerateOptions`类现在支持以下新增参数：

```kotlin
data class AgentGenerateOptions(
    val maxSteps: Int = 1,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val executeTools: Boolean = true,
    val output: JsonElement? = null,
    val onStepFinish: ((StepResult) -> Unit)? = null,
    val threadId: String? = null,
    val threadTitle: String? = null,
    val instructions: String? = null,        // 新增：覆盖Agent默认指令
    val toolsets: Map<String, Map<String, Tool>>? = null, // 新增：额外工具集
    val context: List<LlmMessage>? = null,   // 新增：上下文消息
    val memoryOptions: MemoryOptions? = null, // 新增：内存选项
    val runId: String? = null,               // 新增：运行ID
    val toolChoice: ToolChoice = ToolChoice.Auto, // 新增：工具选择策略
    val topP: Double? = null,                // 新增：top-p采样参数
    val frequencyPenalty: Double? = null,    // 新增：频率惩罚
    val presencePenalty: Double? = null      // 新增：存在惩罚
)
```

### 1.2 AgentStreamOptions增强

`AgentStreamOptions`类现在支持以下新增参数：

```kotlin
data class AgentStreamOptions(
    val threadId: String? = null,
    val resourceId: String? = null,
    val threadTitle: String? = null,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val instructions: String? = null,        // 新增：覆盖Agent默认指令
    val toolsets: Map<String, Map<String, Tool>>? = null, // 新增：额外工具集
    val context: List<LlmMessage>? = null,   // 新增：上下文消息
    val memoryOptions: MemoryOptions? = null, // 新增：内存选项
    val runId: String? = null,               // 新增：运行ID
    val onFinish: ((String) -> Unit)? = null, // 新增：完成回调
    val onStepFinish: ((StepResult) -> Unit)? = null, // 新增：步骤完成回调
    val maxSteps: Int = 1,                   // 新增：最大步骤数
    val output: JsonElement? = null,         // 新增：输出模式
    val toolChoice: ToolChoice = ToolChoice.Auto, // 新增：工具选择策略
    val topP: Double? = null,                // 新增：top-p采样参数
    val frequencyPenalty: Double? = null,    // 新增：频率惩罚
    val presencePenalty: Double? = null      // 新增：存在惩罚
)
```

### 1.3 工具选择策略

我们添加了`ToolChoice`枚举，支持以下工具选择策略：

```kotlin
enum class ToolChoice {
    /** 让模型自行决定是否使用工具 */
    Auto,
    /** 不使用任何工具 */
    None,
    /** 要求模型必须使用工具 */
    Required,
    /** 要求模型使用特定工具 */
    Specific;
    
    /** 特定工具的名称（仅在Specific模式下有效） */
    var toolName: String? = null
    
    companion object {
        /**
         * 创建特定工具选择策略
         */
        fun specific(toolName: String): ToolChoice {
            val choice = Specific
            choice.toolName = toolName
            return choice
        }
    }
}
```

## 2. 默认选项配置

我们增强了`AgentBuilder`，支持配置默认生成选项和流式选项：

```kotlin
class AgentBuilder {
    var name: String = ""
    var instructions: String = ""
    lateinit var model: LlmProvider
    var tools: MutableMap<String, Tool> = mutableMapOf()
    var memory: ai.kastrax.memory.api.Memory? = null
    var defaultGenerateOptions: AgentGenerateOptions = AgentGenerateOptions()
    var defaultStreamOptions: AgentStreamOptions = AgentStreamOptions()
    var toolsets: MutableMap<String, MutableMap<String, Tool>> = mutableMapOf()
    
    // ... 其他方法 ...
    
    /**
     * 配置默认生成选项
     */
    fun defaultGenerateOptions(init: DefaultGenerateOptionsBuilder.() -> Unit) {
        val builder = DefaultGenerateOptionsBuilder(defaultGenerateOptions)
        builder.init()
        defaultGenerateOptions = builder.options
    }
    
    /**
     * 配置默认流式选项
     */
    fun defaultStreamOptions(init: DefaultStreamOptionsBuilder.() -> Unit) {
        val builder = DefaultStreamOptionsBuilder(defaultStreamOptions)
        builder.init()
        defaultStreamOptions = builder.options
    }
    
    // ... 其他方法 ...
}
```

使用示例：

```kotlin
val agent = agent {
    name = "MyAgent"
    instructions = "You are a helpful assistant."
    model = openAi("gpt-4o")
    
    // 配置默认生成选项
    defaultGenerateOptions {
        temperature(0.5)
        maxTokens(100)
        topP(0.9)
        toolChoice(ToolChoice.Auto)
    }
    
    // 配置默认流式选项
    defaultStreamOptions {
        temperature(0.6)
        maxTokens(200)
    }
}
```

## 3. 工具集管理

我们引入了工具集(toolsets)的概念，允许将工具分组管理并在运行时动态组合：

### 3.1 在AgentBuilder中定义工具集

```kotlin
val agent = agent {
    name = "MyAgent"
    instructions = "You are a helpful assistant."
    model = openAi("gpt-4o")
    
    // 添加基础工具
    tools {
        tool(calculatorTool)
    }
    
    // 添加工具集
    toolset("weather") {
        tool(weatherTool)
    }
    
    toolset("search") {
        tool(searchTool)
    }
}
```

### 3.2 在运行时使用工具集

```kotlin
// 使用特定工具集生成响应
val response = agent.generate(
    "What's the weather in New York?",
    AgentGenerateOptions(
        toolsets = mapOf("weather" to mapOf(weatherTool.id to weatherTool))
    )
)

// 组合多个工具集
val response2 = agent.generate(
    "Search for weather in New York and calculate the average temperature.",
    AgentGenerateOptions(
        toolsets = mapOf(
            "weather" to mapOf(weatherTool.id to weatherTool),
            "search" to mapOf(searchTool.id to searchTool)
        )
    )
)
```

## 4. 选项合并机制

我们实现了选项合并机制，运行时参数会覆盖默认参数：

```kotlin
/**
 * 合并选项，其中other参数优先级更高
 */
fun merge(other: AgentGenerateOptions): AgentGenerateOptions {
    return copy(
        maxSteps = other.maxSteps.takeIf { it != 1 } ?: maxSteps,
        temperature = other.temperature.takeIf { it != 0.7 } ?: temperature,
        maxTokens = other.maxTokens ?: maxTokens,
        executeTools = other.executeTools,
        output = other.output ?: output,
        onStepFinish = other.onStepFinish ?: onStepFinish,
        threadId = other.threadId ?: threadId,
        threadTitle = other.threadTitle ?: threadTitle,
        instructions = other.instructions ?: instructions,
        toolsets = other.toolsets?.let { otherToolsets ->
            if (toolsets == null) {
                otherToolsets
            } else {
                toolsets + otherToolsets
            }
        } ?: toolsets,
        context = other.context?.let { otherContext ->
            if (context == null) {
                otherContext
            } else {
                context + otherContext
            }
        } ?: context,
        memoryOptions = other.memoryOptions ?: memoryOptions,
        runId = other.runId ?: runId,
        toolChoice = if (other.toolChoice != ToolChoice.Auto) other.toolChoice else toolChoice,
        topP = other.topP ?: topP,
        frequencyPenalty = other.frequencyPenalty ?: frequencyPenalty,
        presencePenalty = other.presencePenalty ?: presencePenalty
    )
}
```

## 5. LLM抽象扩展

我们扩展了LLM抽象层，支持更多参数和功能：

```kotlin
data class LlmOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stop: List<String> = emptyList(),
    val tools: List<JsonElement> = emptyList(),
    val toolChoice: Any = "auto",
    val responseFormat: JsonElement? = null,
    val seed: Long? = null
)
```

## 6. 使用示例

### 6.1 创建具有默认选项的Agent

```kotlin
val agent = agent {
    name = "AdvancedAgent"
    instructions = "You are an advanced agent."
    model = openAi("gpt-4o")
    
    // 添加基础工具
    tools {
        tool(calculatorTool)
    }
    
    // 添加工具集
    toolset("weather") {
        tool(weatherTool)
    }
    
    // 配置默认生成选项
    defaultGenerateOptions {
        temperature(0.5)
        maxTokens(100)
        topP(0.9)
        toolChoice(ToolChoice.Auto)
    }
    
    // 配置默认流式选项
    defaultStreamOptions {
        temperature(0.6)
        maxTokens(200)
    }
}
```

### 6.2 使用特定工具

```kotlin
// 使用特定工具生成响应
val response = agent.generate(
    "Calculate 2 + 2",
    AgentGenerateOptions(
        toolChoice = ToolChoice.specific("calculator")
    )
)
```

### 6.3 流式响应

```kotlin
// 流式生成响应
val response = agent.stream(
    "Tell me about the weather in New York",
    AgentStreamOptions(
        toolsets = mapOf("weather" to mapOf(weatherTool.id to weatherTool)),
        onFinish = { fullText ->
            println("完整响应: $fullText")
        }
    )
)

// 处理流式响应
response.textStream?.collect { chunk ->
    print(chunk) // 实时显示响应片段
}
```

## 7. 总结

通过这些增强功能，KastraX的Agent系统变得更加灵活和强大，能够满足更复杂的应用场景需求。主要改进包括：

1. 增加了更多配置选项，支持更精细的控制
2. 引入了工具集概念，支持动态组合不同的工具
3. 添加了默认选项配置，简化常用参数设置
4. 实现了工具选择策略，支持更精确的工具使用控制
5. 扩展了LLM抽象层，支持更多高级参数

这些功能使KastraX更接近Mastra的功能水平，并在某些方面提供了更符合Kotlin风格的API设计。
