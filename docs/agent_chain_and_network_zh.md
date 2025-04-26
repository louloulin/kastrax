# Agent链和网络

KastraX提供了两种主要的Agent组合方式：Agent链和Agent网络。这两种方式各有优势，适用于不同的场景。

## 1. Agent链

Agent链是一种简单的顺序执行模式，其中一个Agent的输出作为下一个Agent的输入。这种模式适用于有明确步骤和顺序的任务。

### 1.1 创建Agent链

使用DSL风格的构建器创建Agent链：

```kotlin
val chain = agentChain {
    name = "翻译和总结链"
    
    // 添加翻译代理
    step {
        name = "翻译"
        agent = translationAgent
        description = "将文本翻译成英文"
    }
    
    // 添加总结代理
    step {
        name = "总结"
        agent = summaryAgent
        description = "总结翻译后的文本"
    }
}
```

### 1.2 使用Agent链

```kotlin
// 执行链
val result = chain.execute("这是一段需要翻译和总结的中文文本。")
println(result.finalOutput)

// 获取中间结果
val intermediateResults = result.stepResults
intermediateResults.forEach { (stepName, output) ->
    println("步骤 '$stepName' 的输出: $output")
}
```

## 2. Agent网络

Agent网络是一种更灵活的组合方式，允许多个专业Agent协作解决复杂问题。网络中的路由Agent决定调用哪些专业Agent以及调用顺序。

### 2.1 基本Agent网络

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

### 2.2 增强型Agent网络

KastraX提供了增强型Agent网络功能，支持上下文感知路由和交互可视化：

```kotlin
val enhancedNetwork = agentNetwork {
    name = "增强型研究网络"
    instructions = """
        你是一个研究协调系统，负责将查询路由到适当的专业代理。
        
        根据查询的性质，你可以调用以下专业代理：
        1. 研究代理 - 用于综合信息和协调研究
        2. 网络搜索代理 - 用于查找最新的在线信息
        3. 数据分析代理 - 用于分析和解释数据
        
        你的目标是提供全面、准确的研究结果。
        
        使用协作模式来最大化研究质量：
        - 顺序协作：一个代理接一个代理工作
        - 并行协作：多个代理同时工作，然后综合结果
        - 专家小组：让多个代理评估同一问题
        - 迭代改进：让一个代理工作，然后让另一个代理改进结果
    """.trimIndent()
    model = openAi("gpt-4o")
    
    // 添加专业代理
    agent(researchAgent)
    agent(webSearchAgent)
    agent(dataAnalysisAgent)
    
    // 使用上下文感知路由策略
    useContextAwareRouting()
    
    // 启用交互可视化
    enableVisualization()
}
```

#### 2.2.1 上下文感知路由

上下文感知路由策略提供了更丰富的上下文共享和代理协作功能：

- 在代理之间共享相关上下文
- 支持多种协作模式（顺序、并行、专家小组、迭代改进）
- 提供更详细的指令和上下文类型选项

#### 2.2.2 交互可视化

启用交互可视化后，可以生成HTML格式的可视化报告，展示代理交互的时间线和详细信息：

```kotlin
// 生成并保存可视化
val visualization = network.getAgentInteractionVisualization()
if (visualization != null) {
    val visualizationFile = File("agent_network_visualization.html")
    visualizationFile.writeText(visualization)
    println("可视化已保存到: ${visualizationFile.absolutePath}")
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

// 获取代理交互历史
val history = network.getAgentInteractionSummary()
println(history)
```

## 3. Agent链与Agent网络的比较

| 特性 | Agent链 | Agent网络 |
|------|---------|----------|
| **控制流** | 预定义的顺序执行 | 动态决策的执行流程 |
| **灵活性** | 低（固定步骤） | 高（动态路由） |
| **适用场景** | 明确步骤的任务 | 复杂、需要多专家协作的任务 |
| **实现复杂度** | 低 | 中到高 |
| **可预测性** | 高 | 中 |
| **可扩展性** | 有限 | 高（可轻松添加新代理） |

## 4. 最佳实践

### 4.1 选择合适的组合方式

- 使用**Agent链**当：
  - 任务有明确的顺序步骤
  - 需要高度可预测的执行流程
  - 每个步骤有明确的输入和输出

- 使用**Agent网络**当：
  - 任务复杂且需要多种专业知识
  - 执行路径不固定，需要动态决策
  - 需要代理间的协作和上下文共享

### 4.2 Agent网络设计建议

- 为每个专业Agent提供清晰、具体的指令
- 为路由Agent提供详细的协调指南
- 使用上下文感知路由以提高代理协作效率
- 启用可视化功能以便于调试和分析
- 在复杂任务中使用多种协作模式

### 4.3 性能优化

- 对于Agent链，考虑使用并行执行适用的步骤
- 对于Agent网络，合理设置`maxSteps`参数以控制执行深度
- 使用适当的模型大小，不同步骤可以使用不同复杂度的模型
- 对于频繁使用的子任务，考虑缓存结果
