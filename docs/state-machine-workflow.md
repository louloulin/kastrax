# KastraX 基于状态机的工作流引擎

## 1. 概述

KastraX 基于状态机的工作流引擎提供了一种灵活的方式来管理工作流的状态和转换。通过使用状态机模式，工作流可以在不同状态之间转换，响应各种事件，并在状态转换过程中执行相应的操作。这种方式使得工作流的状态管理更加清晰和可控，特别适合复杂的工作流场景。

## 2. 核心组件

### 2.1 WorkflowState

`WorkflowState` 枚举定义了工作流可能的状态：

- `INITIAL`：初始状态，工作流尚未开始执行
- `RUNNING`：运行中状态，工作流正在执行
- `COMPLETED`：已完成状态，工作流成功执行完毕
- `FAILED`：失败状态，工作流执行失败
- `SUSPENDED`：已暂停状态，工作流暂停执行，等待恢复
- `WAITING`：等待状态，工作流等待外部事件
- `CANCELED`：已取消状态，工作流被取消执行

### 2.2 WorkflowEvent

`WorkflowEvent` 密封类定义了可能触发状态转换的事件：

- `Start`：开始事件，触发工作流开始执行
- `Complete`：完成事件，表示工作流或步骤完成执行
- `Fail`：失败事件，表示工作流或步骤执行失败
- `Suspend`：暂停事件，触发工作流暂停执行
- `Resume`：恢复事件，触发工作流从暂停状态恢复执行
- `Wait`：等待事件，触发工作流进入等待状态
- `Cancel`：取消事件，触发工作流取消执行

### 2.3 StateTransitionResult

`StateTransitionResult` 数据类表示状态转换的结果：

- `nextState`：转换后的状态
- `output`：转换过程中产生的输出数据
- `error`：转换过程中可能发生的错误

### 2.4 SimpleStateMachine

`SimpleStateMachine` 类是状态机的核心实现，提供了以下功能：

- 初始化状态机状态和数据
- 执行状态转换
- 管理状态数据
- 清理状态机资源

## 3. 状态转换

状态转换是状态机的核心功能，它定义了工作流如何从一个状态转换到另一个状态。以下是主要的状态转换规则：

1. **初始状态 -> 运行中**：当收到 `Start` 事件时，工作流从初始状态转换为运行中状态。
2. **运行中 -> 完成**：当收到 `Complete` 事件时，工作流从运行中状态转换为完成状态。
3. **运行中 -> 失败**：当收到 `Fail` 事件时，工作流从运行中状态转换为失败状态。
4. **运行中 -> 暂停**：当收到 `Suspend` 事件时，工作流从运行中状态转换为暂停状态。
5. **运行中 -> 等待**：当收到 `Wait` 事件时，工作流从运行中状态转换为等待状态。
6. **运行中 -> 取消**：当收到 `Cancel` 事件时，工作流从运行中状态转换为取消状态。
7. **暂停 -> 运行中**：当收到 `Resume` 事件时，工作流从暂停状态转换为运行中状态。
8. **等待 -> 运行中**：当收到 `Resume` 事件时，工作流从等待状态转换为运行中状态。

## 4. 使用示例

### 4.1 创建和初始化状态机

```kotlin
// 创建状态机
val stateMachine = SimpleStateMachine()

// 创建状态机ID
val machineId = UUID.randomUUID().toString()

// 初始化状态
val input = mapOf("name" to "Test")
stateMachine.initializeState(machineId, input)
```

### 4.2 执行状态转换

```kotlin
// 初始状态 -> 运行中
var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
println("Current state: ${result.nextState}") // 输出：Current state: RUNNING

// 运行中 -> 完成
val output = mapOf("greeting" to "Hello, Test!")
result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Complete(output))
println("Current state: ${result.nextState}") // 输出：Current state: COMPLETED
println("Output: ${result.output}") // 输出：Output: {greeting=Hello, Test!}
```

### 4.3 处理错误

```kotlin
// 运行中 -> 失败
result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Fail("Test error"))
println("Current state: ${result.nextState}") // 输出：Current state: FAILED
println("Error: ${result.error}") // 输出：Error: Test error
```

### 4.4 暂停和恢复

```kotlin
// 运行中 -> 暂停
result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Suspend("step1"))
println("Current state: ${result.nextState}") // 输出：Current state: SUSPENDED

// 暂停 -> 运行中
val resumeInput = mapOf("approved" to true)
result = stateMachine.transition(machineId, WorkflowState.SUSPENDED, WorkflowEvent.Resume("step1", resumeInput))
println("Current state: ${result.nextState}") // 输出：Current state: RUNNING
println("Resume input: ${result.output}") // 输出：Resume input: {approved=true}
```

### 4.5 等待和外部事件

```kotlin
// 运行中 -> 等待
result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Wait("step1"))
println("Current state: ${result.nextState}") // 输出：Current state: WAITING

// 等待 -> 运行中
val externalInput = mapOf("data" to "Event data")
result = stateMachine.transition(machineId, WorkflowState.WAITING, WorkflowEvent.Resume("step1", externalInput))
println("Current state: ${result.nextState}") // 输出：Current state: RUNNING
println("External input: ${result.output}") // 输出：External input: {data=Event data}
```

### 4.6 取消工作流

```kotlin
// 运行中 -> 取消
result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Cancel)
println("Current state: ${result.nextState}") // 输出：Current state: CANCELED
```

### 4.7 清理资源

```kotlin
// 清理状态机资源
stateMachine.cleanupState(machineId)
```

## 5. 最佳实践

### 5.1 状态机ID管理

- 使用 UUID 或其他唯一标识符作为状态机ID
- 在多个组件之间共享状态机ID，以便协调状态转换
- 在不再需要状态机时，及时清理状态机资源

### 5.2 错误处理

- 在状态转换过程中捕获并处理异常
- 使用 `Fail` 事件将工作流转换为失败状态
- 在失败状态下，考虑是否需要重试或回滚操作

### 5.3 数据管理

- 在状态转换过程中，谨慎管理状态数据
- 避免在状态数据中存储大量数据，以免影响性能
- 考虑使用外部存储来持久化状态数据

## 6. 扩展和定制

### 6.1 自定义状态

可以通过扩展 `WorkflowState` 枚举来添加自定义状态：

```kotlin
enum class CustomWorkflowState {
    INITIAL,
    RUNNING,
    COMPLETED,
    FAILED,
    SUSPENDED,
    WAITING,
    CANCELED,
    REVIEWING, // 自定义状态：审核中
    APPROVED,  // 自定义状态：已批准
    REJECTED   // 自定义状态：已拒绝
}
```

### 6.2 自定义事件

可以通过扩展 `WorkflowEvent` 密封类来添加自定义事件：

```kotlin
sealed class CustomWorkflowEvent {
    object Start : CustomWorkflowEvent()
    data class Complete(val output: Map<String, Any?>) : CustomWorkflowEvent()
    data class Fail(val error: String) : CustomWorkflowEvent()
    data class Suspend(val stepId: String) : CustomWorkflowEvent()
    data class Resume(val stepId: String, val input: Map<String, Any?>) : CustomWorkflowEvent()
    data class Wait(val stepId: String) : CustomWorkflowEvent()
    object Cancel : CustomWorkflowEvent()
    data class Review(val reviewerId: String) : CustomWorkflowEvent() // 自定义事件：审核
    data class Approve(val approverId: String) : CustomWorkflowEvent() // 自定义事件：批准
    data class Reject(val rejecterId: String, val reason: String) : CustomWorkflowEvent() // 自定义事件：拒绝
}
```

### 6.3 自定义状态转换

可以通过扩展 `SimpleStateMachine` 类来实现自定义状态转换逻辑：

```kotlin
class CustomStateMachine : SimpleStateMachine() {
    override fun transition(machineId: String, currentState: WorkflowState, event: WorkflowEvent): StateTransitionResult {
        // 自定义状态转换逻辑
        return when {
            currentState == WorkflowState.RUNNING && event is WorkflowEvent.Wait -> {
                // 自定义等待状态转换逻辑
                StateTransitionResult(WorkflowState.WAITING, mapOf("waitReason" to "Custom wait reason"))
            }
            else -> {
                // 默认状态转换逻辑
                super.transition(machineId, currentState, event)
            }
        }
    }
}
```

## 7. 总结

KastraX 基于状态机的工作流引擎提供了一种灵活、可扩展的方式来管理工作流的状态和转换。通过使用状态机模式，工作流可以在不同状态之间转换，响应各种事件，并在状态转换过程中执行相应的操作。这种方式使得工作流的状态管理更加清晰和可控，特别适合复杂的工作流场景。

状态机工作流引擎的主要优势包括：

- **清晰的状态模型**：通过明确定义工作流的状态和转换规则，使得工作流的行为更加可预测和可理解。
- **灵活的事件处理**：通过事件驱动的方式触发状态转换，使得工作流可以响应各种外部事件和内部变化。
- **可扩展的设计**：通过自定义状态、事件和转换规则，可以轻松扩展状态机以满足特定需求。
- **简化的错误处理**：通过明确的错误状态和转换规则，简化了工作流的错误处理逻辑。

通过使用 KastraX 基于状态机的工作流引擎，开发者可以更轻松地构建复杂、可靠的工作流应用。
