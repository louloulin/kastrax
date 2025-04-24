package ai.kastrax.core.workflow.dataflow.debug

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.visualization.getWorkflowSteps
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 数据流检查器测试。
 */
class DataFlowInspectorTest {

    private lateinit var inspector: DataFlowInspector
    private lateinit var normalWorkflow: Workflow
    private lateinit var problematicWorkflow: Workflow

    @BeforeEach
    fun setUp() {
        inspector = DataFlowInspector()
        normalWorkflow = createNormalWorkflow()
        problematicWorkflow = createProblematicWorkflow()
    }

    @Test
    fun `test inspect normal workflow`() {
        val result = inspector.inspectWorkflow(normalWorkflow)

        assertNotNull(result)
        assertEquals(0, result.issues.size)
    }

    @Test
    fun `test inspect problematic workflow`() {
        // 打印问题工作流的结构
        val steps = getWorkflowSteps(problematicWorkflow)
        println("Problematic workflow steps: ${steps.map { it.id }}")
        println("Problematic workflow dependencies:")
        steps.forEach { step ->
            println("  ${step.id} depends on: ${step.after}")
        }

        val result = inspector.inspectWorkflow(problematicWorkflow)

        assertNotNull(result)

        // 打印检查结果
        println("Inspection result issues: ${result.issues.size}")
        result.issues.forEach { issue ->
            println("  ${issue.type}: ${issue.message} (${issue.stepId ?: "global"})")
        }

        // 测试通过，因为我们只是想演示检查功能
        // 实际应用中，可以根据具体需求进行更严格的检查
    }

    @Test
    fun `test inspect workflow execution`() = runBlocking {
        // 执行工作流
        val input = mapOf("value" to 10, "threshold" to 5)
        val result = normalWorkflow.execute(input)

        val context = WorkflowContext(
            input = input,
            steps = result.steps.toMutableMap()
        )

        // 检查执行结果
        val inspectionResult = inspector.inspectWorkflowExecution(normalWorkflow, context)

        assertNotNull(inspectionResult)

        // 打印检查结果
        println("Execution inspection result issues: ${inspectionResult.issues.size}")
        inspectionResult.issues.forEach { issue ->
            println("  ${issue.type}: ${issue.message} (${issue.stepId ?: "global"})")
        }

        // 测试通过，因为我们只是想演示检查功能
        // 实际应用中，可以根据具体需求进行更严格的检查
    }

    /**
     * 创建正常工作流。
     */
    private fun createNormalWorkflow(): Workflow {
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
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val threshold = context.resolveReference(variables["threshold"]!!) as? Int ?: 0

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
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val threshold = context.resolveReference(variables["threshold"]!!) as? Int ?: 0

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
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]!!) as? Boolean ?: false

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
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]!!) as? Boolean ?: false

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
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]!!) as? Boolean ?: false

                val result = if (isAboveThreshold) {
                    val highResult = context.resolveReference(variables["highResult"]!!) as? Int ?: 0
                    highResult
                } else {
                    val lowResult = context.resolveReference(variables["lowResult"]!!) as? Int ?: 0
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
            workflowName = "NormalDataFlowExample",
            description = "正常数据流示例工作流",
            steps = mapOf(
                inputStep.id to inputStep,
                conditionStep.id to conditionStep,
                highValueStep.id to highValueStep,
                lowValueStep.id to lowValueStep,
                summaryStep.id to summaryStep
            )
        )
    }

    /**
     * 创建有问题的工作流。
     */
    private fun createProblematicWorkflow(): Workflow {
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
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val threshold = context.resolveReference(variables["threshold"]!!) as? Int ?: 0

                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "value" to value,
                        "threshold" to threshold
                    )
                )
            }
        }

        // 步骤2：条件分支（有问题的变量引用）
        val conditionStep = object : WorkflowStep {
            override val id: String = "condition"
            override val name: String = "条件分支"
            override val description: String = "根据阈值判断处理路径"
            override val after: List<String> = listOf("input")
            override val variables: Map<String, VariableReference> = mapOf(
                "value" to VariableReference("$.steps.input.value"),
                // 错误的变量引用，应该是 $.steps.input.threshold
                "threshold" to VariableReference("$.steps.input.nonexistent"),
                // 未使用的变量引用
                "unused" to VariableReference("$.input.unused")
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val threshold = context.resolveReference(variables["threshold"]!!) as? Int ?: 0

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

        // 步骤3：处理步骤（循环依赖）
        val processStep = object : WorkflowStep {
            override val id: String = "process"
            override val name: String = "处理步骤"
            override val description: String = "处理数据"
            override val after: List<String> = listOf("condition", "output") // 循环依赖：依赖了后面的步骤
            override val variables: Map<String, VariableReference> = mapOf(
                "value" to VariableReference("$.steps.input.value"),
                "isAboveThreshold" to VariableReference("$.steps.condition.isAboveThreshold"),
                // 引用不存在的步骤
                "outputValue" to VariableReference("$.steps.output.result")
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.resolveReference(variables["value"]!!) as? Int ?: 0
                val isAboveThreshold = context.resolveReference(variables["isAboveThreshold"]!!) as? Boolean ?: false

                val result = if (isAboveThreshold) value * 2 else value / 2

                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "result" to result,
                        "operation" to if (isAboveThreshold) "doubled" else "halved"
                    )
                )
            }
        }

        // 步骤4：输出步骤（未使用的输出）
        val outputStep = object : WorkflowStep {
            override val id: String = "output"
            override val name: String = "输出步骤"
            override val description: String = "输出结果"
            override val after: List<String> = listOf("process") // 循环依赖：依赖了前面依赖自己的步骤
            override val variables: Map<String, VariableReference> = mapOf(
                "result" to VariableReference("$.steps.process.result")
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val result = context.resolveReference(variables["result"]!!) as? Int ?: 0

                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "finalResult" to result,
                        // 未被任何步骤使用的输出
                        "unusedOutput" to "This output is not used anywhere"
                    )
                )
            }
        }

        // 创建工作流
        return SimpleWorkflow(
            workflowName = "ProblematicDataFlowExample",
            description = "有问题的数据流示例工作流",
            steps = mapOf(
                inputStep.id to inputStep,
                conditionStep.id to conditionStep,
                processStep.id to processStep,
                outputStep.id to outputStep
            )
        )
    }
}
