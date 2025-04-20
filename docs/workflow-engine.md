# 工作流引擎设计与使用指南

## 概述

工作流引擎是 kastrax 框架的核心组件之一，它允许开发者定义和执行由多个 AI 代理组成的复杂工作流。工作流引擎提供了一种声明式的方式来组织和协调多个代理的工作，使它们能够协同完成复杂任务。

## 核心概念

### 工作流 (Workflow)

工作流是一系列步骤的集合，这些步骤按照特定的顺序和依赖关系执行。工作流有以下特点：

- **名称和描述**：每个工作流都有唯一的名称和可选的描述
- **步骤集合**：工作流包含一个或多个步骤
- **执行顺序**：步骤按照依赖关系确定执行顺序
- **输入和输出**：工作流接收输入并产生输出

### 步骤 (Step)

步骤是工作流中的基本执行单元，通常由一个 AI 代理执行。步骤有以下特点：

- **ID 和名称**：每个步骤都有唯一的 ID 和可选的名称
- **描述**：步骤的描述信息
- **依赖关系**：步骤可以依赖于其他步骤
- **变量映射**：步骤可以使用工作流输入和其他步骤的输出作为输入
- **执行逻辑**：步骤的具体执行逻辑，通常由 AI 代理实现

### 变量引用 (Variable Reference)

变量引用是一种在步骤之间传递数据的机制，使用 JSON 路径表达式来引用数据。常见的变量引用包括：

- **输入引用**：`$.input.key` 引用工作流输入中的数据
- **步骤输出引用**：`$.steps.stepId.output.key` 引用其他步骤的输出
- **全局变量引用**：`$.variables.key` 引用工作流上下文中的全局变量

### 工作流上下文 (Workflow Context)

工作流上下文是工作流执行过程中的状态容器，包含以下内容：

- **输入数据**：工作流的输入数据
- **步骤结果**：已执行步骤的结果
- **全局变量**：工作流执行过程中的全局变量

## 工作流引擎架构

工作流引擎的架构由以下主要组件组成：

1. **工作流接口 (Workflow)**：定义工作流的基本操作
2. **工作流步骤接口 (WorkflowStep)**：定义步骤的基本操作
3. **工作流上下文 (WorkflowContext)**：管理工作流执行状态
4. **工作流执行引擎**：负责按照依赖关系执行步骤
5. **变量解析器**：解析变量引用并获取实际值
6. **工作流构建器**：提供 DSL 用于创建工作流

## 使用指南

### 创建工作流

使用 DSL 创建工作流：

```kotlin
val workflow = workflow {
    name = "my-workflow"
    description = "My workflow description"

    step(agent1) {
        id = "step1"
        name = "Step 1"
        description = "First step"
        variables = mapOf(
            "input1" to variable("$.input.key1")
        )
    }

    step(agent2) {
        id = "step2"
        name = "Step 2"
        description = "Second step"
        after("step1")
        variables = mapOf(
            "input2" to variable("$.steps.step1.output.result")
        )
    }
}
```

### 执行工作流

同步执行工作流：

```kotlin
val input = mapOf("key1" to "value1")
val result = workflow.execute(input)

if (result.success) {
    println("Workflow executed successfully")
    println("Output: ${result.output}")
} else {
    println("Workflow execution failed: ${result.error}")
}
```

流式执行工作流：

```kotlin
val input = mapOf("key1" to "value1")
workflow.streamExecute(input).collect { update ->
    when (update.status) {
        WorkflowStatus.STARTED -> println("Workflow started")
        WorkflowStatus.IN_PROGRESS -> println("Step ${update.stepId} in progress (${update.progress}%)")
        WorkflowStatus.COMPLETED -> println("Workflow completed")
        WorkflowStatus.FAILED -> println("Workflow failed: ${update.message}")
    }
}
```

### 自定义步骤输出映射

可以自定义步骤的输出映射函数：

```kotlin
step(agent) {
    id = "custom-step"
    // ...
    outputMapping = { text ->
        // 解析文本并提取结构化数据
        val lines = text.split("\n")
        val title = lines.firstOrNull() ?: ""
        val content = lines.drop(1).joinToString("\n")
        
        mapOf(
            "title" to title,
            "content" to content
        )
    }
}
```

## 高级功能

### 错误处理

工作流引擎提供了多种错误处理机制：

1. **步骤错误回调**：通过 `onStepError` 回调函数处理步骤执行错误
2. **超时处理**：设置 `timeout` 参数处理工作流执行超时
3. **最大步骤限制**：设置 `maxSteps` 参数限制最大执行步骤数

```kotlin
val options = WorkflowExecuteOptions(
    maxSteps = 5,
    timeout = 30000,
    onStepError = { stepId, error ->
        println("Error executing step $stepId: ${error.message}")
    }
)

workflow.execute(input, options)
```

### 进度监控

使用 `onStepFinish` 回调函数监控工作流执行进度：

```kotlin
val options = WorkflowExecuteOptions(
    onStepFinish = { stepResult ->
        println("Step ${stepResult.stepId} completed")
        println("Output: ${stepResult.output}")
    }
)

workflow.execute(input, options)
```

### 与内存系统集成

工作流引擎可以与 kastrax 的内存系统集成，通过 `threadId` 参数指定线程 ID：

```kotlin
val options = WorkflowExecuteOptions(
    threadId = "thread-123"
)

workflow.execute(input, options)
```

## 最佳实践

1. **步骤粒度**：将复杂任务分解为适当粒度的步骤，每个步骤专注于一个特定任务
2. **明确依赖**：使用 `after` 函数明确定义步骤之间的依赖关系
3. **变量引用**：使用变量引用在步骤之间传递数据，避免硬编码
4. **错误处理**：实现适当的错误处理机制，确保工作流能够优雅地处理错误
5. **监控进度**：使用回调函数或流式执行监控工作流执行进度
6. **输出映射**：使用输出映射函数将非结构化文本转换为结构化数据

## 示例

### 内容创作工作流

```kotlin
val contentCreationWorkflow = workflow {
    name = "content-creation"
    description = "Create content about a topic"

    step(researchAgent) {
        id = "research"
        name = "Research"
        description = "Research the topic"
        variables = mapOf(
            "topic" to variable("$.input.topic")
        )
    }

    step(writingAgent) {
        id = "writing"
        name = "Writing"
        description = "Write an article based on research"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text")
        )
    }

    step(editingAgent) {
        id = "editing"
        name = "Editing"
        description = "Edit the article"
        after("writing")
        variables = mapOf(
            "draft" to variable("$.steps.writing.output.text")
        )
    }
}
```

### 数据分析工作流

```kotlin
val dataAnalysisWorkflow = workflow {
    name = "data-analysis"
    description = "Analyze data and generate a report"

    step(dataCollectionAgent) {
        id = "data_collection"
        name = "Data Collection"
        description = "Collect data from sources"
        variables = mapOf(
            "topic" to variable("$.input.topic"),
            "sources" to variable("$.input.sources")
        )
    }

    step(dataAnalysisAgent) {
        id = "data_analysis"
        name = "Data Analysis"
        description = "Analyze collected data"
        after("data_collection")
        variables = mapOf(
            "data" to variable("$.steps.data_collection.output.text")
        )
    }

    step(reportGenerationAgent) {
        id = "report_generation"
        name = "Report Generation"
        description = "Generate a report based on analysis"
        after("data_analysis")
        variables = mapOf(
            "analysis" to variable("$.steps.data_analysis.output.text")
        )
    }
}
```

## 总结

工作流引擎是 kastrax 框架中的强大组件，它允许开发者定义和执行复杂的多代理工作流。通过声明式的 DSL，开发者可以轻松地组织和协调多个 AI 代理的工作，使它们能够协同完成复杂任务。工作流引擎提供了丰富的功能，包括变量引用、错误处理、进度监控等，使其适用于各种复杂的 AI 应用场景。
