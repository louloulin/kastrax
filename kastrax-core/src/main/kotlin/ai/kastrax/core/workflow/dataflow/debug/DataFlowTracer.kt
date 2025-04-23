package ai.kastrax.core.workflow.dataflow.debug

import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.EnhancedWorkflowContext
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.VariableResolver
import ai.kastrax.core.workflow.dataflow.VariableScopeManager
import ai.kastrax.core.workflow.dataflow.visualization.EnhancedVariableReferenceProvider
import ai.kastrax.core.workflow.dataflow.visualization.getWorkflowSteps
import mu.KotlinLogging
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 数据流跟踪器，用于跟踪工作流中的数据流。
 */
class DataFlowTracer {
    private val logger = KotlinLogging.logger {}
    private val scopeManager = VariableScopeManager()
    private val resolver = VariableResolver(scopeManager)

    /**
     * 跟踪工作流执行。
     *
     * @param workflow 工作流
     * @param context 工作流上下文
     * @return 跟踪结果
     */
    fun traceWorkflowExecution(workflow: Workflow, context: WorkflowContext): TraceResult {
        logger.info { "开始跟踪工作流执行: ${workflow.javaClass.simpleName}" }

        val result = TraceResult(workflow, context)

        // 跟踪输入数据
        traceInputData(workflow, context, result)

        // 跟踪步骤执行
        traceStepExecution(workflow, context, result)

        // 跟踪数据流
        traceDataFlow(workflow, context, result)

        logger.info { "工作流执行跟踪完成: ${workflow.javaClass.simpleName}" }

        return result
    }

    /**
     * 跟踪输入数据。
     */
    private fun traceInputData(workflow: Workflow, context: WorkflowContext, result: TraceResult) {
        logger.debug { "跟踪输入数据" }

        context.input.forEach { (key, value) ->
            val trace = DataTrace(
                sourceId = "input",
                sourceType = NodeType.INPUT,
                targetId = null,
                targetType = null,
                variableName = key,
                value = value,
                timestamp = LocalDateTime.now()
            )

            result.dataTraces.add(trace)
        }
    }

    /**
     * 跟踪步骤执行。
     */
    private fun traceStepExecution(workflow: Workflow, context: WorkflowContext, result: TraceResult) {
        logger.debug { "跟踪步骤执行" }

        getWorkflowSteps(workflow).forEach { step ->
            val stepId = step.id
            val stepResult = context.steps[stepId]

            if (stepResult != null) {
                val trace = StepTrace(
                    stepId = stepId,
                    success = stepResult.success,
                    error = stepResult.error,
                    executionTime = stepResult.executionTime,
                    timestamp = LocalDateTime.now()
                )

                result.stepTraces.add(trace)

                // 跟踪步骤输出
                stepResult.output.forEach { (key, value) ->
                    val dataTrace = DataTrace(
                        sourceId = stepId,
                        sourceType = NodeType.STEP,
                        targetId = null,
                        targetType = null,
                        variableName = key,
                        value = value,
                        timestamp = LocalDateTime.now()
                    )

                    result.dataTraces.add(dataTrace)
                }
            }
        }
    }

    /**
     * 跟踪数据流。
     */
    private fun traceDataFlow(workflow: Workflow, context: WorkflowContext, result: TraceResult) {
        logger.debug { "跟踪数据流" }

        // 跟踪步骤间的数据流
        getWorkflowSteps(workflow).forEach { step ->
            val stepId = step.id

            // 跟踪标准变量引用
            step.variables.forEach { (name, reference) ->
                val value = context.resolveReference(reference)

                val sourceId = when {
                    reference.path.startsWith("$.input.") -> "input"
                    reference.path.startsWith("$.steps.") -> {
                        val parts = reference.path.removePrefix("$.steps.").split(".", limit = 2)
                        if (parts.isNotEmpty()) parts[0] else null
                    }
                    reference.path.startsWith("$.variables.") -> "variables"
                    else -> null
                }

                val sourceType = when (sourceId) {
                    "input" -> NodeType.INPUT
                    "variables" -> NodeType.VARIABLE
                    null -> null
                    else -> NodeType.STEP
                }

                if (sourceId != null && sourceType != null) {
                    val trace = DataTrace(
                        sourceId = sourceId,
                        sourceType = sourceType,
                        targetId = stepId,
                        targetType = NodeType.STEP,
                        variableName = name,
                        value = value,
                        timestamp = LocalDateTime.now()
                    )

                    result.dataTraces.add(trace)
                }
            }

            // 跟踪增强变量引用
            if (step is EnhancedVariableReferenceProvider) {
                step.getEnhancedVariableReferences().forEach { reference ->
                    val value = resolver.resolve(reference, context, "default", stepId)

                    val sourceId = when (reference.source) {
                        SourceType.INPUT -> "input"
                        SourceType.STEP -> {
                            val parts = reference.path.split(".", limit = 2)
                            if (parts.isNotEmpty()) parts[0] else null
                        }
                        SourceType.VARIABLE -> "variables"
                        SourceType.CONSTANT -> "constant"
                    }

                    val sourceType = when (reference.source) {
                        SourceType.INPUT -> NodeType.INPUT
                        SourceType.STEP -> NodeType.STEP
                        SourceType.VARIABLE -> NodeType.VARIABLE
                        SourceType.CONSTANT -> NodeType.CONSTANT
                    }

                    val trace = DataTrace(
                        sourceId = sourceId,
                        sourceType = sourceType,
                        targetId = stepId,
                        targetType = NodeType.STEP,
                        variableName = reference.path,
                        value = value,
                        timestamp = LocalDateTime.now()
                    )

                    result.dataTraces.add(trace)
                }
            }
        }
    }

    /**
     * 跟踪变量值的变化。
     *
     * @param workflow 工作流
     * @param variableName 变量名
     * @param context 工作流上下文
     * @return 变量跟踪结果
     */
    fun traceVariable(
        workflow: Workflow,
        variableName: String,
        context: WorkflowContext
    ): VariableTraceResult {
        logger.info { "开始跟踪变量: $variableName" }

        val result = VariableTraceResult(variableName)

        // 检查输入中是否有该变量
        if (variableName in context.input) {
            result.addTrace(
                "input",
                NodeType.INPUT,
                context.input[variableName],
                LocalDateTime.now()
            )
        }

        // 检查步骤输出中是否有该变量
        context.steps.forEach { (stepId, stepResult) ->
            if (variableName in stepResult.output) {
                result.addTrace(
                    stepId,
                    NodeType.STEP,
                    stepResult.output[variableName],
                    LocalDateTime.now()
                )
            }
        }

        // 检查变量作用域中是否有该变量
        val globalScope = scopeManager.getGlobalScope()
        if (globalScope.contains(variableName)) {
            result.addTrace(
                "global",
                NodeType.VARIABLE,
                globalScope.get(variableName),
                LocalDateTime.now()
            )
        }

        val workflowScope = scopeManager.getWorkflowScope("default")
        if (workflowScope.contains(variableName)) {
            result.addTrace(
                "workflow",
                NodeType.VARIABLE,
                workflowScope.get(variableName),
                LocalDateTime.now()
            )
        }

        getWorkflowSteps(workflow).forEach { step ->
            val stepScope = scopeManager.getStepScope("default", step.id)
            if (stepScope.contains(variableName)) {
                result.addTrace(
                    step.id,
                    NodeType.STEP,
                    stepScope.get(variableName),
                    LocalDateTime.now()
                )
            }
        }

        logger.info { "变量跟踪完成: $variableName, 找到 ${result.traces.size} 个跟踪点" }

        return result
    }

    /**
     * 跟踪结果类。
     */
    class TraceResult(val workflow: Workflow, val context: WorkflowContext) {
        val stepTraces = mutableListOf<StepTrace>()
        val dataTraces = mutableListOf<DataTrace>()

        /**
         * 获取特定步骤的跟踪。
         */
        fun getStepTrace(stepId: String): StepTrace? {
            return stepTraces.find { it.stepId == stepId }
        }

        /**
         * 获取特定步骤的数据跟踪。
         */
        fun getStepDataTraces(stepId: String): List<DataTrace> {
            return dataTraces.filter { it.sourceId == stepId || it.targetId == stepId }
        }

        /**
         * 获取特定变量的数据跟踪。
         */
        fun getVariableDataTraces(variableName: String): List<DataTrace> {
            return dataTraces.filter { it.variableName == variableName }
        }

        /**
         * 生成跟踪报告。
         */
        fun generateReport(): String {
            val sb = StringBuilder()
            sb.appendLine("=== 工作流执行跟踪报告 ===")
            sb.appendLine("工作流: ${workflow.javaClass.simpleName}")
            sb.appendLine("步骤数: ${getWorkflowSteps(workflow).size}")
            sb.appendLine("已执行步骤数: ${context.steps.size}")
            sb.appendLine("成功步骤数: ${context.steps.count { it.value.success }}")
            sb.appendLine("失败步骤数: ${context.steps.count { !it.value.success }}")
            sb.appendLine()

            sb.appendLine("步骤执行跟踪:")
            stepTraces.forEach { trace ->
                val step = getWorkflowSteps(workflow).find { it.id == trace.stepId }
                if (step != null) {
                    sb.appendLine("  步骤: ${step.name} (${trace.stepId})")
                    sb.appendLine("    状态: ${if (trace.success) "成功" else "失败"}")
                    sb.appendLine("    执行时间: ${trace.executionTime}ms")
                    sb.appendLine("    时间戳: ${trace.timestamp}")

                    if (trace.error != null) {
                        sb.appendLine("    错误: ${trace.error}")
                    }

                    sb.appendLine()
                }
            }

            sb.appendLine("数据流跟踪:")
            dataTraces.forEach { trace ->
                sb.append("  ")

                // 源
                when (trace.sourceType) {
                    NodeType.INPUT -> sb.append("输入")
                    NodeType.STEP -> {
                        val sourceStep = getWorkflowSteps(workflow).find { it.id == trace.sourceId }
                        if (sourceStep != null) {
                            sb.append("${sourceStep.name} (${trace.sourceId})")
                        } else {
                            sb.append("未知步骤 (${trace.sourceId})")
                        }
                    }
                    NodeType.VARIABLE -> sb.append("变量 (${trace.sourceId})")
                    NodeType.CONSTANT -> sb.append("常量")
                    NodeType.OUTPUT -> sb.append("输出")
                    null -> sb.append("未知来源")
                }

                // 目标
                if (trace.targetId != null && trace.targetType != null) {
                    sb.append(" -> ")

                    when (trace.targetType) {
                        NodeType.INPUT -> sb.append("输入")
                        NodeType.STEP -> {
                            val targetStep = getWorkflowSteps(workflow).find { it.id == trace.targetId }
                            if (targetStep != null) {
                                sb.append("${targetStep.name} (${trace.targetId})")
                            } else {
                                sb.append("未知步骤 (${trace.targetId})")
                            }
                        }
                        NodeType.VARIABLE -> sb.append("变量 (${trace.targetId})")
                        NodeType.CONSTANT -> sb.append("常量")
                        NodeType.OUTPUT -> sb.append("输出")
                    }
                }

                sb.appendLine()
                sb.appendLine("    变量: ${trace.variableName}")
                sb.appendLine("    值: ${trace.value}")
                sb.appendLine("    时间戳: ${trace.timestamp}")
                sb.appendLine()
            }

            return sb.toString()
        }
    }

    /**
     * 变量跟踪结果类。
     */
    class VariableTraceResult(val variableName: String) {
        val traces = mutableListOf<VariableTrace>()

        /**
         * 添加跟踪。
         */
        fun addTrace(sourceId: String, sourceType: NodeType, value: Any?, timestamp: LocalDateTime) {
            traces.add(VariableTrace(sourceId, sourceType, value, timestamp))
        }

        /**
         * 生成跟踪报告。
         */
        fun generateReport(): String {
            val sb = StringBuilder()
            sb.appendLine("=== 变量跟踪报告 ===")
            sb.appendLine("变量: $variableName")
            sb.appendLine("跟踪点数: ${traces.size}")
            sb.appendLine()

            if (traces.isEmpty()) {
                sb.appendLine("未找到变量跟踪点")
            } else {
                sb.appendLine("变量值变化:")
                traces.forEachIndexed { index, trace ->
                    sb.appendLine("${index + 1}. 来源: ${getSourceDescription(trace.sourceId, trace.sourceType)}")
                    sb.appendLine("   值: ${trace.value}")
                    sb.appendLine("   时间: ${trace.timestamp}")
                    sb.appendLine()
                }
            }

            return sb.toString()
        }

        /**
         * 获取来源描述。
         */
        private fun getSourceDescription(sourceId: String, sourceType: NodeType): String {
            return when (sourceType) {
                NodeType.INPUT -> "输入"
                NodeType.STEP -> "步骤 $sourceId"
                NodeType.VARIABLE -> when (sourceId) {
                    "global" -> "全局变量"
                    "workflow" -> "工作流变量"
                    else -> "步骤变量 $sourceId"
                }
                NodeType.CONSTANT -> "常量"
                NodeType.OUTPUT -> "输出"
            }
        }
    }

    /**
     * 步骤跟踪类。
     */
    data class StepTrace(
        val stepId: String,
        val success: Boolean = true,
        val error: String? = null,
        val executionTime: Long = 0,
        val timestamp: LocalDateTime = LocalDateTime.now()
    )

    /**
     * 数据跟踪类。
     */
    data class DataTrace(
        val sourceId: String?,
        val sourceType: NodeType?,
        val targetId: String?,
        val targetType: NodeType?,
        val variableName: String,
        val value: Any?,
        val timestamp: LocalDateTime = LocalDateTime.now()
    )

    /**
     * 变量跟踪类。
     */
    data class VariableTrace(
        val sourceId: String,
        val sourceType: NodeType,
        val value: Any?,
        val timestamp: LocalDateTime = LocalDateTime.now()
    )

    /**
     * 节点类型枚举。
     */
    enum class NodeType {
        /**
         * 输入节点。
         */
        INPUT,

        /**
         * 步骤节点。
         */
        STEP,

        /**
         * 变量节点。
         */
        VARIABLE,

        /**
         * 常量节点。
         */
        CONSTANT,

        /**
         * 输出节点。
         */
        OUTPUT
    }
}
