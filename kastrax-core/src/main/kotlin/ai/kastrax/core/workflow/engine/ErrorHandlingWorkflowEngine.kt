package ai.kastrax.core.workflow.engine

// Commented out due to compilation issues
/*
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.callback.WorkflowCallbackManager
import ai.kastrax.core.workflow.error.ErrorHandler
import ai.kastrax.core.workflow.error.ErrorHandlingConfig
import ai.kastrax.core.workflow.error.ErrorRecoveryStrategy
import ai.kastrax.core.workflow.error.RecoveryResult
import ai.kastrax.core.workflow.error.RecoveryStrategyType
import ai.kastrax.core.workflow.event.DefaultWorkflowEventBus
import ai.kastrax.core.workflow.event.ErrorOccurredEvent
import ai.kastrax.core.workflow.event.InMemoryWorkflowEventStorage
import ai.kastrax.core.workflow.event.RecoveryAppliedEvent
import ai.kastrax.core.workflow.event.WorkflowEventBus
import ai.kastrax.core.workflow.event.WorkflowEventStorage
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
*/

// Class commented out due to compilation issues
/*
/**
 * 增强的工作流引擎，提供高级错误处理能力。
 *
 * 这个引擎提供了以下功能：
 * 1. 全局错误处理配置
 * 2. 步骤级别错误处理策略
 * 3. 错误恢复策略应用
 * 4. 错误传播控制
 *
 * @property workflows 工作流映射
 * @property stateStorage 工作流状态存储
 * @property eventBus 事件总线
 * @property eventStorage 事件存储
 * @property globalErrorHandlingConfig 全局错误处理配置
 * @property errorHandler 全局错误处理器
 */
class ErrorHandlingWorkflowEngine(
    private val workflows: Map<String, Workflow>,
    private val stateStorage: WorkflowStateStorage = InMemoryWorkflowStateStorage(),
    private val eventBus: WorkflowEventBus = DefaultWorkflowEventBus(),
    private val eventStorage: WorkflowEventStorage = InMemoryWorkflowEventStorage(),
    private val globalErrorHandlingConfig: ErrorHandlingConfig = ErrorHandlingConfig.default(),
    private val errorHandler: ErrorHandler? = null
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 执行工作流，添加错误处理能力。
     *
     * @param workflowId 工作流ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    override suspend fun executeWorkflow(
        workflowId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions
    ): WorkflowResult {
        // 增强执行选项，添加错误处理
        val enhancedOptions = enhanceOptionsWithErrorHandling(options, workflowId)

        // 调用父类方法执行工作流
        return super.executeWorkflow(workflowId, input, enhancedOptions)
    }

    /**
     * 流式执行工作流，添加错误处理能力。
     *
     * @param workflowId 工作流ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流状态更新流
     */
    suspend fun streamExecuteWorkflow(
        workflowId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): Flow<ai.kastrax.core.workflow.WorkflowStatusUpdate> {
        val workflow = getWorkflow(workflowId)
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        // 增强执行选项，添加错误处理
        val enhancedOptions = enhanceOptionsWithErrorHandling(options, workflowId)

        // 流式执行工作流
        return workflow.streamExecute(input, enhancedOptions)
    }

    /**
     * 恢复工作流，添加错误处理能力。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    override suspend fun resumeWorkflow(
        workflowId: String,
        runId: String,
        stepId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions
    ): WorkflowResult {
        // 增强执行选项，添加错误处理
        val enhancedOptions = enhanceOptionsWithErrorHandling(options, workflowId)

        // 调用父类方法恢复工作流
        return super.resumeWorkflow(workflowId, runId, stepId, input, enhancedOptions)
    }

    /**
     * 处理步骤执行错误。
     *
     * @param step 失败的步骤
     * @param context 工作流上下文
     * @param error 错误信息
     * @param config 错误处理配置
     * @return 步骤执行结果
     */
    private suspend fun handleStepError(
        step: WorkflowStep,
        context: WorkflowContext,
        error: Throwable,
        config: ErrorHandlingConfig?
    ): WorkflowStepResult {
        logger.debug { "处理步骤 ${step.id} 的错误: ${error.message}" }

        // 获取错误处理配置，优先使用步骤级别的配置，然后是全局配置
        val effectiveConfig = config ?: step.config?.errorHandlingConfig ?: globalErrorHandlingConfig

        // 获取恢复策略类型
        val strategyType = effectiveConfig.getRecoveryStrategyType(error)

        if (strategyType != null) {
            logger.debug { "应用恢复策略: $strategyType 到步骤 ${step.id}" }

            // 创建恢复策略
            val strategy = effectiveConfig.createRecoveryStrategy(strategyType, context, step, error)

            // 应用恢复策略
            val recoveryResult = applyRecoveryStrategy(strategy, context, step, error)

            // 发布恢复策略应用事件
            publishRecoveryEvent(context.runId, step.id, strategyType.name, recoveryResult.success)

            // 如果恢复成功，返回恢复后的步骤结果
            if (recoveryResult.success && recoveryResult.stepResult != null) {
                return recoveryResult.stepResult
            }

            // 如果恢复失败，但不终止工作流，返回处理过的错误结果
            if (!recoveryResult.success && !recoveryResult.terminateWorkflow) {
                return WorkflowStepResult.handledError(
                    stepId = step.id,
                    error = error.message ?: "Unknown error",
                    output = emptyMap()
                )
            }
        }

        // 如果没有恢复策略或恢复失败且需要终止工作流，返回失败结果
        return WorkflowStepResult.failed(step.id, error as Exception)
    }

    /**
     * 应用恢复策略。
     *
     * @param strategy 恢复策略
     * @param context 工作流上下文
     * @param step 失败的步骤
     * @param error 错误信息
     * @return 恢复结果
     */
    private suspend fun applyRecoveryStrategy(
        strategy: ErrorRecoveryStrategy,
        context: WorkflowContext,
        step: WorkflowStep,
        error: Throwable
    ): RecoveryResult {
        return try {
            logger.debug { "应用恢复策略: ${strategy.type} 到步骤 ${step.id}" }
            strategy.apply(context, step, error)
        } catch (e: Exception) {
            logger.error(e) { "应用恢复策略失败: ${strategy.type}" }
            RecoveryResult(
                success = false,
                stepResult = WorkflowStepResult.failed(step.id, e),
                terminateWorkflow = true
            )
        }
    }

    /**
     * 发布恢复策略应用事件。
     *
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param strategyType 策略类型
     * @param success 是否成功
     */
    private suspend fun publishRecoveryEvent(
        runId: String?,
        stepId: String,
        strategyType: String,
        success: Boolean
    ) {
        if (runId != null) {
            val event = RecoveryAppliedEvent(
                workflowId = "unknown", // 这里无法获取工作流ID，使用默认值
                runId = runId,
                stepId = stepId,
                strategyType = strategyType,
                success = success
            )
            publishEvent(event)
        }
    }

    /**
     * 增强执行选项，添加错误处理。
     *
     * @param options 原始执行选项
     * @param workflowId 工作流ID
     * @return 增强后的执行选项
     */
    private fun enhanceOptionsWithErrorHandling(
        options: WorkflowExecuteOptions,
        workflowId: String
    ): WorkflowExecuteOptions {
        // 创建步骤错误处理回调
        val originalOnStepError = options.onStepError
        val enhancedOnStepError: (String, Throwable) -> Unit = { stepId, error ->
            // 调用原始回调
            originalOnStepError?.invoke(stepId, error)

            // 记录错误
            logger.error(error) { "步骤 $stepId 执行失败: ${error.message}" }

            // 发布错误事件
            val errorEvent = ErrorOccurredEvent(
                workflowId = workflowId,
                runId = options.threadId ?: "unknown",
                stepId = stepId,
                error = error,
                context = emptyMap()
            )
            runBlocking {
                publishEvent(errorEvent)
            }
        }

        // 返回增强后的选项
        return options.copy(
            onStepError = enhancedOnStepError
        )
    }

    /**
     * 设置全局错误处理配置。
     *
     * @param config 错误处理配置
     */
    fun setGlobalErrorHandlingConfig(config: ErrorHandlingConfig) {
        this.globalErrorHandlingConfig = config
    }

    /**
     * 获取全局错误处理配置。
     *
     * @return 错误处理配置
     */
    fun getGlobalErrorHandlingConfig(): ErrorHandlingConfig {
        return globalErrorHandlingConfig
    }
}
*/
