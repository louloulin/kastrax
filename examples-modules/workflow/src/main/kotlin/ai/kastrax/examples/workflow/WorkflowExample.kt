package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.workflow
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

/**
 * 工作流示例：内容创作流程
 *
 * 这个示例展示了如何使用工作流引擎创建一个内容创作流程，包括研究、写作和编辑三个步骤。
 */
fun main() = runBlocking {
    // 创建 DeepSeek 提供者
    val deepseek = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        // 显式设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
    }

    // 创建研究代理
    val researchAgent = agent {
        name = "Research Agent"
        instructions = """
            你是一个专业的研究助手。你的任务是对给定的主题进行深入研究，并提供详细的研究结果。
            请确保你的研究全面、准确，并包含最新的信息。

            输出格式：
            1. 主题概述
            2. 关键点（至少3个）
            3. 相关数据和统计
            4. 趋势和发展方向
        """.trimIndent()
        model = deepseek
    }

    // 创建写作代理
    val writingAgent = agent {
        name = "Writing Agent"
        instructions = """
            你是一个专业的内容写作助手。你的任务是根据提供的研究结果，撰写一篇高质量的文章。
            请确保文章结构清晰、内容丰富、语言流畅。

            输出格式：
            1. 标题
            2. 引言
            3. 正文（分段）
            4. 结论
        """.trimIndent()
        model = deepseek
    }

    // 创建编辑代理
    val editingAgent = agent {
        name = "Editing Agent"
        instructions = """
            你是一个专业的编辑助手。你的任务是对提供的文章进行编辑和优化。
            请检查并修正语法错误、拼写错误、表达不清的地方，并优化整体结构和流畅度。

            输出格式：
            - 编辑后的完整文章
        """.trimIndent()
        model = deepseek
    }

    // 创建内容创作工作流
    val contentCreationWorkflow = workflow {
        name = "content-creation"
        description = "创建高质量内容的工作流"
        // 注意：超时时间在执行时设置

        step(researchAgent) {
            id = "research"
            name = "研究"
            description = "研究指定主题"
            variables = mapOf(
                "topic" to variable("$.input.topic")
            )
        }

        step(writingAgent) {
            id = "writing"
            name = "写作"
            description = "根据研究结果撰写文章"
            after("research")
            variables = mapOf(
                "research" to variable("$.steps.research.output.text")
            )
        }

        step(editingAgent) {
            id = "editing"
            name = "编辑"
            description = "编辑和优化文章"
            after("writing")
            variables = mapOf(
                "draft" to variable("$.steps.writing.output.text")
            )
        }
    }

    println("=== 内容创作工作流示例 ===")
    println("正在执行工作流...")

    // 流式执行工作流
    val topic = "人工智能在医疗领域的应用"
    val input = mapOf("topic" to topic)

    contentCreationWorkflow.streamExecute(input).collect { update ->
        when (update.status) {
            ai.kastrax.core.workflow.WorkflowStatus.STARTED -> {
                println("工作流开始执行")
            }
            ai.kastrax.core.workflow.WorkflowStatus.IN_PROGRESS -> {
                println("正在执行: ${update.stepId} (${update.progress}%)")
                if (update.result != null) {
                    println("步骤完成: ${update.stepId}")
                }
            }
            ai.kastrax.core.workflow.WorkflowStatus.COMPLETED -> {
                println("工作流执行完成 (100%)")
            }
            ai.kastrax.core.workflow.WorkflowStatus.FAILED -> {
                println("工作流执行失败: ${update.message}")
            }
        }
    }

    // 执行工作流并获取结果
    val options = WorkflowExecuteOptions(
        timeout = 30.minutes.inWholeMilliseconds // 增加超时时间到 30 分钟
    )

    println("=== 内容创作工作流示例 ===")
    println("正在执行工作流...")

    // 使用流式执行显示进度
    var workflowResult: Map<String, Any?> = emptyMap()
    var workflowSuccess = false

    contentCreationWorkflow.streamExecute(input, options).collect { update ->
        when (update.status) {
            ai.kastrax.core.workflow.WorkflowStatus.STARTED -> {
                println("工作流开始执行")
            }
            ai.kastrax.core.workflow.WorkflowStatus.IN_PROGRESS -> {
                println("正在执行: ${update.stepId} (${update.progress}%)")
                if (update.result != null) {
                    println("步骤完成: ${update.stepId}")
                    // 收集步骤结果
                    val stepId = update.stepId ?: ""
                    val result = update.result
                    if (stepId.isNotEmpty() && result != null) {
                        val text = result.output["text"]
                        workflowResult = workflowResult + (stepId to text)
                    }
                }
            }
            ai.kastrax.core.workflow.WorkflowStatus.COMPLETED -> {
                println("工作流执行完成 (100%)")
                workflowSuccess = true
                // 工作流已经完成，使用已收集的结果
            }
            ai.kastrax.core.workflow.WorkflowStatus.FAILED -> {
                println("工作流执行失败: ${update.message}")
            }
        }
    }

    // 如果工作流成功完成，显示结果
    if (workflowSuccess) {
        println("\n=== 工作流执行结果 ===")
        println("研究结果:")
        println("----------")
        println(workflowResult["research"])

        println("\n文章草稿:")
        println("----------")
        println(workflowResult["writing"])

        println("\n最终文章:")
        println("----------")
        println(workflowResult["editing"])
    }
}
