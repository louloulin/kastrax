package ai.kastrax.core.workflow.dataflow.debug

import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStatus
import ai.kastrax.core.workflow.WorkflowStatusUpdate
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.engine.WorkflowEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.visualization.EnhancedVariableReferenceProvider
import ai.kastrax.core.workflow.dataflow.visualization.getWorkflowSteps
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DataFlowDebuggerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test debug workflow execution`() = runBlocking {
        // 创建测试工作流
        val workflow = createTestWorkflow()

        // 创建输入数据
        val input = mapOf(
            "param1" to "value1",
            "param2" to 42
        )

        // 创建工作流引擎
        val engine = createTestWorkflowEngine()

        // 创建调试器
        val debugger = DataFlowDebugger()

        // 调试工作流
        val outputDir = tempDir.resolve("debug_output").toString()
        val options = DataFlowDebugger.DebugOptions(
            mode = DataFlowDebugger.DebugMode.REPORT,
            outputDir = outputDir,
            generateHtmlReport = true,
            generateVisualizations = true
        )
        val result = debugger.debugWorkflow(
            workflow = workflow,
            input = input,
            options = options
        )

        // 验证结果
        assertNotNull(result)
        assertEquals(3, result.steps.size)
        assertTrue(result.steps.all { it.value.success })

        // 验证调试输出目录
        val outputDirFile = File(outputDir)
        assertTrue(outputDirFile.exists())
        assertTrue(outputDirFile.isDirectory)

        // 验证调试会话目录
        val sessionDirs = outputDirFile.listFiles { file -> file.isDirectory && file.name.startsWith("debug_session_") }
        assertNotNull(sessionDirs)
        assertTrue(sessionDirs!!.isNotEmpty())

        // 验证调试报告文件
        val sessionDir = sessionDirs.first()
        assertTrue(File(sessionDir, "summary.txt").exists())
        assertTrue(File(sessionDir, "detail.txt").exists())
        assertTrue(File(sessionDir, "variables.txt").exists())
        assertTrue(File(sessionDir, "data_flow.txt").exists())
    }

    @Test
    fun `test debug workflow with breakpoints`() = runBlocking {
        // 创建测试工作流
        val workflow = createTestWorkflow()

        // 创建输入数据
        val input = mapOf(
            "param1" to "value1",
            "param2" to 42
        )

        // 创建工作流引擎
        val engine = createTestWorkflowEngine()

        // 创建调试器
        val debugger = DataFlowDebugger()

        // 调试工作流（使用LOG_ONLY模式，避免交互式断点）
        val outputDir = tempDir.resolve("debug_output_breakpoints").toString()
        val options = DataFlowDebugger.DebugOptions(
            mode = DataFlowDebugger.DebugMode.LOG_ONLY,
            breakpoints = listOf("step2"),
            outputDir = outputDir,
            showVariablesAfterStep = true,
            showDataFlowAfterStep = true
        )
        val result = debugger.debugWorkflow(
            workflow = workflow,
            input = input,
            options = options
        )

        // 验证结果
        assertNotNull(result)
        assertEquals(3, result.steps.size)
        assertTrue(result.steps.all { it.value.success })
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

    /**
     * 创建测试工作流引擎。
     */
    private fun createTestWorkflowEngine(): WorkflowEngine {
        // 使用组合而不是继承，因为WorkflowEngine是一个最终类
        return WorkflowEngine(mapOf<String, Workflow>())
    }

    /**
     * 测试执行工作流。
     */
    private suspend fun executeWorkflow(workflow: Workflow, input: Map<String, Any?>, options: WorkflowExecuteOptions = WorkflowExecuteOptions()): WorkflowResult {
        val steps = mutableMapOf<String, WorkflowStepResult>()

        // 按照依赖顺序执行步骤
        val executedSteps = mutableSetOf<String>()

        // 找出没有依赖的步骤
        val workflowSteps = getWorkflowSteps(workflow)
        val noDepSteps = workflowSteps.filter { step -> step.after.isEmpty() }

        // 执行没有依赖的步骤
        for (step in noDepSteps) {
            val result = step.execute(WorkflowContext(input, steps))
            steps[step.id] = result
            executedSteps.add(step.id)
        }

        // 执行剩余步骤
        while (executedSteps.size < workflowSteps.size) {
            val readySteps = workflowSteps.filter { step ->
                step.id !in executedSteps && step.after.all { depId -> depId in executedSteps }
            }

            if (readySteps.isEmpty()) {
                break // 无法继续执行
            }

            for (step in readySteps) {
                val result = step.execute(WorkflowContext(input, steps))
                steps[step.id] = result
                executedSteps.add(step.id)
            }
        }

        return WorkflowResult(success = true, output = mapOf(), steps = steps)
    }

    @Test
    fun `test debug workflow with HTML report generation`() = runBlocking {
        // 创建测试工作流
        val workflow = createTestWorkflow()

        // 创建输入数据
        val input = mapOf(
            "param1" to "value1",
            "param2" to 42
        )

        // 创建调试器
        val debugger = DataFlowDebugger()

        // 调试工作流，启用HTML报告生成
        val outputDir = tempDir.resolve("debug_output_html").toString()
        val options = DataFlowDebugger.DebugOptions(
            mode = DataFlowDebugger.DebugMode.REPORT,
            outputDir = outputDir,
            generateHtmlReport = true,
            generateVisualizations = true,
            traceVariables = true,
            traceDataFlow = true,
            showVariablesAfterStep = true
        )
        val result = debugger.debugWorkflow(
            workflow = workflow,
            input = input,
            options = options
        )

        // 验证结果
        assertNotNull(result)
        assertEquals(3, result.steps.size)
        assertTrue(result.steps.all { it.value.success })

        // 验证调试输出目录
        val outputDirFile = File(outputDir)
        assertTrue(outputDirFile.exists())
        assertTrue(outputDirFile.isDirectory)

        // 验证调试会话目录
        val sessionDirs = outputDirFile.listFiles { file -> file.isDirectory && file.name.startsWith("debug_session_") }
        assertNotNull(sessionDirs)
        assertTrue(sessionDirs!!.isNotEmpty())

        // 验证HTML报告文件
        val sessionDir = sessionDirs.first()
        val htmlReportFile = File(sessionDir, "report.html")
        assertTrue(htmlReportFile.exists())

        // 验证HTML报告内容
        val htmlContent = htmlReportFile.readText()
        assertTrue(htmlContent.contains("<!DOCTYPE html>"))
        assertTrue(htmlContent.contains("<title>工作流调试报告</title>"))
        assertTrue(htmlContent.contains("<h1>工作流调试报告</h1>"))
        assertTrue(htmlContent.contains("<div id=\"summary\" class=\"tabcontent active\">"))
        assertTrue(htmlContent.contains("<div id=\"detail\" class=\"tabcontent\">"))
        assertTrue(htmlContent.contains("<div id=\"variables\" class=\"tabcontent\">"))
        assertTrue(htmlContent.contains("<div id=\"dataflow\" class=\"tabcontent\">"))

        // 验证可视化文件
        assertTrue(File(sessionDir, "workflow_visualization.mmd").exists())
        assertTrue(File(sessionDir, "data_flow_visualization.mmd").exists())
    }
}
