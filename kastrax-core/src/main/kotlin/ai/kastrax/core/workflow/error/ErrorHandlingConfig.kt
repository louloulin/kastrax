package ai.kastrax.core.workflow.error

import ai.kastrax.core.workflow.RetryConfig
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import kotlinx.serialization.Serializable
import java.time.Duration

/**
 * 错误处理配置。
 *
 * @property retryConfig 重试配置
 * @property recoveryStrategies 恢复策略映射，键为异常类名，值为恢复策略类型
 * @property errorHandler 错误处理器
 * @property propagateErrors 是否传播错误
 */
data class ErrorHandlingConfig(
    val retryConfig: RetryConfig? = null,
    val recoveryStrategies: Map<String, RecoveryStrategyType> = emptyMap(),
    val errorHandler: ErrorHandler? = null,
    val propagateErrors: Boolean = true
) {
    /**
     * 获取异常对应的恢复策略类型。
     *
     * @param throwable 异常
     * @return 恢复策略类型，如果没有找到则返回null
     */
    fun getRecoveryStrategyType(throwable: Throwable): RecoveryStrategyType? {
        // 按照异常类的继承层次结构查找匹配的恢复策略
        var currentClass: Class<*>? = throwable.javaClass
        while (currentClass != null && currentClass != Throwable::class.java) {
            val strategyType = recoveryStrategies[currentClass.name]
            if (strategyType != null) {
                return strategyType
            }
            currentClass = currentClass.superclass
        }
        
        // 如果没有找到特定异常类型的策略，尝试使用通用的Exception策略
        return recoveryStrategies[Exception::class.java.name]
    }
    
    /**
     * 创建恢复策略。
     *
     * @param type 恢复策略类型
     * @param context 工作流上下文
     * @param step 工作流步骤
     * @param error 异常
     * @return 恢复策略
     */
    fun createRecoveryStrategy(
        type: RecoveryStrategyType,
        context: WorkflowContext,
        step: WorkflowStep,
        error: Throwable
    ): ErrorRecoveryStrategy {
        return when (type) {
            RecoveryStrategyType.RETRY -> {
                val config = retryConfig ?: RetryConfig()
                RetryRecoveryStrategy(config.maxRetries)
            }
            RecoveryStrategyType.SKIP -> SkipRecoveryStrategy()
            RecoveryStrategyType.ROLLBACK -> {
                // 默认回滚到上一个步骤
                val previousStepId = context.steps.keys.lastOrNull { it != step.id } ?: step.id
                RollbackRecoveryStrategy(previousStepId)
            }
            RecoveryStrategyType.FALLBACK -> {
                // 创建一个简单的替代步骤，返回默认值
                val fallbackStep = object : WorkflowStep {
                    override val id: String = "${step.id}-fallback"
                    override val name: String = "Fallback for ${step.name}"
                    override val description: String = "Fallback step for ${step.id}"
                    override val after: List<String> = emptyList()
                    override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
                    override val config: ai.kastrax.core.workflow.StepConfig? = null
                    
                    override suspend fun execute(context: WorkflowContext): ai.kastrax.core.workflow.WorkflowStepResult {
                        return ai.kastrax.core.workflow.WorkflowStepResult(
                            stepId = id,
                            success = true,
                            output = mapOf("fallback" to true, "originalError" to error.message),
                            executionTime = 0
                        )
                    }
                }
                FallbackRecoveryStrategy(fallbackStep)
            }
            RecoveryStrategyType.TERMINATE -> TerminateRecoveryStrategy("Workflow terminated due to error: ${error.message}")
            RecoveryStrategyType.CUSTOM -> {
                // 如果有自定义错误处理器，使用它创建恢复策略
                if (errorHandler != null) {
                    CustomRecoveryStrategy { ctx, stp, err ->
                        errorHandler.handleError(ctx, stp, err)
                    }
                } else {
                    // 默认终止工作流
                    TerminateRecoveryStrategy("No custom error handler defined")
                }
            }
        }
    }
    
    companion object {
        /**
         * 创建默认的错误处理配置。
         *
         * @return 默认错误处理配置
         */
        fun default(): ErrorHandlingConfig {
            return ErrorHandlingConfig(
                retryConfig = RetryConfig(),
                recoveryStrategies = mapOf(
                    Exception::class.java.name to RecoveryStrategyType.RETRY,
                    RuntimeException::class.java.name to RecoveryStrategyType.RETRY,
                    IllegalArgumentException::class.java.name to RecoveryStrategyType.TERMINATE,
                    IllegalStateException::class.java.name to RecoveryStrategyType.TERMINATE
                ),
                propagateErrors = true
            )
        }
    }
}

/**
 * 错误处理器接口。
 */
interface ErrorHandler {
    /**
     * 处理错误。
     *
     * @param context 工作流上下文
     * @param step 失败的步骤
     * @param error 错误信息
     * @return 恢复结果
     */
    suspend fun handleError(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult
}

/**
 * 错误处理器构建器。
 */
class ErrorHandlingConfigBuilder {
    private var retryConfig: RetryConfig? = null
    private val recoveryStrategies = mutableMapOf<String, RecoveryStrategyType>()
    private var errorHandler: ErrorHandler? = null
    private var propagateErrors: Boolean = true
    
    /**
     * 设置重试配置。
     */
    fun retry(
        maxRetries: Int = 3,
        initialDelay: Duration = Duration.ofMillis(100),
        maxDelay: Duration = Duration.ofSeconds(30),
        backoffFactor: Double = 2.0,
        jitter: Double = 0.1,
        retryableExceptions: Set<Class<out Throwable>> = setOf(Exception::class.java)
    ) {
        retryConfig = RetryConfig(
            maxRetries = maxRetries,
            initialDelay = initialDelay,
            maxDelay = maxDelay,
            backoffFactor = backoffFactor,
            jitter = jitter,
            retryableExceptions = retryableExceptions
        )
    }
    
    /**
     * 添加恢复策略。
     */
    fun onError(exceptionClass: Class<out Throwable>, strategyType: RecoveryStrategyType) {
        recoveryStrategies[exceptionClass.name] = strategyType
    }
    
    /**
     * 设置错误处理器。
     */
    fun errorHandler(handler: ErrorHandler) {
        errorHandler = handler
    }
    
    /**
     * 设置错误处理器。
     */
    fun errorHandler(handler: suspend (context: WorkflowContext, step: WorkflowStep, error: Throwable) -> RecoveryResult) {
        errorHandler = object : ErrorHandler {
            override suspend fun handleError(
                context: WorkflowContext,
                step: WorkflowStep,
                error: Throwable
            ): RecoveryResult {
                return handler(context, step, error)
            }
        }
    }
    
    /**
     * 设置是否传播错误。
     */
    fun propagateErrors(propagate: Boolean) {
        propagateErrors = propagate
    }
    
    /**
     * 构建错误处理配置。
     */
    fun build(): ErrorHandlingConfig {
        return ErrorHandlingConfig(
            retryConfig = retryConfig,
            recoveryStrategies = recoveryStrategies,
            errorHandler = errorHandler,
            propagateErrors = propagateErrors
        )
    }
}
