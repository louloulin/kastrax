# KastraX 内存系统详解

内存系统是 KastraX 框架的核心组件之一，它提供了存储和检索对话历史的功能。本文档详细介绍了如何使用和扩展内存系统。

## 1. 内存系统概述

KastraX 的内存系统具有以下特点：

- **对话历史存储**：保存和检索对话消息
- **线程管理**：支持多个独立的对话线程
- **语义搜索**：根据语义相关性搜索历史消息
- **可扩展存储**：支持不同的存储后端（内存、数据库等）

## 2. 核心接口

内存系统的核心是 `Memory` 接口：

```kotlin
interface Memory {
    /**
     * 保存消息到指定的线程。
     */
    suspend fun saveMessage(message: Message, threadId: String): String
    
    /**
     * 获取指定线程的消息。
     */
    suspend fun getMessages(threadId: String, limit: Int = 10): List<MemoryMessage>
    
    /**
     * 搜索指定线程中与查询相关的消息。
     */
    suspend fun searchMessages(query: String, threadId: String, limit: Int = 5): List<MemoryMessage>
    
    /**
     * 创建新的线程。
     */
    suspend fun createThread(title: String? = null): String
    
    /**
     * 删除指定的线程。
     */
    suspend fun deleteThread(threadId: String): Boolean
    
    /**
     * 获取线程信息。
     */
    suspend fun getThread(threadId: String): MemoryThread?
    
    /**
     * 列出所有线程。
     */
    suspend fun listThreads(limit: Int = 20, offset: Int = 0): List<MemoryThread>
}
```

## 3. 消息和线程

### 3.1 消息接口

消息由 `Message` 接口表示：

```kotlin
interface Message {
    /**
     * 消息角色。
     */
    val role: MessageRole
    
    /**
     * 消息内容。
     */
    val content: String
    
    /**
     * 消息发送者的名称（可选）。
     */
    val name: String?
    
    /**
     * 消息中的工具调用（可选）。
     */
    val toolCalls: List<ToolCall>
    
    /**
     * 如果这是工具响应，则为工具调用ID（可选）。
     */
    val toolCallId: String?
}
```

消息角色由 `MessageRole` 枚举定义：

```kotlin
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}
```

### 3.2 线程

线程由 `MemoryThread` 数据类表示：

```kotlin
data class MemoryThread(
    val id: String,
    val title: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messageCount: Int = 0
)
```

## 4. 使用内存系统

### 4.1 创建内存系统

使用 `MemoryFactory` 创建内存系统：

```kotlin
// 创建内存系统
val memory = MemoryFactory.createMemory {
    storage(MemoryFactory.createInMemoryStorage())
    lastMessages(10)
    semanticRecall(true)
}
```

### 4.2 创建线程

```kotlin
// 创建一个新的对话线程
val threadId = memory.createThread("示例对话")
```

### 4.3 保存消息

```kotlin
// 创建一个简单的消息适配器
class SimpleMessage(
    override val role: MessageRole,
    override val content: String,
    override val name: String? = null,
    override val toolCalls: List<ToolCall> = emptyList(),
    override val toolCallId: String? = null
) : Message

// 保存系统消息
val systemMessage = SimpleMessage(
    role = MessageRole.SYSTEM,
    content = "你是一个有帮助的助手。"
)
memory.saveMessage(systemMessage, threadId)

// 保存用户消息
val userMessage = SimpleMessage(
    role = MessageRole.USER,
    content = "你好，请介绍一下自己。"
)
memory.saveMessage(userMessage, threadId)

// 保存助手消息
val assistantMessage = SimpleMessage(
    role = MessageRole.ASSISTANT,
    content = "你好！我是一个AI助手，我可以回答问题、提供信息和帮助你完成各种任务。"
)
memory.saveMessage(assistantMessage, threadId)
```

### 4.4 获取消息

```kotlin
// 获取对话历史
val messages = memory.getMessages(threadId)
messages.forEach { message ->
    println("[${message.message.role}] ${message.message.content}")
}
```

### 4.5 搜索消息

```kotlin
// 搜索消息
val searchResults = memory.searchMessages("人工智能", threadId)
searchResults.forEach { message ->
    println("[${message.message.role}] ${message.message.content}")
}
```

### 4.6 管理线程

```kotlin
// 获取线程信息
val thread = memory.getThread(threadId)
println("线程标题: ${thread?.title}")
println("消息数量: ${thread?.messageCount}")

// 列出所有线程
val threads = memory.listThreads()
threads.forEach { t ->
    println("${t.id}: ${t.title} (${t.messageCount}条消息)")
}

// 删除线程
memory.deleteThread(threadId)
```

## 5. 在代理中使用内存系统

内存系统可以与代理集成，使代理能够记住对话历史：

```kotlin
// 创建一个带有内存系统的代理
val myAgent = agent {
    name = "记忆助手"
    instructions = "你是一个有记忆能力的助手，能够记住之前的对话内容。"
    model = openAi("gpt-3.5-turbo")
    
    // 设置内存系统
    memory = MemoryFactory.createMemory {
        storage(MemoryFactory.createInMemoryStorage())
        lastMessages(10)
        semanticRecall(true)
    }
}

// 使用代理，它会自动使用内存系统
val response1 = myAgent.generate("你好，我叫张三。")
println(response1.text)

// 代理会记住之前的对话
val response2 = myAgent.generate("你还记得我的名字吗？")
println(response2.text) // 应该会提到"张三"
```

## 6. 存储实现

### 6.1 内存存储

KastraX 提供了一个内存中的存储实现，适用于开发和测试：

```kotlin
val inMemoryStorage = MemoryFactory.createInMemoryStorage()
```

这种存储方式不会持久化数据，应用重启后数据会丢失。

### 6.2 自定义存储

您可以通过实现 `MemoryStorage` 接口来创建自定义存储：

```kotlin
class CustomStorage : MemoryStorage {
    // 实现所有必需的方法
    override suspend fun saveMessage(message: MemoryMessage): String {
        // 实现
    }
    
    override suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage> {
        // 实现
    }
    
    // 其他方法...
}
```

## 7. 高级功能

### 7.1 语义搜索

启用语义搜索可以根据语义相关性而不仅仅是关键词匹配来搜索消息：

```kotlin
val memory = MemoryFactory.createMemory {
    storage(MemoryFactory.createInMemoryStorage())
    semanticRecall(true) // 启用语义搜索
}
```

### 7.2 消息限制

您可以限制检索的消息数量：

```kotlin
val memory = MemoryFactory.createMemory {
    storage(MemoryFactory.createInMemoryStorage())
    lastMessages(5) // 只保留最近的5条消息
}
```

## 8. 最佳实践

1. **线程管理**：为不同的对话创建不同的线程，避免混淆上下文
2. **系统消息**：在每个线程的开始添加一个系统消息，定义代理的行为
3. **定期清理**：定期删除不再需要的线程，以节省存储空间
4. **错误处理**：妥善处理内存操作中可能出现的异常
5. **并发访问**：注意在多线程环境中安全地访问内存系统

## 9. 完整示例

以下是一个完整的内存系统使用示例：

```kotlin
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.impl.MemoryFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建内存系统
    val memory = MemoryFactory.createMemory {
        storage(MemoryFactory.createInMemoryStorage())
        lastMessages(10)
        semanticRecall(true)
    }
    
    // 创建一个新的对话线程
    val threadId = memory.createThread("示例对话")
    
    // 创建一个简单的消息适配器
    class SimpleMessage(
        override val role: MessageRole,
        override val content: String,
        override val name: String? = null,
        override val toolCalls: List<ai.kastrax.memory.api.ToolCall> = emptyList(),
        override val toolCallId: String? = null
    ) : ai.kastrax.memory.api.Message
    
    // 保存系统消息
    val systemMessage = SimpleMessage(
        role = MessageRole.SYSTEM,
        content = "你是一个有帮助的助手。"
    )
    memory.saveMessage(systemMessage, threadId)
    
    // 保存用户消息
    val userMessage = SimpleMessage(
        role = MessageRole.USER,
        content = "你好，请介绍一下自己。"
    )
    memory.saveMessage(userMessage, threadId)
    
    // 保存助手消息
    val assistantMessage = SimpleMessage(
        role = MessageRole.ASSISTANT,
        content = "你好！我是一个AI助手，我可以回答问题、提供信息和帮助你完成各种任务。"
    )
    memory.saveMessage(assistantMessage, threadId)
    
    // 获取对话历史
    val messages = memory.getMessages(threadId)
    messages.forEach { message ->
        println("[${message.message.role}] ${message.message.content}")
    }
    
    // 搜索消息
    val searchResults = memory.searchMessages("AI", threadId)
    searchResults.forEach { message ->
        println("搜索结果: [${message.message.role}] ${message.message.content}")
    }
    
    // 删除线程
    memory.deleteThread(threadId)
}
```
