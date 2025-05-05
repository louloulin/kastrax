# Kastrax 文档迁移计划

## 问题分析

通过分析 kastrax-doc/src/content 目录，我们发现：

1. 英文文档中仍然存在大量 "mastra" 相关内容，特别是在代码示例和导入语句中
2. 英文文档目录包含 306 个 .mdx 文件，而中文文档目录只有 36 个 .mdx 文件
3. 许多文档仍然基于 TypeScript 的 Mastra 框架，而不是 Kotlin 的 Kastrax 框架

## 迁移目标

1. 将所有英文文档中的 "mastra" 相关内容替换为 Kastrax 对应的内容
2. 将所有英文文档转换为中文文档，确保内容准确反映 Kastrax 的 Kotlin 实现
3. 分析整个 Kastrax 代码库，确保文档内容与实际实现一致
4. 在 dox.md 中标记已实现的功能

## 代码库分析

通过分析 Kastrax 代码库，我们发现以下主要模块和功能：

### 1. 核心模块 (kastrax-core)

- **Agent 系统**：包括 Agent 接口、LLMAgent 实现、各种 Agent 架构（自适应、目标导向、层次化、反思型、创造性）
- **工具系统**：包括 Tool 接口、ZodTool 实现、内置工具（文件操作、Web 搜索等）
- **记忆系统**：包括 Memory 接口、各种记忆类型和存储实现
- **工作流系统**：包括 Workflow 接口、工作流执行引擎、状态管理等
- **LLM 集成**：包括 LlmProvider 接口、各种 LLM 实现（DeepSeek、OpenAI 等）

### 2. RAG 系统 (kastrax-rag)

- **文档处理**：包括 DocumentLoader、DocumentSplitter 等
- **向量存储**：包括 RagVectorStore 接口、InMemoryVectorStore 等实现
- **检索系统**：包括 Retriever 接口、各种检索策略（语义、混合、查询增强）
- **重排序**：包括 Reranker 接口、各种重排序策略

### 3. Actor 模型 (kastrax-actor)

- **Actor 系统**：基于 kactor 库的 Actor 实现
- **远程 Actor**：支持分布式 Actor 系统
- **Actor 与 Agent 集成**：将 Actor 模型与 Agent 系统集成

### 4. A2A 协议 (kastrax-a2a)

- **A2A 协议核心**：代理间通信协议
- **代理发现**：代理注册和发现机制
- **任务委派**：任务分配和执行
- **工作流引擎**：A2A 工作流管理

### 5. 原生开发 (graal-native)

- **GraalVM 集成**：支持 GraalVM 原生镜像
- **SDK**：Rust、Go、JavaScript SDK

## 迁移计划

### 阶段 1：文档结构迁移

1. 创建完整的中文文档目录结构，与英文文档保持一致
2. 确保所有必要的文档文件都存在于中文目录中

### 阶段 2：核心模块文档迁移

1. **Agent 系统**
   - 分析 kastrax-core/src/main/kotlin/ai/kastrax/core/agent 目录
   - 更新 Agent 接口、LLMAgent 实现、Agent 架构等文档
   - 提供基于 Kotlin 的代码示例

2. **工具系统**
   - 分析 kastrax-core/src/main/kotlin/ai/kastrax/core/tools 目录
   - 更新工具定义、注册、调用等文档
   - 提供 ZodTool 和内置工具的使用示例

3. **记忆系统**
   - 分析 kastrax-memory-api 和 kastrax-memory-impl 目录
   - 更新记忆类型、查询、处理器等文档
   - 提供不同存储后端的使用示例

4. **工作流系统**
   - 分析 kastrax-core/src/main/kotlin/ai/kastrax/core/workflow 目录
   - 更新工作流定义、执行、状态管理等文档
   - 提供工作流 DSL 的使用示例

### 阶段 3：RAG 系统文档迁移

1. **文档处理**
   - 分析 kastrax-rag/src/main/kotlin/ai/kastrax/rag/document 目录
   - 更新文档加载、分割等文档
   - 提供各种文档类型的处理示例

2. **向量存储**
   - 分析 kastrax-rag/src/main/kotlin/ai/kastrax/rag/vectorstore 目录
   - 更新内存向量存储、数据库向量存储等文档
   - 提供向量存储的使用示例

3. **检索系统**
   - 分析 kastrax-rag/src/main/kotlin/ai/kastrax/rag/retrieval 目录
   - 更新语义检索、混合检索、查询增强检索等文档
   - 提供各种检索策略的使用示例

4. **重排序**
   - 分析 kastrax-rag/src/main/kotlin/ai/kastrax/rag/reranker 目录
   - 更新重排序策略文档
   - 提供重排序的使用示例

### 阶段 4：Actor 模型和 A2A 协议文档迁移

1. **Actor 模型**
   - 分析 kastrax-actor 目录
   - 更新 Actor 基础、远程 Actor、Actor 与 Agent 集成等文档
   - 提供 Actor 系统的使用示例

2. **A2A 协议**
   - 分析 kastrax-a2a 目录
   - 更新 A2A 基础、代理发现、任务委派等文档
   - 提供 A2A 协议的使用示例

### 阶段 5：原生开发文档迁移

1. **GraalVM 集成**
   - 分析 graal-native 目录
   - 更新 GraalVM 原生镜像文档
   - 提供原生镜像构建示例

2. **SDK**
   - 分析 graal-native/sdk-* 目录
   - 更新 Rust、Go、JavaScript SDK 文档
   - 提供各种 SDK 的使用示例

### 阶段 6：示例和最佳实践文档迁移

1. **基础示例**
   - 分析 examples 目录
   - 更新简单对话 Agent、工具使用、记忆使用等示例文档
   - 提供完整的代码示例

2. **高级示例**
   - 更新多 Agent 协作、分布式 Agent 系统、RAG 应用、工作流应用等示例文档
   - 提供完整的代码示例

## 实施计划

### 第 1 周：准备和核心模块迁移

1. 创建完整的中文文档目录结构
2. 迁移 Agent 系统文档
3. 迁移工具系统文档
4. 迁移记忆系统文档

### 第 2 周：工作流和 RAG 系统迁移

1. 迁移工作流系统文档
2. 迁移 RAG 系统文档（文档处理、向量存储）
3. 迁移 RAG 系统文档（检索系统、重排序）

### 第 3 周：Actor 模型、A2A 协议和原生开发迁移

1. 迁移 Actor 模型文档
2. 迁移 A2A 协议文档
3. 迁移原生开发文档

### 第 4 周：示例、最佳实践和完善

1. 迁移基础示例文档
2. 迁移高级示例文档
3. 完善所有文档，确保一致性和准确性
4. 更新 dox.md，标记已实现的功能

## 文档转换方法

1. **代码示例转换**：将 TypeScript 代码示例转换为 Kotlin 代码示例，确保与 Kastrax 实现一致
2. **API 参考转换**：确保 API 参考文档准确反映 Kastrax 的 Kotlin API
3. **概念解释转换**：保持概念解释的准确性，同时考虑 Kotlin 和 JVM 生态系统的特点
4. **中文翻译**：确保中文翻译准确、流畅，使用适当的技术术语

## 质量保证

1. **代码验证**：确保所有代码示例都能在 Kastrax 环境中正常运行
2. **文档一致性**：确保文档内容与 Kastrax 实现一致
3. **术语统一**：确保术语使用一致，特别是在中文翻译中
4. **格式规范**：确保文档格式符合 Markdown 和 MDX 规范

## 结论

通过这个迁移计划，我们将把所有文档从基于 TypeScript 的 Mastra 框架转换为基于 Kotlin 的 Kastrax 框架，确保文档内容准确反映 Kastrax 的实际实现。这将大大提高文档的质量和实用性，帮助用户更好地理解和使用 Kastrax 框架。
