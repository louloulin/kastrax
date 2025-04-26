# KastraX RAG 示例

本目录包含了 KastraX 检索增强生成 (RAG) 系统的示例代码，展示了 RAG 系统的各种功能和用法。

## 主要功能

KastraX RAG 系统提供了以下主要功能：

1. **文档处理**
   - 文档加载和解析
   - 文本分块
   - 元数据提取

2. **向量嵌入**
   - 多种嵌入模型支持
   - 批量嵌入生成
   - 嵌入缓存

3. **向量存储**
   - 内存向量存储
   - 数据库向量存储
   - 分布式向量存储

4. **检索策略**
   - 相似度搜索
   - 混合搜索
   - 重排序和过滤

## 示例说明

### BasicRAGExample

`BasicRAGExample.kt` 展示了基本 RAG 系统的使用，包括：

- 加载和处理文档
- 生成向量嵌入
- 执行相似度搜索
- 增强 LLM 生成

### ChunkingExample

`ChunkingExample.kt` 展示了文档分块策略，包括：

- 固定大小分块
- 重叠分块
- 语义分块
- 分层分块

### EmbeddingExample

`EmbeddingExample.kt` 展示了向量嵌入功能，包括：

- 使用不同嵌入模型
- 批量嵌入生成
- 嵌入缓存
- 嵌入可视化

### HybridSearchExample

`HybridSearchExample.kt` 展示了混合搜索功能，包括：

- 关键词搜索
- 语义搜索
- 混合搜索策略
- 结果重排序

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.rag.BasicRAGExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.rag.ChunkingExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.rag.EmbeddingExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.rag.HybridSearchExampleKt"
```
