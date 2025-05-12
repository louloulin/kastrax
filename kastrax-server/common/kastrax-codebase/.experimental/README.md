# Kastrax Codebase 实验性功能

本目录包含Kastrax Codebase模块的实验性功能实现，这些功能尚未完全集成到主代码库中。

## 功能列表

1. **代码语义理解**
   - 基于Chapi的代码解析器 (`semantic/parser/`)
   - 符号关系图构建 (`semantic/graph/`)

2. **向量存储优化**
   - 代码特定向量存储 (`vector/CodeVectorStore.kt`)
   - 多租户向量存储 (`vector/MultiTenantVectorStore.kt`)

3. **上下文感知检索**
   - 多级上下文构建 (`context/ContextBuilder.kt`)
   - 上下文感知检索引擎 (`retrieval/ContextAwareRetrievalEngine.kt`)

4. **代码嵌入服务**
   - 代码特定嵌入服务 (`embedding/CodeEmbeddingService.kt`)

5. **索引任务处理**
   - 代码库索引任务处理器 (`indexing/CodebaseIndexTaskProcessor.kt`)

## 实现状态

这些功能目前处于实验阶段，存在以下问题：

1. 与现有Kastrax代码库的兼容性问题
2. 依赖关系不完整
3. 编译错误

## 下一步计划

1. 解决与现有代码库的兼容性问题
2. 完成单元测试和集成测试
3. 逐步将这些功能集成到主代码库中

## 使用说明

这些代码仅供参考，不应直接在生产环境中使用。如果需要使用这些功能，请先解决兼容性问题并进行充分测试。
