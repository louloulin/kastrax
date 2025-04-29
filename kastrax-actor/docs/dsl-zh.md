# Kastrax Actor DSL

本文档详细介绍了 Kastrax Actor 模块提供的 DSL（领域特定语言）功能，包括 Actor 化 Agent DSL、Agent 网络 DSL 和消息传递 DSL。

## 目录

- [概述](#概述)
- [Actor 化 Agent DSL](#actor-化-agent-dsl)
- [Agent 网络 DSL](#agent-网络-dsl)
- [消息传递 DSL](#消息传递-dsl)
- [示例](#示例)
- [最佳实践](#最佳实践)

## 概述

Kastrax Actor 模块提供了一系列 DSL，使得创建和管理 Actor 化的 Agent 变得更加简单和直观。这些 DSL 包括：

1. **Actor 化 Agent DSL**：用于创建 Actor 化的 Agent，直接复用现有的 kastrax agent DSL
2. **Agent 网络 DSL**：用于创建和管理 Agent 网络，支持添加 Agent 和设置协调者
3. **消息传递 DSL**：用于在 Actor 之间传递消息，支持发送消息、请求-响应模式和流式请求

## Actor 化 Agent DSL

Actor 化 Agent DSL 允许你使用流畅的 DSL 语法创建 Actor 化的 Agent，直接复用现有的 kastrax agent DSL。

### 基本语法

```kotlin
val agentPid = system.actorAgent {
    // 配置 Agent 部分
    agent {
        // 使用现有的 kastrax agent DSL
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
    
    // 配置 Actor 部分
    actor {
        // actor 特有的配置
        oneForOneStrategy {
            maxRetries = 3
            withinTimeRange = Duration.ofMinutes(1)
        }
        unboundedMailbox()
    }
}
```

### 配置选项

#### Agent 配置

Agent 配置部分直接复用现有的 kastrax agent DSL，支持以下配置：

- `name`：Agent 名称
- `instructions`：Agent 指令
- `model`：LLM 模型配置
- `tools`：工具配置
- 其他 kastrax agent DSL 支持的配置

#### Actor 配置

Actor 配置部分支持以下配置：

- **监督策略**：
  ```kotlin
  oneForOneStrategy {
      maxRetries = 3
      withinTimeRange = Duration.ofMinutes(1)
  }
  ```

- **邮箱类型**：
  ```kotlin
  // 无界邮箱
  unboundedMailbox()
  
  // 有界邮箱
  boundedMailbox(capacity = 100)
  ```

## Agent 网络 DSL

Agent 网络 DSL 允许你使用流畅的 DSL 语法创建和管理 Agent 网络，支持添加 Agent 和设置协调者。

### 基本语法

```kotlin
val network = system.agentNetwork {
    // 设置协调者
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
    
    // 添加专家 Agent
    agent("researcher") {
        agent {
            name = "研究员"
            instructions = "你是一个专业的研究员。"
            model = deepSeek { /* 配置 */ }
        }
    }
    
    // ... 添加更多 Agent
}
```

### 配置选项

#### 协调者配置

协调者配置支持与 Actor 化 Agent DSL 相同的配置选项，包括 Agent 配置和 Actor 配置。

#### Agent 配置

Agent 配置支持与 Actor 化 Agent DSL 相同的配置选项，包括 Agent 配置和 Actor 配置。

### 网络操作

Agent 网络 DSL 创建的网络对象支持以下操作：

- **发送消息给协调者**：
  ```kotlin
  network.sendToCoordinator(message)
  ```

- **发送消息给特定 Agent**：
  ```kotlin
  network.send(agentName, message)
  ```

- **请求-响应模式**：
  ```kotlin
  val response = network.ask(agentName, message)
  ```

- **广播消息**：
  ```kotlin
  network.broadcast(message)
  ```

## 消息传递 DSL

消息传递 DSL 提供了一系列扩展函数，使得在 Actor 之间传递消息变得更加简单和直观。

### 基本语法

```kotlin
// 发送消息
system.sendMessage(agentPid, "你好，我是用户")

// 请求-响应模式
val response = system.askMessage(agentPid, "巴黎的人口是多少？")

// 流式请求
system.streamMessage(agentPid, "讲个故事") { chunk ->
    print(chunk)
}
```

### 扩展函数

#### sendMessage

```kotlin
fun ActorSystem.sendMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions())
```

发送消息给指定的 Agent，不等待响应。

#### askMessage

```kotlin
suspend fun ActorSystem.askMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()): String
```

向指定的 Agent 发送请求并等待响应，返回 Agent 生成的文本。

#### streamMessage

```kotlin
fun ActorSystem.streamMessage(target: PID, prompt: String, options: AgentStreamOptions = AgentStreamOptions(), onChunk: (String) -> Unit)
```

向指定的 Agent 发送流式请求，通过回调函数处理生成的文本块。

## 示例

### Actor 化 Agent DSL 示例

```kotlin
// 创建 Actor 系统
val system = ActorSystem("kastrax-system")

// 创建 Actor 化的 Agent
val agentPid = system.actorAgent {
    // 配置 Agent 部分
    agent {
        agentBuilder.name = "助手"
        agentBuilder.instructions = "你是一个有帮助的助手。"
    }

    // 配置 Actor 部分
    actor {
        oneForOneStrategy {
            maxRetries = 3
            withinTimeRange = Duration.ofMinutes(1)
        }
        unboundedMailbox()
    }
}
```

### Agent 网络 DSL 示例

```kotlin
// 创建 Actor 系统
val system = ActorSystem("kastrax-system")

// 创建 Agent 网络
val network = system.agentNetwork {
    // 设置协调者
    coordinator {
        agent {
            agentBuilder.name = "协调者"
            agentBuilder.instructions = "你是一个协调多个专家的协调者。"
        }
        actor {
            oneForOneStrategy {
                maxRetries = 5
            }
        }
    }

    // 添加专家 Agent
    agent("researcher") {
        agent {
            agentBuilder.name = "研究员"
            agentBuilder.instructions = "你是一个专业的研究员。"
        }
    }

    agent("writer") {
        agent {
            agentBuilder.name = "作家"
            agentBuilder.instructions = "你是一个专业的内容创作者。"
        }
    }
}

// 发送消息给协调者
network.sendToCoordinator(AgentRequest("我需要一份关于气候变化的研究报告"))

// 发送消息给特定 Agent
network.send("researcher", AgentRequest("收集气候变化的最新数据"))

// 请求-响应模式
val response = network.ask("writer", AgentRequest("写一篇关于气候变化的文章"))
println("作家的回应: ${(response as AgentResponse).text}")

// 广播消息
network.broadcast(AgentRequest("项目截止日期是下周五"))
```

### 消息传递 DSL 示例

```kotlin
// 创建 Actor 系统
val system = ActorSystem("kastrax-system")

// 创建 Agent
val mockAgent = MockAgent()
val props = fromProducer { KastraxActor(mockAgent) }
val agentPid = system.root.spawn(props)

// 发送消息
system.sendMessage(agentPid, "你好，我是用户")

// 请求-响应模式
val response = system.askMessage(agentPid, "巴黎的人口是多少？")
println("回答: $response")

// 流式请求
system.streamMessage(agentPid, "讲个故事") { chunk ->
    print(chunk)
}
```

## 最佳实践

### Actor 化 Agent DSL

1. **复用现有的 kastrax agent DSL**：尽可能复用现有的 kastrax agent DSL，避免重复配置
2. **合理设置监督策略**：根据 Agent 的特性和任务需求，设置合适的监督策略
3. **选择合适的邮箱类型**：根据消息处理需求，选择合适的邮箱类型

### Agent 网络 DSL

1. **合理设计网络结构**：根据任务需求，设计合理的网络结构，包括协调者和专家 Agent
2. **明确 Agent 职责**：为每个 Agent 分配明确的职责，避免职责重叠
3. **优化消息传递**：尽量减少不必要的消息传递，优化网络性能

### 消息传递 DSL

1. **选择合适的消息传递方式**：根据需求选择合适的消息传递方式，如发送消息、请求-响应模式或流式请求
2. **处理超时和错误**：在使用请求-响应模式时，合理设置超时时间并处理可能的错误
3. **优化流式处理**：在使用流式请求时，优化文本块的处理逻辑，提高响应速度
