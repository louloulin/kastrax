# Kastrax Examples 修复计划

## 编译问题分析

通过分析编译错误，我们发现以下几类主要问题：

### 1. API 兼容性问题

1. **WorkflowContext.getStepOutput 方法**：
   - 错误：`Cannot access 'fun getStepOutput(stepResult: WorkflowStepResult, outputKey: String): Any?': it is private in 'ai/kastrax/core/workflow/WorkflowContext'`
   - 原因：此方法在当前 API 中是私有的，无法直接访问
   - 解决方案：使用公共方法 `context.getStepResult(stepId)?.output?.get(outputKey)` 或 `context.resolveReference(VariableReference("$.steps.stepId.output.outputKey"))`

2. **WorkflowStep 接口实现**：
   - 错误：`Class '<anonymous>' is not abstract and does not implement abstract member 'variables'`
   - 原因：WorkflowStep 接口增加了 variables 成员，但示例代码中的匿名类未实现
   - 解决方案：为所有 WorkflowStep 实现添加 variables 成员

3. **Agent 创建方法**：
   - 错误：`Too many arguments for 'fun agent(init: AgentBuilder.() -> Unit): Agent'`
   - 原因：agent 方法签名已更改，现在使用 DSL 风格
   - 解决方案：使用新的 DSL 风格创建 Agent

4. **OpenAI 替换为 DeepSeek**：
   - 错误：`Unresolved reference 'openai'`
   - 原因：API 已从 OpenAI 迁移到 DeepSeek
   - 解决方案：将 OpenAI 相关代码替换为 DeepSeek

### 2. 缺失的方法和属性

1. **isResumed 和 getResumeData**：
   - 错误：`Unresolved reference 'isResumed'` 和 `Unresolved reference 'getResumeData'`
   - 原因：这些方法可能已被移除或重命名
   - 解决方案：查找替代方法或重新实现相关功能

2. **suspendableStep 和 parallel**：
   - 错误：`Unresolved reference 'suspendableStep'` 和 `Unresolved reference 'parallel'`
   - 原因：这些方法可能已被移除或重命名
   - 解决方案：查找替代方法或重新实现相关功能

3. **StepStatus**：
   - 错误：`Unresolved reference 'StepStatus'`
   - 原因：此枚举可能已被移除或重命名
   - 解决方案：查找替代枚举或使用新的状态表示方式

## 修复计划

我们将采用以下步骤修复编译问题：

### 步骤 1：创建基础示例模板

1. 创建一个简单的工作流示例模板，使用当前 API 的正确方法
2. 确保模板能够成功编译和运行
3. 使用模板作为参考，修复其他示例

### 步骤 2：修复 WorkflowContext 相关问题

1. 将所有 `context.getStepOutput()` 调用替换为 `context.getStepResult()?.output?.get()` 或 `context.resolveReference()`
2. 修复所有 `get()` 未解析的引用

### 步骤 3：修复 WorkflowStep 实现

1. 为所有 WorkflowStep 匿名类实现添加 `override val variables: Map<String, VariableReference> = emptyMap()` 成员
2. 确保所有 WorkflowStep 实现都符合当前接口要求

### 步骤 4：修复 Agent 创建

1. 将所有 OpenAI 相关代码替换为 DeepSeek
2. 使用新的 DSL 风格创建 Agent：
   ```kotlin
   val agent = agent {
       name = "Agent名称"
       instructions = "指令..."
       model = deepSeek {
           model("deepseek-chat")
           apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
       }
   }
   ```

### 步骤 5：修复缺失的方法和属性

1. 查找 `isResumed` 和 `getResumeData` 的替代方法
2. 查找 `suspendableStep` 和 `parallel` 的替代方法
3. 查找 `StepStatus` 的替代枚举

### 步骤 6：逐个修复示例

我们将按照以下顺序修复示例：

1. **WorkflowExample.kt**：基础工作流示例
2. **AdvancedWorkflowExample.kt**：高级工作流示例
3. **WorkflowRetryExample.kt**：工作流重试示例
4. **DataFlowExample.kt**：数据流示例
5. **ErrorHandlingWorkflowExample.kt**：错误处理示例
6. **EventCallbackWorkflowExample.kt**：事件回调示例
7. **SuspendableWorkflowExample.kt**：可挂起工作流示例
8. **WorkflowEventVisualizerExample.kt**：工作流事件可视化示例

### 步骤 7：测试和验证

1. 为每个修复的示例运行 `./gradlew :examples-modules:<module>:build -x test`
2. 确保所有示例都能成功编译
3. 运行示例并验证功能正确性

## 详细修复计划

### 1. WorkflowExample.kt

1. 检查并修复 WorkflowStep 实现
2. 修复 getStepOutput 调用
3. 确保使用正确的 Agent 创建方式

### 2. AdvancedWorkflowExample.kt

1. 修复所有 WorkflowStep 实现，添加 variables 成员
2. 将所有 getStepOutput 调用替换为 getStepResult()?.output?.get()
3. 修复 isResumed 和 getResumeData 调用
4. 修复 agent、suspendableStep 和 parallel 方法调用
5. 修复 StepStatus 引用

### 3. WorkflowRetryExample.kt

1. 将 OpenAI 替换为 DeepSeek
2. 修复 agent 创建方式
3. 修复 agentStep 相关代码

### 4. DataFlowExample.kt

1. 修复所有 getStepOutput 调用

### 5. ErrorHandlingWorkflowExample.kt

1. 修复所有 getStepOutput 调用

### 6. EventCallbackWorkflowExample.kt

1. 将 OpenAI 替换为 DeepSeek
2. 修复 agent 创建方式

### 7. SuspendableWorkflowExample.kt

1. 将 OpenAI 替换为 DeepSeek
2. 修复 agent 创建方式

### 8. WorkflowEventVisualizerExample.kt

1. 将 OpenAI 替换为 DeepSeek
2. 修复 agent 创建方式

## 时间估计

- 步骤 1（创建基础示例模板）：1 小时
- 步骤 2-5（修复通用问题）：2 小时
- 步骤 6（逐个修复示例）：4 小时（每个示例约 30 分钟）
- 步骤 7（测试和验证）：1 小时

总计：约 8 小时

## 优先级

1. 创建基础示例模板（已完成）
2. 修复 WorkflowExample.kt（基础示例）
3. 修复 AdvancedWorkflowExample.kt（高级示例）
4. 修复其他示例

## 注意事项

1. 保持代码风格一致
2. 添加详细注释，解释 API 的用法
3. 确保所有示例都能正常运行
4. 考虑添加中文注释，提高可读性
