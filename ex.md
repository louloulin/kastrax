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
- [x] HelloWorld - 已成功运行。这是一个简单的Java示例，用于测试编译和运行。

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
- [~] WorkingMemoryExample - 已分析，但由于编译问题无法运行。该示例实现了工作内存功能，可以记录和更新用户信息和对话上下文。

### 第三阶段：RAG 相关示例
- [~] RAGExample - 已分析，但由于编译问题无法运行。该示例实现了基础RAG系统，可以从文档中检索信息并生成回答。
- [~] RAGWorkflowExample - 已分析，但由于编译问题无法运行。该示例实现了RAG工作流，包含研究、分析和报告生成步骤。
- [~] FastEmbedRAGExample - 已分析，但由于编译问题无法运行。该示例实现了使用本地嵌入模型的RAG系统，无需依赖外部API。

### 第四阶段：工作流相关示例
- [~] WorkflowExample - 已分析，但由于编译问题无法运行。该示例实现了内容创作工作流，包括研究、写作和编辑三个步骤。
- [~] WorkflowRetryExample - 已分析，但由于编译问题无法运行。该示例实现了工作流重试机制，可以在步骤失败时自动重试。
- [~] AdvancedWorkflowExample - 已分析，但由于编译问题无法运行。该示例实现了高级工作流功能，包括内容生成、审核、改进、并行处理和最终处理步骤。
- [~] AgentChainExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了Agent链，包括研究规划、信息收集和报告生成三个代理。
- [~] DataFlowExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了数据流工作流，包括数据加载、转换、分析和生成摘要步骤。
- [~] DynamicWorkflowExample - 已分析，但由于编译问题无法运行。该示例实现了动态工作流，可以在运行时生成和组合工作流。
- [~] ErrorHandlingWorkflowExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了错误处理工作流，包括网络错误、验证错误和处理错误的处理策略。
- [~] EventCallbackWorkflowExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了事件和回调工作流，可以监听工作流事件并执行回调函数。
- [~] WorkflowMonitoringExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了工作流监控功能，可以监控工作流执行状态和性能。
- [~] WorkflowVersioningExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了工作流版本控制功能，可以管理工作流的不同版本。
- [~] WorkflowVisualizationExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了工作流可视化功能，可以生成工作流的可视化表示。

### 第五阶段：工具相关示例
- [~] AdvancedZodToolExample - 已分析，但由于编译问题无法运行。该示例实现了高级Zod工具，包括复杂数据结构的验证和转换。
- [~] DataClassZodToolExample - 已分析，但由于编译问题无法运行。该示例实现了使用数据类的Zod工具，包括用户数据验证。
- [~] DateTimeToolExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了日期时间工具，包括获取当前时间、格式化、解析、加减和时区转换等功能。
- [~] ZodAdvancedToolExample - 已分析，但由于编译问题无法运行。该示例实现了高级Zod工具，包括用户搜索功能和复杂数据结构处理。
- [~] ZodAgentExample - 已分析，但由于编译问题无法运行。该示例实现了使用Zod工具的Agent，可以执行数学计算和日期时间处理。
- [~] ZodCalculatorExample - 已分析，但由于编译问题无法运行。该示例实现了计算器工具，可以执行基本的数学运算。
- [~] ZodCalculatorToolExample - 已分析，但由于编译问题无法运行。该示例实现了使用数据类的计算器工具，包括输入验证和结果格式化。
- [~] FileOperationToolExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了文件操作工具，包括文件读写、列表和删除等功能。

### 第六阶段：其他示例
- [~] DataSourceExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了数据源功能，可以从不同来源加载和处理数据。
- [~] AnthropicDirectStreamingExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了Anthropic模型的直接流式调用，可以实时获取模型输出。
- [~] AnthropicStreamingExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了Anthropic模型的流式调用，可以实时获取模型输出。
- [~] GeminiDirectStreamingExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了Gemini模型的直接流式调用，可以实时获取模型输出。
- [~] GeminiStreamingExample - 已分析，但由于Gradle配置问题无法运行。该示例实现了Gemini模型的流式调用，可以实时获取模型输出。
