package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.builder.step
import ai.kastrax.core.workflow.dynamic.DynamicWorkflowGenerator
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 动态工作流示例
 */
class DynamicWorkflowExample {
    fun main() = runBlocking {
    println("动态工作流示例")
    println("=============")

    // 创建代理
    val plannerAgent = agent {
        name = "规划助手"
        instructions = "你是一个规划助手，负责制定计划和步骤。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.3)
            maxTokens(2000)
        }
    }

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

    // 创建动态工作流生成器
    val workflowGenerator = DynamicWorkflowGenerator()

    // 方法1：使用DSL动态创建工作流
    val dynamicWorkflow1 = workflowGenerator.createWorkflow(
        workflowName = "动态内容创建工作流",
        description = "根据主题动态创建内容"
    ) {
        // 规划步骤
        step(plannerAgent) {
            id = "planning"
            name = "规划"
            description = "规划内容创建步骤"
            variables = mutableMapOf(
                "topic" to VariableReference("$.input.topic")
            )
        }

        // 研究步骤
        step(researchAgent) {
            id = "research"
            name = "研究"
            description = "研究主题"
            after("planning")
            variables = mutableMapOf(
                "topic" to VariableReference("$.input.topic"),
                "plan" to VariableReference("$.steps.planning.output.text")
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
                "plan" to VariableReference("$.steps.planning.output.text"),
                "research" to VariableReference("$.steps.research.output.text")
            )
        }
    }

    // 方法2：使用步骤列表创建工作流
    val steps = listOf(
        object : WorkflowStep {
            override val id = "planning2"
            override val name = "规划2"
            override val description = "规划内容创建步骤"
            override val after = emptyList<String>()
            override val variables = mapOf(
                "topic" to VariableReference("$.input.topic")
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val topic = context.resolveReference(variables["topic"]!!) as? String ?: ""
                val result = plannerAgent.generate("请为主题'$topic'制定一个内容创建计划，包括需要研究的要点和写作大纲。")
                return WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("text" to result.text)
                )
            }
        },

        object : WorkflowStep {
            override val id = "research2"
            override val name = "研究2"
            override val description = "研究主题"
            override val after = listOf("planning2")
            override val variables = mapOf(
                "topic" to VariableReference("$.input.topic"),
                "plan" to VariableReference("$.steps.planning2.output.text")
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val topic = context.resolveReference(variables["topic"]!!) as? String ?: ""
                val plan = context.resolveReference(variables["plan"]!!) as? String ?: ""
                val result = researchAgent.generate("请根据以下计划，对主题'$topic'进行研究：\n\n$plan")
                return WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("text" to result.text)
                )
            }
        },

        object : WorkflowStep {
            override val id = "writing2"
            override val name = "写作2"
            override val description = "创建内容"
            override val after = listOf("research2")
            override val variables = mapOf(
                "topic" to VariableReference("$.input.topic"),
                "plan" to VariableReference("$.steps.planning2.output.text"),
                "research" to VariableReference("$.steps.research2.output.text")
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val topic = context.resolveReference(variables["topic"]!!) as? String ?: ""
                val plan = context.resolveReference(variables["plan"]!!) as? String ?: ""
                val research = context.resolveReference(variables["research"]!!) as? String ?: ""
                val result = writingAgent.generate("""
                    请根据以下信息，为主题'$topic'创建内容：

                    计划：
                    $plan

                    研究：
                    $research
                """.trimIndent())
                return WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("text" to result.text)
                )
            }
        }
    )

    val dynamicWorkflow2 = workflowGenerator.createWorkflow(
        workflowName = "动态内容创建工作流2",
        description = "使用步骤列表创建的动态工作流",
        steps = steps
    )

    // 执行第一个动态工作流
    println("\n执行第一个动态工作流...")
    val result1 = dynamicWorkflow1.execute(
        input = mapOf(
            "topic" to "人工智能在教育领域的应用"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n第一个动态工作流执行结果:")
    result1.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出摘要: ${stepResult.output["text"]?.toString()?.take(100)}...")
    }

    // 执行第二个动态工作流
    println("\n执行第二个动态工作流...")
    val result2 = dynamicWorkflow2.execute(
        input = mapOf(
            "topic" to "可持续发展与环保技术"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n第二个动态工作流执行结果:")
    result2.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出摘要: ${stepResult.output["text"]?.toString()?.take(100)}...")
    }

    println("\n动态工作流示例完成")
    }
}
