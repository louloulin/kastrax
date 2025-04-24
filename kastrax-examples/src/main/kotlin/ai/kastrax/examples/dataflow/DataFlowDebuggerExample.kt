package ai.kastrax.examples.dataflow

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.debug.DataFlowDebugger
import ai.kastrax.core.workflow.dataflow.debug.DataFlowDebugger.DebugMode
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 数据流调试器示例，展示如何使用数据流调试工具。
 */
class DataFlowDebuggerExample {

    /**
     * 演示数据流调试。
     */
    fun demonstrateDataFlowDebugging() = runBlocking {
        println("=== 数据流调试示例 ===")
        
        // 创建调试器
        val debugger = DataFlowDebugger()
        
        // 创建示例工作流
        val workflow = createExampleWorkflow()
        
        // 创建输出目录
        val outputDir = File("examples/dataflow")
        outputDir.mkdirs()
        
        // 1. 使用日志模式调试工作流
        println("\n1. 使用日志模式调试工作流")
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
        
        println("日志模式调试结果: ${logOnlyResult.success}, 输出: ${logOnlyResult.output}")
        
        // 2. 使用报告模式调试工作流
        println("\n2. 使用报告模式调试工作流")
        val reportOptions = DataFlowDebugger.DebugOptions(
            mode = DebugMode.REPORT,
            generateVisualizations = true,
            generateReportAfterStep = true,
            generateHtmlReport = true,
            outputDirectory = outputDir.path
        )
        
        val reportResult = debugger.debugWorkflow(
            workflow = workflow,
            input = mapOf("value" to 3, "threshold" to 5),
            options = reportOptions
        )
        
        println("报告模式调试结果: ${reportResult.success}, 输出: ${reportResult.output}")
        println("调试报告已生成到: ${outputDir.absolutePath}")
        
        // 3. 使用断点调试工作流
        println("\n3. 使用断点调试工作流")
        val breakpointOptions = DataFlowDebugger.DebugOptions(
            mode = DebugMode.LOG_ONLY,
            breakpoints = setOf("condition"),
            generateVisualizations = true,
            generateHtmlReport = false
        )
        
        println("注意：此示例中断点功能仅在日志中显示，不会实际暂停执行")
        println("在实际应用中，可以使用交互式模式 (INTERACTIVE) 进行断点调试")
        
        val breakpointResult = debugger.debugWorkflow(
            workflow = workflow,
            input = mapOf("value" to 7, "threshold" to 5),
            options = breakpointOptions
        )
        
        println("断点调试结果: ${breakpointResult.success}, 输出: ${breakpointResult.output}")
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
            workflowName = "DataFlowDebugExample",
            description = "数据流调试示例工作流",
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
            val example = DataFlowDebuggerExample()
            example.demonstrateDataFlowDebugging()
        }
    }
}
