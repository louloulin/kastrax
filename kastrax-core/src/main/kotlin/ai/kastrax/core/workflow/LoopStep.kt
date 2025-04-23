package ai.kastrax.core.workflow

import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import java.util.UUID

/**
 * 循环步骤，重复执行一个步骤直到条件不满足。
 *
 * @property id 步骤ID
 * @property description 步骤描述
 * @property loopCondition 循环条件函数，接收上下文和当前迭代次数，返回是否继续循环
 * @property body 循环体步骤
 * @property maxIterations 最大迭代次数，防止无限循环
 * @property after 前置步骤ID列表
 * @property variables 变量引用
 */
class LoopStep(
    override val id: String,
    override val description: String = "",
    val loopCondition: (WorkflowContext, Int) -> Boolean,
    val body: WorkflowStep,
    val maxIterations: Int = 10,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    override val config: StepConfig? = null
) : WorkflowStep {

    /**
     * 步骤名称。
     */
    override val name: String = "Loop: $id"

    /**
     * 条件函数，决定是否执行步骤。
     */
    override val condition: (WorkflowContext) -> Boolean = { true }

    /**
     * 日志对象。
     */
    private val logger = KotlinLogging.logger("WORKFLOW_STEP:$id")

    /**
     * 执行循环步骤。
     *
     * 重复执行循环体步骤，直到条件不满足或达到最大迭代次数。
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val startTime = System.currentTimeMillis()

        try {
            // 解析变量
            val resolvedVariables = context.resolveVariables(variables)

            // 创建包含解析变量的新上下文
            var currentContext = context.copy(
                variables = context.variables.toMutableMap().apply { putAll(resolvedVariables) }
            )

            // 执行循环
            var iteration = 0
            val results = mutableListOf<WorkflowStepResult>()
            var success = true

            while (loopCondition(currentContext, iteration) && iteration < maxIterations) {
                logger.debug { "执行循环迭代 $iteration" }

                // 添加当前迭代次数到上下文
                currentContext = currentContext.copy(
                    variables = currentContext.variables.toMutableMap().apply { put("iteration", iteration) }
                )

                // 执行循环体
                val stepResult = body.execute(currentContext)
                results.add(stepResult)

                // 如果循环体执行失败，终止循环
                if (!stepResult.success) {
                    logger.debug { "循环体执行失败，终止循环" }
                    success = false
                    break
                }

                // 更新上下文，包含循环体的输出
                currentContext = currentContext.copy(
                    steps = currentContext.steps.toMutableMap().apply { put(
                        "${body.id}-$iteration", stepResult
                    ) }
                )

                iteration++
            }

            // 创建子步骤映射
            val childSteps = results.mapIndexed { index, result ->
                "${body.id}-$index" to result
            }.toMap()

            // 创建最终结果
            return WorkflowStepResult(
                stepId = id,
                success = success,
                output = mapOf(
                    "iterations" to JsonPrimitive(iteration),
                    "results" to results.map { it.output }
                ),
                error = if (!success) {
                    results.find { !it.success }?.error ?: "Loop execution failed"
                } else null,
                executionTime = System.currentTimeMillis() - startTime,

            )
        } catch (e: Exception) {
            logger.error(e) { "循环步骤执行失败" }
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
 * 工作流构建器的扩展函数，用于添加循环步骤。
 */
fun WorkflowBuilder.loop(
    condition: (WorkflowContext, Int) -> Boolean,
    body: WorkflowStep,
    maxIterations: Int = 10,
    id: String = "loop-${UUID.randomUUID()}"
) {
    val loopStep = LoopStep(
        id = id,
        description = "Loop execution of ${body.id}",
        loopCondition = condition,
        body = body,
        maxIterations = maxIterations
    )
    step(loopStep)
}
