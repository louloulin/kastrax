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

## 高级配置

### 负载均衡

在分布式环境中，可以配置负载均衡来分发请求：

```kotlin
// 创建负载均衡器
val loadBalancer = RemoteLoadBalancer()

// 添加远程系统
loadBalancer.addRemoteSystem("localhost", 8090)
loadBalancer.addRemoteSystem("localhost", 8091)
loadBalancer.addRemoteSystem("localhost", 8092)

// 使用负载均衡器发送请求
val response = loadBalancer.ask("remote-assistant", AgentRequest("你好，远程助手！"))
```

### 失败重试

配置失败重试机制，提高系统的可靠性：

```kotlin
// 创建重试配置
val retryConfig = RetryConfig(
    maxRetries = 3,
    retryDelay = 1000, // 毫秒
    exponentialBackoff = true
)

// 使用重试机制发送请求
val response = remoteAgent.askWithRetry("remote-assistant", AgentRequest("你好，远程助手！"), retryConfig)
```

### 超时配置

配置请求超时，防止请求长时间无响应：

```kotlin
// 设置超时时间
val timeout = 30.seconds

// 使用超时发送请求
val response = remoteAgent.askWithTimeout("remote-assistant", AgentRequest("你好，远程助手！"), timeout)
```

## 最佳实践

### 系统设计

1. **合理划分服务**：根据业务需求合理划分服务，避免单个服务负载过重
2. **使用合适的网络拓扑**：根据系统规模和复杂性选择合适的网络拓扑
3. **考虑容错设计**：在设计中考虑容错机制，如失败重试、超时处理等

### 性能优化

1. **使用连接池**：对于频繁的远程调用，使用连接池提高性能
2. **批量处理**：尽可能将多个请求批量处理，减少网络开销
3. **异步处理**：使用异步处理提高并发能力

### 安全性

1. **使用TLS加密**：在生产环境中使用TLS加密保护数据传输
2. **实现身份验证**：添加身份验证机制，防止未授权访问
3. **限制访问权限**：根据角色和权限限制访问

## 故障排除

### 连接问题

如果无法连接到远程系统，请检查：

1. 确保服务器正在运行并监听指定的端口
2. 检查网络连接是否畅通
3. 确保防火墙已开放相应的端口
4. 检查主机名和端口是否正确

### 消息传递问题

如果消息传递失败，请检查：

1. 确保远程 Agent 已注册并正在运行
2. 检查消息格式是否正确
3. 检查是否超过了消息大小限制

### 性能问题

如果遇到性能问题，请尝试：

1. 使用负载均衡分发请求
2. 优化消息大小，减少网络传输
3. 增加系统资源（CPU、内存等）

## 注意事项

1. 确保服务器和客户端之间的网络连接畅通
2. 如果使用防火墙，需要开放相应的端口
3. 在生产环境中，建议配置适当的安全措施，如 TLS 加密和身份验证
4. 定期监控系统状态，及时发现和解决问题
5. 在大规模部署中，考虑使用服务发现机制，简化配置和管理
