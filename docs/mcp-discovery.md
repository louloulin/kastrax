# KastraX MCP 服务发现

本文档介绍了 KastraX MCP 服务发现机制的使用方法。

## 概述

KastraX MCP 服务发现机制允许用户发现和连接到 MCP 服务器，无论它们是本地的还是远程的。它提供了以下功能：

- 注册和管理 MCP 服务器
- 搜索和发现 MCP 服务器
- 连接到 MCP 服务器
- 监听服务器变化

## 核心组件

### MCPRegistry

`MCPRegistry` 是一个注册表，用于注册和发现 MCP 服务器。它提供了以下功能：

- 注册服务器
- 注销服务器
- 获取服务器列表
- 搜索服务器

### MCPDiscoveryService

`MCPDiscoveryService` 是一个发现服务，用于管理多个注册表和发现服务器。它提供了以下功能：

- 添加和移除注册表
- 发现服务器
- 连接到服务器
- 监听服务器变化

### RemoteRegistryClient

`RemoteRegistryClient` 是一个远程注册表客户端，用于从远程注册表加载服务器。它提供了以下功能：

- 连接到远程注册表
- 获取服务器列表
- 搜索服务器

## 使用方法

### 创建 MCP 发现服务

```kotlin
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
```

### 注册服务器

```kotlin
// 获取本地注册表
val localRegistry = discoveryService.getRegistry("Local Registry")
if (localRegistry != null) {
    // 注册服务器
    localRegistry.registerServer(
        MCPRegistryEntry(
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
                ServerSchema(
                    command = "npx",
                    args = listOf("tsx", "weather-server.ts"),
                    env = mapOf(
                        "WEATHER_API_KEY" to EnvVarSchema(
                            description = "天气 API 密钥",
                            required = true
                        )
                    )
                )
            )
        )
    )
}
```

### 从远程注册表加载服务器

```kotlin
// 从远程注册表加载服务器
val count = discoveryService.loadFromRemoteRegistry(URL("https://example.com/mcp-registry"))
println("从远程注册表加载了 $count 个服务器")
```

### 发现服务器

```kotlin
// 发现所有服务器
val allServers = discoveryService.discoverServers()
allServers.forEach { server ->
    println("- ${server.name} (${server.id}): ${server.description}")
}

// 根据查询发现服务器
val weatherServers = discoveryService.discoverServers("天气")
weatherServers.forEach { server ->
    println("- ${server.name} (${server.id}): ${server.description}")
}

// 根据能力发现服务器
val toolServers = discoveryService.discoverServersByCapabilities(listOf("tools"))
toolServers.forEach { server ->
    println("- ${server.name} (${server.id}): ${server.description}")
}
```

### 连接到服务器

```kotlin
// 连接到服务器
val weatherServer = discoveryService.discoverServers("天气").firstOrNull()
if (weatherServer != null) {
    val client = discoveryService.connectToServer(weatherServer)
    
    // 调用工具
    val result = client.callTool("getWeather", mapOf("location" to "北京"))
    println("北京天气: $result")
}
```

### 监听服务器变化

```kotlin
// 监听服务器变化
val job = discoveryService.observeServerChanges()
    .onEach { event ->
        when (event) {
            is ServerChangeEvent.ServerAdded -> {
                println("服务器已添加: ${event.entry.name} (${event.entry.id})")
            }
            is ServerChangeEvent.ServerRemoved -> {
                println("服务器已移除: ${event.id}")
            }
            is ServerChangeEvent.ServerUpdated -> {
                println("服务器已更新: ${event.entry.name} (${event.entry.id})")
            }
        }
    }
    .launchIn(scope)
```

## 完整示例

请参考 `examples/src/mcp/MCPDiscoveryExample.kt` 文件，了解如何使用 MCP 服务发现机制的完整示例。
