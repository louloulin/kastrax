# 高性能嵌入生成

本模块实现了代码库的高性能嵌入生成功能，用于将代码文件转换为高质量的嵌入向量，以便进行相似度搜索和语义理解。

## 功能特点

- **代码特化的嵌入服务**：针对代码文件优化的嵌入生成，提高代码理解能力
- **语义感知的代码分块**：根据代码结构（类、方法、函数等）进行智能分块，保留语义完整性
- **嵌入模型版本管理**：支持多个嵌入模型版本，实现平滑升级
- **高效缓存机制**：缓存嵌入结果，避免重复计算，提高性能
- **批处理优化**：支持批量嵌入生成，提高吞吐量

## 架构设计

高性能嵌入生成模块由以下组件组成：

1. **代码嵌入服务 (CodeEmbeddingService)**：
   - 提供代码特化的嵌入生成
   - 实现代码预处理（移除注释、规范化空白字符等）
   - 提供嵌入缓存机制

2. **代码分块器 (CodeChunker)**：
   - 实现语义感知的代码分块
   - 支持多种编程语言（Java、Kotlin、Python、JavaScript 等）
   - 保留代码的语义完整性

3. **嵌入模型管理器 (EmbeddingModelManager)**：
   - 管理多个嵌入模型版本
   - 支持平滑升级（逐渐过渡到新版本）
   - 提供版本特定的嵌入缓存

## 使用示例

```kotlin
// 创建基础嵌入服务
val baseEmbeddingService = FastEmbeddingService.create()

// 创建代码嵌入服务
val codeEmbeddingService = CodeEmbeddingService(
    baseEmbeddingService = baseEmbeddingService,
    config = CodeEmbeddingServiceConfig(
        cacheSize = 1000,
        batchSize = 32
    )
)

// 创建嵌入模型管理器
val modelManager = EmbeddingModelManager(
    config = EmbeddingModelManagerConfig(
        defaultVersion = "v1",
        cacheSize = 1000
    )
)

// 注册模型版本
modelManager.registerModelVersion("v1", codeEmbeddingService, true)

// 创建代码分块器
val chunker = CodeChunker(
    config = CodeChunkerConfig(
        maxChunkSize = 1000,
        minChunkSize = 100,
        overlap = 50,
        preserveSemantics = true
    )
)

// 分割代码文件
val chunks = chunker.chunkFile(filePath)

// 生成嵌入
val embeddings = chunks.map { chunk ->
    chunk to modelManager.embed(chunk.content)
}

// 计算相似度
val queryEmbedding = modelManager.embed(queryText)
val similarities = embeddings.map { (chunk, embedding) ->
    chunk to cosineSimilarity(queryEmbedding, embedding)
}

// 获取最相似的块
val topSimilar = similarities.sortedByDescending { it.second }.take(3)
```

## 代码分块策略

代码分块器支持以下分块策略：

1. **语义感知分块**：
   - 根据代码结构（类、方法、函数等）进行分块
   - 保留代码的语义完整性
   - 支持多种编程语言

2. **简单分块**：
   - 基于固定大小的分块
   - 支持重叠，确保上下文连续性

语义感知分块针对不同编程语言使用不同的策略：

- **JVM 语言（Java、Kotlin、Scala）**：
  - 按类分块，大类进一步按方法分块
  - 使用正则表达式匹配类和方法定义
  - 使用花括号匹配找到代码块的结束位置

- **Python**：
  - 按类分块，大类进一步按函数分块
  - 使用缩进级别确定代码块的结束位置

- **JavaScript/TypeScript**：
  - 按类、函数和箭头函数分块
  - 支持多种函数定义语法

## 嵌入模型版本管理

嵌入模型管理器支持以下功能：

1. **多版本管理**：
   - 注册多个嵌入模型版本
   - 设置活动版本
   - 获取特定版本的嵌入

2. **平滑升级**：
   - 设置过渡期，在此期间逐渐从旧版本过渡到新版本
   - 根据过渡进度动态选择版本
   - 过渡期结束后自动更新活动版本

3. **版本特定的缓存**：
   - 为每个版本维护单独的嵌入缓存
   - 避免版本升级时的性能下降

## 性能优化

高性能嵌入生成模块通过以下方式优化性能：

1. **嵌入缓存**：
   - 缓存嵌入结果，避免重复计算
   - 支持缓存过期和自动清理

2. **批处理**：
   - 支持批量嵌入生成，减少 API 调用次数
   - 自动将大批量拆分为小批量，避免内存溢出

3. **代码预处理**：
   - 移除注释和不必要的字符
   - 规范化空白字符
   - 减少嵌入模型的输入大小

4. **并行处理**：
   - 使用协程实现并行处理
   - 优化 I/O 操作，减少阻塞
