# kastrax-actor

kastrax-actor 是一个将 kastrax AI Agent 与 kactor Actor 模型结合的模块，使 kastrax Agent 能够在分布式环境中高效运行。

## 已实现功能

### 基础集成

- [x] **KastraxActor 类**：实现了 kactor 的 Actor 接口，将 kastrax Agent 包装为 Actor
- [x] **基本消息传递机制**：实现了 Agent 之间的消息传递
- [x] **DSL 扩展**：支持创建 Actor 化的 Agent，直接复用现有的 agent DSL，包括 Actor 化 Agent DSL、Agent 网络 DSL 和消息传递 DSL
- [x] **基本示例和测试**：提供了基本使用示例和单元测试

### 高级功能

- [x] **Agent 网络功能**：支持创建和管理 Agent 网络
- [x] **流式处理支持**：支持流式生成和处理
- [x] **工具调用机制**：支持 Agent 调用工具
- [x] **协作模式**：支持 Agent 之间的协作

### 分布式功能

- [x] **远程 Actor 配置**：支持在远程服务器上部署和使用 Agent
- [x] **集群支持**：支持创建和管理 Agent 集群

## 使用示例

### DSL 功能

kastrax-actor 模块提供了三种 DSL，使得创建和管理 Actor 化的 Agent 变得更加简单和直观：

1. **Actor 化 Agent DSL**：用于创建 Actor 化的 Agent，直接复用现有的 kastrax agent DSL
2. **Agent 网络 DSL**：用于创建和管理 Agent 网络，支持添加 Agent 和设置协调者
3. **消息传递 DSL**：用于在 Actor 之间传递消息，支持发送消息、请求-响应模式和流式请求

有关 DSL 的详细信息，请参阅 [DSL 文档](docs/dsl-zh.md)。

## 文档导航

- [DSL 文档](docs/dsl-zh.md) - 详细介绍 kastrax-actor 模块的 DSL 功能
- [Agent 网络文档](docs/agent-network-zh.md) - 详细介绍 Agent 协作网络功能
- [远程 Actor 配置文档](docs/remote-zh.md) - 详细介绍远程 Actor 配置功能
- [故障排除指南](docs/troubleshooting-zh.md) - 提供常见问题的故障排除步骤
- [性能优化指南](docs/performance-optimization-zh.md) - 提供性能优化的最佳实践
- [实现总结](docs/implementation-summary-zh.md) - 总结 kastrax-actor 模块的实现情况

### 基本使用

```kotlin
// 创建 Actor 系统
val system = ActorSystem("kastrax-system")

// 创建 Actor 化的 Agent，直接复用现有的 agent DSL
val agentPid = system.actorAgent {
    // 这部分是现有的 kastrax agent DSL
    agent {
        name = "助手"
        instructions = "你是一个有帮助的助手。"
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY"))
        }
        tools {
            tool(calculatorTool)
        }
    }

    // 这部分是 actor 特有的配置
    actor {
        // actor 特有的配置，如监督策略、邮箱类型等
        oneForOneStrategy {
            maxRetries = 3
            withinTimeRange = 1.minutes
        }
        unboundedMailbox()
    }
}

// 发送消息
system.sendMessage(agentPid, "你能帮我计算 2 + 2 吗？")

// 请求-响应模式
val response = system.askMessage(agentPid, "巴黎的人口是多少？")
println("回答: $response")

// 流式请求
system.streamMessage(agentPid, "讲个故事") { chunk ->
    print(chunk)
}

// 关闭系统
system.shutdown()
```

### Agent 网络

```kotlin
// 创建 Actor 系统
val system = ActorSystem("kastrax-system")

// 创建 Agent 网络
val network = system.agentNetwork {
    // 创建协调者
    coordinator {
        agent {
            name = "协调者"
            instructions = "你是一个协调多个专家的协调者。"
            model = deepSeek { /* 配置 */ }
        }
        actor {
            oneForOneStrategy {
                maxRetries = 5
            }
        }
    }

    // 创建专家 Agent
    agent("researcher") {
        agent {
            name = "研究员"
            instructions = "你是一个专业的研究员。"
            model = deepSeek { /* 配置 */ }
        }
    }

    // ... 添加更多 Agent
}

// 发送消息给协调者
network.sendToCoordinator(AgentRequest("我需要一份关于气候变化的研究报告"))

// 发送消息给特定 Agent
network.send("researcher", AgentRequest("收集气候变化的最新数据"))

// 请求-响应模式
val response = network.ask("analyst", AgentRequest("分析这些气候数据的趋势"))
println("分析结果: ${(response as AgentResponse).text}")

// 广播消息
network.broadcast(AgentRequest("项目截止日期是下周五"))

// 关闭系统
system.shutdown()
```

## 最新更新

### 2023年5月更新

1. **改进的日志记录**：增强了KastraxActor中的日志记录机制，使调试更加容易
2. **修复堆栈溢出错误**：增强了递归检测机制，防止无限递归导致的堆栈溢出
3. **修复ProcessNameExistException错误**：改进了远程Actor配置测试，确保每次测试都使用唯一的系统名称和Actor名称
4. **优化远程Actor配置**：改进了远程Actor配置的稳定性和可靠性

## 下一步计划

1. **多模态数据传输**：实现多模态数据的传输机制
2. **集群支持增强**：进一步增强集群支持功能
3. **服务发现机制**：实现服务发现机制
4. **故障恢复功能**：添加故障恢复功能
5. **容错机制**：实现容错机制
6. **性能优化**：优化消息传递和处理性能

## 常见问题解决

如果您在使用 kastrax-actor 模块时遇到问题，请参考[故障排除指南](docs/troubleshooting-zh.md)了解详细的故障排除步骤和解决方案。

以下是一些常见问题的简要解决方法：

### 堆栈溢出错误

如果遇到堆栈溢出错误，可能是由于Agent之间的循环引用或消息传递导致的。解决方法：

1. 检查Agent之间的引用关系，避免循环引用
2. 增加MAX_RECURSION_DEPTH的值（默认为5）
3. 使用更详细的日志记录，帮助诊断问题

### ProcessNameExistException错误

如果遇到ProcessNameExistException错误，可能是由于尝试使用相同的名称注册多个Actor导致的。解决方法：

1. 确保每次注册Actor时使用唯一的名称
2. 在测试中使用随机生成的名称
3. 使用系统提供的自动命名功能

### 远程连接问题

如果遇到远程连接问题，可能是由于网络配置或端口冲突导致的。解决方法：

1. 确保服务器和客户端之间的网络连接畅通
2. 使用可用的随机端口，避免端口冲突
3. 检查防火墙设置，确保端口已开放

更多详细信息和高级故障排除技巧，请参考[故障排除指南](docs/troubleshooting-zh.md)。

## 技术架构

kastrax-actor 采用了以下技术架构：

1. **核心层**：Actor 模型基础设施
   - ActorSystem
   - Actor 生命周期管理
   - 消息传递机制
   - 监督策略

2. **Agent 层**：AI Agent 功能
   - LLM 集成
   - 工具使用
   - 上下文管理
   - 记忆系统

3. **分布式层**：分布式能力
   - 远程 Actor 通信
   - 集群管理
   - 负载均衡
   - 故障恢复

4. **应用层**：高级功能
   - Agent 网络
   - 工作流编排
   - 监控和可视化
   - 安全和访问控制
