package ai.kastrax.integrations.deepseek

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * DeepSeek JSON 实例，用于序列化和反序列化 DeepSeek API 请求和响应
 */
object DeepSeekJson {
    /**
     * 序列化模块，注册所有 DeepSeek 相关类的序列化器
     */
    private val module = SerializersModule {
        // 注册 DeepSeekStreamChunk 及其子类
        polymorphic(DeepSeekStreamChunk::class) {
            subclass(DeepSeekStreamChunk.Content::class)
            subclass(DeepSeekStreamChunk.Finished::class)
            subclass(DeepSeekStreamChunk.Done::class)
        }

        // 显式注册所有需要的序列化器，避免依赖运行时反射
        contextual(DeepSeekChatCompletionRequest::class, DeepSeekChatCompletionRequest.serializer())
        contextual(DeepSeekMessage::class, DeepSeekMessage.serializer())
        contextual(DeepSeekTool::class, DeepSeekTool.serializer())
        contextual(DeepSeekFunction::class, DeepSeekFunction.serializer())
        contextual(DeepSeekToolCall::class, DeepSeekToolCall.serializer())
        contextual(DeepSeekFunctionCall::class, DeepSeekFunctionCall.serializer())
        contextual(DeepSeekChatCompletionResponse::class, DeepSeekChatCompletionResponse.serializer())
        contextual(DeepSeekChoice::class, DeepSeekChoice.serializer())
        contextual(DeepSeekUsage::class, DeepSeekUsage.serializer())
        contextual(DeepSeekEmbeddingRequest::class, DeepSeekEmbeddingRequest.serializer())
        contextual(DeepSeekEmbeddingResponse::class, DeepSeekEmbeddingResponse.serializer())
        contextual(DeepSeekEmbedding::class, DeepSeekEmbedding.serializer())
        contextual(DeepSeekEmbeddingUsage::class, DeepSeekEmbeddingUsage.serializer())
        contextual(DeepSeekErrorResponse::class, DeepSeekErrorResponse.serializer())
        contextual(DeepSeekError::class, DeepSeekError.serializer())
    }

    /**
     * JSON 实例，用于序列化和反序列化 DeepSeek API 请求和响应
     */
    val json = Json {
        serializersModule = module
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }
}
