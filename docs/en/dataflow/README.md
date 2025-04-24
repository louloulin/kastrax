# Data Flow Visualization and Debugging Tools

This document introduces the data flow visualization and debugging tools in the Kastrax workflow engine, which help developers better understand and debug data flow in workflows.

## Table of Contents

- [Data Flow Visualizer](#data-flow-visualizer)
- [Data Flow Debugger](#data-flow-debugger)
- [Data Flow Inspector](#data-flow-inspector)
- [Data Flow Tracer](#data-flow-tracer)
- [Comprehensive Example](#comprehensive-example)

## Data Flow Visualizer

The Data Flow Visualizer (DataFlowVisualizer) is a tool for visualizing workflow data flow. It can display the data flow of a workflow in a graphical way, helping developers better understand the data flow in the workflow.

### Main Features

- Supports multiple visualization formats: DOT, Mermaid, JSON, Text
- Visualizes workflow data flow and execution data flow
- Supports saving visualization results to files

### Usage Example

```kotlin
// Create a visualizer
val visualizer = DataFlowVisualizer()

// Visualize workflow data flow
val mermaidDiagram = visualizer.visualize(workflow, VisualizationFormat.MERMAID)
println(mermaidDiagram)

// Visualize workflow execution data flow
val executionDiagram = visualizer.visualizeExecution(workflow, context, VisualizationFormat.MERMAID)
println(executionDiagram)

// Save visualization results to files
visualizer.saveToFile(mermaidDiagram, "workflow_diagram.mmd", VisualizationFormat.MERMAID)
```

## Data Flow Debugger

The Data Flow Debugger (DataFlowDebugger) is a tool for debugging workflow data flow. It helps developers track data flow during workflow execution, set breakpoints, generate debug reports, etc.

### Main Features

- Supports multiple debug modes: LOG_ONLY, REPORT, INTERACTIVE
- Supports setting breakpoints and step execution tracking
- Generates detailed debug reports

### Usage Example

```kotlin
// Create a debugger
val debugger = DataFlowDebugger()

// Debug workflow with LOG_ONLY mode
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

// Debug workflow with REPORT mode
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

// Debug workflow with breakpoints
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

## Data Flow Inspector

The Data Flow Inspector (DataFlowInspector) is a tool for inspecting workflow data flow. It helps developers discover data flow issues in workflows, such as circular dependencies, missing dependencies, invalid references, etc.

### Main Features

- Inspects issues in workflow data flow
- Checks for circular dependencies, missing dependencies, invalid references, etc.
- Generates inspection reports

### Usage Example

```kotlin
// Create an inspector
val inspector = DataFlowInspector()

// Inspect workflow
val result = inspector.inspectWorkflow(workflow)

// Print inspection results
println("Found ${result.issues.size} issues")
result.issues.forEach { issue ->
    println("- ${issue.type}: ${issue.description} (${issue.stepId ?: "global"})")
}

// Inspect workflow execution results
val executionResult = inspector.inspectWorkflowExecution(workflow, context)

// Print execution inspection results
println("Found ${executionResult.issues.size} issues")
executionResult.issues.forEach { issue ->
    println("- ${issue.type}: ${issue.description} (${issue.stepId ?: "global"})")
}
```

## Data Flow Tracer

The Data Flow Tracer (DataFlowTracer) is a tool for tracing workflow data flow. It helps developers trace data flow during workflow execution, track variable value changes, etc.

### Main Features

- Traces data flow during workflow execution
- Tracks variable value changes
- Generates trace reports

### Usage Example

```kotlin
// Create a tracer
val tracer = DataFlowTracer()

// Trace workflow execution
val traceResult = tracer.traceWorkflowExecution(workflow, context)

// Print step traces
println("Step traces (${traceResult.stepTraces.size}):")
traceResult.stepTraces.forEach { trace ->
    println("- Step: ${trace.stepId}, Success: ${trace.success}, Execution time: ${trace.executionTime}ms")
    if (trace.error != null) {
        println("  Error: ${trace.error}")
    }
}

// Print data traces
println("Data traces (${traceResult.dataTraces.size}):")
traceResult.dataTraces.forEach { trace ->
    println("- Source: ${trace.sourceId} (${trace.sourceType}), Target: ${trace.targetId ?: "N/A"} (${trace.targetType ?: "N/A"})")
    println("  Variable: ${trace.variableName}, Value: ${trace.value}")
}

// Trace specific variable
val variableTraceResult = tracer.traceVariable(workflow, "value", context)

// Print variable trace results
println("Trace results for variable '${variableTraceResult.variableName}':")
variableTraceResult.traces.forEach { trace ->
    println("- Source: ${trace.sourceId} (${trace.sourceType}), Value: ${trace.value}")
}

// Generate trace report
val report = traceResult.generateReport()
println(report)
```

## Comprehensive Example

The Comprehensive Example (ComprehensiveDataFlowExample) demonstrates how to use all data flow tools to analyze and debug workflows.

### Main Features

- Visualizes workflow data flow
- Inspects workflow data flow
- Executes workflow
- Visualizes workflow execution data flow
- Inspects workflow execution results
- Traces workflow execution
- Uses enhanced workflow context
- Generates comprehensive reports

### Usage Example

```kotlin
// Create tool instances
val visualizer = DataFlowVisualizer()
val debugger = DataFlowDebugger()
val inspector = DataFlowInspector()
val tracer = DataFlowTracer()

// Create example workflow
val workflow = createExampleWorkflow()

// 1. Visualize workflow data flow
val mermaidDiagram = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.MERMAID)
visualizer.saveToFile(mermaidDiagram, "workflow_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)

// 2. Inspect workflow data flow
val inspectionResult = inspector.inspectWorkflow(workflow)

// 3. Execute workflow
val input = mapOf("value" to 10, "threshold" to 5)
val result = workflow.execute(input)
val context = WorkflowContext(input = input, steps = result.steps.toMutableMap())

// 4. Visualize workflow execution data flow
val executionDiagram = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.MERMAID)
visualizer.saveToFile(executionDiagram, "execution_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)

// 5. Inspect workflow execution results
val executionInspectionResult = inspector.inspectWorkflowExecution(workflow, context)

// 6. Trace workflow execution
val traceResult = tracer.traceWorkflowExecution(workflow, context)

// 7. Use enhanced workflow context
val enhancedContext = EnhancedWorkflowContext.fromStandardContext(context, "example-workflow")
val enhancedVisualization = enhancedContext.visualizeExecutionDataFlow(workflow)

// 8. Generate comprehensive report
val summaryReport = generateSummaryReport(
    workflow = workflow,
    context = context,
    enhancedContext = enhancedContext,
    inspectionResult = inspectionResult,
    executionInspectionResult = executionInspectionResult,
    traceResult = traceResult
)
```

## Summary

Data flow visualization and debugging tools are an important part of the Kastrax workflow engine. They help developers better understand and debug data flow in workflows. By using these tools, developers can more easily discover and solve data flow issues in workflows, improving workflow development efficiency.
