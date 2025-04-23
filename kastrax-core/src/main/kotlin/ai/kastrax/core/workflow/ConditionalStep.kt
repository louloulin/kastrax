package ai.kastrax.core.workflow

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging

/**
 * 条件步骤，根据条件执行不同的分支。
 *
 * @property id 步骤ID
 * @property description 步骤描述
 * @property conditionFn 条件函数
 * @property thenStep 条件为真时执行的步骤
 * @property elseStep 条件为假时执行的步骤（可选）
 * @property after 前置步骤ID列表
 * @property variables 变量引用
 * @property config 步骤配置
 */
class ConditionalStep(
    override val id: String,
    override val description: String = "",
    val conditionFn: (WorkflowContext) -> Boolean,
    val thenStep: WorkflowStep,
    val elseStep: WorkflowStep? = null,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    override val config: StepConfig? = null
) : WorkflowStep {

    /**
     * 步骤名称。
     */
    override val name: String = "Conditional: $id"

    /**
     * 条件函数，决定是否执行步骤。
     */
    override val condition: (WorkflowContext) -> Boolean = { true }

    /**
     * 日志对象。
     */
    private val logger = KotlinLogging.logger("WORKFLOW_STEP:$id")

    /**
     * 执行条件步骤。
     *
     * 根据条件函数的结果，执行thenStep或elseStep。
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val startTime = System.currentTimeMillis()

        try {
            // 解析变量
            val resolvedVariables = context.resolveVariables(variables)

            // 创建包含解析变量的新上下文
            val enrichedContext = context.copy(
                variables = context.variables.toMutableMap().apply { putAll(resolvedVariables) }
            )

            // 评估条件
            val conditionResult = conditionFn(enrichedContext)
            // 评估条件结果
            println("条件评估结果: $conditionResult")

            // 根据条件执行相应步骤
            val stepResult = if (conditionResult) {
                // 执行条件为真的分支
                println("执行thenStep: ${thenStep.id}")
                thenStep.execute(enrichedContext)
            } else if (elseStep != null) {
                // 执行条件为假的分支
                println("执行elseStep: ${elseStep.id}")
                elseStep.execute(enrichedContext)
            } else {
                // 条件为假且没有elseStep
                println("条件为假且没有elseStep，返回空结果")
                // 如果条件为假且没有elseStep，返回空结果
                WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("conditionResult" to JsonPrimitive(conditionResult)),
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // 创建最终结果
            return WorkflowStepResult(
                stepId = id,
                success = stepResult.success,
                output = stepResult.output + mapOf("conditionResult" to JsonPrimitive(conditionResult)),
                error = stepResult.error,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            // 条件步骤执行失败
            println("条件步骤执行失败: ${e.message}")
            return WorkflowStepResult(
                stepId = id,
                success = false,
                output = emptyMap(),
                error = e.message ?: "Unknown error",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
}

/**
 * 工作流构建器的扩展函数，用于添加条件步骤。
 */
fun WorkflowBuilder.ifThen(
    conditionFn: (WorkflowContext) -> Boolean,
    thenStep: WorkflowStep,
    elseStep: WorkflowStep? = null,
    id: String = "if-${thenStep.id}-${elseStep?.id ?: "none"}"
) {
    val conditionalStep = ConditionalStep(
        id = id,
        description = "If-Then-Else condition",
        conditionFn = conditionFn,
        thenStep = thenStep,
        elseStep = elseStep
    )
    step(conditionalStep)
}
