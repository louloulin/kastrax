# LanceDB 向量存储实现

## 1. 概述

LanceDB 是一个开源的、高性能的向量数据库，支持本地和远程部署。本文档介绍了 kastrax 中 LanceDB 向量存储的实现，包括基本功能、使用方法和性能优化。

## 2. 功能特点

LanceDB 向量存储实现具有以下特点：

1. **本地和远程连接**：支持使用本地文件系统路径或远程 HTTP/HTTPS URL 作为连接 URI。
2. **ANN 索引**：支持创建近似最近邻（ANN）索引，加速向量查询。
3. **批量处理**：支持批量添加向量，提高性能。
4. **元数据过滤**：支持基于元数据进行过滤查询。
5. **多种相似度度量**：支持余弦相似度、欧几里得距离和点积。

## 3. 使用方法

### 3.1 创建 LanceDB 向量存储

```kotlin
// 创建本地 LanceDB 向量存储
val localVectorStore = VectorStoreFactory.createLanceDBVectorStore("/path/to/lancedb")

// 创建远程 LanceDB 向量存储
val remoteVectorStore = VectorStoreFactory.createLanceDBVectorStore("https://lancedb.example.com")
```

### 3.2 创建索引

```kotlin
// 创建索引
val indexName = "example_index"
val dimension = 3
val created = vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)
```

### 3.3 添加向量

```kotlin
// 添加向量
val vectors = listOf(
    floatArrayOf(1f, 0f, 0f),
    floatArrayOf(0f, 1f, 0f),
    floatArrayOf(0f, 0f, 1f)
)
val metadata = listOf(
    mapOf("name" to "向量1", "category" to "A"),
    mapOf("name" to "向量2", "category" to "B"),
    mapOf("name" to "向量3", "category" to "A")
)
val ids = vectorStore.upsert(indexName, vectors, metadata)
```

### 3.4 查询向量

```kotlin
// 基本查询
val queryVector = floatArrayOf(1f, 0f, 0f)
val results = vectorStore.query(indexName, queryVector, 10)

// 带过滤条件的查询
val filteredResults = vectorStore.query(
    indexName = indexName,
    queryVector = queryVector,
    topK = 10,
    filter = mapOf("category" to "A")
)
```

### 3.5 创建 ANN 索引

```kotlin
// 创建 ANN 索引
val annCreated = vectorStore.createAnnIndex(
    indexName = indexName,
    indexType = "ivf_pq",
    params = mapOf("num_partitions" to 10, "num_sub_vectors" to 2)
)
```

### 3.6 批量添加向量

```kotlin
// 批量添加向量
val batchSize = 100
val ids = vectorStore.batchUpsert(indexName, vectors, metadata, batchSize = batchSize)
```

### 3.7 更新和删除向量

```kotlin
// 更新向量
val updated = vectorStore.updateVector(
    indexName = indexName,
    id = "vector_id",
    vector = floatArrayOf(0.5f, 0.5f, 0f),
    metadata = mapOf("name" to "更新的向量", "category" to "D")
)

// 删除向量
val deleted = vectorStore.deleteVectors(indexName, listOf("vector_id"))
```

### 3.8 获取索引信息

```kotlin
// 获取索引信息
val stats = vectorStore.describeIndex(indexName)
println("索引信息: 维度=${stats.dimension}, 向量数量=${stats.count}, 度量方式=${stats.metric}")
```

### 3.9 删除索引

```kotlin
// 删除索引
val indexDeleted = vectorStore.deleteIndex(indexName)
```

## 4. 性能优化

### 4.1 批量处理

使用 `batchUpsert` 方法批量添加向量，可以显著提高性能：

```kotlin
val batchSize = 100
val ids = vectorStore.batchUpsert(indexName, vectors, metadata, batchSize = batchSize)
```

### 4.2 ANN 索引

创建 ANN 索引可以显著提高查询性能，特别是对于大型数据集：

```kotlin
vectorStore.createAnnIndex(indexName, "ivf_pq", mapOf("num_partitions" to 10, "num_sub_vectors" to 2))
```

### 4.3 并发处理

LanceDB 向量存储实现使用 `ConcurrentHashMap` 和 `CopyOnWriteArrayList` 等并发集合，支持并发操作。

## 5. 注意事项

1. **内存管理**：对于大型数据集，应注意内存使用情况，可能需要调整 JVM 内存设置。

2. **元数据存储**：元数据以 JSON 格式存储，支持基本数据类型（字符串、数字、布尔值）和简单的列表。

3. **过滤器语法**：过滤条件使用简单的键值对格式，支持精确匹配。

4. **关闭资源**：使用完 `LanceDBVectorStore` 后，应调用 `close()` 方法释放资源。

## 6. 示例代码

以下是一个完整的示例，演示如何使用 LanceDB 向量存储：

```kotlin
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.VectorStoreFactory.createAnnIndex
import kotlinx.coroutines.runBlocking
import java.nio.file.Files

fun main() = runBlocking {
    // 创建临时目录
    val tempDir = Files.createTempDirectory("lancedb_example").toString()
    println("使用临时目录: $tempDir")

    try {
        // 创建 LanceDB 向量存储
        val vectorStore = VectorStoreFactory.createLanceDBVectorStore(tempDir)
        println("已创建 LanceDB 向量存储")

        // 创建索引
        val indexName = "example_index"
        val dimension = 3
        println("创建索引 $indexName，维度为 $dimension...")
        val created = vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)
        println("索引创建${if (created) "成功" else "失败"}")

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0.5f, 0.5f, 0f),
            floatArrayOf(0.3f, 0.3f, 0.3f)
        )
        val metadata = listOf(
            mapOf("name" to "向量1", "category" to "A"),
            mapOf("name" to "向量2", "category" to "B"),
            mapOf("name" to "向量3", "category" to "A"),
            mapOf("name" to "向量4", "category" to "B"),
            mapOf("name" to "向量5", "category" to "C")
        )
        println("添加 ${vectors.size} 个向量...")
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        println("向量添加成功，ID: $ids")

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        println("查询与 [1, 0, 0] 最相似的 3 个向量...")
        val results = vectorStore.query(indexName, queryVector, 3)
        println("查询结果:")
        results.forEachIndexed { index, result ->
            println("  ${index + 1}. ID: ${result.id}, 分数: ${result.score}, 元数据: ${result.metadata}")
        }

        // 使用过滤器查询
        println("查询类别为 'A' 的向量...")
        val filteredResults = vectorStore.query(
            indexName = indexName,
            queryVector = queryVector,
            topK = 10,
            filter = mapOf("category" to "A")
        )
        println("过滤查询结果:")
        filteredResults.forEachIndexed { index, result ->
            println("  ${index + 1}. ID: ${result.id}, 分数: ${result.score}, 元数据: ${result.metadata}")
        }

        // 创建 ANN 索引
        println("创建 ANN 索引...")
        val annCreated = vectorStore.createAnnIndex(
            indexName = indexName,
            indexType = "ivf_pq",
            params = mapOf("num_partitions" to 2, "num_sub_vectors" to 1)
        )
        println("ANN 索引创建${if (annCreated) "成功" else "失败"}")

        // 删除索引
        println("删除索引...")
        val indexDeleted = vectorStore.deleteIndex(indexName)
        println("索引删除${if (indexDeleted) "成功" else "失败"}")

    } finally {
        // 清理临时目录
        println("清理临时目录...")
        java.io.File(tempDir).deleteRecursively()
        println("示例运行完成")
    }
}
```

## 7. 总结

LanceDB 向量存储实现提供了一个高性能、易用的向量数据库解决方案，支持本地和远程部署。它与其他向量存储实现提供了一致的接口，使得用户可以轻松切换不同的向量数据库实现，同时利用 LanceDB 的特性。
