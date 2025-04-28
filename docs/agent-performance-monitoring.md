# KastraX Agent 性能监控和诊断指南

本文档介绍了如何使用 KastraX Agent 性能监控和诊断工具来监控和诊断 Agent 的性能。

## 概述

KastraX Agent 性能监控和诊断工具提供了一套完整的功能，用于监控 Agent 的性能指标、诊断性能问题、生成性能报告和可视化性能数据。这些工具可以帮助开发者了解 Agent 的性能状况，发现潜在的性能问题，并提供优化建议。

## 核心组件

### AgentPerformanceMonitor

`AgentPerformanceMonitor` 是一个用于监控 Agent 性能的接口，它提供了以下功能：

- 开始和停止监控 Agent
- 获取 Agent 性能指标
- 设置性能阈值
- 发送性能警报

### AgentDiagnosticTool

`AgentDiagnosticTool` 是一个用于诊断 Agent 性能问题的接口，它提供了以下功能：

- 诊断 Agent 性能
- 生成性能优化建议
- 生成详细的诊断报告

### AgentPerformanceReport

`AgentPerformanceReport` 是一个用于生成 Agent 性能报告的类，它提供了以下功能：

- 生成 Markdown 格式的性能报告
- 保存报告到文件

### AgentPerformanceVisualizer

`AgentPerformanceVisualizer` 是一个用于可视化 Agent 性能数据的类，它提供了以下功能：

- 生成 HTML 格式的性能可视化
- 保存可视化到文件

## 使用方法

### 1. 创建性能监控和诊断工具

首先，我们需要创建性能监控和诊断工具的实例：

```kotlin
// 创建指标存储
val metricsStorage = InMemoryAgentMetricsStorage()

// 创建指标收集器
val metricsCollector = AgentMetricsCollector(metricsStorage)

// 创建行为分析器
val behaviorAnalyzer = AgentBehaviorAnalyzer(metricsStorage)

// 创建优化顾问
val optimizationAdvisor = AgentOptimizationAdvisor(behaviorAnalyzer)

// 创建性能监控器
val performanceMonitor = AgentPerformanceMonitorImpl(metricsCollector, metricsStorage)

// 创建诊断工具
val diagnosticTool = AgentDiagnosticToolImpl(
    metricsCollector = metricsCollector,
    metricsStorage = metricsStorage,
    behaviorAnalyzer = behaviorAnalyzer,
    optimizationAdvisor = optimizationAdvisor,
    performanceMonitor = performanceMonitor
)

// 创建性能报告生成器
val performanceReport = AgentPerformanceReport(
    metricsStorage = metricsStorage,
    behaviorAnalyzer = behaviorAnalyzer,
    diagnosticTool = diagnosticTool
)

// 创建性能可视化器
val performanceVisualizer = AgentPerformanceVisualizer(metricsStorage)
```

### 2. 设置性能阈值

我们可以设置性能阈值，当 Agent 的性能指标超过阈值时，会触发性能警报：

```kotlin
performanceMonitor.setPerformanceThresholds(
    AgentPerformanceThresholds(
        maxDurationMs = 30000, // 30 秒
        maxTokens = 2000, // 2000 个 Token
        maxToolCalls = 10, // 10 次工具调用
        maxErrorRate = 0.2, // 20% 错误率
        maxRetryRate = 0.3 // 30% 重试率
    )
)
```

### 3. 监听性能指标和警报

我们可以监听性能指标和警报，以便实时了解 Agent 的性能状况：

```kotlin
// 监听性能指标
val metricsJob = performanceMonitor.getPerformanceMetricsFlow()
    .onEach { metrics ->
        println("收到性能指标: Agent=${metrics.agentId}, 会话=${metrics.sessionId}")
        println("  执行时间: ${metrics.duration}ms")
        println("  步骤数: ${metrics.stepCount}")
        println("  Token 使用量: ${metrics.totalTokens}")
        println("  工具调用次数: ${metrics.toolCalls}")
        println("  错误率: ${String.format("%.2f", metrics.errorRate * 100)}%")
        println("  重试率: ${String.format("%.2f", metrics.retryRate * 100)}%")
    }
    .launchIn(scope)

// 监听性能警报
val alertsJob = performanceMonitor.getPerformanceAlertsFlow()
    .onEach { alert ->
        val levelSymbol = when (alert.level) {
            AlertLevel.INFO -> "ℹ️"
            AlertLevel.WARNING -> "⚠️"
            AlertLevel.ERROR -> "🔴"
        }
        
        val typeSymbol = when (alert.type) {
            AlertType.DURATION -> "⏱️"
            AlertType.TOKEN_USAGE -> "🔤"
            AlertType.TOOL_CALLS -> "🔧"
            AlertType.ERROR_RATE -> "❌"
            AlertType.RETRY_RATE -> "🔄"
        }
        
        println("$levelSymbol $typeSymbol 性能警报: ${alert.message}")
    }
    .launchIn(scope)
```

### 4. 创建 Agent 并添加指标收集器

创建 Agent 时，我们需要添加指标收集器，以便收集 Agent 的性能指标：

```kotlin
val agent = agent {
    name = "计算器助手"
    instructions = """
        你是一个计算器助手，可以帮助用户进行数学计算。
        你可以使用计算器工具进行计算，也可以使用搜索工具查找信息。
        请尽可能详细地解释计算过程和结果。
    """.trimIndent()
    model = openAi("gpt-3.5-turbo")
    tools {
        tool(CalculatorTool())
        tool(SearchTool(SearchToolConfig()))
    }
    
    // 添加指标收集器
    metricsCollector(metricsCollector)
}
```

### 5. 开始监控 Agent

在使用 Agent 之前，我们需要开始监控 Agent：

```kotlin
val sessionId = "example-session"
performanceMonitor.startMonitoring(agent, sessionId)
```

### 6. 使用 Agent 执行任务

现在，我们可以使用 Agent 执行任务，同时监控其性能：

```kotlin
try {
    // 执行一些计算任务
    val response1 = agent.generate("计算 123 + 456 的结果")
    println("Agent 响应: ${response1.text}")
    
    // 执行一些搜索任务
    val response2 = agent.generate("搜索关于人工智能的信息")
    println("Agent 响应: ${response2.text}")
    
    // 执行一些复杂任务
    val response3 = agent.generate("计算 (123 + 456) * 789 的结果，并解释计算过程")
    println("Agent 响应: ${response3.text}")
    
} catch (e: Exception) {
    println("执行任务时发生错误: ${e.message}")
} finally {
    // 停止性能监控
    performanceMonitor.stopMonitoring(agent.id, sessionId)
}
```

### 7. 诊断 Agent 性能

任务完成后，我们可以诊断 Agent 的性能：

```kotlin
val diagnosticResult = diagnosticTool.diagnoseAgentPerformance(agent.id, sessionId)

if (diagnosticResult != null) {
    println("诊断结果:")
    println("  性能问题数量: ${diagnosticResult.performanceIssues.size}")
    diagnosticResult.performanceIssues.forEach { issue ->
        println("  - [${issue.severity}] ${issue.type}: ${issue.description}")
    }
    
    println("  优化建议数量: ${diagnosticResult.optimizationSuggestions.size}")
    diagnosticResult.optimizationSuggestions.forEach { suggestion ->
        println("  - [${suggestion.impact}] ${suggestion.category}: ${suggestion.suggestion}")
    }
}
```

### 8. 生成诊断报告

我们可以生成详细的诊断报告：

```kotlin
val diagnosticReport = diagnosticTool.generateDiagnosticReport(agent.id, sessionId)

if (diagnosticReport != null) {
    // 保存诊断报告
    val reportFile = File("diagnostic_report.md")
    reportFile.writeText(diagnosticReport.detailedReport)
    println("诊断报告已保存到: ${reportFile.absolutePath}")
}
```

### 9. 生成性能报告

我们可以生成 Markdown 格式的性能报告：

```kotlin
val reportFileName = performanceReport.generateReportFileName(agent.id, sessionId)
val reportPath = File(reportFileName).absolutePath
performanceReport.generatePerformanceReport(agent.id, sessionId, outputPath = reportPath)
println("性能报告已保存到: $reportPath")
```

### 10. 生成性能可视化

我们可以生成 HTML 格式的性能可视化：

```kotlin
val visualizationFileName = performanceVisualizer.generateVisualizationFileName(agent.id, sessionId)
val visualizationPath = File(visualizationFileName).absolutePath
performanceVisualizer.generatePerformanceVisualization(agent.id, sessionId, outputPath = visualizationPath)
println("性能可视化已保存到: $visualizationPath")
```

## 性能指标

`AgentPerformanceMetrics` 类包含了以下性能指标：

- `agentId`: Agent ID
- `sessionId`: 会话 ID
- `timestamp`: 时间戳
- `status`: Agent 状态
- `duration`: 执行时间（毫秒）
- `stepCount`: 步骤数
- `averageStepDuration`: 平均步骤时间（毫秒）
- `stepsPerSecond`: 每秒步骤数
- `promptTokens`: 提示词 Token 数
- `completionTokens`: 完成词 Token 数
- `totalTokens`: 总 Token 数
- `tokensPerSecond`: 每秒 Token 数
- `promptTokenRate`: 提示词 Token 比率
- `completionTokenRate`: 完成词 Token 比率
- `toolCalls`: 工具调用次数
- `toolCallsPerSecond`: 每秒工具调用次数
- `errorCount`: 错误次数
- `retryCount`: 重试次数
- `errorRate`: 错误率
- `retryRate`: 重试率

## 性能问题

`PerformanceIssue` 类表示 Agent 的性能问题，包括以下字段：

- `type`: 问题类型
- `severity`: 严重程度
- `description`: 问题描述
- `details`: 详细信息

问题类型包括：

- `LONG_DURATION`: 执行时间过长
- `HIGH_TOKEN_USAGE`: Token 使用量过高
- `EXCESSIVE_TOOL_CALLS`: 过多的工具调用
- `HIGH_ERROR_RATE`: 错误率过高
- `HIGH_RETRY_RATE`: 重试率过高
- `BOTTLENECK_STEP`: 瓶颈步骤
- `HIGH_ERROR_STEP`: 高错误率步骤

严重程度包括：

- `LOW`: 低
- `MEDIUM`: 中
- `HIGH`: 高

## 优化建议

`PerformanceOptimizationSuggestion` 类表示 Agent 的性能优化建议，包括以下字段：

- `category`: 类别
- `suggestion`: 建议
- `impact`: 影响
- `details`: 详细信息

影响包括：

- `LOW`: 低
- `MEDIUM`: 中
- `HIGH`: 高

## 性能报告

性能报告是一个 Markdown 格式的文档，包含以下内容：

- Agent 信息
- 性能摘要
- 性能瓶颈
- 错误分析
- 性能问题
- 优化建议
- 性能图表

## 性能可视化

性能可视化是一个 HTML 格式的文档，包含以下内容：

- Agent 信息
- 性能摘要
- Token 使用分布图
- 步骤执行时间图
- 步骤 Token 使用量图
- 工具调用分布图
- 步骤性能概览图

## 最佳实践

### 1. 设置合适的性能阈值

根据 Agent 的具体需求和使用场景，设置合适的性能阈值，以便及时发现性能问题。

### 2. 监听性能警报

监听性能警报，及时发现和解决性能问题。

### 3. 定期诊断 Agent 性能

定期诊断 Agent 的性能，发现潜在的性能问题，并进行优化。

### 4. 生成性能报告和可视化

生成性能报告和可视化，以便更直观地了解 Agent 的性能状况。

### 5. 根据优化建议进行优化

根据优化建议，对 Agent 进行优化，提高其性能。

## 示例

完整的示例可以在 `examples/src/agent/AgentPerformanceMonitoringExample.kt` 文件中找到。
