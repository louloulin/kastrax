# 远程 Actor 配置

本文档介绍了 kastrax-actor 模块中的远程 Actor 配置功能，该功能允许在分布式环境中部署和使用 kastrax Agent。

## 基本概念

远程 Actor 配置允许您：

1. 在远程服务器上启动 Actor 系统
2. 在远程系统中注册 Agent
3. 从客户端连接到远程系统
4. 向远程 Agent 发送消息并接收响应

## 配置远程 Actor 系统

### 基本配置

使用 `configureRemoteActorSystem` 函数配置远程 Actor 系统：

```kotlin
// 配置远程 Actor 系统
val system = configureRemoteActorSystem(8090)

// 或者指定主机名
val system = configureRemoteActorSystem(8090, "0.0.0.0")
```

### 使用配置对象

如果需要更详细的配置，可以使用 `RemoteActorConfig` 类：

```kotlin
// 创建配置对象
val config = RemoteActorConfig(
    hostname = "0.0.0.0",
    port = 8090,
    advertisedHostname = "localhost",
    advertisedPort = 8090
)

// 使用配置对象创建系统
val system = configureRemoteActorSystemWithConfig("kastrax-remote", config)
```

## 注册 Agent

在远程系统中注册 Agent：

```kotlin
// 创建 Agent
val agent = YourAgent()

// 注册 Agent
val pid = system.registerRemoteAgent(agent, "your-agent-name")
```

## 连接到远程系统

从客户端连接到远程系统：

```kotlin
// 连接到远程系统
val remoteAgent = connectToRemoteSystem("localhost", 8090)
```

## 与远程 Agent 通信

### 发送消息

```kotlin
// 发送消息
remoteAgent.send("your-agent-name", AgentRequest("你好，远程助手！"))
```

### 请求-响应模式

```kotlin
// 发送请求并等待响应
val response = remoteAgent.ask("your-agent-name", AgentRequest("你好，远程助手！"))
println("远程助手回答: ${(response as AgentResponse).text}")
```

## 完整示例

### 服务器端

```kotlin
// 配置远程 Actor 系统
val system = configureRemoteActorSystem(8090)

// 注册 Agent
val agent = YourAgent()
system.registerRemoteAgent(agent, "remote-assistant")

// 保持系统运行
runBlocking {
    while (true) {
        delay(1000)
    }
}
```

### 客户端

```kotlin
// 连接到远程系统
val remoteAgent = connectToRemoteSystem("localhost", 8090)

// 发送消息并接收响应
val response = remoteAgent.ask("remote-assistant", AgentRequest("你好，远程助手！"))
println("远程助手回答: ${(response as AgentResponse).text}")
```

## 注意事项

1. 确保服务器和客户端之间的网络连接畅通
2. 如果使用防火墙，需要开放相应的端口
3. 在生产环境中，建议配置适当的安全措施，如 TLS 加密和身份验证
