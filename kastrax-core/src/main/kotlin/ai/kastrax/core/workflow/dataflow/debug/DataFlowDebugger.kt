package ai.kastrax.core.workflow.dataflow.debug

import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.EnhancedWorkflowContext
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.VariableResolver
import ai.kastrax.core.workflow.dataflow.VariableScopeManager
import ai.kastrax.core.workflow.dataflow.visualization.EnhancedVariableReferenceProvider
import ai.kastrax.core.workflow.dataflow.visualization.getWorkflowSteps
import mu.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 数据流调试器，用于调试工作流中的数据流。
 */
class DataFlowDebugger {
    private val logger = KotlinLogging.logger {}
    private val tracer = DataFlowTracer()
    private val inspector = DataFlowInspector()
    private val scopeManager = VariableScopeManager()
    private val resolver = VariableResolver(scopeManager)

    /**
     * 调试模式枚举。
     */
    enum class DebugMode {
        /**
         * 仅记录日志。
         */
        LOG_ONLY,

        /**
         * 记录日志并生成报告。
         */
        REPORT,

        /**
         * 交互式调试。
         */
        INTERACTIVE
    }

    /**
     * 调试工作流执行。
     *
     * @param workflow 工作流
     * @param input 输入数据
     * @param mode 调试模式
     * @param breakpoints 断点列表，包含步骤ID
     * @param outputDir 输出目录
     * @return 工作流执行结果
     */
    suspend fun debugWorkflow(
        workflow: Workflow,
        input: Map<String, Any?>,
        mode: DebugMode = DebugMode.REPORT,
        breakpoints: List<String> = emptyList(),
        outputDir: String = "debug_output"
    ): ai.kastrax.core.workflow.WorkflowResult {
        // 创建调试上下文
        val debugContext = DebugContext(
            workflow = workflow,
            input = input,
            mode = mode,
            breakpoints = breakpoints,
            outputDir = outputDir
        )

        // 初始化调试环境
        initializeDebugEnvironment(debugContext)

        // 执行工作流
        val result = executeDebugWorkflow(workflow, input, debugContext)

        // 生成调试报告
        if (mode == DebugMode.REPORT) {
            generateDebugReport(debugContext, result)
        }

        return result
    }

    /**
     * 初始化调试环境。
     */
    private fun initializeDebugEnvironment(context: DebugContext) {
        // 创建输出目录
        if (context.mode == DebugMode.REPORT) {
            val outputDir = File(context.outputDir)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // 创建调试会话目录
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val sessionDir = File(outputDir, "debug_session_$timestamp")
            sessionDir.mkdirs()

            context.sessionDir = sessionDir.absolutePath
            logger.info { "调试会话目录: ${context.sessionDir}" }
        }
    }

    /**
     * 生成调试报告。
     */
    private fun generateDebugReport(context: DebugContext, result: ai.kastrax.core.workflow.WorkflowResult) {
        val sessionDir = context.sessionDir ?: return

        // 生成摘要报告
        val summaryFile = File(sessionDir, "summary.txt")
        summaryFile.writeText(generateSummaryReport(context, result))

        // 生成详细报告
        val detailFile = File(sessionDir, "detail.txt")
        detailFile.writeText(generateDetailReport(context, result))

        // 生成变量报告
        val variablesFile = File(sessionDir, "variables.txt")
        variablesFile.writeText(generateVariablesReport(context, result))

        // 生成数据流报告
        val dataFlowFile = File(sessionDir, "data_flow.txt")
        dataFlowFile.writeText(generateDataFlowReport(context, result))

        logger.info { "调试报告已生成: $sessionDir" }
    }

    /**
     * 生成摘要报告。
     */
    private fun generateSummaryReport(context: DebugContext, result: ai.kastrax.core.workflow.WorkflowResult): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流调试摘要 ===")
        sb.appendLine("工作流: ${context.workflow.javaClass.simpleName}")
        sb.appendLine("执行时间: ${context.startTime} - ${context.endTime}")
        sb.appendLine("总执行时间: ${context.getTotalExecutionTime()}ms")
        val workflowSteps = getWorkflowSteps(context.workflow)
        sb.appendLine("步骤数: ${workflowSteps.size}")
        sb.appendLine("已执行步骤数: ${result.steps.size}")
        sb.appendLine("成功步骤数: ${result.steps.count { entry -> entry.value.success }}")
        sb.appendLine("失败步骤数: ${result.steps.count { entry -> !entry.value.success }}")
        sb.appendLine()

        sb.appendLine("步骤执行摘要:")
        workflowSteps.forEach { step ->
            val stepResult = result.steps[step.id]
            val status = when {
                stepResult == null -> "未执行"
                stepResult.success -> "成功"
                else -> "失败"
            }
            val time = stepResult?.executionTime ?: 0
            sb.appendLine("  ${step.id}: $status, ${time}ms")
        }

        return sb.toString()
    }

    /**
     * 生成详细报告。
     */
    private fun generateDetailReport(context: DebugContext, result: ai.kastrax.core.workflow.WorkflowResult): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流调试详情 ===")
        sb.appendLine("工作流: ${context.workflow.javaClass.simpleName}")
        sb.appendLine("执行时间: ${context.startTime} - ${context.endTime}")
        sb.appendLine("总执行时间: ${context.getTotalExecutionTime()}ms")
        sb.appendLine()

        sb.appendLine("输入:")
        context.input.forEach { (key, value) ->
            sb.appendLine("  $key = $value")
        }
        sb.appendLine()

        sb.appendLine("步骤执行详情:")
        context.stepTraces.forEach { (stepId, trace) ->
            val step = getWorkflowSteps(context.workflow).find { it.id == stepId }
            if (step != null) {
                sb.appendLine("  步骤: ${step.id}")
                sb.appendLine("    开始时间: ${trace.startTime}")
                sb.appendLine("    结束时间: ${trace.endTime}")
                sb.appendLine("    执行时间: ${trace.executionTime}ms")
                sb.appendLine("    状态: ${if (trace.success) "成功" else "失败"}")

                if (trace.error != null) {
                    sb.appendLine("    错误: ${trace.error}")
                }

                sb.appendLine("    输入变量:")
                trace.inputVariables.forEach { (key, value) ->
                    sb.appendLine("      $key = $value")
                }

                sb.appendLine("    输出:")
                trace.output.forEach { (key, value) ->
                    sb.appendLine("      $key = $value")
                }

                sb.appendLine()
            }
        }

        return sb.toString()
    }

    /**
     * 生成变量报告。
     */
    private fun generateVariablesReport(context: DebugContext, result: ai.kastrax.core.workflow.WorkflowResult): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流变量报告 ===")
        sb.appendLine("工作流: ${context.workflow.javaClass.simpleName}")
        sb.appendLine()

        sb.appendLine("全局变量:")
        val globalScope = scopeManager.getGlobalScope()
        globalScope.getAll().forEach { (key, value) ->
            sb.appendLine("  $key = $value")
        }
        sb.appendLine()

        sb.appendLine("工作流变量:")
        val workflowScope = scopeManager.getWorkflowScope("default")
        workflowScope.getAll().forEach { (key, value) ->
            sb.appendLine("  $key = $value")
        }
        sb.appendLine()

        sb.appendLine("步骤变量:")
        val workflowSteps = getWorkflowSteps(context.workflow)
        workflowSteps.forEach { step ->
            sb.appendLine("  步骤: ${step.id}")
            val stepScope = scopeManager.getStepScope("default", step.id)
            stepScope.getAll().forEach { (key, value) ->
                sb.appendLine("    $key = $value")
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * 生成数据流报告。
     */
    private fun generateDataFlowReport(context: DebugContext, result: ai.kastrax.core.workflow.WorkflowResult): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流数据流报告 ===")
        sb.appendLine("工作流: ${context.workflow.javaClass.simpleName}")
        sb.appendLine()

        sb.appendLine("数据流跟踪:")
        context.dataFlowTraces.forEach { trace ->
            sb.appendLine("  源: ${trace.sourceDescription}")
            sb.appendLine("  目标: ${trace.targetDescription}")
            sb.appendLine("  变量: ${trace.variableName}")
            sb.appendLine("  值: ${trace.value}")
            sb.appendLine("  时间: ${trace.timestamp}")
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * 在步骤执行前调用。
     */
    suspend fun beforeStepExecution(
        step: ai.kastrax.core.workflow.WorkflowStep,
        context: WorkflowContext,
        debugContext: DebugContext
    ) {
        val stepId = step.id
        val enhancedContext = EnhancedWorkflowContext.fromStandardContext(context, "default")

        // 创建步骤跟踪
        val trace = StepTrace(
            stepId = stepId,
            startTime = LocalDateTime.now()
        )

        // 收集输入变量
        val inputVariables = mutableMapOf<String, Any?>()

        // 收集标准变量引用
        step.variables.forEach { (name, reference) ->
            val value = context.resolveReference(reference)
            inputVariables[name] = value

            // 记录数据流
            val sourceDescription = when {
                reference.path.startsWith("$.input.") -> "输入"
                reference.path.startsWith("$.steps.") -> {
                    val parts = reference.path.removePrefix("$.steps.").split(".", limit = 2)
                    if (parts.isNotEmpty()) {
                        val sourceStepId = parts[0]
                        val sourceStep = getWorkflowSteps(debugContext.workflow).find { it.id == sourceStepId }
                        if (sourceStep != null) {
                            "${sourceStep.id}"
                        } else {
                            "未知步骤 ($sourceStepId)"
                        }
                    } else {
                        "未知来源"
                    }
                }
                reference.path.startsWith("$.variables.") -> "变量"
                else -> "常量"
            }

            val dataFlowTrace = DataFlowTrace(
                sourceDescription = sourceDescription,
                targetDescription = "${step.id}",
                variableName = name,
                value = value,
                timestamp = LocalDateTime.now()
            )

            debugContext.dataFlowTraces.add(dataFlowTrace)
        }

        // 收集增强变量引用
        if (step is EnhancedVariableReferenceProvider) {
            step.getEnhancedVariableReferences().forEach { reference ->
                val value = resolver.resolve(reference, context, "default", stepId)
                val name = reference.path

                val sourceDescription = when (reference.source) {
                    SourceType.INPUT -> "输入"
                    SourceType.STEP -> {
                        val parts = reference.path.split(".", limit = 2)
                        if (parts.isNotEmpty()) {
                            val sourceStepId = parts[0]
                            val sourceStep = getWorkflowSteps(debugContext.workflow).find { it.id == sourceStepId }
                            if (sourceStep != null) {
                                "${sourceStep.id}"
                            } else {
                                "未知步骤 ($sourceStepId)"
                            }
                        } else {
                            "未知来源"
                        }
                    }
                    SourceType.VARIABLE -> "变量"
                    SourceType.CONSTANT -> "常量"
                }

                inputVariables["$name (${reference.source})"] = value

                val dataFlowTrace = DataFlowTrace(
                    sourceDescription = sourceDescription,
                    targetDescription = "${step.id}",
                    variableName = name,
                    value = value,
                    timestamp = LocalDateTime.now()
                )

                debugContext.dataFlowTraces.add(dataFlowTrace)
            }
        }

        trace.inputVariables = inputVariables
        debugContext.stepTraces[stepId] = trace

        // 记录日志
        logger.info { "开始执行步骤: ${step.id}" }
        logger.debug { "步骤输入变量: $inputVariables" }

        // 检查断点
        if (debugContext.breakpoints.contains(stepId) && debugContext.mode == DebugMode.INTERACTIVE) {
            handleBreakpoint(step, context, debugContext)
        }
    }

    /**
     * 在步骤执行后调用。
     */
    suspend fun afterStepExecution(
        step: ai.kastrax.core.workflow.WorkflowStep,
        result: WorkflowStepResult,
        context: WorkflowContext,
        debugContext: DebugContext
    ) {
        val stepId = step.id
        val trace = debugContext.stepTraces[stepId] ?: StepTrace(stepId = stepId, startTime = LocalDateTime.now())

        // 更新步骤跟踪
        trace.endTime = LocalDateTime.now()
        trace.executionTime = result.executionTime
        trace.success = result.success
        trace.error = result.error
        trace.output = result.output

        debugContext.stepTraces[stepId] = trace

        // 记录日志
        if (result.success) {
            logger.info { "步骤执行成功: ${step.id}, 耗时: ${result.executionTime}ms" }
        } else {
            logger.error { "步骤执行失败: ${step.id}, 错误: ${result.error}" }
        }
        logger.debug { "步骤输出: ${result.output}" }

        // 记录数据流
        result.output.forEach { (key, value) ->
            val dataFlowTrace = DataFlowTrace(
                sourceDescription = "${step.id}",
                targetDescription = "输出",
                variableName = key,
                value = value,
                timestamp = LocalDateTime.now()
            )

            debugContext.dataFlowTraces.add(dataFlowTrace)
        }
    }

    /**
     * 处理断点。
     */
    private suspend fun handleBreakpoint(
        step: ai.kastrax.core.workflow.WorkflowStep,
        context: WorkflowContext,
        debugContext: DebugContext
    ) {
        logger.info { "遇到断点: ${step.id}" }
        logger.info { "按Enter键继续执行..." }

        // 在实际应用中，这里可以实现交互式调试界面
        // 例如，显示变量值，允许修改变量，单步执行等

        // 简单实现：等待用户输入
        readLine()
    }

    /**
     * 调试上下文，用于存储调试信息。
     */
    class DebugContext(
        val workflow: Workflow,
        val input: Map<String, Any?>,
        val mode: DebugMode,
        val breakpoints: List<String>,
        val outputDir: String
    ) {
        val startTime: LocalDateTime = LocalDateTime.now()
        var endTime: LocalDateTime? = null
        var sessionDir: String? = null

        val stepTraces = mutableMapOf<String, StepTrace>()
        val dataFlowTraces = mutableListOf<DataFlowTrace>()

        /**
         * 获取总执行时间。
         */
        fun getTotalExecutionTime(): Long {
            val end = endTime ?: LocalDateTime.now()
            return java.time.Duration.between(startTime, end).toMillis()
        }
    }

    /**
     * 步骤跟踪，用于记录步骤执行信息。
     */
    data class StepTrace(
        val stepId: String,
        val startTime: LocalDateTime,
        var endTime: LocalDateTime? = null,
        var executionTime: Long = 0,
        var success: Boolean = false,
        var error: String? = null,
        var inputVariables: Map<String, Any?> = emptyMap(),
        var output: Map<String, Any?> = emptyMap()
    )

    /**
     * 数据流跟踪，用于记录数据流信息。
     */
    data class DataFlowTrace(
        val sourceDescription: String,
        val targetDescription: String,
        val variableName: String,
        val value: Any?,
        val timestamp: LocalDateTime
    )

    /**
     * 执行调试工作流。
     */
    private suspend fun executeDebugWorkflow(
        workflow: Workflow,
        input: Map<String, Any?>,
        debugContext: DebugContext
    ): ai.kastrax.core.workflow.WorkflowResult {
        logger.info { "开始调试工作流: ${workflow.javaClass.simpleName}" }

        // 创建调试工作流
        val debugWorkflow = createDebugWorkflow(workflow)

        // 执行工作流
        val options = WorkflowExecuteOptions()
        val result = workflow.execute(input, options)

        // 更新调试上下文
        debugContext.endTime = LocalDateTime.now()

        logger.info { "工作流调试完成: ${workflow.javaClass.simpleName}, 耗时: ${debugContext.getTotalExecutionTime()}ms" }

        return result
    }

    /**
     * 创建调试工作流，包装原始步骤，添加调试功能。
     */
    private fun createDebugWorkflow(workflow: Workflow): Workflow {
        // 返回原始工作流，因为我们不需要包装步骤
        return workflow
    }
}
