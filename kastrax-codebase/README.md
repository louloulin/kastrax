# KastraX Codebase

KastraX Codebase 是 KastraX 框架的代码库理解模块，提供类似 Augment 和 Cursor 的代码库索引和理解能力。

## 功能特点

### 代码库索引

- **实时文件系统监控**：实时检测文件创建、修改和删除
- **Git 分支切换检测**：识别 Git 分支变更，支持分支切换时的索引更新
- **智能文件过滤**：排除不需要索引的文件，如二进制文件、临时文件等
- **增量更新机制**：只处理变更的文件，提高索引效率
- **批量处理能力**：高效处理大量文件变更
- **优先级任务调度**：根据任务类型和重要性分配处理资源
- **可扩展的索引处理器**：支持自定义索引处理逻辑

### 代码理解

- **多语言代码解析**：支持 Java、Kotlin、Python、TypeScript/JavaScript 和 Go 等多种编程语言
- **代码语义分析**：深度理解代码结构和语义，构建符号关系图
- **控制流和数据流分析**：分析代码的控制流和数据流，理解代码逻辑

### 上下文引擎

- **统一的上下文引擎接口**：提供统一的上下文检索和构建能力
- **多级上下文构建**：支持文件、类、方法等多个级别的上下文构建
- **基于向量的语义检索**：使用向量存储进行高效的语义相似度检索
- **上下文感知的检索引擎**：根据当前编辑位置和查询意图提供相关上下文

## 模块结构

- **filesystem**：文件系统监控和文件过滤
- **git**：Git 分支监控
- **indexing**：增量索引和批处理
- **semantic**：代码语义分析和符号关系图
- **flow**：代码控制流和数据流分析
- **vector**：代码向量存储和检索
- **context**：上下文构建和管理
- **engine**：上下文引擎实现
- **examples**：使用示例

## 快速开始

### 上下文引擎用法

```kotlin
// 创建向量存储
val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

// 创建嵌入服务
val embeddingService = EmbeddingServiceFactory.createEmbeddingService()

// 创建上下文引擎
val contextEngine = ContextEngineImpl.create(
    rootPath = Path("/path/to/your/project"),
    vectorStore = vectorStore,
    embeddingService = embeddingService,
    config = ContextEngineConfig(
        enableFileSystemMonitoring = true,
        enableGitMonitoring = true,
        enableIncrementalIndexing = true
    )
)

// 监听事件
launch {
    (contextEngine as? ContextEngineImpl)?.events?.collect { event ->
        println("[事件] ${event.type}: ${event.message}")
    }
}

// 索引代码库
contextEngine.indexCodebase(Path("/path/to/your/project"))

// 获取查询上下文
val context = contextEngine.getQueryContext("文件系统监控", 10, 0.0)

// 获取文件上下文
val fileContext = contextEngine.getFileContext(Path("/path/to/your/file.kt"), 10)

// 获取符号上下文
val symbolContext = contextEngine.getSymbolContext("CodeElement", 10, 0.0)

// 关闭上下文引擎
contextEngine.close()
```

### 索引管理器用法

```kotlin
// 创建索引任务处理器
val indexTaskProcessor = SimpleIndexTaskProcessor()

// 创建索引管理器
val indexManager = CodebaseIndexManager(
    rootPath = Path("/path/to/your/project"),
    config = CodebaseIndexManagerConfig(),
    indexTaskProcessor = indexTaskProcessor
)

// 启动索引管理器
indexManager.start()

// 监听索引事件
launch {
    indexManager.indexEvents.collect { event ->
        when (event) {
            is CodebaseIndexEvent.StatusChanged -> println("状态变更: ${event.status}")
            is CodebaseIndexEvent.Progress -> println("进度: ${event.current}/${event.total}")
            is CodebaseIndexEvent.Error -> println("错误: ${event.error}")
        }
    }
}

// 请求重新索引
indexManager.requestReindex()

// 停止索引管理器
indexManager.stop()
```

### 自定义索引处理器

```kotlin
class MyIndexTaskProcessor : IndexTaskProcessor {
    override suspend fun processTask(task: IndexTask) {
        when (task.type) {
            IndexTaskType.ADD, IndexTaskType.UPDATE -> {
                // 处理文件添加或更新
                val content = task.path.readText()
                // 解析代码、生成嵌入向量、存储到向量数据库等
            }
            IndexTaskType.DELETE -> {
                // 处理文件删除
                // 从向量数据库中删除相关嵌入向量
            }
            IndexTaskType.BRANCH_CHANGE -> {
                // 处理分支变更
                val previousBranch = task.metadata["previousBranch"]
                val currentBranch = task.metadata["currentBranch"]
                // 切换到新分支的索引
            }
            IndexTaskType.FULL_REINDEX -> {
                // 处理完全重新索引
                // 遍历所有文件并重新索引
            }
        }
    }
}
```

## 配置选项

### 文件系统监控配置

```kotlin
val fileSystemMonitorConfig = FileSystemMonitorConfig(
    excludePatterns = setOf(Regex("\\.git/.*"), Regex("build/.*")),
    excludeExtensions = setOf("class", "jar", "zip"),
    excludeDirectories = setOf(".git", "build", "node_modules"),
    pollIntervalMs = 1000
)
```

### Git 分支监控配置

```kotlin
val gitBranchMonitorConfig = GitBranchMonitorConfig(
    pollIntervalSeconds = 5
)
```

### 文件过滤配置

```kotlin
val fileFilterConfig = FileFilterConfig(
    includeExtensions = setOf("java", "kt", "py", "js"),
    excludeBinaryFiles = true,
    maxFileSizeBytes = 1024 * 1024 // 1MB
)
```

### 增量索引器配置

```kotlin
val incrementalIndexerConfig = IncrementalIndexerConfig(
    batchSize = 100,
    maxQueueSize = 10000,
    deduplicationWindowMs = 1000
)
```

### 批处理器配置

```kotlin
val batchProcessorConfig = BatchProcessorConfig(
    maxConcurrentBatches = 3,
    maxTasksPerBatch = 1000,
    maxRetries = 3
)
```

## 下一步计划

- 增强代码解析的准确性和完整性
- 支持更多编程语言（如 Rust、C/C++、C#）
- 实现更高级的混合检索策略
- 优化索引性能，支持每秒处理数千个文件
- 实现代码特化的嵌入模型
- 实现语义记忆增强检索 (SEM-RAG)
- 添加多租户支持
- 实现与 IDE 的集成

## 贡献

欢迎贡献代码、报告问题或提出改进建议。请参阅 [CONTRIBUTING.md](../CONTRIBUTING.md) 了解更多信息。

## 许可证

KastraX Codebase 使用与 KastraX 框架相同的许可证。
