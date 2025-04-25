package ai.kastrax.app.agents

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
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
    model = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        // 显式设置 API 密钥，并确保其格式正确
        apiKey(System.getenv("DEEPSEEK_API_KEY")?.trim() ?: "sk-85e83081df28490b9ae63188f0cb4f79".trim())
        // 增加超时时间，防止复杂请求超时
        timeout(120) // 120秒
        // 设置温度和最大令牌数
        temperature(0.7)
        maxTokens(1000)
    }

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
