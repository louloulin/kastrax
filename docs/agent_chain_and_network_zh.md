# KastraX Agent链和组合模式功能文档

本文档详细介绍了KastraX中新增的Agent链和组合模式功能，包括AgentChain和AgentNetwork的实现和使用方法。

## 1. Agent链

Agent链允许按顺序执行多个Agent，每个Agent的输出作为下一个Agent的输入，形成一个处理流水线。

### 1.1 AgentChain类

`AgentChain`类是Agent链的核心实现，它具有以下主要特性：

```kotlin
class AgentChain(config: AgentChainConfig) : KastraXBase(component = "AGENT_CHAIN", name = config.name) {
    // 链名称
    private val _name: String = config.name
    // 链描述
    val description: String = config.description
    // 代理列表
    val agents: List<Agent> = config.agents
    // 输入模式
    val inputSchema: JsonElement? = config.inputSchema
    // 输出模式
    val outputSchema: JsonElement? = config.outputSchema
    
    // 执行方法
    suspend fun execute(input: String, options: AgentChainExecuteOptions = AgentChainExecuteOptions()): AgentChainResult
    
    // 流式执行方法
    suspend fun streamExecute(input: String, options: AgentChainExecuteOptions = AgentChainExecuteOptions()): Flow<AgentChainStatusUpdate>
}
```

### 1.2 创建Agent链

使用DSL风格的构建器创建Agent链：

```kotlin
val chain = agentChain {
    name = "研究链"
    description = "一个用于执行研究任务的代理链"
    
    // 添加代理（按执行顺序）
    agent(researchPlannerAgent)
    agent(informationGatheringAgent)
    agent(reportGenerationAgent)
}
```

### 1.3 执行Agent链

有两种方式执行Agent链：

#### 1.3.1 同步执行

```kotlin
val result = chain.execute("研究量子计算在密码学中的应用")

// 检查结果
if (result.success) {
    println("最终输出: ${result.output}")
    
    // 查看每个步骤的结果
    result.steps.forEach { step ->
        println("代理: ${step.agentName}")
        println("输入: ${step.input}")
        println("输出: ${step.output}")
    }
} else {
    println("执行失败: ${result.error}")
}
```

#### 1.3.2 流式执行

```kotlin
chain.streamExecute("研究量子计算在密码学中的应用").collect { update ->
    when (update.status) {
        AgentChainStatus.STARTED -> {
            println("链开始执行")
        }
        AgentChainStatus.IN_PROGRESS -> {
            println("正在执行: ${update.agentName} (进度: ${update.progress}%)")
        }
        AgentChainStatus.STEP_COMPLETED -> {
            println("步骤完成: ${update.agentName}")
            println("输出摘要: ${update.step?.output?.take(100)}...")
        }
        AgentChainStatus.COMPLETED -> {
            println("链执行完成 (进度: ${update.progress}%)")
            println("最终输出: ${update.output}")
        }
        else -> {
            println("状态更新: ${update.status} - ${update.message}")
        }
    }
}
```

### 1.4 执行选项

`AgentChainExecuteOptions`类提供了执行选项：

```kotlin
data class AgentChainExecuteOptions(
    // 代理执行选项
    val agentOptions: AgentGenerateOptions = AgentGenerateOptions(),
    // 步骤完成回调
    val onStepFinish: ((AgentChainStep) -> Unit)? = null,
    // 步骤错误回调
    val onStepError: ((String, Throwable) -> Unit)? = null
)
```

## 2. Agent网络

Agent网络允许多个专业Agent协作解决复杂问题，使用一个路由Agent来决定调用哪个专业Agent。

### 2.1 AgentNetwork类

`AgentNetwork`类是Agent网络的核心实现，它实现了`Agent`接口，可以像普通Agent一样使用：

```kotlin
class AgentNetwork(config: AgentNetworkConfig) : KastraXBase(component = "NETWORK", name = config.name), Agent {
    // 网络指令
    private val instructions = config.instructions
    // 专业代理列表
    private val agents = config.agents
    // LLM模型
    private val model = config.model
    // 路由代理
    private val routingAgent: LLMAgent
    
    // 代理历史记录
    private val agentHistory: MutableMap<String, MutableList<AgentInteraction>> = mutableMapOf()
    
    // 获取代理历史记录
    fun getAgentHistory(agentId: String): List<AgentInteraction>
    
    // 获取所有代理交互历史
    fun getAgentInteractionHistory(): Map<String, List<AgentInteraction>>
    
    // 获取代理交互摘要
    fun getAgentInteractionSummary(): String
    
    // 获取路由代理
    fun getRoutingAgent(): Agent
    
    // 获取代理列表
    fun getAgents(): List<Agent>
}
```

### 2.2 创建Agent网络

使用DSL风格的构建器创建Agent网络：

```kotlin
val network = agentNetwork {
    name = "研究网络"
    instructions = """
        你是一个研究协调系统，负责将查询路由到适当的专业代理。
        
        根据查询的性质，你可以调用以下专业代理：
        1. 研究代理 - 用于综合信息和协调研究
        2. 网络搜索代理 - 用于查找最新的在线信息
        3. 数据分析代理 - 用于分析和解释数据
        
        你的目标是提供全面、准确的研究结果。
    """.trimIndent()
    model = openAi("gpt-4o")
    
    // 添加专业代理
    agent(researchAgent)
    agent(webSearchAgent)
    agent(dataAnalysisAgent)
}
```

### 2.3 使用Agent网络

由于`AgentNetwork`实现了`Agent`接口，可以像使用普通Agent一样使用它：

```kotlin
// 生成响应
val response = network.generate("分析人工智能在医疗保健中的最新应用和趋势")
println(response.text)

// 流式生成
val streamResponse = network.stream("分析人工智能在医疗保健中的最新应用和趋势")
streamResponse.textStream?.collect { chunk ->
    print(chunk)
}

// 查看代理交互历史
println(network.getAgentInteractionSummary())
```

### 2.4 工作原理

Agent网络使用一个路由代理和一个特殊的"transmit"工具来协调多个专业代理：

1. 用户查询发送到路由代理
2. 路由代理分析查询并决定调用哪个专业代理
3. 路由代理使用"transmit"工具调用选定的专业代理
4. 专业代理处理任务并返回结果
5. 路由代理可以继续调用其他专业代理或提供最终答案

"transmit"工具支持以下格式的调用：

```json
{
  "actions": [
    {
      "agent": "agent_name",
      "input": "给代理的详细指令",
      "includeHistory": true
    }
  ]
}
```

## 3. 工作流集成

KastraX还提供了工作流系统与Agent链的集成，允许在工作流中使用Agent链作为步骤。

### 3.1 AgentChainWorkflowStep类

`AgentChainWorkflowStep`类实现了`WorkflowStep`接口，允许在工作流中使用Agent链：

```kotlin
class AgentChainWorkflowStep(
    override val id: String,
    override val name: String = id,
    override val description: String = "",
    val chain: AgentChain,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    val outputMapping: (String) -> Map<String, Any?> = { mapOf("text" to it) },
    override val condition: (WorkflowContext) -> Boolean = { true },
    override val config: StepConfig? = null
) : WorkflowStep
```

### 3.2 在工作流中使用Agent链

使用`agentChainStep`扩展函数在工作流中添加Agent链步骤：

```kotlin
val workflow = workflow {
    name = "研究工作流"
    description = "执行研究任务的工作流"
    
    // 添加Agent链步骤
    agentChainStep(
        id = "research",
        chain = researchChain,
        after = listOf("previous_step")
    ) {
        name = "研究步骤"
        description = "执行研究任务"
        
        // 定义变量引用
        variable("input", "workflow.input.query")
        
        // 定义输出映射
        outputMapping = { result ->
            mapOf(
                "text" to result,
                "summary" to result.take(100)
            )
        }
        
        // 定义执行条件
        condition = { context ->
            context.getVariable("workflow.input.execute_research") == true
        }
    }
}
```

## 4. 使用场景

### 4.1 Agent链使用场景

- **多步骤处理流水线**：将复杂任务分解为多个顺序步骤
- **专业化处理**：每个Agent专注于特定任务，如规划、研究、总结等
- **渐进式精炼**：每个Agent逐步精炼和改进前一个Agent的输出
- **质量控制**：使用后续Agent验证和改进前面Agent的输出

### 4.2 Agent网络使用场景

- **多领域问题解决**：协调多个专业Agent解决跨领域问题
- **动态任务分配**：根据查询内容动态选择最合适的Agent
- **并行处理**：同时调用多个Agent处理不同方面的问题
- **协作解决**：多个Agent协作解决复杂问题，每个Agent贡献自己的专业知识

## 5. 最佳实践

### 5.1 Agent链最佳实践

- 每个Agent应该有明确的职责和专业领域
- 按照逻辑顺序排列Agent，确保信息流畅传递
- 使用流式执行获取实时进度和状态更新
- 实现错误处理回调，处理执行过程中的异常

### 5.2 Agent网络最佳实践

- 为路由代理提供清晰的指令，说明每个专业Agent的能力和适用场景
- 为每个专业Agent提供详细的系统提示，确保它们能够正确处理任务
- 使用includeHistory参数在需要时提供历史上下文
- 定期检查代理交互历史，了解网络的决策过程

## 6. 总结

KastraX的Agent链和组合模式功能提供了强大的工具，使开发者能够构建更复杂、更智能的AI应用。通过组合多个专业Agent，可以解决单个Agent难以处理的复杂问题，同时保持代码的模块化和可维护性。

这些功能的实现使KastraX更接近Mastra的功能水平，并在某些方面提供了更符合Kotlin风格的API设计。未来，我们将继续增强这些功能，添加更多高级特性，如Agent状态管理、内存共享和更复杂的路由策略。
