package ai.kastrax.examples.mcp

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.config.mcpConfig
import ai.kastrax.mcp.MCPManager
import kotlinx.coroutines.runBlocking

/**
 * 多服务器配置示例
 */
fun main() = runBlocking {
    println("KastraX MCP 多服务器配置示例")
    println("========================")

    // 创建 MCP 配置
    val mcpConfig = mcpConfig {
        // 添加股票价格服务器
        stdioServer("stockPrice") {
            command("node")
            args("examples/stock-price.js")
            env("API_KEY", "your-api-key")
        }
        
        // 添加天气服务器
        sseServer("weather") {
            url("http://localhost:8080/mcp/sse")
            header("Authorization", "Bearer your-token")
        }
    }
    
    // 创建 MCP 管理器
    val mcpManager = MCPManager(mcpConfig)
    
    try {
        // 获取所有工具集
        val toolsets = mcpManager.getToolsets()
        
        // 创建一个使用多个 MCP 工具的代理
        val agent = agent {
            name = "Multi-tool Assistant"
            instructions = "你是一个助手，可以提供股票价格和天气信息。"
            
            // 使用 OpenAI 模型
            // 注意：这里的模型配置仅作示例，实际使用时需要替换为正确的模型
            // model = openAi("gpt-4")
        }
        
        // 使用代理，传入工具集
        val response = agent.generate(
            "纽约的天气怎么样？苹果公司的股票价格是多少？",
            toolsets = toolsets
        )
        println("Agent response: ${response.text}")
    } finally {
        // 断开所有连接
        mcpManager.disconnectAll()
    }
    
    println("\n示例完成!")
}
