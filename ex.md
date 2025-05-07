# Kastrax 示例运行记录

本文档记录了已经运行过的 Kastrax 示例程序。

## 已运行示例

- [x] DeepSeekDirectStreamingExample - 已运行并优化流式响应性能
- [x] DeepSeekExample - 已运行成功，能够正常回答问题
- [x] DeepSeekStreamingExample - 已运行成功，能够流式响应，但在工具调用时有序列化问题
- [x] MemoryAgentExample - 已运行成功，能够记忆用户信息并在对话中使用，已使用 DSL 风格优化
- [x] MemorySystemExample - 已运行成功，能够创建、查询和删除对话线程，已使用 DSL 风格优化
- [x] SimpleZodToolExample - 已运行成功，能够验证输入并反转字符串
- [x] ToolsExample - 已运行成功，能够使用文件系统和 Web 搜索工具
- [x] CreativeAgentExample - 已运行成功，能够生成创意内容并进行自主探索，已使用标准方式实现
- [x] DeepseekAgentExample - 已运行成功，能够回答各种问题
- [x] DeepseekToolAgentExample - 已运行成功，能够使用工具执行任务
- [x] DeepseekArchitectureExample - 已运行成功，能够使用分层架构回答问题
- [x] DeepseekMemoryExample - 已运行成功，能够模拟记忆功能
- [x] DeepseekMain - 已运行成功，能够选择运行不同的示例
- [x] CalculatorExample - 已运行成功，能够计算数学表达式
- [x] SemanticSearchExample - 已运行成功，能够进行语义搜索和对话
- [x] EnhancedMemoryExample - 已运行成功，能够使用增强型内存系统进行对话
- [x] EnhancedWorkflowExample - 已运行成功，能够执行工作流程
- [x] EnhancedRagExample - 已运行成功，能够使用增强型 RAG 系统回答问题
- [x] EnhancedRetrievalExample - 已运行成功，能够使用增强型检索功能
- [x] EnhancedDocumentProcessingExample - 已运行成功，能够处理和转换文档

## 后续执行计划

根据对 examples 目录中示例的分析，我们将按以下顺序继续执行示例：

### 第一阶段：Agent 相关示例
- [~] AdaptiveAgentExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了一个自适应 Agent，可以根据用户偏好调整响应。
- [~] AdvancedAgentExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了一个高级 Agent，具有更复杂的功能。
- [~] AgentStateExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了 Agent 状态管理和会话控制功能。
- [~] AgentVersioningExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了 Agent 版本控制和回滚功能。
- [~] GoalOrientedAgentExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了目标导向 Agent，可以自动提取目标并分解任务。
- [~] ReflectiveAgentExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了反思型 Agent，可以对自己的响应进行反思和学习。
- [~] HierarchicalAgentExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了分层 Agent，包含协调器和多个专业子 Agent。
- [~] AgentNetworkExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了 Agent 网络，包含多个专业 Agent 协同工作。
- [~] CollaborativeAgentNetworkExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了协作型 Agent 网络，包含上下文感知路由和可视化功能。

### 第二阶段：内存相关示例
- [~] MemoryCompressionExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了内存压缩功能，可以在对话变长时自动压缩历史记录。
- [~] MemoryManagerExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了记忆管理器，可以进行高级查询、导出和管理对话记忆。
- [~] TagsAndSharingExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了标签和线程共享功能，可以对消息进行分类和在不同线程之间共享消息。
- [~] WorkingMemoryExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了工作内存功能，可以记录和更新用户信息和对话上下文。

### 第三阶段：RAG 相关示例
- [~] RAGExample - 已分析，但由于 Gradle 配置问题无法运行。该示例实现了基础 RAG 系统，可以从文档和网页中检索信息并生成回答。
- [ ] RAGWorkflowExample - RAG 工作流示例
- [ ] FastEmbedRAGExample - 快速嵌入 RAG 示例

### 第四阶段：工作流相关示例
- [ ] WorkflowExample - 基础工作流示例
- [ ] WorkflowRetryExample - 工作流重试示例
- [ ] AdvancedWorkflowExample - 高级工作流示例
- [ ] AgentChainExample - Agent 链示例
- [ ] DataFlowExample - 数据流示例
- [ ] DynamicWorkflowExample - 动态工作流示例
- [ ] ErrorHandlingWorkflowExample - 错误处理工作流示例
- [ ] EventCallbackWorkflowExample - 事件回调工作流示例
- [ ] WorkflowMonitoringExample - 工作流监控示例
- [ ] WorkflowVersioningExample - 工作流版本控制示例
- [ ] WorkflowVisualizationExample - 工作流可视化示例

### 第五阶段：工具相关示例
- [ ] AdvancedZodToolExample - 高级 Zod 工具示例
- [ ] DataClassZodToolExample - 数据类 Zod 工具示例
- [ ] DateTimeToolExample - 日期时间工具示例
- [ ] ZodAdvancedToolExample - Zod 高级工具示例
- [ ] ZodAgentExample - Zod Agent 示例
- [ ] ZodCalculatorExample - Zod 计算器示例
- [ ] ZodCalculatorToolExample - Zod 计算器工具示例
- [ ] FileOperationToolExample - 文件操作工具示例

### 第六阶段：其他示例
- [ ] DataSourceExample - 数据源示例
- [ ] AnthropicDirectStreamingExample - Anthropic 直接流式示例
- [ ] AnthropicStreamingExample - Anthropic 流式示例
- [ ] GeminiDirectStreamingExample - Gemini 直接流式示例
- [ ] GeminiStreamingExample - Gemini 流式示例
