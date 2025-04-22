package ai.kastrax.core.workflow

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Agent链工作流步骤
 *
 * @property id 步骤ID
 * @property name 步骤名称
 * @property description 步骤描述
 * @property chain Agent链
 * @property after 依赖的步骤ID
 * @property variables 变量引用
 * @property outputMapping 输出映射函数
 * @property condition 条件函数
 * @property config 步骤配置
 */
class AgentChainWorkflowStep(
    override val id: String,
    override val name: String = id,
    override val description: String = "",
    val chain: AgentChain,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    val outputMapping: (String) -> Map<String, Any?> = { mapOf("text" to it) },
    override val condition: (WorkflowContext) -> Boolean = { true },
    override val config: StepConfig? = null
) : WorkflowStep {

    /**
     * 执行Agent链步骤
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val startTime = System.currentTimeMillis()
        val executionTime = { System.currentTimeMillis() - startTime }

        return try {
            // 解析变量
            val resolvedVariables = context.resolveVariables(variables)

            // 构建输入
            val input = buildPrompt(resolvedVariables)

            // 执行Agent链
            val result = chain.execute(input, AgentChainExecuteOptions())

            if (!result.success) {
                // 失败结果
                createErrorResult(result.error ?: "Agent链执行失败", executionTime())
            } else {
                // 成功结果
                val output = outputMapping(result.output)
                createSuccessResult(output, executionTime())
            }
        } catch (e: Exception) {
            // 异常结果
            createErrorResult(e.message ?: "Unknown error", executionTime())
        }
    }

    /**
     * 创建成功结果
     */
    private fun createSuccessResult(output: Map<String, Any?>, executionTime: Long): WorkflowStepResult {
        return WorkflowStepResult(
            stepId = id,
            success = true,
            output = output,
            executionTime = executionTime
        )
    }

    /**
     * 创建错误结果
     */
    private fun createErrorResult(error: String, executionTime: Long): WorkflowStepResult {
        return WorkflowStepResult(
            stepId = id,
            success = false,
            output = emptyMap(),
            error = error,
            executionTime = executionTime
        )
    }

    /**
     * 构建提示
     */
    private fun buildPrompt(variables: Map<String, Any?>): String {
        // 按优先级查找变量
        val promptValue = when {
            variables.containsKey("prompt") -> variables["prompt"].toString()
            variables.containsKey("input") -> variables["input"].toString()
            else -> ""
        }

        return promptValue
    }
}

/**
 * 工作流构建器的扩展函数，用于添加Agent链步骤
 */
fun WorkflowBuilder.agentChainStep(
    id: String,
    chain: AgentChain,
    after: List<String> = emptyList(),
    init: AgentChainStepBuilder.() -> Unit = {}
) {
    val builder = AgentChainStepBuilder(id, chain, after)
    builder.init()
    val step = builder.build()
    step(step)
}

/**
 * 工作流构建器的扩展函数，用于添加工作流步骤
 */
fun WorkflowBuilder.step(step: WorkflowStep) {
    // 使用反射来访问私有属性
    try {
        val stepsField = this.javaClass.getDeclaredField("steps")
        stepsField.isAccessible = true
        val stepsMap = stepsField.get(this) as MutableMap<String, WorkflowStep>
        stepsMap[step.id] = step
    } catch (e: Exception) {
        error("无法添加步骤: ${e.message}")
    }
}

/**
 * Agent链步骤构建器
 */
class AgentChainStepBuilder(
    val id: String,
    val chain: AgentChain,
    val after: List<String> = emptyList()
) {
    var name: String = id
    var description: String = ""
    val variables: MutableMap<String, VariableReference> = mutableMapOf()
    var outputMapping: (String) -> Map<String, Any?> = { mapOf("text" to it) }
    var condition: (WorkflowContext) -> Boolean = { true }
    var config: StepConfig? = null

    /**
     * 添加变量引用
     */
    fun variable(name: String, reference: String) {
        variables[name] = VariableReference(reference)
    }

    /**
     * 构建Agent链步骤
     */
    fun build(): WorkflowStep {
        return AgentChainWorkflowStep(
            id = id,
            name = name,
            description = description,
            chain = chain,
            after = after,
            variables = variables,
            outputMapping = outputMapping,
            condition = condition,
            config = config
        )
    }
}
