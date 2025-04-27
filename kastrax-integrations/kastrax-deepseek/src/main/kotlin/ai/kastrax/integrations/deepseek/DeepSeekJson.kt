package ai.kastrax.integrations.deepseek

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * DeepSeek JSON 实例，用于序列化和反序列化 DeepSeek API 请求和响应
 */
object DeepSeekJson {
    /**
     * 序列化模块，注册所有 DeepSeek 相关类的序列化器
     */
    private val module = SerializersModule {}

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
