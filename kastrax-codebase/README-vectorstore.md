# 向量存储与检索优化

本模块实现了代码库的向量存储与检索优化功能，用于高效存储和检索代码嵌入向量，以支持快速相似度搜索和语义理解。

## 功能特点

- **代码特化的向量存储**：针对代码文件优化的向量存储，提高检索效率和准确性
- **多租户索引共享**：支持多个租户共享索引，优化内存使用
- **向量索引分片**：实现向量索引分片和分布式存储，提高可扩展性
- **索引压缩技术**：减少存储空间需求，同时保持检索质量
- **高效相似度搜索**：优化相似度搜索算法，提高检索效率

## 架构设计

向量存储与检索优化模块由以下组件组成：

1. **代码向量存储 (CodeVectorStore)**：
   - 提供代码特化的向量存储
   - 支持高效的向量检索
   - 优化相似度计算

2. **多租户向量存储 (MultiTenantVectorStore)**：
   - 支持多个租户共享索引
   - 实现租户隔离
   - 提供租户级别的资源管理
   - 支持 LRU 和 LFU 驱逐策略

3. **分片向量存储 (ShardedVectorStore)**：
   - 实现向量索引分片
   - 支持分布式存储
   - 提供副本机制，增强可用性
   - 支持不同的一致性级别

4. **压缩向量存储 (CompressedVectorStore)**：
   - 实现向量压缩技术
   - 支持标量量化、乘积量化和二值化
   - 减少存储空间需求
   - 保持检索质量

## 使用示例

```kotlin
// 创建基础向量存储
val baseVectorStore = VectorStoreFactory.createInMemoryVectorStore()

// 创建代码向量存储
val codeVectorStore = CodeVectorStore(
    baseVectorStore = baseVectorStore,
    config = CodeVectorStoreConfig(
        maxVectors = 10000,
        dimension = embeddingService.dimension,
        distanceThreshold = 0.6
    )
)

// 添加向量
val id = codeVectorStore.addVector(embedding, metadata)

// 搜索向量
val results = codeVectorStore.searchVector(queryEmbedding, limit = 3)

// 创建多租户向量存储
val multiTenantStore = MultiTenantVectorStore(
    baseVectorStoreFactory = { tenantId ->
        VectorStoreFactory.createInMemoryVectorStore()
    },
    config = MultiTenantVectorStoreConfig(
        maxTenantsInMemory = 3,
        maxVectorsPerTenant = 1000,
        evictionStrategy = EvictionStrategy.LRU
    )
)

// 添加向量到特定租户
val id = multiTenantStore.addVector("tenant1", embedding, metadata)

// 搜索特定租户的向量
val results = multiTenantStore.searchVector("tenant1", queryEmbedding, limit = 3)
```

## 向量压缩技术

压缩向量存储支持以下压缩方法：

1. **标量量化**：
   - 将每个浮点数量化为 8 位整数
   - 保存每个维度的最小值和最大值
   - 压缩率约为 4:1

2. **乘积量化**：
   - 将向量分成子向量
   - 对每个子向量应用 K-means 聚类
   - 存储聚类中心索引
   - 压缩率约为 8:1 到 32:1

3. **二值化**：
   - 将每个浮点数转换为 1 位（0 或 1）
   - 基于向量均值进行二值化
   - 压缩率约为 32:1

## 分片和分布式存储

分片向量存储支持以下功能：

1. **分片策略**：
   - 基于向量数量的分片
   - 自动负载均衡

2. **副本机制**：
   - 支持多个副本
   - 提高可用性和读取性能

3. **一致性级别**：
   - ANY：任意一个副本成功即可
   - QUORUM：大多数副本成功
   - ALL：所有副本成功

## 多租户索引共享

多租户向量存储支持以下功能：

1. **租户隔离**：
   - 每个租户有独立的索引
   - 支持租户级别的操作

2. **内存优化**：
   - 限制内存中的租户数量
   - 支持租户驱逐策略

3. **驱逐策略**：
   - LRU：最近最少使用
   - LFU：最少使用

## 性能优化

向量存储与检索优化模块通过以下方式优化性能：

1. **相似度计算优化**：
   - 使用余弦相似度
   - 支持提前终止

2. **批处理**：
   - 支持批量添加和检索
   - 减少操作开销

3. **并行处理**：
   - 使用协程实现并行处理
   - 优化 I/O 操作，减少阻塞

4. **内存管理**：
   - 限制向量数量
   - 支持自动清理
