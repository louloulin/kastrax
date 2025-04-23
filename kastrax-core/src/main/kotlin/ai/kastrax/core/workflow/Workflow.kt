package ai.kastrax.core.workflow

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
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
 * @property runId 运行ID
 */
data class WorkflowResult(
    val success: Boolean,
    val output: Map<String, Any?>,
    val steps: Map<String, WorkflowStepResult>,
    val error: String? = null,
    val executionTime: Long = 0,
    val runId: String? = null
)

/**
 * 工作流步骤结果。
 *
 * @property stepId 步骤ID
 * @property success 是否成功
 * @property output 输出数据
 * @property error 错误信息（如果失败）
 * @property executionTime 执行时间（毫秒）
 * @property errorHandled 错误是否已处理
 * @property skipped 是否跳过
 * @property suspendPayload 暂停时的附加数据
 */
data class WorkflowStepResult(
    val stepId: String,
    val success: Boolean,
    val output: Map<String, Any?>,
    val error: String? = null,
    val executionTime: Long = 0,
    val errorHandled: Boolean = false,
    val skipped: Boolean = false,
    val suspendPayload: Any? = null
) {
    companion object {
        /**
         * 创建成功的步骤结果。
         *
         * @param stepId 步骤ID
         * @param output 输出数据
         * @param executionTime 执行时间
         * @return 成功的步骤结果
         */
        fun success(stepId: String, output: Map<String, Any?>, executionTime: Long = 0): WorkflowStepResult {
            return WorkflowStepResult(
                stepId = stepId,
                success = true,
                output = output,
                executionTime = executionTime
            )
        }

        /**
         * 创建失败的步骤结果。
         *
         * @param stepId 步骤ID
         * @param error 错误对象
         * @param errorMessage 错误消息
         * @return 失败的步骤结果
         */
        fun failed(stepId: String, error: Exception, errorMessage: String? = null): WorkflowStepResult {
            return WorkflowStepResult(
                stepId = stepId,
                success = false,
                output = emptyMap(),
                error = errorMessage ?: error.message ?: "Unknown error",
                executionTime = 0
            )
        }

        /**
         * 创建已处理错误的步骤结果。
         *
         * @param stepId 步骤ID
         * @param error 错误消息
         * @param output 输出数据
         * @return 已处理错误的步骤结果
         */
        fun handledError(stepId: String, error: String, output: Map<String, Any?> = emptyMap()): WorkflowStepResult {
            return WorkflowStepResult(
                stepId = stepId,
                success = true,
                output = output,
                error = error,
                errorHandled = true
            )
        }

        /**
         * 创建跳过的步骤结果。
         *
         * @param stepId 步骤ID
         * @param reason 跳过原因
         * @return 跳过的步骤结果
         */
        fun skipped(stepId: String, reason: String? = null): WorkflowStepResult {
            return WorkflowStepResult(
                stepId = stepId,
                success = true,
                output = emptyMap(),
                error = reason,
                skipped = true
            )
        }
    }
}

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
     * 步骤配置。
     */
    val config: StepConfig?
        get() = null

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
 * @property runId 运行ID
 */
data class WorkflowContext(
    val input: Map<String, Any?>,
    val steps: MutableMap<String, WorkflowStepResult> = mutableMapOf(),
    val variables: MutableMap<String, Any?> = mutableMapOf(),
    val runId: String? = null
) {
    /**
     * 解析变量引用。
     *
     * @param reference 变量引用
     * @return 解析后的值
     */
    fun resolveReference(reference: VariableReference): Any? {
        val path = reference.path

        // 如果是常量值，直接返回
        if (!path.startsWith("$")) {
            return path
        }

        return when {
            path.startsWith("$.input.") -> resolveInputReference(path)
            path.startsWith("$.steps.") -> resolveStepReference(path)
            path.startsWith("$.variables.") -> resolveVariableReference(path)
            else -> null
        }
    }

    /**
     * 解析输入引用。
     */
    private fun resolveInputReference(path: String): Any? {
        val key = path.removePrefix("$.input.")
        return input[key]
    }

    /**
     * 解析步骤引用。
     */
    private fun resolveStepReference(path: String): Any? {
        // 验证路径格式
        val result = validateAndParsePath(path) ?: return null
        val (stepId, outputKey) = result

        // 获取步骤结果
        val stepResult = steps[stepId] ?: return null

        // 返回输出
        return getStepOutput(stepResult, outputKey)
    }

    /**
     * 验证和解析路径
     */
    private fun validateAndParsePath(path: String): Pair<String, String>? {
        val parts = path.removePrefix("$.steps.").split(".")
        if (parts.size < 2) return null

        return parts[0] to parts[1]
    }

    /**
     * 获取步骤输出
     */
    private fun getStepOutput(stepResult: WorkflowStepResult, outputKey: String): Any? {
        return if (outputKey == "output") {
            stepResult.output
        } else {
            stepResult.output[outputKey.removePrefix("output.")]
        }
    }

    /**
     * 解析变量引用。
     */
    private fun resolveVariableReference(path: String): Any? {
        val key = path.removePrefix("$.variables.")
        return variables[key]
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
    override val condition: (WorkflowContext) -> Boolean = { true },
    override val config: StepConfig? = null
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
    private val outputMappings = mutableMapOf<String, OutputMapping>()

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
     * 定义输出映射。
     */
    fun output(init: OutputMappingBuilder.() -> Unit) {
        val builder = OutputMappingBuilder()
        builder.init()
        outputMappings.putAll(builder.mappings)
    }

    /**
     * 构建工作流。
     */
    fun build(): SimpleWorkflow {
        require(name.isNotEmpty()) { "Workflow name must not be empty" }

        return SimpleWorkflow(
            workflowName = name,
            description = description,
            steps = steps,
            outputMappings = outputMappings
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
        var config: StepConfig? = null

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
                condition = condition,
                config = config
            )
        }

        /**
         * 设置重试配置。
         *
         * @param maxRetries 最大重试次数
         * @param initialDelay 初始延迟时间
         * @param maxDelay 最大延迟时间
         * @param backoffFactor 退避因子
         */
        fun retry(
            maxRetries: Int = 3,
            initialDelay: Duration = Duration.ofMillis(100),
            maxDelay: Duration = Duration.ofSeconds(30),
            backoffFactor: Double = 2.0
        ) {
            config = StepConfig(
                retryConfig = RetryConfig(
                    maxRetries = maxRetries,
                    initialDelay = initialDelay,
                    maxDelay = maxDelay,
                    backoffFactor = backoffFactor
                )
            )
        }
    }
}

/**
 * 输出映射，用于将步骤输出映射到工作流输出。
 *
 * @property source 源路径（使用变量引用语法）
 * @property transform 可选的转换函数
 */
data class OutputMapping(
    val source: String,
    val transform: ((Any?) -> Any?)? = null
)

/**
 * 输出映射构建器。
 */
class OutputMappingBuilder {
    val mappings = mutableMapOf<String, OutputMapping>()

    /**
     * 从源路径映射输出。
     */
    infix fun String.from(source: String) {
        mappings[this] = OutputMapping(source)
    }

    /**
     * 从源路径映射输出，并应用转换函数。
     */
    infix fun String.from(init: () -> Pair<String, (Any?) -> Any?>) {
        val (source, transform) = init()
        mappings[this] = OutputMapping(source, transform)
    }
}

/**
 * 简单工作流实现。
 *
 * @property description 工作流描述
 * @property steps 工作流步骤
 * @property outputMappings 输出映射
 */
class SimpleWorkflow(
    workflowName: String,
    val description: String = "",
    val steps: Map<String, WorkflowStep>,
    val outputMappings: Map<String, OutputMapping> = emptyMap()
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
            val result = executeSteps(executionOrder, context, executedSteps, options, startTime)
            if (!result.success) {
                return result
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
            logger.error(e) { "Workflow execution failed" }
            return createErrorResult(executedSteps, "Workflow execution failed: ${e.message}", startTime)
        }
    }

    /**
     * 执行工作流步骤。
     */
    private suspend fun executeSteps(
        executionOrder: List<String>,
        context: WorkflowContext,
        executedSteps: MutableMap<String, WorkflowStepResult>,
        options: WorkflowExecuteOptions,
        startTime: Long
    ): WorkflowResult {
        for (stepId in executionOrder) {
            val step = steps[stepId] ?: continue

            // 检查执行限制
            val limitResult = checkExecutionLimits(executedSteps, options, startTime)
            if (limitResult != null) {
                return limitResult
            }

            // 检查条件
            if (!step.condition.invoke(context)) {
                logger.debug { "Skipping step $stepId due to condition" }
                continue
            }

            // 执行步骤
            val stepResult = executeStep(step, stepId, context, executedSteps, options, startTime)
            if (!stepResult.success) {
                return stepResult
            }
        }

        return WorkflowResult(success = true, output = emptyMap(), steps = executedSteps)
    }

    /**
     * 执行单个工作流步骤。
     */
    private suspend fun executeStep(
        step: WorkflowStep,
        stepId: String,
        context: WorkflowContext,
        executedSteps: MutableMap<String, WorkflowStepResult>,
        options: WorkflowExecuteOptions,
        startTime: Long
    ): WorkflowResult {
        try {
            // 检查是否配置了重试
            val retryConfig = step.config?.retryConfig

            // 执行步骤，如果配置了重试则使用重试机制
            val stepResult = if (retryConfig != null) {
                try {
                    withRetry(retryConfig) {
                        step.execute(context)
                    }
                } catch (e: Exception) {
                    // 重试失败，返回失败结果
                    WorkflowStepResult.failed(stepId, e)
                }
            } else {
                // 直接执行步骤
                step.execute(context)
            }

            // 调用步骤完成回调
            options.onStepFinish?.invoke(stepResult)

            // 如果步骤失败，终止工作流
            if (!stepResult.success) {
                return createErrorResult(executedSteps, "Step $stepId failed: ${stepResult.error}", startTime)
            }

            // 只有成功的步骤才添加到执行步骤和上下文中
            executedSteps[stepId] = stepResult
            context.steps[stepId] = stepResult

            return WorkflowResult(success = true, output = emptyMap(), steps = executedSteps)
        } catch (e: IllegalArgumentException) {
            // 调用步骤错误回调
            options.onStepError?.invoke(stepId, e)
            logger.error(e) { "Invalid arguments for step $stepId" }
            return createErrorResult(executedSteps, "Invalid arguments for step $stepId: ${e.message}", startTime)
        } catch (e: Exception) {
            // 调用步骤错误回调
            options.onStepError?.invoke(stepId, e)
            logger.error(e) { "Error executing step $stepId" }
            return createErrorResult(executedSteps, "Error executing step $stepId: ${e.message}", startTime)
        }
    }

    /**
     * 检查执行限制（最大步骤数和超时）。
     */
    private fun checkExecutionLimits(
        executedSteps: Map<String, WorkflowStepResult>,
        options: WorkflowExecuteOptions,
        startTime: Long
    ): WorkflowResult? {
        // 检查是否超过最大步骤数
        if (executedSteps.size >= options.maxSteps) {
            return createErrorResult(executedSteps, "Maximum number of steps exceeded", startTime)
        }

        // 检查是否超时
        if (System.currentTimeMillis() - startTime > options.timeout) {
            return createErrorResult(executedSteps, "Workflow execution timed out", startTime)
        }

        return null
    }

    /**
     * 创建错误结果。
     */
    private fun createErrorResult(
        executedSteps: Map<String, WorkflowStepResult>,
        error: String,
        startTime: Long
    ): WorkflowResult {
        return WorkflowResult(
            success = false,
            output = emptyMap(),
            steps = executedSteps,
            error = error,
            executionTime = System.currentTimeMillis() - startTime
        )
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
        emitStatus(WorkflowStatus.STARTED, "Starting workflow execution")

        try {
            // 计算步骤执行顺序
            val executionOrder = computeExecutionOrder()
            val totalSteps = executionOrder.size

            // 执行步骤
            streamExecuteSteps(executionOrder, context, executedSteps, options, startTime, totalSteps)

            // 收集最终输出
            collectOutput(executedSteps)

            // 发送完成状态
            emitStatus(WorkflowStatus.COMPLETED, "Workflow execution completed", progress = 100)
        } catch (e: Exception) {
            logger.error(e) { "Workflow execution failed" }
            // 发送失败状态
            emitStatus(WorkflowStatus.FAILED, "Workflow execution failed: ${e.message}")
        }
    }

    /**
     * 流式执行工作流步骤。
     */
    private suspend fun FlowCollector<WorkflowStatusUpdate>.streamExecuteSteps(
        executionOrder: List<String>,
        context: WorkflowContext,
        executedSteps: MutableMap<String, WorkflowStepResult>,
        options: WorkflowExecuteOptions,
        startTime: Long,
        totalSteps: Int
    ) {
        for ((index, stepId) in executionOrder.withIndex()) {
            val step = steps[stepId] ?: continue

            // 发送进行中状态
            emitStatus(
                status = WorkflowStatus.IN_PROGRESS,
                message = "Executing step $stepId",
                stepId = stepId,
                progress = ((index.toDouble() / totalSteps) * 100).toInt()
            )

            // 检查执行限制
            if (checkStreamExecutionLimits(executedSteps, options, startTime)) {
                return
            }

            // 检查条件
            if (!step.condition.invoke(context)) {
                logger.debug { "Skipping step $stepId due to condition" }
                continue
            }

            // 执行步骤
            if (!streamExecuteStep(step, stepId, context, executedSteps, options, index, totalSteps)) {
                return
            }
        }
    }

    /**
     * 流式执行单个工作流步骤。
     *
     * @return 是否继续执行工作流
     */
    private suspend fun FlowCollector<WorkflowStatusUpdate>.streamExecuteStep(
        step: WorkflowStep,
        stepId: String,
        context: WorkflowContext,
        executedSteps: MutableMap<String, WorkflowStepResult>,
        options: WorkflowExecuteOptions,
        index: Int,
        totalSteps: Int
    ): Boolean {
        try {
            // 检查是否配置了重试
            val retryConfig = step.config?.retryConfig

            // 执行步骤，如果配置了重试则使用重试机制
            val stepResult = if (retryConfig != null) {
                try {
                    withRetry(retryConfig) {
                        step.execute(context)
                    }
                } catch (e: Exception) {
                    // 重试失败，返回失败结果
                    WorkflowStepResult.failed(stepId, e)
                }
            } else {
                // 直接执行步骤
                step.execute(context)
            }

            // 调用步骤完成回调
            options.onStepFinish?.invoke(stepResult)

            // 发送步骤完成状态
            emitStepResult(
                stepId = stepId,
                stepResult = stepResult,
                index = index,
                totalSteps = totalSteps
            )

            // 如果步骤失败，终止工作流
            if (!stepResult.success) {
                emitStatus(WorkflowStatus.FAILED, "Workflow failed: Step $stepId failed: ${stepResult.error}")
                return false
            }

            // 只有成功的步骤才添加到执行步骤和上下文中
            executedSteps[stepId] = stepResult
            context.steps[stepId] = stepResult

            return true
        } catch (e: IllegalArgumentException) {
            // 调用步骤错误回调
            options.onStepError?.invoke(stepId, e)
            logger.error(e) { "Invalid arguments for step $stepId" }

            // 发送错误状态
            emitStatus(
                status = WorkflowStatus.FAILED,
                message = "Invalid arguments for step $stepId: ${e.message}",
                stepId = stepId
            )

            return false
        } catch (e: Exception) {
            // 调用步骤错误回调
            options.onStepError?.invoke(stepId, e)
            logger.error(e) { "Error executing step $stepId" }

            // 发送错误状态
            emitStatus(
                status = WorkflowStatus.FAILED,
                message = "Error executing step $stepId: ${e.message}",
                stepId = stepId
            )

            return false
        }
    }

    /**
     * 检查流式执行限制。
     *
     * @return 是否应该停止执行
     */
    private suspend fun FlowCollector<WorkflowStatusUpdate>.checkStreamExecutionLimits(
        executedSteps: Map<String, WorkflowStepResult>,
        options: WorkflowExecuteOptions,
        startTime: Long
    ): Boolean {
        // 检查是否超过最大步骤数
        if (executedSteps.size >= options.maxSteps) {
            emitStatus(WorkflowStatus.FAILED, "Maximum number of steps exceeded")
            return true
        }

        // 检查是否超时
        if (System.currentTimeMillis() - startTime > options.timeout) {
            emitStatus(WorkflowStatus.FAILED, "Workflow execution timed out")
            return true
        }

        return false
    }

    /**
     * 发送状态更新。
     */
    private suspend fun FlowCollector<WorkflowStatusUpdate>.emitStatus(
        status: WorkflowStatus,
        message: String,
        stepId: String? = null,
        progress: Int = 0,
        result: WorkflowStepResult? = null
    ) {
        emit(WorkflowStatusUpdate(
            status = status,
            message = message,
            stepId = stepId,
            progress = progress,
            result = result
        ))
    }

    /**
     * 发送步骤结果状态。
     */
    private suspend fun FlowCollector<WorkflowStatusUpdate>.emitStepResult(
        stepId: String,
        stepResult: WorkflowStepResult,
        index: Int,
        totalSteps: Int
    ) {
        val status = if (stepResult.success) WorkflowStatus.IN_PROGRESS else WorkflowStatus.FAILED
        val message = if (stepResult.success) "Step $stepId completed" else "Step $stepId failed: ${stepResult.error}"
        val progress = ((index + 1.0) / totalSteps * 100).toInt()

        emit(WorkflowStatusUpdate(
            status = status,
            stepId = stepId,
            message = message,
            progress = progress,
            result = stepResult
        ))
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
        val output = mutableMapOf<String, Any?>()

        // 如果有输出映射，使用映射规则
        if (outputMappings.isNotEmpty()) {
            val context = WorkflowContext(
                input = emptyMap(),
                steps = executedSteps.toMutableMap()
            )

            for ((key, mapping) in outputMappings) {
                val reference = VariableReference(mapping.source)
                val value = context.resolveReference(reference)
                val transformedValue = mapping.transform?.invoke(value) ?: value
                output[key] = transformedValue
            }
        } else {
            // 否则，收集所有步骤的输出
            for ((stepId, stepResult) in executedSteps) {
                if (stepResult.success) {
                    output[stepId] = stepResult.output
                }
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
