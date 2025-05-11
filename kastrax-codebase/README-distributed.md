# 分布式索引系统

本模块实现了基于 Actor 模型的分布式索引系统，用于高效处理大量文件的索引任务。

## 功能特点

- **基于 Actor 模型**：使用 kactor 实现分布式架构，支持高并发和容错
- **任务优先级调度**：根据任务类型和重要性分配处理资源
- **并行处理**：支持并行处理多个索引任务，提高处理效率
- **批量处理**：优化大量文件的处理效率
- **状态跟踪与恢复**：跟踪任务处理状态，支持失败任务的重试和恢复

## 架构设计

分布式索引系统由以下组件组成：

1. **索引协调者 (IndexCoordinatorActor)**：
   - 负责任务分配和工作者管理
   - 维护任务队列和状态
   - 处理工作者注册和注销
   - 提供系统状态监控

2. **索引工作者 (IndexTaskActor)**：
   - 处理具体的索引任务
   - 支持任务重试和超时处理
   - 向协调者报告任务状态

3. **分布式索引系统 (DistributedIndexSystem)**：
   - 提供统一的接口
   - 管理 Actor 系统的生命周期
   - 提供任务提交和状态查询功能

4. **工厂类 (DistributedIndexSystemFactory)**：
   - 简化系统创建和配置
   - 提供默认配置

## 使用示例

```kotlin
// 创建 Actor 系统
val actorSystem = ActorSystem("distributed-index-example")

// 创建嵌入服务和文档存储
val embeddingService = FastEmbeddingService.create()
val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
val documentStore = DocumentVectorStoreAdapter(
    vectorStore = vectorStore,
    indexName = "distributed_index_example",
    dimension = embeddingService.dimension
)

// 创建分布式索引系统
val indexSystem = DistributedIndexSystemFactory.createDistributedIndexSystem(
    documentStore = documentStore,
    embeddingService = embeddingService,
    rootPath = directoryPath,
    actorSystem = actorSystem
)

// 启动系统
indexSystem.start()

// 提交任务
val task = IndexTask(
    type = IndexTaskType.FULL_REINDEX,
    path = directoryPath
)
indexSystem.submitTask(task)

// 获取系统状态
val status = indexSystem.getStatus()
println("待处理任务数量: ${status.pendingTaskCount}")
println("活动任务数量: ${status.activeTaskCount}")
println("已完成任务数量: ${status.completedTaskCount}")
println("失败任务数量: ${status.failedTaskCount}")
println("工作者数量: ${status.workerCount}")

// 监听事件
indexSystem.getEventFlow().collect { event ->
    when (event) {
        is IndexCoordinatorEvent.TaskSubmitted -> 
            println("任务已提交: ${event.taskId}")
        is IndexCoordinatorEvent.TaskAssigned -> 
            println("任务已分配: ${event.taskId} 到工作者: ${event.workerId}")
        is IndexCoordinatorEvent.TaskCompleted -> 
            println("任务已完成: ${event.taskId} 由工作者: ${event.workerId}")
        is IndexCoordinatorEvent.TaskFailed -> 
            println("任务失败: ${event.taskId} 由工作者: ${event.workerId}, 错误: ${event.error}")
    }
}

// 关闭系统
indexSystem.stop()
actorSystem.shutdown()
```

## 配置选项

### 协调者配置

```kotlin
val coordinatorConfig = IndexCoordinatorConfig(
    maxPendingTasks = 10000,
    taskAssignmentInterval = Duration.seconds(1),
    workerStatusCheckInterval = Duration.seconds(10),
    initialWorkerCount = Runtime.getRuntime().availableProcessors()
)
```

### 工作者配置

```kotlin
val taskActorConfig = IndexTaskActorConfig(
    taskTimeout = Duration.seconds(60),
    maxRetries = 3
)
```

## 性能优化

分布式索引系统通过以下方式优化性能：

1. **并行处理**：使用多个工作者并行处理任务
2. **任务优先级**：优先处理重要任务，如分支变更
3. **批量处理**：批量提交和处理任务，减少通信开销
4. **状态监控**：实时监控系统状态，动态调整资源分配

## 容错机制

系统提供以下容错机制：

1. **任务重试**：失败任务自动重试，最多重试 3 次
2. **超时处理**：任务超时自动取消并重试
3. **工作者监控**：监控工作者状态，自动重启失败的工作者
4. **任务重分配**：工作者失败时，自动重分配其任务

## 扩展性

系统设计考虑了扩展性：

1. **可配置工作者数量**：根据系统资源调整工作者数量
2. **可替换组件**：索引处理器、嵌入服务和文档存储都可以替换
3. **事件驱动**：通过事件流支持外部系统集成
4. **可定制配置**：提供丰富的配置选项
