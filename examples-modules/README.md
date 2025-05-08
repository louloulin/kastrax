# Kastrax 示例模块

这个目录包含了Kastrax框架的各种示例，按照功能分类组织成多个子模块。

## 模块结构

- **workflow**: 工作流相关示例
- **rag**: RAG（检索增强生成）相关示例
- **memory**: 内存相关示例
- **tools**: 工具相关示例
- **agent**: Agent相关示例
- **other**: 其他示例
- **plugin**: 插件相关示例

## 如何运行示例

### 使用脚本运行

在项目根目录下，运行以下命令：

```bash
./run_examples_modules.sh <类别> [示例名称]
```

例如：

```bash
# 运行动态工作流示例
./run_examples_modules.sh workflow dynamic

# 运行基础RAG示例
./run_examples_modules.sh rag basic

# 运行所有示例
./run_examples_modules.sh all
```

### 使用Gradle运行

在`examples-modules`目录下，运行以下命令：

```bash
./gradlew run --args="<类别> [示例名称]"
```

例如：

```bash
# 运行动态工作流示例
./gradlew run --args="workflow dynamic"

# 运行基础RAG示例
./gradlew run --args="rag basic"

# 运行所有示例
./gradlew run --args="all"
```

### 直接运行特定示例

每个子模块都有自己的Gradle任务来运行特定的示例：

```bash
# 运行动态工作流示例
./gradlew :workflow:runDynamicWorkflowExample

# 运行基础RAG示例
./gradlew :rag:runRAGExample

# 运行工作内存示例
./gradlew :memory:runWorkingMemoryExample
```

## 可用的示例

### 工作流示例

- **WorkflowExample**: 基础工作流示例，实现了内容创作工作流，包括研究、写作和编辑三个步骤。
- **DynamicWorkflowExample**: 动态工作流示例，实现了动态工作流，可以在运行时生成和组合工作流。
- **AdvancedWorkflowExample**: 高级工作流示例，实现了高级工作流功能，包括内容生成、审核、改进、并行处理和最终处理步骤。
- **WorkflowRetryExample**: 工作流重试示例，实现了工作流重试机制，可以在步骤失败时自动重试。

### RAG示例

- **RAGExample**: 基础RAG示例，实现了基础RAG系统，可以从文档中检索信息并生成回答。
- **RAGWorkflowExample**: RAG工作流示例，实现了RAG工作流，包含研究、分析和报告生成步骤。
- **FastEmbedRAGExample**: 快速嵌入RAG示例，实现了使用本地嵌入模型的RAG系统，无需依赖外部API。

### 内存示例

- **WorkingMemoryExample**: 工作内存示例，实现了工作内存功能，可以记录和更新用户信息和对话上下文。
- **MemoryCompressionExample**: 内存压缩示例，实现了内存压缩功能，可以在对话变长时自动压缩历史记录。
- **MemoryManagerExample**: 内存管理器示例，实现了记忆管理器，可以进行高级查询、导出和管理对话记忆。
- **TagsAndSharingExample**: 标签和共享示例，实现了标签和线程共享功能，可以对消息进行分类和在不同线程之间共享消息。

### 工具示例

- **AdvancedZodToolExample**: 高级Zod工具示例，实现了高级Zod工具，包括复杂数据结构的验证和转换。
- **DataClassZodToolExample**: 数据类Zod工具示例，实现了使用数据类的Zod工具，包括用户数据验证。
- **DateTimeToolExample**: 日期时间工具示例，实现了日期时间工具，包括获取当前时间、格式化、解析、加减和时区转换等功能。
- **ZodAdvancedToolExample**: Zod高级工具示例，实现了高级Zod工具，包括用户搜索功能和复杂数据结构处理。
- **ZodCalculatorExample**: Zod计算器示例，实现了计算器工具，可以执行基本的数学运算。
- **ZodCalculatorToolExample**: Zod计算器工具示例，实现了使用数据类的计算器工具，包括输入验证和结果格式化。

### Agent示例

- **ZodAgentExample**: Zod代理示例，实现了使用Zod工具的Agent，可以执行数学计算和日期时间处理。
- **AdaptiveAgentExample**: 自适应代理示例，实现了一个自适应Agent，可以根据用户偏好调整响应。
- **AdvancedAgentExample**: 高级代理示例，实现了一个高级Agent，具有更复杂的功能。
- **AgentStateExample**: 代理状态示例，实现了Agent状态管理和会话控制功能。
- **AgentVersioningExample**: 代理版本控制示例，实现了Agent版本控制和回滚功能。
- **GoalOrientedAgentExample**: 目标导向代理示例，实现了目标导向Agent，可以自动提取目标并分解任务。
- **ReflectiveAgentExample**: 反思型代理示例，实现了反思型Agent，可以对自己的响应进行反思和学习。
- **HierarchicalAgentExample**: 分层代理示例，实现了分层Agent，包含协调器和多个专业子Agent。
- **AgentNetworkExample**: 代理网络示例，实现了Agent网络，包含多个专业Agent协同工作。

### 其他示例

- **DataSourceExample**: 数据源示例，实现了数据源功能，可以从不同来源加载和处理数据。
- **AnthropicDirectStreamingExample**: Anthropic直接流式示例，实现了Anthropic模型的直接流式调用，可以实时获取模型输出。
- **AnthropicStreamingExample**: Anthropic流式示例，实现了Anthropic模型的流式调用，可以实时获取模型输出。
- **GeminiDirectStreamingExample**: Gemini直接流式示例，实现了Gemini模型的直接流式调用，可以实时获取模型输出。
- **GeminiStreamingExample**: Gemini流式示例，实现了Gemini模型的流式调用，可以实时获取模型输出。

### 插件相关示例

- **HttpConnectorPlugin**: HTTP连接器插件示例，实现了HTTP连接器插件，可以通过HTTP协议连接外部服务。
- **HttpStepPlugin**: HTTP步骤插件示例，实现了HTTP步骤插件，可以在工作流中执行HTTP请求。
