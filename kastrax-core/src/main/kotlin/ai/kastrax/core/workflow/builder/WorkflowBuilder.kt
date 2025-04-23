package ai.kastrax.core.workflow.builder

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowBuilder
import ai.kastrax.core.workflow.WorkflowStep

/**
 * DSL函数，用于创建工作流。
 *
 * @param name 工作流名称
 * @param description 工作流描述
 * @param init 工作流构建器初始化函数
 * @return 创建的工作流
 */
fun workflow(name: String, description: String, init: WorkflowBuilder.() -> Unit): Workflow {
    val builder = WorkflowBuilder()
    builder.name = name
    builder.description = description
    builder.init()
    return builder.build()
}

/**
 * 添加步骤到工作流。
 *
 * @param step 工作流步骤
 */
fun WorkflowBuilder.step(step: WorkflowStep) {
    val steps = this::class.java.getDeclaredField("steps").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val stepsMap = steps.get(this) as MutableMap<String, WorkflowStep>
    stepsMap[step.id] = step
}
