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

class SymbolIndexerAndQueryEngineTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var semanticAnalyzer: CodeSemanticAnalyzer
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    private lateinit var symbolGraphBuilder: SymbolGraphBuilder
    private lateinit var symbolIndexer: SymbolIndexer
    private lateinit var symbolQueryEngine: SymbolQueryEngine
    
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
            relationAnalyzer = relationAnalyzer
        )
        
        symbolIndexer = SymbolIndexer(
            config = SymbolIndexerConfig(
                enableFullTextSearch = true,
                enableFuzzySearch = true
            )
        )
        
        symbolQueryEngine = SymbolQueryEngine(
            indexer = symbolIndexer,
            config = SymbolQueryEngineConfig(
                enableCaching = true
            )
        )
        
        // 创建测试文件
        createTestFiles()
    }
    
    @Test
    fun `test indexing and querying symbols by name`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 查询符号
        val query = SymbolQuery(name = "TestClass", type = SymbolType.CLASS)
        val results = symbolQueryEngine.querySymbols(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("TestClass", results.first().name)
        assertEquals(SymbolType.CLASS, results.first().type)
    }
    
    @Test
    fun `test querying symbols by qualified name`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 查询符号
        val query = SymbolQuery(qualifiedName = "ai.kastrax.codebase.test.TestClass")
        val results = symbolQueryEngine.querySymbols(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("TestClass", results.first().name)
    }
    
    @Test
    fun `test full text search`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 全文搜索
        val query = SymbolQuery(searchText = "test method")
        val results = symbolQueryEngine.querySymbols(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
    }
    
    @Test
    fun `test fuzzy search`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 模糊搜索
        val query = SymbolQuery(fuzzyText = "test")
        val results = symbolQueryEngine.querySymbols(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
    }
    
    @Test
    fun `test relation query`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 查询类符号
        val classQuery = SymbolQuery(name = "TestClass", type = SymbolType.CLASS)
        val classResults = symbolQueryEngine.querySymbols(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classNode = classResults.first()
        
        // 查询接口符号
        val interfaceQuery = SymbolQuery(name = "TestInterface", type = SymbolType.INTERFACE)
        val interfaceResults = symbolQueryEngine.querySymbols(interfaceQuery)
        
        // 验证接口结果
        assertTrue(interfaceResults.isNotEmpty())
        val interfaceNode = interfaceResults.first()
        
        // 关系查询
        val relationQuery = SymbolQuery(
            relationQuery = RelationQuery(
                sourceId = classNode.id,
                type = SymbolRelationType.IMPLEMENTS,
                direction = RelationDirection.OUTGOING
            )
        )
        
        val relationResults = symbolQueryEngine.querySymbols(relationQuery)
        
        // 验证关系结果
        assertTrue(relationResults.isNotEmpty())
        assertEquals(interfaceNode.id, relationResults.first().id)
    }
    
    @Test
    fun `test path query`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 查询类符号
        val classQuery = SymbolQuery(name = "TestClass", type = SymbolType.CLASS)
        val classResults = symbolQueryEngine.querySymbols(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classNode = classResults.first()
        
        // 查询方法符号
        val methodQuery = SymbolQuery(name = "testMethod", type = SymbolType.METHOD)
        val methodResults = symbolQueryEngine.querySymbols(methodQuery)
        
        // 验证方法结果
        assertTrue(methodResults.isNotEmpty())
        val methodNode = methodResults.first()
        
        // 路径查询
        val pathQuery = SymbolQuery(
            pathQuery = PathQuery(
                sourceId = classNode.id,
                targetId = methodNode.id,
                maxLength = 3
            )
        )
        
        val pathResults = symbolQueryEngine.querySymbols(pathQuery)
        
        // 验证路径结果
        assertTrue(pathResults.isNotEmpty())
        assertEquals(classNode.id, pathResults.first().id)
        assertEquals(methodNode.id, pathResults.last().id)
    }
    
    @Test
    fun `test querying class methods`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 查询类符号
        val classQuery = SymbolQuery(name = "TestClass", type = SymbolType.CLASS)
        val classResults = symbolQueryEngine.querySymbols(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classNode = classResults.first()
        
        // 查询类方法
        val methods = symbolQueryEngine.queryClassMethods(classNode)
        
        // 验证方法结果
        assertTrue(methods.isNotEmpty())
        assertTrue(methods.any { it.name == "getName" })
        assertTrue(methods.any { it.name == "getAge" })
        assertTrue(methods.any { it.name == "testMethod" })
    }
    
    @Test
    fun `test querying class fields`() = runBlocking {
        // 构建符号图
        val graph = symbolGraphBuilder.buildGraphFromDirectory(tempDir)
        
        // 索引符号图
        symbolIndexer.indexGraph(graph)
        
        // 查询类符号
        val classQuery = SymbolQuery(name = "TestClass", type = SymbolType.CLASS)
        val classResults = symbolQueryEngine.querySymbols(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classNode = classResults.first()
        
        // 查询类字段
        val fields = symbolQueryEngine.queryClassFields(classNode)
        
        // 验证字段结果
        assertTrue(fields.isNotEmpty())
        assertTrue(fields.any { it.name == "name" })
        assertTrue(fields.any { it.name == "age" })
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
             * 这是一个测试类，用于测试符号索引器和查询引擎。
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
             * 这是一个测试接口，用于测试符号索引器和查询引擎。
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
    }
}
