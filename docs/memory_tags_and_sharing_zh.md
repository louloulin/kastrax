# KastraX 内存标签和线程共享功能详解

KastraX 内存系统提供了强大的内存标签和线程共享功能，使得对话历史管理更加灵活和高效。本文档详细介绍了这些功能的使用方法和最佳实践。

## 1. 内存标签系统

内存标签系统允许对消息进行标记和分类，方便后续检索和管理。

### 1.1 标签的基本概念

标签由以下几个部分组成：

- **名称（name）**：标签的名称，如"topic"、"language"等
- **值（value）**：标签的值，如"Python"、"Java"等
- **颜色（color）**：标签的颜色，用于UI显示，如"#3572A5"

### 1.2 使用标签

#### 1.2.1 创建和添加标签

```kotlin
// 创建增强型内存，启用标签管理器
val memory = enhancedMemory {
    lastMessages(10)
    tagManager(true)
}

// 创建线程
val threadId = memory.createThread("Python讨论")

// 保存消息
val messageId = memory.saveMessage(
    SimpleMessage(
        role = MessageRole.USER,
        content = "Python是一种简单易学的编程语言，适合初学者。"
    ),
    threadId
)

// 添加标签
(memory as EnhancedMemory).addTagToMessage(
    messageId,
    MemoryTag(
        name = "language",
        value = "Python",
        color = "#3572A5"
    )
)
```

#### 1.2.2 获取消息的标签

```kotlin
// 获取消息的标签
val tags = (memory as EnhancedMemory).getMessageTags(messageId)

// 输出标签
tags.forEach { tag ->
    println("${tag.name}: ${tag.value}")
}
```

#### 1.2.3 根据标签搜索消息

```kotlin
// 搜索Python相关消息
val pythonMessages = (memory as EnhancedMemory).searchMessagesByTag(
    threadId,
    tagName = "language",
    tagValue = "Python"
)

// 输出搜索结果
pythonMessages.forEach { message ->
    println("${message.message.role}: ${message.message.content}")
}
```

### 1.3 标签分类

标签分类允许将相关标签分组，方便管理和检索。

```kotlin
// 创建标签分类
(memory as EnhancedMemory).tagManager?.createTagCategory(
    name = "programming_languages",
    description = "编程语言相关标签"
)

// 添加标签到分类
(memory as EnhancedMemory).tagManager?.addTagToCategory(
    tagName = "language",
    categoryName = "programming_languages"
)

// 获取所有标签分类
val categories = (memory as EnhancedMemory).tagManager?.getTagCategories()
```

### 1.4 自动标签

KastraX 支持自动为消息添加标签，例如根据消息的角色添加标签：

```kotlin
// 自动添加角色标签
(memory as EnhancedMemory).addTagToMessage(
    messageId,
    MemoryTag(
        name = "role",
        value = message.role,
        color = when (message.role) {
            "user" -> "#2196F3"
            "assistant" -> "#4CAF50"
            "system" -> "#FF9800"
            else -> "#9E9E9E"
        }
    )
)
```

## 2. 线程共享功能

线程共享功能允许将多个线程的消息合并到一个共享线程中，方便跨线程分析和讨论。

### 2.1 创建共享线程

```kotlin
// 创建增强型内存，启用线程共享
val memory = enhancedMemory {
    lastMessages(10)
    threadSharing(true)
}

// 创建两个线程
val threadId1 = memory.createThread("Python讨论")
val threadId2 = memory.createThread("Java讨论")

// 在两个线程中添加消息
// ...

// 创建共享线程
val sharedThreadId = (memory as EnhancedMemory).createSharedThread(
    "编程语言比较",
    listOf(threadId1, threadId2)
)
```

### 2.2 在共享线程中添加消息

```kotlin
// 在共享线程中添加新消息
val newMessage = SimpleMessage(
    role = MessageRole.USER,
    content = "Python和Java各有优势，选择哪种语言取决于具体的应用场景和需求。"
)

memory.saveMessage(newMessage, sharedThreadId)
```

### 2.3 获取共享线程中的消息

```kotlin
// 获取共享线程中的消息
val sharedMessages = memory.getMessages(sharedThreadId)

// 输出消息
sharedMessages.forEach { message ->
    println("${message.message.role}: ${message.message.content}")
}
```

## 3. 访问控制

访问控制功能允许对线程的访问进行权限管理，确保敏感信息的安全。

### 3.1 添加用户到访问控制列表

```kotlin
// 添加用户到线程的访问控制列表
val userId = "user123"
(memory as EnhancedMemory).addUserToThreadAccess(threadId, userId)
```

### 3.2 检查用户是否有权限访问线程

```kotlin
// 检查用户是否有权限访问线程
val hasAccess = (memory as EnhancedMemory).hasAccessToThread(threadId, userId)

if (hasAccess) {
    println("用户有权限访问线程")
} else {
    println("用户无权限访问线程")
}
```

### 3.3 从访问控制列表中移除用户

```kotlin
// 从线程的访问控制列表中移除用户
(memory as EnhancedMemory).removeUserFromThreadAccess(threadId, userId)
```

## 4. 存储后端

KastraX 支持多种存储后端的标签管理器和线程共享实现：

### 4.1 内存存储

```kotlin
// 创建增强型内存，使用内存存储
val memory = enhancedMemory {
    lastMessages(10)
    tagManager(true)
    threadSharing(true)
}
```

### 4.2 Redis存储

```kotlin
// 创建Redis连接池
val jedisPool = JedisPool(JedisPoolConfig(), "localhost", 6379)

// 创建增强型内存，使用Redis存储
val memory = enhancedMemory {
    storage(jedisPool)
    lastMessages(10)
    tagManager(true)
    threadSharing(true)
}
```

## 5. 最佳实践

### 5.1 标签命名规范

为了保持一致性和可维护性，建议遵循以下标签命名规范：

- 使用小写字母和下划线命名标签，如"language"、"topic"、"priority"等
- 使用有意义的标签名称，避免使用过于抽象或模糊的名称
- 对于常用标签，使用统一的命名约定，如"role"表示消息角色，"language"表示编程语言等

### 5.2 标签值规范

标签值应该简洁明了，便于检索和过滤：

- 使用简短的词语或短语作为标签值，如"Python"、"Java"、"high_priority"等
- 对于枚举类型的标签，使用固定的值集合，如优先级可以是"high"、"medium"、"low"
- 对于颜色标签，使用标准的十六进制颜色代码，如"#3572A5"、"#B07219"等

### 5.3 线程共享最佳实践

- 只共享相关的线程，避免将不相关的线程合并到一起
- 为共享线程设置有意义的标题，反映共享线程的主题或目的
- 在共享线程中添加总结性消息，帮助理解线程之间的关系
- 使用标签对共享线程中的消息进行分类，方便后续检索和分析

### 5.4 访问控制最佳实践

- 默认情况下，不设置访问控制，允许所有用户访问
- 只对包含敏感信息的线程设置访问控制
- 定期审查访问控制列表，移除不再需要访问权限的用户
- 记录访问控制变更，方便审计和问题排查

## 6. 示例应用

KastraX 提供了多个示例应用，展示如何使用内存标签和线程共享功能：

1. **标签和线程共享示例**：展示如何使用标签管理器和线程共享功能
2. **多Agent协作示例**：展示如何使用线程共享功能实现多Agent协作
3. **信息分类示例**：展示如何使用标签系统对信息进行分类和检索

这些示例可以在`examples/src/main/kotlin/ai/kastrax/examples/memory`目录中找到。

## 7. 总结

KastraX 内存标签和线程共享功能提供了强大的对话历史管理能力，使得信息检索、分类和共享更加灵活和高效。通过合理使用这些功能，可以构建出具有更强大记忆和协作能力的智能Agent系统。
