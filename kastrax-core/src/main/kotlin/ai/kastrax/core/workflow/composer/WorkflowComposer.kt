package ai.kastrax.core.workflow.composer

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.workflow.ConditionalStep
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.SubWorkflowStep
import ai.kastrax.core.workflow.SubWorkflowStepBuilder
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowBuilder
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.engine.WorkflowEngine
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * 工作流组合器，支持将多个工作流组合成一个新的工作流。
 *
 * 工作流组合器提供了多种组合模式：
 * 1. 顺序组合：将多个工作流按顺序执行
 * 2. 并行组合：将多个工作流并行执行
 * 3. 条件组合：根据条件执行不同的工作流
 * 4. 循环组合：重复执行工作流直到满足条件
 * 5. 嵌套组合：将工作流嵌套在另一个工作流中
 * 6. DSL组合：使用DSL语法灵活组合工作流
 */
class WorkflowComposer(
    composerName: String,
    private val workflowEngine: WorkflowEngine
) : KastraXBase(component = "WORKFLOW_COMPOSER", name = composerName) {

    // Use the logger from KastraXBase

    /**
     * 顺序组合多个工作流。
     *
     * @param workflowName 新工作流名称
     * @param description 新工作流描述
     * @param workflows 要组合的工作流列表
     * @param inputMapping 输入映射函数，将组合工作流输入映射到第一个子工作流
     * @param outputMapping 输出映射函数，将最后一个子工作流输出映射到组合工作流输出
     * @return 组合后的工作流
     */
    fun sequentialCompose(
        workflowName: String,
        description: String,
        workflows: List<Pair<String, String>>, // Pair of (workflowId, stepId)
        inputMapping: (Map<String, Any?>) -> Map<String, Any?> = { it },
        outputMapping: (Map<String, Any?>) -> Map<String, Any?> = { it }
    ): Workflow {
        logger.info { "顺序组合工作流: $workflowName, 子工作流数量: ${workflows.size}" }

        require(workflows.isNotEmpty()) { "至少需要一个工作流进行组合" }

        val steps = mutableListOf<WorkflowStep>()
        var previousStepId: String? = null

        // 为每个工作流创建子工作流步骤
        for ((index, workflow) in workflows.withIndex()) {
            val (workflowId, stepId) = workflow
            val isFirst = index == 0
            val isLast = index == workflows.size - 1

            // 创建子工作流步骤
            val step = SubWorkflowStep(
                id = stepId,
                name = "子工作流: $workflowId",
                description = "执行子工作流: $workflowId",
                after = previousStepId?.let { listOf(it) } ?: emptyList(),
                workflowId = workflowId,
                inputMapping = { context ->
                    when {
                        isFirst -> inputMapping(context.input)
                        else -> {
                            // 从前一个步骤获取输出
                            val prevStepOutput = previousStepId?.let { context.steps[it]?.output } ?: emptyMap()
                            prevStepOutput
                        }
                    }
                },
                outputMapping = { result ->
                    when {
                        isLast -> outputMapping(result.output)
                        else -> result.output
                    }
                },
                workflowEngine = workflowEngine
            )

            steps.add(step)
            previousStepId = stepId
        }

        // 创建组合工作流
        return SimpleWorkflow(
            workflowName = workflowName,
            description = description,
            steps = steps.associateBy { it.id }
        )
    }

    /**
     * 并行组合多个工作流。
     *
     * @param workflowName 新工作流名称
     * @param description 新工作流描述
     * @param workflows 要组合的工作流映射 (stepId -> workflowId)
     * @param inputMapping 输入映射函数，将组合工作流输入映射到各个子工作流
     * @param outputMapping 输出映射函数，将所有子工作流输出合并为组合工作流输出
     * @param mergeStep 可选的合并步骤，用于处理所有子工作流的输出
     * @return 组合后的工作流
     */
    fun parallelCompose(
        workflowName: String,
        description: String,
        workflows: Map<String, String>, // Map of stepId -> workflowId
        inputMapping: (String, Map<String, Any?>) -> Map<String, Any?> = { _, input -> input },
        outputMapping: (Map<String, Map<String, Any?>>) -> Map<String, Any?> = { outputs ->
            outputs.flatMap { (stepId, output) ->
                output.map { (key, value) -> "${stepId}_$key" to value }
            }.toMap()
        },
        mergeStep: WorkflowStep? = null
    ): Workflow {
        logger.info { "并行组合工作流: $workflowName, 子工作流数量: ${workflows.size}" }

        require(workflows.isNotEmpty()) { "至少需要一个工作流进行组合" }

        val steps = mutableListOf<WorkflowStep>()

        // 为每个工作流创建子工作流步骤
        for ((stepId, workflowId) in workflows) {
            // 创建子工作流步骤
            val step = SubWorkflowStep(
                id = stepId,
                name = "子工作流: $workflowId",
                description = "执行子工作流: $workflowId",
                workflowId = workflowId,
                inputMapping = { context ->
                    inputMapping(stepId, context.input)
                },
                workflowEngine = workflowEngine
            )

            steps.add(step)
        }

        // 如果提供了合并步骤，添加它
        if (mergeStep != null) {
            // 设置合并步骤的前置步骤为所有子工作流步骤
            val mergeStepWithDependencies = object : WorkflowStep by mergeStep {
                override val after: List<String> = workflows.keys.toList()
            }

            steps.add(mergeStepWithDependencies)
        } else {
            // 创建默认的合并步骤
            val defaultMergeStep = object : WorkflowStep {
                override val id: String = "merge"
                override val name: String = "合并步骤"
                override val description: String = "合并所有子工作流的输出"
                override val after: List<String> = workflows.keys.toList()
                override val variables: Map<String, VariableReference> = emptyMap()
                override val config: StepConfig? = null

                override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                    // 收集所有子工作流的输出
                    val outputs = workflows.keys.associateWith { stepId ->
                        context.steps[stepId]?.output ?: emptyMap()
                    }

                    // 合并输出
                    val mergedOutput = outputMapping(outputs)

                    return WorkflowStepResult.success(
                        stepId = id,
                        output = mergedOutput
                    )
                }
            }

            steps.add(defaultMergeStep)
        }

        // 创建组合工作流
        return SimpleWorkflow(
            workflowName = workflowName,
            description = description,
            steps = steps.associateBy { it.id }
        )
    }

    /**
     * 使用DSL组合工作流。
     *
     * @param workflowName 新工作流名称
     * @param description 新工作流描述
     * @param init 工作流构建器初始化函数
     * @return 组合后的工作流
     */
    fun compose(
        workflowName: String,
        description: String,
        init: CompositionBuilder.() -> Unit
    ): Workflow {
        logger.info { "使用DSL组合工作流: $workflowName" }

        val builder = CompositionBuilder(workflowEngine)
        builder.init()

        val workflowBuilder = WorkflowBuilder()
        workflowBuilder.name = workflowName
        workflowBuilder.description = description

        // 添加所有子工作流步骤
        for (step in builder.steps) {
            // 使用反射访问私有字段
            val stepsField = workflowBuilder::class.java.getDeclaredField("steps")
            stepsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val steps = stepsField.get(workflowBuilder) as MutableMap<String, WorkflowStep>
            steps[step.id] = step
        }

        return workflowBuilder.build()
    }
}

/**
 * 组合构建器。
 */
class CompositionBuilder(private val workflowEngine: WorkflowEngine) {
    val steps = mutableListOf<WorkflowStep>()

    /**
     * 添加子工作流步骤。
     */
    fun subWorkflow(init: SubWorkflowStepBuilder.() -> Unit) {
        val builder = SubWorkflowStepBuilder(workflowEngine)
        builder.init()
        val step = builder.build()
        steps.add(step)
    }

    /**
     * 添加自定义步骤。
     */
    fun step(step: WorkflowStep) {
        steps.add(step)
    }
}
