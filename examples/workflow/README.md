# KastraX 工作流示例

本目录包含了 KastraX 工作流引擎的示例代码，展示了工作流引擎的各种功能和用法。

## 主要功能

KastraX 工作流引擎提供了以下主要功能：

1. **工作流暂停和恢复**
   - 支持在任意步骤暂停工作流
   - 支持从暂停点恢复工作流执行
   - 提供状态持久化和恢复机制

2. **并行步骤执行**
   - 支持并行执行多个步骤
   - 自动管理并行步骤的依赖关系
   - 合并并行步骤的执行结果

3. **条件分支和循环**
   - 支持基于条件的分支执行
   - 支持循环执行步骤直到条件不满足
   - 提供动态执行路径支持

4. **动态工作流生成**
   - 支持在运行时动态生成工作流
   - 提供工作流模板系统
   - 允许参数化工作流定义

## 示例说明

### SimpleWorkflowExample

`SimpleWorkflowExample.kt` 展示了一个基本的工作流示例，包括：

- 创建和配置工作流
- 定义工作流步骤
- 执行工作流
- 处理工作流结果

### ParallelWorkflowExample

`ParallelWorkflowExample.kt` 展示了并行工作流示例，包括：

- 创建并行步骤
- 管理步骤依赖关系
- 合并执行结果
- 错误处理

### ConditionalWorkflowExample

`ConditionalWorkflowExample.kt` 展示了条件工作流示例，包括：

- 创建条件分支
- 定义条件逻辑
- 实现循环执行
- 动态路径选择

### DynamicWorkflowExample

`DynamicWorkflowExample.kt` 展示了动态工作流示例，包括：

- 创建工作流模板
- 参数化工作流定义
- 运行时工作流生成
- 模板复用

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.workflow.SimpleWorkflowExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.workflow.ParallelWorkflowExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.workflow.ConditionalWorkflowExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.workflow.DynamicWorkflowExampleKt"
```
