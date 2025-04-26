# KastraX 示例

这个目录包含了使用KastraX AI Agent框架的示例程序，展示了如何使用框架的各种功能。示例按功能模块分类，方便学习和参考。

## 示例目录结构

```
examples/
├── agent/                # 代理相关示例
├── memory/               # 内存系统示例
├── rag/                  # 检索增强生成示例
├── tools/                # 工具使用示例
├── workflow/             # 工作流示例
├── integrations/         # 不同模型集成示例
│   ├── openai/           # OpenAI模型示例
│   ├── deepseek/         # DeepSeek模型示例
│   └── anthropic/        # Anthropic模型示例
└── README.md             # 主要示例文档
```

## 示例分类

### 代理示例 (agent/)

代理示例展示了如何创建和使用KastraX代理，包括基本代理配置、多代理系统和代理网络。

- **SimpleAgentExample.kt**: 基本代理设置和使用
- **MultiAgentExample.kt**: 多个代理协同工作
- **AgentNetworkExample.kt**: 代理网络通信

### 内存系统示例 (memory/)

内存系统示例展示了如何使用KastraX的内存功能，包括基本内存使用、语义搜索和工作内存。

- **BasicMemoryExample.kt**: 简单内存使用
- **SemanticSearchExample.kt**: 使用语义搜索与内存
- **WorkingMemoryExample.kt**: 工作内存实现
- **PersistentMemoryExample.kt**: 持久化内存存储

### RAG示例 (rag/)

RAG示例展示了如何使用KastraX的检索增强生成功能，包括文档分块、向量嵌入和混合搜索。

- **BasicRAGExample.kt**: 简单RAG实现
- **ChunkingExample.kt**: 文档分块策略
- **EmbeddingExample.kt**: 文档向量嵌入
- **HybridSearchExample.kt**: 结合关键词和语义搜索

### 工具示例 (tools/)

工具示例展示了如何创建和使用KastraX工具，包括基本工具、Web搜索工具和文件系统工具。

- **BasicToolExample.kt**: 创建和使用简单工具
- **WebSearchToolExample.kt**: Web搜索工具实现
- **FileSystemToolExample.kt**: 文件系统操作
- **CustomToolExample.kt**: 创建自定义工具

### 工作流示例 (workflow/)

工作流示例展示了如何使用KastraX的工作流功能，包括基本工作流、并行执行和条件分支。

- **SimpleWorkflowExample.kt**: 基本工作流创建
- **ParallelWorkflowExample.kt**: 并行执行步骤
- **ConditionalWorkflowExample.kt**: 条件分支
- **DynamicWorkflowExample.kt**: 动态生成工作流

### 集成示例 (integrations/)

集成示例展示了如何使用KastraX与不同的模型提供商集成，包括OpenAI、DeepSeek和Anthropic。

- **OpenAIExample.kt**: 使用OpenAI模型
- **DeepSeekExample.kt**: 使用DeepSeek模型
- **AnthropicExample.kt**: 使用Anthropic模型

## 运行示例

要运行示例，请使用以下命令：

```bash
# 编译和运行特定示例
cd kastra
./gradlew run --args="ai.kastrax.examples.<category>.<ExampleName>Kt"

# 例如，运行简单代理示例
./gradlew run --args="ai.kastrax.examples.agent.SimpleAgentExampleKt"
```

## 学习路径

如果你是KastraX的新用户，我们建议按照以下顺序学习这些示例：

1. **agent/SimpleAgentExample.kt**: 基础的Agent创建和使用
2. **tools/BasicToolExample.kt**: 基本工具创建和使用
3. **memory/BasicMemoryExample.kt**: 基本内存系统使用
4. **workflow/SimpleWorkflowExample.kt**: 基本工作流创建和使用
5. **rag/BasicRAGExample.kt**: 基本RAG实现

然后，你可以根据自己的需求探索更高级的示例。
