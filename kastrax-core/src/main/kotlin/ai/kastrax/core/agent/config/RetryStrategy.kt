package ai.kastrax.core.agent.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 重试策略配置，控制生成失败时的重试行为。
 *
 * @property maxRetries 最大重试次数
 * @property initialDelayMs 初始延迟时间（毫秒）
 * @property maxDelayMs 最大延迟时间（毫秒）
 * @property backoffFactor 退避因子，控制延迟增长速度
 * @property retryableErrors 可重试的错误类型列表
 */
data class RetryStrategy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffFactor: Double = 2.0,
    val retryableErrors: List<ErrorType> = listOf(
        ErrorType.RATE_LIMIT,
        ErrorType.TIMEOUT,
        ErrorType.SERVER_ERROR
    )
) {
    /**
     * 可重试的错误类型。
     */
    enum class ErrorType {
        RATE_LIMIT,      // 速率限制错误
        TIMEOUT,         // 超时错误
        SERVER_ERROR,    // 服务器错误
        CONTEXT_LENGTH,  // 上下文长度错误
        TOKEN_LIMIT,     // 令牌限制错误
        CONTENT_FILTER,  // 内容过滤错误
        API_ERROR        // 通用API错误
    }

    /**
     * 将重试策略转换为 JsonElement。
     */
    fun toJsonElement(): JsonElement {
        return JsonObject(mapOf(
            "maxRetries" to JsonPrimitive(maxRetries),
            "initialDelayMs" to JsonPrimitive(initialDelayMs),
            "maxDelayMs" to JsonPrimitive(maxDelayMs),
            "backoffFactor" to JsonPrimitive(backoffFactor),
            "retryableErrors" to JsonPrimitive(retryableErrors.joinToString(",") { it.name })
        ))
    }

    companion object {
        /**
         * 创建默认的重试策略。
         */
        fun default(): RetryStrategy = RetryStrategy()

        /**
         * 创建激进的重试策略，适用于需要高可靠性的场景。
         */
        fun aggressive(): RetryStrategy = RetryStrategy(
            maxRetries = 5,
            initialDelayMs = 500,
            maxDelayMs = 10000,
            backoffFactor = 1.5,
            retryableErrors = ErrorType.values().toList()
        )

        /**
         * 创建保守的重试策略，适用于非关键场景。
         */
        fun conservative(): RetryStrategy = RetryStrategy(
            maxRetries = 2,
            initialDelayMs = 2000,
            maxDelayMs = 20000,
            backoffFactor = 3.0,
            retryableErrors = listOf(ErrorType.SERVER_ERROR, ErrorType.TIMEOUT)
        )
    }
}
