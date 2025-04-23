package ai.kastrax.core.workflow.dynamic

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowBuilder
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import mu.KotlinLogging

/**
 * 动态工作流生成器，支持在运行时动态创建工作流。
 */
class DynamicWorkflowGenerator {
    private val logger = KotlinLogging.logger {}
    
    /**
     * 从步骤列表创建工作流。
     *
     * @param workflowName 工作流名称
     * @param description 工作流描述
     * @param steps 步骤列表
     * @return 创建的工作流
     */
    fun createWorkflow(
        workflowName: String,
        description: String,
        steps: List<WorkflowStep>
    ): Workflow {
        logger.info { "动态创建工作流: $workflowName, 步骤数: ${steps.size}" }
        
        val stepsMap = steps.associateBy { it.id }
        
        return SimpleWorkflow(
            workflowName = workflowName,
            description = description,
            steps = stepsMap
        )
    }
    
    /**
     * 使用DSL创建工作流。
     *
     * @param workflowName 工作流名称
     * @param description 工作流描述
     * @param init 工作流构建器初始化函数
     * @return 创建的工作流
     */
    fun createWorkflow(
        workflowName: String,
        description: String,
        init: WorkflowBuilder.() -> Unit
    ): Workflow {
        logger.info { "使用DSL动态创建工作流: $workflowName" }
        
        val builder = WorkflowBuilder()
        builder.name = workflowName
        builder.description = description
        builder.init()
        
        return builder.build()
    }
    
    /**
     * 创建动态步骤。
     *
     * @param id 步骤ID
     * @param name 步骤名称
     * @param description 步骤描述
     * @param after 前置步骤ID列表
     * @param variables 步骤输入变量映射
     * @param executeFunction 执行函数
     * @param condition 条件函数
     * @param config 步骤配置
     * @return 创建的步骤
     */
    fun createDynamicStep(
        id: String,
        name: String,
        description: String,
        after: List<String> = emptyList(),
        variables: Map<String, VariableReference> = emptyMap(),
        executeFunction: suspend (WorkflowContext) -> WorkflowStepResult,
        condition: (WorkflowContext) -> Boolean = { true },
        config: StepConfig? = null
    ): WorkflowStep {
        return DynamicStep(
            id = id,
            name = name,
            description = description,
            after = after,
            variables = variables,
            executeFunction = executeFunction,
            condition = condition,
            config = config
        )
    }
    
    /**
     * 动态步骤，支持在运行时定义执行逻辑。
     */
    private class DynamicStep(
        override val id: String,
        override val name: String,
        override val description: String,
        override val after: List<String>,
        override val variables: Map<String, VariableReference>,
        private val executeFunction: suspend (WorkflowContext) -> WorkflowStepResult,
        override val condition: (WorkflowContext) -> Boolean,
        override val config: StepConfig?
    ) : WorkflowStep {
        
        /**
         * 执行步骤。
         *
         * @param context 工作流上下文
         * @return 步骤执行结果
         */
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            return executeFunction(context)
        }
    }
}

/**
 * 动态步骤构建器。
 */
class DynamicStepBuilder {
    var id: String = ""
    var name: String = ""
    var description: String = ""
    var after: MutableList<String> = mutableListOf()
    var variables: Map<String, VariableReference> = mutableMapOf()
    var executeFunction: suspend (WorkflowContext) -> WorkflowStepResult = { 
        WorkflowStepResult.success(id, emptyMap()) 
    }
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
     * 设置执行函数。
     */
    fun execute(block: suspend (WorkflowContext) -> WorkflowStepResult) {
        executeFunction = block
    }
    
    /**
     * 构建动态步骤。
     */
    fun build(): WorkflowStep {
        require(id.isNotEmpty()) { "Step ID must not be empty" }
        
        return DynamicWorkflowGenerator().createDynamicStep(
            id = id,
            name = name.ifEmpty { id },
            description = description,
            after = after,
            variables = variables,
            executeFunction = executeFunction,
            condition = condition,
            config = config
        )
    }
}

/**
 * 工作流构建器的扩展函数，用于添加动态步骤。
 */
fun WorkflowBuilder.dynamicStep(init: DynamicStepBuilder.() -> Unit) {
    val builder = DynamicStepBuilder()
    builder.init()
    val step = builder.build()
    
    // 使用反射访问私有字段
    val stepsField = this::class.java.getDeclaredField("steps")
    stepsField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val steps = stepsField.get(this) as MutableMap<String, WorkflowStep>
    steps[step.id] = step
}
