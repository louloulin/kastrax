package ai.kastrax.codex.adapter

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.core.llm.LlmStreamResponse
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.core.llm.LlmUsage
import com.intellij.openapi.diagnostic.Logger
import ee.carlrobert.codegpt.completions.CompletionRequestService
import ee.carlrobert.llm.client.openai.completion.request.OpenAIChatCompletionMessage
import ee.carlrobert.llm.client.openai.completion.request.OpenAIChatCompletionRequest
import ee.carlrobert.llm.completion.CompletionEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.sse.EventSource

/**
 * 适配器，将 kastrax-codex 的 LLM 客户端与 kastrax 的 LlmProvider 接口集成
 */
class LlmProviderAdapter(
    override val model: String
) : LlmProvider {
    private val logger = Logger.getInstance(LlmProviderAdapter::class.java)
    private val completionRequestService = CompletionRequestService.getInstance()
    
    /**
     * 将 kastrax LlmMessage 转换为 OpenAI 消息格式
     */
    private fun convertToOpenAIMessage(message: LlmMessage): OpenAIChatCompletionMessage {
        val role = when (message.role) {
            LlmMessageRole.SYSTEM -> "system"
            LlmMessageRole.USER -> "user"
            LlmMessageRole.ASSISTANT -> "assistant"
            LlmMessageRole.TOOL -> "tool"
        }
        
        return OpenAIChatCompletionMessage(role, message.content)
    }
    
    /**
     * 创建 OpenAI 请求
     */
    private fun createOpenAIRequest(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): OpenAIChatCompletionRequest {
        val openAIMessages = messages.map { convertToOpenAIMessage(it) }
        
        val builder = OpenAIChatCompletionRequest.Builder(model)
            .setMessages(openAIMessages)
            .setTemperature(options.temperature)
            .setStream(false)
        
        options.maxTokens?.let { builder.setMaxTokens(it) }
        options.topP?.let { builder.setTopP(it) }
        options.frequencyPenalty?.let { builder.setFrequencyPenalty(it) }
        options.presencePenalty?.let { builder.setPresencePenalty(it) }
        
        if (options.stop.isNotEmpty()) {
            builder.setStop(options.stop)
        }
        
        if (options.tools.isNotEmpty()) {
            // 转换工具格式
            val tools = options.tools.map { toolJson ->
                val jsonObject = toolJson.jsonObject
                val functionObject = jsonObject["function"]?.jsonObject
                
                mapOf(
                    "type" to "function",
                    "function" to mapOf(
                        "name" to (functionObject?.get("name") as? JsonPrimitive)?.content,
                        "description" to (functionObject?.get("description") as? JsonPrimitive)?.content,
                        "parameters" to functionObject?.get("parameters")
                    )
                )
            }
            
            builder.setFunctions(tools)
            
            // 设置工具选择
            when (options.toolChoice) {
                "auto" -> builder.setFunctionCall("auto")
                "none" -> builder.setFunctionCall("none")
                "required" -> builder.setFunctionCall("required")
                is JsonObject -> {
                    val functionObject = (options.toolChoice as JsonObject)["function"]?.jsonObject
                    val functionName = (functionObject?.get("name") as? JsonPrimitive)?.content
                    if (functionName != null) {
                        builder.setFunctionCall(mapOf("name" to functionName))
                    }
                }
            }
        }
        
        return builder.build()
    }
    
    /**
     * 解析工具调用
     */
    private fun parseToolCalls(toolCallsJson: List<Map<String, Any>>?): List<LlmToolCall> {
        if (toolCallsJson == null || toolCallsJson.isEmpty()) {
            return emptyList()
        }
        
        return toolCallsJson.mapNotNull { toolCall ->
            val id = toolCall["id"] as? String ?: return@mapNotNull null
            val function = toolCall["function"] as? Map<String, Any> ?: return@mapNotNull null
            val name = function["name"] as? String ?: return@mapNotNull null
            val arguments = function["arguments"] as? String ?: "{}"
            
            LlmToolCall(
                id = id,
                name = name,
                arguments = arguments
            )
        }
    }
    
    /**
     * 生成响应
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse = withContext(Dispatchers.IO) {
        try {
            val request = createOpenAIRequest(messages, options)
            val response = completionRequestService.getChatCompletion(request)
            
            // 解析响应
            val openAIResponse = Json.parseToJsonElement(response).jsonObject
            val choices = openAIResponse["choices"]?.jsonObject
            val message = choices?.get("message")?.jsonObject
            
            val content = message?.get("content")?.toString() ?: ""
            val toolCalls = parseToolCalls(message?.get("tool_calls") as? List<Map<String, Any>>)
            
            val usage = openAIResponse["usage"]?.jsonObject?.let { usageJson ->
                LlmUsage(
                    promptTokens = (usageJson["prompt_tokens"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                    completionTokens = (usageJson["completion_tokens"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                    totalTokens = (usageJson["total_tokens"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                )
            }
            
            val finishReason = (choices?.get("finish_reason") as? JsonPrimitive)?.content
            
            LlmResponse(
                content = content,
                toolCalls = toolCalls,
                usage = usage,
                finishReason = finishReason
            )
        } catch (e: Exception) {
            logger.error("Error generating response", e)
            LlmResponse(content = "Error: ${e.message}")
        }
    }
    
    /**
     * 流式生成
     */
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> = callbackFlow {
        var eventSource: EventSource? = null
        
        try {
            val request = createOpenAIRequest(messages, options.copy())
                .toBuilder()
                .setStream(true)
                .build()
            
            val listener = object : CompletionEventListener<String> {
                override fun onEvent(event: String) {
                    trySend(event)
                }
                
                override fun onComplete() {
                    close()
                }
                
                override fun onError(error: Throwable) {
                    close(error)
                }
            }
            
            eventSource = completionRequestService.getChatCompletionAsync(request, listener)
        } catch (e: Exception) {
            logger.error("Error in stream generation", e)
            close(e)
        }
        
        awaitClose {
            eventSource?.cancel()
        }
    }
    
    /**
     * 流式生成带工具调用
     */
    override suspend fun streamGenerateWithTools(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmStreamResponse {
        // 简单实现，只返回文本流
        val textStream = streamGenerate(messages, options)
        return LlmStreamResponse(textStream = textStream, toolCallStream = null)
    }
    
    /**
     * 生成文本嵌入
     */
    override suspend fun embedText(text: String): List<Float> {
        // 暂不实现嵌入功能
        logger.warn("Embedding not implemented in LlmProviderAdapter")
        return emptyList()
    }
}
