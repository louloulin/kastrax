package ai.kastrax.core.workflow.error

import ai.kastrax.core.workflow.WorkflowStepResult

/**
 * 增强的工作流步骤执行结果，添加了错误处理相关的字段。
 *
 * @property stepId 步骤ID
 * @property success 是否成功
 * @property output 输出数据
 * @property error 错误信息
 * @property executionTime 执行时间（毫秒）
 * @property errorHandled 错误是否已处理
 * @property recoveryStrategy 使用的恢复策略
 * @property retryCount 重试次数
 * @property skipped 是否跳过
 */
data class EnhancedWorkflowStepResult(
    val stepId: String,
    val success: Boolean,
    val output: Map<String, Any?>,
    val error: String? = null,
    val executionTime: Long = 0,
    val errorHandled: Boolean = false,
    val recoveryStrategy: RecoveryStrategyType? = null,
    val retryCount: Int = 0,
    val skipped: Boolean = false
) {
    /**
     * 转换为标准的工作流步骤结果。
     */
    fun toStandardResult(): WorkflowStepResult {
        return WorkflowStepResult(
            stepId = stepId,
            success = success,
            output = output,
            error = error,
            executionTime = executionTime
        )
    }

    companion object {
        /**
         * 从标准的工作流步骤结果创建增强的工作流步骤结果。
         */
        fun fromStandardResult(result: WorkflowStepResult, errorHandled: Boolean = false): EnhancedWorkflowStepResult {
            return EnhancedWorkflowStepResult(
                stepId = result.stepId,
                success = result.success,
                output = result.output,
                error = result.error,
                executionTime = result.executionTime,
                errorHandled = errorHandled
            )
        }

        /**
         * 创建成功结果。
         */
        fun success(stepId: String, output: Map<String, Any?>, executionTime: Long = 0): EnhancedWorkflowStepResult {
            return EnhancedWorkflowStepResult(
                stepId = stepId,
                success = true,
                output = output,
                executionTime = executionTime
            )
        }

        /**
         * 创建失败结果。
         */
        fun failed(stepId: String, error: String, executionTime: Long = 0): EnhancedWorkflowStepResult {
            return EnhancedWorkflowStepResult(
                stepId = stepId,
                success = false,
                output = emptyMap(),
                error = error,
                executionTime = executionTime
            )
        }

        /**
         * 创建失败结果。
         */
        fun failed(stepId: String, exception: Throwable, executionTime: Long = 0): EnhancedWorkflowStepResult {
            return failed(stepId, exception.message ?: "Unknown error", executionTime)
        }

        /**
         * 创建已处理错误的结果。
         */
        fun handledError(
            stepId: String,
            error: String,
            recoveryStrategy: RecoveryStrategyType,
            output: Map<String, Any?> = emptyMap(),
            executionTime: Long = 0
        ): EnhancedWorkflowStepResult {
            return EnhancedWorkflowStepResult(
                stepId = stepId,
                success = true,
                output = output,
                error = error,
                executionTime = executionTime,
                errorHandled = true,
                recoveryStrategy = recoveryStrategy
            )
        }

        /**
         * 创建跳过的结果。
         */
        fun skipped(stepId: String, reason: String? = null): EnhancedWorkflowStepResult {
            return EnhancedWorkflowStepResult(
                stepId = stepId,
                success = true,
                output = emptyMap(),
                error = reason,
                executionTime = 0,
                skipped = true
            )
        }

        /**
         * 创建重试的结果。
         */
        fun retried(
            stepId: String,
            originalError: String,
            retryCount: Int,
            output: Map<String, Any?>,
            executionTime: Long = 0
        ): EnhancedWorkflowStepResult {
            return EnhancedWorkflowStepResult(
                stepId = stepId,
                success = true,
                output = output,
                error = originalError,
                executionTime = executionTime,
                errorHandled = true,
                recoveryStrategy = RecoveryStrategyType.RETRY,
                retryCount = retryCount
            )
        }
    }
}
