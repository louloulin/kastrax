# KastraX 工作流引擎示例

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

4. **工作流版本控制和迁移**
   - 支持工作流版本管理
   - 提供工作流状态迁移工具
   - 确保向后兼容性

5. **工作流可视化和监控**
   - 支持生成工作流图
   - 提供执行监控和性能分析
   - 记录执行日志和历史

## 示例说明

### AdvancedWorkflowExample

`AdvancedWorkflowExample.kt` 展示了一个完整的工作流示例，包括：

- 内容生成步骤
- 可暂停的内容审核步骤
- 内容改进步骤
- 并行处理步骤（格式化和分析）
- 最终处理步骤

这个示例演示了如何：

1. 创建和配置工作流步骤
2. 使用工作流构建器构建工作流
3. 执行工作流并处理结果
4. 处理工作流暂停和恢复
5. 使用并行步骤提高执行效率

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.workflow.AdvancedWorkflowExampleKt"
```

## 测试

工作流引擎的测试代码位于 `examples/src/test/kotlin/ai/kastrax/examples/workflow/WorkflowEngineTest.kt`，包括：

- 基本工作流执行测试
- 条件分支测试
- 并行执行测试
- 循环执行测试
- 工作流暂停和恢复测试

要运行测试，请执行以下命令：

```bash
./gradlew :examples:test --tests "ai.kastrax.examples.workflow.WorkflowEngineTest"
```
