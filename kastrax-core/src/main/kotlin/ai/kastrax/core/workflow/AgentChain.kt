package ai.kastrax.core.workflow

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Agent链配置
 *
 * @property name 链名称
 * @property description 链描述
 * @property agents 代理列表
 * @property inputSchema 输入模式
 * @property outputSchema 输出模式
 */
data class AgentChainConfig(
    val name: String,
    val description: String = "",
    val agents: List<Agent>,
    val inputSchema: JsonElement? = null,
    val outputSchema: JsonElement? = null
)

/**
 * Agent链，用于按顺序执行多个代理
 */
class AgentChain(config: AgentChainConfig) : KastraXBase(component = "AGENT_CHAIN", name = config.name) {
    val description: String = config.description
    val agents: List<Agent> = config.agents
    val inputSchema: JsonElement? = config.inputSchema
    val outputSchema: JsonElement? = config.outputSchema

    /**
     * 执行Agent链
     *
     * @param input 输入文本
     * @param options 执行选项
     * @return 链执行结果
     */
    suspend fun execute(
        input: String,
        options: AgentChainExecuteOptions = AgentChainExecuteOptions()
    ): AgentChainResult {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<AgentChainStep>()
        var currentInput = input
        var success = true
        var error: String? = null

        try {
            // 执行每个代理
            for ((index, agent) in agents.withIndex()) {
                logger.debug("执行代理 ${index + 1}/${agents.size}: ${agent.name}")

                val stepStartTime = System.currentTimeMillis()

                try {
                    // 执行代理
                    val result = agent.generate(currentInput, options.agentOptions)

                    // 记录步骤
                    val step = AgentChainStep(
                        agentName = agent.name,
                        input = currentInput,
                        output = result.text,
                        success = true,
                        executionTime = System.currentTimeMillis() - stepStartTime
                    )
                    steps.add(step)

                    // 更新当前输入为上一步的输出
                    currentInput = result.text

                    // 调用步骤完成回调
                    options.onStepFinish?.invoke(step)
                } catch (e: Exception) {
                    // 记录失败步骤
                    val step = AgentChainStep(
                        agentName = agent.name,
                        input = currentInput,
                        output = "",
                        success = false,
                        error = e.message,
                        executionTime = System.currentTimeMillis() - stepStartTime
                    )
                    steps.add(step)

                    // 调用步骤错误回调
                    options.onStepError?.invoke(agent.name, e)

                    success = false
                    error = e.message
                    break
                }
            }
        } catch (e: Exception) {
            success = false
            error = e.message
        }

        val executionTime = System.currentTimeMillis() - startTime

        return AgentChainResult(
            success = success,
            output = currentInput,
            steps = steps,
            error = error,
            executionTime = executionTime
        )
    }

    /**
     * 流式执行Agent链
     *
     * @param input 输入文本
     * @param options 执行选项
     * @return 链状态更新的流
     */
    suspend fun streamExecute(
        input: String,
        options: AgentChainExecuteOptions = AgentChainExecuteOptions()
    ): Flow<AgentChainStatusUpdate> = flow {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<AgentChainStep>()
        var currentInput = input
        var success = true
        var error: String? = null

        // 发送开始状态
        emit(AgentChainStatusUpdate(
            status = AgentChainStatus.STARTED,
            message = "开始执行Agent链",
            progress = 0
        ))

        try {
            // 执行每个代理
            for ((index, agent) in agents.withIndex()) {
                val progress = ((index.toDouble() / agents.size) * 100).toInt()

                // 发送进行中状态
                emit(AgentChainStatusUpdate(
                    status = AgentChainStatus.IN_PROGRESS,
                    message = "执行代理 ${index + 1}/${agents.size}: ${agent.name}",
                    agentName = agent.name,
                    progress = progress
                ))

                val stepStartTime = System.currentTimeMillis()

                try {
                    // 执行代理
                    val result = agent.generate(currentInput, options.agentOptions)

                    // 记录步骤
                    val step = AgentChainStep(
                        agentName = agent.name,
                        input = currentInput,
                        output = result.text,
                        success = true,
                        executionTime = System.currentTimeMillis() - stepStartTime
                    )
                    steps.add(step)

                    // 更新当前输入为上一步的输出
                    currentInput = result.text

                    // 调用步骤完成回调
                    options.onStepFinish?.invoke(step)

                    // 发送步骤完成状态
                    emit(AgentChainStatusUpdate(
                        status = AgentChainStatus.STEP_COMPLETED,
                        message = "代理 ${agent.name} 执行完成",
                        agentName = agent.name,
                        progress = ((index + 1.0) / agents.size * 100).toInt(),
                        step = step
                    ))
                } catch (e: Exception) {
                    // 记录失败步骤
                    val step = AgentChainStep(
                        agentName = agent.name,
                        input = currentInput,
                        output = "",
                        success = false,
                        error = e.message,
                        executionTime = System.currentTimeMillis() - stepStartTime
                    )
                    steps.add(step)

                    // 调用步骤错误回调
                    options.onStepError?.invoke(agent.name, e)

                    // 发送步骤失败状态
                    emit(AgentChainStatusUpdate(
                        status = AgentChainStatus.STEP_FAILED,
                        message = "代理 ${agent.name} 执行失败: ${e.message}",
                        agentName = agent.name,
                        progress = ((index + 1.0) / agents.size * 100).toInt(),
                        step = step,
                        error = e.message
                    ))

                    success = false
                    error = e.message
                    break
                }
            }

            // 发送完成状态
            val finalStatus = if (success) AgentChainStatus.COMPLETED else AgentChainStatus.FAILED
            val finalMessage = if (success) "Agent链执行完成" else "Agent链执行失败: $error"

            emit(AgentChainStatusUpdate(
                status = finalStatus,
                message = finalMessage,
                progress = if (success) 100 else -1,
                output = if (success) currentInput else null,
                error = error
            ))
        } catch (e: Exception) {
            // 发送失败状态
            emit(AgentChainStatusUpdate(
                status = AgentChainStatus.FAILED,
                message = "Agent链执行失败: ${e.message}",
                progress = -1,
                error = e.message
            ))
        }
    }
}

/**
 * Agent链执行选项
 *
 * @property agentOptions 代理执行选项
 * @property onStepFinish 步骤完成回调
 * @property onStepError 步骤错误回调
 */
data class AgentChainExecuteOptions(
    val agentOptions: AgentGenerateOptions = AgentGenerateOptions(),
    val onStepFinish: ((AgentChainStep) -> Unit)? = null,
    val onStepError: ((String, Throwable) -> Unit)? = null
)

/**
 * Agent链执行结果
 *
 * @property success 是否成功
 * @property output 输出文本
 * @property steps 执行的步骤
 * @property error 错误信息（如果失败）
 * @property executionTime 执行时间（毫秒）
 */
data class AgentChainResult(
    val success: Boolean,
    val output: String,
    val steps: List<AgentChainStep>,
    val error: String? = null,
    val executionTime: Long = 0
)

/**
 * Agent链步骤
 *
 * @property agentName 代理名称
 * @property input 输入文本
 * @property output 输出文本
 * @property success 是否成功
 * @property error 错误信息（如果失败）
 * @property executionTime 执行时间（毫秒）
 */
data class AgentChainStep(
    val agentName: String,
    val input: String,
    val output: String,
    val success: Boolean,
    val error: String? = null,
    val executionTime: Long = 0
)

/**
 * Agent链状态
 */
enum class AgentChainStatus {
    STARTED,
    IN_PROGRESS,
    STEP_COMPLETED,
    STEP_FAILED,
    COMPLETED,
    FAILED
}

/**
 * Agent链状态更新
 *
 * @property status 状态
 * @property message 消息
 * @property agentName 当前代理名称
 * @property progress 进度（0-100）
 * @property step 步骤结果
 * @property output 最终输出（仅在完成时）
 * @property error 错误信息（如果失败）
 */
data class AgentChainStatusUpdate(
    val status: AgentChainStatus,
    val message: String,
    val agentName: String? = null,
    val progress: Int = 0,
    val step: AgentChainStep? = null,
    val output: String? = null,
    val error: String? = null
)

/**
 * 创建Agent链的DSL函数
 */
fun agentChain(init: AgentChainBuilder.() -> Unit): AgentChain {
    val builder = AgentChainBuilder()
    builder.init()
    return builder.build()
}

/**
 * Agent链构建器
 */
class AgentChainBuilder {
    var name: String = ""
    var description: String = ""
    var inputSchema: JsonElement? = null
    var outputSchema: JsonElement? = null
    val agents: MutableList<Agent> = mutableListOf()

    /**
     * 添加代理
     */
    fun agent(agent: Agent) {
        agents.add(agent)
    }

    /**
     * 构建Agent链
     */
    fun build(): AgentChain {
        require(name.isNotEmpty()) { "Agent链名称不能为空" }
        require(agents.isNotEmpty()) { "Agent链必须至少有一个代理" }

        return AgentChain(
            AgentChainConfig(
                name = name,
                description = description,
                agents = agents,
                inputSchema = inputSchema,
                outputSchema = outputSchema
            )
        )
    }
}
