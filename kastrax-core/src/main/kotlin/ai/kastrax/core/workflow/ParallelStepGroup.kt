package ai.kastrax.core.workflow

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import java.util.UUID

/**
 * 并行步骤组，同时执行多个步骤。
 *
 * @property id 步骤ID
 * @property description 步骤描述
 * @property steps 要并行执行的步骤列表
 * @property after 前置步骤ID列表
 * @property variables 变量引用
 */
class ParallelStepGroup(
    override val id: String,
    override val description: String = "",
    val steps: List<WorkflowStep>,
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    override val config: StepConfig? = null
) : WorkflowStep {

    /**
     * 步骤名称。
     */
    override val name: String = "Parallel: $id"

    /**
     * 条件函数，决定是否执行步骤。
     */
    override val condition: (WorkflowContext) -> Boolean = { true }

    /**
     * 日志对象。
     */
    private val logger = KotlinLogging.logger("WORKFLOW_STEP:$id")

    /**
     * 执行并行步骤组。
     *
     * 同时执行所有步骤，并收集它们的结果。
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult = coroutineScope {
        val startTime = System.currentTimeMillis()

        try {
            // 解析变量
            val resolvedVariables = context.resolveVariables(variables)

            // 创建包含解析变量的新上下文
            val enrichedContext = context.copy(
                variables = context.variables.toMutableMap().apply { putAll(resolvedVariables) }
            )

            // 并行执行所有步骤
            // 开始并行执行步骤
            println("开始并行执行 ${steps.size} 个步骤")

            val stepResults = steps.map { step ->
                async {
                    try {
                        // 执行并行步骤
                        println("执行并行步骤: ${step.id}")
                        step.execute(enrichedContext)
                    } catch (e: Exception) {
                        // 并行步骤执行失败
                        println("并行步骤 ${step.id} 执行失败: ${e.message}")
                        WorkflowStepResult(
                            stepId = step.id,
                            success = false,
                            output = emptyMap(),
                            error = e.message ?: "Unknown error",
                            executionTime = 0
                        )
                    }
                }
            }.awaitAll()

            // 检查是否所有步骤都成功执行
            val allSuccessful = stepResults.all { it.success }

            // 合并所有步骤的输出
            val combinedOutput = stepResults.fold(emptyMap<String, Any?>()) { acc, result ->
                acc + result.output
            }

            // 创建子步骤映射
            val childSteps = stepResults.associateBy { it.stepId }

            // 创建最终结果
            return@coroutineScope WorkflowStepResult(
                stepId = id,
                success = allSuccessful,
                output = combinedOutput,
                error = if (!allSuccessful) {
                    stepResults.filter { !it.success }
                        .joinToString(", ") { "${it.stepId}: ${it.error}" }
                } else null,
                executionTime = System.currentTimeMillis() - startTime,

            )
        } catch (e: Exception) {
            // 并行步骤组执行失败
            println("并行步骤组执行失败: ${e.message}")
            return@coroutineScope WorkflowStepResult(
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
 * 工作流构建器的扩展函数，用于添加并行步骤组。
 */
fun WorkflowBuilder.parallel(
    vararg steps: WorkflowStep,
    id: String = "parallel-${UUID.randomUUID()}"
) {
    val parallelStepGroup = ParallelStepGroup(
        id = id,
        description = "Parallel execution of ${steps.size} steps",
        steps = steps.toList()
    )
    step(parallelStepGroup)
}
