# 基于 kactor 构建分布式 kastrax AI Agent 框架计划

## 0. 项目背景与目标

### 0.1 背景

kastrax 是一个功能强大的 AI Agent 框架，提供了丰富的 Agent 架构（分层、自适应、目标导向、反思型）和工具集成能力。kactor 是一个基于 Kotlin 实现的 Actor 模型框架，提供了高并发和分布式系统的基础设施。将这两者结合，可以创建一个具有强大分布式能力的 AI Agent 框架，使 kastrax 能够在分布式环境中高效运行。

同时，参考 AgentScope 的设计，我们可以借鉴其基于 Actor 的分布式框架，实现本地和分布式部署的无缝转换，以及自动并行优化。

### 0.2 目标

- 直接复用 kactor 的 Actor 模型和 kastrax 的 AI Agent 能力
- 实现 kastrax Agent 作为 kactor Actor 运行的能力
- 支持 Agent 之间的消息传递和协作
## 4. 核心组件详细设计

### 4.1 KastraxActor

```kotlin
class KastraxActor(private val agent: Agent) : Actor {
    override fun receive(context: Context) {
        when (val msg = context.message) {
            is AgentRequest -> {
                // 使用 kastrax Agent 处理请求
                val response = runBlocking {
                    agent.generate(msg.prompt, msg.options)
                }
                // 发送响应
                context.respond(AgentResponse(response.text, response.toolCalls))
            }
            is AgentStreamRequest -> {
                // 处理流式请求
                val job = context.actorSystem.dispatcher.dispatch {
                    agent.stream(msg.prompt, msg.options) { chunk ->
                        context.send(msg.sender, AgentStreamChunk(chunk))
                    }
                    // 流结束后发送完成消息
                    context.send(msg.sender, AgentStreamComplete())
                }
                // 存储作业以便可以取消
                context.stash(job)
            }
            is CancelRequest -> {
                // 取消正在进行的流式请求
                val job = context.unstash() as? Job
                job?.cancel()
                context.respond(CancelResponse(success = true))
            }
            is ToolCallRequest -> {
                // 处理工具调用
                val result = runBlocking {
                    agent.executeTool(msg.toolName, msg.input)
                }
                context.respond(ToolCallResponse(result))
            }
            is CollaborationRequest -> {
                // 处理与其他 Agent 的协作请求
                val collaborationResult = runBlocking {
                    agent.generate(
                        "处理来自 ${msg.sender} 的请求: ${msg.task}",
                        AgentGenerateOptions(metadata = msg.metadata)
                    )
                }
                context.respond(CollaborationResponse(collaborationResult.text))
            }
            is PoisonPill -> {
                // 优雅地关闭 Agent
                context.stop(context.self)
            }
        }
    }
}
```

### 4.2 AgentMessage

```kotlin
// 基础消息接口
sealed interface AgentMessage

// Agent 请求
data class AgentRequest(
    val prompt: String,
    val options: AgentGenerateOptions = AgentGenerateOptions()
) : AgentMessage

// Agent 响应
data class AgentResponse(
    val text: String,
    val toolCalls: List<ToolCall> = emptyList()
) : AgentMessage

// 流式请求
data class AgentStreamRequest(
    val prompt: String,
    val options: AgentStreamOptions = AgentStreamOptions(),
    val sender: PID
) : AgentMessage

// 流式响应块
data class AgentStreamChunk(val chunk: String) : AgentMessage

// 流式响应完成
object AgentStreamComplete : AgentMessage

// 取消请求
object CancelRequest : AgentMessage

// 取消响应
data class CancelResponse(val success: Boolean) : AgentMessage

// 工具调用请求
data class ToolCallRequest(
    val toolName: String,
    val input: JsonObject
) : AgentMessage

// 工具调用响应
data class ToolCallResponse(val result: JsonObject) : AgentMessage

// 协作请求
data class CollaborationRequest(
    val task: String,
    val sender: String,
    val metadata: Map<String, String> = emptyMap()
) : AgentMessage

// 协作响应
data class CollaborationResponse(val result: String) : AgentMessage
```

### 4.3 AgentRegistry

```kotlin
class AgentRegistry {
    private val agents = ConcurrentHashMap<String, PID>()

    // 注册 Agent
    fun register(name: String, pid: PID) {
        agents[name] = pid
    }

    // 注销 Agent
    fun unregister(name: String) {
        agents.remove(name)
    }

    // 查找 Agent
    fun lookup(name: String): PID? {
        return agents[name]
    }

    // 获取所有注册的 Agent
    fun getAllAgents(): Map<String, PID> {
        return agents.toMap()
    }
}
```

### 4.4 RemoteAgent

```kotlin
class RemoteAgent(
    private val system: ActorSystem,
    private val address: String
) {
    // 连接到远程 Agent
    fun connect(agentId: String): PID {
        return PID(address, agentId)
    }

    // 发送消息给远程 Agent
    fun send(agentId: String, message: AgentMessage) {
        val pid = connect(agentId)
        system.root.send(pid, message)
    }

    // 请求-响应模式
    suspend fun ask(agentId: String, message: AgentMessage): AgentMessage {
        val pid = connect(agentId)
        return system.root.requestAwait<AgentMessage>(pid, message, timeout = 30.seconds)
    }
}

## 5. DSL 设计

### 5.1 Actor 化 Agent DSL

```kotlin
// 创建一个 Actor 化的 Agent，直接复用现有的 agent DSL
fun ActorSystem.actorAgent(block: ActorAgentBuilder.() -> Unit): PID {
    val builder = ActorAgentBuilder()
    builder.block()
    val agent = builder.agentBuilder.build()

    // 创建 Props，应用 actor 配置
    val props = Props.fromProducer { KastraxActor(agent) }
        .withMailbox(builder.actorBuilder.mailbox)
        .withDispatcher(builder.actorBuilder.dispatcher)
        .withSupervisor(builder.actorBuilder.supervisionStrategy)

    // 使用 agent 名称或生成随机名称
    return if (agent.name.isNotEmpty()) {
        this.root.spawnNamed(props, agent.name)
    } else {
        this.root.spawn(props)
    }
}

// Actor Agent 构建器，包含 agent 和 actor 两部分配置
class ActorAgentBuilder {
    val agentBuilder = AgentBuilder()
    val actorBuilder = ActorBuilder()

    // 配置 agent 部分
    fun agent(block: AgentBuilder.() -> Unit) {
        agentBuilder.apply(block)
    }

    // 配置 actor 部分
    fun actor(block: ActorBuilder.() -> Unit) {
        actorBuilder.apply(block)
    }
}

// Actor 配置构建器
class ActorBuilder {
    var supervisionStrategy: SupervisorStrategy = DefaultStrategy
    var mailbox: MailboxProducer = UnboundedMailboxProducer
    var dispatcher: Dispatcher = DefaultDispatcher

    // 监督策略配置
    fun oneForOneStrategy(block: OneForOneStrategyBuilder.() -> Unit) {
        val builder = OneForOneStrategyBuilder()
        builder.block()
        supervisionStrategy = builder.build()
    }

    // 邮箱配置
    fun unboundedMailbox() {
        mailbox = UnboundedMailboxProducer
    }

    fun boundedMailbox(capacity: Int) {
        mailbox = BoundedMailboxProducer(capacity)
    }
}

// 监督策略构建器
class OneForOneStrategyBuilder {
    var maxRetries: Int = 10
    var withinTimeRange: Duration = 10.seconds
    var decider: (Exception) -> SupervisorDirective = { SupervisorDirective.Restart }

    fun build(): SupervisorStrategy {
        return OneForOneStrategy(maxRetries, withinTimeRange, decider)
    }
}
```

### 5.2 Agent 网络 DSL

```kotlin
// 创建 Agent 网络
fun ActorSystem.agentNetwork(block: AgentNetworkBuilder.() -> Unit): AgentNetwork {
    val builder = AgentNetworkBuilder(this)
    builder.block()
    return builder.build()
}

class AgentNetworkBuilder(private val system: ActorSystem) {
    private val agents = mutableMapOf<String, PID>()
    private var coordinator: PID? = null

    // 添加 Agent
    fun agent(name: String, block: ActorAgentBuilder.() -> Unit) {
        val builder = ActorAgentBuilder()
        builder.block()
        val agent = builder.agentBuilder.build()

        // 创建 Props，应用 actor 配置
        val props = Props.fromProducer { KastraxActor(agent) }
            .withMailbox(builder.actorBuilder.mailbox)
            .withDispatcher(builder.actorBuilder.dispatcher)
            .withSupervisor(builder.actorBuilder.supervisionStrategy)

        val pid = system.root.spawnNamed(props, name)
        agents[name] = pid
    }

    // 设置协调者
    fun coordinator(block: ActorAgentBuilder.() -> Unit) {
        val builder = ActorAgentBuilder()
        builder.block()
        val agent = builder.agentBuilder.build()

        // 创建 Props，应用 actor 配置
        val props = Props.fromProducer { KastraxActor(agent) }
            .withMailbox(builder.actorBuilder.mailbox)
            .withDispatcher(builder.actorBuilder.dispatcher)
            .withSupervisor(builder.actorBuilder.supervisionStrategy)

        coordinator = system.root.spawnNamed(props, "coordinator")
    }

    // 构建网络
    fun build(): AgentNetwork {
        return AgentNetwork(system, agents, coordinator)
    }
}

class AgentNetwork(
    private val system: ActorSystem,
    private val agents: Map<String, PID>,
    private val coordinator: PID?
) {
    // 发送消息给特定 Agent
    fun send(agentName: String, message: AgentMessage) {
        val pid = agents[agentName] ?: throw IllegalArgumentException("Agent not found: $agentName")
        system.root.send(pid, message)
    }

    // 发送消息给协调者
    fun sendToCoordinator(message: AgentMessage) {
        coordinator?.let { system.root.send(it, message) }
    }

    // 广播消息给所有 Agent
    fun broadcast(message: AgentMessage) {
        agents.values.forEach { system.root.send(it, message) }
    }

    // 请求-响应模式
    suspend fun ask(agentName: String, message: AgentMessage): AgentMessage {
        val pid = agents[agentName] ?: throw IllegalArgumentException("Agent not found: $agentName")
        return system.root.requestAwait<AgentMessage>(pid, message)
    }
}
```

### 5.3 消息传递 DSL

```kotlin
// 发送消息
fun ActorSystem.sendMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()) {
    root.send(target, AgentRequest(prompt, options))
}

// 请求-响应模式
suspend fun ActorSystem.askMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()): String {
    val response = root.requestAwait<AgentResponse>(target, AgentRequest(prompt, options))
    return response.text
}

// 流式请求
fun ActorSystem.streamMessage(target: PID, prompt: String, options: AgentStreamOptions = AgentStreamOptions(), onChunk: (String) -> Unit) {
    val streamHandler = spawnStreamHandler(onChunk)
    root.send(target, AgentStreamRequest(prompt, options, streamHandler))
}

// 创建流处理 Actor
private fun ActorSystem.spawnStreamHandler(onChunk: (String) -> Unit): PID {
    val props = Props.fromFunc { ctx ->
        when (val msg = ctx.message) {
            is AgentStreamChunk -> onChunk(msg.chunk)
            is AgentStreamComplete -> ctx.stop(ctx.self)
        }
    }
    return root.spawn(props)
}
```

## 6. 分布式部署

### 6.1 远程 Actor 配置

```kotlin
// 配置远程 Actor 系统
fun configureRemoteActorSystem(port: Int): ActorSystem {
    // 创建 kactor 远程配置
    val config = RemoteConfig()
    config.port = port

    // 创建带远程配置的 ActorSystem
    return ActorSystem("kastrax-remote", config)
}

// 连接到远程 Actor 系统
fun connectToRemoteSystem(address: String, port: Int): RemoteAgent {
    val system = ActorSystem("kastrax-client")
    val remoteAddress = "$address:$port"
    return RemoteAgent(system, remoteAddress)
}
```

### 6.2 集群配置

```kotlin
// 配置集群
fun configureCluster(port: Int, seeds: List<String>): ActorSystem {
    // 创建 kactor 集群配置
    val config = ClusterConfig()
    config.port = port
    config.seeds = seeds

    // 创建带集群配置的 ActorSystem
    return ActorSystem("kastrax-cluster", config)
}

// 加入集群
fun ActorSystem.joinCluster() {
    // 启动集群
    this.cluster().join()
}

// 离开集群
fun ActorSystem.leaveCluster() {
    // 离开集群
    this.cluster().leave()
}
```

### 6.3 多模态数据传输

参考 AgentScope 的设计，我们实现了多模态数据的传输机制：

```kotlin
// 多模态消息
data class MultiModalMessage(
    val text: String,
    val images: List<ImageData> = emptyList(),
    val audio: List<AudioData> = emptyList(),
    val video: List<VideoData> = emptyList(),
    val files: List<FileData> = emptyList()
) : AgentMessage

// 图像数据
data class ImageData(
    val url: String,
    val mimeType: String = "image/jpeg",
    val metadata: Map<String, String> = emptyMap()
)

// 音频数据
data class AudioData(
    val url: String,
    val mimeType: String = "audio/mp3",
    val metadata: Map<String, String> = emptyMap()
)

// 视频数据
data class VideoData(
    val url: String,
    val mimeType: String = "video/mp4",
    val metadata: Map<String, String> = emptyMap()
)

// 文件数据
data class FileData(
    val url: String,
    val mimeType: String = "application/octet-stream",
    val filename: String,
    val metadata: Map<String, String> = emptyMap()
)

// 发送多模态消息
fun ActorSystem.sendMultiModalMessage(target: PID, message: MultiModalMessage) {
    root.send(target, message)
}
```

### 6.4 容错机制

我们实现了全面的容错机制，包括服务级重试和基于规则的纠正工具：

```kotlin
// 重试策略
class RetryStrategy(
    val maxRetries: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds,
    val backoffFactor: Double = 2.0
) {
    suspend fun <T> retry(block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                delay(currentDelay.inWholeMilliseconds)
                currentDelay = (currentDelay.toDouble() * backoffFactor)
                    .coerceAtMost(maxDelay.toDouble())
                    .toDuration(DurationUnit.MILLISECONDS)
            }
        }
        throw IllegalStateException("Should not reach here")
    }
}

// 应用重试策略
suspend fun <T> withRetry(
    retryStrategy: RetryStrategy = RetryStrategy(),
    block: suspend () -> T
): T {
    return retryStrategy.retry(block)
}
```
## 7. 使用示例

### 7.1 基本使用

```kotlin
fun main() = runBlocking {
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
}
```

### 7.2 Agent 网络

```kotlin
fun main() = runBlocking {
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

        agent("analyst") {
            agent {
                name = "分析师"
                instructions = "你是一个数据分析专家。"
                model = deepSeek { /* 配置 */ }
            }
        }

        agent("writer") {
            agent {
                name = "作家"
                instructions = "你是一个专业的内容创作者。"
                model = deepSeek { /* 配置 */ }
            }
        }
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
}
```

### 7.3 分布式部署

```kotlin
// 服务器端
fun startServer() {
    // 配置远程 Actor 系统
    val system = configureRemoteActorSystem(8080)

    // 注册 Agent
    system.actorAgent {
        agent {
            name = "远程助手"
            instructions = "你是一个远程助手，可以回答问题。"
            model = deepSeek { /* 配置 */ }
        }
        actor {
            // 远程 Actor 特有的配置
            oneForOneStrategy {
                maxRetries = 5
            }
        }
    }

    // 保持系统运行
    runBlocking {
        delay(Long.MAX_VALUE)
    }
}

// 客户端
fun connectToServer() = runBlocking {
    // 连接到远程系统
    val remoteAgent = connectToRemoteSystem("localhost", 8080)

    // 发送消息给远程 Agent
    val response = remoteAgent.ask("assistant", AgentRequest("你好，远程助手！"))
    println("远程助手回答: ${(response as AgentResponse).text}")
}
```

### 7.4 多模态使用示例

```kotlin
fun main() = runBlocking {
    // 创建 Actor 系统
    val system = ActorSystem("kastrax-system")

    // 创建支持多模态的 Agent
    val multimodalAgentPid = system.actorAgent {
        agent {
            name = "多模态助手"
            instructions = "你是一个能够处理图像和文本的助手。"
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY"))
                multimodal(true)
            }
        }
    }

    // 创建多模态消息
    val message = MultiModalMessage(
        text = "这张图片中有什么？",
        images = listOf(
            ImageData(url = "https://example.com/image.jpg")
        )
    )

    // 发送多模态消息
    system.sendMultiModalMessage(multimodalAgentPid, message)

    // 关闭系统
    system.shutdown()
}

## 8. 实现路线图

### 8.1 第一阶段：基础集成（2-3 周）

1. 创建 KastraxActor 类，实现 kactor 的 Actor 接口 [✅ 已实现] - 2023-07-15
2. 实现基本的消息传递机制 [✅ 已实现] - 2023-07-15
3. 开发 DSL 扩展，支持创建 Actor 化的 Agent [✅ 已实现] - 2023-07-15
4. 编写基本示例和测试 [✅ 已实现] - 2023-07-15, 2023-07-16

### 8.2 第二阶段：高级功能（2-3 周）

1. 实现 Agent 网络功能 [✅ 已实现] - 2023-07-15
2. 添加流式处理支持 [✅ 已实现] - 2023-07-15
3. 实现工具调用机制 [✅ 已实现] - 2023-07-15
4. 开发协作模式 [✅ 已实现] - 2023-07-15
5. 实现多模态数据传输

### 8.3 第三阶段：分布式功能（3-4 周）

1. 实现远程 Actor 配置
2. 开发集群支持
3. 实现服务发现机制
4. 添加故障恢复功能
5. 实现容错机制

### 8.4 第四阶段：优化和文档（1-2 周）

1. 性能优化
2. 编写详细文档
3. 创建更多示例
4. 进行测试和调试

## 9. 结论

通过直接整合 kactor 的 Actor 模型和 kastrax 的 AI Agent 功能，我们可以创建一个强大的分布式 AI Agent 框架，无需重新实现核心功能。这种方法最大限度地复用现有代码，同时提供新的分布式能力。

这个框架将使开发者能够：
- 创建可扩展的 AI Agent 系统
- 实现 Agent 之间的高效通信和协作
- 部署分布式 Agent 网络
- 构建复杂的 AI 应用
- 处理多模态数据
- 实现高可用性和容错能力

通过这种直接集成的方式，我们可以快速实现一个功能完整的分布式 AI Agent 框架，为 kastrax 生态系统增加重要的分布式能力。
## 2. 系统架构

### 2.1 整体架构

基于对 kastrax、kactor 和 AgentScope 的分析，我们设计了以下整体架构：

```
+------------------------------------------+
|            KastraxActorSystem            |
+------------------------------------------+
|                                          |
|  +-------------+       +-------------+   |
|  | KastraxActor|<----->| KastraxActor|   |
|  | (Agent)     |       | (Agent)     |   |
|  +-------------+       +-------------+   |
|        ^                     ^           |
|        |                     |           |
|        v                     v           |
|  +-------------+       +-------------+   |
|  | KastraxActor|<----->| KastraxActor|   |
|  | (Agent)     |       | (Agent)     |   |
|  +-------------+       +-------------+   |
|                                          |
+------------------------------------------+
           ^                     ^
           |                     |
           v                     v
+------------------+   +------------------+
|  kastrax tools   |   |  LLM Providers   |
+------------------+   +------------------+
```

这个架构的核心是将 kastrax 的 Agent 包装为 kactor 的 Actor，使其能够在 Actor 系统中运行。同时，我们保留了 kastrax 的所有功能，包括 LLM 集成和工具使用。

### 2.2 核心组件

1. **KastraxActorSystem**：扩展 kactor 的 ActorSystem，管理 KastraxActor 实例
2. **KastraxActor**：将 kastrax Agent 包装为 kactor Actor
3. **AgentMessage**：定义 Agent 之间通信的消息格式
4. **RemoteAgent**：支持远程 Agent 通信的组件
5. **AgentRegistry**：管理和发现 Agent 的注册表

### 2.3 分层架构

我们的架构分为以下几个层次：

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

## 3. 技术实现

### 3.1 直接集成方案

直接将 kastrax Agent 包装为 kactor Actor，保持两者的核心功能不变：

1. 创建 `KastraxActor` 类，实现 kactor 的 `Actor` 接口
2. 在 `KastraxActor` 内部持有 kastrax `Agent` 实例
3. 将接收到的 Actor 消息转换为 Agent 可处理的格式
4. 使用 Agent 处理消息并返回结果
5. 扩展 kastrax DSL，支持创建 Actor 化的 Agent

### 3.2 代码复用策略

最大限度地复用现有代码：

1. 直接使用 kactor 的 Actor 模型实现，不修改其核心功能
2. 直接使用 kastrax 的 Agent 实现，不修改其核心功能
3. 创建适配层连接两个系统
4. 扩展而非修改现有 API
- 实现分布式 Agent 部署和通信
- 保持与现有 kastrax DSL 的兼容性
- 提供简单的 API 进行集成

## 2. 系统架构

### 2.1 整体架构

```
+------------------------------------------+
|            KastraxActorSystem            |
+------------------------------------------+
|                                          |
|  +-------------+       +-------------+   |
|  | KastraxActor|<----->| KastraxActor|   |
|  | (Agent)     |       | (Agent)     |   |
|  +-------------+       +-------------+   |
|        ^                     ^           |
|        |                     |           |
|        v                     v           |
|  +-------------+       +-------------+   |
|  | KastraxActor|<----->| KastraxActor|   |
|  | (Agent)     |       | (Agent)     |   |
|  +-------------+       +-------------+   |
|                                          |
+------------------------------------------+
           ^                     ^
           |                     |
           v                     v
+------------------+   +------------------+
|  kastrax tools   |   |  LLM Providers   |
+------------------+   +------------------+
```

### 2.2 核心组件

1. **KastraxActorSystem**：扩展 kactor 的 ActorSystem，管理 KastraxActor 实例
2. **KastraxActor**：将 kastrax Agent 包装为 kactor Actor
3. **AgentMessage**：定义 Agent 之间通信的消息格式
4. **RemoteAgent**：支持远程 Agent 通信的组件
5. **AgentRegistry**：管理和发现 Agent 的注册表

## 3. 实现方案

### 3.1 直接集成方案

直接将 kastrax Agent 包装为 kactor Actor，保持两者的核心功能不变：

1. 创建 `KastraxActor` 类，实现 kactor 的 `Actor` 接口
2. 在 `KastraxActor` 内部持有 kastrax `Agent` 实例
3. 将接收到的 Actor 消息转换为 Agent 可处理的格式
4. 使用 Agent 处理消息并返回结果
5. 扩展 kastrax DSL，支持创建 Actor 化的 Agent

### 3.2 代码复用策略

最大限度地复用现有代码：

1. 直接使用 kactor 的 Actor 模型实现，不修改其核心功能
2. 直接使用 kastrax 的 Agent 实现，不修改其核心功能
3. 创建适配层连接两个系统
4. 扩展而非修改现有 API

## 4. 核心组件详细设计

### 4.1 KastraxActor

```kotlin
class KastraxActor(private val agent: Agent) : Actor {
    override fun receive(context: Context) {
        when (val msg = context.message) {
            is AgentRequest -> {
                // 使用 kastrax Agent 处理请求
                val response = runBlocking {
                    agent.generate(msg.prompt, msg.options)
                }
                // 发送响应
                context.respond(AgentResponse(response.text, response.toolCalls))
            }
            is AgentStreamRequest -> {
                // 处理流式请求
                val job = context.actorSystem.dispatcher.dispatch {
                    agent.stream(msg.prompt, msg.options) { chunk ->
                        context.send(msg.sender, AgentStreamChunk(chunk))
                    }
                    // 流结束后发送完成消息
                    context.send(msg.sender, AgentStreamComplete())
                }
                // 存储作业以便可以取消
                context.stash(job)
            }
            is CancelRequest -> {
                // 取消正在进行的流式请求
                val job = context.unstash() as? Job
                job?.cancel()
                context.respond(CancelResponse(success = true))
            }
            is ToolCallRequest -> {
                // 处理工具调用
                val result = runBlocking {
                    agent.executeTool(msg.toolName, msg.input)
                }
                context.respond(ToolCallResponse(result))
            }
            is CollaborationRequest -> {
                // 处理与其他 Agent 的协作请求
                val collaborationResult = runBlocking {
                    agent.generate(
                        "处理来自 ${msg.sender} 的请求: ${msg.task}",
                        AgentGenerateOptions(metadata = msg.metadata)
                    )
                }
                context.respond(CollaborationResponse(collaborationResult.text))
            }
            is PoisonPill -> {
                // 优雅地关闭 Agent
                context.stop(context.self)
            }
        }
    }
}
```

### 4.2 KastraxActorSystem

```kotlin
class KastraxActorSystem(name: String) {
    // 内部 kactor ActorSystem
    private val actorSystem = ActorSystem(name)

    // Agent 注册表
    private val agentRegistry = AgentRegistry()

    // 创建 Actor 化的 Agent
    fun actorOf(agent: Agent, name: String? = null): PID {
        val props = Props.fromProducer { KastraxActor(agent) }
        return if (name != null) {
            actorSystem.root.spawnNamed(props, name)
        } else {
            actorSystem.root.spawn(props)
        }
    }

    // 发送消息给 Agent
    fun send(target: PID, message: Any) {
        actorSystem.root.send(target, message)
    }

    // 请求-响应模式
    suspend fun ask(target: PID, message: Any): Any {
        return actorSystem.root.requestAwait<Any>(target, message, timeout = 30.seconds)
    }

    // 创建远程 Agent
    fun createRemoteAgent(address: String, agentId: String): PID {
        return PID(address, agentId)
    }

    // 注册 Agent 以便远程访问
    fun registerAgent(agent: Agent, name: String): PID {
        val pid = actorOf(agent, name)
        agentRegistry.register(name, pid)
        return pid
    }

    // 查找 Agent
    fun lookupAgent(name: String): PID? {
        return agentRegistry.lookup(name)
    }

    // 关闭系统
    fun shutdown() {
        actorSystem.shutdown()
    }
}
```

### 4.3 AgentMessage

```kotlin
// 基础消息接口
sealed interface AgentMessage

// Agent 请求
data class AgentRequest(
    val prompt: String,
    val options: AgentGenerateOptions = AgentGenerateOptions()
) : AgentMessage

// Agent 响应
data class AgentResponse(
    val text: String,
    val toolCalls: List<ToolCall> = emptyList()
) : AgentMessage

// 流式请求
data class AgentStreamRequest(
    val prompt: String,
    val options: AgentStreamOptions = AgentStreamOptions(),
    val sender: PID
) : AgentMessage

// 流式响应块
data class AgentStreamChunk(val chunk: String) : AgentMessage

// 流式响应完成
object AgentStreamComplete : AgentMessage

// 取消请求
object CancelRequest : AgentMessage

// 取消响应
data class CancelResponse(val success: Boolean) : AgentMessage

// 工具调用请求
data class ToolCallRequest(
    val toolName: String,
    val input: JsonObject
) : AgentMessage

// 工具调用响应
data class ToolCallResponse(val result: JsonObject) : AgentMessage

// 协作请求
data class CollaborationRequest(
    val task: String,
    val sender: String,
    val metadata: Map<String, String> = emptyMap()
) : AgentMessage

// 协作响应
data class CollaborationResponse(val result: String) : AgentMessage
```

### 4.4 AgentRegistry

```kotlin
class AgentRegistry {
    private val agents = ConcurrentHashMap<String, PID>()

    // 注册 Agent
    fun register(name: String, pid: PID) {
        agents[name] = pid
    }

    // 注销 Agent
    fun unregister(name: String) {
        agents.remove(name)
    }

    // 查找 Agent
    fun lookup(name: String): PID? {
        return agents[name]
    }

    // 获取所有注册的 Agent
    fun getAllAgents(): Map<String, PID> {
        return agents.toMap()
    }
}
```

### 4.5 RemoteAgent

```kotlin
class RemoteAgent(
    private val system: KastraxActorSystem,
    private val address: String
) {
    // 连接到远程 Agent
    fun connect(agentId: String): PID {
        return system.createRemoteAgent(address, agentId)
    }

    // 发送消息给远程 Agent
    fun send(agentId: String, message: AgentMessage) {
        val pid = connect(agentId)
        system.send(pid, message)
    }

    // 请求-响应模式
    suspend fun ask(agentId: String, message: AgentMessage): AgentMessage {
        val pid = connect(agentId)
        return system.ask(pid, message) as AgentMessage
    }
}
```

## 5. DSL 扩展

### 5.1 Actor 化 Agent DSL

```kotlin
// 创建一个 Actor 化的 Agent，直接复用现有的 agent DSL
fun ActorSystem.actorAgent(block: ActorAgentBuilder.() -> Unit): PID {
    val builder = ActorAgentBuilder()
    builder.block()
    val agent = builder.agentBuilder.build()

    // 创建 Props，应用 actor 配置
    val props = Props.fromProducer { KastraxActor(agent) }
        .withMailbox(builder.actorBuilder.mailbox)
        .withDispatcher(builder.actorBuilder.dispatcher)
        .withSupervisor(builder.actorBuilder.supervisionStrategy)

    // 使用 agent 名称或生成随机名称
    return if (agent.name.isNotEmpty()) {
        this.root.spawnNamed(props, agent.name)
    } else {
        this.root.spawn(props)
    }
}

// Actor Agent 构建器，包含 agent 和 actor 两部分配置
class ActorAgentBuilder {
    val agentBuilder = AgentBuilder()
    val actorBuilder = ActorBuilder()

    // 配置 agent 部分
    fun agent(block: AgentBuilder.() -> Unit) {
        agentBuilder.apply(block)
    }

    // 配置 actor 部分
    fun actor(block: ActorBuilder.() -> Unit) {
        actorBuilder.apply(block)
    }
}

// Actor 配置构建器
class ActorBuilder {
    var supervisionStrategy: SupervisorStrategy = DefaultStrategy
    var mailbox: MailboxProducer = UnboundedMailboxProducer
    var dispatcher: Dispatcher = DefaultDispatcher

    // 监督策略配置
    fun oneForOneStrategy(block: OneForOneStrategyBuilder.() -> Unit) {
        val builder = OneForOneStrategyBuilder()
        builder.block()
        supervisionStrategy = builder.build()
    }

    // 邮箱配置
    fun unboundedMailbox() {
        mailbox = UnboundedMailboxProducer
    }

    fun boundedMailbox(capacity: Int) {
        mailbox = BoundedMailboxProducer(capacity)
    }
}

// 监督策略构建器
class OneForOneStrategyBuilder {
    var maxRetries: Int = 10
    var withinTimeRange: Duration = 10.seconds
    var decider: (Exception) -> SupervisorDirective = { SupervisorDirective.Restart }

    fun build(): SupervisorStrategy {
        return OneForOneStrategy(maxRetries, withinTimeRange, decider)
    }
}
```

### 5.2 Agent 网络 DSL

```kotlin
// 创建 Agent 网络
fun ActorSystem.agentNetwork(block: AgentNetworkBuilder.() -> Unit): AgentNetwork {
    val builder = AgentNetworkBuilder(this)
    builder.block()
    return builder.build()
}

class AgentNetworkBuilder(private val system: ActorSystem) {
    private val agents = mutableMapOf<String, PID>()
    private var coordinator: PID? = null

    // 添加 Agent
    fun agent(name: String, block: ActorAgentBuilder.() -> Unit) {
        val builder = ActorAgentBuilder()
        builder.block()
        val agent = builder.agentBuilder.build()

        // 创建 Props，应用 actor 配置
        val props = Props.fromProducer { KastraxActor(agent) }
            .withMailbox(builder.actorBuilder.mailbox)
            .withDispatcher(builder.actorBuilder.dispatcher)
            .withSupervisor(builder.actorBuilder.supervisionStrategy)

        val pid = system.root.spawnNamed(props, name)
        agents[name] = pid
    }

    // 设置协调者
    fun coordinator(block: ActorAgentBuilder.() -> Unit) {
        val builder = ActorAgentBuilder()
        builder.block()
        val agent = builder.agentBuilder.build()

        // 创建 Props，应用 actor 配置
        val props = Props.fromProducer { KastraxActor(agent) }
            .withMailbox(builder.actorBuilder.mailbox)
            .withDispatcher(builder.actorBuilder.dispatcher)
            .withSupervisor(builder.actorBuilder.supervisionStrategy)

        coordinator = system.root.spawnNamed(props, "coordinator")
    }

    // 构建网络
    fun build(): AgentNetwork {
        return AgentNetwork(system, agents, coordinator)
    }
}

class AgentNetwork(
    private val system: ActorSystem,
    private val agents: Map<String, PID>,
    private val coordinator: PID?
) {
    // 发送消息给特定 Agent
    fun send(agentName: String, message: AgentMessage) {
        val pid = agents[agentName] ?: throw IllegalArgumentException("Agent not found: $agentName")
        system.send(pid, message)
    }

    // 发送消息给协调者
    fun sendToCoordinator(message: AgentMessage) {
        coordinator?.let { system.send(it, message) }
    }

    // 广播消息给所有 Agent
    fun broadcast(message: AgentMessage) {
        agents.values.forEach { system.send(it, message) }
    }

    // 请求-响应模式
    suspend fun ask(agentName: String, message: AgentMessage): AgentMessage {
        val pid = agents[agentName] ?: throw IllegalArgumentException("Agent not found: $agentName")
        return system.ask(pid, message) as AgentMessage
    }
}
```

### 5.3 消息传递 DSL

```kotlin
// 发送消息
fun ActorSystem.sendMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()) {
    root.send(target, AgentRequest(prompt, options))
}

// 请求-响应模式
suspend fun ActorSystem.askMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()): String {
    val response = root.requestAwait<AgentResponse>(target, AgentRequest(prompt, options))
    return response.text
}

// 流式请求
fun ActorSystem.streamMessage(target: PID, prompt: String, options: AgentStreamOptions = AgentStreamOptions(), onChunk: (String) -> Unit) {
    val streamHandler = spawnStreamHandler(onChunk)
    root.send(target, AgentStreamRequest(prompt, options, streamHandler))
}

// 创建流处理 Actor
private fun ActorSystem.spawnStreamHandler(onChunk: (String) -> Unit): PID {
    val props = Props.fromFunc { ctx ->
        when (val msg = ctx.message) {
            is AgentStreamChunk -> onChunk(msg.chunk)
            is AgentStreamComplete -> ctx.stop(ctx.self)
        }
    }
    return root.spawn(props)
}
```

## 6. 分布式部署

### 6.1 远程 Actor 配置

```kotlin
// 配置远程 Actor 系统
fun configureRemoteActorSystem(port: Int): ActorSystem {
    // 创建 kactor 远程配置
    val config = RemoteConfig()
    config.port = port

    // 创建带远程配置的 ActorSystem
    return ActorSystem("kastrax-remote", config)
}

// 连接到远程 Actor 系统
fun connectToRemoteSystem(address: String, port: Int): RemoteAgent {
    val system = ActorSystem("kastrax-client")
    val remoteAddress = "$address:$port"
    return RemoteAgent(system, remoteAddress)
}
```

### 6.2 集群配置

```kotlin
// 配置集群
fun configureCluster(port: Int, seeds: List<String>): ActorSystem {
    // 创建 kactor 集群配置
    val config = ClusterConfig()
    config.port = port
    config.seeds = seeds

    // 创建带集群配置的 ActorSystem
    return ActorSystem("kastrax-cluster", config)
}

// 加入集群
fun ActorSystem.joinCluster() {
    // 启动集群
    this.cluster().join()
}

// 离开集群
fun ActorSystem.leaveCluster() {
    // 离开集群
    this.cluster().leave()
}
```

## 7. 使用示例

### 7.1 基本使用

```kotlin
fun main() = runBlocking {
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
}
```

### 7.2 Agent 网络

```kotlin
fun main() = runBlocking {
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

        agent("analyst") {
            agent {
                name = "分析师"
                instructions = "你是一个数据分析专家。"
                model = deepSeek { /* 配置 */ }
            }
        }

        agent("writer") {
            agent {
                name = "作家"
                instructions = "你是一个专业的内容创作者。"
                model = deepSeek { /* 配置 */ }
            }
        }
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
}
```

### 7.3 分布式部署

```kotlin
// 服务器端
fun startServer() {
    // 配置远程 Actor 系统
    val system = configureRemoteActorSystem(8080)

    // 注册 Agent
    system.actorAgent {
        agent {
            name = "远程助手"
            instructions = "你是一个远程助手，可以回答问题。"
            model = deepSeek { /* 配置 */ }
        }
        actor {
            // 远程 Actor 特有的配置
            oneForOneStrategy {
                maxRetries = 5
            }
        }
    }

    // 保持系统运行
    runBlocking {
        delay(Long.MAX_VALUE)
    }
}

// 客户端
fun connectToServer() = runBlocking {
    // 连接到远程系统
    val remoteAgent = connectToRemoteSystem("localhost", 8080)

    // 发送消息给远程 Agent
    val response = remoteAgent.ask("assistant", AgentRequest("你好，远程助手！"))
    println("远程助手回答: ${(response as AgentResponse).text}")
}
```

## 8. 实现路线图

### 8.1 第一阶段：基础集成（2-3 周）

1. 创建 KastraxActor 和 KastraxActorSystem 类
2. 实现基本的消息传递机制
3. 开发 DSL 扩展
4. 编写基本示例

### 8.2 第二阶段：高级功能（2-3 周）

1. 实现 Agent 网络功能
2. 添加流式处理支持
3. 实现工具调用机制
4. 开发协作模式

### 8.3 第三阶段：分布式功能（3-4 周）

1. 实现远程 Actor 配置
2. 开发集群支持
3. 实现服务发现机制
4. 添加故障恢复功能

### 8.4 第四阶段：优化和文档（1-2 周）

1. 性能优化
2. 编写详细文档
3. 创建更多示例
4. 进行测试和调试

## 9. 结论

通过直接整合 kactor 的 Actor 模型和 kastrax 的 AI Agent 功能，我们可以创建一个强大的分布式 AI Agent 框架，无需重新实现核心功能。这种方法最大限度地复用现有代码，同时提供新的分布式能力。

这个框架将使开发者能够：
- 创建可扩展的 AI Agent 系统
- 实现 Agent 之间的高效通信和协作
- 部署分布式 Agent 网络
- 构建复杂的 AI 应用

通过这种直接集成的方式，我们可以快速实现一个功能完整的分布式 AI Agent 框架，为 kastrax 生态系统增加重要的分布式能力。
