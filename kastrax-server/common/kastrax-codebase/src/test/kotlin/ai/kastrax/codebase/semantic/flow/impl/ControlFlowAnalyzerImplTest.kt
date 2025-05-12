package ai.kastrax.codebase.semantic.flow.impl

import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerConfig
import ai.kastrax.codebase.semantic.flow.FlowEdgeType
import ai.kastrax.codebase.semantic.flow.FlowNodeType
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.*

class ControlFlowAnalyzerImplTest {
    
    private lateinit var analyzer: ControlFlowAnalyzerImpl
    
    @BeforeEach
    fun setUp() {
        analyzer = ControlFlowAnalyzerImpl(
            config = CodeFlowAnalyzerConfig(
                maxDepth = 5,
                analyzeControlFlow = true,
                analyzeDataFlow = false
            )
        )
    }
    
    @Test
    fun `test analyze empty method`() = runBlocking {
        // 创建一个空方法
        val method = createMethodElement("emptyMethod", emptyList())
        
        // 分析方法
        val flowGraph = analyzer.analyzeFlow(method)
        
        // 验证结果
        assertNotNull(flowGraph)
        assertEquals(2, flowGraph.nodes.size) // 入口和出口节点
        assertEquals(1, flowGraph.edges.size) // 入口到出口的边
        
        // 验证入口和出口节点
        assertNotNull(flowGraph.entryNodeId)
        assertEquals(1, flowGraph.exitNodeIds.size)
        
        // 验证节点类型
        val entryNode = flowGraph.nodes[flowGraph.entryNodeId]
        assertNotNull(entryNode)
        assertEquals(FlowNodeType.ENTRY, entryNode!!.type)
        
        val exitNodeId = flowGraph.exitNodeIds.first()
        val exitNode = flowGraph.nodes[exitNodeId]
        assertNotNull(exitNode)
        assertEquals(FlowNodeType.EXIT, exitNode!!.type)
        
        // 验证边类型
        val edge = flowGraph.edges.values.first()
        assertEquals(FlowEdgeType.SEQUENTIAL, edge.type)
        assertEquals(flowGraph.entryNodeId, edge.sourceId)
        assertEquals(exitNodeId, edge.targetId)
    }
    
    @Test
    fun `test analyze method with statements`() = runBlocking {
        // 创建一个包含语句的方法
        val statements = listOf(
            createStatementElement("statement1"),
            createStatementElement("statement2"),
            createStatementElement("statement3")
        )
        val method = createMethodElement("methodWithStatements", statements)
        
        // 分析方法
        val flowGraph = analyzer.analyzeFlow(method)
        
        // 验证结果
        assertNotNull(flowGraph)
        assertEquals(5, flowGraph.nodes.size) // 入口、3个语句和出口节点
        assertEquals(4, flowGraph.edges.size) // 入口到语句1，语句1到语句2，语句2到语句3，语句3到出口
        
        // 验证入口和出口节点
        assertNotNull(flowGraph.entryNodeId)
        assertEquals(1, flowGraph.exitNodeIds.size)
        
        // 验证节点类型
        val entryNode = flowGraph.nodes[flowGraph.entryNodeId]
        assertNotNull(entryNode)
        assertEquals(FlowNodeType.ENTRY, entryNode!!.type)
        
        val exitNodeId = flowGraph.exitNodeIds.first()
        val exitNode = flowGraph.nodes[exitNodeId]
        assertNotNull(exitNode)
        assertEquals(FlowNodeType.EXIT, exitNode!!.type)
        
        // 验证语句节点
        val statementNodes = flowGraph.nodes.values.filter { it.type == FlowNodeType.STATEMENT }
        assertEquals(3, statementNodes.size)
        
        // 验证边连接
        val entryOutEdges = flowGraph.getOutgoingEdges(flowGraph.entryNodeId!!)
        assertEquals(1, entryOutEdges.size)
        
        val exitInEdges = flowGraph.getIncomingEdges(exitNodeId)
        assertEquals(1, exitInEdges.size)
    }
    
    @Test
    fun `test analyze method with if statement`() = runBlocking {
        // 创建一个包含 if 语句的方法
        val ifStatement = createIfStatementElement("ifCondition", listOf(
            createStatementElement("thenStatement")
        ))
        val method = createMethodElement("methodWithIf", listOf(ifStatement))
        
        // 分析方法
        val flowGraph = analyzer.analyzeFlow(method)
        
        // 验证结果
        assertNotNull(flowGraph)
        assertTrue(flowGraph.nodes.size >= 4) // 入口、if条件、then语句和出口节点
        
        // 验证 if 节点
        val ifNodes = flowGraph.nodes.values.filter { it.type == FlowNodeType.CONDITION }
        assertFalse(ifNodes.isEmpty())
        
        // 验证 then 语句节点
        val statementNodes = flowGraph.nodes.values.filter { it.type == FlowNodeType.STATEMENT }
        assertFalse(statementNodes.isEmpty())
    }
    
    @Test
    fun `test analyze class`() = runBlocking {
        // 创建一个类
        val methods = listOf(
            createMethodElement("method1", emptyList()),
            createMethodElement("method2", emptyList())
        )
        val classElement = createClassElement("TestClass", methods)
        
        // 分析类
        val flowGraph = analyzer.analyzeFlow(classElement)
        
        // 验证结果
        assertNotNull(flowGraph)
        assertTrue(flowGraph.nodes.size >= 4) // 入口、2个方法和出口节点
        
        // 验证入口和出口节点
        assertNotNull(flowGraph.entryNodeId)
        assertEquals(1, flowGraph.exitNodeIds.size)
        
        // 验证方法节点
        val methodNodes = flowGraph.nodes.values.filter { it.type == FlowNodeType.CALL }
        assertEquals(2, methodNodes.size)
        
        // 验证边连接
        val entryOutEdges = flowGraph.getOutgoingEdges(flowGraph.entryNodeId!!)
        assertEquals(2, entryOutEdges.size) // 入口到两个方法
        
        val exitInEdges = flowGraph.getIncomingEdges(flowGraph.exitNodeIds.first())
        assertEquals(2, exitInEdges.size) // 两个方法到出口
    }
    
    @Test
    fun `test analyze file`() = runBlocking {
        // 创建一个文件
        val classes = listOf(
            createClassElement("Class1", emptyList()),
            createClassElement("Class2", emptyList())
        )
        val functions = listOf(
            createMethodElement("function1", emptyList())
        )
        val fileElement = createFileElement("TestFile.kt", classes + functions)
        
        // 分析文件
        val flowGraph = analyzer.analyzeFlow(fileElement)
        
        // 验证结果
        assertNotNull(flowGraph)
        assertTrue(flowGraph.nodes.size >= 5) // 入口、2个类、1个函数和出口节点
        
        // 验证入口和出口节点
        assertNotNull(flowGraph.entryNodeId)
        assertEquals(1, flowGraph.exitNodeIds.size)
        
        // 验证类和函数节点
        val topLevelNodes = flowGraph.nodes.values.filter { 
            it.type == FlowNodeType.STATEMENT || it.type == FlowNodeType.CALL 
        }
        assertEquals(3, topLevelNodes.size)
        
        // 验证边连接
        val entryOutEdges = flowGraph.getOutgoingEdges(flowGraph.entryNodeId!!)
        assertEquals(3, entryOutEdges.size) // 入口到3个顶级元素
        
        val exitInEdges = flowGraph.getIncomingEdges(flowGraph.exitNodeIds.first())
        assertEquals(3, exitInEdges.size) // 3个顶级元素到出口
    }
    
    @Test
    fun `test unsupported element type`() = runBlocking {
        // 创建一个不支持的元素类型
        val element = CodeElement(
            id = UUID.randomUUID().toString(),
            name = "unsupported",
            qualifiedName = "unsupported",
            type = CodeElementType.UNKNOWN,
            location = Location(
                filePath = Path.of("test.kt"),
                startLine = 1,
                startColumn = 1,
                endLine = 1,
                endColumn = 1
            )
        )
        
        // 分析元素
        val flowGraph = analyzer.analyzeFlow(element)
        
        // 验证结果
        assertNotNull(flowGraph)
        assertEquals(2, flowGraph.nodes.size) // 入口和出口节点
        assertEquals(1, flowGraph.edges.size) // 入口到出口的边
        assertTrue(flowGraph.metadata["isEmpty"] as Boolean)
    }
    
    // 辅助方法：创建方法元素
    private fun createMethodElement(name: String, statements: List<CodeElement>): CodeElement {
        val method = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = "test.$name",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = Path.of("test.kt"),
                startLine = 1,
                startColumn = 1,
                endLine = statements.size + 1,
                endColumn = 1
            ),
            language = "kotlin"
        )
        
        // 添加语句作为子元素
        statements.forEach { statement ->
            statement.parent = method
            method.children.add(statement)
        }
        
        return method
    }
    
    // 辅助方法：创建语句元素
    private fun createStatementElement(name: String): CodeElement {
        return CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = name,
            type = CodeElementType.STATEMENT,
            location = Location(
                filePath = Path.of("test.kt"),
                startLine = 1,
                startColumn = 1,
                endLine = 1,
                endColumn = 10
            ),
            language = "kotlin"
        )
    }
    
    // 辅助方法：创建 if 语句元素
    private fun createIfStatementElement(condition: String, thenStatements: List<CodeElement>): CodeElement {
        val ifStatement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = "if ($condition)",
            qualifiedName = "if ($condition)",
            type = CodeElementType.IF_STATEMENT,
            location = Location(
                filePath = Path.of("test.kt"),
                startLine = 1,
                startColumn = 1,
                endLine = thenStatements.size + 1,
                endColumn = 1
            ),
            language = "kotlin"
        )
        
        // 添加 then 语句作为子元素
        thenStatements.forEach { statement ->
            statement.parent = ifStatement
            ifStatement.children.add(statement)
        }
        
        return ifStatement
    }
    
    // 辅助方法：创建类元素
    private fun createClassElement(name: String, methods: List<CodeElement>): CodeElement {
        val classElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = "test.$name",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = Path.of("test.kt"),
                startLine = 1,
                startColumn = 1,
                endLine = methods.size + 1,
                endColumn = 1
            ),
            language = "kotlin"
        )
        
        // 添加方法作为子元素
        methods.forEach { method ->
            method.parent = classElement
            classElement.children.add(method)
        }
        
        return classElement
    }
    
    // 辅助方法：创建文件元素
    private fun createFileElement(name: String, topLevelElements: List<CodeElement>): CodeElement {
        val fileElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = name,
            type = CodeElementType.FILE,
            location = Location(
                filePath = Path.of(name),
                startLine = 1,
                startColumn = 1,
                endLine = topLevelElements.size + 1,
                endColumn = 1
            ),
            language = "kotlin"
        )
        
        // 添加顶级元素作为子元素
        topLevelElements.forEach { element ->
            element.parent = fileElement
            fileElement.children.add(element)
        }
        
        return fileElement
    }
}
