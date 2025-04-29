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

## 下一步计划

1. **多模态数据传输**：实现多模态数据的传输机制
2. **远程 Actor 配置**：实现远程 Actor 配置
3. **集群支持**：开发集群支持
4. **服务发现机制**：实现服务发现机制
5. **故障恢复功能**：添加故障恢复功能
6. **容错机制**：实现容错机制

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
