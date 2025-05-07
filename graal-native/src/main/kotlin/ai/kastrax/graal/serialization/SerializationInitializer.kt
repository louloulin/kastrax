package ai.kastrax.graal.serialization

import ai.kastrax.graal.AppConfig
import ai.kastrax.integrations.deepseek.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * 序列化初始化器，确保序列化模块在构建时初始化
 *
 * 这个类负责在 GraalVM 原生镜像构建时注册所有需要序列化的类，
 * 避免在运行时依赖反射发现序列化器，这在 GraalVM 原生镜像中是不可靠的。
 */
@Suppress("unused")
object SerializationInitializer {
    /**
     * 初始化序列化模块，包含所有需要的序列化器
     */
    val module = SerializersModule {
        // 注册 DeepSeekStreamChunk 及其子类
        // 注意：这里不直接调用polymorphic方法，避免在构建时初始化这些类
        // polymorphic(DeepSeekStreamChunk::class) {
        //     subclass(DeepSeekStreamChunk.Content::class)
        //     subclass(DeepSeekStreamChunk.Finished::class)
        //     subclass(DeepSeekStreamChunk.Done::class)
        // }
    }

    /**
     * 创建 JSON 实例，配置为宽松模式以提高兼容性
     */
    val json = Json {
        serializersModule = module
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        prettyPrint = false // 生产环境通常不需要美化输出
        coerceInputValues = true // 尝试强制转换不匹配的值，提高兼容性
    }

    /**
     * 初始化序列化模块，确保所有需要序列化的类都被显式注册
     * 这对 GraalVM 原生镜像构建至关重要，因为它不支持运行时反射发现
     */
    fun initialize() {
        // 确保序列化模块在构建时初始化
        val serializers = mutableListOf<KSerializer<*>>(
            // DeepSeek 聊天完成相关类
            kotlinx.serialization.serializer<DeepSeekChatCompletionRequest>(),
            kotlinx.serialization.serializer<DeepSeekMessage>(),
            kotlinx.serialization.serializer<DeepSeekTool>(),
            kotlinx.serialization.serializer<DeepSeekFunction>(),
            kotlinx.serialization.serializer<DeepSeekToolCall>(),
            kotlinx.serialization.serializer<DeepSeekFunctionCall>(),
            kotlinx.serialization.serializer<DeepSeekChatCompletionResponse>(),
            kotlinx.serialization.serializer<DeepSeekChoice>(),
            kotlinx.serialization.serializer<DeepSeekUsage>(),

            // DeepSeek 嵌入相关类
            kotlinx.serialization.serializer<DeepSeekEmbeddingRequest>(),
            kotlinx.serialization.serializer<DeepSeekEmbeddingResponse>(),
            kotlinx.serialization.serializer<DeepSeekEmbedding>(),
            kotlinx.serialization.serializer<DeepSeekEmbeddingUsage>(),

            // DeepSeek 错误相关类
            kotlinx.serialization.serializer<DeepSeekErrorResponse>(),
            kotlinx.serialization.serializer<DeepSeekError>(),

            // DeepSeek 流式相关类
            // 注意：这里不直接调用serializer方法，避免在构建时初始化这些类
            // kotlinx.serialization.serializer<DeepSeekStreamChunk>(),
            // kotlinx.serialization.serializer<DeepSeekStreamChunk.Content>(),
            // kotlinx.serialization.serializer<DeepSeekStreamChunk.Finished>(),
            // kotlinx.serialization.serializer<DeepSeekStreamChunk.Done>(),

            // JSON 基础类型
            kotlinx.serialization.serializer<JsonObject>(),
            kotlinx.serialization.serializer<JsonArray>(),
            kotlinx.serialization.serializer<JsonPrimitive>(),

            // 应用配置相关类
            kotlinx.serialization.serializer<AppConfig>(),
            kotlinx.serialization.serializer<AppConfig.Logging>(),
            kotlinx.serialization.serializer<AppConfig.ApiKeys>()
        )

        // 确保序列化器被加载，触发它们的初始化
        serializers.forEach { serializer ->
            // 访问描述符强制加载序列化器
            val descriptor = serializer.descriptor
            // 记录已加载的序列化器，便于调试
            println("Initialized serializer: ${descriptor.serialName}")
        }

        println("SerializationInitializer: 已初始化 ${serializers.size} 个序列化器")
    }
}
