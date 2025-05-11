package ai.kastrax.codebase.examples

// TODO: 暂时注释掉示例代码，等待相关依赖问题解决

// 空实现以避免语法错误
class ContextAwareSearchExample

/*
import ai.kastrax.codebase.embedding.EmbeddingModel
import ai.kastrax.codebase.embedding.EmbeddingModelManager
import ai.kastrax.codebase.embedding.EmbeddingService
import ai.kastrax.codebase.retrieval.context.ContextBuilder
import ai.kastrax.codebase.retrieval.context.ContextBuilderConfig
import ai.kastrax.codebase.retrieval.context.ContextBuilderEventType
import ai.kastrax.codebase.retrieval.context.ContextLevel
import ai.kastrax.codebase.retrieval.context.ContextType
import ai.kastrax.codebase.retrieval.context.IntentBasedContextSelector
import ai.kastrax.codebase.retrieval.context.IntentDetectorConfig
import ai.kastrax.codebase.retrieval.context.ContextSelectorConfig
import ai.kastrax.codebase.retrieval.model.RetrievalContext
import ai.kastrax.codebase.semantic.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.CodeRelationAnalyzerConfig
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.codebase.symbol.SymbolGraphBuilder
import ai.kastrax.codebase.symbol.SymbolGraphBuilderConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path

/**
 * 上下文感知搜索示例
 */
object ContextAwareSearchExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取要分析的代码库路径
        val codebasePath = if (args.isNotEmpty()) {
            Path(args[0])
        } else {
            // 默认使用当前目录
            Path(".")
        }

        println("开始分析代码库: $codebasePath")

        // 注册代码解析器
        registerParsers()

        // 创建嵌入模型管理器
        val embeddingModelManager = EmbeddingModelManager()

        // 注册默认嵌入模型
        embeddingModelManager.registerModel(
            EmbeddingModel(
                name = "default",
                dimension = 1536,
                provider = "openai",
                modelName = "text-embedding-3-small"
            )
        )

        // 创建嵌入服务
        val embeddingService = EmbeddingService(embeddingModelManager)

        // 创建代码语义分析器
        val semanticAnalyzer = CodeSemanticAnalyzer(
            config = CodeSemanticAnalyzerConfig(
                maxConcurrentFiles = 10
            )
        )

        // 创建代码关系分析器
        val relationAnalyzer = CodeRelationAnalyzer(
            config = CodeRelationAnalyzerConfig(
                analyzeInheritance = true,
                analyzeUsage = true,
                analyzeDependency = true,
                analyzeOverride = true
            )
        )

        // 创建符号关系图构建器
        val symbolGraphBuilder = SymbolGraphBuilder(
            semanticAnalyzer = semanticAnalyzer,
            relationAnalyzer = relationAnalyzer,
            config = SymbolGraphBuilderConfig(
                includeReferences = true,
                includeCalls = true,
                includeInheritance = true,
                includeImplementations = true,
                includeOverrides = true,
                includeImports = true,
                includeDependencies = true
            )
        )

        // 创建上下文构建器
        val contextBuilder = ContextBuilder(
            semanticAnalyzer = semanticAnalyzer,
            symbolGraphBuilder = symbolGraphBuilder,
            config = ContextBuilderConfig(
                maxConcurrentTasks = 10,
                enableEventNotifications = true,
                includeDocumentation = true,
                includeComments = true,
                includeHistory = true,
                maxHistorySize = 10
            )
        )

        // 创建上下文选择器
        val contextSelector = IntentBasedContextSelector(
            contextHierarchy = contextBuilder.getContextHierarchy(),
            embeddingService = embeddingService,
            intentDetectorConfig = IntentDetectorConfig(
                embeddingModelName = "default",
                confidenceThreshold = 0.6,
                enableCaching = true,
                cacheSize = 1000
            ),
            selectorConfig = ContextSelectorConfig(
                minRelevanceScore = 0.3,
                enableCaching = true,
                cacheSize = 1000
            )
        )

        // 启动事件监听
        val eventJob = launch {
            contextBuilder.events.collect { event ->
                when (event.type) {
                    ContextBuilderEventType.INITIALIZED -> {
                        println("上下文构建器初始化完成")
                    }
                    ContextBuilderEventType.BUILDING_STARTED -> {
                        println("开始构建上下文: ${event.message}")
                    }
                    ContextBuilderEventType.BUILDING_COMPLETED -> {
                        println("构建上下文完成: ${event.message}")
                    }
                    ContextBuilderEventType.CONTEXT_ADDED -> {
                        println("添加上下文: ${event.message}")
                    }
                    ContextBuilderEventType.ERROR -> {
                        println("错误: ${event.message}")
                    }
                    else -> {
                        println("事件: ${event.type} - ${event.message}")
                    }
                }
            }
        }

        // 初始化上下文构建器
        contextBuilder.initialize()

        // 构建代码库上下文
        println("\n开始构建代码库上下文...")
        val success = contextBuilder.buildCodebaseContext(codebasePath)

        if (success) {
            println("构建代码库上下文成功")
        } else {
            println("构建代码库上下文失败")
            return@runBlocking
        }

        // 获取上下文统计信息
        val stats = contextBuilder.getStats()
        println("\n上下文统计信息:")
        println("上下文总数: ${stats["totalContexts"]}")

        val contextsByLevel = stats["contextsByLevel"] as Map<*, *>
        println("\n上下文级别统计:")
        contextsByLevel.entries.sortedByDescending { it.value as Int }.forEach { (level, count) ->
            println("$level: $count")
        }

        val contextsByType = stats["contextsByType"] as Map<*, *>
        println("\n上下文类型统计:")
        contextsByType.entries.sortedByDescending { it.value as Int }.forEach { (type, count) ->
            println("$type: $count")
        }

        // 演示上下文感知搜索
        demonstrateContextAwareSearch(contextBuilder, contextSelector)

        // 取消事件监听
        eventJob.cancel()

        println("\n上下文感知搜索示例完成")
    }

    /**
     * 注册代码解析器
     */
    private fun registerParsers() {
        CodeParserFactory.registerParser(ChapiJavaCodeParser())
        CodeParserFactory.registerParser(ChapiKotlinCodeParser())
    }

    /**
     * 演示上下文感知搜索
     *
     * @param contextBuilder 上下文构建器
     * @param contextSelector 上下文选择器
     */
    private suspend fun demonstrateContextAwareSearch(
        contextBuilder: ContextBuilder,
        contextSelector: IntentBasedContextSelector
    ) {
        println("\n演示上下文感知搜索:")

        // 创建会话 ID
        val sessionId = "example-session"

        // 添加自定义上下文
        println("\n添加自定义上下文:")
        val customContextId = contextBuilder.addCustomContext(
            level = ContextLevel.GLOBAL,
            type = ContextType.DOCUMENTATION,
            content = "这是一个自定义文档上下文，用于演示上下文感知搜索功能。",
            metadata = mapOf("key1" to "value1", "key2" to "value2")
        )

        println("自定义上下文添加成功: $customContextId")

        // 添加查询上下文
        println("\n添加查询上下文:")
        val queryContextId = contextBuilder.addQueryContext(
            query = "什么是上下文感知搜索？",
            sessionId = sessionId
        )

        println("查询上下文添加成功: $queryContextId")

        // 执行第一次搜索
        println("\n第一次搜索:")
        val retrievalContext1 = RetrievalContext(
            query = "什么是上下文感知搜索？",
            sessionId = sessionId
        )

        val result1 = contextSelector.selectContexts(retrievalContext1)

        println("查询意图: ${result1.intent.type}, 置信度: ${result1.intent.confidence}")
        println("选择的上下文数量: ${result1.selectedContexts.size}")

        result1.selectedContexts.forEachIndexed { index, context ->
            val score = result1.relevanceScores[context.id] ?: 0.0
            println("${index + 1}. 级别: ${context.level}, 类型: ${context.type}, 分数: $score")
            println("   内容: ${context.content.take(100)}${if (context.content.length > 100) "..." else ""}")
        }

        // 添加用户反馈上下文
        println("\n添加用户反馈上下文:")
        val feedbackContextId = contextBuilder.addFeedbackContext(
            memoryId = "memory1",
            score = 0.8,
            comment = "这个结果很有帮助",
            sessionId = sessionId
        )

        println("用户反馈上下文添加成功: $feedbackContextId")

        // 执行第二次搜索
        println("\n第二次搜索:")
        val retrievalContext2 = RetrievalContext(
            query = "如何实现上下文感知搜索？",
            previousQueries = listOf("什么是上下文感知搜索？"),
            sessionId = sessionId
        )

        val result2 = contextSelector.selectContexts(retrievalContext2)

        println("查询意图: ${result2.intent.type}, 置信度: ${result2.intent.confidence}")
        println("选择的上下文数量: ${result2.selectedContexts.size}")

        result2.selectedContexts.forEachIndexed { index, context ->
            val score = result2.relevanceScores[context.id] ?: 0.0
            println("${index + 1}. 级别: ${context.level}, 类型: ${context.type}, 分数: $score")
            println("   内容: ${context.content.take(100)}${if (context.content.length > 100) "..." else ""}")
        }

        // 执行第三次搜索（带当前文件和选中文本）
        println("\n第三次搜索:")
        val retrievalContext3 = RetrievalContext(
            query = "上下文选择器如何工作？",
            previousQueries = listOf("什么是上下文感知搜索？", "如何实现上下文感知搜索？"),
            currentFile = "IntentBasedContextSelector.kt",
            selectedText = "selectContexts",
            sessionId = sessionId
        )

        val result3 = contextSelector.selectContexts(retrievalContext3)

        println("查询意图: ${result3.intent.type}, 置信度: ${result3.intent.confidence}")
        println("选择的上下文数量: ${result3.selectedContexts.size}")

        result3.selectedContexts.forEachIndexed { index, context ->
            val score = result3.relevanceScores[context.id] ?: 0.0
            println("${index + 1}. 级别: ${context.level}, 类型: ${context.type}, 分数: $score")
            println("   内容: ${context.content.take(100)}${if (context.content.length > 100) "..." else ""}")
        }

        // 更新上下文
        println("\n更新上下文:")
        val updateSuccess = contextBuilder.updateContext(
            contextId = customContextId,
            content = "这是更新后的自定义文档上下文，用于演示上下文感知搜索功能。",
            metadata = mapOf("key3" to "value3")
        )

        println("上下文更新${if (updateSuccess) "成功" else "失败"}")

        // 移除上下文
        println("\n移除上下文:")
        val removeSuccess = contextBuilder.removeContext(customContextId)

        println("上下文移除${if (removeSuccess) "成功" else "失败"}")
    }
}
*/
