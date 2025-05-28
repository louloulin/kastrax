package ai.kastrax.integrations.qwen

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Qwen JSON 实例，用于序列化和反序列化 Qwen API 请求和响应
 */
object QwenJson {
    /**
     * 序列化模块，注册所有 Qwen 相关类的序列化器
     */
    private val module = SerializersModule {
        // 显式注册所有需要的序列化器，避免依赖运行时反射
        contextual(QwenChatCompletionRequest::class, QwenChatCompletionRequest.serializer())
        contextual(QwenMessage::class, QwenMessage.serializer())
        contextual(QwenTool::class, QwenTool.serializer())
        contextual(QwenFunction::class, QwenFunction.serializer())
        contextual(QwenToolCall::class, QwenToolCall.serializer())
        contextual(QwenFunctionCall::class, QwenFunctionCall.serializer())
        contextual(QwenChatCompletionResponse::class, QwenChatCompletionResponse.serializer())
        contextual(QwenChoice::class, QwenChoice.serializer())
        contextual(QwenUsage::class, QwenUsage.serializer())
    }

    /**
     * JSON 实例，用于序列化和反序列化 Qwen API 请求和响应
     */
    val json = Json {
        serializersModule = module
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }
}