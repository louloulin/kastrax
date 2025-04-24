# KastraX 基于状态机的工作流引擎测试文档

## 1. 概述

本文档详细介绍了 KastraX 基于状态机的工作流引擎的测试实现，包括测试类、测试方法和测试场景。这些测试确保状态机工作流引擎的各个组件正常工作，并满足预期的功能需求。

## 2. 测试类

### 2.1 SimpleStateMachineTest

`SimpleStateMachineTest` 类测试简单状态机的功能，包括状态转换、错误处理、暂停和恢复、等待和外部事件、取消等场景。

#### 2.1.1 测试方法

- `test state transitions()`：测试基本的状态转换功能
- `test error handling()`：测试错误处理功能
- `test suspend and resume()`：测试暂停和恢复功能
- `test wait and external event()`：测试等待和外部事件功能
- `test cancel()`：测试取消功能

#### 2.1.2 测试场景

```kotlin
@Test
fun `test state transitions`() {
    // 创建状态机ID
    val machineId = UUID.randomUUID().toString()
    
    // 初始化状态
    val input = mapOf("name" to "Test")
    stateMachine.initializeState(machineId, input)
    
    // 初始状态 -> 运行中
    var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
    assertEquals(WorkflowState.RUNNING, result.nextState)
    
    // 运行中 -> 完成
    val output = mapOf("greeting" to "Hello, Test!")
    result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Complete(output))
    assertEquals(WorkflowState.COMPLETED, result.nextState)
    assertEquals("Hello, Test!", result.output["greeting"])
    
    // 清理状态
    stateMachine.cleanupState(machineId)
}
```

## 3. 测试覆盖率

基于状态机的工作流引擎的测试覆盖了以下方面：

- **状态转换**：测试了所有主要的状态转换场景，包括初始状态到运行中、运行中到完成、运行中到失败等。
- **事件处理**：测试了所有类型的事件处理，包括开始、完成、失败、暂停、恢复、等待和取消事件。
- **数据管理**：测试了状态机在状态转换过程中的数据管理功能，包括输入数据、输出数据和错误信息的处理。
- **资源管理**：测试了状态机的资源管理功能，包括初始化状态和清理状态。

## 4. 测试场景详解

### 4.1 基本状态转换

基本状态转换测试验证了状态机能够正确地从一个状态转换到另一个状态，并在转换过程中处理数据。

```kotlin
@Test
fun `test state transitions`() {
    // 创建状态机ID
    val machineId = UUID.randomUUID().toString()
    
    // 初始化状态
    val input = mapOf("name" to "Test")
    stateMachine.initializeState(machineId, input)
    
    // 初始状态 -> 运行中
    var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
    assertEquals(WorkflowState.RUNNING, result.nextState)
    
    // 运行中 -> 完成
    val output = mapOf("greeting" to "Hello, Test!")
    result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Complete(output))
    assertEquals(WorkflowState.COMPLETED, result.nextState)
    assertEquals("Hello, Test!", result.output["greeting"])
    
    // 清理状态
    stateMachine.cleanupState(machineId)
}
```

### 4.2 错误处理

错误处理测试验证了状态机能够正确地处理错误情况，并将工作流转换为失败状态。

```kotlin
@Test
fun `test error handling`() {
    // 创建状态机ID
    val machineId = UUID.randomUUID().toString()
    
    // 初始化状态
    val input = mapOf("name" to "Test")
    stateMachine.initializeState(machineId, input)
    
    // 初始状态 -> 运行中
    var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
    assertEquals(WorkflowState.RUNNING, result.nextState)
    
    // 运行中 -> 失败
    result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Fail("Test error"))
    assertEquals(WorkflowState.FAILED, result.nextState)
    assertEquals("Test error", result.error)
    
    // 清理状态
    stateMachine.cleanupState(machineId)
}
```

### 4.3 暂停和恢复

暂停和恢复测试验证了状态机能够正确地暂停工作流执行，并在稍后恢复执行。

```kotlin
@Test
fun `test suspend and resume`() {
    // 创建状态机ID
    val machineId = UUID.randomUUID().toString()
    
    // 初始化状态
    val input = mapOf("name" to "Test")
    stateMachine.initializeState(machineId, input)
    
    // 初始状态 -> 运行中
    var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
    assertEquals(WorkflowState.RUNNING, result.nextState)
    
    // 运行中 -> 暂停
    result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Suspend("step1"))
    assertEquals(WorkflowState.SUSPENDED, result.nextState)
    
    // 暂停 -> 运行中
    val resumeInput = mapOf("approved" to true)
    result = stateMachine.transition(machineId, WorkflowState.SUSPENDED, WorkflowEvent.Resume("step1", resumeInput))
    assertEquals(WorkflowState.RUNNING, result.nextState)
    assertEquals(true, result.output["approved"])
    
    // 清理状态
    stateMachine.cleanupState(machineId)
}
```

### 4.4 等待和外部事件

等待和外部事件测试验证了状态机能够正确地等待外部事件，并在收到事件后恢复执行。

```kotlin
@Test
fun `test wait and external event`() {
    // 创建状态机ID
    val machineId = UUID.randomUUID().toString()
    
    // 初始化状态
    val input = mapOf("name" to "Test")
    stateMachine.initializeState(machineId, input)
    
    // 初始状态 -> 运行中
    var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
    assertEquals(WorkflowState.RUNNING, result.nextState)
    
    // 运行中 -> 等待
    result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Wait("step1"))
    assertEquals(WorkflowState.WAITING, result.nextState)
    
    // 等待 -> 运行中
    val externalInput = mapOf("data" to "Event data")
    result = stateMachine.transition(machineId, WorkflowState.WAITING, WorkflowEvent.Resume("step1", externalInput))
    assertEquals(WorkflowState.RUNNING, result.nextState)
    assertEquals("Event data", result.output["data"])
    
    // 清理状态
    stateMachine.cleanupState(machineId)
}
```

### 4.5 取消

取消测试验证了状态机能够正确地取消工作流执行。

```kotlin
@Test
fun `test cancel`() {
    // 创建状态机ID
    val machineId = UUID.randomUUID().toString()
    
    // 初始化状态
    val input = mapOf("name" to "Test")
    stateMachine.initializeState(machineId, input)
    
    // 初始状态 -> 运行中
    var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
    assertEquals(WorkflowState.RUNNING, result.nextState)
    
    // 运行中 -> 取消
    result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Cancel)
    assertEquals(WorkflowState.CANCELED, result.nextState)
    
    // 清理状态
    stateMachine.cleanupState(machineId)
}
```

## 5. 测试最佳实践

在编写基于状态机的工作流引擎的测试时，我们遵循了以下最佳实践：

- **独立性**：每个测试方法都是独立的，不依赖于其他测试方法的执行结果。
- **可重复性**：测试可以重复执行，并且每次执行都会产生相同的结果。
- **可读性**：测试方法名称清晰地描述了测试的内容，测试代码结构清晰，易于理解。
- **全面性**：测试覆盖了所有主要的功能和场景，确保状态机在各种情况下都能正常工作。
- **资源管理**：测试在执行完毕后会清理状态机资源，避免资源泄漏。

## 6. 测试运行

可以使用以下命令运行基于状态机的工作流引擎的测试：

```bash
./gradlew :kastrax-core:test --tests "ai.kastrax.core.workflow.statemachine.SimpleStateMachineTest"
```

## 7. 总结

KastraX 基于状态机的工作流引擎的测试确保了状态机的各个组件正常工作，并满足预期的功能需求。通过全面的测试，我们可以自信地使用状态机工作流引擎来构建复杂、可靠的工作流应用。
