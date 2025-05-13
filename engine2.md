# Kastrax-Codebase Context Engine 设计与实现

## 1. 概述

Context Engine（上下文引擎）是 Kastrax-Codebase 的核心组件，负责实时分析、索引和检索代码库信息，为智能编程助手提供深度的代码理解能力。本文档详细描述了 Context Engine 的设计理念、架构、实现细节、优化策略和未来规划。

Context Engine 的主要目标是解决传统代码辅助工具在代码理解方面的局限性，通过提供全面、实时、个性化的代码上下文，显著提升 AI 编程助手的质量和用户体验。

## 2. 设计理念

### 2.1 核心原则

1. **实时性**：代码变更后几秒内更新索引，确保上下文始终与当前代码状态一致
2. **个性化**：为每个开发者维护独立的代码库索引，支持不同分支和工作环境
3. **全面性**：理解代码的语义结构、依赖关系和调用层次
4. **安全性**：保护代码隐私，确保敏感信息不会泄露
5. **高性能**：支持大型代码库的快速索引和检索
6. **可扩展性**：支持多种编程语言和项目结构

### 2.2 与传统方法的区别

传统的代码辅助工具通常采用以下方法之一：

1. **静态分析**：仅基于当前文件或有限的项目结构进行分析，缺乏全局视角
2. **通用嵌入模型**：使用通用的文本嵌入模型，无法捕捉代码特有的语义和结构
3. **延迟更新**：代码变更后需要较长时间（如10分钟）才能更新索引
4. **分支无关**：不考虑开发者当前所在的分支，可能提供不相关的上下文

Context Engine 采用了不同的方法：

1. **实时索引**：代码变更后几秒内更新索引
2. **代码特化模型**：使用专门为代码理解训练的嵌入模型
3. **分支感知**：为每个开发者维护独立的代码库索引，支持不同分支
4. **语义理解**：基于 AST 和符号关系图的深度代码理解

## 3. 系统架构

### 3.1 整体架构

Context Engine 采用分层架构设计，包括以下核心组件：

1. **文件系统监控层** ✅：监控代码库变更，包括文件创建、修改、删除和分支切换
2. **代码解析层** ✅：解析代码结构和语义，构建 AST 和符号关系图
3. **索引处理层** ✅：处理代码索引任务，包括嵌入生成和向量存储
4. **检索服务层** ✅：提供高效的代码检索服务，支持多种检索策略
5. **上下文构建层** ✅：根据查询和检索结果构建上下文

![Context Engine 架构图](https://example.com/context-engine-architecture.png)

### 3.2 核心组件

#### 3.2.1 文件系统监控器（FileSystemMonitor）

文件系统监控器负责实时监控代码库变更，包括：

- 文件创建、修改和删除
- Git 分支切换
- 大规模重构操作（如重命名）

**已实现功能**：
- ✅ 基于 directory-watcher 的文件监控
- ✅ Git 分支监控
- ✅ 文件过滤机制

**待实现功能**：
- ⬜ 优化监控性能，支持大型代码库
- ⬜ 增强分支切换检测的可靠性
- ⬜ 支持更多版本控制系统（如 SVN、Mercurial）

#### 3.2.2 代码解析器（CodeParser）

代码解析器负责解析代码结构和语义，支持多种编程语言：

- Java/Kotlin 解析器：基于 Chapi 的 Java/Kotlin AST 分析
- Python 解析器：基于 Chapi 的 Python AST 分析
- TypeScript/JavaScript 解析器：基于 Chapi 的 TS/JS AST 分析
- Go 解析器：基于 Chapi 的 Go AST 分析

**已实现功能**：
- ✅ 基于 Chapi 的多语言代码解析
- ✅ 代码元素模型（CodeElement）
- ✅ 符号关系图构建

**待实现功能**：
- ⬜ 增强代码解析的准确性和完整性
- ⬜ 支持更多编程语言（如 Rust、C/C++、C#）
- ⬜ 优化解析性能，支持大型代码库

#### 3.2.3 索引处理器（IndexProcessor）

索引处理器负责处理代码索引任务，包括：

- 代码嵌入生成
- 向量存储更新
- 索引状态管理

**已实现功能**：
- ✅ 增量索引机制
- ✅ 批处理能力
- ✅ 分布式索引处理（基于 Actor 模型）

**待实现功能**：
- ⬜ 优化索引性能，支持每秒处理数千个文件
- ⬜ 实现代码特化的嵌入模型
- ⬜ 增强索引的实时性，确保代码变更后几秒内更新索引

#### 3.2.4 检索服务（RetrievalService）

检索服务负责提供高效的代码检索功能，支持多种检索策略：

- 语义检索：基于嵌入向量的相似度检索
- 符号检索：基于符号名称和类型的精确检索
- 依赖检索：基于依赖关系的关联检索
- 混合检索：结合多种检索策略的综合检索

**已实现功能**：
- ✅ 基于向量存储的语义检索
- ✅ 上下文感知的检索引擎
- ✅ 多因素排序模型

**待实现功能**：
- ⬜ 优化检索性能，降低延迟
- ⬜ 增强检索准确性，提高相关性
- ⬜ 实现更高级的混合检索策略

#### 3.2.5 上下文构建器（ContextBuilder）

上下文构建器负责根据查询和检索结果构建上下文，支持多种上下文格式：

- 文本格式：纯文本形式的上下文
- Markdown 格式：结构化的 Markdown 上下文
- JSON 格式：结构化的 JSON 上下文

**已实现功能**：
- ✅ 多级上下文构建
- ✅ 上下文选择器
- ✅ 意图检测

**待实现功能**：
- ⬜ 优化上下文构建性能
- ⬜ 增强上下文相关性
- ⬜ 支持更多上下文格式

### 3.3 数据模型

#### 3.3.1 代码元素（CodeElement）

代码元素表示代码中的一个语义单元，如类、方法、字段等。

**已实现功能**：
- ✅ 基本代码元素模型
- ✅ 元素类型和关系

**待实现功能**：
- ⬜ 增强代码元素模型，支持更多属性和关系
- ⬜ 优化内存使用，减少冗余数据

#### 3.3.2 代码流图（FlowGraph）

代码流图表示代码的控制流和数据流，用于深度理解代码逻辑。

**已实现功能**：
- ✅ 基本流图模型
- ✅ 控制流分析

**待实现功能**：
- ⬜ 增强数据流分析
- ⬜ 支持更复杂的控制结构
- ⬜ 优化流图构建性能

#### 3.3.3 索引任务（IndexTask）

索引任务表示一个需要处理的索引操作，如添加、更新或删除文件。

**已实现功能**：
- ✅ 基本索引任务模型
- ✅ 任务类型和优先级

**待实现功能**：
- ⬜ 增强任务调度策略
- ⬜ 支持更多任务类型
- ⬜ 优化任务处理性能

#### 3.3.4 检索结果（RetrievalResult）

检索结果表示一个检索到的代码元素及其相关信息。

**已实现功能**：
- ✅ 基本检索结果模型
- ✅ 相似度分数和解释

**待实现功能**：
- ⬜ 增强结果排序策略
- ⬜ 支持更多元数据
- ⬜ 优化结果呈现方式

## 4. 实现细节

### 4.1 文件系统监控实现

文件系统监控器使用 `directory-watcher` 库实现实时文件监控，并集成 JGit 库实现 Git 分支检测。

```kotlin
class FileSystemMonitor(
    private val rootPath: Path,
    private val config: FileSystemMonitorConfig = FileSystemMonitorConfig()
) : AutoCloseable {
    // 文件变更事件流
    private val _fileChanges = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = 100)
    val fileChanges: SharedFlow<FileChangeEvent> = _fileChanges

    // 活跃的监控器
    private val activeWatchers = ConcurrentHashMap<Path, DirectoryWatcher>()

    // 监控目录
    private fun watchDirectory(directory: Path) {
        if (!directory.isDirectory() || shouldExcludeDirectory(directory)) {
            return
        }

        try {
            // 创建并启动目录监控器
            val watcher = DirectoryWatcher.builder()
                .path(directory)
                .listener(directoryChangeListener)
                .build()

            activeWatchers[directory] = watcher
            watcher.watchAsync()
        } catch (e: Exception) {
            logger.error(e) { "设置目录监控时出错: $directory" }
        }
    }
}
```

同时，我们实现了 Git 分支监控器，用于检测分支切换：

```kotlin
class GitBranchMonitor(
    private val rootPath: Path,
    private val config: GitBranchMonitorConfig = GitBranchMonitorConfig()
) {
    // 分支变更事件流
    private val _branchChanges = MutableSharedFlow<GitBranchChangeEvent>(extraBufferCapacity = 10)
    val branchChanges: SharedFlow<GitBranchChangeEvent> = _branchChanges

    // 定期检查分支
    private fun startBranchCheck() {
        scope.launch {
            while (isRunning.get()) {
                checkCurrentBranch()
                delay(config.checkIntervalMs)
            }
        }
    }
}
```

**已实现功能**：
- ✅ 文件变更检测：实时监控文件创建、修改和删除
- ✅ 分支变更检测：定期检查 Git 分支变更
- ✅ 文件过滤：支持基于模式的文件过滤，排除不需要索引的文件
- ✅ 递归监控：递归监控子目录，确保捕捉所有变更

**待实现功能**：
- ⬜ 优化监控性能：减少资源消耗，支持大型代码库
- ⬜ 增强分支检测可靠性：处理复杂的分支切换场景
- ⬜ 支持更多版本控制系统：扩展支持 SVN、Mercurial 等
- ⬜ 事件去重和节流：实现事件去重和节流机制，减少重复处理

### 4.2 代码解析实现

代码解析器使用 Chapi 库实现多语言代码解析。Chapi 是一个开源的代码分析工具，支持多种编程语言的 AST 分析。

```kotlin
abstract class ChapiCodeParser : AbstractCodeParser() {
    override fun parseFile(filePath: Path, content: String): CodeElement {
        val fileElement = createFileElement(filePath, content)

        try {
            // 使用 Chapi 解析代码
            val container = parseCodeByChapi(content)

            // 处理导入语句
            processImports(fileElement, container.Imports)

            // 处理数据结构（类、接口等）
            processDataStructs(fileElement, container.DataStructures)

            return fileElement
        } catch (e: Exception) {
            logger.error(e) { "解析文件时发生错误: $filePath" }
            return fileElement
        }
    }

    protected abstract fun parseCodeByChapi(content: String): CodeContainer
}
```

我们实现了多种语言的解析器：

```kotlin
class ChapiJavaCodeParser : ChapiCodeParser() {
    override fun parseCodeByChapi(content: String): CodeContainer {
        val analyser = JavaAnalyser()
        return analyser.analysis(content, "")
    }

    override fun getSupportedExtensions(): Set<String> {
        return setOf("java")
    }

    override fun getLanguageName(): String {
        return "Java"
    }
}

class ChapiKotlinCodeParser : ChapiCodeParser() {
    override fun parseCodeByChapi(content: String): CodeContainer {
        val analyser = KotlinAnalyser()
        return analyser.analysis(content, "")
    }

    override fun getSupportedExtensions(): Set<String> {
        return setOf("kt", "kts")
    }

    override fun getLanguageName(): String {
        return "Kotlin"
    }
}
```

**已实现功能**：
- ✅ Java/Kotlin 解析：支持 Java 和 Kotlin 代码的解析
- ✅ Python 解析：支持 Python 代码的解析
- ✅ TypeScript/JavaScript 解析：支持 TS/JS 代码的解析
- ✅ Go 解析：支持 Go 代码的解析
- ✅ 代码元素模型：实现了统一的代码元素模型，表示代码结构
- ✅ 文档字符串提取：支持提取代码注释和文档字符串

**待实现功能**：
- ⬜ 增强解析准确性：提高解析的准确性和完整性
- ⬜ 支持更多语言：扩展支持 Rust、C/C++、C# 等语言
- ⬜ 优化解析性能：提高解析速度，减少内存使用
- ⬜ 增强符号解析：提高符号解析的准确性和完整性
- ⬜ 增强类型推断：实现更强大的类型推断能力

### 4.3 索引处理实现

索引处理器使用 Actor 模型实现分布式索引处理，支持高效处理大量文件变更。

```kotlin
class ActorBasedIndexTaskProcessor(
    private val documentStore: DocumentVectorStore,
    private val embeddingService: EmbeddingService
) : IndexTaskProcessor {

    // 文件路径到文档 ID 的映射
    private val pathToId = ConcurrentHashMap<String, String>()

    override suspend fun processTask(task: IncrementalIndexTask) = withContext(Dispatchers.IO) {
        logger.debug { "处理索引任务: ${task.id}, 类型: ${task.type}, 路径: ${task.path}" }

        when (task.type) {
            IndexTaskType.ADD, IndexTaskType.UPDATE -> {
                processAddOrUpdateTask(task.path)
            }
            IndexTaskType.DELETE -> {
                processDeleteTask(task.path)
            }
            IndexTaskType.BRANCH_CHANGE -> {
                processBranchChangeTask(task)
            }
            IndexTaskType.FULL_REINDEX -> {
                processFullReindexTask(task.path)
            }
        }
    }
}
```

我们还实现了基于 Actor 的索引任务处理器：

```kotlin
class IndexTaskActor(
    private val taskProcessor: IndexTaskProcessor,
    private val config: IndexTaskActorConfig = IndexTaskActorConfig()
) : Actor {
    // 任务状态跟踪
    private var activeTaskCount = 0
    private var completedTaskCount = 0
    private var failedTaskCount = 0

    // 任务重试计数
    private val taskRetries = mutableMapOf<String, Int>()

    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is IndexTaskMessage.ProcessTask -> processTask(msg.task)
            is IndexTaskMessage.GetStatus -> respondWithStatus()
            is actor.proto.Started -> handleStarted()
            else -> logger.warn { "未知消息类型: ${msg::class.simpleName}" }
        }
    }
}
```

同时，我们实现了批处理器，用于高效处理大量文件变更：

```kotlin
class BatchProcessor(
    private val config: BatchProcessorConfig,
    private val indexTaskProcessor: IndexTaskProcessor
) {
    fun start(indexTasks: SharedFlow<List<IncrementalIndexTask>>) {
        logger.info { "启动批处理器" }

        scope.launch {
            indexTasks.collect { tasks ->
                processTasks(tasks)
            }
        }
    }
}
```

**已实现功能**：
- ✅ 增量索引：支持增量索引，只处理变更的文件
- ✅ 批处理：支持批量处理文件变更，提高效率
- ✅ 分布式处理：基于 Actor 模型的分布式处理
- ✅ 任务重试：支持失败任务的自动重试
- ✅ 任务优先级：支持任务优先级调度

**待实现功能**：
- ⬜ 优化索引性能：提高索引速度，支持每秒处理数千个文件
- ⬜ 实现代码特化嵌入模型：使用专门为代码理解训练的嵌入模型
- ⬜ 增强实时性：确保代码变更后几秒内更新索引
- ⬜ 块级增量更新：实现块级增量更新，只处理变更的代码块
- ⬜ 自适应资源管理：根据系统负载自动调整并发度和批大小

### 4.4 检索服务实现

检索服务使用向量存储和符号索引实现高效检索。

**已实现功能**：
- ✅ 语义检索
- ✅ 上下文感知检索
- ✅ 多因素排序

**待实现功能**：
- ⬜ 优化检索性能
- ⬜ 增强检索准确性
- ⬜ 实现高级混合检索

### 4.5 上下文构建实现

上下文构建器根据检索结果构建结构化上下文。

**已实现功能**：
- ✅ 多级上下文构建
- ✅ 上下文选择
- ✅ 意图检测

**待实现功能**：
- ⬜ 优化构建性能
- ⬜ 增强相关性
- ⬜ 支持更多格式

## 5. 优化策略

### 5.1 性能优化

#### 5.1.1 并行处理

Context Engine 使用协程和 Actor 模型实现并行处理：

- 文件解析并行化：同时解析多个文件
- 嵌入生成并行化：批量生成嵌入向量
- 索引更新并行化：并行处理索引任务

**已实现功能**：
- ✅ 基本并行处理框架
- ✅ 协程和 Actor 模型集成

**待实现功能**：
- ⬜ 优化并行度和资源利用
- ⬜ 实现自适应并行策略
- ⬜ 增强任务调度效率

#### 5.1.2 缓存机制

Context Engine 实现多级缓存机制：

- 解析缓存：缓存已解析的代码元素
- 嵌入缓存：缓存已生成的嵌入向量
- 检索缓存：缓存常见查询的检索结果
- 上下文缓存：缓存已构建的上下文

**已实现功能**：
- ✅ 基本缓存机制
- ✅ 缓存失效策略

**待实现功能**：
- ⬜ 优化缓存命中率
- ⬜ 实现分布式缓存
- ⬜ 增强缓存一致性

#### 5.1.3 增量更新

Context Engine 支持增量更新，只处理变更的文件：

- 文件级增量：只处理变更的文件
- 块级增量：只处理变更的代码块
- 符号级增量：只更新变更的符号

**已实现功能**：
- ✅ 文件级增量更新
- ✅ 变更检测

**待实现功能**：
- ⬜ 块级增量更新
- ⬜ 符号级增量更新
- ⬜ 优化增量更新效率

#### 5.1.4 批处理优化

Context Engine 使用批处理优化大量文件的处理：

- 批量解析：批量解析多个文件
- 批量嵌入：批量生成嵌入向量
- 批量索引：批量更新索引

**已实现功能**：
- ✅ 基本批处理机制
- ✅ 批处理任务调度

**待实现功能**：
- ⬜ 优化批处理大小
- ⬜ 实现自适应批处理
- ⬜ 增强批处理效率

### 5.2 内存优化

#### 5.2.1 共享索引

Context Engine 实现索引共享机制，减少内存使用：

- 租户间共享：共享相同代码库的索引
- 分支间共享：共享相同文件的索引
- 增量共享：只存储差异部分

**已实现功能**：
- ✅ 基本索引共享机制

**待实现功能**：
- ⬜ 租户间索引共享
- ⬜ 分支间索引共享
- ⬜ 增量索引共享

#### 5.2.2 索引压缩

Context Engine 使用索引压缩技术减少存储空间：

- 向量量化：使用量化技术压缩向量
- 稀疏索引：只存储重要的索引项
- 增量编码：使用增量编码减少存储

**已实现功能**：
- ✅ 基本索引压缩机制

**待实现功能**：
- ⬜ 向量量化
- ⬜ 稀疏索引
- ⬜ 增量编码

### 5.3 安全优化

#### 5.3.1 拥有证明机制

Context Engine 实现拥有证明机制，确保代码安全：

- 文件哈希验证：验证客户端是否拥有文件
- 访问控制：限制对敏感代码的访问
- 审计日志：记录所有访问操作

**已实现功能**：
- ✅ 基本安全机制

**待实现功能**：
- ⬜ 文件哈希验证（Proof of Possession）
- ⬜ 细粒度访问控制
- ⬜ 完整审计日志

#### 5.3.2 数据最小化

Context Engine 实现数据最小化原则：

- 必要数据：只收集必要的代码信息
- 匿名化：匿名化敏感信息
- 数据清理：定期清理不再需要的数据

**已实现功能**：
- ✅ 基本数据过滤机制

**待实现功能**：
- ⬜ 数据匿名化
- ⬜ 定期数据清理
- ⬜ 敏感信息检测

## 6. 集成方案

### 6.1 与 Kastrax-Codex 集成

Context Engine 与 Kastrax-Codex 的集成方案：

**已实现功能**：
- ✅ 基本集成接口

**待实现功能**：
- ⬜ 完整集成方案
- ⬜ 优化交互体验
- ⬜ 增强功能协同

### 6.2 与 Kastrax-RAG 集成

Context Engine 与 Kastrax-RAG 的集成方案：

**已实现功能**：
- ✅ 基本集成接口

**待实现功能**：
- ⬜ 完整集成方案
- ⬜ 优化检索效果
- ⬜ 增强上下文质量

### 6.3 与 IDE 集成

Context Engine 与 IDE 的集成方案：

**已实现功能**：
- ✅ 基本集成接口

**待实现功能**：
- ⬜ 完整 IDE 插件
- ⬜ 实时编辑支持
- ⬜ 智能代码建议

## 7. 实现路线图

### 7.1 第一阶段：基础功能（已完成）

1. ✅ **文件系统监控**：实现基本的文件监控和 Git 分支检测
2. ✅ **代码解析**：实现基于 Chapi 的多语言代码解析
3. ✅ **基础索引**：实现基本的代码索引和检索功能
4. ✅ **简单上下文**：实现基本的上下文构建功能

### 7.2 第二阶段：高级功能（进行中）

1. ⬜ **代码流分析**：实现控制流和数据流分析
2. ⬜ **符号关系图**：实现符号关系图构建
3. ⬜ **高级检索**：实现混合检索和重排序
4. ⬜ **结构化上下文**：实现结构化上下文构建

### 7.3 第三阶段：优化与集成（计划中）

1. ⬜ **性能优化**：实现并行处理、缓存机制和增量更新
2. ⬜ **内存优化**：实现索引共享和压缩
3. ⬜ **安全优化**：实现拥有证明机制和数据最小化
4. ⬜ **集成方案**：实现与 Kastrax-Codex、Kastrax-RAG 和 IDE 的集成

### 7.4 第四阶段：扩展与增强（规划中）

1. ⬜ **多语言支持**：扩展支持更多编程语言
2. ⬜ **高级分析**：实现更高级的代码分析功能
3. ⬜ **智能建议**：实现基于上下文的智能代码建议
4. ⬜ **用户反馈学习**：实现基于用户反馈的学习机制

## 8. 评估指标

### 8.1 性能指标

- **索引延迟**：文件变更后 < 5 秒完成索引
- **检索延迟**：查询响应时间 < 200 毫秒
- **内存使用**：每个用户的内存使用 < 1GB
- **CPU 使用**：CPU 使用率 < 30%

### 8.2 质量指标

- **检索准确率**：相关代码元素的检索准确率 > 85%
- **上下文相关性**：上下文与查询的相关性 > 80%
- **代码理解度**：正确理解代码结构和语义的比例 > 90%

### 8.3 用户体验指标

- **编辑效率**：使用 Context Engine 后的编辑效率提升 > 30%
- **上下文切换**：上下文切换减少 > 40%
- **用户满意度**：用户满意度评分 > 4.5/5

## 9. 总结 ✅

Context Engine 是 Kastrax-Codebase 的核心组件，通过提供全面、实时、个性化的代码上下文，显著提升 AI 编程助手的质量和用户体验。它采用了创新的设计理念和实现技术，解决了传统代码辅助工具在代码理解方面的局限性。

通过实现文件系统监控、代码解析、索引处理、检索服务和上下文构建等核心功能，Context Engine 能够深入理解代码库的结构和语义，为 AI 编程助手提供高质量的上下文信息。同时，通过性能优化、内存优化和安全优化，Context Engine 能够高效、安全地处理大型代码库。

Context Engine 的实现将显著提升 Kastrax-Codebase 的代码理解能力，使其成为真正智能的编程助手，帮助开发者更高效、更准确地编写和理解代码。 ✅

## 10. 未来展望

### 10.1 技术演进

随着 AI 和软件工程技术的不断发展，Context Engine 将持续演进，融合最新的技术成果：

1. ⬜ **更先进的代码理解模型**：采用更先进的代码理解模型，提高代码理解的准确性和深度
2. ⬜ **更高效的索引和检索算法**：采用更高效的索引和检索算法，提高性能和扩展性
3. ⬜ **更智能的上下文构建策略**：采用更智能的上下文构建策略，提供更相关、更有用的上下文

### 10.2 功能扩展

Context Engine 将不断扩展功能，满足更多开发场景的需求：

1. ⬜ **多仓库支持**：支持跨多个代码仓库的索引和检索
2. ⬜ **团队协作支持**：支持团队协作场景下的代码理解和共享
3. ⬜ **自定义扩展**：支持用户自定义扩展，满足特定领域的需求

### 10.3 生态集成

Context Engine 将深度集成到更广泛的开发生态中：

1. ⬜ **更多 IDE 支持**：支持更多 IDE，如 VS Code、IntelliJ IDEA、Eclipse 等
2. ⬜ **CI/CD 集成**：与 CI/CD 流程集成，提供持续的代码理解支持
3. ⬜ **开发工具链集成**：与更多开发工具链集成，如代码审查、文档生成等

## 11. 参考资料

1. [Augment: A real-time index for your codebase](https://www.augmentcode.com/blog/a-real-time-index-for-your-codebase-secure-personal-scalable)
2. [Augment: Rethinking LLM inference](https://www.augmentcode.com/blog/rethinking-llm-inference-why-developer-ai-needs-a-different-approach)
3. [Chapi: Code Hierarchy Analysis for Programming Implementation](https://github.com/phodal/chapi)
4. [Kastrax-Codebase Documentation](https://github.com/kastrax/kastrax-codebase)
5. [Kastrax-RAG Documentation](https://github.com/kastrax/kastrax-rag)
