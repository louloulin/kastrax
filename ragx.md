# Kastrax RAG 模块迁移计划

> **状态**: 规划阶段

## 1. 背景与目标

Kastrax 项目已经实现了新的 vector store 架构，提供了更加灵活、高效的向量存储功能。目前，RAG 模块仍然使用旧的向量存储实现，需要完全迁移到新的 vector store 架构，以统一代码库并利用新架构的优势。

### 主要目标

1. 完全迁移 RAG 模块到新的 vector store 架构
2. 保持 RAG 模块的现有功能和 API 兼容性
3. 优化 RAG 模块的性能和可扩展性
4. 简化代码库，减少重复代码
5. 提供更好的文档和示例

## 2. 当前状态分析

### 2.1 RAG 模块现状

RAG 模块目前使用自己的向量存储接口和实现，主要包括：

- `RagVectorStore` 接口：定义了 RAG 向量存储的基本操作
- `RagDocument` 类：表示 RAG 文档
- `SearchResult` 类：表示搜索结果
- 多种检索器实现：`TopKRetriever`、`HybridRetriever` 等
- 文档处理工具：`DocumentLoader`、`DocumentSplitter` 等

### 2.2 新 Vector Store 架构

新的 vector store 架构已经实现，主要包括：

- `VectorStore` 接口：定义了向量存储的基本操作
- `Document` 类：表示文档
- `SearchResult` 类：表示搜索结果
- `DocumentVectorStore` 接口：提供文档操作的统一接口
- `RagVectorStoreAdapter` 类：将 `VectorStore` 适配为 RAG 使用的接口
- 多种向量存储实现：`InMemoryVectorStore`、`PineconeVectorStore`、`QdrantVectorStore`、`LanceDBVectorStore` 等

## 3. 迁移计划

### 3.1 阶段一：准备工作

2. **定义核心接口和类**
   - 定义 `rag` 主类，作为新 RAG 模块的入口
   - 定义 `ragOptions` 类，配置 RAG 处理选项
   - 定义 `Retriever` 接口，统一检索器接口

### 3.2 阶段二：基础功能迁移

1. **文档处理功能迁移**
   - 迁移 `DocumentLoader` 接口和实现
   - 迁移 `DocumentSplitter` 接口和实现
   - 迁移文档过滤器和转换器

2. **检索器迁移**
   - 实现基于新 vector store 的 `TopKRetriever`
   - 实现基于新 vector store 的 `HybridRetriever`
   - 实现 `RetrieverFactory` 工厂类

3. **上下文构建功能迁移**
   - 迁移 `ContextBuilder` 类
   - 迁移上下文构建策略

### 3.3 阶段三：高级功能迁移

1. **重排序器迁移**
   - 迁移 `Reranker` 接口和实现
   - 迁移 `RerankerFactory` 工厂类

2. **查询增强功能迁移**
   - 迁移 `QueryTransformer` 接口和实现
   - 迁移 `QueryEnhancedRetriever` 类

3. **实时 RAG 功能迁移**
   - 迁移 `RealTimeRag` 类
   - 迁移 `RealTimeRagTool` 类

### 3.4 阶段四：集成与测试

1. **单元测试**
   - 为所有迁移的类编写单元测试
   - 确保测试覆盖率达到 80% 以上

2. **集成测试**
   - 编写端到端测试，验证整个 RAG 流程
   - 测试与不同 LLM 提供商的集成

3. **性能测试**
   - 测试大规模文档集的性能
   - 测试并发处理能力

### 3.5 阶段五：文档和示例

1. **API 文档**
   - 为所有公共 API 编写详细文档
   - 提供使用指南和最佳实践

2. **示例代码**
   - 编写基本 RAG 示例
   - 编写高级 RAG 示例（混合检索、查询增强等）
   - 编写与工作流集成的示例

## 4. 技术实现细节

### 4.1 核心类设计

#### rag 主类

```kotlin
class rag(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService,
    private val reranker: Reranker = IdentityReranker(),
    private val defaultOptions: ragOptions = ragOptions()
) {
    // 文档加载方法
    suspend fun loadDocuments(loader: DocumentLoader, splitter: DocumentSplitter? = null): Int
    
    // 搜索方法
    suspend fun search(query: String, limit: Int = 5, minScore: Double = 0.0, options: ragOptions? = null): List<SearchResult>
    
    // 上下文生成方法
    suspend fun generateContext(query: String, limit: Int = 5, minScore: Double = 0.0, options: ragOptions? = null): String
    
    // 检索上下文方法
    suspend fun retrieveContext(query: String, options: ragOptions? = null, limit: Int = 5, minScore: Double = 0.0): RetrieveContextResult
}
```

#### Retriever 接口

```kotlin
interface Retriever {
    suspend fun retrieve(query: String, limit: Int = 5, minScore: Double = 0.0): List<Document>
}
```

### 4.2 适配层设计

为了保持向后兼容性，我们将实现适配层，将旧的 API 调用转发到新的实现：

```kotlin
class LegacyRagAdapter(private val rag: rag) : RAG {
    // 实现旧的 RAG 接口方法，内部调用 rag 方法
}
```

### 4.3 文档处理

文档处理功能将直接使用新的 vector store 架构中的 `Document` 类，不再使用旧的 `RagDocument` 类：

```kotlin
interface DocumentLoader {
    suspend fun load(): List<Document>
}

interface DocumentSplitter {
    fun split(document: Document): List<Document>
}
```

### 4.4 检索器实现

检索器将基于新的 vector store 架构实现：

```kotlin
class TopKRetriever(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService
) : Retriever {
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<Document> {
        val embedding = embeddingService.embed(query)
        val results = vectorStore.query(
            indexName = "default",
            queryVector = embedding,
            topK = limit
        ).filter { it.score >= minScore }
        
        return results.map { result ->
            val content = result.metadata?.get("content") as? String ?: ""
            Document(result.id, content, result.metadata ?: emptyMap())
        }
    }
}
```

## 5. 迁移策略

### 5.1 渐进式迁移

1. **并行开发**
   - 在不影响现有 RAG 模块的情况下开发新的 rag 模块
   - 完成基本功能后进行初步测试

2. **适配层过渡**
   - 实现适配层，允许现有代码通过适配层使用新的实现
   - 逐步将现有代码迁移到直接使用新的 API

3. **完全迁移**
   - 所有代码迁移完成后，移除旧的实现和适配层
   - 更新所有示例和文档

### 5.2 兼容性保证

1. **API 兼容性**
   - 保持主要 API 的签名和行为一致
   - 对于必须更改的 API，提供明确的迁移指南

2. **功能等价性**
   - 确保所有现有功能在新实现中都有对应
   - 通过测试验证功能等价性

## 6. 时间线

| 阶段 | 任务 | 预计时间 |
|------|------|----------|
| 阶段一 | 准备工作 | 1 周 |
| 阶段二 | 基础功能迁移 | 2 周 |
| 阶段三 | 高级功能迁移 | 2 周 |
| 阶段四 | 集成与测试 | 2 周 |
| 阶段五 | 文档和示例 | 1 周 |
| 总计 | | 8 周 |

## 7. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| API 不兼容 | 现有代码需要大量修改 | 提供适配层和详细的迁移指南 |
| 性能退化 | 用户体验下降 | 进行性能测试和优化 |
| 功能缺失 | 用户无法使用某些功能 | 详细分析现有功能，确保全部迁移 |
| 测试覆盖不足 | 潜在的 bug | 增加测试覆盖率，进行全面测试 |

## 8. 结论

通过将 RAG 模块完全迁移到新的 vector store 架构，我们可以统一代码库，减少重复代码，提高性能和可扩展性。这将为用户提供更好的体验，并为未来的功能扩展奠定基础。

迁移计划分为五个阶段，预计需要 8 周时间完成。通过渐进式迁移和适配层设计，我们可以确保平稳过渡，最小化对现有代码的影响。

## 9. 附录

### 9.1 相关文件和模块

- `kastrax-rag`: 当前 RAG 模块
- `kastrax-store`: 新的 vector store 架构
- `kastrax-rag`: 新的 RAG 模块（待创建）

### 9.2 参考资料

- [Kastrax 向量存储优化计划](vector.md)
- [RAG 模块文档](kastrax-rag/README.md)
- [Vector Store 模块文档](kastrax-store/README.md)
