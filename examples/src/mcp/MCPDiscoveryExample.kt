package ai.kastrax.examples.mcp

import ai.kastrax.mcp.discovery.mcpDiscoveryService
import ai.kastrax.mcp.protocol.ServerCapabilities
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URL

/**
 * MCP 服务发现示例
 * 
 * 这个示例展示了如何使用 MCP 服务发现机制来发现和连接到 MCP 服务器。
 */
fun main() = runBlocking {
    println("启动 MCP 服务发现示例...")
    
    // 创建 MCP 发现服务
    val discoveryService = mcpDiscoveryService {
        // 添加本地注册表
        registry {
            name("Local Registry")
            description("本地 MCP 服务器注册表")
        }
        
        // 添加远程注册表
        registry {
            name("Example Registry")
            description("示例 MCP 服务器注册表")
            homepage("https://example.com/mcp-registry")
            url("https://example.com/mcp-registry")
        }
    }
    
    // 监听服务器变化
    val job = discoveryService.observeServerChanges()
        .onEach { event ->
            println("服务器变化事件: $event")
        }
        .launchIn(this)
    
    // 获取本地注册表
    val localRegistry = discoveryService.getRegistry("Local Registry")
    if (localRegistry != null) {
        println("\n本地注册表: ${localRegistry.name}")
        println("描述: ${localRegistry.description}")
        
        // 注册一些示例服务器
        println("\n注册示例服务器...")
        
        // 注册天气服务器
        localRegistry.registerServer(
            ai.kastrax.mcp.discovery.MCPRegistryEntry(
                id = "weather-server",
                name = "天气服务器",
                description = "提供天气查询功能的 MCP 服务器",
                version = "1.0.0",
                capabilities = ServerCapabilities(
                    resources = false,
                    tools = true,
                    prompts = false
                ),
                schemas = listOf(
                    ai.kastrax.mcp.discovery.ServerSchema(
                        command = "npx",
                        args = listOf("tsx", "weather-server.ts"),
                        env = mapOf(
                            "WEATHER_API_KEY" to ai.kastrax.mcp.discovery.EnvVarSchema(
                                description = "天气 API 密钥",
                                required = true
                            )
                        )
                    )
                )
            )
        )
        
        // 注册股票服务器
        localRegistry.registerServer(
            ai.kastrax.mcp.discovery.MCPRegistryEntry(
                id = "stock-server",
                name = "股票服务器",
                description = "提供股票行情查询功能的 MCP 服务器",
                version = "1.0.0",
                capabilities = ServerCapabilities(
                    resources = false,
                    tools = true,
                    prompts = false
                ),
                schemas = listOf(
                    ai.kastrax.mcp.discovery.ServerSchema(
                        command = "npx",
                        args = listOf("tsx", "stock-server.ts"),
                        env = mapOf(
                            "STOCK_API_KEY" to ai.kastrax.mcp.discovery.EnvVarSchema(
                                description = "股票 API 密钥",
                                required = true
                            )
                        )
                    )
                )
            )
        )
        
        // 注册翻译服务器
        localRegistry.registerServer(
            ai.kastrax.mcp.discovery.MCPRegistryEntry(
                id = "translation-server",
                name = "翻译服务器",
                description = "提供多语言翻译功能的 MCP 服务器",
                version = "1.0.0",
                capabilities = ServerCapabilities(
                    resources = false,
                    tools = true,
                    prompts = false
                ),
                schemas = listOf(
                    ai.kastrax.mcp.discovery.ServerSchema(
                        command = "npx",
                        args = listOf("tsx", "translation-server.ts"),
                        env = mapOf(
                            "TRANSLATION_API_KEY" to ai.kastrax.mcp.discovery.EnvVarSchema(
                                description = "翻译 API 密钥",
                                required = true
                            )
                        )
                    )
                )
            )
        )
    }
    
    // 尝试从远程注册表加载服务器
    println("\n尝试从远程注册表加载服务器...")
    try {
        // 注意：这里使用的是示例 URL，实际使用时应该替换为真实的注册表 URL
        val count = discoveryService.loadFromRemoteRegistry(URL("https://example.com/mcp-registry"))
        println("从远程注册表加载了 $count 个服务器")
    } catch (e: Exception) {
        println("从远程注册表加载服务器失败: ${e.message}")
    }
    
    // 发现所有服务器
    println("\n发现所有服务器:")
    val allServers = discoveryService.discoverServers()
    allServers.forEach { server ->
        println("- ${server.name} (${server.id}): ${server.description}")
    }
    
    // 根据查询发现服务器
    println("\n搜索包含 '天气' 的服务器:")
    val weatherServers = discoveryService.discoverServers("天气")
    weatherServers.forEach { server ->
        println("- ${server.name} (${server.id}): ${server.description}")
    }
    
    // 根据能力发现服务器
    println("\n发现支持工具的服务器:")
    val toolServers = discoveryService.discoverServersByCapabilities(listOf("tools"))
    toolServers.forEach { server ->
        println("- ${server.name} (${server.id}): ${server.description}")
    }
    
    // 连接到服务器（模拟）
    println("\n模拟连接到天气服务器...")
    try {
        val weatherServer = discoveryService.discoverServers("天气").firstOrNull()
        if (weatherServer != null) {
            println("找到天气服务器: ${weatherServer.name} (${weatherServer.id})")
            
            // 在实际应用中，这里会连接到服务器并调用其工具
            // val client = discoveryService.connectToServer(weatherServer)
            // val result = client.callTool("getWeather", mapOf("location" to "北京"))
            // println("北京天气: $result")
            
            println("模拟调用 getWeather 工具...")
            println("北京天气: 晴朗，温度 25°C，湿度 45%，风速 10 km/h")
        } else {
            println("未找到天气服务器")
        }
    } catch (e: Exception) {
        println("连接到天气服务器失败: ${e.message}")
    }
    
    // 等待一段时间，以便观察服务器变化事件
    println("\n等待服务器变化事件...")
    delay(1000)
    
    // 取消监听
    job.cancel()
    
    println("\nMCP 服务发现示例结束")
}
