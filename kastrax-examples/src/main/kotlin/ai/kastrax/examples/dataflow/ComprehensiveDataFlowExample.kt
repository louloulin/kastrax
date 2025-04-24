package ai.kastrax.examples.dataflow

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.EnhancedWorkflowContext
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.TransformType
import ai.kastrax.core.workflow.dataflow.VariableScopeManager
import ai.kastrax.core.workflow.dataflow.debug.DataFlowDebugger
import ai.kastrax.core.workflow.dataflow.debug.DataFlowInspector
import ai.kastrax.core.workflow.dataflow.debug.DataFlowTracer
import ai.kastrax.core.workflow.dataflow.visualization.DataFlowVisualizer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 综合数据流示例，展示如何使用所有数据流工具。
 */
class ComprehensiveDataFlowExample {

    /**
     * 演示综合数据流功能。
     */
    fun demonstrateComprehensiveDataFlow() = runBlocking {
        println("=== 综合数据流示例 ===")
        
        // 创建工具实例
        val visualizer = DataFlowVisualizer()
        val debugger = DataFlowDebugger()
        val inspector = DataFlowInspector()
        val tracer = DataFlowTracer()
        
        // 创建示例工作流
        val workflow = createExampleWorkflow()
        
        // 创建输出目录
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val outputDir = File("examples/dataflow/comprehensive_$timestamp")
        outputDir.mkdirs()
        
        println("\n输出目录: ${outputDir.absolutePath}")
        
        // 1. 可视化工作流数据流
        println("\n1. 可视化工作流数据流")
        val mermaidDiagram = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.MERMAID)
        visualizer.saveToFile(mermaidDiagram, "${outputDir.path}/workflow_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)
        
        // 2. 检查工作流数据流
        println("\n2. 检查工作流数据流")
        val inspectionResult = inspector.inspectWorkflow(workflow)
        
        println("检查结果:")
        println("发现 ${inspectionResult.issues.size} 个问题")
        inspectionResult.issues.forEach { issue ->
            println("- ${issue.type}: ${issue.description} (${issue.stepId ?: "全局"})")
        }
        
        // 保存检查报告
        val inspectionReport = inspectionResult.generateReport()
        File(outputDir, "inspection_report.txt").writeText(inspectionReport)
        
        // 3. 执行工作流
        println("\n3. 执行工作流")
        val input = mapOf("value" to 10, "threshold" to 5)
        
        // 使用调试器执行工作流
        val debugOptions = DataFlowDebugger.DebugOptions(
            mode = DataFlowDebugger.DebugMode.REPORT,
            generateVisualizations = true,
            generateHtmlReport = true,
            outputDirectory = outputDir.path
        )
        
        val result = debugger.debugWorkflow(workflow, input, debugOptions)
        
        println("工作流执行结果: ${result.success}, 输出: ${result.output}")
        
        // 创建工作流上下文
        val context = WorkflowContext(
            input = input,
            steps = result.steps
        )
        
        // 4. 可视化工作流执行数据流
        println("\n4. 可视化工作流执行数据流")
        val executionDiagram = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.MERMAID)
        visualizer.saveToFile(executionDiagram, "${outputDir.path}/execution_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)
        
        // 5. 检查工作流执行结果
        println("\n5. 检查工作流执行结果")
        val executionInspectionResult = inspector.inspectWorkflowExecution(workflow, context)
        
        println("执行检查结果:")
        println("发现 ${executionInspectionResult.issues.size} 个问题")
        executionInspectionResult.issues.forEach { issue ->
            println("- ${issue.type}: ${issue.description} (${issue.stepId ?: "全局"})")
        }
        
        // 保存执行检查报告
        val executionInspectionReport = executionInspectionResult.generateReport()
        File(outputDir, "execution_inspection_report.txt").writeText(executionInspectionReport)
        
        // 6. 跟踪工作流执行
        println("\n6. 跟踪工作流执行")
        val traceResult = tracer.traceWorkflowExecution(workflow, context)
        
        println("步骤跟踪 (${traceResult.stepTraces.size}):")
        traceResult.stepTraces.forEach { trace ->
            println("- 步骤: ${trace.stepId}, 成功: ${trace.success}, 执行时间: ${trace.executionTime}ms")
        }
        
        // 保存跟踪报告
        val traceReport = tracer.generateTraceReport(traceResult)
        File(outputDir, "trace_report.txt").writeText(traceReport)
        
        // 7. 使用增强的工作流上下文
        println("\n7. 使用增强的工作流上下文")
        val scopeManager = VariableScopeManager()
        val enhancedContext = EnhancedWorkflowContext.fromStandardContext(context, "example-workflow", scopeManager)
        
        // 设置全局变量
        val globalScope = scopeManager.getGlobalScope()
        globalScope.set("globalValue", 100)
        
        // 设置工作流变量
        val workflowScope = scopeManager.getWorkflowScope("example-workflow")
        workflowScope.set("workflowValue", 200)
        
        // 设置步骤变量
        val stepScope = scopeManager.getStepScope("example-workflow", "input")
        stepScope.set("stepValue", 300)
        
        // 使用增强的上下文可视化数据流
        val enhancedVisualization = enhancedContext.visualizeExecutionDataFlow(workflow)
        visualizer.saveToFile(enhancedVisualization, "${outputDir.path}/enhanced_diagram.mmd", DataFlowVisualizer.VisualizationFormat.MERMAID)
        
        // 8. 生成综合报告
        println("\n8. 生成综合报告")
        val summaryReport = generateSummaryReport(
            workflow = workflow,
            context = context,
            enhancedContext = enhancedContext,
            inspectionResult = inspectionResult,
            executionInspectionResult = executionInspectionResult,
            traceResult = traceResult
        )
        
        File(outputDir, "summary_report.html").writeText(summaryReport)
        
        println("\n所有输出文件已保存到: ${outputDir.absolutePath}")
    }
    
    /**
     * 生成综合报告。
     */
    private fun generateSummaryReport(
        workflow: Workflow,
        context: WorkflowContext,
        enhancedContext: EnhancedWorkflowContext,
        inspectionResult: ai.kastrax.core.workflow.dataflow.debug.InspectionResult,
        executionInspectionResult: ai.kastrax.core.workflow.dataflow.debug.ExecutionInspectionResult,
        traceResult: ai.kastrax.core.workflow.dataflow.debug.TraceResult
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html>")
        sb.appendLine("<head>")
        sb.appendLine("  <title>数据流综合报告</title>")
        sb.appendLine("  <style>")
        sb.appendLine("    body { font-family: Arial, sans-serif; margin: 20px; }")
        sb.appendLine("    h1, h2, h3 { color: #333; }")
        sb.appendLine("    .section { margin-bottom: 30px; }")
        sb.appendLine("    .issue { margin: 10px 0; padding: 10px; border-left: 4px solid #f44336; background-color: #ffebee; }")
        sb.appendLine("    .trace { margin: 10px 0; padding: 10px; border-left: 4px solid #2196f3; background-color: #e3f2fd; }")
        sb.appendLine("    .step { margin: 10px 0; padding: 10px; border-left: 4px solid #4caf50; background-color: #e8f5e9; }")
        sb.appendLine("    table { border-collapse: collapse; width: 100%; }")
        sb.appendLine("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
        sb.appendLine("    th { background-color: #f2f2f2; }")
        sb.appendLine("    .mermaid { margin: 20px 0; }")
        sb.appendLine("  </style>")
        sb.appendLine("  <script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>")
        sb.appendLine("  <script>mermaid.initialize({startOnLoad:true});</script>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")
        
        // 标题
        sb.appendLine("  <h1>数据流综合报告</h1>")
        
        // 工作流信息
        sb.appendLine("  <div class=\"section\">")
        sb.appendLine("    <h2>工作流信息</h2>")
        sb.appendLine("    <p><strong>名称:</strong> ${workflow.javaClass.simpleName}</p>")
        sb.appendLine("    <p><strong>输入:</strong> ${context.input}</p>")
        sb.appendLine("    <p><strong>输出:</strong> ${context.steps["summary"]?.output ?: "无"}</p>")
        sb.appendLine("  </div>")
        
        // 工作流可视化
        sb.appendLine("  <div class=\"section\">")
        sb.appendLine("    <h2>工作流可视化</h2>")
        sb.appendLine("    <div class=\"mermaid\">")
        
        val visualizer = DataFlowVisualizer()
        val mermaidDiagram = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.MERMAID)
        val mermaidContent = mermaidDiagram.lines()
            .filter { !it.startsWith("```") }
            .joinToString("\n")
        
        sb.appendLine(mermaidContent)
        sb.appendLine("    </div>")
        sb.appendLine("  </div>")
        
        // 执行可视化
        sb.appendLine("  <div class=\"section\">")
        sb.appendLine("    <h2>执行可视化</h2>")
        sb.appendLine("    <div class=\"mermaid\">")
        
        val executionDiagram = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.MERMAID)
        val executionContent = executionDiagram.lines()
            .filter { !it.startsWith("```") }
            .joinToString("\n")
        
        sb.appendLine(executionContent)
        sb.appendLine("    </div>")
        sb.appendLine("  </div>")
        
        // 检查结果
        sb.appendLine("  <div class=\"section\">")
        sb.appendLine("    <h2>检查结果</h2>")
        
        if (inspectionResult.issues.isEmpty()) {
            sb.appendLine("    <p>未发现静态检查问题。</p>")
        } else {
            sb.appendLine("    <h3>静态检查问题</h3>")
            inspectionResult.issues.forEach { issue ->
                sb.appendLine("    <div class=\"issue\">")
                sb.appendLine("      <p><strong>${issue.type}:</strong> ${issue.description}</p>")
                sb.appendLine("      <p><strong>步骤:</strong> ${issue.stepId ?: "全局"}</p>")
                sb.appendLine("    </div>")
            }
        }
        
        if (executionInspectionResult.issues.isEmpty()) {
            sb.appendLine("    <p>未发现执行检查问题。</p>")
        } else {
            sb.appendLine("    <h3>执行检查问题</h3>")
            executionInspectionResult.issues.forEach { issue ->
                sb.appendLine("    <div class=\"issue\">")
                sb.appendLine("      <p><strong>${issue.type}:</strong> ${issue.description}</p>")
                sb.appendLine("      <p><strong>步骤:</strong> ${issue.stepId ?: "全局"}</p>")
                sb.appendLine("    </div>")
            }
        }
        
        sb.appendLine("  </div>")
        
        // 跟踪结果
        sb.appendLine("  <div class=\"section\">")
        sb.appendLine("    <h2>跟踪结果</h2>")
        
        sb.appendLine("    <h3>步骤跟踪</h3>")
        sb.appendLine("    <table>")
        sb.appendLine("      <tr><th>步骤ID</th><th>成功</th><th>执行时间</th><th>错误</th></tr>")
        
        traceResult.stepTraces.forEach { trace ->
            sb.appendLine("      <tr>")
            sb.appendLine("        <td>${trace.stepId}</td>")
            sb.appendLine("        <td>${trace.success}</td>")
            sb.appendLine("        <td>${trace.executionTime}ms</td>")
            sb.appendLine("        <td>${trace.error ?: "-"}</td>")
            sb.appendLine("      </tr>")
        }
        
        sb.appendLine("    </table>")
        
        sb.appendLine("    <h3>数据跟踪</h3>")
        sb.appendLine("    <table>")
        sb.appendLine("      <tr><th>源</th><th>目标</th><th>变量</th><th>值</th></tr>")
        
        traceResult.dataTraces.take(10).forEach { trace ->
            sb.appendLine("      <tr>")
            sb.appendLine("        <td>${trace.sourceId} (${trace.sourceType})</td>")
            sb.appendLine("        <td>${trace.targetId ?: "N/A"} (${trace.targetType ?: "N/A"})</td>")
            sb.appendLine("        <td>${trace.variableName}</td>")
            sb.appendLine("        <td>${trace.value}</td>")
            sb.appendLine("      </tr>")
        }
        
        if (traceResult.dataTraces.size > 10) {
            sb.appendLine("      <tr><td colspan=\"4\">... 还有 ${traceResult.dataTraces.size - 10} 条记录 ...</td></tr>")
        }
        
        sb.appendLine("    </table>")
        sb.appendLine("  </div>")
        
        // 步骤详情
        sb.appendLine("  <div class=\"section\">")
        sb.appendLine("    <h2>步骤详情</h2>")
        
        context.steps.forEach { (stepId, stepResult) ->
            sb.appendLine("    <div class=\"step\">")
            sb.appendLine("      <h3>步骤: $stepId</h3>")
            sb.appendLine("      <p><strong>状态:</strong> ${if (stepResult.success) "成功" else "失败"}</p>")
            if (stepResult.skipped) {
                sb.appendLine("      <p><strong>已跳过</strong></p>")
            } else {
                sb.appendLine("      <p><strong>执行时间:</strong> ${stepResult.executionTime}ms</p>")
                sb.appendLine("      <p><strong>输出:</strong></p>")
                sb.appendLine("      <pre>${stepResult.output}</pre>")
                if (stepResult.error != null) {
                    sb.appendLine("      <p><strong>错误:</strong> ${stepResult.error}</p>")
                }
            }
            sb.appendLine("    </div>")
        }
        
        sb.appendLine("  </div>")
        
        sb.appendLine("</body>")
        sb.appendLine("</html>")
        
        return sb.toString()
    }
    
    /**
     * 创建示例工作流。
     */
    private fun createExampleWorkflow(): Workflow {
        // 步骤1：数据输入
        val inputStep = object : WorkflowStep {
            override val id: String = "input"
            override val name: String = "数据输入"
            override val description: String = "接收输入数据"
            override val after: List<String> = emptyList()
            override val variables: Map<String, VariableReference> = mapOf(
                "value" to VariableReference("$.input.value"),
                "threshold" to VariableReference("$.input.threshold")
            )
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.resolveReference(variables["value"]) as? Int ?: 0
                val threshold = context.resolveReference(variables["threshold"]) as? Int ?: 0
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "value" to value,
                        "threshold" to threshold
                    )
                )
            }
        }
        
        // 步骤2：条件分支
        val conditionStep = object : WorkflowStep {
            override val id: String = "condition"
            override val name: String = "条件分支"
            override val description: String = "根据阈值判断处理路径"
            override val after: List<String> = listOf("input")
            override val variables: Map<String, VariableReference> = mapOf(
                "value" to VariableReference("$.steps.input.value"),
                "threshold" to VariableReference("$.steps.input.threshold")
            )
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.resolveReference(variables["value"]) as? Int ?: 0
                val threshold = context.resolveReference(variables["threshold"]) as? Int ?: 0
                
                val isAboveThreshold = value > threshold
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "isAboveThreshold" to isAboveThreshold,
                        "value" to value,
                        "threshold" to threshold
                    )
                )
            }
        }
        
        // 步骤3A：高值处理
        val highValueStep = object : WorkflowStep {
            override val id: String = "highValue"
            override val name: String = "高值处理"
            override val description: String = "处理高于阈值的数据"
            override val after: List<String> = listOf("condition")
            override val variables: Map<String, VariableReference> = mapOf(
                "value" to VariableReference("$.steps.input.value"),
                "isAboveThreshold" to VariableReference("$.steps.condition.isAboveThreshold")
            )
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.resolveReference(variables["value"]) as? Int ?: 0
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]) as? Boolean ?: false
                
                // 如果不满足条件，跳过此步骤
                if (!isAboveThreshold) {
                    return WorkflowStepResult.skipped(id)
                }
                
                val result = value * 2
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "result" to result,
                        "operation" to "doubled"
                    )
                )
            }
        }
        
        // 步骤3B：低值处理
        val lowValueStep = object : WorkflowStep {
            override val id: String = "lowValue"
            override val name: String = "低值处理"
            override val description: String = "处理低于或等于阈值的数据"
            override val after: List<String> = listOf("condition")
            override val variables: Map<String, VariableReference> = mapOf(
                "value" to VariableReference("$.steps.input.value"),
                "isAboveThreshold" to VariableReference("$.steps.condition.isAboveThreshold")
            )
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.resolveReference(variables["value"]) as? Int ?: 0
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]) as? Boolean ?: false
                
                // 如果不满足条件，跳过此步骤
                if (isAboveThreshold) {
                    return WorkflowStepResult.skipped(id)
                }
                
                val result = value / 2
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "result" to result,
                        "operation" to "halved"
                    )
                )
            }
        }
        
        // 步骤4：结果汇总
        val summaryStep = object : WorkflowStep {
            override val id: String = "summary"
            override val name: String = "结果汇总"
            override val description: String = "汇总处理结果"
            override val after: List<String> = listOf("highValue", "lowValue")
            override val variables: Map<String, VariableReference> = mapOf(
                "highResult" to VariableReference("$.steps.highValue.result"),
                "lowResult" to VariableReference("$.steps.lowValue.result"),
                "isAboveThreshold" to VariableReference("$.steps.condition.isAboveThreshold")
            )
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]) as? Boolean ?: false
                
                val result = if (isAboveThreshold) {
                    val highResult = context.resolveReference(variables["highResult"]) as? Int ?: 0
                    highResult
                } else {
                    val lowResult = context.resolveReference(variables["lowResult"]) as? Int ?: 0
                    lowResult
                }
                
                val operation = if (isAboveThreshold) "doubled" else "halved"
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "finalResult" to result,
                        "operation" to operation,
                        "isAboveThreshold" to isAboveThreshold
                    )
                )
            }
        }
        
        // 创建工作流
        return SimpleWorkflow(
            workflowName = "ComprehensiveDataFlowExample",
            description = "综合数据流示例工作流",
            steps = mapOf(
                inputStep.id to inputStep,
                conditionStep.id to conditionStep,
                highValueStep.id to highValueStep,
                lowValueStep.id to lowValueStep,
                summaryStep.id to summaryStep
            )
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val example = ComprehensiveDataFlowExample()
            example.demonstrateComprehensiveDataFlow()
        }
    }
}
