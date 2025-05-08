package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.builder.step
import ai.kastrax.core.workflow.template.WorkflowTemplate
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 工作流模板示例
 */
class WorkflowTemplateExample {
    fun main() = runBlocking {
    println("工作流模板示例")
    println("=============")

    // 创建代理
    val researchAgent = agent {
        name = "研究助手"
        instructions = "你是一个研究助手，负责收集和整理信息。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    val writingAgent = agent {
        name = "写作助手"
        instructions = "你是一个写作助手，负责根据提供的信息撰写内容。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    val editingAgent = agent {
        name = "编辑助手"
        instructions = "你是一个编辑助手，负责审核和改进内容。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.5)
            maxTokens(2000)
        }
    }

    // 创建工作流模板
    val contentCreationTemplate = ai.kastrax.core.workflow.template.workflowTemplate {
        name = "内容创建模板"
        description = "用于创建各种主题的内容的模板"

        // 定义参数
        parameter(
            name = "topic",
            description = "要创建内容的主题",
            type = String::class.java,
            required = true
        )

        parameter(
            name = "style",
            description = "内容的风格，如'学术'、'通俗'等",
            type = String::class.java,
            required = false,
            defaultValue = "通俗"
        )

        parameter(
            name = "length",
            description = "内容的大致长度，如'短'、'中'、'长'",
            type = String::class.java,
            required = false,
            defaultValue = "中"
        )

        // 定义工作流构建器
        workflowBuilderDsl { params ->
            {
                name = "内容创建: ${params["topic"]}"
                description = "为主题'${params["topic"]}'创建${params["style"]}风格的${params["length"]}篇内容"

                // 研究步骤
                step(researchAgent) {
                    id = "research"
                    name = "研究"
                    description = "研究主题"
                    variables = mutableMapOf(
                        "topic" to VariableReference("$.input.topic"),
                        "style" to VariableReference("$.input.style"),
                        "length" to VariableReference("$.input.length")
                    )
                }

                // 写作步骤
                step(writingAgent) {
                    id = "writing"
                    name = "写作"
                    description = "创建内容"
                    after("research")
                    variables = mutableMapOf(
                        "topic" to VariableReference("$.input.topic"),
                        "style" to VariableReference("$.input.style"),
                        "length" to VariableReference("$.input.length"),
                        "research" to VariableReference("$.steps.research.output.text")
                    )
                }

                // 编辑步骤
                step(editingAgent) {
                    id = "editing"
                    name = "编辑"
                    description = "审核和改进内容"
                    after("writing")
                    variables = mutableMapOf(
                        "topic" to VariableReference("$.input.topic"),
                        "style" to VariableReference("$.input.style"),
                        "length" to VariableReference("$.input.length"),
                        "draft" to VariableReference("$.steps.writing.output.text")
                    )
                }
            }
        }
    }

    // 使用模板创建工作流实例
    println("\n使用模板创建工作流实例...")

    // 创建学术风格的工作流
    val academicWorkflow = contentCreationTemplate.createWorkflow(
        mapOf(
            "topic" to "量子计算",
            "style" to "学术",
            "length" to "长"
        )
    )

    // 创建通俗风格的工作流
    val popularWorkflow = contentCreationTemplate.createWorkflow(
        mapOf(
            "topic" to "人工智能伦理",
            "style" to "通俗",
            "length" to "中"
        )
    )

    // 执行学术风格工作流
    println("\n执行学术风格工作流...")
    val academicResult = academicWorkflow.execute(
        input = mapOf(
            "topic" to "量子计算",
            "style" to "学术",
            "length" to "长"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n学术风格工作流执行结果:")
    academicResult.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出摘要: ${stepResult.output["text"]?.toString()?.take(100)}...")
    }

    // 执行通俗风格工作流
    println("\n执行通俗风格工作流...")
    val popularResult = popularWorkflow.execute(
        input = mapOf(
            "topic" to "人工智能伦理",
            "style" to "通俗",
            "length" to "中"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n通俗风格工作流执行结果:")
    popularResult.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出摘要: ${stepResult.output["text"]?.toString()?.take(100)}...")
    }

    println("\n工作流模板示例完成")
    }
}
