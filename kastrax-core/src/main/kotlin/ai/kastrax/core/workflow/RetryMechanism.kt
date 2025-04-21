package ai.kastrax.core.workflow

import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 工作流步骤配置，包含重试机制的配置。
 *
 * @property timeout 超时时间
 * @property maxTokens 最大令牌数
 * @property onError 错误处理回调
 * @property retryConfig 重试配置
 */
data class StepConfig(
    val timeout: Duration = Duration.ofMinutes(5),
    val maxTokens: Int? = null,
    val onError: ((Throwable) -> Any?)? = null,
    val retryConfig: RetryConfig? = null
)

/**
 * 重试配置。
 *
 * @property maxRetries 最大重试次数
 * @property initialDelay 初始延迟时间
 * @property maxDelay 最大延迟时间
 * @property backoffFactor 退避因子
 * @property jitter 抖动因子
 * @property retryableExceptions 可重试的异常类型
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelay: Duration = Duration.ofMillis(100),
    val maxDelay: Duration = Duration.ofSeconds(30),
    val backoffFactor: Double = 2.0,
    val jitter: Double = 0.1,
    val retryableExceptions: Set<Class<out Throwable>> = setOf(Exception::class.java)
) {
    /**
     * 检查异常是否可重试。
     *
     * @param throwable 异常
     * @return 是否可重试
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return retryableExceptions.any { it.isInstance(throwable) }
    }

    /**
     * 计算下一次重试的延迟时间。
     *
     * @param attempt 当前尝试次数
     * @return 延迟时间（毫秒）
     */
    fun calculateDelay(attempt: Int): Long {
        // 指数退避算法
        val baseDelay = initialDelay.toMillis() * backoffFactor.pow(attempt - 1)
        
        // 添加抖动
        val jitterAmount = baseDelay * jitter
        val jitteredDelay = baseDelay + Random.nextDouble(-jitterAmount, jitterAmount)
        
        // 确保延迟在合理范围内
        return min(maxDelay.toMillis(), max(initialDelay.toMillis(), jitteredDelay.toLong()))
    }
}

/**
 * Double的幂运算扩展函数。
 */
private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())

/**
 * 使用重试机制执行操作。
 *
 * @param config 重试配置
 * @param operation 要执行的操作
 * @return 操作结果
 */
suspend fun <T> withRetry(
    config: RetryConfig,
    operation: suspend () -> T
): T {
    var attempt = 1
    var lastException: Throwable? = null

    while (attempt <= config.maxRetries + 1) { // +1 是因为第一次尝试不算重试
        try {
            return operation()
        } catch (e: Throwable) {
            lastException = e
            
            // 检查是否可以重试
            if (!config.isRetryable(e) || attempt > config.maxRetries) {
                throw e
            }
            
            // 计算延迟时间
            val delayMillis = config.calculateDelay(attempt)
            
            // 延迟后重试
            delay(delayMillis)
            attempt++
        }
    }
    
    // 这里理论上不会执行到，因为如果所有重试都失败，会在循环中抛出异常
    throw lastException ?: IllegalStateException("Retry failed for unknown reason")
}

/**
 * 工作流步骤的扩展函数，添加重试机制。
 *
 * @param context 工作流上下文
 * @param config 重试配置
 * @return 步骤执行结果
 */
suspend fun WorkflowStep.executeWithRetry(
    context: WorkflowContext,
    config: RetryConfig
): WorkflowStepResult {
    return withRetry(config) {
        execute(context)
    }
}
