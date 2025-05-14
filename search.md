# Kastrax 代码搜索功能实现计划

## 1. 概述

本文档提供了在 Kastrax 中实现高性能代码搜索功能的详细计划。该功能将基于 ripgrep 实现，并结合 Augment 和 Cursor 等工具的最佳实践，为用户提供快速、准确且功能丰富的代码搜索体验。

## 2. 背景分析

### 2.1 现有工具分析

#### 2.1.1 ripgrep

ripgrep 是目前最快的文本搜索工具之一，具有以下特点：

- 使用 Rust 编写，性能卓越
- 支持完整的 Unicode 搜索
- 默认遵循 .gitignore 规则
- 支持多种搜索模式（字面量、正则表达式等）
- 使用 SIMD 加速的 Teddy 算法进行多模式搜索
- 支持增量搜索，无需将整个文件加载到内存
- 支持并行搜索

#### 2.1.2 AutoDev 搜索实现

AutoDev 的搜索功能主要基于 ripgrep，具有以下特点：

- 通过 `RipgrepSearcher` 类封装 ripgrep 命令行工具
  - 使用 `--json` 参数获取结构化输出
  - 支持上下文展示（`--context` 参数）
  - 支持正则表达式搜索（`-e` 参数）
  - 支持文件模式过滤（`--glob` 参数）
- 使用 `RipgrepOutputProcessor` 解析 JSON 输出
  - 处理 match 和 context 类型的输出
  - 提取文件路径、行号、匹配内容等信息
  - 支持前后上下文的收集
- 提供多种搜索命令实现
  - `RipgrepSearchInsCommand` 用于 AI 助手集成
  - `LocalSearchInsCommand` 用于本地搜索
- 支持文件系统遍历和过滤
  - `PatternSearcher` 用于基于正则表达式的文件查找
  - 支持文件缓存以提高性能
- 提供向量搜索功能
  - `EmbeddingSearchIndex` 接口定义向量搜索能力
  - `InMemoryEmbeddingSearchIndex` 提供内存中的向量搜索
  - `DiskSynchronizedEmbeddingSearchIndex` 提供磁盘同步的向量搜索
  - 支持余弦相似度计算和向量归一化

#### 2.1.3 Augment 搜索实现

Augment 的上下文引擎使用复杂的搜索策略：

- 结合语义搜索和关键词搜索
- 使用向量嵌入进行相似度搜索
- 支持代码结构感知搜索
- 提供相关性排序
- 集成 ripgrep 进行快速文本搜索

#### 2.1.4 Cursor 搜索实现

Cursor 的搜索功能结合了多种技术：

- 使用 ripgrep 进行基础文本搜索
- 支持语义搜索和符号搜索
- 提供智能上下文感知搜索
- 集成代码理解功能
- 支持多种文件类型和编程语言

### 2.2 Kastrax 现有搜索功能

Kastrax 目前已经实现了一些搜索相关的组件：

- `KeywordSearcher`：基于关键词的搜索
- `CodeSearchService`：提供代码搜索服务
- `HybridRetriever`：结合向量搜索和关键词搜索
- `CodeVectorStore`：存储代码元素的向量表示

## 3. 需求分析

### 3.1 功能需求

1. **基础搜索功能** [已实现]
   - 支持字面量搜索 [已实现]
   - 支持正则表达式搜索 [已实现]
   - 支持大小写敏感/不敏感搜索 [已实现]
   - 支持全词匹配 [已实现]
   - 支持多模式搜索（OR 搜索） [已实现]

2. **高级搜索功能** [已实现]
   - 支持语义搜索 [已实现]
   - 支持符号搜索（类、方法、变量等） [已实现]
   - 支持结构感知搜索 [已实现]
   - 支持上下文感知搜索 [已实现]
   - 支持代码流分析结果搜索 [已实现]

3. **过滤功能** [已实现]
   - 支持文件类型过滤 [已实现]
   - 支持目录过滤 [已实现]
   - 支持 .gitignore 规则 [已实现]
   - 支持自定义过滤规则 [已实现]
   - 支持排除二进制文件和隐藏文件 [已实现]

4. **结果展示** [已实现]
   - 支持高亮显示匹配结果 [已实现]
   - 支持显示上下文 [已实现]
   - 支持结果分组 [已实现]
   - 支持结果排序 [已实现]
   - 支持结果预览 [已实现]

5. **性能需求** [已实现]
   - 支持大型代码库的快速搜索 [已实现]
   - 支持增量搜索 [已实现]
   - 支持并行搜索 [已实现]
   - 支持搜索结果缓存 [已实现]
   - 支持实时搜索 [已实现]

### 3.2 非功能需求

1. **可扩展性**
   - 支持添加新的搜索算法
   - 支持添加新的过滤规则
   - 支持添加新的结果展示方式

2. **可用性**
   - 提供简单易用的 API
   - 提供详细的文档
   - 提供丰富的示例

3. **可测试性**
   - 提供单元测试
   - 提供集成测试
   - 提供性能测试

## 4. 架构设计

### 4.1 整体架构

```
+---------------------------+
|      SearchFacade         |
+---------------------------+
            |
            v
+---------------------------+
|     SearchCoordinator     |
+---------------------------+
            |
    +-------+-------+
    |               |
    v               v
+----------+  +------------+
| Searcher |  | Processor  |
+----------+  +------------+
    |               |
    v               v
+----------+  +------------+
| Provider |  |  Renderer  |
+----------+  +------------+
```

### 4.2 核心组件

1. **SearchFacade**
   - 提供统一的搜索入口
   - 处理搜索请求参数
   - 协调各个搜索组件

2. **SearchCoordinator**
   - 管理搜索流程
   - 分发搜索任务
   - 收集搜索结果

3. **Searcher**
   - 实现具体的搜索算法
   - 支持多种搜索模式
   - 处理搜索过滤

4. **Provider**
   - 提供搜索数据源
   - 支持文件系统、内存、数据库等

5. **Processor**
   - 处理搜索结果
   - 实现结果过滤、排序、分组等

6. **Renderer**
   - 渲染搜索结果
   - 支持多种展示方式

### 4.3 搜索引擎集成

1. **RipgrepSearcher**
   - 封装 ripgrep 命令行工具
   - 处理 ripgrep 输出
   - 支持 ripgrep 的各种选项

2. **VectorSearcher**
   - 实现向量搜索
   - 支持语义相似度搜索
   - 集成现有的 `CodeVectorStore`

3. **HybridSearcher**
   - 结合文本搜索和向量搜索
   - 实现搜索结果融合
   - 提供相关性排序

## 5. AutoDev ripgrep 实现分析

基于对 AutoDev 代码的分析，我们可以看到其 ripgrep 实现的主要组件和工作原理：

### 5.1 RipgrepSearcher 实现

AutoDev 的 `RipgrepSearcher` 类是对 ripgrep 命令行工具的封装，主要功能包括：

1. **查找 ripgrep 二进制文件**
   - 通过静态方法 `findRipgrepBinary()` 实现
   - 支持多平台（Windows、macOS、Linux）
   - 检查环境变量和常见安装位置

2. **构建 ripgrep 命令**
   - 支持多种搜索选项（大小写敏感、正则表达式等）
   - 使用 `--json` 参数获取结构化输出
   - 支持文件类型过滤和文件模式过滤

3. **执行搜索命令**
   - 使用 `ProcessBuilder` 执行命令
   - 支持异步执行和结果收集
   - 提供进度监控和错误处理

4. **处理搜索结果**
   - 使用 `RipgrepOutputProcessor` 解析 JSON 输出
   - 支持异步结果收集
   - 提供格式化的搜索结果

代码示例：

```kotlin
public object RipgrepSearcher {
    private val logger = logger<RipgrepSearcher>()

    fun searchFiles(
        project: Project,
        directory: String,
        searchText: String,
        filePattern: String? = null,
        options: Map<String, String> = emptyMap()
    ): CompletableFuture<String?> {
        val rgPath = findRipgrepBinary() ?: return CompletableFuture.completedFuture(null)

        val command = mutableListOf(rgPath.toString())
        command.add("--json")

        // Add options
        if (options.containsKey("--context")) {
            command.add("--context")
            command.add(options["--context"]!!)
        }

        // Add search pattern
        command.add("-e")
        command.add(searchText)

        // Add file pattern if provided
        if (filePattern != null) {
            command.add("--glob")
            command.add(filePattern)
        }

        // Add directory to search
        command.add(directory)

        return executeCommand(project, command)
    }

    // ... other methods
}
```

### 5.2 RipgrepOutputProcessor 实现

AutoDev 的 `RipgrepOutputProcessor` 类负责解析 ripgrep 的 JSON 输出，主要功能包括：

1. **解析 JSON 输出**
   - 处理 `match` 类型的输出（匹配结果）
   - 处理 `context` 类型的输出（上下文行）
   - 支持增量解析（处理不完整的 JSON）

2. **构建搜索结果**
   - 创建 `RipgrepSearchResult` 对象
   - 收集文件路径、行号、匹配内容等信息
   - 收集前后上下文行

3. **结果管理**
   - 维护当前处理的搜索结果
   - 支持多个结果的收集
   - 提供结果获取接口

代码示例：

```kotlin
class RipgrepOutputProcessor : ProcessAdapter() {
    private val results: MutableList<RipgrepSearchResult> = ArrayList<RipgrepSearchResult>()
    private var currentResult: RipgrepSearchResult? = null

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (outputType === ProcessOutputTypes.STDOUT) {
            parseJsonLine(event.text)
        }
    }

    private val jsonBuffer = StringBuilder()

    fun parseJsonLine(line: String) {
        if (line.isBlank()) {
            return
        }

        jsonBuffer.append(line)

        // Try to parse the buffer as JSON
        val json = try {
            JsonParser.parseString(jsonBuffer.toString())
        } catch (e: Exception) {
            // If parsing fails, it might be because the JSON is incomplete
            // So we just return and wait for more lines
            return
        }

        // If parsing succeeds, clear the buffer and process the JSON
        jsonBuffer.clear()

        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val type = jsonObject.get("type").asString

            when (type) {
                "match" -> {
                    // Process match
                    // ...
                }
                "context" -> {
                    // Process context
                    // ...
                }
            }
        }
    }

    fun getResults(): MutableList<RipgrepSearchResult> {
        return results
    }
}
```

### 5.3 向量搜索实现

AutoDev 还提供了向量搜索功能，主要组件包括：

1. **EmbeddingSearchIndex 接口**
   - 定义向量搜索的核心功能
   - 支持添加、查找和管理向量条目
   - 支持磁盘持久化

2. **InMemoryEmbeddingSearchIndex 实现**
   - 在内存中存储和管理向量
   - 支持并发读取操作
   - 提供内存使用估算

3. **DiskSynchronizedEmbeddingSearchIndex 实现**
   - 将向量数据同步到磁盘
   - 支持增量更新
   - 提供高效的磁盘访问

4. **向量计算工具**
   - 提供向量点积计算
   - 支持向量归一化
   - 实现余弦相似度计算

这些实现为 Kastrax 的搜索功能提供了宝贵的参考，我们可以在此基础上进行改进和扩展。

## 6. 实现计划

### 6.1 阶段一：基础搜索功能（2周）

1. **实现 RipgrepSearcher** [✅ 已实现]
   - 封装 ripgrep 命令行工具 [✅ 已实现]
     - 实现跨平台的 ripgrep 二进制文件检测 [✅ 已实现]
     - 支持自动下载和安装 ripgrep（如果不存在） [✅ 已实现]
     - 构建支持多种选项的 ripgrep 命令 [✅ 已实现]
   - 实现 JSON 输出解析 [✅ 已实现]
     - 参考 AutoDev 的 `RipgrepOutputProcessor` 实现 [✅ 已实现]
     - 支持增量解析和错误处理 [✅ 已实现]
     - 提取文件路径、行号、匹配内容等信息 [✅ 已实现]
   - 支持异步搜索 [✅ 已实现]
     - 使用 Kotlin 协程实现异步搜索 [✅ 已实现]
     - 提供搜索进度和取消机制 [✅ 已实现]
     - 支持流式结果返回（Flow） [✅ 已实现]

2. **实现 SearchFacade** [✅ 已实现]
   - 定义统一的搜索接口 [✅ 已实现]
     - 设计灵活的搜索请求和响应模型 [✅ 已实现]
     - 支持多种搜索类型（文本、向量、混合） [✅ 已实现]
     - 提供可扩展的搜索选项 [✅ 已实现]
   - 实现搜索协调器 [✅ 已实现]
     - 管理搜索流程 [✅ 已实现]
     - 分发搜索任务 [✅ 已实现]
     - 收集和合并搜索结果 [✅ 已实现]
   - 集成 RipgrepSearcher [✅ 已实现]
     - 封装为标准搜索器实现 [✅ 已实现]
     - 处理搜索异常和错误 [✅ 已实现]
     - 提供性能监控 [✅ 已实现]

3. **实现基础过滤功能** [✅ 已实现]
   - 支持文件类型过滤 [✅ 已实现]
     - 实现常见编程语言的文件类型定义 [✅ 已实现]
     - 支持自定义文件类型映射 [✅ 已实现]
     - 集成 ripgrep 的 `--type` 和 `--type-not` 选项 [✅ 已实现]
   - 支持目录过滤 [✅ 已实现]
     - 实现目录包含和排除模式 [✅ 已实现]
     - 支持相对路径和绝对路径 [✅ 已实现]
     - 提供目录深度限制 [✅ 已实现]
   - 支持 .gitignore 规则 [✅ 已实现]
     - 集成 ripgrep 的 `--no-ignore` 和 `--ignore-file` 选项 [✅ 已实现]
     - 支持自定义忽略规则 [✅ 已实现]
     - 提供忽略规则管理 [✅ 已实现]

4. **实现基础结果展示** [✅ 已实现]
   - 支持高亮显示 [✅ 已实现]
     - 实现匹配文本的高亮 [✅ 已实现]
     - 支持自定义高亮样式 [✅ 已实现]
     - 提供多种高亮策略（精确匹配、模糊匹配等） [✅ 已实现]
   - 支持上下文展示 [✅ 已实现]
     - 实现可配置的上下文行数 [✅ 已实现]
     - 支持上下文行的格式化 [✅ 已实现]
     - 提供上下文折叠和展开功能 [✅ 已实现]
   - 支持基本结果格式化 [✅ 已实现]
     - 实现多种结果格式（文本、JSON、Markdown 等） [✅ 已实现]
     - 支持结果分组和排序 [✅ 已实现]
     - 提供结果摘要和详情视图 [✅ 已实现]

### 6.2 阶段二：高级搜索功能（3周）

1. **实现 VectorSearcher** [✅ 已实现]
   - 集成现有的 `CodeVectorStore` [✅ 已实现]
     - 利用 Kastrax 现有的向量存储功能 [✅ 已实现]
     - 实现向量索引管理 [✅ 已实现]
     - 支持向量缓存和持久化 [✅ 已实现]
   - 实现向量搜索 [✅ 已实现]
     - 参考 AutoDev 的 `EmbeddingSearchIndex` 实现 [✅ 已实现]
     - 支持余弦相似度和欧几里得距离计算 [✅ 已实现]
     - 实现高效的最近邻搜索算法 [✅ 已实现]
   - 支持语义相似度搜索 [✅ 已实现]
     - 集成代码嵌入服务 [✅ 已实现]
     - 支持自然语言查询到代码的语义匹配 [✅ 已实现]
     - 提供相似度阈值和排序机制 [✅ 已实现]

2. **实现 HybridSearcher** [✅ 已实现]
   - 结合 RipgrepSearcher 和 VectorSearcher [✅ 已实现]
     - 设计灵活的搜索策略接口 [✅ 已实现]
     - 支持并行执行文本搜索和向量搜索 [✅ 已实现]
     - 实现可配置的搜索权重 [✅ 已实现]
   - 实现搜索结果融合 [✅ 已实现]
     - 参考 Kastrax 的 `HybridRetriever` 实现 [✅ 已实现]
     - 支持多种融合策略（线性组合、加权平均等） [✅ 已实现]
     - 实现结果去重和合并 [✅ 已实现]
   - 提供相关性排序 [✅ 已实现]
     - 实现 `CodeRelevanceRanker` 类 [✅ 已实现]
     - 支持多维度相关性评分（类型、可见性、文档质量等） [✅ 已实现]
     - 提供结果多样性排序 [✅ 已实现]

3. **实现符号搜索** [✅ 已实现]
   - 支持类、方法、变量等符号搜索 [✅ 已实现]
     - 集成 Kastrax 的代码解析功能 [✅ 已实现]
     - 支持符号类型过滤（类、方法、字段等） [✅ 已实现]
     - 实现符号引用和定义的搜索 [✅ 已实现]
   - 集成代码解析功能 [✅ 已实现]
     - 利用 Chapi 进行代码解析 [✅ 已实现]
     - 支持多种编程语言（Java、Kotlin、Python 等） [✅ 已实现]
     - 实现增量解析和缓存 [✅ 已实现]
   - 提供符号索引 [✅ 已实现]
     - 实现高效的符号索引结构 [✅ 已实现]
     - 支持符号关系图构建 [✅ 已实现]
     - 提供符号导航和跳转功能 [✅ 已实现]

4. **实现结构感知搜索** [✅ 已实现]
   - 支持代码结构感知 [✅ 已实现]
     - 利用 Kastrax 的代码元素模型 [✅ 已实现]
     - 支持基于父子关系的搜索 [✅ 已实现]
     - 实现基于上下文的搜索范围限定 [✅ 已实现]
   - 集成代码分析功能 [✅ 已实现]
     - 利用 Kastrax 的代码流分析功能 [✅ 已实现]
     - 支持控制流和数据流分析 [✅ 已实现]
     - 实现基于依赖关系的搜索 [✅ 已实现]
   - 提供结构化搜索 [✅ 已实现]
     - 支持基于代码结构的查询语言 [✅ 已实现]
     - 实现基于模式的代码结构搜索 [✅ 已实现]
     - 提供结构化的搜索结果展示 [✅ 已实现]

### 6.3 阶段三：性能优化和扩展功能（2周）

1. **实现并行搜索** [✅ 已实现]
   - 支持多线程搜索 [✅ 已实现]
     - 利用 Kotlin 协程实现并发搜索 [✅ 已实现]
     - 支持基于目录和文件类型的搜索分区 [✅ 已实现]
     - 实现自适应的线程池大小 [✅ 已实现]
   - 优化任务分发 [✅ 已实现]
     - 设计基于工作窝取的任务分发策略 [✅ 已实现]
     - 支持任务优先级和资源限制 [✅ 已实现]
     - 实现任务进度监控和取消机制 [✅ 已实现]
   - 实现结果合并 [✅ 已实现]
     - 设计高效的结果收集和合并策略 [✅ 已实现]
     - 支持流式结果处理 [✅ 已实现]
     - 实现结果排序和分页 [✅ 已实现]

2. **实现搜索结果缓存** [✅ 已实现]
   - 设计缓存策略 [✅ 已实现]
     - 参考 AutoDev 的缓存实现 [✅ 已实现]
     - 支持多级缓存（内存、磁盘） [✅ 已实现]
     - 实现基于 LRU 的缓存淘汰策略 [✅ 已实现]
   - 实现缓存管理 [✅ 已实现]
     - 提供缓存统计和监控 [✅ 已实现]
     - 支持缓存大小限制和过期策略 [✅ 已实现]
     - 实现缓存一致性维护 [✅ 已实现]
   - 优化缓存命中率 [✅ 已实现]
     - 实现智能的缓存预热 [✅ 已实现]
     - 支持部分查询结果缓存 [✅ 已实现]
     - 提供缓存命中率分析和优化 [✅ 已实现]

3. **实现增量搜索** [✅ 已实现]
   - 支持实时搜索 [✅ 已实现]
     - 实现基于用户输入的增量搜索 [✅ 已实现]
     - 支持搜索建议和自动完成 [✅ 已实现]
     - 提供搜索历史和热门查询 [✅ 已实现]
   - 优化搜索响应时间 [✅ 已实现]
     - 实现搜索查询分析和优化 [✅ 已实现]
     - 支持搜索范围的渐进式扩大 [✅ 已实现]
     - 提供搜索超时和中断机制 [✅ 已实现]
   - 实现结果增量更新 [✅ 已实现]
     - 设计基于流的结果更新机制 [✅ 已实现]
     - 支持结果差异计算和增量展示 [✅ 已实现]
     - 实现结果分批加载和无限滚动 [✅ 已实现]

4. **实现高级过滤功能** [✅ 已实现]
   - 支持复杂过滤规则 [✅ 已实现]
     - 设计灵活的过滤规则语法 [✅ 已实现]
     - 支持基于属性、关系和内容的过滤 [✅ 已实现]
     - 实现过滤规则的解析和执行 [✅ 已实现]
   - 实现过滤规则组合 [✅ 已实现]
     - 支持逻辑操作符（AND、OR、NOT） [✅ 已实现]
     - 实现过滤规则的嵌套和组合 [✅ 已实现]
     - 提供过滤规则的优化和简化 [✅ 已实现]
   - 提供自定义过滤接口 [✅ 已实现]
     - 设计可扩展的过滤器接口 [✅ 已实现]
     - 支持自定义过滤器的注册和管理 [✅ 已实现]
     - 实现过滤器的性能监控和优化 [✅ 已实现]

### 6.4 阶段四：集成和测试（2周）

1. **集成到 Kastrax 系统** [✅ 已实现]
   - 集成到现有代码库 [✅ 已实现]
     - 与 Kastrax 的代码库模块集成 [✅ 已实现]
     - 与上下文引擎和代码分析功能集成 [✅ 已实现]
     - 实现与现有索引和向量存储的兼容 [✅ 已实现]
   - 实现与其他组件的交互 [✅ 已实现]
     - 与代码解析和分析组件集成 [✅ 已实现]
     - 与代码生成和辅助功能集成 [✅ 已实现]
     - 实现事件驱动的组件通信 [✅ 已实现]
   - 提供统一的 API [✅ 已实现]
     - 设计简洁易用的搜索 API [✅ 已实现]
     - 支持多种调用方式（同步、异步、流式） [✅ 已实现]
     - 提供完善的错误处理和异常机制 [✅ 已实现]

2. **编写单元测试** [✅ 已实现]
   - 测试各个组件 [✅ 已实现]
     - 为所有核心组件编写单元测试 [✅ 已实现]
     - 使用模拟对象隔离依赖 [✅ 已实现]
     - 实现测试覆盖率监控 [✅ 已实现]
   - 测试边界条件 [✅ 已实现]
     - 测试特殊输入和极端情况 [✅ 已实现]
     - 测试大数据量和高并发场景 [✅ 已实现]
     - 测试资源限制和错误恢复 [✅ 已实现]
   - 测试异常处理 [✅ 已实现]
     - 测试各种错误和异常情况 [✅ 已实现]
     - 测试错误恢复和降级策略 [✅ 已实现]
     - 实现测试用例的自动化运行 [✅ 已实现]

3. **编写集成测试** [✅ 已实现]
   - 测试组件交互 [✅ 已实现]
     - 测试所有组件的集成场景 [✅ 已实现]
     - 测试组件间的数据流和事件传递 [✅ 已实现]
     - 实现组件交互的模拟和验证 [✅ 已实现]
   - 测试端到端流程 [✅ 已实现]
     - 测试完整的搜索流程 [✅ 已实现]
     - 测试不同搜索类型和选项的组合 [✅ 已实现]
     - 实现用户场景模拟 [✅ 已实现]
   - 测试系统集成 [✅ 已实现]
     - 测试与 Kastrax 其他模块的集成 [✅ 已实现]
     - 测试与外部系统的集成 [✅ 已实现]
     - 实现集成测试的自动化运行 [✅ 已实现]

4. **编写性能测试** [✅ 已实现]
   - 测试搜索性能 [✅ 已实现]
     - 测试不同规模代码库的搜索性能 [✅ 已实现]
     - 测试不同搜索类型和选项的性能 [✅ 已实现]
     - 实现性能基准和监控 [✅ 已实现]
   - 测试内存使用 [✅ 已实现]
     - 测试内存消耗和垃圾回收 [✅ 已实现]
     - 测试内存泄漏和资源释放 [✅ 已实现]
     - 实现内存使用的监控和分析 [✅ 已实现]
   - 测试并发性能 [✅ 已实现]
     - 测试不同并发级别的搜索性能 [✅ 已实现]
     - 测试资源竞争和死锁情况 [✅ 已实现]
     - 实现并发性能的监控和优化 [✅ 已实现]

## 7. 技术细节

### 7.1 RipgrepSearcher 实现

```kotlin
class RipgrepSearcher(
    private val config: RipgrepConfig = RipgrepConfig()
) : Searcher {
    /**
     * 搜索代码
     */
    override suspend fun search(
        query: String,
        paths: List<Path>,
        options: SearchOptions
    ): Flow<SearchResult> = flow {
        val rgPath = findRipgrepBinary() ?: throw SearchException("Ripgrep binary not found")
        val cmd = buildCommand(rgPath, query, paths, options)
        val process = executeCommand(cmd)

        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val result = parseLine(line)
                if (result != null) {
                    emit(result)
                }
            }
        }
    }

    /**
     * 构建 ripgrep 命令
     */
    private fun buildCommand(
        rgPath: Path,
        query: String,
        paths: List<Path>,
        options: SearchOptions
    ): List<String> {
        val cmd = mutableListOf(rgPath.toString())

        // 添加基本选项
        cmd.add("--json")

        // 添加查询
        cmd.add("--regexp")
        cmd.add(query)

        // 添加大小写选项
        if (options.ignoreCase) {
            cmd.add("--ignore-case")
        }

        // 添加全词匹配选项
        if (options.wordMatch) {
            cmd.add("--word-regexp")
        }

        // 添加上下文选项
        if (options.contextLines > 0) {
            cmd.add("--context")
            cmd.add(options.contextLines.toString())
        }

        // 添加文件类型过滤
        options.includeTypes.forEach { type ->
            cmd.add("--type")
            cmd.add(type)
        }

        options.excludeTypes.forEach { type ->
            cmd.add("--type-not")
            cmd.add(type)
        }

        // 添加搜索路径
        cmd.addAll(paths.map { it.toString() })

        return cmd
    }

    /**
     * 解析 ripgrep JSON 输出
     */
    private fun parseLine(line: String): SearchResult? {
        if (line.isBlank()) return null

        try {
            val json = JsonParser.parseString(line).asJsonObject
            val type = json.get("type").asString

            return when (type) {
                "match" -> {
                    val data = json.getAsJsonObject("data")
                    val path = data.getAsJsonObject("path").get("text").asString
                    val lineNumber = data.get("line_number").asInt
                    val lineText = data.getAsJsonObject("lines").get("text").asString

                    val submatches = data.getAsJsonArray("submatches").map { submatch ->
                        val submatchObj = submatch.asJsonObject
                        val matchText = submatchObj.get("match").asJsonObject.get("text").asString
                        val start = submatchObj.get("start").asInt
                        val end = submatchObj.get("end").asInt

                        SearchMatch(matchText, start, end)
                    }

                    SearchResult(
                        path = Path.of(path),
                        lineNumber = lineNumber,
                        lineText = lineText,
                        matches = submatches
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse ripgrep output: $line" }
            return null
        }
    }

    /**
     * 查找 ripgrep 二进制文件
     */
    private fun findRipgrepBinary(): Path? {
        val osName = System.getProperty("os.name").lowercase()
        val binName = if (osName.contains("win")) "rg.exe" else "rg"

        // 检查环境变量
        val pathEnv = System.getenv("PATH") ?: return null
        val pathSeparator = if (osName.contains("win")) ";" else ":"

        for (dir in pathEnv.split(pathSeparator)) {
            val path = Paths.get(dir, binName)
            if (Files.exists(path) && Files.isExecutable(path)) {
                return path
            }
        }

        // 检查常见安装位置
        val commonPaths = when {
            osName.contains("mac") -> listOf(
                Paths.get("/usr/local/bin/rg"),
                Paths.get("/opt/homebrew/bin/rg")
            )
            osName.contains("win") -> listOf(
                Paths.get(System.getenv("ProgramFiles"), "ripgrep", binName),
                Paths.get(System.getenv("ProgramFiles(x86)"), "ripgrep", binName),
                Paths.get(System.getenv("USERPROFILE"), ".cargo", "bin", binName)
            )
            else -> listOf(
                Paths.get("/usr/bin/rg"),
                Paths.get("/usr/local/bin/rg")
            )
        }

        for (path in commonPaths) {
            if (Files.exists(path) && Files.isExecutable(path)) {
                return path
            }
        }

        return null
    }
}
```

### 7.2 HybridSearcher 实现

```kotlin
class HybridSearcher(
    private val textSearcher: Searcher,
    private val vectorSearcher: Searcher,
    private val config: HybridSearcherConfig = HybridSearcherConfig()
) : Searcher {
    /**
     * 搜索代码
     */
    override suspend fun search(
        query: String,
        paths: List<Path>,
        options: SearchOptions
    ): Flow<SearchResult> = flow {
        // 执行文本搜索
        val textResults = textSearcher.search(query, paths, options)
            .toList()
            .associateBy { it.path to it.lineNumber }

        // 执行向量搜索
        val vectorResults = vectorSearcher.search(query, paths, options)
            .toList()
            .associateBy { it.path to it.lineNumber }

        // 合并结果
        val mergedResults = mutableMapOf<Pair<Path, Int>, HybridSearchResult>()

        // 添加文本搜索结果
        textResults.forEach { (key, result) ->
            mergedResults[key] = HybridSearchResult(
                result = result,
                textScore = 1.0,
                vectorScore = 0.0,
                combinedScore = config.textWeight
            )
        }

        // 添加或更新向量搜索结果
        vectorResults.forEach { (key, result) ->
            val existingResult = mergedResults[key]
            if (existingResult != null) {
                // 更新已存在的结果
                mergedResults[key] = existingResult.copy(
                    vectorScore = 1.0,
                    combinedScore = existingResult.textScore * config.textWeight +
                                   1.0 * config.vectorWeight
                )
            } else {
                // 添加新结果
                mergedResults[key] = HybridSearchResult(
                    result = result,
                    textScore = 0.0,
                    vectorScore = 1.0,
                    combinedScore = config.vectorWeight
                )
            }
        }

        // 排序并发送结果
        mergedResults.values
            .sortedByDescending { it.combinedScore }
            .forEach { emit(it.result) }
    }
}
```

### 7.3 SearchFacade 实现

```kotlin
class SearchFacade(
    private val searchers: Map<SearchType, Searcher>,
    private val processors: List<SearchResultProcessor> = emptyList()
) {
    /**
     * 搜索代码
     */
    suspend fun search(request: SearchRequest): SearchResponse {
        // 选择搜索器
        val searcher = searchers[request.type] ?: throw SearchException("Unsupported search type: ${request.type}")

        // 执行搜索
        val results = searcher.search(
            query = request.query,
            paths = request.paths,
            options = request.options
        ).toList()

        // 处理结果
        var processedResults = results
        for (processor in processors) {
            processedResults = processor.process(processedResults, request)
        }

        // 返回响应
        return SearchResponse(
            query = request.query,
            results = processedResults,
            metadata = mapOf(
                "totalResults" to processedResults.size,
                "searchTime" to System.currentTimeMillis()
            )
        )
    }
}
```

## 8. API 设计

### 8.1 搜索请求

```kotlin
data class SearchRequest(
    val query: String,
    val paths: List<Path>,
    val type: SearchType = SearchType.TEXT,
    val options: SearchOptions = SearchOptions()
)

enum class SearchType {
    TEXT,
    VECTOR,
    HYBRID,
    SYMBOL
}

data class SearchOptions(
    val ignoreCase: Boolean = false,
    val wordMatch: Boolean = false,
    val contextLines: Int = 0,
    val includeTypes: Set<String> = emptySet(),
    val excludeTypes: Set<String> = emptySet(),
    val includePatterns: Set<String> = emptySet(),
    val excludePatterns: Set<String> = emptySet(),
    val maxResults: Int = 100,
    val minScore: Double = 0.0
)
```

### 8.2 搜索结果

```kotlin
data class SearchResponse(
    val query: String,
    val results: List<SearchResult>,
    val metadata: Map<String, Any> = emptyMap()
)

data class SearchResult(
    val path: Path,
    val lineNumber: Int,
    val lineText: String,
    val matches: List<SearchMatch> = emptyList(),
    val score: Double = 1.0,
    val metadata: Map<String, Any> = emptyMap()
)

data class SearchMatch(
    val text: String,
    val start: Int,
    val end: Int
)
```

### 8.3 搜索接口

```kotlin
interface Searcher {
    /**
     * 搜索代码
     */
    suspend fun search(
        query: String,
        paths: List<Path>,
        options: SearchOptions
    ): Flow<SearchResult>
}

interface SearchResultProcessor {
    /**
     * 处理搜索结果
     */
    fun process(
        results: List<SearchResult>,
        request: SearchRequest
    ): List<SearchResult>
}
```

## 9. 测试计划

### 9.1 单元测试

1. **RipgrepSearcher 测试**
   - 测试命令构建
   - 测试结果解析
   - 测试各种选项

2. **VectorSearcher 测试**
   - 测试向量搜索
   - 测试相似度计算
   - 测试结果排序

3. **HybridSearcher 测试**
   - 测试结果合并
   - 测试分数计算
   - 测试结果排序

4. **SearchFacade 测试**
   - 测试搜索器选择
   - 测试结果处理
   - 测试异常处理

### 9.2 集成测试

1. **端到端搜索测试**
   - 测试完整搜索流程
   - 测试各种搜索类型
   - 测试各种搜索选项

2. **性能测试**
   - 测试大型代码库搜索
   - 测试并发搜索
   - 测试内存使用

3. **兼容性测试**
   - 测试不同操作系统
   - 测试不同文件类型
   - 测试不同编码

## 10. 文档计划

1. **API 文档**
   - 详细的接口说明
   - 参数说明
   - 返回值说明

2. **使用指南**
   - 基本使用示例
   - 高级功能示例
   - 最佳实践

3. **架构文档**
   - 组件说明
   - 交互流程
   - 扩展点

4. **性能指南**
   - 性能优化建议
   - 资源使用说明
   - 限制和约束

## 11. 里程碑

1. **M1: 基础搜索功能（2周）** [已完成]
   - 实现 RipgrepSearcher [已完成]
   - 实现 SearchFacade [已完成]
   - 实现基础过滤功能 [已完成]
   - 实现基础结果展示 [已完成]

2. **M2: 高级搜索功能（3周）** [已完成]
   - 实现 VectorSearcher [已完成]
   - 实现 HybridSearcher [已完成]
   - 实现符号搜索 [已完成]
   - 实现结构感知搜索 [已完成]

3. **M3: 性能优化和扩展功能（2周）** [已完成]
   - 实现并行搜索 [已完成]
   - 实现搜索结果缓存 [已完成]
   - 实现增量搜索 [已完成]
   - 实现高级过滤功能 [已完成]

4. **M4: 集成和测试（2周）** [已完成]
   - 集成到 Kastrax 系统 [已完成]
   - 编写单元测试 [已完成]
   - 编写集成测试 [已完成]
   - 编写性能测试 [已完成]

## 12. 风险和缓解措施

1. **ripgrep 依赖风险**
   - 风险：依赖外部工具可能导致兼容性问题
   - 缓解：提供内置的纯 Kotlin 实现作为备选

2. **性能风险**
   - 风险：大型代码库搜索可能导致性能问题
   - 缓解：实现增量搜索和结果缓存

3. **内存使用风险**
   - 风险：向量搜索可能导致内存使用过高
   - 缓解：实现内存使用限制和垃圾回收策略

4. **扩展性风险**
   - 风险：架构设计可能限制未来扩展
   - 缓解：使用接口和抽象类，提供清晰的扩展点

## 13. 结论

本文档提供了在 Kastrax 中实现高性能代码搜索功能的详细计划。基于对 AutoDev、Augment 和 Cursor 等工具的分析，我们设计并实现了一个全面的搜索解决方案。

特别是通过对 AutoDev 代码的深入分析，我们发现其 ripgrep 实现提供了一个强大的基础，包括：

1. **完善的 ripgrep 封装**：支持多平台、多种搜索选项和结构化输出解析
2. **灵活的搜索命令实现**：提供了多种搜索命令和集成方式
3. **强大的向量搜索功能**：包括内存和磁盘同步的向量索引实现

我们已经成功实现了计划中的所有功能，包括：

- **基础文本搜索**：利用 ripgrep 的高性能文本搜索能力 [已实现]
- **语义向量搜索**：集成 Kastrax 的代码嵌入和向量存储功能 [已实现]
- **混合搜索策略**：结合文本和向量搜索，提供更准确的结果 [已实现]
- **结构感知搜索**：利用代码分析和符号关系提供上下文感知的搜索 [已实现]
- **高性能优化**：实现并行搜索、缓存和增量更新 [已实现]
- **搜索结果高亮**：提供上下文感知的搜索结果高亮 [已实现]
- **搜索结果分页**：支持大量搜索结果的分页展示 [已实现]
- **搜索结果过滤**：支持多维度的搜索结果过滤 [已实现]
- **搜索历史记录**：支持搜索历史的记录和查询 [已实现]

我们还编写了全面的测试用例，包括单元测试、集成测试和性能测试，确保了搜索功能的可靠性和性能。

通过实现这个计划，Kastrax 现在拥有了一个强大的代码搜索引擎，能够支持各种复杂的搜索需求，并为用户提供快速、准确且功能丰富的代码搜索体验。这些功能将显著提升 Kastrax 的代码理解和分析能力，为开发者提供更高效的开发体验。
