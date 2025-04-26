# KastraX 记忆查询和管理 API

KastraX 记忆查询和管理 API 提供了强大的记忆系统查询、统计、导出和批量操作功能，使开发者能够更灵活地管理和利用对话历史数据。

## 1. 概述

记忆查询和管理 API 扩展了基本的记忆系统，提供以下功能：

- **高级查询**：按角色、时间范围、内容、元数据和优先级等条件查询消息
- **统计分析**：获取消息统计信息，如总数、角色分布、平均优先级等
- **导出导入**：支持多种格式导出和导入对话线程
- **批量操作**：批量删除消息和更新消息优先级

## 2. 使用增强型记忆系统

### 2.1 创建增强型记忆实例

```kotlin
import ai.kastrax.memory.api.*
import ai.kastrax.memory.impl.*

// 创建增强型记忆系统
val memory = EnhancedMemoryFactory.createEnhancedMemory {
    storage(MemoryFactory.createInMemoryStorage())
    lastMessages(10)
    semanticRecall(true)
}
```

### 2.2 基本使用

增强型记忆系统完全兼容基本记忆系统的 API，可以像使用普通记忆系统一样使用它：

```kotlin
// 创建线程
val threadId = memory.createThread("示例对话")

// 保存消息
memory.saveMessage(message, threadId)

// 获取消息
val messages = memory.getMessages(threadId)
```

## 3. 高级查询功能

### 3.1 使用 MemoryQuery 查询消息

`MemoryQuery` 类提供了丰富的查询条件，可以组合使用：

```kotlin
// 按角色查询
val userMessages = memory.queryMessages(threadId, MemoryQuery(
    roles = listOf(MessageRole.USER),
    limit = 10
))

// 按内容查询
val aiMessages = memory.queryMessages(threadId, MemoryQuery(
    contentContains = "人工智能",
    limit = 10
))

// 按时间范围查询
val recentMessages = memory.queryMessages(threadId, MemoryQuery(
    timeRange = TimeRange(
        start = Clock.System.now().minus(1, DateTimeUnit.DAY),
        end = null
    ),
    limit = 10
))

// 按优先级查询
val highPriorityMessages = memory.queryMessages(threadId, MemoryQuery(
    priorityRange = PriorityRange(min = 0.7f),
    limit = 10
))

// 组合条件查询
val complexQuery = memory.queryMessages(threadId, MemoryQuery(
    roles = listOf(MessageRole.USER),
    contentContains = "人工智能",
    priorityRange = PriorityRange(min = 0.5f),
    sortBy = SortField.PRIORITY,
    sortDirection = SortDirection.DESC,
    limit = 10
))
```

### 3.2 查询条件说明

`MemoryQuery` 支持以下查询条件：

| 条件 | 类型 | 说明 |
|------|------|------|
| roles | List<MessageRole>? | 按消息角色过滤 |
| timeRange | TimeRange? | 按时间范围过滤 |
| contentContains | String? | 按内容包含关键词过滤 |
| metadata | Map<String, Any>? | 按元数据过滤 |
| priorityRange | PriorityRange? | 按优先级范围过滤 |
| limit | Int | 返回结果数量限制 |
| offset | Int | 分页偏移量 |
| sortBy | SortField | 排序字段 |
| sortDirection | SortDirection | 排序方向 |

## 4. 统计和分析功能

### 4.1 获取消息统计信息

```kotlin
val stats = memory.getMessageStats(threadId)

println("总消息数: ${stats.totalMessages}")
println("按角色统计:")
stats.messagesByRole.forEach { (role, count) ->
    println("- $role: $count")
}
println("平均优先级: ${stats.averagePriority}")
println("最早消息时间: ${stats.oldestMessage}")
println("最新消息时间: ${stats.newestMessage}")
```

### 4.2 统计信息说明

`MessageStats` 包含以下统计信息：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalMessages | Int | 总消息数 |
| messagesByRole | Map<MessageRole, Int> | 按角色统计的消息数量 |
| averagePriority | Float | 平均优先级 |
| oldestMessage | Instant? | 最早消息时间 |
| newestMessage | Instant? | 最新消息时间 |
| mostAccessedMessage | String? | 访问次数最多的消息ID |

## 5. 导出和导入功能

### 5.1 导出线程

支持多种格式导出线程数据：

```kotlin
// 导出为 JSON
val jsonExport = memory.exportThread(threadId, ExportFormat.JSON)
File("thread_export.json").writeText(jsonExport)

// 导出为 CSV
val csvExport = memory.exportThread(threadId, ExportFormat.CSV)
File("thread_export.csv").writeText(csvExport)

// 导出为 Markdown
val markdownExport = memory.exportThread(threadId, ExportFormat.MARKDOWN)
File("thread_export.md").writeText(markdownExport)
```

### 5.2 导入线程

目前支持从 JSON 格式导入线程数据：

```kotlin
// 从 JSON 导入
val jsonData = File("thread_export.json").readText()
val newThreadId = memory.importThread(jsonData, ExportFormat.JSON)
```

## 6. 批量操作功能

### 6.1 批量删除消息

```kotlin
// 批量删除消息
val messageIds = listOf("message-id-1", "message-id-2", "message-id-3")
val deletedCount = memory.batchDeleteMessages(messageIds)
println("已删除 $deletedCount 条消息")
```

### 6.2 批量更新消息优先级

```kotlin
// 批量更新消息优先级
val updates = mapOf(
    "message-id-1" to MemoryPriority(0.9f),
    "message-id-2" to MemoryPriority(0.8f),
    "message-id-3" to MemoryPriority(0.7f)
)
val updatedCount = memory.batchUpdatePriorities(updates)
println("已更新 $updatedCount 条消息的优先级")
```

## 7. 与代理集成

增强型记忆系统可以与 KastraX 代理无缝集成：

```kotlin
// 创建带有增强型记忆系统的代理
val myAgent = agent {
    name = "记忆助手"
    instructions = "你是一个有记忆能力的助手，能够记住之前的对话内容。"
    model = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
    }

    // 配置增强型记忆系统
    memory = EnhancedMemoryFactory.createEnhancedMemory {
        storage(MemoryFactory.createInMemoryStorage())
        lastMessages(10)
        semanticRecall(true)
    }
}

// 使用代理
val response = myAgent.generate("你好，我是张三。")

// 查询代理记忆中的用户消息
val userMessages = (myAgent.memory as? MemoryWithQueryAPI)?.queryMessages(
    threadId = response.threadId!!,
    query = MemoryQuery(roles = listOf(MessageRole.USER))
) ?: emptyList()
```

## 8. 最佳实践

### 8.1 查询优化

- 使用适当的 `limit` 和 `offset` 进行分页，避免一次获取过多数据
- 组合使用多个查询条件，缩小结果范围
- 对于频繁使用的查询，考虑缓存结果

### 8.2 批量操作

- 批量操作比单条操作更高效，特别是对于大量数据
- 批量删除操作不可撤销，请谨慎使用
- 考虑在非关键时段执行大规模批量操作，避免影响系统性能

### 8.3 导出导入

- 定期导出重要对话线程作为备份
- JSON 格式保留了最完整的信息，适合用于备份和恢复
- CSV 格式适合数据分析和与其他系统集成
- Markdown 格式适合人工阅读和文档化
