# 工作流事件和回调机制

本文档详细介绍了 KastraX 工作流引擎中的事件和回调机制，包括事件类型、事件总线、回调注册和使用方法。

## 1. 事件系统概述

KastraX 工作流引擎的事件系统允许开发者监听工作流执行过程中的各种事件，并在事件发生时执行自定义逻辑。事件系统基于发布-订阅模式，通过事件总线实现事件的发布和订阅。

## 2. 工作流事件类型

KastraX 工作流引擎支持以下工作流事件类型：

### 2.1 工作流级别事件

- **WorkflowStartedEvent**：工作流开始执行
- **WorkflowCompletedEvent**：工作流成功完成
- **WorkflowFailedEvent**：工作流执行失败
- **WorkflowSuspendedEvent**：工作流被暂停
- **WorkflowResumedEvent**：工作流恢复执行
- **WorkflowCanceledEvent**：工作流被取消

### 2.2 步骤级别事件

- **StepStartedEvent**：步骤开始执行
- **StepCompletedEvent**：步骤成功完成
- **StepFailedEvent**：步骤执行失败
- **StepSkippedEvent**：步骤被跳过
- **StepRetryingEvent**：步骤正在重试

### 2.3 数据级别事件

- **VariableSetEvent**：变量被设置
- **VariableAccessedEvent**：变量被访问
- **DataTransformEvent**：数据转换事件

## 3. 事件总线

KastraX 工作流引擎使用事件总线来管理事件的发布和订阅。事件总线是一个中心化的组件，负责将事件从发布者传递给订阅者。

### 3.1 事件总线接口

```kotlin
interface WorkflowEventBus {
    fun publish(event: WorkflowEvent)
    fun subscribe(eventType: Class<out WorkflowEvent>, handler: EventHandler<WorkflowEvent>)
    fun unsubscribe(eventType: Class<out WorkflowEvent>, handler: EventHandler<WorkflowEvent>)
}
```

### 3.2 事件处理器接口

```kotlin
interface EventHandler<T : WorkflowEvent> {
    fun handle(event: T)
}
```

## 4. 回调机制

KastraX 工作流引擎提供了回调机制，允许开发者在工作流执行的不同阶段执行自定义逻辑。

### 4.1 回调接口

```kotlin
interface WorkflowCallback {
    fun onWorkflowStarted(workflowId: String, input: Map<String, Any?>)
    fun onWorkflowCompleted(workflowId: String, output: Map<String, Any?>)
    fun onWorkflowFailed(workflowId: String, error: Throwable)
    fun onStepStarted(workflowId: String, stepId: String)
    fun onStepCompleted(workflowId: String, stepId: String, output: Map<String, Any?>)
    fun onStepFailed(workflowId: String, stepId: String, error: Throwable)
}
```

### 4.2 回调适配器

为了简化回调的使用，KastraX 工作流引擎提供了回调适配器，允许开发者只实现感兴趣的回调方法：

```kotlin
abstract class WorkflowCallbackAdapter : WorkflowCallback {
    override fun onWorkflowStarted(workflowId: String, input: Map<String, Any?>) {}
    override fun onWorkflowCompleted(workflowId: String, output: Map<String, Any?>) {}
    override fun onWorkflowFailed(workflowId: String, error: Throwable) {}
    override fun onStepStarted(workflowId: String, stepId: String) {}
    override fun onStepCompleted(workflowId: String, stepId: String, output: Map<String, Any?>) {}
    override fun onStepFailed(workflowId: String, stepId: String, error: Throwable) {}
}
```

## 5. 使用事件和回调

### 5.1 订阅事件

```kotlin
// 创建事件处理器
val workflowCompletedHandler = object : EventHandler<WorkflowCompletedEvent> {
    override fun handle(event: WorkflowCompletedEvent) {
        println("Workflow ${event.workflowId} completed with output: ${event.output}")
    }
}

// 订阅事件
workflowEngine.eventBus.subscribe(WorkflowCompletedEvent::class.java, workflowCompletedHandler)
```

### 5.2 注册回调

```kotlin
// 创建回调
val workflowCallback = object : WorkflowCallbackAdapter() {
    override fun onWorkflowCompleted(workflowId: String, output: Map<String, Any?>) {
        println("Workflow $workflowId completed with output: $output")
    }
    
    override fun onStepFailed(workflowId: String, stepId: String, error: Throwable) {
        println("Step $stepId in workflow $workflowId failed: ${error.message}")
    }
}

// 注册回调
workflowEngine.registerCallback(workflowCallback)
```

### 5.3 在工作流定义中使用回调

```kotlin
workflow {
    name = "workflow-with-callbacks"
    
    // 工作流级别回调
    onStart { input ->
        println("Workflow started with input: $input")
    }
    
    onComplete { output ->
        println("Workflow completed with output: $output")
    }
    
    onError { error ->
        println("Workflow failed: ${error.message}")
    }
    
    // 步骤定义
    step(myAgent) {
        id = "my_step"
        
        // 步骤级别回调
        onStart {
            println("Step started")
        }
        
        onComplete { output ->
            println("Step completed with output: $output")
        }
        
        onError { error ->
            println("Step failed: ${error.message}")
        }
    }
}
```

## 6. 高级事件处理

### 6.1 事件过滤

```kotlin
// 创建事件过滤器
val stepCompletedFilter = object : EventFilter<StepCompletedEvent> {
    override fun filter(event: StepCompletedEvent): Boolean {
        return event.stepId == "important_step"
    }
}

// 创建事件处理器
val filteredStepCompletedHandler = object : EventHandler<StepCompletedEvent> {
    override fun handle(event: StepCompletedEvent) {
        println("Important step completed with output: ${event.output}")
    }
}

// 订阅带过滤器的事件
workflowEngine.eventBus.subscribe(
    StepCompletedEvent::class.java,
    filteredStepCompletedHandler,
    stepCompletedFilter
)
```

### 6.2 异步事件处理

```kotlin
// 创建异步事件处理器
val asyncWorkflowCompletedHandler = object : AsyncEventHandler<WorkflowCompletedEvent> {
    override suspend fun handleAsync(event: WorkflowCompletedEvent) {
        // 执行耗时操作，如发送通知或保存数据
        notificationService.sendCompletionNotification(event.workflowId, event.output)
        analyticsService.recordWorkflowCompletion(event.workflowId, event.executionTime)
    }
}

// 订阅异步事件
workflowEngine.eventBus.subscribeAsync(WorkflowCompletedEvent::class.java, asyncWorkflowCompletedHandler)
```

### 6.3 事件链

```kotlin
// 创建事件链
val workflowCompletionChain = EventChain<WorkflowCompletedEvent>()
    .addHandler { event -> println("Handler 1: Workflow ${event.workflowId} completed") }
    .addHandler { event -> notificationService.sendCompletionNotification(event.workflowId) }
    .addHandler { event -> analyticsService.recordWorkflowCompletion(event.workflowId) }

// 订阅事件链
workflowEngine.eventBus.subscribe(WorkflowCompletedEvent::class.java, workflowCompletionChain)
```

## 7. 自定义事件

KastraX 工作流引擎允许开发者定义和发布自定义事件。

### 7.1 定义自定义事件

```kotlin
// 定义自定义事件
data class DataProcessingCompletedEvent(
    val workflowId: String,
    val stepId: String,
    val recordsProcessed: Int,
    val processingTime: Duration
) : WorkflowEvent
```

### 7.2 发布自定义事件

```kotlin
// 在步骤中发布自定义事件
step {
    id = "data_processing"
    execute { context ->
        val startTime = System.currentTimeMillis()
        
        // 执行数据处理
        val records = processData(context.getVariable("data"))
        
        val endTime = System.currentTimeMillis()
        val processingTime = Duration.ofMillis(endTime - startTime)
        
        // 发布自定义事件
        context.eventBus.publish(DataProcessingCompletedEvent(
            workflowId = context.workflowId,
            stepId = id,
            recordsProcessed = records.size,
            processingTime = processingTime
        ))
        
        // 返回结果
        WorkflowStepResult.success(id, mapOf("records" to records))
    }
}
```

### 7.3 订阅自定义事件

```kotlin
// 订阅自定义事件
workflowEngine.eventBus.subscribe(DataProcessingCompletedEvent::class.java) { event ->
    println("Data processing completed: ${event.recordsProcessed} records in ${event.processingTime.toMillis()} ms")
    metricsService.recordProcessingMetrics(event.workflowId, event.recordsProcessed, event.processingTime)
}
```

## 8. 事件持久化

KastraX 工作流引擎支持事件持久化，允许将事件保存到存储后端，用于审计、分析和故障排查。

### 8.1 事件存储接口

```kotlin
interface EventStorage {
    suspend fun saveEvent(event: WorkflowEvent): Boolean
    suspend fun getEvents(workflowId: String): List<WorkflowEvent>
    suspend fun getEvents(workflowId: String, eventType: Class<out WorkflowEvent>): List<WorkflowEvent>
    suspend fun getEvents(workflowId: String, timeRange: ClosedRange<Instant>): List<WorkflowEvent>
}
```

### 8.2 配置事件持久化

```kotlin
// 创建工作流引擎，配置事件存储
val workflowEngine = WorkflowEngine.Builder()
    .withEventStorage(DatabaseEventStorage(dataSource))
    .withEventPersistenceEnabled(true)
    .build()
```

### 8.3 查询事件历史

```kotlin
// 查询工作流事件历史
val events = workflowEngine.getEventStorage().getEvents(workflowId)
events.forEach { event ->
    println("${event.timestamp}: ${event.javaClass.simpleName}")
}

// 查询特定类型的事件
val stepFailedEvents = workflowEngine.getEventStorage().getEvents(workflowId, StepFailedEvent::class.java)
stepFailedEvents.forEach { event ->
    println("Step ${event.stepId} failed at ${event.timestamp}: ${event.error.message}")
}
```

## 9. 最佳实践

### 9.1 事件处理性能

- 避免在事件处理器中执行耗时操作，使用异步事件处理器代替
- 使用事件过滤器减少不必要的事件处理
- 考虑使用事件批处理来提高性能

### 9.2 回调异常处理

```kotlin
// 创建带异常处理的回调
val safeCallback = object : WorkflowCallbackAdapter() {
    override fun onWorkflowCompleted(workflowId: String, output: Map<String, Any?>) {
        try {
            // 执行回调逻辑
            notificationService.sendCompletionNotification(workflowId, output)
        } catch (e: Exception) {
            // 处理异常
            logger.error("Failed to send completion notification", e)
        }
    }
}
```

### 9.3 避免回调循环

避免在回调中触发可能导致同一回调再次被调用的操作，以防止无限循环。

## 10. 示例

### 10.1 监控工作流执行时间

```kotlin
// 创建工作流执行时间监控器
class WorkflowExecutionTimeMonitor(private val metricsService: MetricsService) : WorkflowCallbackAdapter() {
    private val workflowStartTimes = mutableMapOf<String, Long>()
    
    override fun onWorkflowStarted(workflowId: String, input: Map<String, Any?>) {
        workflowStartTimes[workflowId] = System.currentTimeMillis()
    }
    
    override fun onWorkflowCompleted(workflowId: String, output: Map<String, Any?>) {
        val startTime = workflowStartTimes.remove(workflowId) ?: return
        val executionTime = System.currentTimeMillis() - startTime
        
        // 记录执行时间指标
        metricsService.recordWorkflowExecutionTime(workflowId, executionTime)
        
        // 如果执行时间超过阈值，发送警报
        if (executionTime > 60000) { // 1分钟
            alertService.sendAlert("Workflow $workflowId took too long to execute: ${executionTime}ms")
        }
    }
}

// 注册监控器
workflowEngine.registerCallback(WorkflowExecutionTimeMonitor(metricsService))
```

### 10.2 工作流执行通知系统

```kotlin
// 创建工作流执行通知系统
class WorkflowNotificationSystem(private val notificationService: NotificationService) {
    fun initialize(workflowEngine: WorkflowEngine) {
        // 订阅工作流完成事件
        workflowEngine.eventBus.subscribe(WorkflowCompletedEvent::class.java) { event ->
            notificationService.sendNotification(
                recipient = "admin@example.com",
                subject = "Workflow ${event.workflowId} completed",
                message = "Workflow ${event.workflowId} completed successfully with output: ${event.output}"
            )
        }
        
        // 订阅工作流失败事件
        workflowEngine.eventBus.subscribe(WorkflowFailedEvent::class.java) { event ->
            notificationService.sendNotification(
                recipient = "admin@example.com",
                subject = "Workflow ${event.workflowId} failed",
                message = "Workflow ${event.workflowId} failed with error: ${event.error.message}"
            )
        }
        
        // 订阅步骤失败事件，但只关注特定步骤
        workflowEngine.eventBus.subscribe(StepFailedEvent::class.java, object : EventHandler<StepFailedEvent> {
            override fun handle(event: StepFailedEvent) {
                if (event.stepId in setOf("critical_step_1", "critical_step_2")) {
                    notificationService.sendNotification(
                        recipient = "admin@example.com",
                        subject = "Critical step ${event.stepId} failed",
                        message = "Critical step ${event.stepId} in workflow ${event.workflowId} failed with error: ${event.error.message}"
                    )
                }
            }
        })
    }
}

// 初始化通知系统
val notificationSystem = WorkflowNotificationSystem(notificationService)
notificationSystem.initialize(workflowEngine)
```

## 11. 总结

KastraX 工作流引擎提供了强大的事件和回调机制，允许开发者监听工作流执行过程中的各种事件，并在事件发生时执行自定义逻辑。通过事件总线，开发者可以订阅感兴趣的事件，实现工作流执行的监控、通知和集成。回调机制则提供了更简单的方式来响应工作流执行的关键阶段，如开始、完成和失败。这些功能使 KastraX 工作流引擎能够支持各种复杂的工作流场景，提高工作流系统的可观察性和可扩展性。
