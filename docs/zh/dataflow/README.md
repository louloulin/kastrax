# 数据流可视化和调试工具

本文档介绍了 Kastrax 工作流引擎中的数据流可视化和调试工具，这些工具可以帮助开发者更好地理解和调试工作流中的数据流。

## 目录

- [数据流可视化器](#数据流可视化器)
- [数据流调试器](#数据流调试器)
- [数据流检查器](#数据流检查器)
- [数据流跟踪器](#数据流跟踪器)
- [综合示例](#综合示例)

## 数据流可视化器

数据流可视化器（DataFlowVisualizer）是一个用于可视化工作流数据流的工具，它可以将工作流的数据流以图形化的方式展示出来，帮助开发者更好地理解工作流中的数据流动。

### 主要功能

- 支持多种可视化格式：DOT、Mermaid、JSON、文本
- 可视化工作流数据流和执行数据流
- 支持保存可视化结果到文件

### 使用示例

```kotlin
// 创建可视化器
val visualizer = DataFlowVisualizer()

// 可视化工作流数据流
val mermaidDiagram = visualizer.visualize(workflow, VisualizationFormat.MERMAID)
println(mermaidDiagram)

// 可视化工作流执行数据流
val executionDiagram = visualizer.visualizeExecution(workflow, context, VisualizationFormat.MERMAID)
println(executionDiagram)

// 保存可视化结果到文件
visualizer.saveToFile(mermaidDiagram, "workflow_diagram.mmd", VisualizationFormat.MERMAID)
```

## 数据流调试器

数据流调试器（DataFlowDebugger）是一个用于调试工作流数据流的工具，它可以帮助开发者在工作流执行过程中跟踪数据流，设置断点，生成调试报告等。

### 主要功能

- 支持多种调试模式：日志、报告、交互式
- 支持断点设置和步骤执行跟踪
- 生成详细的调试报告

### 使用示例

```kotlin
// 创建调试器
val debugger = DataFlowDebugger()

// 使用日志模式调试工作流
val logOnlyOptions = DataFlowDebugger.DebugOptions(
    mode = DebugMode.LOG_ONLY,
    generateVisualizations = true,
    generateReportAfterStep = false,
    generateHtmlReport = false
)

val logOnlyResult = debugger.debugWorkflow(
    workflow = workflow,
    input = mapOf("value" to 10, "threshold" to 5),
    options = logOnlyOptions
)

// 使用报告模式调试工作流
val reportOptions = DataFlowDebugger.DebugOptions(
    mode = DebugMode.REPORT,
    generateVisualizations = true,
    generateReportAfterStep = true,
    generateHtmlReport = true,
    outputDirectory = "debug_reports"
)

val reportResult = debugger.debugWorkflow(
    workflow = workflow,
    input = mapOf("value" to 3, "threshold" to 5),
    options = reportOptions
)

// 使用断点调试工作流
val breakpointOptions = DataFlowDebugger.DebugOptions(
    mode = DebugMode.LOG_ONLY,
    breakpoints = setOf("condition"),
    generateVisualizations = true,
    generateHtmlReport = false
)

val breakpointResult = debugger.debugWorkflow(
    workflow = workflow,
    input = mapOf("value" to 7, "threshold" to 5),
    options = breakpointOptions
)
```

## 数据流检查器

数据流检查器（DataFlowInspector）是一个用于检查工作流数据流的工具，它可以帮助开发者发现工作流中的数据流问题，如循环依赖、缺失依赖、无效引用等。

### 主要功能

- 检查工作流数据流中的问题
- 检查循环依赖、缺失依赖、无效引用等
- 生成检查报告

### 使用示例

```kotlin
// 创建检查器
val inspector = DataFlowInspector()

// 检查工作流
val result = inspector.inspectWorkflow(workflow)

// 打印检查结果
println("发现 ${result.issues.size} 个问题")
result.issues.forEach { issue ->
    println("- ${issue.type}: ${issue.description} (${issue.stepId ?: "全局"})")
}

// 检查工作流执行结果
val executionResult = inspector.inspectWorkflowExecution(workflow, context)

// 打印执行检查结果
println("发现 ${executionResult.issues.size} 个问题")
executionResult.issues.forEach { issue ->
    println("- ${issue.type}: ${issue.description} (${issue.stepId ?: "全局"})")
}
```

## 数据流跟踪器

数据流跟踪器（DataFlowTracer）是一个用于跟踪工作流数据流的工具，它可以帮助开发者跟踪工作流执行过程中的数据流，跟踪变量值的变化等。

### 主要功能

- 跟踪工作流执行过程中的数据流
- 跟踪变量值的变化
- 生成跟踪报告

### 使用示例

```kotlin
// 创建跟踪器
val tracer = DataFlowTracer()

// 跟踪工作流执行
val traceResult = tracer.traceWorkflowExecution(workflow, context)

// 打印步骤跟踪
println("步骤跟踪 (${traceResult.stepTraces.size}):")
traceResult.stepTraces.forEach { trace ->
    println("- 步骤: ${trace.stepId}, 成功: ${trace.success}, 执行时间: ${trace.executionTime}ms")
    if (trace.error != null) {
        println("  错误: ${trace.error}")
    }
}

// 打印数据跟踪
println("数据跟踪 (${traceResult.dataTraces.size}):")
traceResult.dataTraces.forEach { trace ->
    println("- 源: ${trace.sourceId} (${trace.sourceType}), 目标: ${trace.targetId ?: "N/A"} (${trace.targetType ?: "N/A"})")
    println("  变量: ${trace.variableName}, 值: ${trace.value}")
}

// 跟踪特定变量
val variableTraceResult = tracer.traceVariable(workflow, "value", context)

// 打印变量跟踪结果
println("变量 '${variableTraceResult.variableName}' 的跟踪结果:")
variableTraceResult.traces.forEach { trace ->
    println("- 源: ${trace.sourceId} (${trace.sourceType}), 值: ${trace.value}")
}

// 生成跟踪报告
val report = traceResult.generateReport()
println(report)
```

## 综合示例

综合示例（ComprehensiveDataFlowExample）展示了如何使用所有数据流工具来分析和调试工作流。

### 主要功能

- 可视化工作流数据流
- 检查工作流数据流
- 执行工作流
- 可视化工作流执行数据流
- 检查工作流执行结果
- 跟踪工作流执行
- 使用增强的工作流上下文
- 生成综合报告

### 使用示例

```kotlin
// 创建工具实例
val visualizer = DataFlowVisualizer()
val debugger = DataFlowDebugger()
val inspector = DataFlowInspector()
val tracer = DataFlowTracer()

// 创建示例工作流
val workflow = createExampleWorkflow()

// 1. 可视化工作流数据流
val mermaidDiagram = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.MERMAID)
visualizer.saveToFile(mermaidDiagram, "workflow_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)

// 2. 检查工作流数据流
val inspectionResult = inspector.inspectWorkflow(workflow)

// 3. 执行工作流
val input = mapOf("value" to 10, "threshold" to 5)
val result = workflow.execute(input)
val context = WorkflowContext(input = input, steps = result.steps.toMutableMap())

// 4. 可视化工作流执行数据流
val executionDiagram = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.MERMAID)
visualizer.saveToFile(executionDiagram, "execution_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)

// 5. 检查工作流执行结果
val executionInspectionResult = inspector.inspectWorkflowExecution(workflow, context)

// 6. 跟踪工作流执行
val traceResult = tracer.traceWorkflowExecution(workflow, context)

// 7. 使用增强的工作流上下文
val enhancedContext = EnhancedWorkflowContext.fromStandardContext(context, "example-workflow")
val enhancedVisualization = enhancedContext.visualizeExecutionDataFlow(workflow)

// 8. 生成综合报告
val summaryReport = generateSummaryReport(
    workflow = workflow,
    context = context,
    enhancedContext = enhancedContext,
    inspectionResult = inspectionResult,
    executionInspectionResult = executionInspectionResult,
    traceResult = traceResult
)
```

## 总结

数据流可视化和调试工具是 Kastrax 工作流引擎的重要组成部分，它们可以帮助开发者更好地理解和调试工作流中的数据流。通过使用这些工具，开发者可以更轻松地发现和解决工作流中的数据流问题，提高工作流开发效率。
