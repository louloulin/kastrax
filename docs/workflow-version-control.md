# KastraX 工作流版本控制

## 1. 概述

KastraX 工作流版本控制系统提供了完整的工作流版本管理功能，包括版本创建、查询、切换、比较、迁移和回滚。本文档详细介绍了工作流版本控制系统的设计、实现和使用方法。

## 2. 核心组件

### 2.1 WorkflowVersion

`WorkflowVersion` 类表示工作流的一个版本，包含以下属性：

- `workflowId`: 工作流 ID
- `version`: 版本号（语义化版本）
- `description`: 版本描述
- `createdAt`: 创建时间
- `createdBy`: 创建者
- `isActive`: 是否为活动版本

### 2.2 VersionedWorkflow

`VersionedWorkflow` 类将工作流与版本信息关联，包含以下属性：

- `id`: 工作流 ID
- `name`: 工作流名称
- `description`: 工作流描述
- `version`: 工作流版本
- `steps`: 工作流步骤列表
- `connections`: 工作流连接列表
- `metadata`: 工作流元数据

### 2.3 WorkflowVersionManager

`WorkflowVersionManager` 类提供工作流版本管理功能，包括：

- 创建新工作流
- 创建工作流新版本
- 获取工作流版本列表
- 获取工作流特定版本
- 获取工作流活动版本
- 设置工作流活动版本
- 删除工作流版本

### 2.4 WorkflowVersionStorage

`WorkflowVersionStorage` 接口定义了工作流版本存储的基本操作，包括：

- 保存工作流版本
- 获取工作流版本
- 获取工作流所有版本
- 删除工作流版本
- 设置活动版本

KastraX 提供了 `InMemoryWorkflowVersionStorage` 实现，用于内存中存储工作流版本。

### 2.5 WorkflowDiffTool

`WorkflowDiffTool` 类提供工作流版本差异比较功能，包括：

- 比较两个工作流版本
- 生成差异报告
- 格式化差异报告

### 2.6 CompatibilityChecker

`CompatibilityChecker` 类提供工作流版本兼容性检查功能，包括：

- 检查两个工作流版本的兼容性
- 生成兼容性报告
- 生成迁移计划

### 2.7 WorkflowRollback

`WorkflowRollback` 类提供工作流版本回滚功能，包括：

- 回滚到指定版本
- 回滚到前一版本
- 基于历史版本创建新版本

## 3. 使用示例

### 3.1 创建工作流和版本

```kotlin
// 创建版本管理器
val versionStorage = InMemoryWorkflowVersionStorage()
val versionManager = WorkflowVersionManager(versionStorage)

// 创建工作流
val workflow = versionManager.createWorkflow(
    name = "示例工作流",
    description = "这是一个示例工作流",
    steps = listOf(
        WorkflowStep(
            id = "step1",
            name = "步骤 1",
            type = "test",
            description = "测试步骤 1"
        ),
        WorkflowStep(
            id = "step2",
            name = "步骤 2",
            type = "test",
            description = "测试步骤 2"
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

// 创建新版本
val newVersion = versionManager.createNewVersion(
    workflowId = workflow.id,
    newVersion = "1.1.0",
    description = "添加了步骤 3",
    steps = workflow.steps + WorkflowStep(
        id = "step3",
        name = "步骤 3",
        type = "test",
        description = "测试步骤 3"
    ),
    connections = workflow.connections + StepConnection(
        sourceId = "step2",
        targetId = "step3"
    ),
    setActive = true
)
```

### 3.2 比较工作流版本

```kotlin
// 创建差异比较工具
val diffTool = WorkflowDiffTool()

// 比较两个版本
val diff = diffTool.compareWorkflows(workflow, newVersion)

// 格式化差异报告
val formattedDiff = diffTool.formatDiff(diff)
println(formattedDiff)
```

### 3.3 检查兼容性和生成迁移计划

```kotlin
// 创建兼容性检查器
val compatibilityChecker = CompatibilityChecker()

// 检查兼容性
val compatibilityResult = compatibilityChecker.checkCompatibility(workflow, newVersion)
if (compatibilityResult.isCompatible) {
    println("工作流版本兼容")
} else {
    println("工作流版本不兼容：")
    compatibilityResult.issues.forEach { println("- ${it.type}: ${it.description}") }
}

// 生成迁移计划
val migrationPlan = compatibilityChecker.generateMigrationPlan(workflow, newVersion)
if (migrationPlan != null) {
    println("迁移计划生成成功")
    // 应用迁移计划
    val migratedWorkflow = migrationPlan.migration.apply(workflow)
    println("工作流迁移成功")
}
```

### 3.4 回滚工作流版本

```kotlin
// 创建回滚工具
val rollback = WorkflowRollback(versionManager)

// 回滚到指定版本
val rolledBackWorkflow = rollback.rollbackToVersion(
    workflowId = workflow.id,
    targetVersion = "1.0.0",
    setActive = true
)

// 回滚到前一版本
val previousVersionWorkflow = rollback.rollbackToPreviousVersion(
    workflowId = workflow.id,
    setActive = true
)

// 基于历史版本创建新版本
val newVersionFromPrevious = rollback.createVersionFromPrevious(
    workflowId = workflow.id,
    sourceVersion = "1.0.0",
    newVersion = "1.0.1",
    description = "基于 1.0.0 版本创建的新版本",
    createdBy = "user",
    setActive = true
)
```

## 4. 最佳实践

### 4.1 版本命名

建议使用语义化版本号（SemVer）命名工作流版本，格式为 `主版本号.次版本号.修订号`：

- **主版本号**：当做了不兼容的 API 修改时递增
- **次版本号**：当做了向下兼容的功能性新增时递增
- **修订号**：当做了向下兼容的问题修正时递增

### 4.2 版本管理策略

- 为每个重要的工作流变更创建新版本
- 在版本描述中详细说明变更内容
- 在进行重大变更前，先检查兼容性
- 保留关键版本，以便在需要时回滚
- 定期清理不再需要的历史版本

### 4.3 迁移和兼容性

- 尽量保持向后兼容性，避免破坏性变更
- 对于不兼容的变更，使用迁移计划进行平滑过渡
- 在测试环境中验证迁移计划，确保迁移成功
- 记录迁移过程，以便在出现问题时进行回滚

## 5. 常见问题

### 5.1 如何处理版本冲突？

当多个用户同时修改同一工作流时，可能会出现版本冲突。建议采用以下策略处理冲突：

1. 使用乐观锁机制，在保存版本时检查是否有其他用户修改了同一版本
2. 如果发现冲突，提示用户解决冲突或创建新版本
3. 使用差异比较工具帮助用户理解冲突并解决冲突

### 5.2 如何管理大量工作流版本？

当工作流版本数量变得很大时，可能会影响系统性能和用户体验。建议采用以下策略管理大量版本：

1. 实现版本归档功能，将不常用的版本归档到低成本存储中
2. 提供版本过滤和搜索功能，帮助用户快速找到所需版本
3. 实现版本清理策略，自动清理过期或不再需要的版本
4. 使用分页加载机制，避免一次加载所有版本

### 5.3 如何确保版本迁移的安全性？

版本迁移可能会导致数据丢失或工作流行为改变。建议采用以下策略确保迁移安全：

1. 在迁移前创建备份，以便在迁移失败时恢复
2. 在测试环境中验证迁移计划，确保迁移成功
3. 实现迁移回滚机制，允许在迁移失败时回滚到之前的版本
4. 记录迁移过程，以便在出现问题时进行分析和修复

## 6. 总结

KastraX 工作流版本控制系统提供了完整的工作流版本管理功能，包括版本创建、查询、切换、比较、迁移和回滚。通过使用这些功能，用户可以安全地管理工作流的演化，确保工作流的可靠性和可维护性。
