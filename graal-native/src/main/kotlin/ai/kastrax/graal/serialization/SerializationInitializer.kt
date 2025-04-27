package ai.kastrax.graal.serialization

import ai.kastrax.integrations.deepseek.DeepSeekChatCompletionRequest
import ai.kastrax.integrations.deepseek.DeepSeekMessage
import ai.kastrax.integrations.deepseek.DeepSeekTool
import ai.kastrax.integrations.deepseek.DeepSeekFunction
import ai.kastrax.integrations.deepseek.DeepSeekToolCall
import ai.kastrax.integrations.deepseek.DeepSeekFunctionCall
import ai.kastrax.integrations.deepseek.DeepSeekChatCompletionResponse
import ai.kastrax.integrations.deepseek.DeepSeekChoice
import ai.kastrax.integrations.deepseek.DeepSeekUsage
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * 序列化初始化器，确保序列化模块在构建时初始化
 */
@Suppress("unused")
object SerializationInitializer {
    /**
     * 初始化序列化模块
     */
    val module = SerializersModule {}

    /**
     * 创建 JSON 实例
     */
    val json = Json {
        serializersModule = module
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    /**
     * 初始化序列化模块
     */
    fun initialize() {
        // 确保序列化模块在构建时初始化
        val serializers = listOf(
            kotlinx.serialization.serializer<DeepSeekChatCompletionRequest>(),
            kotlinx.serialization.serializer<DeepSeekMessage>(),
            kotlinx.serialization.serializer<DeepSeekTool>(),
            kotlinx.serialization.serializer<DeepSeekFunction>(),
            kotlinx.serialization.serializer<DeepSeekToolCall>(),
            kotlinx.serialization.serializer<DeepSeekFunctionCall>(),
            kotlinx.serialization.serializer<DeepSeekChatCompletionResponse>(),
            kotlinx.serialization.serializer<DeepSeekChoice>(),
            kotlinx.serialization.serializer<DeepSeekUsage>()
        )

        // 确保序列化器被加载
        serializers.forEach { it.descriptor }
    }
}
