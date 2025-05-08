package ai.kastrax.examples.workflow.simple

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.AgentStep
import ai.kastrax.core.workflow.OutputMapping
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 简单工作流示例
 * 这个示例展示了如何创建和执行一个简单的工作流，包含两个步骤：
 * 1. 初始步骤：生成一个简单的消息
 * 2. 代理步骤：使用DeepSeek代理处理消息
 */
fun main() = runBlocking {
    println("开始执行SimpleWorkflowExample...")

    // 创建DeepSeek代理
    val deepseekAgent = agent {
        name = "DeepSeek助手"
        instructions = """
            你是一个有帮助的助手，可以回答用户的问题。
            请用简洁明了的语言回答，并提供有用的信息。
        """.trimIndent()

        model = deepSeek {
            model("deepseek-chat")
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
    }

    // 创建初始步骤
    val initStep = object : WorkflowStep {
        override val id: String = "init"
        override val name: String = "初始步骤"
        override val description: String = "生成初始消息"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行初始步骤...")

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf("message" to "请介绍一下Kotlin语言的主要特点。"),
                executionTime = 100
            )
        }
    }

    // 创建代理步骤
    val agentStep = AgentStep(
        id = "agent",
        name = "代理步骤",
        description = "使用DeepSeek代理处理消息",
        agent = deepseekAgent,
        after = listOf("init"),
        variables = mapOf(
            "prompt" to VariableReference("$.init.message")
        ),
        outputMapping = { text -> mapOf("response" to text) }
    )

    // 创建工作流
    val workflow = SimpleWorkflow(
        workflowName = "简单工作流",
        description = "一个包含初始步骤和代理步骤的简单工作流",
        steps = mapOf(
            "init" to initStep,
            "agent" to agentStep
        ),
        outputMappings = mapOf(
            "initialMessage" to OutputMapping("$.steps.init.output.message"),
            "agentResponse" to OutputMapping("$.steps.agent.output.response")
        )
    )

    // 执行工作流
    println("开始执行工作流...")
    val result = workflow.execute(emptyMap())

    // 输出结果
    println("\n工作流执行结果:")
    println("成功: ${result.success}")
    println("执行时间: ${result.executionTime}ms")
    println("\n初始消息:")
    println(result.output["initialMessage"])
    println("\n代理响应:")
    println(result.output["agentResponse"])
}
