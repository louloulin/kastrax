package ai.kastrax.core.workflow.error

import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import kotlinx.serialization.Serializable

/**
 * 错误恢复策略枚举。
 */
enum class RecoveryStrategyType {
    /**
     * 重试策略，尝试重新执行失败的步骤。
     */
    RETRY,

    /**
     * 跳过策略，跳过失败的步骤，继续执行后续步骤。
     */
    SKIP,

    /**
     * 回滚策略，回滚到之前的状态。
     */
    ROLLBACK,

    /**
     * 替代执行策略，执行替代步骤。
     */
    FALLBACK,

    /**
     * 终止策略，终止工作流执行。
     */
    TERMINATE,

    /**
     * 自定义策略，执行自定义恢复逻辑。
     */
    CUSTOM
}

/**
 * 错误恢复策略接口。
 */
interface ErrorRecoveryStrategy {
    /**
     * 策略类型。
     */
    val type: RecoveryStrategyType

    /**
     * 应用恢复策略。
     *
     * @param context 工作流上下文
     * @param step 失败的步骤
     * @param error 错误信息
     * @return 恢复结果
     */
    suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult
}

/**
 * 恢复结果。
 *
 * @property success 是否成功恢复
 * @property stepResult 恢复后的步骤结果
 * @property nextSteps 下一步要执行的步骤ID列表
 * @property terminateWorkflow 是否终止工作流
 */
data class RecoveryResult(
    val success: Boolean,
    val stepResult: WorkflowStepResult? = null,
    val nextSteps: List<String> = emptyList(),
    val terminateWorkflow: Boolean = false
)

/**
 * 重试恢复策略。
 *
 * @property maxRetries 最大重试次数
 * @property currentRetry 当前重试次数
 */
class RetryRecoveryStrategy(
    val maxRetries: Int = 3,
    val currentRetry: Int = 0
) : ErrorRecoveryStrategy {
    override val type = RecoveryStrategyType.RETRY

    override suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult {
        // 如果已经达到最大重试次数，则恢复失败
        if (currentRetry >= maxRetries) {
            return RecoveryResult(
                success = false,
                stepResult = WorkflowStepResult.failed(step.id, error as Exception),
                terminateWorkflow = false
            )
        }

        // 否则，返回成功结果，表示应该重试
        return RecoveryResult(
            success = true,
            nextSteps = listOf(step.id)
        )
    }
}

/**
 * 跳过恢复策略。
 *
 * @property defaultOutput 默认输出
 */
class SkipRecoveryStrategy(
    val defaultOutput: Map<String, Any?> = emptyMap()
) : ErrorRecoveryStrategy {
    override val type = RecoveryStrategyType.SKIP

    override suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult {
        // 创建一个成功的步骤结果，但标记为跳过
        val stepResult = WorkflowStepResult(
            stepId = step.id,
            success = true,
            output = defaultOutput,
            skipped = true,
            error = error.message
        )

        // 返回成功结果，表示应该跳过当前步骤
        return RecoveryResult(
            success = true,
            stepResult = stepResult,
            nextSteps = emptyList()
        )
    }
}

/**
 * 回滚恢复策略。
 *
 * @property rollbackToStepId 回滚到的步骤ID
 */
class RollbackRecoveryStrategy(
    val rollbackToStepId: String
) : ErrorRecoveryStrategy {
    override val type = RecoveryStrategyType.ROLLBACK

    override suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult {
        // 返回成功结果，表示应该回滚到指定步骤
        return RecoveryResult(
            success = true,
            nextSteps = listOf(rollbackToStepId)
        )
    }
}

/**
 * 替代执行恢复策略。
 *
 * @property fallbackStep 替代步骤
 */
class FallbackRecoveryStrategy(
    val fallbackStep: WorkflowStep
) : ErrorRecoveryStrategy {
    override val type = RecoveryStrategyType.FALLBACK

    override suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult {
        // 执行替代步骤
        val fallbackResult = try {
            fallbackStep.execute(context)
        } catch (e: Exception) {
            // 如果替代步骤也失败，则返回失败结果
            return RecoveryResult(
                success = false,
                stepResult = WorkflowStepResult.failed(fallbackStep.id, e),
                terminateWorkflow = false
            )
        }

        // 返回成功结果，包含替代步骤的执行结果
        return RecoveryResult(
            success = true,
            stepResult = fallbackResult,
            nextSteps = emptyList()
        )
    }
}

/**
 * 终止恢复策略。
 *
 * @property errorMessage 错误消息
 */
class TerminateRecoveryStrategy(
    val errorMessage: String? = null
) : ErrorRecoveryStrategy {
    override val type = RecoveryStrategyType.TERMINATE

    override suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult {
        // 返回失败结果，表示应该终止工作流
        return RecoveryResult(
            success = false,
            stepResult = WorkflowStepResult.failed(step.id, error as Exception, errorMessage),
            terminateWorkflow = true
        )
    }
}

/**
 * 自定义恢复策略。
 *
 * @property handler 自定义处理函数
 */
class CustomRecoveryStrategy(
    val handler: suspend (context: WorkflowContext, step: WorkflowStep, error: Throwable) -> RecoveryResult
) : ErrorRecoveryStrategy {
    override val type = RecoveryStrategyType.CUSTOM

    override suspend fun apply(context: WorkflowContext, step: WorkflowStep, error: Throwable): RecoveryResult {
        return handler(context, step, error)
    }
}
