# KastraX 工作流版本控制测试文档

## 1. 概述

本文档详细介绍了 KastraX 工作流版本控制系统的测试实现，包括测试类、测试方法和测试场景。这些测试确保工作流版本控制系统的各个组件正常工作，并满足预期的功能需求。

## 2. 测试类

### 2.1 WorkflowVersionManagerTest

`WorkflowVersionManagerTest` 类测试工作流版本管理器的功能，包括创建工作流、创建新版本、获取版本列表、设置活动版本等。

#### 2.1.1 测试方法

- `test create workflow()`: 测试创建新工作流
- `test create new version()`: 测试创建工作流新版本
- `test get workflow versions()`: 测试获取工作流版本列表
- `test get workflow version()`: 测试获取工作流特定版本
- `test get active workflow version()`: 测试获取工作流活动版本
- `test set active workflow version()`: 测试设置工作流活动版本
- `test delete workflow version()`: 测试删除工作流版本

#### 2.1.2 测试场景

```kotlin
@Test
fun `test create workflow`() {
    // 创建工作流
    val workflow = versionManager.createWorkflow(
        name = "Test Workflow",
        description = "Test workflow description",
        steps = listOf(
            WorkflowStep(
                id = "step1",
                name = "Step 1",
                type = "test",
                description = "Test step 1"
            ),
            WorkflowStep(
                id = "step2",
                name = "Step 2",
                type = "test",
                description = "Test step 2"
            )
        ),
        connections = listOf(
            StepConnection(
                sourceId = "step1",
                targetId = "step2"
            )
        ),
        metadata = mapOf("key" to "value")
    )
    
    // 验证工作流
    assertNotNull(workflow)
    assertEquals("Test Workflow", workflow.name)
    assertEquals("Test workflow description", workflow.description)
    assertEquals(2, workflow.steps.size)
    assertEquals(1, workflow.connections.size)
    assertEquals(1, workflow.metadata.size)
    assertEquals("1.0.0", workflow.version.version)
    assertTrue(workflow.version.isActive)
}
```

### 2.2 WorkflowDiffToolTest

`WorkflowDiffToolTest` 类测试工作流差异比较工具的功能，包括比较工作流、生成差异报告、格式化差异报告等。

#### 2.2.1 测试方法

- `test compare workflows with different IDs()`: 测试比较不同 ID 的工作流
- `test compare workflows with different versions()`: 测试比较不同版本的工作流
- `test compare workflows with different names()`: 测试比较不同名称的工作流
- `test compare workflows with different descriptions()`: 测试比较不同描述的工作流
- `test compare workflows with different metadata()`: 测试比较不同元数据的工作流
- `test compare workflows with different steps()`: 测试比较不同步骤的工作流
- `test compare workflows with modified steps()`: 测试比较修改步骤的工作流
- `test compare workflows with different connections()`: 测试比较不同连接的工作流
- `test format diff()`: 测试格式化差异报告

#### 2.2.2 测试场景

```kotlin
@Test
fun `test compare workflows with different steps`() {
    // 创建源工作流
    val sourceWorkflow = createTestWorkflow(
        "workflow1", "1.0.0",
        steps = listOf(
            WorkflowStep("step1", "Step 1", "type1", "Description 1"),
            WorkflowStep("step2", "Step 2", "type2", "Description 2")
        )
    )
    
    // 创建目标工作流，具有不同的步骤
    val targetWorkflow = createTestWorkflow(
        "workflow1", "1.0.0",
        steps = listOf(
            WorkflowStep("step1", "Step 1", "type1", "Description 1"),
            WorkflowStep("step3", "Step 3", "type3", "Description 3")
        )
    )
    
    // 比较工作流
    val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)
    
    // 验证差异
    assertNotNull(diff)
    assertTrue(diff.differences.any { it.type == DifferenceType.STEPS_REMOVED })
    assertTrue(diff.differences.any { it.type == DifferenceType.STEPS_ADDED })
}
```

### 2.3 CompatibilityCheckerTest

`CompatibilityCheckerTest` 类测试工作流兼容性检查器的功能，包括检查兼容性、生成兼容性报告、生成迁移计划等。

#### 2.3.1 测试方法

- `test check compatibility with compatible workflows()`: 测试检查兼容的工作流
- `test check compatibility with incompatible workflows - removed step()`: 测试检查不兼容的工作流（删除步骤）
- `test check compatibility with incompatible workflows - changed step type()`: 测试检查不兼容的工作流（更改步骤类型）
- `test check compatibility with incompatible workflows - removed connection()`: 测试检查不兼容的工作流（删除连接）
- `test generate migration plan for compatible workflows()`: 测试为兼容的工作流生成迁移计划
- `test generate migration plan for incompatible workflows()`: 测试为不兼容的工作流生成迁移计划
- `test apply migration plan()`: 测试应用迁移计划

#### 2.3.2 测试场景

```kotlin
@Test
fun `test check compatibility with compatible workflows`() {
    // 创建源工作流
    val sourceWorkflow = createTestWorkflow(
        "workflow1", "1.0.0",
        steps = listOf(
            WorkflowStep("step1", "Step 1", "type1", "Description 1"),
            WorkflowStep("step2", "Step 2", "type2", "Description 2")
        ),
        connections = listOf(
            StepConnection("step1", "step2")
        )
    )
    
    // 创建目标工作流，具有兼容的变更（添加步骤和连接）
    val targetWorkflow = createTestWorkflow(
        "workflow1", "1.1.0",
        steps = listOf(
            WorkflowStep("step1", "Step 1 Modified", "type1", "Description 1 Modified"),
            WorkflowStep("step2", "Step 2", "type2", "Description 2"),
            WorkflowStep("step3", "Step 3", "type3", "Description 3")
        ),
        connections = listOf(
            StepConnection("step1", "step2"),
            StepConnection("step2", "step3")
        )
    )
    
    // 检查兼容性
    val result = compatibilityChecker.checkCompatibility(sourceWorkflow, targetWorkflow)
    
    // 验证结果
    assertTrue(result.isCompatible)
    assertEquals(0, result.issues.size)
}
```

### 2.4 WorkflowRollbackTest

`WorkflowRollbackTest` 类测试工作流回滚功能，包括回滚到指定版本、回滚到前一版本、基于历史版本创建新版本等。

#### 2.4.1 测试方法

- `test rollback to previous version()`: 测试回滚到指定版本
- `test rollback to non-existent version()`: 测试回滚到不存在的版本
- `test rollback to previous version automatically()`: 测试自动回滚到前一版本
- `test create version from previous()`: 测试基于历史版本创建新版本
- `test create version from non-existent previous version()`: 测试基于不存在的历史版本创建新版本

#### 2.4.2 测试场景

```kotlin
@Test
fun `test rollback to previous version`() {
    // 创建初始工作流
    val initialWorkflow = createTestWorkflow(versionManager, "1.0.0")
    
    // 创建新版本
    val v110Workflow = versionManager.createNewVersion(
        workflowId = initialWorkflow.id,
        newVersion = "1.1.0",
        description = "Version 1.1.0",
        steps = initialWorkflow.steps.map { 
            if (it.id == "step1") {
                it.copy(name = "Modified Step 1")
            } else {
                it
            }
        }
    )
    
    val v200Workflow = versionManager.createNewVersion(
        workflowId = initialWorkflow.id,
        newVersion = "2.0.0",
        description = "Version 2.0.0",
        steps = initialWorkflow.steps + WorkflowStep(
            id = "step3",
            name = "Step 3",
            type = "test",
            description = "New step"
        ),
        setActive = true
    )
    
    // 验证活动版本是 2.0.0
    val activeVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
    assertEquals("2.0.0", activeVersion!!.version)
    
    // 回滚到版本 1.1.0
    val rolledBackWorkflow = rollback.rollbackToVersion(
        workflowId = initialWorkflow.id,
        targetVersion = "1.1.0",
        setActive = true
    )
    
    // 验证回滚
    assertNotNull(rolledBackWorkflow)
    assertEquals("1.1.0", rolledBackWorkflow!!.version.version)
    
    // 验证活动版本现在是 1.1.0
    val newActiveVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
    assertEquals("1.1.0", newActiveVersion!!.version)
    
    // 验证工作流内容
    assertEquals(2, rolledBackWorkflow.steps.size)
    val modifiedStep = rolledBackWorkflow.steps.find { it.id == "step1" }
    assertNotNull(modifiedStep)
    assertEquals("Modified Step 1", modifiedStep!!.name)
}
```

## 3. 测试覆盖率

工作流版本控制系统的测试覆盖了以下方面：

- **功能覆盖率**：测试覆盖了所有主要功能，包括版本创建、查询、切换、比较、迁移和回滚
- **场景覆盖率**：测试覆盖了常见场景和边缘情况，如兼容和不兼容的变更、不存在的版本等
- **组件覆盖率**：测试覆盖了所有核心组件，包括 WorkflowVersionManager、WorkflowDiffTool、CompatibilityChecker 和 WorkflowRollback

## 4. 测试最佳实践

在编写工作流版本控制系统的测试时，我们遵循了以下最佳实践：

- **独立性**：每个测试方法都是独立的，不依赖于其他测试方法的执行结果
- **可重复性**：测试可以重复执行，并且每次执行都会产生相同的结果
- **可读性**：测试方法名称清晰地描述了测试的内容，测试代码结构清晰，易于理解
- **全面性**：测试覆盖了正常情况和异常情况，确保系统在各种情况下都能正常工作
- **隔离性**：使用 InMemoryWorkflowVersionStorage 进行测试，避免对外部系统的依赖

## 5. 测试运行

可以使用以下命令运行工作流版本控制系统的测试：

```bash
./gradlew :kastrax-core:test --tests "ai.kastrax.core.workflow.version.*"
```

或者运行特定的测试类：

```bash
./gradlew :kastrax-core:test --tests "ai.kastrax.core.workflow.version.WorkflowVersionManagerTest"
./gradlew :kastrax-core:test --tests "ai.kastrax.core.workflow.version.WorkflowDiffToolTest"
./gradlew :kastrax-core:test --tests "ai.kastrax.core.workflow.version.CompatibilityCheckerTest"
./gradlew :kastrax-core:test --tests "ai.kastrax.core.workflow.version.WorkflowRollbackTest"
```

## 6. 总结

KastraX 工作流版本控制系统的测试确保了系统的各个组件正常工作，并满足预期的功能需求。通过全面的测试，我们可以自信地发布和使用工作流版本控制系统，为用户提供可靠的工作流版本管理功能。
