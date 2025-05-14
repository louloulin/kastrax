package ai.kastrax.codebase.flow

import ai.kastrax.codebase.semantic.flow.FlowType
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerConfig
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class CodeFlowAnalyzerTest {

    private lateinit var controlFlowAnalyzer: ControlFlowAnalyzer
    private lateinit var dataFlowAnalyzer: DataFlowAnalyzer

    @BeforeEach
    fun setUp() {
        val config = CodeFlowAnalyzerConfig()
        controlFlowAnalyzer = ControlFlowAnalyzer(config)
        dataFlowAnalyzer = DataFlowAnalyzer(config)
    }

    @Test
    fun `test control flow analysis for method`() = runBlocking {
        // 创建测试方法元素
        val methodElement = createTestMethodElement()

        // 分析控制流
        val flowGraph = controlFlowAnalyzer.analyze(methodElement)

        // 验证结果
        assertNotNull(flowGraph)
        assertEquals(FlowType.CONTROL_FLOW, flowGraph!!.type)
        assertEquals(methodElement, flowGraph.element)
        assertNotNull(flowGraph.entryNode)
        assertTrue(flowGraph.exitNodes.isNotEmpty())
        assertTrue(flowGraph.nodes.isNotEmpty())
        assertTrue(flowGraph.edges.isNotEmpty())
    }

    @Test
    fun `test data flow analysis for method`() = runBlocking {
        // 创建测试方法元素
        val methodElement = createTestMethodElement()

        // 分析数据流
        val flowGraph = dataFlowAnalyzer.analyze(methodElement)

        // 验证结果
        assertNotNull(flowGraph)
        assertEquals(FlowType.DATA_FLOW, flowGraph!!.type)
        assertEquals(methodElement, flowGraph.element)
        assertTrue(flowGraph.nodes.isNotEmpty())
    }

    @Test
    fun `test control flow analysis for non-method element`() = runBlocking {
        // 创建测试类元素
        val classElement = createTestClassElement()

        // 分析控制流
        val flowGraph = controlFlowAnalyzer.analyze(classElement)

        // 验证结果
        assertEquals(null, flowGraph)
    }

    @Test
    fun `test data flow analysis for non-method element`() = runBlocking {
        // 创建测试类元素
        val classElement = createTestClassElement()

        // 分析数据流
        val flowGraph = dataFlowAnalyzer.analyze(classElement)

        // 验证结果
        assertEquals(null, flowGraph)
    }

    /**
     * 创建测试方法元素
     *
     * @return 方法元素
     */
    private fun createTestMethodElement(): CodeElement {
        val classElement = createTestClassElement()

        val methodElement = CodeElement(
            id = "test-method",
            name = "testMethod",
            qualifiedName = "com.example.TestClass.testMethod",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = Paths.get("src/test/resources/TestClass.java").toString(),
                startLine = 10,
                startColumn = 1,
                endLine = 20,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            parent = classElement
        )

        // 添加方法体
        methodElement.metadata["body"] = """
            public void testMethod(int a, String b) {
                if (a > 0) {
                    System.out.println("Positive: " + a);
                } else {
                    System.out.println("Non-positive: " + a);
                }

                for (int i = 0; i < a; i++) {
                    System.out.println("Loop: " + i);
                }

                try {
                    int result = a / 0;
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }

                return;
            }
        """.trimIndent()

        // 添加方法调用信息
        methodElement.metadata["methodCalls"] = listOf(
            mapOf("target" to "System.out.println"),
            mapOf("target" to "e.getMessage")
        )

        // 添加条件语句信息
        methodElement.metadata["conditions"] = listOf(
            mapOf("condition" to "a > 0"),
            mapOf("condition" to "i < a")
        )

        // 添加循环语句信息
        methodElement.metadata["loops"] = listOf(
            mapOf("condition" to "i < a")
        )

        // 添加异常处理信息
        methodElement.metadata["exceptionHandlers"] = listOf(
            mapOf("exceptionType" to "Exception")
        )

        // 添加局部变量信息
        methodElement.metadata["localVariables"] = listOf(
            mapOf("name" to "i", "type" to "int", "initialValue" to "0"),
            mapOf("name" to "result", "type" to "int")
        )

        // 添加赋值语句信息
        methodElement.metadata["assignments"] = listOf(
            mapOf("target" to "result", "value" to "a / 0")
        )

        // 添加返回语句信息
        methodElement.metadata["returns"] = listOf(
            mapOf("value" to "")
        )

        return methodElement
    }

    /**
     * 创建测试类元素
     *
     * @return 类元素
     */
    private fun createTestClassElement(): CodeElement {
        return CodeElement(
            id = "test-class",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = Paths.get("src/test/resources/TestClass.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 30,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )
    }
}
