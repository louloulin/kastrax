# Kastrax 向量存储优化计划

> **实现状态**: 已完成基础架构和部分存储实现

## 1. 当前状态分析

### Mastra 向量存储实现

1. **架构**:
   - 使用基础抽象类 `MastraVector` 定义通用接口
   - 实现特定向量存储提供者 (Pinecone, Chroma, Qdrant, MongoDB 等)
   - 提供创建索引、更新向量和查询的一致接口

2. **主要特性**:
   - 支持多种向量数据库 (10+ 实现)
   - 所有实现具有一致的 API
   - 支持过滤、元数据存储和向量操作
   - 处理批量操作和维度验证
   - 包含遥测和跟踪支持

3. **接口方法**:
   - `createIndex`: 创建具有指定维度和度量的新索引
   - `upsert`: 向存储添加带有元数据的向量
   - `query`: 使用过滤选项搜索相似向量
   - `describeIndex`: 获取索引的统计信息
   - `deleteIndex`: 删除索引
   - `deleteVectors`: 删除特定向量

### Kastrax 向量存储实现

1. **架构**:
   - 使用 `RagVectorStore` 接口提供基本功能
   - 有 `EnhancedVectorStore` 接口提供高级功能
   - 实现特定提供者 (InMemoryVectorStore, FaissVectorStore 等)

2. **主要特性**:
   - 支持基本向量操作
   - 提供以文档为中心的 API (addDocument, getDocument)
   - 包括相似度搜索和关键词搜索
   - 支持元数据过滤

3. **接口方法**:
   - `addDocument`: 添加带有嵌入和元数据的文档
   - `getDocument`: 通过 ID 检索文档
   - `similaritySearch`: 搜索相似文档
   - `keywordSearch`: 使用关键词搜索
   - `metadataSearch`: 通过元数据过滤文档

## 2. 对比与优化点

1. **API 设计**:
   - Mastra 使用更以向量为中心的 API (vectors, metadata)
   - Kastrax 使用以文档为中心的 API (documents, embeddings)
   - **优化**: 提供两种方法以增加灵活性

2. **向量数据库支持**:
   - Mastra 支持 10+ 向量数据库
   - Kastrax 实现较少
   - **优化**: 扩展对更多向量数据库的支持

3. **高级功能**:
   - Mastra 具有更好的批处理和错误处理
   - Kastrax 具有更高级的搜索功能 (混合搜索)
   - **优化**: 结合两者的优势

4. **性能**:
   - Mastra 为大型操作实现批处理
   - Kastrax 缺乏优化的批处理
   - **优化**: 实现高效批处理

5. **可扩展性**:
   - Mastra 有更清晰的扩展模型
   - Kastrax 有更专业化的实现
   - **优化**: 创建更模块化的架构

## 3. 增强型向量存储实现计划

### 3.1 核心接口重构

1. **统一基础接口**:
   ```kotlin
   interface VectorStore {
       // 向量操作
       suspend fun createIndex(name: String, dimension: Int, metric: SimilarityMetric = SimilarityMetric.COSINE): Boolean
       suspend fun upsert(indexName: String, vectors: List<FloatArray>, metadata: List<Map<String, Any>> = emptyList(), ids: List<String>? = null): List<String>
       suspend fun query(indexName: String, queryVector: FloatArray, topK: Int = 10, filter: Map<String, Any>? = null, includeVectors: Boolean = false): List<QueryResult>
       suspend fun deleteVectors(indexName: String, ids: List<String>): Boolean
       suspend fun deleteIndex(indexName: String): Boolean
       suspend fun describeIndex(indexName: String): IndexStats

       // 文档操作 (兼容现有 API)
       suspend fun addDocument(document: String, embedding: FloatArray, metadata: Map<String, String> = emptyMap()): String
       suspend fun getDocument(id: String): RagDocument?
       suspend fun similaritySearch(query: String, embeddingService: EmbeddingService, limit: Int = 5, minScore: Double = 0.0): List<SearchResult>
   }
   ```

2. **高级接口扩展**:
   ```kotlin
   interface EnhancedVectorStore : VectorStore {
       suspend fun batchUpsert(indexName: String, vectors: List<FloatArray>, metadata: List<Map<String, Any>> = emptyList(), batchSize: Int = 100): List<String>
       suspend fun hybridSearch(query: String, embeddingService: EmbeddingService, keywords: List<String>, options: QueryOptions = QueryOptions()): List<SearchResult>
       suspend fun advancedQuery(indexName: String, queryVector: FloatArray, options: QueryOptions): List<QueryResult>
   }
   ```

### 3.2 新增向量存储实现

1. **优先实现列表**:
   - [✅] 内存向量存储 (InMemoryVectorStore)
   - [✅] Chroma
   - [✅] Qdrant
   - [✅] PostgreSQL/PgVector
   - [✅] Pinecone
   - [✅] MongoDB Atlas
   - [✅] LanceDB
   - [ ] Milvus
   - [ ] Weaviate
   - [ ] Redis
   - [ ] Elasticsearch/OpenSearch

2. **每个实现的标准结构**:
   ```kotlin
   class XxxVectorStore(
       // 配置参数
   ) : EnhancedVectorStore {
       // 连接管理
       private val client: XxxClient

       // 实现接口方法
       override suspend fun createIndex(...) { ... }
       override suspend fun upsert(...) { ... }
       // ...

       // 特定于实现的辅助方法
       private suspend fun validateConnection() { ... }
       private suspend fun optimizeBatchSize(count: Int): Int { ... }
   }
   ```

### 3.3 性能优化

1. **批处理优化**:
   - [✅] 实现自适应批处理大小
   - [✅] 添加并行处理选项
   - [ ] 优化大型向量集合的内存使用

2. **连接池管理**:
   - [✅] 实现连接池以重用数据库连接
   - [ ] 添加连接健康检查
   - [ ] 实现自动重连机制

3. **缓存策略**:
   - 实现索引元数据缓存
   - 添加热门查询结果缓存
   - 实现智能预取机制

### 3.4 高级功能

1. **混合搜索增强**:
   - [✅] 实现向量搜索和关键词搜索的可配置混合
   - [✅] 添加基于上下文的权重调整
   - [✅] 支持多查询向量融合

2. **过滤和排序**:
   - [✅] 增强元数据过滤能力
   - [✅] 添加后处理排序选项
   - [✅] 实现多阶段检索管道

3. **向量索引优化**:
   - 支持 HNSW、IVF、PQ 等索引类型
   - 添加索引参数自动调优
   - 实现增量索引更新

### 3.5 可观测性和管理

1. **监控和指标**:
   - [✅] 添加详细的性能指标收集
   - [✅] 实现查询延迟和吞吐量监控
   - [✅] 添加索引健康检查

2. **管理功能**:
   - 实现索引备份和恢复
   - 添加向量存储迁移工具
   - 提供索引重建和优化功能

3. **调试支持**:
   - 添加详细日志记录
   - 实现查询解释功能
   - 提供性能分析工具

## 4. 实现时间表

### 阶段 1: 核心重构 (1-2 周)
- 重新设计核心接口
- 更新现有实现以使用新接口
- 添加基本测试套件

### 阶段 2: 扩展支持 (2-4 周)
- 实现前 5 个优先向量存储
- 添加批处理和连接池优化
- 扩展测试覆盖范围

### 阶段 3: 高级功能 (2-3 周)
- 实现混合搜索增强
- 添加高级过滤和排序
- 实现向量索引优化

### 阶段 4: 可观测性和管理 (1-2 周)
- 添加监控和指标
- 实现管理功能
- 添加调试支持

### 阶段 5: 文档和示例 (1 周)
- 更新 API 文档
- 创建使用示例
- 编写性能优化指南

## 5. 存储方式扩展

### 5.1 本地存储选项

1. **文件系统向量存储**:
   - 使用 FAISS 或 Annoy 进行本地索引
   - 添加内存映射选项以处理大型索引
   - 实现增量更新支持

2. **嵌入式数据库**:
   - SQLite 与向量扩展
   - RocksDB 用于高性能键值存储
   - LevelDB 用于轻量级应用

3. **混合存储**:
   - 向量索引在内存中，数据在磁盘上
   - 热/冷数据分层存储
   - 实现自动缓存管理

### 5.2 云存储选项

1. **托管向量数据库**:
   - Pinecone
   - Qdrant Cloud
   - Weaviate Cloud
   - MongoDB Atlas Vector Search
   - Astra DB (DataStax)

2. **通用云数据库扩展**:
   - Amazon RDS PostgreSQL + pgvector
   - Google Cloud SQL + 向量扩展
   - Azure Database + 向量支持
   - Supabase 向量支持

3. **无服务器选项**:
   - Upstash Vector
   - Cloudflare Vectorize
   - Neon Postgres 向量支持

### 5.3 分布式存储选项

1. **分布式向量数据库**:
   - Milvus
   - Vespa
   - Elasticsearch/OpenSearch
   - Qdrant 集群

2. **自定义分布式解决方案**:
   - 分片向量索引
   - 复制和负载均衡
   - 分布式查询处理

## 6. 架构设计原则

1. **模块化**:
   - 核心接口与实现分离
   - 可插拔的存储后端
   - 可扩展的查询处理管道

2. **可配置性**:
   - 每个组件的详细配置选项
   - 基于环境的自动配置
   - 运行时可调整参数

3. **可测试性**:
   - 模拟和存根接口
   - 综合性能测试套件
   - 一致性和正确性验证

4. **可扩展性**:
   - 水平扩展支持
   - 资源使用优化
   - 负载管理策略

## 7. 总结

通过实施这一全面的优化计划，Kastrax 向量存储将显著增强其功能、性能和可扩展性。新的架构将支持更广泛的存储选项，同时保持一致的 API，使开发人员能够轻松切换不同的向量数据库实现。高级功能如混合搜索、批处理优化和监控将使 Kastrax 成为构建高性能 RAG 系统的强大基础。

这一计划的实施将使 Kastrax 在向量存储领域处于领先地位，为各种规模和复杂度的 AI 应用提供支持。

## 8. 实现进展

### 8.1 已实现功能

1. **核心架构**:
   - [✅] 新的 `VectorStore` 接口
   - [✅] `BaseVectorStore` 抽象基类
   - [✅] 与 RAG 模块的集成适配器
   - [✅] 向量存储工厂

2. **存储实现**:
   - [✅] 内存向量存储 (InMemoryVectorStore)
   - [✅] Chroma 向量存储
   - [✅] Qdrant 向量存储
   - [✅] PostgreSQL/PgVector 向量存储
   - [✅] Pinecone 向量存储
   - [✅] MongoDB Atlas 向量存储
   - [✅] LanceDB 向量存储

3. **性能优化**:
   - [✅] 批量处理
   - [✅] 并行查询

4. **测试**:
   - [✅] 内存向量存储测试

5. **高级功能**:
   - [✅] 混合搜索
   - [✅] 多阶段检索管道
   - [✅] 高级检索器
   - [✅] 多查询向量融合
   - [✅] 性能监控和指标收集
   - [✅] 索引健康检查
   - [✅] 向量存储备份和恢复
   - [✅] 告警和通知机制

6. **示例与文档**:
   - [✅] 基本向量存储示例
   - [✅] 混合搜索示例
   - [✅] 中文文档

### 8.2 已实现的迁移功能

1. **RAG 模块与向量存储集成**:
   - [✅] 创建了 StoreBackedVectorStore 类，将 RAG 模块与向量存储模块集成
   - [✅] 实现了 RagVectorStoreFactory 工厂类，用于创建各种类型的向量存储
   - [✅] 更新了 Retriever 接口和实现类，使其返回 RagDocument 而不是 SearchResult

2. **检索器增强**:
   - [✅] 实现了 VectorStoreRetriever 类，使用向量存储进行检索
   - [✅] 更新了 HybridRetriever 类，使其返回 RagDocument
   - [✅] 创建了 RetrieverFactory 工厂类，用于创建各种类型的检索器

3. **示例与测试**:
   - [✅] 添加了 VectorStoreExample 示例，展示如何使用新的向量存储和检索器
   - [✅] 添加了测试类，验证新功能的正确性

### 8.3 新增功能

1. **Pinecone 向量存储**:
   - [✅] 实现 Pinecone 向量存储
   - [✅] 添加单元测试
   - [✅] 添加集成测试

2. **多查询向量融合**:
   - [✅] 实现加权融合策略
   - [✅] 实现最大分数融合策略
   - [✅] 实现平均分数融合策略
   - [✅] 实现递归融合策略

3. **监控和指标收集**:
   - [✅] 实现性能指标收集
   - [✅] 添加操作记录和跟踪
   - [✅] 实现指标报告

### 8.4 健康检查功能

1. **索引健康检查**:
   - [✅] 实现向量存储健康检查
   - [✅] 实现索引健康检查
   - [✅] 添加健康状态报告

2. **MongoDB 向量存储**:
   - [✅] 实现 MongoDB Atlas 向量存储
   - [✅] 添加单元测试
   - [✅] 添加集成测试

### 8.5 备份和告警功能

1. **备份和恢复**:
   - [✅] 实现向量存储备份
   - [✅] 实现向量存储恢复
   - [✅] 支持选择性备份和恢复

2. **告警和通知**:
   - [✅] 实现告警管理器
   - [✅] 支持自定义告警规则
   - [✅] 实现通知处理器

### 8.6 LanceDB 向量存储

1. **LanceDB 实现**:
   - [✅] 基于官方 Java 客户端实现
   - [✅] 支持本地和远程连接
   - [✅] 支持 ANN 索引创建
   - [✅] 支持批量处理
   - [✅] 支持元数据过滤
   - [✅] 添加单元测试

### 8.7 下一步计划

1. 完成其他向量存储实现 (Milvus, Weaviate 等)
2. 添加更多测试用例
3. 实现分布式向量存储
4. 添加更多告警规则和通知渠道
5. 完善文档和示例
