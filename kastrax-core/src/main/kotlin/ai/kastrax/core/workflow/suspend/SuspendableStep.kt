package ai.kastrax.core.workflow.suspend

import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult

/**
 * 可暂停的工作流步骤接口。
 */
interface SuspendableStep : WorkflowStep {
    /**
     * 执行步骤，支持暂停。
     *
     * @param context 工作流上下文
     * @param suspendController 暂停控制器
     * @return 步骤执行结果
     */
    suspend fun execute(context: WorkflowContext, suspendController: SuspendController): WorkflowStepResult

    /**
     * 执行步骤。
     *
     * 此方法会创建一个暂停控制器，并调用带有暂停控制器的execute方法。
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val suspendController = SuspendControllerImpl()
        return execute(context, suspendController)
    }
}

/**
 * 暂停控制器接口，用于控制工作流步骤的暂停。
 */
interface SuspendController {
    /**
     * 暂停工作流步骤。
     *
     * @param payload 暂停时的附加数据
     */
    suspend fun suspend(payload: Any? = null): Nothing
}

/**
 * 暂停控制器实现。
 */
class SuspendControllerImpl : SuspendController {
    override suspend fun suspend(payload: Any?): Nothing {
        throw WorkflowSuspendedException(payload)
    }
}

/**
 * 工作流暂停异常，用于实现暂停功能。
 */
class WorkflowSuspendedException(val payload: Any? = null) : Exception("Workflow execution suspended")

/**
 * 可暂停的工作流步骤抽象类。
 */
abstract class AbstractSuspendableStep(
    override val id: String,
    override val name: String = id,
    override val description: String = "",
    override val after: List<String> = emptyList(),
    override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap(),
    override val config: ai.kastrax.core.workflow.StepConfig? = null
) : SuspendableStep {

    /**
     * 执行步骤，支持暂停。
     *
     * @param context 工作流上下文
     * @param suspendController 暂停控制器
     * @return 步骤执行结果
     */
    abstract override suspend fun execute(context: WorkflowContext, suspendController: SuspendController): WorkflowStepResult
}
