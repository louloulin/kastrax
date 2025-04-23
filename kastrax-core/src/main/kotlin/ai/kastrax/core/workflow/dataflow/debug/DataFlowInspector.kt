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

/**
 * 数据流检查器，用于分析和检查工作流中的数据流。
 */
class DataFlowInspector {
    private val logger = KotlinLogging.logger {}
    private val scopeManager = VariableScopeManager()
    private val resolver = VariableResolver(scopeManager)

    /**
     * 检查工作流中的数据流。
     *
     * @param workflow 工作流
     * @return 检查结果
     */
    fun inspectWorkflow(workflow: Workflow): InspectionResult {
        logger.info { "开始检查工作流: ${workflow.javaClass.simpleName}" }

        val result = InspectionResult(workflow)

        // 检查步骤依赖关系
        inspectStepDependencies(workflow, result)

        // 检查变量引用
        inspectVariableReferences(workflow, result)

        // 检查数据流路径
        inspectDataFlowPaths(workflow, result)

        logger.info { "工作流检查完成: ${workflow.javaClass.simpleName}" }
        logger.info { "发现 ${result.issues.size} 个问题" }

        return result
    }

    /**
     * 检查步骤依赖关系。
     */
    private fun inspectStepDependencies(workflow: Workflow, result: InspectionResult) {
        logger.debug { "检查步骤依赖关系" }

        // 检查循环依赖
        val dependencies = mutableMapOf<String, Set<String>>()
        getWorkflowSteps(workflow).forEach { step ->
            dependencies[step.id] = step.after.toSet()
        }

        val visited = mutableSetOf<String>()
        val stack = mutableSetOf<String>()

        fun detectCycle(stepId: String): Boolean {
            if (stepId in stack) return true
            if (stepId in visited) return false

            visited.add(stepId)
            stack.add(stepId)

            dependencies[stepId]?.forEach { afterStepId ->
                if (detectCycle(afterStepId)) return true
            }

            stack.remove(stepId)
            return false
        }

        getWorkflowSteps(workflow).forEach { step ->
            if (detectCycle(step.id)) {
                result.addIssue(
                    IssueType.CYCLIC_DEPENDENCY,
                    "步骤 ${step.id} 存在循环依赖",
                    step.id
                )
            }
        }

        // 检查不存在的依赖
        getWorkflowSteps(workflow).forEach { step ->
            step.after.forEach { afterStepId ->
                if (getWorkflowSteps(workflow).none { it.id == afterStepId }) {
                    result.addIssue(
                        IssueType.MISSING_DEPENDENCY,
                        "步骤 ${step.id} 依赖不存在的步骤 $afterStepId",
                        step.id
                    )
                }
            }
        }
    }

    /**
     * 检查变量引用。
     */
    private fun inspectVariableReferences(workflow: Workflow, result: InspectionResult) {
        logger.debug { "检查变量引用" }

        // 检查标准变量引用
        getWorkflowSteps(workflow).forEach { step ->
            step.variables.forEach { (name, reference) ->
                // 检查步骤输出引用
                if (reference.path.startsWith("$.steps.")) {
                    val parts = reference.path.removePrefix("$.steps.").split(".", limit = 2)
                    if (parts.isNotEmpty()) {
                        val sourceStepId = parts[0]

                        // 检查引用的步骤是否存在
                        if (getWorkflowSteps(workflow).none { it.id == sourceStepId }) {
                            result.addIssue(
                                IssueType.INVALID_REFERENCE,
                                "步骤 ${step.id} 引用不存在的步骤 $sourceStepId",
                                step.id
                            )
                        }

                        // 检查引用的步骤是否在当前步骤之前执行
                        if (getWorkflowSteps(workflow).any { it.id == sourceStepId } && !step.after.contains(sourceStepId)) {
                            // 检查是否有间接依赖
                            val hasIndirectDependency = hasIndirectDependency(workflow, step.id, sourceStepId)
                            if (!hasIndirectDependency) {
                                result.addIssue(
                                    IssueType.MISSING_DEPENDENCY,
                                    "步骤 ${step.id} 引用步骤 $sourceStepId 的输出，但没有依赖关系",
                                    step.id
                                )
                            }
                        }
                    }
                }
            }
        }

        // 检查增强变量引用
        getWorkflowSteps(workflow).forEach { step ->
            if (step is EnhancedVariableReferenceProvider) {
                step.getEnhancedVariableReferences().forEach { reference ->
                    if (reference.source == SourceType.STEP) {
                        val parts = reference.path.split(".", limit = 2)
                        if (parts.isNotEmpty()) {
                            val sourceStepId = parts[0]

                            // 检查引用的步骤是否存在
                            if (getWorkflowSteps(workflow).none { it.id == sourceStepId }) {
                                result.addIssue(
                                    IssueType.INVALID_REFERENCE,
                                    "步骤 ${step.id} 引用不存在的步骤 $sourceStepId",
                                    step.id
                                )
                            }

                            // 检查引用的步骤是否在当前步骤之前执行
                            if (getWorkflowSteps(workflow).any { it.id == sourceStepId } && !step.after.contains(sourceStepId)) {
                                // 检查是否有间接依赖
                                val hasIndirectDependency = hasIndirectDependency(workflow, step.id, sourceStepId)
                                if (!hasIndirectDependency) {
                                    result.addIssue(
                                        IssueType.MISSING_DEPENDENCY,
                                        "步骤 ${step.id} 引用步骤 $sourceStepId 的输出，但没有依赖关系",
                                        step.id
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查数据流路径。
     */
    private fun inspectDataFlowPaths(workflow: Workflow, result: InspectionResult) {
        logger.debug { "检查数据流路径" }

        // 构建数据流图
        val dataFlowGraph = buildDataFlowGraph(workflow)

        // 检查未使用的输出
        getWorkflowSteps(workflow).forEach { step ->
            val stepId = step.id
            val outgoingEdges = dataFlowGraph.edges.filter { it.source == stepId }

            if (outgoingEdges.isEmpty() && getWorkflowSteps(workflow).any { s -> s.after.contains(stepId) }) {
                result.addIssue(
                    IssueType.UNUSED_OUTPUT,
                    "步骤 $stepId 的输出未被使用",
                    stepId
                )
            }
        }

        // 检查未使用的输入
        getWorkflowSteps(workflow).forEach { step ->
            val stepId = step.id
            val incomingEdges = dataFlowGraph.edges.filter { edge -> edge.target == stepId }

            if (incomingEdges.isEmpty() && step.after.isNotEmpty()) {
                result.addIssue(
                    IssueType.UNUSED_INPUT,
                    "步骤 $stepId 未使用其依赖步骤的输出",
                    stepId
                )
            }
        }
    }

    /**
     * 检查是否有间接依赖关系。
     */
    private fun hasIndirectDependency(workflow: Workflow, stepId: String, dependencyId: String): Boolean {
        val visited = mutableSetOf<String>()

        fun dfs(currentId: String): Boolean {
            if (currentId == dependencyId) return true
            if (currentId in visited) return false

            visited.add(currentId)

            val step = getWorkflowSteps(workflow).find { s -> s.id == currentId } ?: return false
            return step.after.any { dfs(it) }
        }

        return dfs(stepId)
    }

    /**
     * 构建数据流图。
     */
    private fun buildDataFlowGraph(workflow: Workflow): DataFlowGraph {
        val nodes = mutableListOf<DataFlowNode>()
        val edges = mutableListOf<DataFlowEdge>()

        // 添加输入节点
        nodes.add(DataFlowNode("input", "Input", NodeType.INPUT))

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            nodes.add(DataFlowNode(step.id, step.name, NodeType.STEP))
        }

        // 添加输出节点
        nodes.add(DataFlowNode("output", "Output", NodeType.OUTPUT))

        // 添加数据流边
        getWorkflowSteps(workflow).forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            val dependencies = analyzeStepDependencies(step)

            dependencies.forEach { (sourceId, targetId) ->
                if (sourceId == "input") {
                    edges.add(DataFlowEdge("input", targetId, "input"))
                } else {
                    edges.add(DataFlowEdge(sourceId, targetId, "data"))
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { it.after.contains(step.id) }) {
                edges.add(DataFlowEdge(step.id, "output", "output"))
            }
        }

        return DataFlowGraph(nodes, edges)
    }

    /**
     * 分析步骤依赖关系。
     *
     * @param step 工作流步骤
     * @return 依赖关系列表，每个元素是一个Pair，first是源步骤ID，second是目标步骤ID
     */
    private fun analyzeStepDependencies(step: ai.kastrax.core.workflow.WorkflowStep): List<Pair<String, String>> {
        val dependencies = mutableListOf<Pair<String, String>>()

        // 添加显式依赖（after关系）
        step.after.forEach { afterStepId ->
            dependencies.add(Pair(afterStepId, step.id))
        }

        // 分析变量引用，找出隐式依赖
        step.variables.values.forEach { reference ->
            if (reference.path.startsWith("$.steps.")) {
                val parts = reference.path.removePrefix("$.steps.").split(".", limit = 2)
                if (parts.isNotEmpty()) {
                    val sourceStepId = parts[0]
                    dependencies.add(Pair(sourceStepId, step.id))
                }
            } else if (reference.path.startsWith("$.input.")) {
                dependencies.add(Pair("input", step.id))
            }
        }

        // 如果步骤实现了EnhancedVariableReferenceProvider，分析更多依赖
        if (step is EnhancedVariableReferenceProvider) {
            step.getEnhancedVariableReferences().forEach { reference ->
                when (reference.source) {
                    SourceType.STEP -> {
                        val parts = reference.path.split(".", limit = 2)
                        if (parts.isNotEmpty()) {
                            val sourceStepId = parts[0]
                            dependencies.add(Pair(sourceStepId, step.id))
                        }
                    }
                    SourceType.INPUT -> {
                        dependencies.add(Pair("input", step.id))
                    }
                    else -> { /* 忽略其他类型 */ }
                }
            }
        }

        return dependencies.distinct()
    }

    /**
     * 检查工作流执行结果。
     *
     * @param workflow 工作流
     * @param context 工作流上下文
     * @return 检查结果
     */
    fun inspectWorkflowExecution(workflow: Workflow, context: WorkflowContext): ExecutionInspectionResult {
        logger.info { "开始检查工作流执行结果: ${workflow.javaClass.simpleName}" }

        val result = ExecutionInspectionResult(workflow, context)

        // 检查步骤执行状态
        inspectStepExecutionStatus(workflow, context, result)

        // 检查数据流执行
        inspectDataFlowExecution(workflow, context, result)

        logger.info { "工作流执行结果检查完成: ${workflow.javaClass.simpleName}" }
        logger.info { "发现 ${result.issues.size} 个问题" }

        return result
    }

    /**
     * 检查步骤执行状态。
     */
    private fun inspectStepExecutionStatus(
        workflow: Workflow,
        context: WorkflowContext,
        result: ExecutionInspectionResult
    ) {
        logger.debug { "检查步骤执行状态" }

        // 检查失败的步骤
        context.steps.forEach { (stepId, stepResult) ->
            if (!stepResult.success) {
                result.addIssue(
                    IssueType.STEP_EXECUTION_FAILED,
                    "步骤 $stepId 执行失败: ${stepResult.error}",
                    stepId
                )
            }
        }

        // 检查未执行的步骤
        getWorkflowSteps(workflow).forEach { step ->
            if (step.id !in context.steps) {
                result.addIssue(
                    IssueType.STEP_NOT_EXECUTED,
                    "步骤 ${step.id} 未执行",
                    step.id
                )
            }
        }
    }

    /**
     * 检查数据流执行。
     */
    private fun inspectDataFlowExecution(
        workflow: Workflow,
        context: WorkflowContext,
        result: ExecutionInspectionResult
    ) {
        logger.debug { "检查数据流执行" }

        // 检查步骤输出是否为空
        context.steps.forEach { (stepId, stepResult) ->
            if (stepResult.success && stepResult.output.isEmpty()) {
                result.addIssue(
                    IssueType.EMPTY_STEP_OUTPUT,
                    "步骤 $stepId 的输出为空",
                    stepId
                )
            }
        }

        // 检查步骤输入是否被使用
        getWorkflowSteps(workflow).forEach { step ->
            val stepId = step.id
            val stepResult = context.steps[stepId]

            if (stepResult != null && stepResult.success) {
                // 检查步骤变量是否被使用
                step.variables.forEach { (name, reference) ->
                    val value = context.resolveReference(reference)

                    // 如果变量值为null，可能表示引用无效
                    if (value == null) {
                        result.addIssue(
                            IssueType.UNUSED_VARIABLE,
                            "步骤 $stepId 的变量 $name 值为null",
                            stepId
                        )
                    }
                }
            }
        }
    }

    /**
     * 检查结果类。
     */
    class InspectionResult(val workflow: Workflow) {
        val issues = mutableListOf<Issue>()

        /**
         * 添加问题。
         */
        fun addIssue(type: IssueType, message: String, stepId: String? = null) {
            issues.add(Issue(type, message, stepId))
        }

        /**
         * 获取特定类型的问题。
         */
        fun getIssuesByType(type: IssueType): List<Issue> {
            return issues.filter { it.type == type }
        }

        /**
         * 获取特定步骤的问题。
         */
        fun getIssuesByStep(stepId: String): List<Issue> {
            return issues.filter { it.stepId == stepId }
        }

        /**
         * 生成检查报告。
         */
        fun generateReport(): String {
            val sb = StringBuilder()
            sb.appendLine("=== 工作流检查报告 ===")
            sb.appendLine("工作流: ${workflow.javaClass.simpleName}")
            sb.appendLine("步骤数: ${getWorkflowSteps(workflow).size}")
            sb.appendLine("问题数: ${issues.size}")
            sb.appendLine()

            if (issues.isEmpty()) {
                sb.appendLine("未发现问题")
            } else {
                sb.appendLine("问题列表:")
                issues.forEachIndexed { index, issue ->
                    sb.appendLine("${index + 1}. [${issue.type}] ${issue.message}")
                    if (issue.stepId != null) {
                        sb.appendLine("   步骤: ${issue.stepId}")
                    }
                }
            }

            return sb.toString()
        }
    }

    /**
     * 执行检查结果类。
     */
    class ExecutionInspectionResult(val workflow: Workflow, val context: WorkflowContext) {
        val issues = mutableListOf<Issue>()

        /**
         * 添加问题。
         */
        fun addIssue(type: IssueType, message: String, stepId: String? = null) {
            issues.add(Issue(type, message, stepId))
        }

        /**
         * 获取特定类型的问题。
         */
        fun getIssuesByType(type: IssueType): List<Issue> {
            return issues.filter { it.type == type }
        }

        /**
         * 获取特定步骤的问题。
         */
        fun getIssuesByStep(stepId: String): List<Issue> {
            return issues.filter { it.stepId == stepId }
        }

        /**
         * 生成检查报告。
         */
        fun generateReport(): String {
            val sb = StringBuilder()
            sb.appendLine("=== 工作流执行检查报告 ===")
            sb.appendLine("工作流: ${workflow.javaClass.simpleName}")
            sb.appendLine("步骤数: ${getWorkflowSteps(workflow).size}")
            sb.appendLine("已执行步骤数: ${context.steps.size}")
            sb.appendLine("成功步骤数: ${context.steps.count { entry -> entry.value.success }}")
            sb.appendLine("失败步骤数: ${context.steps.count { entry -> !entry.value.success }}")
            sb.appendLine("问题数: ${issues.size}")
            sb.appendLine()

            if (issues.isEmpty()) {
                sb.appendLine("未发现问题")
            } else {
                sb.appendLine("问题列表:")
                issues.forEachIndexed { index, issue ->
                    sb.appendLine("${index + 1}. [${issue.type}] ${issue.message}")
                    if (issue.stepId != null) {
                        sb.appendLine("   步骤: ${issue.stepId}")
                    }
                }
            }

            return sb.toString()
        }
    }

    /**
     * 问题类。
     */
    data class Issue(
        val type: IssueType,
        val message: String,
        val stepId: String? = null
    )

    /**
     * 问题类型枚举。
     */
    enum class IssueType {
        /**
         * 循环依赖。
         */
        CYCLIC_DEPENDENCY,

        /**
         * 缺失依赖。
         */
        MISSING_DEPENDENCY,

        /**
         * 无效引用。
         */
        INVALID_REFERENCE,

        /**
         * 未使用的输出。
         */
        UNUSED_OUTPUT,

        /**
         * 未使用的输入。
         */
        UNUSED_INPUT,

        /**
         * 步骤执行失败。
         */
        STEP_EXECUTION_FAILED,

        /**
         * 步骤未执行。
         */
        STEP_NOT_EXECUTED,

        /**
         * 空步骤输出。
         */
        EMPTY_STEP_OUTPUT,

        /**
         * 未使用的变量。
         */
        UNUSED_VARIABLE
    }

    /**
     * 数据流图类。
     */
    data class DataFlowGraph(
        val nodes: List<DataFlowNode>,
        val edges: List<DataFlowEdge>
    )

    /**
     * 数据流节点类。
     */
    data class DataFlowNode(
        val id: String,
        val label: String,
        val type: NodeType
    )

    /**
     * 数据流边类。
     */
    data class DataFlowEdge(
        val source: String,
        val target: String,
        val label: String
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
         * 输出节点。
         */
        OUTPUT
    }
}
