package ai.kastrax.codebase.examples

// TODO: 暂时注释掉，等待依赖问题解决

// 空实现以避免语法错误
class CodeSemanticAnalysisExample

/*
import ai.kastrax.codebase.semantic.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.CodeRelationAnalyzerConfig
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.CodeSemanticQueryEngine
import ai.kastrax.codebase.semantic.ElementQuery
import ai.kastrax.codebase.semantic.RelationDirection
import ai.kastrax.codebase.semantic.RelationQuery
import ai.kastrax.codebase.semantic.RelationType
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.parser.ChapiGoCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiPythonCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiTypeScriptCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path

/**
 * 代码语义分析示例
 */
object CodeSemanticAnalysisExample {

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

        // 创建代码语义查询引擎
        val queryEngine = CodeSemanticQueryEngine(
            analyzer = semanticAnalyzer,
            relationAnalyzer = relationAnalyzer
        )

        // 分析代码库
        println("正在分析代码结构...")
        val codebase = semanticAnalyzer.analyzeCodebase(codebasePath)

        // 分析代码关系
        println("正在分析代码关系...")
        val relations = relationAnalyzer.analyzeRelations(codebase)

        // 打印代码库统计信息
        printCodebaseStats(codebase)

        // 打印关系统计信息
        printRelationStats(relations)

        // 演示查询功能
        demonstrateQueries(queryEngine)

        println("\n代码语义分析示例完成")
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
     * 打印代码库统计信息
     *
     * @param codebase 代码库元素
     */
    private fun printCodebaseStats(codebase: ai.kastrax.codebase.semantic.model.CodeElement) {
        println("\n代码库统计信息:")

        // 获取所有元素
        val allElements = codebase.getAllChildren() + codebase

        // 按类型统计元素
        val elementsByType = allElements.groupBy { it.type }

        // 打印统计信息
        println("总元素数量: ${allElements.size}")

        elementsByType.forEach { (type, elements) ->
            println("${type.name}: ${elements.size}")
        }
    }

    /**
     * 打印关系统计信息
     *
     * @param relations 关系列表
     */
    private fun printRelationStats(relations: List<ai.kastrax.codebase.semantic.CodeRelation>) {
        println("\n关系统计信息:")

        // 按类型统计关系
        val relationsByType = relations.groupBy { it.type }

        // 打印统计信息
        println("总关系数量: ${relations.size}")

        relationsByType.forEach { (type, rels) ->
            println("${type.name}: ${rels.size}")
        }
    }

    /**
     * 演示查询功能
     *
     * @param queryEngine 查询引擎
     */
    private suspend fun demonstrateQueries(queryEngine: CodeSemanticQueryEngine) {
        println("\n演示查询功能:")

        // 查询所有类
        println("\n查询所有类:")
        val classQuery = ElementQuery(type = CodeElementType.CLASS)
        val classes = queryEngine.queryElements(classQuery)

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

            // 查询类的依赖
            val dependencies = queryEngine.queryElementDependencies(selectedClass)
            println("依赖数量: ${dependencies.size}")
            dependencies.take(5).forEach { dependency ->
                println("- ${dependency.name}")
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

            // 使用关系查询
            println("\n使用关系查询:")

            // 查询实现关系
            val implementsQuery = ElementQuery(
                relationQuery = RelationQuery(
                    sourceId = selectedClass.id,
                    type = RelationType.IMPLEMENTS,
                    direction = RelationDirection.OUTGOING
                )
            )

            val implementsResults = queryEngine.queryElements(implementsQuery)
            println("实现接口数量: ${implementsResults.size}")
            implementsResults.forEach { result ->
                println("- ${result.name}")
            }
        }
    }
}
*/
