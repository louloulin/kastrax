package ai.kastrax.examples.mcp

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.security.mcpSecurity
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 安全 MCP 应用案例
 * 
 * 这个示例展示了如何创建一个具有安全性和访问控制功能的 MCP 服务器，
 * 以及如何使用 MCP 客户端连接到该服务器并进行身份验证。
 */
fun main() = runBlocking {
    println("启动安全 MCP 应用案例...")
    
    // 创建安全配置
    val security = mcpSecurity {
        config {
            enable(true)
            enableAuthentication(true)
            enableAuthorization(true)
            enableTokenValidation(true)
            secretKey("secure-mcp-example-key")
            allowClientIds(listOf("secure-client"))
            allowServerIds(listOf("secure-server"))
            defaultScope(listOf("resources:read", "tools:use", "prompts:read"))
        }
    }
    
    // 注册客户端和服务器
    val clientId = "secure-client"
    val clientSecret = security.authenticator.generateClientSecret(clientId)
    security.authenticator.registerClient(clientId, clientSecret)
    
    val serverId = "secure-server"
    val serverToken = security.authenticator.generateServerToken(serverId)
    security.authenticator.registerServer(serverId, serverToken)
    
    // 授予权限
    security.authorizer.grantPermission(clientId, "resources:read:all")
    security.authorizer.grantPermission(clientId, "tools:use:all")
    security.authorizer.grantPermission(clientId, "prompts:read:all")
    
    // 启动服务器
    val serverJob = launch {
        val server = createSecureServer(security, serverId)
        server.startSSE(port = 8086)
        println("安全 MCP 服务器已启动在端口 8086")
        
        // 保持服务器运行
        try {
            while (true) {
                delay(1000)
            }
        } finally {
            server.stop()
            println("安全 MCP 服务器已停止")
        }
    }
    
    // 等待服务器启动
    delay(2000)
    
    // 创建并使用客户端
    val client = createSecureClient(clientId, clientSecret)
    
    try {
        // 连接到服务器
        println("连接到安全 MCP 服务器...")
        client.connect(clientSecret)
        println("已连接到安全 MCP 服务器")
        
        // 获取访问令牌
        println("获取访问令牌...")
        val token = client.getAccessToken()
        println("已获取访问令牌: $token")
        
        // 获取服务器能力
        val hasTools = client.supportsCapability("tools")
        println("服务器支持工具: $hasTools")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 调用工具
        println("\n调用 echo 工具...")
        val echoResult = client.callTool("echo", mapOf("message" to "Hello, Secure MCP!"))
        println("Echo 结果: $echoResult")
        
        // 调用需要授权的工具
        println("\n调用 secure_data 工具...")
        val secureResult = client.callTool("secure_data", mapOf("key" to "user_info"))
        println("Secure Data 结果: $secureResult")
        
        // 刷新访问令牌
        println("\n刷新访问令牌...")
        val newToken = client.refreshAccessToken()
        println("已刷新访问令牌: $newToken")
        
        // 吊销访问令牌
        println("\n吊销访问令牌...")
        val revoked = client.revokeAccessToken()
        println("令牌吊销结果: $revoked")
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与安全 MCP 服务器的连接")
        
        // 停止服务器
        serverJob.cancel()
    }
}

/**
 * 创建安全 MCP 服务器
 */
private fun createSecureServer(security: ai.kastrax.mcp.security.MCPSecurity, serverId: String) = mcpServer {
    name("SecureMCPServer")
    version("1.0.0")
    serverId(serverId)
    
    // 设置安全配置
    security(security.getSecurityConfig())
    
    // 添加 echo 工具
    tool {
        name = "echo"
        description = "回显消息"
        
        // 添加参数
        parameters {
            parameter {
                name = "message"
                description = "要回显的消息"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val message = params["message"] as? String ?: ""
            println("执行 echo 工具，消息: $message")
            "Echo: $message"
        }
    }
    
    // 添加需要授权的工具
    tool {
        name = "secure_data"
        description = "获取安全数据"
        
        // 添加参数
        parameters {
            parameter {
                name = "key"
                description = "数据键"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val key = params["key"] as? String ?: ""
            println("执行 secure_data 工具，键: $key")
            
            // 模拟安全数据
            val secureData = mapOf(
                "user_info" to "用户信息: 姓名=张三, 年龄=30, 职业=工程师",
                "financial_data" to "财务数据: 收入=10000, 支出=7000, 储蓄=3000",
                "health_data" to "健康数据: 身高=175cm, 体重=70kg, 血型=A型"
            )
            
            secureData[key] ?: "未找到数据: $key"
        }
    }
}

/**
 * 创建安全 MCP 客户端
 */
private fun createSecureClient(clientId: String, clientSecret: String) = mcpClient {
    name("SecureMCPClient")
    version("1.0.0")
    clientId(clientId)
    clientSecret(clientSecret)
    
    server {
        sse {
            url = "http://localhost:8086"
        }
    }
    
    // 设置安全配置
    security {
        enable(true)
        enableAuthentication(true)
        enableAuthorization(true)
        enableTokenValidation(true)
    }
}
