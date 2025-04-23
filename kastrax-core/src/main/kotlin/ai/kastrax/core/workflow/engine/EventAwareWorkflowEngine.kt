package ai.kastrax.core.workflow.engine

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.callback.StepCallback
import ai.kastrax.core.workflow.callback.WorkflowCallback
import ai.kastrax.core.workflow.callback.WorkflowCallbackManager
import ai.kastrax.core.workflow.event.CustomWorkflowEvent
import ai.kastrax.core.workflow.event.DefaultWorkflowEventBus
import ai.kastrax.core.workflow.event.ErrorOccurredEvent
import ai.kastrax.core.workflow.event.InMemoryWorkflowEventStorage
import ai.kastrax.core.workflow.event.RecoveryAppliedEvent
import ai.kastrax.core.workflow.event.RetryStartedEvent
import ai.kastrax.core.workflow.event.StepCompletedEvent
import ai.kastrax.core.workflow.event.StepFailedEvent
import ai.kastrax.core.workflow.event.StepResumedEvent
import ai.kastrax.core.workflow.event.StepSkippedEvent
import ai.kastrax.core.workflow.event.StepStartedEvent
import ai.kastrax.core.workflow.event.StepSuspendedEvent
import ai.kastrax.core.workflow.event.WorkflowCompletedEvent
import ai.kastrax.core.workflow.event.WorkflowEvent
import ai.kastrax.core.workflow.event.WorkflowEventBus
import ai.kastrax.core.workflow.event.WorkflowEventStorage
import ai.kastrax.core.workflow.event.WorkflowFailedEvent
import ai.kastrax.core.workflow.event.WorkflowResumedEvent
import ai.kastrax.core.workflow.event.WorkflowStartedEvent
import ai.kastrax.core.workflow.event.WorkflowSuspendedEvent
import ai.kastrax.core.workflow.event.WorkflowCanceledEvent
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStatus
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import ai.kastrax.core.workflow.state.toWorkflowState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.UUID

/**
 * 支持事件和回调的工作流引擎。
 */
class EventAwareWorkflowEngine(
    private val workflows: Map<String, Workflow>,
    private val stateStorage: WorkflowStateStorage = InMemoryWorkflowStateStorage(),
    private val eventBus: WorkflowEventBus = DefaultWorkflowEventBus(),
    private val eventStorage: WorkflowEventStorage = InMemoryWorkflowEventStorage()
) : KastraXBase(component = "EVENT_AWARE_WORKFLOW_ENGINE", name = "default") {

    // 状态更新流
    private val _stateUpdates = MutableSharedFlow<WorkflowState>(replay = 0)
    val stateUpdates = _stateUpdates.asSharedFlow()

    // 回调管理器
    private val callbackManager = WorkflowCallbackManager()

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

        // 生成运行ID
        val runId = UUID.randomUUID().toString()
        logger.info { "执行工作流: $workflowId, 运行ID: $runId" }

        // 调用工作流开始前回调
        callWorkflowCallbacks { it.beforeWorkflowStart(workflowId, runId, input) }

        // 发布工作流开始事件
        val startEvent = WorkflowStartedEvent(workflowId, runId, input)
        publishEvent(startEvent)

        // 执行工作流
        val enhancedOptions = enhanceOptions(options, workflowId, runId)
        val startTime = System.currentTimeMillis()
        val result = try {
            workflow.execute(input, enhancedOptions)
        } catch (e: Exception) {
            logger.error(e) { "工作流执行失败: $workflowId, $runId" }

            // 发布错误事件
            val errorEvent = ErrorOccurredEvent(workflowId, runId, null, e, mapOf("input" to input))
            publishEvent(errorEvent)

            // 创建失败结果
            val failResult = WorkflowResult(
                success = false,
                output = emptyMap(),
                steps = emptyMap(),
                error = e.message,
                executionTime = System.currentTimeMillis() - startTime,
                runId = runId
            )

            // 调用工作流失败回调
            callWorkflowCallbacks { it.onWorkflowFail(workflowId, runId, e.message, failResult.executionTime) }

            // 发布工作流失败事件
            val failEvent = WorkflowFailedEvent(workflowId, runId, e.message, failResult.executionTime)
            publishEvent(failEvent)

            failResult
        }

        // 保存工作流状态
        val state = result.toWorkflowState(
            runId = runId,
            workflowId = workflowId
        )
        stateStorage.saveWorkflowState(workflowId, runId, state)

        // 发送状态更新
        _stateUpdates.emit(state)

        // 根据结果发布相应事件
        if (result.success) {
            // 调用工作流完成回调
            callWorkflowCallbacks { it.afterWorkflowComplete(workflowId, runId, result.output, result.executionTime) }

            // 发布工作流完成事件
            val completeEvent = WorkflowCompletedEvent(workflowId, runId, result.output, result.executionTime)
            publishEvent(completeEvent)
        } else {
            // 调用工作流失败回调
            callWorkflowCallbacks { it.onWorkflowFail(workflowId, runId, result.error, result.executionTime) }

            // 发布工作流失败事件
            val failEvent = WorkflowFailedEvent(workflowId, runId, result.error, result.executionTime)
            publishEvent(failEvent)
        }

        return result
    }

    /**
     * 恢复工作流。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    suspend fun resumeWorkflow(
        workflowId: String,
        runId: String,
        stepId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): WorkflowResult {
        val workflow = workflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        // 获取工作流状态
        val state = stateStorage.getWorkflowState(workflowId, runId)
            ?: throw IllegalArgumentException("Workflow state not found: $workflowId, $runId")

        // 检查工作流状态
        if (state.status != WorkflowStateStatus.SUSPENDED) {
            throw IllegalStateException("Workflow is not suspended: $workflowId, $runId")
        }

        // 检查步骤状态
        val stepState = state.steps[stepId]
            ?: throw IllegalArgumentException("Step not found: $stepId")

        if (stepState.status != ai.kastrax.core.workflow.state.StepStateStatus.SUSPENDED) {
            throw IllegalStateException("Step is not suspended: $stepId")
        }

        // 调用工作流恢复回调
        callWorkflowCallbacks { it.onWorkflowResume(workflowId, runId, stepId, input) }

        // 发布工作流恢复事件
        val resumeEvent = WorkflowResumedEvent(workflowId, runId, stepId, input)
        publishEvent(resumeEvent)

        // 发布步骤恢复事件
        val stepResumeEvent = StepResumedEvent(workflowId, runId, stepId, input)
        publishEvent(stepResumeEvent)

        // 更新工作流状态
        val updatedState = state.copy(
            status = WorkflowStateStatus.RUNNING,
            suspendedSteps = state.suspendedSteps - stepId,
            updatedAt = System.currentTimeMillis()
        )
        stateStorage.saveWorkflowState(workflowId, runId, updatedState)

        // 发送状态更新
        _stateUpdates.emit(updatedState)

        // 执行工作流
        val enhancedOptions = enhanceOptions(options, workflowId, runId)
        val result = workflow.execute(input, enhancedOptions)

        // 保存工作流状态
        val finalState = result.toWorkflowState(
            runId = runId,
            workflowId = workflowId,
            suspendedSteps = updatedState.suspendedSteps
        )
        stateStorage.saveWorkflowState(workflowId, runId, finalState)

        // 发送状态更新
        _stateUpdates.emit(finalState)

        // 根据结果发布相应事件
        if (result.success) {
            // 调用工作流完成回调
            callWorkflowCallbacks { it.afterWorkflowComplete(workflowId, runId, result.output, result.executionTime) }

            // 发布工作流完成事件
            val completeEvent = WorkflowCompletedEvent(workflowId, runId, result.output, result.executionTime)
            publishEvent(completeEvent)
        } else {
            // 调用工作流失败回调
            callWorkflowCallbacks { it.onWorkflowFail(workflowId, runId, result.error, result.executionTime) }

            // 发布工作流失败事件
            val failEvent = WorkflowFailedEvent(workflowId, runId, result.error, result.executionTime)
            publishEvent(failEvent)
        }

        return result
    }

    /**
     * 取消工作流。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param reason 取消原因
     * @return 是否取消成功
     */
    suspend fun cancelWorkflow(
        workflowId: String,
        runId: String,
        reason: String? = null
    ): Boolean {
        // 获取工作流状态
        val state = stateStorage.getWorkflowState(workflowId, runId)
            ?: return false

        // 更新工作流状态
        val updatedState = state.copy(
            status = WorkflowStateStatus.CANCELED,
            updatedAt = System.currentTimeMillis()
        )
        val result = stateStorage.saveWorkflowState(workflowId, runId, updatedState)

        if (result) {
            // 发送状态更新
            _stateUpdates.emit(updatedState)

            // 调用工作流取消回调
            callWorkflowCallbacks { it.onWorkflowCancel(workflowId, runId, reason) }

            // 发布工作流取消事件
            val cancelEvent = WorkflowCanceledEvent(workflowId, runId, reason)
            publishEvent(cancelEvent)
        }

        return result
    }

    /**
     * 获取工作流状态。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 工作流状态
     */
    suspend fun getWorkflowState(workflowId: String, runId: String): WorkflowState? {
        return stateStorage.getWorkflowState(workflowId, runId)
    }

    /**
     * 获取工作流的所有运行状态。
     *
     * @param workflowId 工作流ID
     * @param limit 限制返回的数量
     * @param offset 偏移量
     * @return 工作流运行状态列表
     */
    suspend fun getWorkflowRuns(workflowId: String, limit: Int = 10, offset: Int = 0) {
        stateStorage.getWorkflowRuns(workflowId, limit, offset)
    }

    /**
     * 发布自定义事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param eventName 事件名称
     * @param eventData 事件数据
     * @param stepId 步骤ID（可选）
     */
    suspend fun publishCustomEvent(
        workflowId: String,
        runId: String,
        eventName: String,
        eventData: Map<String, Any?>,
        stepId: String? = null
    ) {
        val event = CustomWorkflowEvent(workflowId, runId, stepId, eventName, eventData)
        publishEvent(event)
    }

    /**
     * 注册工作流回调。
     *
     * @param callback 工作流回调
     */
    fun registerWorkflowCallback(callback: WorkflowCallback) {
        callbackManager.registerWorkflowCallback(callback)
    }

    /**
     * 注销工作流回调。
     *
     * @param callback 工作流回调
     */
    fun unregisterWorkflowCallback(callback: WorkflowCallback) {
        callbackManager.unregisterWorkflowCallback(callback)
    }

    /**
     * 注册步骤回调。
     *
     * @param callback 步骤回调
     * @param stepId 特定步骤ID，如果为null则适用于所有步骤
     */
    fun registerStepCallback(callback: StepCallback, stepId: String? = null) {
        callbackManager.registerStepCallback(callback, stepId)
    }

    /**
     * 注销步骤回调。
     *
     * @param callback 步骤回调
     * @param stepId 特定步骤ID，如果为null则从所有步骤中注销
     */
    fun unregisterStepCallback(callback: StepCallback, stepId: String? = null) {
        callbackManager.unregisterStepCallback(callback, stepId)
    }

    /**
     * 获取事件总线。
     *
     * @return 事件总线
     */
    fun getEventBus(): WorkflowEventBus {
        return eventBus
    }

    /**
     * 获取事件存储。
     *
     * @return 事件存储
     */
    fun getEventStorage(): WorkflowEventStorage {
        return eventStorage
    }

    /**
     * 增强执行选项，添加事件和回调支持。
     *
     * @param options 原始执行选项
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 增强后的执行选项
     */
    private fun enhanceOptions(
        options: WorkflowExecuteOptions,
        workflowId: String,
        runId: String
    ): WorkflowExecuteOptions {
        // 创建步骤开始回调
        val onStepStart: (String, WorkflowContext) -> Unit = { stepId, context ->
            val stepCallbacks = callbackManager.getStepCallbacks(stepId)

            // 发布步骤开始事件
            val event = StepStartedEvent(workflowId, runId, stepId, context.input)
            runBlocking {
                eventBus.publish(event)
                eventStorage.saveEvent(event)
            }
        }

        // 创建步骤完成回调
        val originalOnStepFinish = options.onStepFinish
        val enhancedOnStepFinish: (WorkflowStepResult) -> Unit = { result ->
            // 调用原始回调
            originalOnStepFinish?.invoke(result)

            // 获取步骤回调
            val stepCallbacks = callbackManager.getStepCallbacks(result.stepId)

            if (result.success) {
                // 发布步骤完成事件
                val event = StepCompletedEvent(workflowId, runId, result.stepId, result)
                runBlocking {
                    eventBus.publish(event)
                    eventStorage.saveEvent(event)
                }
            } else {
                // 发布步骤失败事件
                val event = StepFailedEvent(workflowId, runId, result.stepId, result.error, null)
                runBlocking {
                    eventBus.publish(event)
                    eventStorage.saveEvent(event)
                }
            }
        }

        // 创建步骤错误回调
        val originalOnStepError = options.onStepError
        val enhancedOnStepError: (String, Throwable) -> Unit = { stepId, error ->
            // 调用原始回调
            originalOnStepError?.invoke(stepId, error)

            // 获取步骤回调
            val stepCallbacks = callbackManager.getStepCallbacks(stepId)

            // 发布步骤失败事件
            val event = StepFailedEvent(workflowId, runId, stepId, error.message, error)
            runBlocking {
                eventBus.publish(event)
                eventStorage.saveEvent(event)

                // 发布错误事件
                val errorEvent = ErrorOccurredEvent(workflowId, runId, stepId, error, emptyMap())
                eventBus.publish(errorEvent)
                eventStorage.saveEvent(errorEvent)
            }
        }

        // 创建步骤暂停回调
        val onStepSuspend: (String, Map<String, Any?>) -> Unit = { stepId, suspensionData ->
            // 获取步骤回调
            val stepCallbacks = callbackManager.getStepCallbacks(stepId)

            // 发布步骤暂停事件
            val event = StepSuspendedEvent(workflowId, runId, stepId, suspensionData)
            runBlocking {
                eventBus.publish(event)
                eventStorage.saveEvent(event)

                // 检查是否所有步骤都暂停了
                val state = stateStorage.getWorkflowState(workflowId, runId)
                if (state != null && state.status == WorkflowStateStatus.SUSPENDED) {
                    // 调用工作流暂停回调
                    callWorkflowCallbacks { it.onWorkflowSuspend(workflowId, runId, state.suspendedSteps) }

                    // 发布工作流暂停事件
                    val suspendEvent = WorkflowSuspendedEvent(workflowId, runId, state.suspendedSteps, state)
                    eventBus.publish(suspendEvent)
                    eventStorage.saveEvent(suspendEvent)
                }
            }
        }

        // 创建步骤跳过回调
        val onStepSkip: (String, String) -> Unit = { stepId, reason ->
            // 获取步骤回调
            val stepCallbacks = callbackManager.getStepCallbacks(stepId)

            // 发布步骤跳过事件
            val event = StepSkippedEvent(workflowId, runId, stepId, reason)
            runBlocking {
                eventBus.publish(event)
                eventStorage.saveEvent(event)
            }
        }

        // 创建重试开始回调
        val onRetryStart: (String, Int, Int, Long) -> Unit = { stepId, attempt, maxRetries, delay ->
            // 发布重试开始事件
            val event = RetryStartedEvent(workflowId, runId, stepId, attempt, maxRetries, delay)
            runBlocking {
                eventBus.publish(event)
                eventStorage.saveEvent(event)
            }
        }

        // 创建恢复策略应用回调
        val onRecoveryApplied: (String, String, Boolean) -> Unit = { stepId, strategyType, success ->
            // 发布恢复策略应用事件
            val event = RecoveryAppliedEvent(workflowId, runId, stepId, strategyType, success)
            runBlocking {
                eventBus.publish(event)
                eventStorage.saveEvent(event)
            }
        }

        // 返回增强后的选项
        return options.copy(
            onStepFinish = enhancedOnStepFinish,
            onStepError = enhancedOnStepError
        )
    }

    /**
     * 发布事件并保存。
     *
     * @param event 工作流事件
     */
    private suspend fun publishEvent(event: WorkflowEvent) {
        eventBus.publish(event)
        eventStorage.saveEvent(event)
    }

    /**
     * 调用所有工作流回调。
     *
     * @param action 回调操作
     */
    private suspend fun callWorkflowCallbacks(action: suspend (WorkflowCallback) -> Unit) {
        val callbacks = callbackManager.getWorkflowCallbacks()
        for (callback in callbacks) {
            try {
                action(callback)
            } catch (e: Exception) {
                logger.error(e) { "调用工作流回调失败: ${callback.javaClass.name}" }
            }
        }
    }

    /**
     * 调用所有步骤回调。
     *
     * @param stepId 步骤ID
     * @param action 回调操作
     */
    private suspend fun callStepCallbacks(stepId: String, action: suspend (StepCallback) -> Unit) {
        val callbacks = callbackManager.getStepCallbacks(stepId)
        for (callback in callbacks) {
            try {
                action(callback)
            } catch (e: Exception) {
                logger.error(e) { "调用步骤回调失败: ${callback.javaClass.name}" }
            }
        }
    }
}
