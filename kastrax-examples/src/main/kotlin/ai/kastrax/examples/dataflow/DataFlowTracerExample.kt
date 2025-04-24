package ai.kastrax.examples.dataflow

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.debug.DataFlowTracer
import ai.kastrax.core.workflow.dataflow.debug.NodeType
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 数据流跟踪器示例，展示如何使用数据流跟踪工具。
 */
class DataFlowTracerExample {

    /**
     * 演示数据流跟踪。
     */
    fun demonstrateDataFlowTracing() = runBlocking {
        println("=== 数据流跟踪示例 ===")
        
        // 创建跟踪器
        val tracer = DataFlowTracer()
        
        // 创建示例工作流
        val workflow = createExampleWorkflow()
        
        // 执行工作流
        val input = mapOf("value" to 10, "threshold" to 5)
        val result = workflow.execute(input)
        
        // 创建工作流上下文
        val context = WorkflowContext(
            input = input,
            steps = result.steps
        )
        
        // 1. 跟踪工作流执行
        println("\n1. 跟踪工作流执行")
        val traceResult = tracer.traceWorkflowExecution(workflow, context)
        
        println("步骤跟踪 (${traceResult.stepTraces.size}):")
        traceResult.stepTraces.forEach { trace ->
            println("- 步骤: ${trace.stepId}, 成功: ${trace.success}, 执行时间: ${trace.executionTime}ms")
            if (trace.error != null) {
                println("  错误: ${trace.error}")
            }
        }
        
        println("\n数据跟踪 (${traceResult.dataTraces.size}):")
        traceResult.dataTraces.forEach { trace ->
            println("- 源: ${trace.sourceId} (${trace.sourceType}), 目标: ${trace.targetId ?: "N/A"} (${trace.targetType ?: "N/A"})")
            println("  变量: ${trace.variableName}, 值: ${trace.value}")
        }
        
        // 2. 跟踪特定变量
        println("\n2. 跟踪特定变量")
        val variableTraceResult = tracer.traceVariable(workflow, "value", context)
        
        println("变量 '${variableTraceResult.variableName}' 的跟踪结果:")
        variableTraceResult.traces.forEach { trace ->
            println("- 源: ${trace.sourceId} (${trace.sourceType}), 值: ${trace.value}")
        }
        
        // 3. 跟踪数据流路径
        println("\n3. 跟踪数据流路径")
        val dataFlowPaths = tracer.traceDataFlowPaths(workflow, context)
        
        println("数据流路径:")
        dataFlowPaths.forEach { path ->
            println("- 路径: ${path.sourceId} -> ${path.steps.joinToString(" -> ") { it.id }} -> ${path.targetId}")
            println("  变量: ${path.variableName}, 值: ${path.finalValue}")
        }
        
        // 4. 生成跟踪报告
        println("\n4. 生成跟踪报告")
        val report = tracer.generateTraceReport(traceResult)
        
        println("跟踪报告摘要:")
        println(report.split("\n").take(10).joinToString("\n"))
        println("...")
        
        // 保存报告到文件
        val outputDir = File("examples/dataflow")
        outputDir.mkdirs()
        
        val reportFile = File(outputDir, "trace_report.txt")
        reportFile.writeText(report)
        
        println("\n完整跟踪报告已保存到: ${reportFile.absolutePath}")
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
            workflowName = "DataFlowTraceExample",
            description = "数据流跟踪示例工作流",
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
            val example = DataFlowTracerExample()
            example.demonstrateDataFlowTracing()
        }
    }
}
