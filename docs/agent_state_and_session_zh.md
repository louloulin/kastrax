# KastraX Agent状态管理和会话控制功能文档

本文档详细介绍了KastraX中新增的Agent状态管理和会话控制功能，包括状态跟踪、会话管理和使用方法。

## 1. 概述

Agent状态管理和会话控制功能允许开发者跟踪和管理Agent的运行状态，以及创建和管理会话。这些功能对于构建具有状态感知和会话持久化的应用程序非常重要，特别是在构建聊天机器人、虚拟助手和其他需要维护上下文的应用程序时。

主要功能包括：

1. **状态管理**：跟踪Agent的运行状态（空闲、思考中、执行工具等）
2. **会话控制**：创建、获取和管理会话
3. **消息管理**：保存和检索会话消息
4. **状态持久化**：支持状态和会话的持久化

## 2. 核心组件

### 2.1 AgentState

`AgentState`类表示Agent的运行状态，包含以下主要属性：

```kotlin
data class AgentState(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String? = null,
    val resourceId: String? = null,
    val status: AgentStatus = AgentStatus.IDLE,
    val metadata: Map<String, String> = emptyMap(),
    val variables: Map<String, JsonElement> = emptyMap(),
    val lastUpdated: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now()
)
```

### 2.2 AgentStatus

`AgentStatus`枚举定义了Agent的可能状态：

```kotlin
enum class AgentStatus {
    IDLE,        // 空闲状态
    THINKING,    // 思考中
    EXECUTING,   // 执行工具
    RESPONDING,  // 生成响应
    ERROR,       // 错误状态
    PAUSED       // 暂停状态
}
```

### 2.3 SessionInfo

`SessionInfo`类表示会话信息：

```kotlin
data class SessionInfo(
    val id: String,
    val title: String? = null,
    val resourceId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val messageCount: Int = 0,
    val lastUpdated: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now()
)
```

### 2.4 SessionMessage

`SessionMessage`类表示会话中的消息：

```kotlin
data class SessionMessage(
    val id: String,
    val sessionId: String,
    val message: LlmMessage,
    val createdAt: Instant = Clock.System.now()
)
```

### 2.5 SessionManager

`SessionManager`接口定义了会话管理的方法：

```kotlin
interface SessionManager {
    suspend fun createSession(
        title: String? = null,
        resourceId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): SessionInfo
    
    suspend fun getSession(sessionId: String): SessionInfo?
    
    suspend fun updateSession(
        sessionId: String,
        updates: Map<String, Any>
    ): SessionInfo
    
    suspend fun deleteSession(sessionId: String): Boolean
    
    suspend fun getSessionsByResource(resourceId: String): List<SessionInfo>
    
    suspend fun saveMessage(message: LlmMessage, sessionId: String): String
    
    suspend fun getMessages(sessionId: String, limit: Int = 100): List<SessionMessage>
}
```

### 2.6 StateManager

`StateManager`接口定义了状态管理的方法：

```kotlin
interface StateManager {
    suspend fun saveState(state: AgentState): AgentState
    
    suspend fun getState(stateId: String): AgentState?
    
    suspend fun getStateByThread(threadId: String): AgentState?
    
    suspend fun getStatesByResource(resourceId: String): List<AgentState>
    
    suspend fun deleteState(stateId: String): Boolean
}
```

### 2.7 Agent接口扩展

Agent接口扩展了以下方法来支持状态管理和会话控制：

```kotlin
interface Agent {
    // 现有方法...
    
    suspend fun getState(): AgentState?
    
    suspend fun updateState(status: AgentStatus): AgentState?
    
    suspend fun createSession(
        title: String? = null,
        resourceId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): SessionInfo?
    
    suspend fun getSession(sessionId: String): SessionInfo?
    
    suspend fun getSessionMessages(sessionId: String, limit: Int = 100): List<SessionMessage>?
}
```

### 2.8 AgentResponse扩展

`AgentResponse`类扩展了以下属性来包含状态和会话信息：

```kotlin
data class AgentResponse(
    // 现有属性...
    val state: AgentState? = null,
    val sessionInfo: SessionInfo? = null
)
```

## 3. 实现类

### 3.1 InMemorySessionManager

`InMemorySessionManager`类是`SessionManager`接口的内存实现，用于在内存中管理会话和消息。

### 3.2 InMemoryStateManager

`InMemoryStateManager`类是`StateManager`接口的内存实现，用于在内存中管理状态。

## 4. 使用方法

### 4.1 创建带有状态管理和会话控制的Agent

```kotlin
// 创建会话管理器和状态管理器
val sessionManager = InMemorySessionManager()
val stateManager = InMemoryStateManager()

// 创建Agent
val agent = agent {
    name = "StateAwareAgent"
    instructions = "You are a helpful assistant with state management capabilities."
    model = openAi("gpt-4o")
    
    // 添加会话管理器和状态管理器
    sessionManager(sessionManager)
    stateManager(stateManager)
}
```

### 4.2 创建和管理会话

```kotlin
// 创建会话
val session = agent.createSession(
    title = "用户会话",
    resourceId = "user-123",
    metadata = mapOf("category" to "general")
)

// 获取会话信息
val sessionInfo = agent.getSession(session.id)

// 获取会话消息
val messages = agent.getSessionMessages(session.id)
```

### 4.3 管理Agent状态

```kotlin
// 获取当前状态
val currentState = agent.getState()

// 更新状态
val thinkingState = agent.updateState(AgentStatus.THINKING)

// 重置状态
agent.reset()
```

### 4.4 生成带有状态和会话信息的响应

```kotlin
// 生成响应
val response = agent.generate(
    "Hello, how are you?",
    AgentGenerateOptions(threadId = session.id)
)

// 获取响应中的状态和会话信息
val responseState = response.state
val responseSessionInfo = response.sessionInfo
```

### 4.5 流式生成带有状态和会话信息的响应

```kotlin
// 流式生成响应
val streamResponse = agent.stream(
    "Tell me a story",
    AgentStreamOptions(threadId = session.id)
)

// 处理流式响应
streamResponse.textStream?.collect { chunk ->
    print(chunk)
}

// 获取最终状态
val finalState = agent.getState()
```

## 5. 状态转换流程

在Agent的生成和流式生成过程中，状态会按照以下流程转换：

1. **初始状态**：IDLE（空闲）
2. **生成开始**：THINKING（思考中）
3. **执行工具**：EXECUTING（执行工具）
4. **生成响应**：RESPONDING（响应中）
5. **完成**：IDLE（空闲）

如果在任何阶段发生错误，状态将变为ERROR（错误）。

## 6. 会话和消息流程

会话和消息的处理流程如下：

1. **创建会话**：使用`createSession`方法创建新会话
2. **用户消息**：当用户发送消息时，消息会保存到会话中
3. **生成响应**：Agent生成响应，响应也会保存到会话中
4. **检索消息**：使用`getSessionMessages`方法检索会话消息

## 7. 最佳实践

### 7.1 状态管理最佳实践

- 在长时间运行的操作开始前更新状态为THINKING或EXECUTING
- 在操作完成后更新状态为IDLE
- 在发生错误时更新状态为ERROR
- 使用状态元数据存储额外的状态信息

### 7.2 会话管理最佳实践

- 为每个用户创建单独的会话
- 使用会话元数据存储会话相关信息（如用户偏好、会话类型等）
- 定期清理不再需要的会话
- 在生成响应时始终提供会话ID

## 8. 扩展和自定义

### 8.1 自定义SessionManager

你可以通过实现`SessionManager`接口来创建自定义的会话管理器，例如使用数据库或Redis来持久化会话：

```kotlin
class DatabaseSessionManager(private val database: Database) : SessionManager {
    // 实现接口方法...
}
```

### 8.2 自定义StateManager

同样，你可以通过实现`StateManager`接口来创建自定义的状态管理器：

```kotlin
class RedisStateManager(private val redisClient: RedisClient) : StateManager {
    // 实现接口方法...
}
```

## 9. 总结

KastraX的Agent状态管理和会话控制功能提供了强大的工具，使开发者能够构建具有状态感知和会话持久化的AI应用程序。通过跟踪Agent的运行状态和管理会话，可以创建更智能、更具上下文感知的用户体验。

这些功能的实现使KastraX更接近Mastra的功能水平，并在某些方面提供了更符合Kotlin风格的API设计。未来，我们将继续增强这些功能，添加更多高级特性，如分布式会话管理、状态转换钩子和更复杂的状态机。
