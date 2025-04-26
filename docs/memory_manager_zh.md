# KastraX 记忆管理器

KastraX 记忆管理器提供了强大的记忆系统查询、统计、导出和批量操作功能，使开发者能够更灵活地管理和利用对话历史数据。

## 1. 概述

记忆管理器扩展了基本的记忆系统，提供以下功能：

- **高级查询**：使用复杂的过滤器组合查询消息
- **上下文获取**：获取特定消息的上下文
- **线程管理**：查询、合并和分割线程
- **统计分析**：获取消息和线程的统计信息
- **导出导入**：支持多种格式导出和导入对话线程
- **批量操作**：批量删除消息和更新消息优先级

## 2. 使用记忆管理器

### 2.1 创建记忆管理器

```kotlin
import ai.kastrax.memory.api.*
import ai.kastrax.memory.impl.*

// 方法1：使用现有的存储创建记忆管理器
val storage = InMemoryStorage()
val memory = MemoryImpl(storage)
val manager = MemoryManagerFactory.createManager(storage)

// 方法2：直接创建内存中的记忆管理器
val inMemoryManager = MemoryManagerFactory.createInMemoryManager()

// 方法3：创建基于SQLite的记忆管理器
val sqliteManager = MemoryManagerFactory.createSQLiteManager("memory.db")

// 方法4：使用DSL函数
val manager1 = inMemoryManager()
val manager2 = memoryManager(storage)
```

### 2.2 基本使用

记忆管理器与基本记忆系统配合使用：

```kotlin
// 使用基本记忆系统创建线程和保存消息
val threadId = memory.createThread("示例对话")
memory.saveMessage(message, threadId)

// 使用记忆管理器进行高级操作
val userMessages = manager.queryMessages(
    threadId,
    MemoryQueryOptions(
        filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
    )
)
```

## 3. 高级查询功能

### 3.1 使用过滤器查询消息

记忆管理器支持多种过滤器组合查询消息：

```kotlin
// 角色过滤器
val userMessages = manager.queryMessages(
    threadId,
    MemoryQueryOptions(
        filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
    )
)

// 内容过滤器
val aiMessages = manager.queryMessages(
    threadId,
    MemoryQueryOptions(
        filters = listOf(ContentFilter(
            query = "人工智能",
            matchType = ContentMatchType.CONTAINS
        ))
    )
)

// 元数据过滤器
val taggedMessages = manager.queryMessages(
    threadId,
    MemoryQueryOptions(
        filters = listOf(MetadataFilter(
            key = "tag",
            value = "important",
            matchType = MetadataMatchType.EQUALS
        ))
    )
)

// 优先级范围过滤器
val highPriorityMessages = manager.queryMessages(
    threadId,
    MemoryQueryOptions(
        filters = listOf(PriorityRangeFilter(
            minPriority = MemoryPriority(0.7f),
            maxPriority = null
        ))
    )
)

// 组合多个过滤器
val complexQuery = manager.queryMessages(
    threadId,
    MemoryQueryOptions(
        filters = listOf(
            RoleFilter(roles = listOf(MessageRole.USER)),
            ContentFilter(query = "人工智能", matchType = ContentMatchType.CONTAINS)
        ),
        sortBy = SortField.PRIORITY,
        sortDirection = SortDirection.DESC,
        limit = 10
    )
)
```

### 3.2 获取消息上下文

```kotlin
// 获取消息上下文
val contextMessages = manager.getMessageContext(
    ContextSelector(
        messageId = messageId,
        beforeCount = 2,  // 获取前面2条消息
        afterCount = 2    // 获取后面2条消息
    )
)

// 获取多个消息的上下文
val multipleContexts = manager.getMultipleMessageContexts(
    listOf(
        ContextSelector(messageId1, 1, 1),
        ContextSelector(messageId2, 1, 1)
    )
)
```

### 3.3 查询线程

```kotlin
// 查询线程
val threads = manager.queryThreads(
    ThreadQueryOptions(
        titleFilter = "对话",
        timeRange = TimeRange(
            start = Clock.System.now().minus(DateTimePeriod(days = 7)),
            end = null
        ),
        metadataFilters = listOf(
            MetadataFilter(key = "category", value = "support", matchType = MetadataMatchType.EQUALS)
        ),
        sortBy = ThreadSortField.UPDATED_AT,
        sortDirection = SortDirection.DESC,
        limit = 10
    )
)
```

## 4. 统计和分析功能

### 4.1 获取消息统计信息

```kotlin
val stats = manager.getMessageStats(threadId)

println("总消息数: ${stats.totalMessages}")
println("按角色统计:")
stats.messagesByRole.forEach { (role, count) ->
    println("- $role: $count")
}
println("平均消息长度: ${stats.averageMessageLength}")
println("最早消息时间: ${stats.oldestMessageTime}")
println("最新消息时间: ${stats.newestMessageTime}")
println("按优先级统计:")
stats.messagesByPriority.forEach { (priority, count) ->
    println("- ${priority.value}: $count")
}
```

### 4.2 获取线程统计信息

```kotlin
val threadStats = manager.getThreadStats()

println("总线程数: ${threadStats.totalThreads}")
println("活跃线程数: ${threadStats.activeThreads}")
println("平均每个线程的消息数: ${threadStats.averageMessagesPerThread}")
println("最早线程时间: ${threadStats.oldestThreadTime}")
println("最新线程时间: ${threadStats.newestThreadTime}")
```

## 5. 导出和导入功能

### 5.1 导出线程

支持多种格式导出线程数据：

```kotlin
// 导出为 JSON
val jsonExport = manager.exportThread(threadId, ExportFormat.JSON)
File("thread_export.json").writeText(jsonExport)

// 导出为 CSV
val csvExport = manager.exportThread(threadId, ExportFormat.CSV)
File("thread_export.csv").writeText(csvExport)

// 导出为 Markdown
val markdownExport = manager.exportThread(threadId, ExportFormat.MARKDOWN)
File("thread_export.md").writeText(markdownExport)
```

### 5.2 导入线程

目前支持从 JSON 格式导入线程数据：

```kotlin
// 从 JSON 导入
val jsonData = File("thread_export.json").readText()
val newThreadId = manager.importThread(jsonData, ExportFormat.JSON)
```

## 6. 线程管理功能

### 6.1 合并线程

```kotlin
// 合并多个线程
val mergedThreadId = manager.mergeThreads(
    sourceThreadIds = listOf(threadId1, threadId2, threadId3),
    title = "合并的对话"
)

// 合并到现有线程
val targetThreadId = manager.mergeThreads(
    sourceThreadIds = listOf(threadId1, threadId2),
    targetThreadId = existingThreadId,
    title = null  // 保留现有线程的标题
)
```

### 6.2 分割线程

```kotlin
// 分割线程
val splitThreadIds = manager.splitThread(
    threadId = threadId,
    splitPoints = listOf(messageId1, messageId3),  // 在这些消息处分割
    newThreadTitles = listOf("第一部分", "第二部分", "第三部分")
)
```

## 7. 批量操作功能

### 7.1 批量更新消息

```kotlin
// 批量更新消息优先级
val updatedCount = manager.batchUpdateMessages(
    options = MessageBatchOptions(
        threadId = threadId,
        filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
    ),
    updates = MessageUpdateOptions(
        priority = MemoryPriority(0.9f),
        metadata = mapOf("reviewed" to true)
    )
)

// 批量更新特定消息
val updatedCount2 = manager.batchUpdateMessages(
    options = MessageBatchOptions(
        messageIds = listOf(messageId1, messageId2, messageId3)
    ),
    updates = MessageUpdateOptions(
        priority = MemoryPriority(0.5f)
    )
)
```

### 7.2 批量删除消息

```kotlin
// 批量删除消息
val deletedCount = manager.batchDeleteMessages(
    options = MessageBatchOptions(
        threadId = threadId,
        filters = listOf(
            ContentFilter(query = "spam", matchType = ContentMatchType.CONTAINS)
        )
    )
)

// 批量删除特定消息
val deletedCount2 = manager.batchDeleteMessages(
    options = MessageBatchOptions(
        messageIds = listOf(messageId1, messageId2)
    )
)
```

## 8. 与代理集成

记忆管理器可以与 KastraX 代理无缝集成：

```kotlin
// 创建带有记忆系统的代理
val myAgent = agent {
    name = "记忆助手"
    instructions = "你是一个有记忆能力的助手，能够记住之前的对话内容。"
    model = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
    }

    // 配置记忆系统
    memory = MemoryImpl(InMemoryStorage())
}

// 使用代理
val response = myAgent.generate("你好，我是张三。")

// 创建记忆管理器
val manager = MemoryManagerFactory.createManager(
    (myAgent.memory as MemoryImpl).storage
)

// 查询代理记忆中的用户消息
val userMessages = manager.queryMessages(
    threadId = response.threadId!!,
    options = MemoryQueryOptions(
        filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
    )
)
```

## 9. 最佳实践

### 9.1 查询优化

- 使用适当的过滤器组合缩小查询范围
- 使用适当的 `limit` 和 `offset` 进行分页，避免一次获取过多数据
- 对于频繁使用的查询，考虑缓存结果

### 9.2 批量操作

- 批量操作比单条操作更高效，特别是对于大量数据
- 批量删除操作不可撤销，请谨慎使用
- 考虑在非关键时段执行大规模批量操作，避免影响系统性能

### 9.3 线程管理

- 定期合并相关线程，保持对话的连贯性
- 分割过长的线程，提高查询和管理效率
- 为线程添加有意义的标题和元数据，便于后续查询和管理
