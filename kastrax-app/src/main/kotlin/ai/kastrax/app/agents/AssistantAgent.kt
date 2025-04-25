package ai.kastrax.app.agents

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.app.constants.AgentIds
import ai.kastrax.app.tools.calculatorTool
import ai.kastrax.app.tools.weatherTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement

private val logger = KotlinLogging.logger {}

/**
 * 助手代理。
 * 这是一个通用的助手代理，可以回答问题并使用工具。
 */
val assistantAgent = agent {
    // 设置名称
    name = "助手代理"

    // 设置指令（系统提示）
    instructions = """
        你是一个有用的助手，可以回答用户的问题并使用工具来获取信息。

        当用户询问数学计算时，使用计算器工具。
        当用户询问天气时，使用天气工具。

        始终以友好、专业的方式回答，并提供准确的信息。
    """.trimIndent()

    // 设置模型
    model = openAi("gpt-4")

    // 添加工具
    tools {
        tool(calculatorTool)
        tool(weatherTool)
    }

    // 配置默认生成选项
    defaultGenerateOptions {
        temperature(0.7)
        maxTokens(1000)
    }

    // 注意：示例对话需要通过其他方式设置，当前Agent接口不直接支持examples DSL
    // 可以通过LlmMessage列表在generate方法中提供示例
}
