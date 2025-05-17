# 代码智能体 (CodeAgent) 改造计划

## 背景

当前的代码智能体实现需要改进，以支持更灵活的交互模式，包括普通模式和流式模式。我们需要参考 kastrax 中的 Agent 接口定义，设计一个更加通用和强大的 CodeAgent 接口。

## 目标

1. 创建 CodeAgent 接口，定义普通模式和流式模式的方法
2. 实现 CodeAgent 接口，支持普通模式和流式模式
3. 确保实现类能够正确处理工具调用和上下文
4. 无缝集成到现有的 kastrax 生态系统中

## 设计方案

### 1. CodeAgent 接口

创建 CodeAgent 接口，参考 kastrax 的 Agent 接口，支持普通模式和流式模式：

```kotlin
interface CodeAgent {
    /**
     * 代理名称
     */
    val name: String
    
    /**
     * 生成响应
     *
     * @param prompt 提示文本
     * @param options 生成选项
     * @return 代理响应
     */
    suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): AgentResponse
    
    /**
     * 生成响应
     *
     * @param messages 消息列表
     * @param options 生成选项
     * @return 代理响应
     */
    suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): AgentResponse
    
    /**
     * 流式生成响应
     *
     * @param prompt 提示文本
     * @param options 流式选项
     * @return 带有流的代理响应
     */
    suspend fun stream(
        prompt: String,
        options: AgentStreamOptions = AgentStreamOptions()
    ): AgentResponse
    
    /**
     * 流式生成响应
     *
     * @param messages 消息列表
     * @param options 流式选项
     * @return 带有流的代理响应
     */
    suspend fun stream(
        messages: List<LlmMessage>,
        options: AgentStreamOptions = AgentStreamOptions()
    ): AgentResponse
    
    /**
     * 重置代理状态
     */
    suspend fun reset()
}
```

### 2. CodeAgentImpl 实现类

实现 CodeAgent 接口，基于 kastrax-core 的 Agent 实现：

```kotlin
class CodeAgentImpl(
    override val name: String,
    private val agent: Agent,
    private val config: CodeAgentConfig = CodeAgentConfig()
) : KastraXBase("CODE_AGENT", name), CodeAgent {

    /**
     * 生成响应
     *
     * @param prompt 提示文本
     * @param options 生成选项
     * @return 代理响应
     */
    override suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions
    ): AgentResponse {
        logger.debug("生成响应，提示：$prompt")
        
        // 增强提示
        val enhancedPrompt = enhancePrompt(prompt)
        
        // 使用底层 Agent 生成响应
        return agent.generate(enhancedPrompt, options)
    }
    
    /**
     * 生成响应
     *
     * @param messages 消息列表
     * @param options 生成选项
     * @return 代理响应
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): AgentResponse {
        logger.debug("生成响应，消息数：${messages.size}")
        
        // 增强消息
        val enhancedMessages = enhanceMessages(messages)
        
        // 使用底层 Agent 生成响应
        return agent.generate(enhancedMessages, options)
    }
    
    /**
     * 流式生成响应
     *
     * @param prompt 提示文本
     * @param options 流式选项
     * @return 带有流的代理响应
     */
    override suspend fun stream(
        prompt: String,
        options: AgentStreamOptions
    ): AgentResponse {
        logger.debug("流式生成响应，提示：$prompt")
        
        // 增强提示
        val enhancedPrompt = enhancePrompt(prompt)
        
        // 使用底层 Agent 流式生成响应
        return agent.stream(enhancedPrompt, options)
    }
    
    /**
     * 流式生成响应
     *
     * @param messages 消息列表
     * @param options 流式选项
     * @return 带有流的代理响应
     */
    override suspend fun stream(
        messages: List<LlmMessage>,
        options: AgentStreamOptions
    ): AgentResponse {
        logger.debug("流式生成响应，消息数：${messages.size}")
        
        // 增强消息
        val enhancedMessages = enhanceMessages(messages)
        
        // 创建用户消息
        val lastUserMessage = enhancedMessages.lastOrNull { it.role == LlmMessageRole.USER }?.content ?: ""
        
        // 使用底层 Agent 流式生成响应
        return agent.stream(lastUserMessage, options)
    }
    
    /**
     * 重置代理状态
     */
    override suspend fun reset() {
        logger.debug("重置代理状态")
        agent.reset()
    }
    
    /**
     * 增强提示
     *
     * @param prompt 原始提示
     * @return 增强后的提示
     */
    private fun enhancePrompt(prompt: String): String {
        // 在这里可以添加代码相关的上下文增强
        return prompt
    }
    
    /**
     * 增强消息
     *
     * @param messages 原始消息列表
     * @return 增强后的消息列表
     */
    private fun enhanceMessages(messages: List<LlmMessage>): List<LlmMessage> {
        // 在这里可以对消息进行增强，例如添加代码上下文
        return messages
    }
}
```

### 3. CodeAgentConfig 配置类

创建配置类，用于配置代码智能体的行为：

```kotlin
data class CodeAgentConfig(
    // 代码生成温度
    val codeGenerationTemperature: Double = 0.3,
    // 代码生成最大令牌数
    val codeGenerationMaxTokens: Int = 2000,
    // 代码解释温度
    val codeExplanationTemperature: Double = 0.7,
    // 代码解释最大令牌数
    val codeExplanationMaxTokens: Int = 2000,
    // 代码补全温度
    val codeCompletionTemperature: Double = 0.2,
    // 代码补全最大令牌数
    val codeCompletionMaxTokens: Int = 1000,
    // 测试生成温度
    val testGenerationTemperature: Double = 0.5,
    // 测试生成最大令牌数
    val testGenerationMaxTokens: Int = 2000
)
```

## 实现步骤

1. 创建 CodeAgent 接口
   - 定义 generate 和 stream 方法
   - 支持单条提示和多条消息两种输入方式

2. 实现 CodeAgentImpl 类
   - 基于 kastrax-core 的 Agent 实现
   - 添加提示和消息增强功能
   - 实现所有接口方法

3. 创建 CodeAgentConfig 配置类
   - 定义代码生成、解释、补全和测试生成的配置参数

4. 编写单元测试
   - 测试普通模式生成
   - 测试流式模式生成
   - 测试工具调用功能

5. 集成到现有系统
   - 确保与现有的 kastrax 生态系统兼容
   - 更新相关的服务和控制器

## 使用示例

```kotlin
// 创建底层 Agent
val baseAgent = agent {
    name = "CodeAssistant"
    instructions = "你是一个专业的代码助手，擅长编写高质量的代码。"
    model = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CODER)
        apiKey(System.getenv("DEEPSEEK_API_KEY"))
        temperature(0.3)
        maxTokens(2000)
    }
}

// 创建 CodeAgent
val codeAgent = CodeAgentImpl(
    name = "CodeAgent",
    agent = baseAgent,
    config = CodeAgentConfig()
)

// 普通模式使用
val response = codeAgent.generate("编写一个计算斐波那契数列的函数", AgentGenerateOptions())
println(response.text)

// 流式模式使用
val streamResponse = codeAgent.stream("编写一个快速排序算法", AgentStreamOptions())
streamResponse.textStream?.collect { chunk ->
    print(chunk)
}
```

## 优势

1. **统一接口**: 提供统一的接口，支持普通模式和流式模式
2. **灵活性**: 支持单条提示和多条消息两种输入方式
3. **可扩展性**: 可以轻松添加新的功能和增强
4. **集成性**: 无缝集成到现有的 kastrax 生态系统中

## 后续工作

1. 实现更高级的提示和消息增强功能
2. 添加更多的代码相关工具
3. 优化流式生成的性能
4. 添加更多的配置选项
5. 实现更多的专业化代码智能体，如代码解释、代码重构等

## 时间线

- 第 1 周: 设计和实现 CodeAgent 接口和基本实现
- 第 2 周: 实现提示和消息增强功能，编写单元测试
- 第 3 周: 集成到现有系统，进行系统测试
- 第 4 周: 优化性能，添加更多功能，完成文档
