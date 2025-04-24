package ai.kastrax.app.agents

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.agent
import ai.kastrax.app.tools.calculatorTool
import ai.kastrax.app.tools.weatherTool
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 助手代理。
 * 这是一个通用的助手代理，可以回答问题并使用工具。
 */
val assistantAgent = agent("assistant") {
    description = "一个通用的助手代理，可以回答问题并使用工具"
    
    // 添加工具
    tools(
        calculatorTool,
        weatherTool
    )
    
    // 设置系统提示
    systemPrompt("""
        你是一个有用的助手，可以回答用户的问题并使用工具来获取信息。
        
        当用户询问数学计算时，使用计算器工具。
        当用户询问天气时，使用天气工具。
        
        始终以友好、专业的方式回答，并提供准确的信息。
    """.trimIndent())
    
    // 设置示例对话
    exampleConversation(
        """
        用户: 你好，你能帮我计算一下 235 * 48 吗？
        助手: 我可以帮你计算。让我使用计算器工具。
        
        235 * 48 = 11,280
        
        用户: 谢谢！北京今天的天气怎么样？
        助手: 让我为你查询北京的天气。
        
        根据我的查询，北京今天的天气是晴朗，气温为 25°C，湿度为 45%。是个不错的天气！
        """.trimIndent()
    )
    
    // 设置模型
    model = "gpt-4"
    
    // 设置参数
    temperature = 0.7
    maxTokens = 1000
}
