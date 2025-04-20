package ai.kastrax.core.workflow

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * 工作流接口，定义了工作流的基本操作。
 */
interface Workflow {
    /**
     * 执行工作流。
     *
     * @param input 工作流的输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    suspend fun execute(
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): WorkflowResult

    /**
     * 流式执行工作流，返回执行状态的流。
     *
     * @param input 工作流的输入数据
     * @param options 执行选项
     * @return 工作流状态更新的流
     */
    suspend fun streamExecute(
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): Flow<WorkflowStatusUpdate>
}

/**
 * 工作流执行选项。
 *
 * @property maxSteps 最大执行步骤数
 * @property timeout 超时时间（毫秒）
 * @property onStepFinish 步骤完成回调
 * @property onStepError 步骤错误回调
 * @property threadId 可选的线程ID（用于内存系统）
 */
data class WorkflowExecuteOptions(
    val maxSteps: Int = 10,
    val timeout: Long = 60000,
    val onStepFinish: ((WorkflowStepResult) -> Unit)? = null,
    val onStepError: ((String, Throwable) -> Unit)? = null,
    val threadId: String? = null
)

/**
 * 工作流执行结果。
 *
 * @property success 是否成功
 * @property output 输出数据
 * @property steps 执行的步骤结果
 * @property error 错误信息（如果失败）
 * @property executionTime 执行时间（毫秒）
 */
data class WorkflowResult(
    val success: Boolean,
    val output: Map<String, Any?>,
    val steps: Map<String, WorkflowStepResult>,
    val error: String? = null,
    val executionTime: Long = 0
)

/**
 * 工作流步骤结果。
 *
 * @property stepId 步骤ID
 * @property success 是否成功
 * @property output 输出数据
 * @property error 错误信息（如果失败）
 * @property executionTime 执行时间（毫秒）
 */
data class WorkflowStepResult(
    val stepId: String,
    val success: Boolean,
    val output: Map<String, Any?>,
    val error: String? = null,
    val executionTime: Long = 0
)

/**
 * 工作流状态更新。
 *
 * @property status 状态（开始、进行中、完成、失败）
 * @property stepId 当前步骤ID
 * @property message 状态消息
 * @property progress 进度（0-100）
 * @property result 步骤结果（如果有）
 */
data class WorkflowStatusUpdate(
    val status: WorkflowStatus,
    val stepId: String? = null,
    val message: String = "",
    val progress: Int = 0,
    val result: WorkflowStepResult? = null
)

/**
 * 工作流状态枚举。
 */
enum class WorkflowStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

/**
 * 工作流步骤接口。
 */
interface WorkflowStep {
    /**
     * 步骤ID。
     */
    val id: String

    /**
     * 步骤名称。
     */
    val name: String

    /**
     * 步骤描述。
     */
    val description: String

    /**
     * 前置步骤ID列表。
     */
    val after: List<String>

    /**
     * 步骤输入变量映射。
     */
    val variables: Map<String, VariableReference>

    /**
     * 条件函数，决定是否执行步骤。
     * 默认总是返回 true。
     */
    val condition: (WorkflowContext) -> Boolean
        get() = { true }

    /**
     * 执行步骤。
     *
     * @param context 工作流上下文
     * @return 步骤执行结果
     */
    suspend fun execute(context: WorkflowContext): WorkflowStepResult
}

/**
 * 变量引用，用于在步骤之间传递数据。
 *
 * @property path JSON路径表达式
 */
data class VariableReference(
    val path: String
)

/**
 * 工作流上下文，包含工作流执行的状态和数据。
 *
 * @property input 工作流输入
 * @property steps 已执行步骤的结果
 * @property variables 全局变量
 */
data class WorkflowContext(
    val input: Map<String, Any?>,
    val steps: MutableMap<String, WorkflowStepResult> = mutableMapOf(),
    val variables: MutableMap<String, Any?> = mutableMapOf()
) {
    /**
     * 解析变量引用。
     *
     * @param reference 变量引用
     * @return 解析后的值
     */
    fun resolveReference(reference: VariableReference): Any? {
        val path = reference.path

        // 处理简单路径
        if (path.startsWith("$.input.")) {
            val key = path.removePrefix("$.input.")
            return input[key]
        }

        if (path.startsWith("$.steps.")) {
            val parts = path.removePrefix("$.steps.").split(".")
            if (parts.size >= 2) {
                val stepId = parts[0]
                val outputKey = parts[1]
                val stepResult = steps[stepId]
                if (stepResult != null) {
                    if (outputKey == "output") {
                        return stepResult.output
                    } else {
                        return stepResult.output[outputKey.removePrefix("output.")]
                    }
                }
            }
        }

        if (path.startsWith("$.variables.")) {
            val key = path.removePrefix("$.variables.")
            return variables[key]
        }

        // 如果是常量值，直接返回
        if (!path.startsWith("$")) {
            return path
        }

        return null
    }

    /**
     * 解析变量映射。
     *
     * @param variables 变量映射
     * @return 解析后的变量映射
     */
    fun resolveVariables(variables: Map<String, VariableReference>): Map<String, Any?> {
        return variables.mapValues { (_, reference) ->
            resolveReference(reference)
        }
    }
}

/**
 * 代理步骤，使用AI代理执行任务。
 *
 * @property id 步骤ID
 * @property name 步骤名称
 * @property description 步骤描述
 * @property agent AI代理
 * @property after 前置步骤ID列表
 * @property variables 步骤输入变量映射
 * @property outputMapping 输出映射函数
 */
class AgentStep(
    override val id: String,
    override val name: String = id,
    override val description: String = "",
    val agent: Agent,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    val outputMapping: (String) -> Map<String, Any?> = { mapOf("text" to it) },
    override val condition: (WorkflowContext) -> Boolean = { true }
) : WorkflowStep {

    /**
     * 执行代理步骤。
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val startTime = System.currentTimeMillis()

        try {
            // 解析变量
            val resolvedVariables = context.resolveVariables(variables)

            // 构建提示
            val prompt = buildPrompt(resolvedVariables)

            // 执行代理
            val response = agent.generate(prompt)

            // 映射输出
            val output = outputMapping(response.text)

            val executionTime = System.currentTimeMillis() - startTime

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = output,
                executionTime = executionTime
            )
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime

            return WorkflowStepResult(
                stepId = id,
                success = false,
                output = emptyMap(),
                error = e.message ?: "Unknown error",
                executionTime = executionTime
            )
        }
    }

    /**
     * 构建代理提示。
     */
    private fun buildPrompt(variables: Map<String, Any?>): String {
        // 简单实现：将变量转换为字符串
        return variables.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }
    }
}

/**
 * 工作流构建器。
 */
class WorkflowBuilder {
    var name: String = ""
    var description: String = ""
    private val steps = mutableMapOf<String, WorkflowStep>()

    /**
     * 添加代理步骤。
     */
    fun step(agent: Agent, init: AgentStepBuilder.() -> Unit) {
        val builder = AgentStepBuilder(agent)
        builder.init()
        val step = builder.build()
        steps[step.id] = step
    }

    /**
     * 构建工作流。
     */
    fun build(): SimpleWorkflow {
        require(name.isNotEmpty()) { "Workflow name must not be empty" }

        return SimpleWorkflow(
            workflowName = name,
            description = description,
            steps = steps
        )
    }

    /**
     * 代理步骤构建器。
     */
    class AgentStepBuilder(private val agent: Agent) {
        var id: String = ""
        var name: String = ""
        var description: String = ""
        var after: MutableList<String> = mutableListOf()
        var variables: Map<String, VariableReference> = mutableMapOf()
        var outputMapping: (String) -> Map<String, Any?> = { mapOf("text" to it) }
        var condition: (WorkflowContext) -> Boolean = { true }

        /**
         * 设置前置步骤。
         */
        fun after(vararg stepIds: String) {
            after.addAll(stepIds)
        }

        /**
         * 设置变量引用。
         */
        fun variable(path: String): VariableReference {
            return VariableReference(path)
        }

        /**
         * 构建代理步骤。
         */
        fun build(): AgentStep {
            require(id.isNotEmpty()) { "Step ID must not be empty" }

            return AgentStep(
                id = id,
                name = name.ifEmpty { id },
                description = description,
                agent = agent,
                after = after,
                variables = variables,
                outputMapping = outputMapping,
                condition = condition
            )
        }
    }
}

/**
 * 简单工作流实现。
 *
 * @property description 工作流描述
 * @property steps 工作流步骤
 */
class SimpleWorkflow(
    workflowName: String,
    val description: String = "",
    val steps: Map<String, WorkflowStep>
) : KastraXBase(component = "WORKFLOW", name = workflowName), Workflow {

    /**
     * 执行工作流。
     */
    override suspend fun execute(
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions
    ): WorkflowResult {
        val startTime = System.currentTimeMillis()
        val context = WorkflowContext(input = input)
        val executedSteps = mutableMapOf<String, WorkflowStepResult>()

        try {
            // 计算步骤执行顺序
            val executionOrder = computeExecutionOrder()

            // 执行步骤
            for (stepId in executionOrder) {
                val step = steps[stepId] ?: continue

                // 检查是否超过最大步骤数
                if (executedSteps.size >= options.maxSteps) {
                    return WorkflowResult(
                        success = false,
                        output = emptyMap(),
                        steps = executedSteps,
                        error = "Maximum number of steps exceeded",
                        executionTime = System.currentTimeMillis() - startTime
                    )
                }

                // 检查是否超时
                if (System.currentTimeMillis() - startTime > options.timeout) {
                    return WorkflowResult(
                        success = false,
                        output = emptyMap(),
                        steps = executedSteps,
                        error = "Workflow execution timed out",
                        executionTime = System.currentTimeMillis() - startTime
                    )
                }

                // 检查条件
                if (!step.condition(context)) {
                    logger.debug { "Skipping step $stepId due to condition" }
                    continue
                }

                // 执行步骤
                try {
                    val stepResult = step.execute(context)

                    // 调用步骤完成回调
                    options.onStepFinish?.invoke(stepResult)

                    // 如果步骤失败，终止工作流
                    if (!stepResult.success) {
                        return WorkflowResult(
                            success = false,
                            output = emptyMap(),
                            steps = executedSteps,
                            error = "Step $stepId failed: ${stepResult.error}",
                            executionTime = System.currentTimeMillis() - startTime
                        )
                    }

                    // 只有成功的步骤才添加到执行步骤和上下文中
                    executedSteps[stepId] = stepResult
                    context.steps[stepId] = stepResult
                } catch (e: Exception) {
                    // 调用步骤错误回调
                    options.onStepError?.invoke(stepId, e)

                    return WorkflowResult(
                        success = false,
                        output = emptyMap(),
                        steps = executedSteps,
                        error = "Error executing step $stepId: ${e.message}",
                        executionTime = System.currentTimeMillis() - startTime
                    )
                }
            }

            // 收集最终输出
            val output = collectOutput(executedSteps)

            return WorkflowResult(
                success = true,
                output = output,
                steps = executedSteps,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            return WorkflowResult(
                success = false,
                output = emptyMap(),
                steps = executedSteps,
                error = "Workflow execution failed: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 流式执行工作流。
     */
    override suspend fun streamExecute(
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions
    ): Flow<WorkflowStatusUpdate> = flow {
        val startTime = System.currentTimeMillis()
        val context = WorkflowContext(input = input)
        val executedSteps = mutableMapOf<String, WorkflowStepResult>()

        // 发送开始状态
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.STARTED,
            message = "Starting workflow execution"
        ))

        try {
            // 计算步骤执行顺序
            val executionOrder = computeExecutionOrder()
            val totalSteps = executionOrder.size

            // 执行步骤
            for ((index, stepId) in executionOrder.withIndex()) {
                val step = steps[stepId] ?: continue

                // 发送进行中状态
                emit(WorkflowStatusUpdate(
                    status = WorkflowStatus.IN_PROGRESS,
                    stepId = stepId,
                    message = "Executing step $stepId",
                    progress = ((index.toDouble() / totalSteps) * 100).toInt()
                ))

                // 检查是否超过最大步骤数
                if (executedSteps.size >= options.maxSteps) {
                    emit(WorkflowStatusUpdate(
                        status = WorkflowStatus.FAILED,
                        message = "Maximum number of steps exceeded"
                    ))
                    return@flow
                }

                // 检查是否超时
                if (System.currentTimeMillis() - startTime > options.timeout) {
                    emit(WorkflowStatusUpdate(
                        status = WorkflowStatus.FAILED,
                        message = "Workflow execution timed out"
                    ))
                    return@flow
                }

                // 检查条件
                if (!step.condition(context)) {
                    logger.debug { "Skipping step $stepId due to condition" }
                    continue
                }

                // 执行步骤
                try {
                    val stepResult = step.execute(context)

                    // 调用步骤完成回调
                    options.onStepFinish?.invoke(stepResult)

                    // 发送步骤完成状态
                    emit(WorkflowStatusUpdate(
                        status = if (stepResult.success) WorkflowStatus.IN_PROGRESS else WorkflowStatus.FAILED,
                        stepId = stepId,
                        message = if (stepResult.success) "Step $stepId completed" else "Step $stepId failed: ${stepResult.error}",
                        progress = ((index + 1.0) / totalSteps * 100).toInt(),
                        result = stepResult
                    ))

                    // 如果步骤失败，终止工作流
                    if (!stepResult.success) {
                        emit(WorkflowStatusUpdate(
                            status = WorkflowStatus.FAILED,
                            message = "Workflow failed: Step $stepId failed"
                        ))
                        return@flow
                    }

                    // 只有成功的步骤才添加到执行步骤和上下文中
                    executedSteps[stepId] = stepResult
                    context.steps[stepId] = stepResult
                } catch (e: Exception) {
                    // 调用步骤错误回调
                    options.onStepError?.invoke(stepId, e)

                    // 发送错误状态
                    emit(WorkflowStatusUpdate(
                        status = WorkflowStatus.FAILED,
                        stepId = stepId,
                        message = "Error executing step $stepId: ${e.message}"
                    ))

                    return@flow
                }
            }

            // 收集最终输出
            val output = collectOutput(executedSteps)

            // 发送完成状态
            emit(WorkflowStatusUpdate(
                status = WorkflowStatus.COMPLETED,
                message = "Workflow execution completed",
                progress = 100
            ))
        } catch (e: Exception) {
            // 发送失败状态
            emit(WorkflowStatusUpdate(
                status = WorkflowStatus.FAILED,
                message = "Workflow execution failed: ${e.message}"
            ))
        }
    }

    /**
     * 计算步骤执行顺序。
     */
    private fun computeExecutionOrder(): List<String> {
        val visited = mutableSetOf<String>()
        val order = mutableListOf<String>()

        // 拓扑排序
        fun visit(stepId: String) {
            if (stepId in visited) return
            visited.add(stepId)

            val step = steps[stepId] ?: return
            for (dependencyId in step.after) {
                visit(dependencyId)
            }

            order.add(stepId)
        }

        // 访问所有步骤
        for (stepId in steps.keys) {
            visit(stepId)
        }

        return order
    }

    /**
     * 收集工作流输出。
     */
    private fun collectOutput(executedSteps: Map<String, WorkflowStepResult>): Map<String, Any?> {
        // 简单实现：收集所有步骤的输出
        val output = mutableMapOf<String, Any?>()

        for ((stepId, stepResult) in executedSteps) {
            if (stepResult.success) {
                output[stepId] = stepResult.output
            }
        }

        return output
    }
}

/**
 * DSL函数，用于创建工作流。
 */
fun workflow(init: WorkflowBuilder.() -> Unit): Workflow {
    val builder = WorkflowBuilder()
    builder.init()
    return builder.build()
}
