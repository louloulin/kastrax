# LanceDB 向量存储实现

## 1. 概述

本文档介绍了 kastrax 中 LanceDB 向量存储的实现。LanceDB 是一个开源的、高性能的向量数据库，支持本地和远程部署。我们基于 LanceDB 的官方 Java 客户端实现了 `LanceDBVectorStore` 类，提供了与其他向量存储一致的接口，同时利用了 LanceDB 的特性，如 ANN 索引。

## 2. 实现细节

### 2.1 依赖

LanceDB 向量存储实现依赖于以下库：

```kotlin
// LanceDB Java Client
implementation("com.lancedb:lance-core:0.18.0")
```

### 2.2 核心类

`LanceDBVectorStore` 类是 LanceDB 向量存储的核心实现，它继承自 `BaseVectorStore` 抽象类，并实现了所有必要的方法。

```kotlin
class LanceDBVectorStore(
    private val uri: String
) : BaseVectorStore() {
    // 实现代码...
}
```

### 2.3 连接管理

`LanceDBVectorStore` 支持两种连接模式：

1. **本地连接**：使用本地文件系统路径作为 URI
2. **远程连接**：使用 HTTP/HTTPS URL 作为 URI

```kotlin
private val connection: Connection by lazy {
    if (uri.startsWith("http://") || uri.startsWith("https://")) {
        // 远程连接
        LanceDB.connect(uri)
    } else {
        // 本地连接
        val directory = File(uri)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        LanceDB.connect(directory.absolutePath)
    }
}
```

### 2.4 数据模型

LanceDB 使用 Apache Arrow 作为数据模型，我们需要将 Kastrax 的数据模型转换为 Arrow 格式：

```kotlin
// 创建 Arrow 向量
val idVector = VarCharVector("id", allocator)
val vectorVector = ListVector("vector", allocator, FieldType(true, ArrowType.List.INSTANCE, null), null)
val metadataVector = VarCharVector("metadata", allocator)

// 填充数据
for (i in vectors.indices) {
    // ID
    idVector.setSafe(i, vectorIds[i].toByteArray(StandardCharsets.UTF_8))

    // 向量
    val vector = vectors[i]
    vectorVector.startNewValue(i)
    for (j in vector.indices) {
        vectorVector.setSafe(vectorVector.valueCount, vector[j])
    }
    vectorVector.endValue(i, vector.size)

    // 元数据
    val metadataJson = normalizedMetadata[i].entries.joinToString(",", "{", "}") { (key, value) ->
        "\"$key\":\"$value\""
    }
    metadataVector.setSafe(i, metadataJson.toByteArray(StandardCharsets.UTF_8))
}
```

### 2.5 查询和过滤

LanceDB 支持向量相似度查询和基于 SQL 的过滤：

```kotlin
// 构建查询
var query = table.query()
    .nearest("vector", queryVector)
    .limit(topK)

// 添加过滤条件
if (filter != null && filter.isNotEmpty()) {
    val filterStr = buildString {
        filter.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) append(" AND ")
            when (value) {
                is String -> append("json_extract(metadata, '$.$key') = '$value'")
                is Number -> append("json_extract(metadata, '$.$key') = $value")
                is Boolean -> append("json_extract(metadata, '$.$key') = ${value.toString().lowercase()}")
                is List<*> -> {
                    append("json_extract(metadata, '$.$key') IN (")
                    value.forEachIndexed { i, item ->
                        if (i > 0) append(", ")
                        when (item) {
                            is String -> append("'$item'")
                            else -> append("$item")
                        }
                    }
                    append(")")
                }
                else -> append("json_extract(metadata, '$.$key') = '${value}'")
            }
        }
    }
    query = query.filter(filterStr)
}
```

### 2.6 ANN 索引

LanceDB 支持创建 ANN（近似最近邻）索引，以加速向量查询：

```kotlin
suspend fun createAnnIndex(
    indexName: String,
    indexType: String = "ivf_pq",
    params: Map<String, Any> = emptyMap()
): Boolean = withContext(Dispatchers.IO) {
    try {
        // 获取表
        val table = connection.openTable(indexName)

        // 构建索引参数
        val indexParams = mutableMapOf<String, Any>()
        indexParams["type"] = indexType
        indexParams.putAll(params)

        // 创建索引
        table.createIndex("vector", indexParams)

        logger.debug { "Created ANN index for table $indexName" }
        return@withContext true
    } catch (e: Exception) {
        logger.error(e) { "Error creating ANN index for table $indexName" }
        throw e
    }
}
```

## 3. 使用示例

### 3.1 创建 LanceDB 向量存储

```kotlin
// 创建本地 LanceDB 向量存储
val localVectorStore = VectorStoreFactory.createLanceDBVectorStore("/path/to/lancedb")

// 创建远程 LanceDB 向量存储
val remoteVectorStore = VectorStoreFactory.createLanceDBVectorStore("https://lancedb.example.com")
```

### 3.2 创建索引和添加向量

```kotlin
// 创建索引
val indexName = "example_index"
val dimension = 3
vectorStore.createIndex(indexName, dimension)

// 添加向量
val vectors = listOf(
    floatArrayOf(1f, 0f, 0f),
    floatArrayOf(0f, 1f, 0f),
    floatArrayOf(0f, 0f, 1f)
)
val metadata = listOf(
    mapOf("name" to "vector1", "category" to "A"),
    mapOf("name" to "vector2", "category" to "B"),
    mapOf("name" to "vector3", "category" to "A")
)
val ids = vectorStore.upsert(indexName, vectors, metadata)
```

### 3.3 查询向量

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

### 3.4 创建 ANN 索引

```kotlin
// 创建 IVF-PQ 索引
vectorStore.createAnnIndex(
    indexName = indexName,
    indexType = "ivf_pq",
    params = mapOf(
        "num_partitions" to 10,
        "num_sub_vectors" to 2
    )
)
```

## 4. 性能优化

### 4.1 批量处理

`LanceDBVectorStore` 支持批量添加向量，以提高性能：

```kotlin
val batchSize = 100
val ids = vectorStore.batchUpsert(indexName, vectors, metadata, batchSize = batchSize)
```

### 4.2 ANN 索引

创建 ANN 索引可以显著提高查询性能，特别是对于大型数据集：

```kotlin
vectorStore.createAnnIndex(indexName, "ivf_pq", mapOf("num_partitions" to 10, "num_sub_vectors" to 2))
```

## 5. 注意事项

1. **内存管理**：LanceDB 使用 Apache Arrow，需要手动管理内存。在 `LanceDBVectorStore` 中，我们确保在操作完成后释放 Arrow 向量的内存。

2. **元数据存储**：LanceDB 不直接支持复杂的元数据结构，我们将元数据序列化为 JSON 字符串存储，并在查询时解析。

3. **过滤器语法**：LanceDB 使用 SQL 语法进行过滤，我们将 Kastrax 的过滤条件转换为 LanceDB 支持的 SQL 语法。

4. **关闭资源**：使用完 `LanceDBVectorStore` 后，应调用 `close()` 方法释放资源。

## 6. 总结

`LanceDBVectorStore` 提供了与 LanceDB 的无缝集成，支持本地和远程连接、ANN 索引、批量处理等功能。它与其他向量存储实现提供了一致的接口，使得用户可以轻松切换不同的向量数据库实现，同时利用 LanceDB 的特性。
