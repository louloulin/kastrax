package ai.kastrax.core.workflow.error

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.workflow.RetryConfig
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.executeWithRetry
import ai.kastrax.core.workflow.withRetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import java.util.UUID

/**
 * 支持错误处理的工作流引擎。
 *
 * @property workflows 工作流映射
 * @property globalErrorHandlingConfig 全局错误处理配置
 */
class ErrorHandlingWorkflowEngine(
    private val workflows: Map<String, Workflow>,
    private val globalErrorHandlingConfig: ErrorHandlingConfig = ErrorHandlingConfig.default()
) : KastraXBase(component = "WORKFLOW_ENGINE", name = "error-handling") {
    // 使用父类的 logger

    /**
     * 执行工作流。
     *
     * @param workflowId 工作流ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    suspend fun executeWorkflow(
        workflowId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): WorkflowResult {
        val workflow = workflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        logger.info { "执行工作流: $workflowId" }
        val startTime = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()

        // 创建初始上下文
        val context = WorkflowContext(
            input = input,
            variables = input.toMutableMap(),
            runId = runId
        )

        // 执行工作流
        return executeWorkflowWithErrorHandling(workflow, context, options, startTime)
    }

    /**
     * 流式执行工作流。
     *
     * @param workflowId 工作流ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流状态更新流
     */
    fun streamWorkflow(
        workflowId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): Flow<WorkflowStatusUpdate> = flow {
        val workflow = workflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        logger.info { "流式执行工作流: $workflowId" }
        val startTime = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()

        // 创建初始上下文
        val context = WorkflowContext(
            input = input,
            variables = input.toMutableMap(),
            runId = runId
        )

        // 发送开始状态
        emit(WorkflowStatusUpdate.Started(workflowId, runId))

        // 执行工作流
        // 获取工作流的所有步骤
        val workflowSteps = when (workflow) {
            is ai.kastrax.core.workflow.SimpleWorkflow -> workflow.steps
            else -> emptyMap() // 如果不是SimpleWorkflow，则使用空Map
        }

        // 执行步骤
        val executedSteps = mutableMapOf<String, WorkflowStepResult>()
        var currentStepIndex = 0
        val totalSteps = workflowSteps.size

        // 执行每个步骤
        for ((index, entry) in workflowSteps.entries.withIndex()) {
            currentStepIndex = index
            val stepId = entry.key
            val step = entry.value

            // 发送步骤开始状态
            emit(WorkflowStatusUpdate.StepStarted(
                workflowId = workflowId,
                runId = runId,
                stepId = stepId,
                stepIndex = index,
                totalSteps = totalSteps
            ))

            // 检查步骤的前置条件
            val canExecute = checkStepPreconditions(step, context, executedSteps)
            if (!canExecute) {
                // 跳过步骤
                val skipResult = WorkflowStepResult(
                    stepId = stepId,
                    success = true,
                    output = emptyMap(),
                    skipped = true
                )
                executedSteps[stepId] = skipResult

                // 发送步骤跳过状态
                emit(WorkflowStatusUpdate.StepSkipped(
                    workflowId = workflowId,
                    runId = runId,
                    stepId = stepId,
                    stepIndex = index,
                    totalSteps = totalSteps
                ))
                continue
            }

            // 执行步骤
            val stepResult = executeStepWithErrorHandling(step, context, executedSteps)
            executedSteps[stepId] = stepResult

            // 更新上下文
            context.steps[stepId] = stepResult

            // 发送步骤完成状态
            if (stepResult.success) {
                emit(WorkflowStatusUpdate.StepCompleted(
                    workflowId = workflowId,
                    runId = runId,
                    stepId = stepId,
                    stepIndex = index,
                    totalSteps = totalSteps,
                    result = stepResult
                ))
            } else {
                emit(WorkflowStatusUpdate.StepFailed(
                    workflowId = workflowId,
                    runId = runId,
                    stepId = stepId,
                    stepIndex = index,
                    totalSteps = totalSteps,
                    result = stepResult
                ))

                // 如果步骤失败且没有被处理，终止工作流
                if (!stepResult.errorHandled) {
                    emit(WorkflowStatusUpdate.Failed(
                        workflowId = workflowId,
                        runId = runId,
                        result = createErrorResult(executedSteps, "Step $stepId failed: ${stepResult.error}", startTime, runId)
                    ))
                    return@flow
                }
            }
        }

        // 创建最终结果
        val result = createSuccessResult(executedSteps, startTime, runId)

        // 发送完成状态
        emit(WorkflowStatusUpdate.Completed(
            workflowId = workflowId,
            runId = runId,
            result = result
        ))
    }

    /**
     * 使用错误处理执行工作流。
     */
    private suspend fun executeWorkflowWithErrorHandling(
        workflow: Workflow,
        context: WorkflowContext,
        options: WorkflowExecuteOptions,
        startTime: Long
    ): WorkflowResult {
        // 执行工作流
        // 获取工作流的所有步骤
        val workflowSteps = when (workflow) {
            is ai.kastrax.core.workflow.SimpleWorkflow -> workflow.steps
            else -> emptyMap() // 如果不是SimpleWorkflow，则使用空Map
        }

        // 执行步骤
        val executedSteps = mutableMapOf<String, WorkflowStepResult>()

        // 执行每个步骤
        for ((stepId, step) in workflowSteps) {

            // 检查步骤的前置条件
            val canExecute = checkStepPreconditions(step, context, executedSteps)
            if (!canExecute) {
                // 跳过步骤
                val skipResult = WorkflowStepResult(
                    stepId = stepId,
                    success = true,
                    output = emptyMap(),
                    skipped = true
                )
                executedSteps[stepId] = skipResult
                continue
            }

            // 执行步骤
            val stepResult = executeStepWithErrorHandling(step, context, executedSteps)
            executedSteps[stepId] = stepResult

            // 更新上下文
            context.steps[stepId] = stepResult

            // 如果步骤失败且没有被处理，终止工作流
            if (!stepResult.success && !stepResult.errorHandled) {
                return createErrorResult(executedSteps, "Step $stepId failed: ${stepResult.error}", startTime, context.runId)
            }
        }

        // 创建最终结果
        return createSuccessResult(executedSteps, startTime, context.runId)
    }

    /**
     * 使用错误处理执行步骤。
     */
    private suspend fun executeStepWithErrorHandling(
        step: WorkflowStep,
        context: WorkflowContext,
        executedSteps: MutableMap<String, WorkflowStepResult>
    ): WorkflowStepResult {
        // 获取步骤配置
        val stepConfig = step.config
        val retryConfig = stepConfig?.retryConfig
        val errorHandlingConfig = stepConfig?.errorHandlingConfig ?: globalErrorHandlingConfig

        try {
            // 如果配置了重试机制，则手动实现重试逻辑
            if (retryConfig != null) {
                var attempt = 1
                var lastException: Exception? = null

                while (attempt <= retryConfig.maxRetries + 1) { // +1 是因为第一次尝试不算重试
                    try {
                        return step.execute(context)
                    } catch (e: Exception) {
                        lastException = e

                        // 检查是否可以重试
                        if (!retryConfig.isRetryable(e) || attempt >= retryConfig.maxRetries + 1) {
                            // 如果不能重试或者已经达到最大重试次数，尝试应用错误恢复策略
                            return handleStepError(step, context, e, errorHandlingConfig)
                        }

                        // 计算延迟时间
                        val delayMillis = retryConfig.calculateDelay(attempt)

                        // 记录重试信息
                        logger.debug { "步骤 ${step.id} 执行失败，将在 $delayMillis 毫秒后重试，当前尝试次数: $attempt" }

                        // 延迟后重试
                        kotlinx.coroutines.delay(delayMillis)
                        attempt++
                    }
                }

                // 如果所有重试都失败，尝试应用错误恢复策略
                return handleStepError(step, context, lastException ?: Exception("Unknown error"), errorHandlingConfig)
            } else {
                // 直接执行步骤
                try {
                    return step.execute(context)
                } catch (e: Exception) {
                    // 执行失败，尝试应用错误恢复策略
                    return handleStepError(step, context, e, errorHandlingConfig)
                }
            }
        } catch (e: Exception) {
            // 如果错误处理也失败，返回失败结果
            logger.error(e) { "步骤 ${step.id} 执行失败，错误处理也失败" }
            return WorkflowStepResult(
                stepId = step.id,
                success = false,
                output = emptyMap(),
                error = "Error handling failed: ${e.message}",
                executionTime = 0
            )
        }
    }

    /**
     * 处理步骤错误。
     */
    private suspend fun handleStepError(
        step: WorkflowStep,
        context: WorkflowContext,
        error: Throwable,
        errorHandlingConfig: ErrorHandlingConfig
    ): WorkflowStepResult {
        logger.debug { "处理步骤 ${step.id} 的错误: ${error.message}" }

        // 获取错误对应的恢复策略类型
        val strategyType = errorHandlingConfig.getRecoveryStrategyType(error)
        if (strategyType == null) {
            // 如果没有找到恢复策略，返回失败结果
            logger.debug { "没有找到错误 ${error.javaClass.name} 的恢复策略" }
            return WorkflowStepResult(
                stepId = step.id,
                success = false,
                output = emptyMap(),
                error = error.message,
                executionTime = 0
            )
        }

        // 创建恢复策略
        val strategy = errorHandlingConfig.createRecoveryStrategy(strategyType, context, step, error)

        // 应用恢复策略
        val recoveryResult = strategy.apply(context, step, error)

        // 处理恢复结果
        return if (recoveryResult.success) {
            // 如果恢复成功，检查是否需要重试
            if (strategyType == RecoveryStrategyType.RETRY) {
                // 如果是重试策略，则重新执行步骤
                try {
                    return step.execute(context)
                } catch (e: Exception) {
                    // 如果重试仍然失败，则递归处理错误
                    return handleStepError(step, context, e, errorHandlingConfig)
                }
            } else {
                // 如果不是重试策略，返回恢复后的步骤结果
                val stepResult = recoveryResult.stepResult ?: WorkflowStepResult(
                    stepId = step.id,
                    success = true,
                    output = mapOf("recovered" to true, "originalError" to error.message),
                    errorHandled = true,
                    executionTime = 0
                )

                // 标记错误已处理
                stepResult.copy(errorHandled = true)
            }
        } else {
            // 如果恢复失败，返回失败结果
            WorkflowStepResult(
                stepId = step.id,
                success = false,
                output = emptyMap(),
                error = recoveryResult.stepResult?.error ?: error.message,
                executionTime = 0
            )
        }
    }

    /**
     * 检查步骤的前置条件。
     */
    private fun checkStepPreconditions(
        step: WorkflowStep,
        context: WorkflowContext,
        executedSteps: Map<String, WorkflowStepResult>
    ): Boolean {
        // 检查前置步骤是否都已成功执行
        for (afterStepId in step.after) {
            val afterStepResult = executedSteps[afterStepId]
            if (afterStepResult == null || !afterStepResult.success) {
                logger.debug { "步骤 ${step.id} 的前置步骤 $afterStepId 未成功执行，跳过" }
                return false
            }
        }

        // 检查条件是否满足
        if (!step.condition(context)) {
            logger.debug { "步骤 ${step.id} 的条件不满足，跳过" }
            return false
        }

        return true
    }

    /**
     * 创建成功结果。
     */
    private fun createSuccessResult(
        executedSteps: Map<String, WorkflowStepResult>,
        startTime: Long,
        runId: String? = null
    ): WorkflowResult {
        // 收集所有步骤的输出
        val output = executedSteps.values
            .filter { it.success && !it.skipped }
            .flatMap { it.output.entries }
            .associate { it.key to it.value }

        return WorkflowResult(
            success = true,
            output = output,
            steps = executedSteps,
            executionTime = System.currentTimeMillis() - startTime,
            runId = runId
        )
    }

    /**
     * 创建错误结果。
     */
    private fun createErrorResult(
        executedSteps: Map<String, WorkflowStepResult>,
        error: String,
        startTime: Long,
        runId: String? = null
    ): WorkflowResult {
        return WorkflowResult(
            success = false,
            output = emptyMap(),
            steps = executedSteps,
            error = error,
            executionTime = System.currentTimeMillis() - startTime,
            runId = runId
        )
    }
}

/**
 * 工作流状态更新。
 */
sealed class WorkflowStatusUpdate {
    /**
     * 工作流ID。
     */
    abstract val workflowId: String

    /**
     * 运行ID。
     */
    abstract val runId: String

    /**
     * 工作流开始。
     */
    data class Started(
        override val workflowId: String,
        override val runId: String
    ) : WorkflowStatusUpdate()

    /**
     * 工作流完成。
     */
    data class Completed(
        override val workflowId: String,
        override val runId: String,
        val result: WorkflowResult
    ) : WorkflowStatusUpdate()

    /**
     * 工作流失败。
     */
    data class Failed(
        override val workflowId: String,
        override val runId: String,
        val result: WorkflowResult
    ) : WorkflowStatusUpdate()

    /**
     * 步骤开始。
     */
    data class StepStarted(
        override val workflowId: String,
        override val runId: String,
        val stepId: String,
        val stepIndex: Int,
        val totalSteps: Int
    ) : WorkflowStatusUpdate()

    /**
     * 步骤完成。
     */
    data class StepCompleted(
        override val workflowId: String,
        override val runId: String,
        val stepId: String,
        val stepIndex: Int,
        val totalSteps: Int,
        val result: WorkflowStepResult
    ) : WorkflowStatusUpdate()

    /**
     * 步骤失败。
     */
    data class StepFailed(
        override val workflowId: String,
        override val runId: String,
        val stepId: String,
        val stepIndex: Int,
        val totalSteps: Int,
        val result: WorkflowStepResult
    ) : WorkflowStatusUpdate()

    /**
     * 步骤跳过。
     */
    data class StepSkipped(
        override val workflowId: String,
        override val runId: String,
        val stepId: String,
        val stepIndex: Int,
        val totalSteps: Int
    ) : WorkflowStatusUpdate()
}
