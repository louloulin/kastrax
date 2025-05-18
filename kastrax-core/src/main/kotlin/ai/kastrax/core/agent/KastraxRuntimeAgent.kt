package ai.kastrax.core.agent

import ai.kastrax.core.agent.config.ResponseFormat
import ai.kastrax.core.agent.config.RetryStrategy
import ai.kastrax.core.agent.config.SafetySettings
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.core.llm.LlmStreamResponse
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.core.llm.LlmUsage
import ai.kastrax.core.memory.toMessage
import ai.kastrax.core.memory.toLlmMessage
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ToolCallResult
import ai.kastrax.memory.api.Memory
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * 使用kastrax协程运行时的Agent实现
 *
 * @property name Agent名称
 * @property instructions Agent指令
 * @property model LLM提供者
 * @property tools 可用工具
 * @property memory 可选的记忆系统
 * @property defaultGenerateOptions 默认生成选项
 * @property defaultStreamOptions 默认流式选项
 * @property toolsets 额外的工具集
 * @property sessionManager 可选的会话管理器
 * @property stateManager 可选的状态管理器
 * @property versionManager 可选的版本管理器
 * @property runtime 协程运行时
 */
class KastraxRuntimeAgent(
    name: String,
    val instructions: String,
    val model: LlmProvider,
    val tools: Map<String, Tool> = emptyMap(),
    val memory: Memory? = null,
    val defaultGenerateOptions: AgentGenerateOptions = AgentGenerateOptions(),
    val defaultStreamOptions: AgentStreamOptions = AgentStreamOptions(),
    val toolsets: Map<String, Map<String, Tool>> = emptyMap(),
    val sessionManager: SessionManager? = null,
    val stateManager: StateManager? = null,
    override val versionManager: AgentVersionManager? = null,
    val runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
) : KastraXBase(component = "AGENT", name = name), Agent {

    // 当前状态ID
    private var currentStateId: String? = null

    // 协程作用域
    private val scope = runtime.getScope(this)

    /**
     * 生成响应
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): AgentResponse {
        // 更新状态为思考中
        updateState(AgentStatus.THINKING)

        // 合并默认选项与提供的选项
        val mergedOptions = defaultGenerateOptions.merge(options)

        // 使用覆盖指令（如果提供）
        val effectiveInstructions = mergedOptions.instructions ?: instructions

        // 准备消息和工具
        val allMessages = prepareMessages(messages, effectiveInstructions, mergedOptions)
        val llmOptions = prepareLlmOptions(mergedOptions)

        // 使用IO调度器生成响应
        val response = runtime.ioDispatcher().withContext {
            model.generate(allMessages, llmOptions)
        }

        // 处理工具调用（如果存在且启用执行）
        val toolResults = if (mergedOptions.executeTools && response.toolCalls.isNotEmpty()) {
            // 更新状态为执行工具
            updateState(AgentStatus.EXECUTING)
            executeToolCalls(response.toolCalls)
        } else {
            emptyMap()
        }

        // 更新状态为响应中
        updateState(AgentStatus.RESPONDING)

        // 创建步骤结果用于回调
        val stepResult = StepResult(
            text = response.content,
            toolCalls = response.toolCalls,
            toolResults = toolResults
        )

        // 调用步骤完成回调（如果提供）
        mergedOptions.onStepFinish?.invoke(stepResult)

        // 更新状态为空闲
        updateState(AgentStatus.IDLE)

        // 获取当前状态
        val currentState = getState()

        // 获取会话信息
        val sessionInfo = if (sessionManager != null && mergedOptions.threadId != null) {
            sessionManager.getSession(mergedOptions.threadId)
        } else {
            null
        }

        // 返回响应
        return AgentResponse(
            text = response.content,
            toolCalls = response.toolCalls,
            toolResults = toolResults,
            usage = response.usage,
            threadId = mergedOptions.threadId,
            state = currentState,
            sessionInfo = sessionInfo
        )
    }

    /**
     * 从单个提示生成响应
     */
    override suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions
    ): AgentResponse {
        // 创建用户消息
        val userMessage = LlmMessage(role = LlmMessageRole.USER, content = prompt)

        // 如果有内存系统，创建或使用现有线程
        val threadId = options.threadId ?: if (memory != null) {
            memory.createThread(options.threadTitle)
        } else {
            null
        }

        // 如果有内存系统和线程ID，保存用户消息
        if (memory != null && threadId != null) {
            // 保存用户消息
            memory.saveMessage(userMessage.toMessage(), threadId)

            // 获取历史消息
            val historyMessages = memory.getMessages(threadId).map { it.message.toLlmMessage() }

            // 生成响应
            val response = generate(historyMessages.toList(), options)

            // 保存助手消息
            val assistantMessage = LlmMessage(
                role = LlmMessageRole.ASSISTANT,
                content = response.text,
                toolCalls = response.toolCalls
            )
            memory.saveMessage(assistantMessage.toMessage(), threadId)

            // 返回带有线程ID的响应
            return response.copy(threadId = threadId)
        } else {
            // 如果没有内存系统，直接生成响应
            val messages = listOf(userMessage)
            return generate(messages, options)
        }
    }

    /**
     * 流式生成响应
     */
    override suspend fun stream(
        prompt: String,
        options: AgentStreamOptions
    ): AgentResponse {
        // 更新状态为思考中
        updateState(AgentStatus.THINKING)

        // 合并默认选项与提供的选项
        val mergedOptions = defaultStreamOptions.merge(options)

        // 使用覆盖指令（如果提供）
        val effectiveInstructions = mergedOptions.instructions ?: instructions

        // 准备流式生成
        val threadId = prepareThreadId(mergedOptions)
        val userMessage = LlmMessage(role = LlmMessageRole.USER, content = prompt)
        saveUserMessage(userMessage, threadId)

        // 准备消息和工具
        val allMessages = prepareMessages(userMessage, effectiveInstructions, mergedOptions, threadId)
        val llmOptions = prepareLlmOptions(mergedOptions)

        // 使用IO调度器流式生成响应
        return runtime.ioDispatcher().withContext {
            streamResponse(allMessages, llmOptions, threadId)
        }
    }

    /**
     * 流式生成响应
     */
    override suspend fun generateStream(
        prompt: String,
        options: AgentStreamOptions
    ): Flow<AgentStreamResponse> = flow {
        val response = stream(prompt, options)

        if (response.textStream != null) {
            response.textStream.collect { chunk ->
                emit(AgentStreamResponse(chunk))
            }
        } else {
            emit(AgentStreamResponse(response.text))
        }
    }

    /**
     * 重置Agent
     */
    override suspend fun reset() {
        // 重置状态
        updateState(AgentStatus.IDLE)
    }

    /**
     * 获取Agent状态
     */
    override suspend fun getState(): AgentState? {
        return stateManager?.getState(currentStateId)
    }

    /**
     * 更新Agent状态
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        return if (stateManager != null) {
            val state = AgentState(
                status = status,
                lastUpdated = Clock.System.now()
            )
            currentStateId = stateManager.updateState(state)
            state
        } else {
            null
        }
    }

    /**
     * 创建会话
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        if (sessionManager == null) {
            logger.warn { "No session manager configured" }
            return null
        }

        val session = sessionManager.createSession(title, resourceId, metadata)

        // 如果有状态管理器，更新状态中的线程ID
        if (stateManager != null && currentStateId != null) {
            val currentState = stateManager.getState(currentStateId!!)
            if (currentState != null) {
                val updatedState = currentState.copy(threadId = session.id)
                stateManager.updateState(updatedState)
            }
        }

        logger.debug { "Created session: ${session.id}" }
        return session
    }

    /**
     * 获取会话信息
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        return sessionManager?.getSession(sessionId)
    }

    /**
     * 获取会话消息
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return sessionManager?.getMessages(sessionId, limit)
    }

    /**
     * 创建版本
     */
    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion? {
        return versionManager?.createVersion(
            instructions = instructions,
            name = name,
            description = description,
            metadata = metadata,
            activateImmediately = activateImmediately
        )
    }

    /**
     * 获取所有版本
     */
    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        return versionManager?.getVersions(limit, offset)
    }

    /**
     * 获取当前激活版本
     */
    override suspend fun getActiveVersion(): AgentVersion? {
        return versionManager?.getActiveVersion()
    }

    /**
     * 激活版本
     */
    override suspend fun activateVersion(versionId: String): AgentVersion? {
        return versionManager?.activateVersion(versionId)
    }

    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        return versionManager?.rollbackToVersion(versionId)
    }

    // 辅助方法

    /**
     * 准备线程ID
     */
    private suspend fun prepareThreadId(options: AgentStreamOptions): String? {
        return options.threadId ?: if (memory != null) {
            memory.createThread(options.threadTitle)
        } else {
            null
        }
    }

    /**
     * 保存用户消息
     */
    private suspend fun saveUserMessage(message: LlmMessage, threadId: String?) {
        if (memory != null && threadId != null) {
            memory.saveMessage(message.toMessage(), threadId)
        }
    }

    /**
     * 保存助手消息
     */
    private suspend fun saveAssistantMessage(content: String, threadId: String?) {
        if (memory != null && threadId != null) {
            val assistantMessage = LlmMessage(
                role = LlmMessageRole.ASSISTANT,
                content = content
            )
            memory.saveMessage(assistantMessage.toMessage(), threadId)
        }
    }

    /**
     * 准备消息
     */
    private suspend fun prepareMessages(
        userMessage: LlmMessage,
        instructions: String,
        options: AgentStreamOptions,
        threadId: String?
    ): List<LlmMessage> {
        // 创建系统消息
        val systemMessage = LlmMessage(role = LlmMessageRole.SYSTEM, content = instructions)

        // 获取历史消息
        val historyMessages = if (memory != null && threadId != null) {
            memory.getMessages(threadId).map { it.message.toLlmMessage() }
        } else {
            emptyList()
        }

        // 合并所有消息
        return listOf(systemMessage) + historyMessages + userMessage
    }

    /**
     * 准备消息
     */
    private fun prepareMessages(
        messages: List<LlmMessage>,
        instructions: String,
        options: AgentGenerateOptions
    ): List<LlmMessage> {
        // 创建系统消息
        val systemMessage = LlmMessage(role = LlmMessageRole.SYSTEM, content = instructions)

        // 合并所有消息
        return listOf(systemMessage) + messages
    }

    /**
     * 准备LLM选项
     */
    private fun prepareLlmOptions(options: AgentGenerateOptions): LlmOptions {
        val toolsList = prepareTools(options).values.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parameters
                )
            )
        }

        return LlmOptions(
            temperature = options.temperature,
            maxTokens = options.maxTokens,
            tools = toolsList,
            toolChoice = when(options.toolChoice) {
                ToolChoice.Auto -> "auto"
                ToolChoice.None -> "none"
                is ToolChoice.Tool -> mapOf(
                    "type" to "function",
                    "function" to mapOf(
                        "name" to (options.toolChoice as ToolChoice.Tool).name
                    )
                )
            },
            topP = options.topP,
            frequencyPenalty = options.frequencyPenalty,
            presencePenalty = options.presencePenalty,
            stop = options.stopSequences,
            logitBias = options.logitBias,
            seed = options.seed,
            responseFormat = null // 暂时不支持响应格式
        )
    }

    /**
     * 准备LLM选项
     */
    private fun prepareLlmOptions(options: AgentStreamOptions): LlmOptions {
        val toolsList = prepareTools(options).values.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parameters
                )
            )
        }

        return LlmOptions(
            temperature = options.temperature,
            maxTokens = options.maxTokens,
            tools = toolsList,
            toolChoice = when(options.toolChoice) {
                ToolChoice.Auto -> "auto"
                ToolChoice.None -> "none"
                is ToolChoice.Tool -> mapOf(
                    "type" to "function",
                    "function" to mapOf(
                        "name" to (options.toolChoice as ToolChoice.Tool).name
                    )
                )
            },
            topP = options.topP,
            frequencyPenalty = options.frequencyPenalty,
            presencePenalty = options.presencePenalty,
            stop = options.stopSequences,
            logitBias = options.logitBias,
            seed = options.seed,
            responseFormat = null // 暂时不支持响应格式
        )
    }

    /**
     * 准备工具
     */
    private fun prepareTools(options: AgentGenerateOptions): Map<String, Tool> {
        // 合并基本工具和工具集
        val allTools = mutableMapOf<String, Tool>()
        allTools.putAll(tools)

        // 添加选项中指定的工具集
        options.toolsets?.forEach { (_, toolset) ->
            allTools.putAll(toolset)
        }

        // 添加默认工具集
        toolsets.forEach { (_, toolset) ->
            allTools.putAll(toolset)
        }

        return allTools
    }

    /**
     * 准备工具
     */
    private fun prepareTools(options: AgentStreamOptions): Map<String, Tool> {
        // 合并基本工具和工具集
        val allTools = mutableMapOf<String, Tool>()
        allTools.putAll(tools)

        // 添加选项中指定的工具集
        options.toolsets?.forEach { (_, toolset) ->
            allTools.putAll(toolset)
        }

        // 添加默认工具集
        toolsets.forEach { (_, toolset) ->
            allTools.putAll(toolset)
        }

        return allTools
    }

    /**
     * 执行工具调用
     */
    private suspend fun executeToolCalls(toolCalls: List<LlmToolCall>): Map<String, ToolCallResult> {
        val results = mutableMapOf<String, ToolCallResult>()

        for (toolCall in toolCalls) {
            val tool = tools[toolCall.name]

            if (tool != null) {
                try {
                    // 使用IO调度器执行工具
                    val result = runtime.ioDispatcher().withContext {
                        tool.call(toolCall.arguments)
                    }

                    results[toolCall.id] = ToolCallResult(
                        toolCall = toolCall,
                        result = result,
                        error = null
                    )
                } catch (e: Exception) {
                    results[toolCall.id] = ToolCallResult(
                        toolCall = toolCall,
                        result = null,
                        error = e.message
                    )
                }
            } else {
                results[toolCall.id] = ToolCallResult(
                    toolCall = toolCall,
                    result = null,
                    error = "Tool not found: ${toolCall.name}"
                )
            }
        }

        return results
    }

    /**
     * 流式生成响应
     */
    private suspend fun streamResponse(
        messages: List<LlmMessage>,
        llmOptions: LlmOptions,
        threadId: String?
    ): AgentResponse {
        // 更新状态为响应中
        updateState(AgentStatus.RESPONDING)

        try {
            // 尝试使用带工具调用的流式生成
            val streamResponse = model.streamGenerateWithTools(messages, llmOptions)

            // 创建响应流，收集完整文本
            val responseBuilder = StringBuilder()
            val processedTextStream = streamResponse.textStream?.let { textStream ->
                flow {
                    textStream.collect { chunk ->
                        responseBuilder.append(chunk)
                        emit(chunk)
                    }

                    // 当流完成后，保存助手消息
                    val content = responseBuilder.toString()
                    saveAssistantMessage(content, threadId)

                    // 更新状态为空闲
                    updateState(AgentStatus.IDLE)
                }
            }

            // 获取当前状态
            val currentState = getState()

            // 获取会话信息
            val sessionInfo = if (sessionManager != null && threadId != null) {
                sessionManager.getSession(threadId)
            } else {
                null
            }

            // 创建响应对象
            return AgentResponse(
                text = responseBuilder.toString(),
                textStream = processedTextStream,
                toolCallStream = streamResponse.toolCallStream,
                threadId = threadId,
                state = currentState,
                sessionInfo = sessionInfo
            )
        } catch (e: Exception) {
            // 如果带工具调用的流式生成失败，回退到普通流式生成
            logger.warn { "Failed to use streamGenerateWithTools, falling back to streamGenerate: ${e.message}" }

            // 从LLM流式生成响应
            val responseStream = model.streamGenerate(messages, llmOptions)

            // 创建响应流，收集完整文本
            val responseBuilder = StringBuilder()
            val processedStream = flow {
                responseStream.collect { chunk ->
                    responseBuilder.append(chunk)
                    emit(chunk)
                }

                // 当流完成后，保存助手消息
                val content = responseBuilder.toString()
                saveAssistantMessage(content, threadId)

                // 更新状态为空闲
                updateState(AgentStatus.IDLE)
            }

            // 获取当前状态
            val currentState = getState()

            // 获取会话信息
            val sessionInfo = if (sessionManager != null && threadId != null) {
                sessionManager.getSession(threadId)
            } else {
                null
            }

            // 创建响应对象
            return AgentResponse(
                text = responseBuilder.toString(),
                textStream = processedStream,
                threadId = threadId,
                state = currentState,
                sessionInfo = sessionInfo
            )
        }
    }
}
