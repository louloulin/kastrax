package ai.kastrax.codebase.examples

// TODO: 暂时注释掉示例代码，等待相关依赖问题解决

// 空实现以避免语法错误
class SymbolGraphExample

/*
import ai.kastrax.codebase.semantic.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.CodeRelationAnalyzerConfig
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.parser.ChapiGoCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiPythonCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiTypeScriptCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.codebase.symbol.RelationDirection
import ai.kastrax.codebase.symbol.RelationQuery
import ai.kastrax.codebase.symbol.SymbolGraphBuilder
import ai.kastrax.codebase.symbol.SymbolGraphBuilderConfig
import ai.kastrax.codebase.symbol.SymbolIndexer
import ai.kastrax.codebase.symbol.SymbolIndexerConfig
import ai.kastrax.codebase.symbol.SymbolQuery
import ai.kastrax.codebase.symbol.SymbolQueryEngine
import ai.kastrax.codebase.symbol.SymbolQueryEngineConfig
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType
import ai.kastrax.codebase.symbol.visualization.OutputFormat
import ai.kastrax.codebase.symbol.visualization.SymbolGraphVisualizer
import ai.kastrax.codebase.symbol.visualization.SymbolGraphVisualizerConfig
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path

/**
 * 符号关系图示例
 */
object SymbolGraphExample {

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

        // 创建符号索引器
        val symbolIndexer = SymbolIndexer(
            config = SymbolIndexerConfig(
                enableFullTextSearch = true,
                enableFuzzySearch = true
            )
        )

        // 创建符号查询引擎
        val symbolQueryEngine = SymbolQueryEngine(
            indexer = symbolIndexer,
            config = SymbolQueryEngineConfig(
                enableCaching = true
            )
        )

        // 创建符号关系图可视化器
        val symbolGraphVisualizer = SymbolGraphVisualizer(
            config = SymbolGraphVisualizerConfig(
                maxNodes = 100,
                outputFormat = OutputFormat.HTML
            )
        )

        // 构建符号关系图
        println("正在构建符号关系图...")
        val graph = symbolGraphBuilder.buildGraphFromDirectory(codebasePath)

        // 索引符号关系图
        println("正在索引符号关系图...")
        symbolIndexer.indexGraph(graph)

        // 打印符号关系图统计信息
        val stats = graph.getStats()
        println("\n符号关系图统计信息:")
        println("节点数量: ${stats["nodeCount"]}")
        println("关系数量: ${stats["relationCount"]}")

        val nodesByType = stats["nodesByType"] as Map<*, *>
        println("\n节点类型统计:")
        nodesByType.entries.sortedByDescending { it.value as Int }.forEach { (type, count) ->
            println("$type: $count")
        }

        val relationsByType = stats["relationsByType"] as Map<*, *>
        println("\n关系类型统计:")
        relationsByType.entries.sortedByDescending { it.value as Int }.forEach { (type, count) ->
            println("$type: $count")
        }

        // 演示查询功能
        demonstrateQueries(symbolQueryEngine)

        // 可视化符号关系图
        println("\n正在可视化符号关系图...")
        val outputPath = Path("symbol-graph.html")
        val success = symbolGraphVisualizer.visualize(graph, outputPath)

        if (success) {
            println("符号关系图可视化完成: $outputPath")
        } else {
            println("符号关系图可视化失败")
        }

        println("\n符号关系图示例完成")
    }

    /**
     * 注册代码解析器
     */
    private fun registerParsers() {
        CodeParserFactory.registerParser(ChapiJavaCodeParser())
        CodeParserFactory.registerParser(ChapiKotlinCodeParser())
        CodeParserFactory.registerParser(ChapiPythonCodeParser())
        CodeParserFactory.registerParser(ChapiTypeScriptCodeParser())
        CodeParserFactory.registerParser(ChapiGoCodeParser())
    }

    /**
     * 演示查询功能
     *
     * @param queryEngine 查询引擎
     */
    private suspend fun demonstrateQueries(queryEngine: SymbolQueryEngine) {
        println("\n演示查询功能:")

        // 查询所有类
        println("\n查询所有类:")
        val classQuery = SymbolQuery(type = SymbolType.CLASS)
        val classes = queryEngine.querySymbols(classQuery)

        println("找到 ${classes.size} 个类")

        if (classes.isNotEmpty()) {
            // 选择一个类进行详细查询
            val selectedClass = classes.first()
            println("\n选择类: ${selectedClass.name}")

            // 查询类的方法
            val methods = queryEngine.queryClassMethods(selectedClass)
            println("方法数量: ${methods.size}")
            methods.take(5).forEach { method ->
                println("- ${method.name}")
            }

            // 查询类的字段
            val fields = queryEngine.queryClassFields(selectedClass)
            println("字段数量: ${fields.size}")
            fields.take(5).forEach { field ->
                println("- ${field.name}")
            }

            // 查询类的父类
            val superclasses = queryEngine.queryClassSuperclasses(selectedClass)
            println("父类数量: ${superclasses.size}")
            superclasses.forEach { superclass ->
                println("- ${superclass.name}")
            }

            // 查询类实现的接口
            val interfaces = queryEngine.queryClassInterfaces(selectedClass)
            println("接口数量: ${interfaces.size}")
            interfaces.forEach { interface_ ->
                println("- ${interface_.name}")
            }

            // 查询类的子类
            val subclasses = queryEngine.queryClassSubclasses(selectedClass)
            println("子类数量: ${subclasses.size}")
            subclasses.take(5).forEach { subclass ->
                println("- ${subclass.name}")
            }

            // 如果有方法，查询方法的调用关系
            if (methods.isNotEmpty()) {
                val selectedMethod = methods.first()
                println("\n选择方法: ${selectedMethod.name}")

                // 查询方法调用的方法
                val methodCalls = queryEngine.queryMethodCalls(selectedMethod)
                println("调用方法数量: ${methodCalls.size}")
                methodCalls.forEach { call ->
                    println("- ${call.name}")
                }

                // 查询调用该方法的方法
                val methodCallers = queryEngine.queryMethodCallers(selectedMethod)
                println("被调用次数: ${methodCallers.size}")
                methodCallers.take(5).forEach { caller ->
                    println("- ${caller.name}")
                }

                // 查询方法重写的方法
                val overriddenMethods = queryEngine.queryMethodOverrides(selectedMethod)
                println("重写方法数量: ${overriddenMethods.size}")
                overriddenMethods.forEach { overridden ->
                    println("- ${overridden.name}")
                }

                // 查询重写该方法的方法
                val overridingMethods = queryEngine.queryMethodOverriddenBy(selectedMethod)
                println("被重写次数: ${overridingMethods.size}")
                overridingMethods.forEach { overriding ->
                    println("- ${overriding.name}")
                }
            }
        }

        // 全文搜索
        println("\n全文搜索:")
        val searchQuery = SymbolQuery(searchText = "main")
        val searchResults = queryEngine.querySymbols(searchQuery)

        println("找到 ${searchResults.size} 个结果")
        searchResults.take(5).forEach { result ->
            println("- ${result.name} (${result.type.name.lowercase()})")
        }

        // 模糊搜索
        println("\n模糊搜索:")
        val fuzzyQuery = SymbolQuery(fuzzyText = "test")
        val fuzzyResults = queryEngine.querySymbols(fuzzyQuery)

        println("找到 ${fuzzyResults.size} 个结果")
        fuzzyResults.take(5).forEach { result ->
            println("- ${result.name} (${result.type.name.lowercase()})")
        }

        // 关系查询
        println("\n关系查询:")
        if (classes.isNotEmpty()) {
            val relationQuery = SymbolQuery(
                relationQuery = RelationQuery(
                    sourceId = classes.first().id,
                    type = SymbolRelationType.IMPLEMENTS,
                    direction = RelationDirection.OUTGOING
                )
            )

            val relationResults = queryEngine.querySymbols(relationQuery)

            println("找到 ${relationResults.size} 个结果")
            relationResults.forEach { result ->
                println("- ${result.name} (${result.type.name.lowercase()})")
            }
        }
    }
}
*/
