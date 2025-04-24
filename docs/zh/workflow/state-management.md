# 工作流状态管理和持久化

本文档详细介绍了 KastraX 工作流引擎中的状态管理和持久化机制，包括状态模型、持久化接口和使用方法。

## 1. 工作流状态模型

KastraX 工作流引擎支持以下工作流状态：

- **CREATED**：工作流已创建但尚未开始执行
- **RUNNING**：工作流正在执行中
- **COMPLETED**：工作流已成功完成
- **FAILED**：工作流执行失败
- **SUSPENDED**：工作流已暂停，可以恢复
- **WAITING**：工作流正在等待外部事件
- **CANCELED**：工作流已被取消

### 1.1 状态转换

工作流状态的转换遵循以下规则：

- CREATED -> RUNNING：工作流开始执行
- RUNNING -> COMPLETED：工作流成功完成
- RUNNING -> FAILED：工作流执行失败
- RUNNING -> SUSPENDED：工作流被暂停
- RUNNING -> WAITING：工作流等待外部事件
- RUNNING -> CANCELED：工作流被取消
- SUSPENDED -> RUNNING：工作流恢复执行
- WAITING -> RUNNING：工作流继续执行

## 2. 工作流状态持久化

KastraX 工作流引擎提供了多种持久化选项，支持将工作流状态持久化到不同的存储后端。

### 2.1 持久化接口

工作流状态持久化通过 `WorkflowStateStorage` 接口实现：

```kotlin
interface WorkflowStateStorage {
    suspend fun saveState(workflowId: String, state: WorkflowState): Boolean
    suspend fun loadState(workflowId: String): WorkflowState?
    suspend fun deleteState(workflowId: String): Boolean
    suspend fun listWorkflows(): List<String>
}
```

### 2.2 内置存储实现

KastraX 工作流引擎提供了以下内置存储实现：

- **InMemoryStateStorage**：将工作流状态存储在内存中，适用于开发和测试
- **FileStateStorage**：将工作流状态存储在文件系统中，适用于单机部署
- **DatabaseStateStorage**：将工作流状态存储在数据库中，适用于生产环境

### 2.3 自定义存储实现

开发者可以通过实现 `WorkflowStateStorage` 接口来创建自定义存储实现：

```kotlin
class CustomStateStorage : WorkflowStateStorage {
    override suspend fun saveState(workflowId: String, state: WorkflowState): Boolean {
        // 实现保存状态的逻辑
    }

    override suspend fun loadState(workflowId: String): WorkflowState? {
        // 实现加载状态的逻辑
    }

    override suspend fun deleteState(workflowId: String): Boolean {
        // 实现删除状态的逻辑
    }

    override suspend fun listWorkflows(): List<String> {
        // 实现列出工作流的逻辑
    }
}
```

## 3. 工作流暂停和恢复

KastraX 工作流引擎支持在任意点暂停和恢复工作流执行。

### 3.1 暂停工作流

可以通过以下方式暂停工作流：

```kotlin
// 暂停工作流
workflowEngine.suspendWorkflow(workflowId)

// 在步骤中暂停工作流
step {
    id = "my_step"
    execute { context ->
        // 执行逻辑
        if (shouldSuspend) {
            context.suspendWorkflow()
        }
        // 继续执行
    }
}
```

### 3.2 恢复工作流

可以通过以下方式恢复工作流：

```kotlin
// 恢复工作流
workflowEngine.resumeWorkflow(workflowId)
```

## 4. 长时间运行工作流

KastraX 工作流引擎支持长时间运行的工作流，包括断点续执行。

### 4.1 断点续执行

断点续执行允许工作流在系统重启后从上次执行的位置继续执行：

```kotlin
// 创建工作流引擎，配置持久化存储
val workflowEngine = WorkflowEngine.Builder()
    .withStateStorage(FileStateStorage("workflow-states"))
    .build()

// 执行工作流，支持断点续执行
workflowEngine.execute(workflow, input, options = ExecutionOptions(
    resumeOnRestart = true
))
```

### 4.2 超时和心跳

长时间运行的工作流可以配置超时和心跳机制：

```kotlin
// 配置工作流超时
workflow {
    name = "long-running-workflow"
    config = WorkflowConfig(
        timeout = Duration.ofHours(24),
        heartbeatInterval = Duration.ofMinutes(5)
    )
    // 步骤定义
}
```

## 5. 工作流状态查询和管理

KastraX 工作流引擎提供了丰富的 API 来查询和管理工作流状态。

### 5.1 查询工作流状态

```kotlin
// 查询工作流状态
val state = workflowEngine.getWorkflowState(workflowId)
println("Workflow state: ${state.status}")
println("Current step: ${state.currentStep}")
println("Completed steps: ${state.completedSteps}")
println("Failed steps: ${state.failedSteps}")
```

### 5.2 查询工作流执行历史

```kotlin
// 查询工作流执行历史
val history = workflowEngine.getWorkflowHistory(workflowId)
history.events.forEach { event ->
    println("${event.timestamp}: ${event.type} - ${event.details}")
}
```

### 5.3 管理工作流

```kotlin
// 取消工作流
workflowEngine.cancelWorkflow(workflowId)

// 重置工作流
workflowEngine.resetWorkflow(workflowId)

// 删除工作流
workflowEngine.deleteWorkflow(workflowId)
```

## 6. 最佳实践

### 6.1 选择合适的存储后端

- 开发和测试环境：使用 InMemoryStateStorage
- 单机部署：使用 FileStateStorage
- 生产环境：使用 DatabaseStateStorage

### 6.2 处理状态转换错误

```kotlin
try {
    workflowEngine.resumeWorkflow(workflowId)
} catch (e: IllegalStateTransitionException) {
    println("Cannot resume workflow: ${e.message}")
}
```

### 6.3 定期备份工作流状态

```kotlin
// 备份工作流状态
val stateStorage = workflowEngine.getStateStorage()
val workflowIds = stateStorage.listWorkflows()
workflowIds.forEach { workflowId ->
    val state = stateStorage.loadState(workflowId)
    if (state != null) {
        backupService.backup(workflowId, state)
    }
}
```

## 7. 示例

### 7.1 使用文件存储的长时间运行工作流

```kotlin
// 创建工作流引擎，配置文件存储
val workflowEngine = WorkflowEngine.Builder()
    .withStateStorage(FileStateStorage("workflow-states"))
    .build()

// 定义工作流
val longRunningWorkflow = workflow {
    name = "data-processing-workflow"
    description = "Process large amounts of data"
    
    config = WorkflowConfig(
        timeout = Duration.ofHours(24),
        heartbeatInterval = Duration.ofMinutes(5)
    )
    
    step(dataLoadingAgent) {
        id = "data_loading"
        name = "Data Loading"
        description = "Load data from source"
        variables = mapOf(
            "source" to variable("$.input.source")
        )
    }
    
    step(dataProcessingAgent) {
        id = "data_processing"
        name = "Data Processing"
        description = "Process loaded data"
        after("data_loading")
        variables = mapOf(
            "data" to variable("$.steps.data_loading.output.data")
        )
    }
    
    step(resultSavingAgent) {
        id = "result_saving"
        name = "Result Saving"
        description = "Save processing results"
        after("data_processing")
        variables = mapOf(
            "results" to variable("$.steps.data_processing.output.results"),
            "destination" to variable("$.input.destination")
        )
    }
}

// 执行工作流，支持断点续执行
val workflowId = workflowEngine.execute(longRunningWorkflow, mapOf(
    "source" to "s3://data-bucket/input",
    "destination" to "s3://result-bucket/output"
), options = ExecutionOptions(
    resumeOnRestart = true
))

// 查询工作流状态
val state = workflowEngine.getWorkflowState(workflowId)
println("Workflow state: ${state.status}")
```

## 8. 总结

KastraX 工作流引擎提供了强大的状态管理和持久化机制，支持工作流的暂停、恢复和断点续执行。通过灵活的存储接口，开发者可以选择适合自己需求的存储后端，实现工作流状态的可靠持久化。这些功能使 KastraX 工作流引擎能够支持各种复杂的工作流场景，包括长时间运行的工作流和需要高可靠性的生产环境工作流。
