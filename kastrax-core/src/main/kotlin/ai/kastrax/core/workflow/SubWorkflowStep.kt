package ai.kastrax.core.workflow

import ai.kastrax.core.workflow.engine.WorkflowEngine
import mu.KotlinLogging

/**
 * 子工作流步骤，允许在工作流中嵌套执行其他工作流。
 *
 * @property id 步骤ID
 * @property name 步骤名称
 * @property description 步骤描述
 * @property after 前置步骤ID列表
 * @property variables 步骤输入变量映射
 * @property workflowId 要执行的子工作流ID
 * @property inputMapping 输入映射函数，将父工作流上下文映射到子工作流输入
 * @property outputMapping 输出映射函数，将子工作流结果映射到步骤输出
 * @property workflowEngine 工作流引擎实例
 * @property condition 条件函数，决定是否执行步骤
 * @property config 步骤配置
 */
class SubWorkflowStep(
    override val id: String,
    override val name: String,
    override val description: String,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    val workflowId: String,
    val inputMapping: (WorkflowContext) -> Map<String, Any?> = { mapOf("parentContext" to it) },
    val outputMapping: (WorkflowResult) -> Map<String, Any?> = { it.output },
    private val workflowEngine: WorkflowEngine,
    override val condition: (WorkflowContext) -> Boolean = { true },
    override val config: StepConfig? = null
) : WorkflowStep {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 执行子工作流步骤。
     *
     * @param context 父工作流上下文
     * @return 步骤执行结果
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val startTime = System.currentTimeMillis()
        
        try {
            logger.info { "执行子工作流步骤: $id, 子工作流: $workflowId" }
            
            // 映射输入
            val subWorkflowInput = inputMapping(context)
            
            // 执行子工作流
            val subWorkflowResult = workflowEngine.executeWorkflow(
                workflowId = workflowId,
                input = subWorkflowInput,
                options = WorkflowExecuteOptions(
                    threadId = context.runId // 使用父工作流的runId作为线程ID，以便在内存系统中关联
                )
            )
            
            // 检查子工作流执行结果
            if (!subWorkflowResult.success) {
                val executionTime = System.currentTimeMillis() - startTime
                return WorkflowStepResult(
                    stepId = id,
                    success = false,
                    output = mapOf("subWorkflowError" to (subWorkflowResult.error ?: "Unknown error")),
                    error = "子工作流执行失败: ${subWorkflowResult.error}",
                    executionTime = executionTime
                )
            }
            
            // 映射输出
            val output = outputMapping(subWorkflowResult)
            
            // 添加子工作流的详细信息到输出
            val enhancedOutput = output.toMutableMap().apply {
                this["subWorkflowId"] = workflowId
                this["subWorkflowRunId"] = subWorkflowResult.runId
                this["subWorkflowSteps"] = subWorkflowResult.steps.size
                this["subWorkflowExecutionTime"] = subWorkflowResult.executionTime
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = enhancedOutput,
                executionTime = executionTime
            )
        } catch (e: Exception) {
            logger.error(e) { "子工作流步骤执行失败: $id" }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            return WorkflowStepResult(
                stepId = id,
                success = false,
                output = emptyMap(),
                error = "子工作流步骤执行异常: ${e.message}",
                executionTime = executionTime
            )
        }
    }
}

/**
 * 子工作流步骤构建器。
 */
class SubWorkflowStepBuilder(private val workflowEngine: WorkflowEngine) {
    var id: String = ""
    var name: String = ""
    var description: String = ""
    var after: MutableList<String> = mutableListOf()
    var variables: Map<String, VariableReference> = mutableMapOf()
    var workflowId: String = ""
    var inputMapping: (WorkflowContext) -> Map<String, Any?> = { mapOf("parentContext" to it) }
    var outputMapping: (WorkflowResult) -> Map<String, Any?> = { it.output }
    var condition: (WorkflowContext) -> Boolean = { true }
    var config: StepConfig? = null
    
    /**
     * 设置前置步骤。
     */
    fun after(vararg stepIds: String) {
        after.addAll(stepIds)
    }
    
    /**
     * 设置输入变量。
     */
    fun variables(init: MutableMap<String, VariableReference>.() -> Unit) {
        val map = mutableMapOf<String, VariableReference>()
        map.init()
        variables = map
    }
    
    /**
     * 构建子工作流步骤。
     */
    fun build(): SubWorkflowStep {
        require(id.isNotEmpty()) { "Step ID must not be empty" }
        require(workflowId.isNotEmpty()) { "Workflow ID must not be empty" }
        
        return SubWorkflowStep(
            id = id,
            name = name.ifEmpty { id },
            description = description,
            after = after,
            variables = variables,
            workflowId = workflowId,
            inputMapping = inputMapping,
            outputMapping = outputMapping,
            workflowEngine = workflowEngine,
            condition = condition,
            config = config
        )
    }
}

/**
 * 工作流构建器的扩展函数，用于添加子工作流步骤。
 */
fun WorkflowBuilder.subWorkflow(workflowEngine: WorkflowEngine, init: SubWorkflowStepBuilder.() -> Unit) {
    val builder = SubWorkflowStepBuilder(workflowEngine)
    builder.init()
    val step = builder.build()
    
    // 使用反射访问私有字段
    val stepsField = this::class.java.getDeclaredField("steps")
    stepsField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val steps = stepsField.get(this) as MutableMap<String, WorkflowStep>
    steps[step.id] = step
}
