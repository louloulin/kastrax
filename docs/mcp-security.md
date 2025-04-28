# KastraX MCP 安全性和访问控制

本文档介绍了 KastraX MCP 安全性和访问控制功能的使用方法。

## 概述

KastraX MCP 安全性和访问控制功能允许用户保护 MCP 服务器和客户端，控制对资源和工具的访问，并管理用户身份验证和授权。它提供了以下功能：

- 客户端和服务器身份验证
- 资源和工具访问控制
- 令牌管理
- 权限管理

## 核心组件

### MCPSecurity

`MCPSecurity` 是安全性和访问控制的核心接口，它提供了以下功能：

- 验证客户端和服务器身份
- 控制对资源和工具的访问
- 生成和验证访问令牌
- 管理安全配置

### MCPAuthenticator

`MCPAuthenticator` 是身份验证器接口，用于验证客户端和服务器的身份。它提供了以下功能：

- 验证客户端和服务器身份
- 生成客户端密钥和服务器令牌
- 注册和吊销客户端和服务器

### MCPAuthorizer

`MCPAuthorizer` 是授权器接口，用于控制对资源和工具的访问。它提供了以下功能：

- 控制对资源和工具的访问
- 授予和撤销权限
- 检查权限

## 使用方法

### 创建安全配置

```kotlin
// 创建安全配置
val security = mcpSecurity {
    config {
        enable(true)
        enableAuthentication(true)
        enableAuthorization(true)
        enableTokenValidation(true)
        secretKey("your-secret-key")
        allowClientIds(listOf("client1", "client2"))
        allowServerIds(listOf("server1", "server2"))
        defaultScope(listOf("resources:read", "tools:use", "prompts:read"))
    }
}
```

### 注册客户端和服务器

```kotlin
// 注册客户端
val clientId = "client1"
val clientSecret = security.authenticator.generateClientSecret(clientId)
security.authenticator.registerClient(clientId, clientSecret)

// 注册服务器
val serverId = "server1"
val serverToken = security.authenticator.generateServerToken(serverId)
security.authenticator.registerServer(serverId, serverToken)
```

### 授予权限

```kotlin
// 授予资源访问权限
security.authorizer.grantResourceAccess(clientId, "resource1")

// 授予工具使用权限
security.authorizer.grantToolAccess(clientId, "tool1")

// 授予提示使用权限
security.authorizer.grantPromptAccess(clientId, "prompt1")

// 授予权限
security.authorizer.grantPermission(clientId, "resources:read:all")
security.authorizer.grantPermission(clientId, "tools:use:all")
security.authorizer.grantPermission(clientId, "prompts:read:all")
```

### 创建安全 MCP 服务器

```kotlin
// 创建安全 MCP 服务器
val server = mcpServer {
    name("SecureMCPServer")
    version("1.0.0")
    serverId(serverId)
    
    // 设置安全配置
    security(security.getSecurityConfig())
    
    // 添加工具
    tool {
        name = "secure_tool"
        description = "安全工具"
        
        // 设置执行函数
        handler { params ->
            "安全工具执行结果"
        }
    }
}
```

### 创建安全 MCP 客户端

```kotlin
// 创建安全 MCP 客户端
val client = mcpClient {
    name("SecureMCPClient")
    version("1.0.0")
    clientId(clientId)
    clientSecret(clientSecret)
    
    server {
        sse {
            url = "http://localhost:8080"
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
```

### 连接到服务器并进行身份验证

```kotlin
// 连接到服务器并进行身份验证
client.connect(clientSecret)

// 获取访问令牌
val token = client.getAccessToken()

// 使用访问令牌
client.setAccessToken(token)

// 调用工具
val result = client.callTool("secure_tool", emptyMap())

// 刷新访问令牌
val newToken = client.refreshAccessToken()

// 吊销访问令牌
client.revokeAccessToken()
```

## 安全最佳实践

1. **使用强密钥和令牌**：使用足够长且复杂的密钥和令牌，避免使用简单或可预测的值。

2. **限制访问范围**：只授予客户端所需的最小权限，避免过度授权。

3. **定期轮换密钥和令牌**：定期更换密钥和令牌，减少凭据泄露的风险。

4. **启用所有安全功能**：启用身份验证、授权和令牌验证等所有安全功能，提供多层防护。

5. **监控和审计**：记录所有身份验证和授权事件，定期审查日志以检测异常活动。

6. **安全传输**：使用 HTTPS 或其他加密通道传输敏感信息，防止中间人攻击。

7. **验证输入**：验证所有客户端输入，防止注入攻击和其他安全漏洞。

## 完整示例

请参考 `examples/src/mcp/SecureMCPExample.kt` 文件，了解如何使用 MCP 安全性和访问控制功能的完整示例。
