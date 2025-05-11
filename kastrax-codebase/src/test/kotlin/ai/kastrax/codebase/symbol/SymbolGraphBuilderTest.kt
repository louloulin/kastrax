package ai.kastrax.codebase.symbol

import ai.kastrax.codebase.semantic.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.CodeRelationAnalyzerConfig
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.codebase.symbol.model.SymbolType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SymbolGraphBuilderTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var semanticAnalyzer: CodeSemanticAnalyzer
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    private lateinit var symbolGraphBuilder: SymbolGraphBuilder
    
    @BeforeEach
    fun setUp() {
        // 注册解析器
        CodeParserFactory.registerParser(ChapiJavaCodeParser())
        CodeParserFactory.registerParser(ChapiKotlinCodeParser())
        
        // 创建分析器
        semanticAnalyzer = CodeSemanticAnalyzer(
            config = CodeSemanticAnalyzerConfig(
                excludePatterns = emptySet(),
                excludeDirectories = emptySet()
            )
        )
        
        relationAnalyzer = CodeRelationAnalyzer(
            config = CodeRelationAnalyzerConfig(
                analyzeInheritance = true,
                analyzeUsage = true,
                analyzeDependency = true,
                analyzeOverride = true
            )
        )
        
        symbolGraphBuilder = SymbolGraphBuilder(
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
        
        // 创建测试文件
        createTestFiles()
    }
    
    @Test
    fun `test building symbol graph from file`() = runBlocking {
        // 分析 Java 文件
        val javaFile = tempDir.resolve("TestClass.java")
        val graph = symbolGraphBuilder.buildGraphFromFile(javaFile)
        
        // 验证图
        assertNotNull(graph)
        assertTrue(graph.getNodeCount() > 0)
        assertTrue(graph.getRelationCount() > 0)
        
        // 验证节点类型
        val classNodes = graph.findNodesByType(SymbolType.CLASS)
        val methodNodes = graph.findNodesByType(SymbolType.METHOD)
        val fieldNodes = graph.findNodesByType(SymbolType.FIELD)
        
        assertTrue(classNodes.isNotEmpty())
        assertTrue(methodNodes.isNotEmpty())
        assertTrue(fieldNodes.isNotEmpty())
        
        // 验证节点名称
        val testClassNode = graph.findNodesByName("TestClass").firstOrNull()
        assertNotNull(testClassNode)
        assertEquals("TestClass", testClassNode.name)
        
        // 验证关系
        val outgoingRelations = graph.getOutgoingRelations(testClassNode.id)
        assertTrue(outgoingRelations.isNotEmpty())
    }
    
    @Test
    fun `test building symbol graph from directory`() = runBlocking {
        // 分析目录
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 验证图
        assertNotNull(graph)
        assertTrue(graph.getNodeCount() > 0)
        assertTrue(graph.getRelationCount() > 0)
        
        // 验证节点类型
        val classNodes = graph.findNodesByType(SymbolType.CLASS)
        val interfaceNodes = graph.findNodesByType(SymbolType.INTERFACE)
        val methodNodes = graph.findNodesByType(SymbolType.METHOD)
        
        assertTrue(classNodes.isNotEmpty())
        assertTrue(interfaceNodes.isNotEmpty())
        assertTrue(methodNodes.isNotEmpty())
        
        // 验证节点名称
        val testClassNode = graph.findNodesByName("TestClass").firstOrNull()
        val testInterfaceNode = graph.findNodesByName("TestInterface").firstOrNull()
        
        assertNotNull(testClassNode)
        assertNotNull(testInterfaceNode)
        
        // 验证继承关系
        val implementsRelations = graph.getOutgoingRelations(testClassNode.id, SymbolRelationType.IMPLEMENTS)
        assertTrue(implementsRelations.isNotEmpty())
        assertEquals(testInterfaceNode.id, implementsRelations.first().targetId)
    }
    
    @Test
    fun `test merging symbol graphs`() = runBlocking {
        // 分析 Java 文件
        val javaFile = tempDir.resolve("TestClass.java")
        val graph1 = symbolGraphBuilder.buildGraphFromFile(javaFile)
        
        // 分析 Kotlin 文件
        val kotlinFile = tempDir.resolve("TestClass.kt")
        val graph2 = symbolGraphBuilder.buildGraphFromFile(kotlinFile)
        
        // 合并图
        val mergedGraph = symbolGraphBuilder.mergeGraphs(listOf(graph1, graph2))
        
        // 验证合并后的图
        assertNotNull(mergedGraph)
        assertTrue(mergedGraph.getNodeCount() >= graph1.getNodeCount() + graph2.getNodeCount())
        assertTrue(mergedGraph.getRelationCount() >= graph1.getRelationCount() + graph2.getRelationCount())
    }
    
    /**
     * 创建测试文件
     */
    private fun createTestFiles() {
        // 创建 Java 文件
        val javaFile = tempDir.resolve("TestClass.java")
        javaFile.writeText("""
            package ai.kastrax.codebase.test;
            
            import java.util.List;
            import java.util.ArrayList;
            
            /**
             * 这是一个测试类，用于测试符号关系图构建器。
             */
            public class TestClass implements TestInterface {
                private String name;
                private int age;
                private List<String> items;
                
                /**
                 * 构造函数
                 */
                public TestClass(String name, int age) {
                    this.name = name;
                    this.age = age;
                    this.items = new ArrayList<>();
                }
                
                /**
                 * 获取名称
                 */
                @Override
                public String getName() {
                    return name;
                }
                
                /**
                 * 设置名称
                 */
                public void setName(String name) {
                    this.name = name;
                }
                
                /**
                 * 获取年龄
                 */
                @Override
                public int getAge() {
                    return age;
                }
                
                /**
                 * 设置年龄
                 */
                public void setAge(int age) {
                    this.age = age;
                }
                
                /**
                 * 测试方法
                 */
                @Override
                public void testMethod() {
                    System.out.println("This is a test method.");
                    anotherMethod();
                }
                
                /**
                 * 另一个方法
                 */
                private void anotherMethod() {
                    System.out.println("This is another method.");
                }
                
                /**
                 * 添加项目
                 */
                public void addItem(String item) {
                    items.add(item);
                }
                
                /**
                 * 获取项目
                 */
                public List<String> getItems() {
                    return items;
                }
                
                /**
                 * 内部类
                 */
                public static class InnerClass {
                    private String value;
                    
                    public InnerClass(String value) {
                        this.value = value;
                    }
                    
                    public String getValue() {
                        return value;
                    }
                }
            }
        """.trimIndent())
        
        // 创建 Java 接口文件
        val javaInterfaceFile = tempDir.resolve("TestInterface.java")
        javaInterfaceFile.writeText("""
            package ai.kastrax.codebase.test;
            
            /**
             * 这是一个测试接口，用于测试符号关系图构建器。
             */
            public interface TestInterface {
                /**
                 * 获取名称
                 */
                String getName();
                
                /**
                 * 获取年龄
                 */
                int getAge();
                
                /**
                 * 测试方法
                 */
                void testMethod();
            }
        """.trimIndent())
        
        // 创建 Java 子类文件
        val javaSubclassFile = tempDir.resolve("TestSubclass.java")
        javaSubclassFile.writeText("""
            package ai.kastrax.codebase.test;
            
            /**
             * 这是一个测试子类，用于测试符号关系图构建器。
             */
            public class TestSubclass extends TestClass {
                private boolean active;
                
                /**
                 * 构造函数
                 */
                public TestSubclass(String name, int age, boolean active) {
                    super(name, age);
                    this.active = active;
                }
                
                /**
                 * 获取活动状态
                 */
                public boolean isActive() {
                    return active;
                }
                
                /**
                 * 设置活动状态
                 */
                public void setActive(boolean active) {
                    this.active = active;
                }
                
                /**
                 * 重写测试方法
                 */
                @Override
                public void testMethod() {
                    System.out.println("This is an overridden test method.");
                    super.testMethod();
                }
            }
        """.trimIndent())
        
        // 创建 Kotlin 文件
        val kotlinFile = tempDir.resolve("TestClass.kt")
        kotlinFile.writeText("""
            package ai.kastrax.codebase.test
            
            import java.util.List
            import java.util.ArrayList
            
            /**
             * 这是一个 Kotlin 测试类，用于测试符号关系图构建器。
             */
            class TestClass(
                private var name: String,
                private var age: Int
            ) : TestInterface {
                private val items: MutableList<String> = ArrayList()
                
                /**
                 * 获取名称
                 */
                override fun getName(): String {
                    return name
                }
                
                /**
                 * 设置名称
                 */
                fun setName(name: String) {
                    this.name = name
                }
                
                /**
                 * 获取年龄
                 */
                override fun getAge(): Int {
                    return age
                }
                
                /**
                 * 设置年龄
                 */
                fun setAge(age: Int) {
                    this.age = age
                }
                
                /**
                 * 测试方法
                 */
                override fun testMethod() {
                    println("This is a test method.")
                    anotherMethod()
                }
                
                /**
                 * 另一个方法
                 */
                private fun anotherMethod() {
                    println("This is another method.")
                }
                
                /**
                 * 添加项目
                 */
                fun addItem(item: String) {
                    items.add(item)
                }
                
                /**
                 * 获取项目
                 */
                fun getItems(): List<String> {
                    return items
                }
                
                /**
                 * 内部类
                 */
                class InnerClass(private val value: String) {
                    fun getValue(): String {
                        return value
                    }
                }
                
                /**
                 * 伴生对象
                 */
                companion object {
                    const val VERSION = "1.0"
                    
                    fun create(name: String, age: Int): TestClass {
                        return TestClass(name, age)
                    }
                }
            }
        """.trimIndent())
    }
}
