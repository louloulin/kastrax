package ai.kastrax.codebase.examples

import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerConfig
import ai.kastrax.codebase.semantic.flow.FlowNodeType
import ai.kastrax.codebase.semantic.flow.impl.CodeFlowAnalyzerImpl
import ai.kastrax.codebase.semantic.flow.impl.ControlFlowAnalyzerImpl
import ai.kastrax.codebase.semantic.flow.impl.DataFlowAnalyzerImpl
import ai.kastrax.codebase.semantic.flow.lang.JavaKotlinFlowAnalyzer
import ai.kastrax.codebase.semantic.flow.viz.FlowGraphExplorer
import ai.kastrax.codebase.semantic.flow.viz.FlowGraphExplorerConfig
import ai.kastrax.codebase.semantic.flow.viz.FlowGraphRenderer
import ai.kastrax.codebase.semantic.flow.viz.OutputFormat
import ai.kastrax.codebase.semantic.model.CodeElementType
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 代码流分析示例
 *
 * 演示如何使用代码流分析功能分析代码的控制流和数据流
 */
object CodeFlowAnalysisExample {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("=== 代码流分析示例 ===")
        
        // 解析命令行参数
        if (args.isEmpty()) {
            println("用法: CodeFlowAnalysisExample <代码库路径> [输出目录]")
            return@runBlocking
        }
        
        val codebasePath = Paths.get(args[0])
        val outputDir = if (args.size > 1) Paths.get(args[1]) else Paths.get("flow-output")
        
        // 创建输出目录
        if (!outputDir.toFile().exists()) {
            outputDir.toFile().mkdirs()
        }
        
        println("代码库路径: $codebasePath")
        println("输出目录: $outputDir")
        
        // 创建语义分析器
        val semanticAnalyzer = CodeSemanticAnalyzer()
        
        // 创建流分析器
        val controlFlowAnalyzer = ControlFlowAnalyzerImpl()
        val dataFlowAnalyzer = DataFlowAnalyzerImpl()
        val codeFlowAnalyzer = CodeFlowAnalyzerImpl(
            controlFlowAnalyzer = controlFlowAnalyzer,
            dataFlowAnalyzer = dataFlowAnalyzer
        )
        val javaKotlinFlowAnalyzer = JavaKotlinFlowAnalyzer(
            baseAnalyzer = codeFlowAnalyzer
        )
        
        // 创建流图渲染器
        val flowGraphRenderer = FlowGraphRenderer()
        
        // 创建流图探索器
        val flowGraphExplorer = FlowGraphExplorer()
        
        // 分析代码库
        println("正在分析代码库...")
        val codebase = semanticAnalyzer.analyzeCodebase(codebasePath)
        
        // 查找所有方法和函数
        val methods = findMethods(codebase)
        println("找到 ${methods.size} 个方法/函数")
        
        // 分析前 5 个方法的流图
        val methodsToAnalyze = methods.take(5)
        for (method in methodsToAnalyze) {
            println("\n分析方法: ${method.qualifiedName}")
            
            // 分析控制流
            val controlFlowGraph = controlFlowAnalyzer.analyzeFlow(method)
            println("控制流图节点数: ${controlFlowGraph.nodes.size}, 边数: ${controlFlowGraph.edges.size}")
            
            // 分析数据流
            val dataFlowGraph = dataFlowAnalyzer.analyzeFlow(method)
            println("数据流图节点数: ${dataFlowGraph.nodes.size}, 边数: ${dataFlowGraph.edges.size}")
            
            // 分析综合流图
            val combinedFlowGraph = codeFlowAnalyzer.analyzeFlow(method)
            println("综合流图节点数: ${combinedFlowGraph.nodes.size}, 边数: ${combinedFlowGraph.edges.size}")
            
            // 渲染流图
            val dotOutput = flowGraphRenderer.render(combinedFlowGraph)
            val outputFile = outputDir.resolve("${sanitizeFileName(method.qualifiedName)}.dot")
            outputFile.toFile().writeText(dotOutput)
            println("流图已保存到: $outputFile")
            
            // 探索流图
            exploreFlowGraph(combinedFlowGraph, flowGraphExplorer)
        }
        
        // 查找所有类
        val classes = findClasses(codebase)
        println("\n找到 ${classes.size} 个类")
        
        // 分析前 3 个类的流图
        val classesToAnalyze = classes.take(3)
        for (classElement in classesToAnalyze) {
            println("\n分析类: ${classElement.qualifiedName}")
            
            // 分析 Java/Kotlin 特定流图
            if (classElement.language.equals("java", ignoreCase = true) || 
                classElement.language.equals("kotlin", ignoreCase = true)) {
                val javaKotlinFlowGraph = javaKotlinFlowAnalyzer.analyzeFlow(classElement)
                println("Java/Kotlin 流图节点数: ${javaKotlinFlowGraph.nodes.size}, 边数: ${javaKotlinFlowGraph.edges.size}")
                
                // 渲染流图
                val dotOutput = flowGraphRenderer.render(javaKotlinFlowGraph)
                val outputFile = outputDir.resolve("${sanitizeFileName(classElement.qualifiedName)}_jk.dot")
                outputFile.toFile().writeText(dotOutput)
                println("Java/Kotlin 流图已保存到: $outputFile")
            }
        }
        
        println("\n代码流分析示例完成")
    }
    
    /**
     * 查找所有方法和函数
     *
     * @param codeElement 代码元素
     * @return 方法和函数列表
     */
    private fun findMethods(codeElement: ai.kastrax.codebase.semantic.model.CodeElement): List<ai.kastrax.codebase.semantic.model.CodeElement> {
        val methods = mutableListOf<ai.kastrax.codebase.semantic.model.CodeElement>()
        
        // 如果当前元素是方法或函数，则添加到列表
        if (codeElement.type == CodeElementType.METHOD || codeElement.type == CodeElementType.FUNCTION) {
            methods.add(codeElement)
        }
        
        // 递归查找子元素中的方法和函数
        for (child in codeElement.children) {
            methods.addAll(findMethods(child))
        }
        
        return methods
    }
    
    /**
     * 查找所有类
     *
     * @param codeElement 代码元素
     * @return 类列表
     */
    private fun findClasses(codeElement: ai.kastrax.codebase.semantic.model.CodeElement): List<ai.kastrax.codebase.semantic.model.CodeElement> {
        val classes = mutableListOf<ai.kastrax.codebase.semantic.model.CodeElement>()
        
        // 如果当前元素是类，则添加到列表
        if (codeElement.type == CodeElementType.CLASS) {
            classes.add(codeElement)
        }
        
        // 递归查找子元素中的类
        for (child in codeElement.children) {
            classes.addAll(findClasses(child))
        }
        
        return classes
    }
    
    /**
     * 探索流图
     *
     * @param flowGraph 流图
     * @param explorer 流图探索器
     */
    private fun exploreFlowGraph(
        flowGraph: ai.kastrax.codebase.semantic.flow.FlowGraph,
        explorer: FlowGraphExplorer
    ) {
        // 查找条件节点
        val conditionNodes = flowGraph.nodes.values.filter { it.type == FlowNodeType.CONDITION }
        println("条件节点数: ${conditionNodes.size}")
        
        // 查找循环
        val cycles = explorer.findCycles(flowGraph)
        println("循环数: ${cycles.size}")
        
        // 查找不可达节点
        val unreachableNodes = explorer.findUnreachableNodes(flowGraph)
        println("不可达节点数: ${unreachableNodes.size}")
        
        // 查找死代码
        val deadCode = explorer.findDeadCode(flowGraph)
        println("死代码节点数: ${deadCode.size}")
    }
    
    /**
     * 清理文件名
     *
     * @param fileName 文件名
     * @return 清理后的文件名
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    }
}
