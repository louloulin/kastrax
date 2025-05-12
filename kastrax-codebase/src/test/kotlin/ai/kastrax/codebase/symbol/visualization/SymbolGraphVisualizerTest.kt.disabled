package ai.kastrax.codebase.symbol.visualization

import ai.kastrax.codebase.semantic.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.CodeRelationAnalyzerConfig
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.codebase.symbol.SymbolGraphBuilder
import ai.kastrax.codebase.symbol.SymbolGraphBuilderConfig
import ai.kastrax.codebase.symbol.model.SymbolType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertTrue

class SymbolGraphVisualizerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var semanticAnalyzer: CodeSemanticAnalyzer
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    private lateinit var symbolGraphBuilder: SymbolGraphBuilder
    private lateinit var symbolGraphVisualizer: SymbolGraphVisualizer
    
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
        
        symbolGraphVisualizer = SymbolGraphVisualizer(
            config = SymbolGraphVisualizerConfig(
                maxNodes = 100,
                includeNodeTypes = null,
                includeRelationTypes = null,
                outputFormat = OutputFormat.DOT
            )
        )
        
        // 创建测试文件
        createTestFiles()
    }
    
    @Test
    fun `test visualizing symbol graph as DOT`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 可视化为 DOT 格式
        val outputPath = tempDir.resolve("symbol-graph.dot")
        val success = symbolGraphVisualizer.visualize(graph, outputPath)
        
        // 验证结果
        assertTrue(success)
        assertTrue(File(outputPath.toString()).exists())
        assertTrue(File(outputPath.toString()).readText().contains("digraph"))
    }
    
    @Test
    fun `test visualizing symbol graph as JSON`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 可视化为 JSON 格式
        val outputPath = tempDir.resolve("symbol-graph.json")
        val jsonVisualizer = SymbolGraphVisualizer(
            config = SymbolGraphVisualizerConfig(
                outputFormat = OutputFormat.JSON
            )
        )
        
        val success = jsonVisualizer.visualize(graph, outputPath)
        
        // 验证结果
        assertTrue(success)
        assertTrue(File(outputPath.toString()).exists())
        assertTrue(File(outputPath.toString()).readText().contains("\"nodes\""))
        assertTrue(File(outputPath.toString()).readText().contains("\"relations\""))
    }
    
    @Test
    fun `test visualizing symbol graph as HTML`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 可视化为 HTML 格式
        val outputPath = tempDir.resolve("symbol-graph.html")
        val htmlVisualizer = SymbolGraphVisualizer(
            config = SymbolGraphVisualizerConfig(
                outputFormat = OutputFormat.HTML
            )
        )
        
        val success = htmlVisualizer.visualize(graph, outputPath)
        
        // 验证结果
        assertTrue(success)
        assertTrue(File(outputPath.toString()).exists())
        assertTrue(File(outputPath.toString()).readText().contains("<html>"))
        assertTrue(File(outputPath.toString()).readText().contains("<script>"))
    }
    
    @Test
    fun `test visualizing symbol graph with node type filter`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 可视化为 DOT 格式，只包含类和接口
        val outputPath = tempDir.resolve("symbol-graph-filtered.dot")
        val filteredVisualizer = SymbolGraphVisualizer(
            config = SymbolGraphVisualizerConfig(
                includeNodeTypes = setOf(SymbolType.CLASS, SymbolType.INTERFACE)
            )
        )
        
        val success = filteredVisualizer.visualize(graph, outputPath)
        
        // 验证结果
        assertTrue(success)
        assertTrue(File(outputPath.toString()).exists())
        
        // 验证只包含类和接口
        val dotContent = File(outputPath.toString()).readText()
        assertTrue(dotContent.contains("(class)"))
        assertTrue(dotContent.contains("(interface)"))
        assertTrue(!dotContent.contains("(method)") || !dotContent.contains("(field)"))
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
             * 这是一个测试类，用于测试符号关系图可视化器。
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
            }
        """.trimIndent())
        
        // 创建 Java 接口文件
        val javaInterfaceFile = tempDir.resolve("TestInterface.java")
        javaInterfaceFile.writeText("""
            package ai.kastrax.codebase.test;
            
            /**
             * 这是一个测试接口，用于测试符号关系图可视化器。
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
             * 这是一个测试子类，用于测试符号关系图可视化器。
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
    }
}
