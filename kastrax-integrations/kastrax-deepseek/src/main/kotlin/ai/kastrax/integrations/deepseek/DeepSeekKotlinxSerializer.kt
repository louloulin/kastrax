package ai.kastrax.integrations.deepseek

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

/**
 * DeepSeek Kotlinx 序列化器工厂，用于创建 Ktor 的 ContentNegotiation 序列化器
 *
 * 这个类使用预先注册的序列化器，避免在运行时依赖反射发现序列化器
 */
object DeepSeekKotlinxSerializer {
    /**
     * 创建一个预先注册了所有 DeepSeek 相关类的 JSON 序列化器
     *
     * @return 预先注册了所有 DeepSeek 相关类的 JSON 序列化器
     */
    fun createJsonConverter() = DeepSeekJson.json
}
