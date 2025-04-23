package ai.kastrax.core.workflow.dataflow.visualization

import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStatus
import ai.kastrax.core.workflow.WorkflowStatusUpdate
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.visualization.getWorkflowSteps
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DataFlowVisualizerTest {

    @Test
    fun `test visualize workflow`() {
        // 创建测试工作流
        val workflow = createTestWorkflow()

        // 创建可视化器
        val visualizer = DataFlowVisualizer()

        // 测试不同格式的可视化
        val mermaidDiagram = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.MERMAID)
        val dotGraph = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.DOT)
        val jsonRepresentation = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.JSON)
        val textRepresentation = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.TEXT)

        // 验证结果
        assertNotNull(mermaidDiagram)
        assertNotNull(dotGraph)
        assertNotNull(jsonRepresentation)
        assertNotNull(textRepresentation)

        // 验证Mermaid图表包含预期的元素
        assertTrue(mermaidDiagram.contains("```mermaid"))
        assertTrue(mermaidDiagram.contains("graph LR"))
        assertTrue(mermaidDiagram.contains("input((\"Input\"))"))
        assertTrue(mermaidDiagram.contains("step_step1"))
        assertTrue(mermaidDiagram.contains("step_step2"))
        assertTrue(mermaidDiagram.contains("step_step3"))
        assertTrue(mermaidDiagram.contains("output((\"Output\"))"))

        // 验证DOT图包含预期的元素
        assertTrue(dotGraph.contains("digraph DataFlow"))
        assertTrue(dotGraph.contains("rankdir=LR"))
        assertTrue(dotGraph.contains("input [label=\"Input\""))
        assertTrue(dotGraph.contains("step_step1"))
        assertTrue(dotGraph.contains("step_step2"))
        assertTrue(dotGraph.contains("step_step3"))
        assertTrue(dotGraph.contains("output [label=\"Output\""))

        // 验证JSON表示包含预期的元素
        assertTrue(jsonRepresentation.contains("nodes"))
        assertTrue(jsonRepresentation.contains("edges"))
        assertTrue(jsonRepresentation.contains("input"))
        assertTrue(jsonRepresentation.contains("step_step1"))
        assertTrue(jsonRepresentation.contains("step_step2"))
        assertTrue(jsonRepresentation.contains("step_step3"))
        assertTrue(jsonRepresentation.contains("output"))

        // 验证文本表示包含预期的元素
        assertTrue(textRepresentation.contains("=== 工作流数据流 ==="))
        assertTrue(textRepresentation.contains("输入 -> 步骤:"))
        assertTrue(textRepresentation.contains("步骤间数据流:"))
        assertTrue(textRepresentation.contains("步骤 -> 输出:"))
    }

    @Test
    fun `test visualize workflow execution`() {
        // 创建测试工作流
        val workflow = createTestWorkflow()

        // 创建工作流上下文
        val context = createTestWorkflowContext()

        // 创建可视化器
        val visualizer = DataFlowVisualizer()

        // 测试不同格式的可视化
        val mermaidDiagram = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.MERMAID)
        val dotGraph = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.DOT)
        val jsonRepresentation = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.JSON)
        val textRepresentation = visualizer.visualizeExecution(workflow, context, DataFlowVisualizer.VisualizationFormat.TEXT)

        // 验证结果
        assertNotNull(mermaidDiagram)
        assertNotNull(dotGraph)
        assertNotNull(jsonRepresentation)
        assertNotNull(textRepresentation)

        // 验证Mermaid图表包含预期的元素
        assertTrue(mermaidDiagram.contains("```mermaid"))
        assertTrue(mermaidDiagram.contains("graph LR"))
        assertTrue(mermaidDiagram.contains("input((\"Input\"))"))
        assertTrue(mermaidDiagram.contains("step_step1"))
        assertTrue(mermaidDiagram.contains("step_step2"))
        assertTrue(mermaidDiagram.contains("step_step3"))
        assertTrue(mermaidDiagram.contains("output((\"Output\"))"))

        // 验证DOT图包含预期的元素
        assertTrue(dotGraph.contains("digraph DataFlowExecution"))
        assertTrue(dotGraph.contains("rankdir=LR"))
        assertTrue(dotGraph.contains("input [label=\"Input\""))
        assertTrue(dotGraph.contains("step_step1"))
        assertTrue(dotGraph.contains("step_step2"))
        assertTrue(dotGraph.contains("step_step3"))
        assertTrue(dotGraph.contains("output [label=\"Output\""))

        // 验证JSON表示包含预期的元素
        assertTrue(jsonRepresentation.contains("nodes"))
        assertTrue(jsonRepresentation.contains("edges"))
        assertTrue(jsonRepresentation.contains("input"))
        assertTrue(jsonRepresentation.contains("step_step1"))
        assertTrue(jsonRepresentation.contains("step_step2"))
        assertTrue(jsonRepresentation.contains("step_step3"))
        assertTrue(jsonRepresentation.contains("output"))

        // 验证文本表示包含预期的元素
        assertTrue(textRepresentation.contains("=== 工作流执行数据流 ==="))
        assertTrue(textRepresentation.contains("步骤执行:"))
        assertTrue(textRepresentation.contains("数据流:"))
    }

    @Test
    fun `test save visualization to file`() {
        // 创建测试工作流
        val workflow = createTestWorkflow()

        // 创建可视化器
        val visualizer = DataFlowVisualizer()

        // 生成可视化
        val mermaidDiagram = visualizer.visualize(workflow, DataFlowVisualizer.VisualizationFormat.MERMAID)

        // 创建临时文件
        val tempFile = File.createTempFile("dataflow_test", "")
        val filePath = tempFile.absolutePath
        tempFile.delete() // 删除临时文件，让可视化器创建新文件

        try {
            // 保存可视化到文件
            visualizer.saveToFile(mermaidDiagram, filePath, DataFlowVisualizer.VisualizationFormat.MERMAID)

            // 验证文件已创建
            val savedFile = File("$filePath.mmd")
            assertTrue(savedFile.exists())

            // 验证文件内容
            val content = savedFile.readText()
            assertTrue(content.contains("```mermaid"))
            assertTrue(content.contains("graph LR"))

            // 清理
            savedFile.delete()
        } catch (e: Exception) {
            // 确保清理
            File("$filePath.mmd").delete()
            throw e
        }
    }

    /**
     * 创建测试工作流。
     */
    private fun createTestWorkflow(): Workflow {
        // 创建测试步骤
        val step1 = createTestStep("step1", "Step 1", emptyList())
        val step2 = createTestStep("step2", "Step 2", listOf("step1"))
        val step3 = createTestStep("step3", "Step 3", listOf("step2"))

        // 创建测试工作流
        val steps = listOf<WorkflowStep>(step1, step2, step3)

        // 使用mock方式创建工作流
        return object : Workflow {
            // 添加steps字段，使其可以通过反射访问
            val steps: List<WorkflowStep> = steps

            override suspend fun execute(input: Map<String, Any?>, options: WorkflowExecuteOptions): WorkflowResult {
                val stepResults = mutableMapOf<String, WorkflowStepResult>()
                steps.forEach { step ->
                    val result = step.execute(WorkflowContext(input))
                    stepResults[step.id] = result
                }
                return WorkflowResult(success = true, output = mapOf(), steps = stepResults)
            }

            override suspend fun streamExecute(input: Map<String, Any?>, options: WorkflowExecuteOptions): Flow<WorkflowStatusUpdate> = flow {
                emit(WorkflowStatusUpdate(status = WorkflowStatus.STARTED, message = "Starting workflow"))

                val stepResults = mutableMapOf<String, WorkflowStepResult>()
                steps.forEach { step ->
                    emit(WorkflowStatusUpdate(status = WorkflowStatus.IN_PROGRESS, stepId = step.id, message = "Executing step ${step.id}"))
                    val result = step.execute(WorkflowContext(input))
                    stepResults[step.id] = result
                    emit(WorkflowStatusUpdate(status = WorkflowStatus.IN_PROGRESS, stepId = step.id, message = "Step ${step.id} completed", result = result))
                }

                emit(WorkflowStatusUpdate(status = WorkflowStatus.COMPLETED, message = "Workflow completed"))
            }
        }
    }

    /**
     * 创建测试工作流上下文。
     */
    private fun createTestWorkflowContext(): WorkflowContext {
        // 创建输入数据
        val input = mapOf(
            "param1" to "value1",
            "param2" to 42
        )

        // 创建步骤结果
        val steps = mutableMapOf<String, WorkflowStepResult>()

        // 步骤1结果
        steps["step1"] = WorkflowStepResult(
            stepId = "step1",
            success = true,
            output = mapOf("result" to "Step 1 output"),
            executionTime = 100
        )

        // 步骤2结果
        steps["step2"] = WorkflowStepResult(
            stepId = "step2",
            success = true,
            output = mapOf("result" to "Step 2 output"),
            executionTime = 150
        )

        // 步骤3结果
        steps["step3"] = WorkflowStepResult(
            stepId = "step3",
            success = true,
            output = mapOf("result" to "Step 3 output"),
            executionTime = 200
        )

        // 创建工作流上下文
        return WorkflowContext(
            input = input,
            steps = steps
        )
    }

    /**
     * 创建测试步骤。
     */
    private fun createTestStep(
        id: String,
        name: String,
        after: List<String>,
        variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap(),
        config: ai.kastrax.core.workflow.StepConfig? = null,
        description: String = "Test step"
    ): WorkflowStep = object : WorkflowStep, EnhancedVariableReferenceProvider {
        override val id: String = id
        override val name: String = name
        override val after: List<String> = after
        override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = variables
        override val config: ai.kastrax.core.workflow.StepConfig? = config
        override val description: String = description

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf("result" to "$name output"),
                executionTime = 100
            )
        }

        override fun getEnhancedVariableReferences(): List<EnhancedVariableReference> {
            return when (id) {
                "step1" -> listOf(
                    EnhancedVariableReference(SourceType.INPUT, "param1")
                )
                "step2" -> listOf(
                    EnhancedVariableReference(SourceType.STEP, "step1.result")
                )
                "step3" -> listOf(
                    EnhancedVariableReference(SourceType.STEP, "step2.result")
                )
                else -> emptyList()
            }
        }
    }
}
