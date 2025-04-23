package ai.kastrax.core.workflow.dataflow

import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.debug.DataFlowDebugger
import ai.kastrax.core.workflow.dataflow.debug.DataFlowInspector
import ai.kastrax.core.workflow.dataflow.debug.DataFlowTracer
import ai.kastrax.core.workflow.dataflow.visualization.DataFlowVisualizer

/**
 * 增强的工作流上下文，支持变量作用域和数据转换。
 *
 * @property workflowId 工作流ID
 * @property input 工作流输入
 * @property steps 已执行步骤的结果
 * @property scopeManager 变量作用域管理器
 * @property transformer 数据转换器
 * @property visualizer 数据流可视化器
 * @property debugger 数据流调试器
 * @property inspector 数据流检查器
 * @property tracer 数据流跟踪器
 * @property runId 运行ID
 */
class EnhancedWorkflowContext(
    val workflowId: String,
    val input: Map<String, Any?>,
    val steps: MutableMap<String, WorkflowStepResult> = mutableMapOf(),
    val scopeManager: VariableScopeManager = VariableScopeManager(),
    val transformer: DataTransformer = DataTransformer(),
    val visualizer: DataFlowVisualizer = DataFlowVisualizer(),
    val debugger: DataFlowDebugger = DataFlowDebugger(),
    val inspector: DataFlowInspector = DataFlowInspector(),
    val tracer: DataFlowTracer = DataFlowTracer(),
    val runId: String? = null
) {
    /**
     * 获取标准工作流上下文。
     */
    fun toStandardContext(): WorkflowContext {
        val globalScope = scopeManager.getGlobalScope()
        val workflowScope = scopeManager.getWorkflowScope(workflowId)

        val variables = mutableMapOf<String, Any?>()
        variables.putAll(globalScope.getAll())
        variables.putAll(workflowScope.getAll())

        return WorkflowContext(
            input = input,
            steps = steps,
            variables = variables
        )
    }

    /**
     * 从标准工作流上下文创建增强的工作流上下文。
     */
    companion object {
        fun fromStandardContext(
            context: WorkflowContext,
            workflowId: String,
            scopeManager: VariableScopeManager = VariableScopeManager(),
            transformer: DataTransformer = DataTransformer()
        ): EnhancedWorkflowContext {
            // 将标准上下文中的变量复制到作用域管理器中
            val globalScope = scopeManager.getGlobalScope()
            val workflowScope = scopeManager.getWorkflowScope(workflowId)

            for ((key, value) in context.variables) {
                workflowScope.set(key, value)
            }

            return EnhancedWorkflowContext(
                workflowId = workflowId,
                input = context.input,
                steps = context.steps,
                scopeManager = scopeManager,
                transformer = transformer,
                runId = context.runId
            )
        }
    }

    /**
     * 获取全局变量。
     *
     * @param name 变量名
     * @return 变量值
     */
    fun getGlobalVariable(name: String): Any? {
        return scopeManager.getGlobalScope().get(name)
    }

    /**
     * 设置全局变量。
     *
     * @param name 变量名
     * @param value 变量值
     */
    fun setGlobalVariable(name: String, value: Any?) {
        scopeManager.getGlobalScope().set(name, value)
    }

    /**
     * 获取工作流变量。
     *
     * @param name 变量名
     * @return 变量值
     */
    fun getWorkflowVariable(name: String): Any? {
        return scopeManager.getWorkflowScope(workflowId).get(name)
    }

    /**
     * 设置工作流变量。
     *
     * @param name 变量名
     * @param value 变量值
     */
    fun setWorkflowVariable(name: String, value: Any?) {
        scopeManager.getWorkflowScope(workflowId).set(name, value)
    }

    /**
     * 获取步骤变量。
     *
     * @param stepId 步骤ID
     * @param name 变量名
     * @return 变量值
     */
    fun getStepVariable(stepId: String, name: String): Any? {
        return scopeManager.getStepScope(workflowId, stepId).get(name)
    }

    /**
     * 设置步骤变量。
     *
     * @param stepId 步骤ID
     * @param name 变量名
     * @param value 变量值
     */
    fun setStepVariable(stepId: String, name: String, value: Any?) {
        scopeManager.getStepScope(workflowId, stepId).set(name, value)
    }

    /**
     * 获取步骤输出。
     *
     * @param stepId 步骤ID
     * @return 步骤输出
     */
    fun getStepOutput(stepId: String): Map<String, Any?>? {
        return steps[stepId]?.output
    }

    /**
     * 获取步骤输出字段。
     *
     * @param stepId 步骤ID
     * @param field 字段名
     * @return 字段值
     */
    fun getStepOutputField(stepId: String, field: String): Any? {
        return steps[stepId]?.output?.get(field)
    }

    /**
     * 解析变量引用。
     *
     * @param reference 变量引用
     * @param stepId 当前步骤ID
     * @return 解析后的值
     */
    fun resolveReference(reference: EnhancedVariableReference, stepId: String? = null): Any? {
        val resolver = VariableResolver(scopeManager)
        return resolver.resolve(reference, toStandardContext(), workflowId, stepId)
    }

    /**
     * 解析变量映射。
     *
     * @param variables 变量映射
     * @param stepId 当前步骤ID
     * @return 解析后的变量映射
     */
    fun resolveVariables(
        variables: Map<String, EnhancedVariableReference>,
        stepId: String? = null
    ): Map<String, Any?> {
        return variables.mapValues { (_, reference) ->
            resolveReference(reference, stepId)
        }
    }

    /**
     * 映射数据。
     *
     * @param input 输入数据
     * @param mapping 映射规则
     * @param stepId 当前步骤ID
     * @return 映射后的数据
     */
    fun mapData(
        input: Any?,
        mapping: Map<String, EnhancedVariableReference>,
        stepId: String? = null
    ): Map<String, Any?> {
        return transformer.map(input, mapping, toStandardContext(), workflowId, stepId)
    }

    /**
     * 过滤列表。
     *
     * @param input 输入列表
     * @param predicate 过滤条件
     * @return 过滤后的列表
     */
    fun filterList(input: List<*>, predicate: (Any?) -> Boolean): List<*> {
        return transformer.filter(input, predicate)
    }

    /**
     * 过滤映射。
     *
     * @param input 输入映射
     * @param predicate 过滤条件
     * @return 过滤后的映射
     */
    fun filterMap(input: Map<*, *>, predicate: (Map.Entry<*, *>) -> Boolean): Map<*, *> {
        return transformer.filter(input, predicate)
    }

    /**
     * 聚合列表。
     *
     * @param input 输入列表
     * @param initialValue 初始值
     * @param operation 聚合操作
     * @return 聚合结果
     */
    fun <T, R> aggregateList(input: List<T>, initialValue: R, operation: (R, T) -> R): R {
        return transformer.aggregate(input, initialValue, operation)
    }

    /**
     * 可视化工作流数据流。
     *
     * @param workflow 工作流
     * @param format 可视化格式
     * @param includeValues 是否包含变量值
     * @return 可视化表示
     */
    fun visualizeDataFlow(
        workflow: Workflow,
        format: DataFlowVisualizer.VisualizationFormat = DataFlowVisualizer.VisualizationFormat.MERMAID,
        includeValues: Boolean = false
    ): String {
        return visualizer.visualize(workflow, format, includeValues)
    }

    /**
     * 可视化工作流执行的数据流。
     *
     * @param workflow 工作流
     * @param format 可视化格式
     * @param includeValues 是否包含变量值
     * @return 可视化表示
     */
    fun visualizeExecutionDataFlow(
        workflow: Workflow,
        format: DataFlowVisualizer.VisualizationFormat = DataFlowVisualizer.VisualizationFormat.MERMAID,
        includeValues: Boolean = true
    ): String {
        return visualizer.visualizeExecution(workflow, toStandardContext(), format, includeValues)
    }

    /**
     * 调试工作流执行。
     *
     * @param workflow 工作流
     * @param mode 调试模式
     * @param breakpoints 断点列表
     * @param outputDir 输出目录
     * @return 工作流执行结果
     */
    suspend fun debugWorkflow(
        workflow: Workflow,
        mode: DataFlowDebugger.DebugMode = DataFlowDebugger.DebugMode.REPORT,
        breakpoints: List<String> = emptyList(),
        outputDir: String = "debug_output"
    ): ai.kastrax.core.workflow.WorkflowResult {
        return debugger.debugWorkflow(workflow, input, mode, breakpoints, outputDir)
    }

    /**
     * 检查工作流数据流。
     *
     * @param workflow 工作流
     * @return 检查结果
     */
    fun inspectDataFlow(workflow: Workflow): DataFlowInspector.InspectionResult {
        return inspector.inspectWorkflow(workflow)
    }

    /**
     * 检查工作流执行结果。
     *
     * @param workflow 工作流
     * @return 检查结果
     */
    fun inspectExecutionDataFlow(workflow: Workflow): DataFlowInspector.ExecutionInspectionResult {
        return inspector.inspectWorkflowExecution(workflow, toStandardContext())
    }

    /**
     * 跟踪工作流执行的数据流。
     *
     * @param workflow 工作流
     * @return 跟踪结果
     */
    fun traceDataFlow(workflow: Workflow): DataFlowTracer.TraceResult {
        return tracer.traceWorkflowExecution(workflow, toStandardContext())
    }

    /**
     * 跟踪变量值的变化。
     *
     * @param workflow 工作流
     * @param variableName 变量名
     * @return 变量跟踪结果
     */
    fun traceVariable(workflow: Workflow, variableName: String): DataFlowTracer.VariableTraceResult {
        return tracer.traceVariable(workflow, variableName, toStandardContext())
    }
}
