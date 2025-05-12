package ai.kastrax.codebase.semantic

import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeSemanticQueryEngineTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var semanticAnalyzer: CodeSemanticAnalyzer
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    private lateinit var queryEngine: CodeSemanticQueryEngine
    
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
        
        queryEngine = CodeSemanticQueryEngine(
            analyzer = semanticAnalyzer,
            relationAnalyzer = relationAnalyzer
        )
        
        // 创建测试文件
        createTestFiles()
    }
    
    @Test
    fun `test querying elements by name`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询类元素
        val query = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val results = queryEngine.queryElements(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("TestClass", results.first().name)
        assertEquals(CodeElementType.CLASS, results.first().type)
    }
    
    @Test
    fun `test querying elements by qualified name`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询类元素
        val query = ElementQuery(qualifiedName = "ai.kastrax.codebase.test.TestClass")
        val results = queryEngine.queryElements(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("TestClass", results.first().name)
        assertEquals("ai.kastrax.codebase.test.TestClass", results.first().qualifiedName)
    }
    
    @Test
    fun `test querying class methods`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询类元素
        val classQuery = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val classResults = queryEngine.queryElements(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classElement = classResults.first()
        
        // 查询类方法
        val methods = queryEngine.queryClassMethods(classElement)
        
        // 验证方法结果
        assertTrue(methods.isNotEmpty())
        assertTrue(methods.any { it.name == "getName" })
        assertTrue(methods.any { it.name == "getAge" })
        assertTrue(methods.any { it.name == "testMethod" })
    }
    
    @Test
    fun `test querying class fields`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询类元素
        val classQuery = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val classResults = queryEngine.queryElements(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classElement = classResults.first()
        
        // 查询类字段
        val fields = queryEngine.queryClassFields(classElement)
        
        // 验证字段结果
        assertTrue(fields.isNotEmpty())
        assertTrue(fields.any { it.name == "name" })
        assertTrue(fields.any { it.name == "age" })
    }
    
    @Test
    fun `test querying class superclasses and interfaces`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询子类元素
        val subclassQuery = ElementQuery(name = "TestSubclass", type = CodeElementType.CLASS)
        val subclassResults = queryEngine.queryElements(subclassQuery)
        
        // 验证子类结果
        assertTrue(subclassResults.isNotEmpty())
        val subclassElement = subclassResults.first()
        
        // 查询父类
        val superclasses = queryEngine.queryClassSuperclasses(subclassElement)
        
        // 验证父类结果
        assertTrue(superclasses.isNotEmpty())
        assertEquals("TestClass", superclasses.first().name)
        
        // 查询类元素
        val classQuery = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val classResults = queryEngine.queryElements(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classElement = classResults.first()
        
        // 查询接口
        val interfaces = queryEngine.queryClassInterfaces(classElement)
        
        // 验证接口结果
        assertTrue(interfaces.isNotEmpty())
        assertEquals("TestInterface", interfaces.first().name)
    }
    
    @Test
    fun `test querying method overrides`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询子类元素
        val subclassQuery = ElementQuery(name = "TestSubclass", type = CodeElementType.CLASS)
        val subclassResults = queryEngine.queryElements(subclassQuery)
        
        // 验证子类结果
        assertTrue(subclassResults.isNotEmpty())
        val subclassElement = subclassResults.first()
        
        // 查询子类方法
        val subclassMethods = queryEngine.queryClassMethods(subclassElement)
        val testMethod = subclassMethods.find { it.name == "testMethod" }
        
        // 验证方法结果
        assertNotNull(testMethod)
        
        // 查询重写的方法
        val overriddenMethods = queryEngine.queryMethodOverrides(testMethod)
        
        // 验证重写方法结果
        assertTrue(overriddenMethods.isNotEmpty())
        assertEquals("testMethod", overriddenMethods.first().name)
        
        // 查询类元素
        val classQuery = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val classResults = queryEngine.queryElements(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classElement = classResults.first()
        
        // 查询类方法
        val classMethods = queryEngine.queryClassMethods(classElement)
        val classTestMethod = classMethods.find { it.name == "testMethod" }
        
        // 验证方法结果
        assertNotNull(classTestMethod)
        
        // 查询被重写的方法
        val overridingMethods = queryEngine.queryMethodOverriddenBy(classTestMethod)
        
        // 验证被重写方法结果
        assertTrue(overridingMethods.isNotEmpty())
        assertEquals("testMethod", overridingMethods.first().name)
    }
    
    @Test
    fun `test querying element dependencies`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询类元素
        val classQuery = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val classResults = queryEngine.queryElements(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classElement = classResults.first()
        
        // 查询依赖
        val dependencies = queryEngine.queryElementDependencies(classElement)
        
        // 验证依赖结果
        assertTrue(dependencies.isNotEmpty())
    }
    
    @Test
    fun `test querying relation elements`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        relationAnalyzer.analyzeRelations(codebase)
        
        // 查询类元素
        val classQuery = ElementQuery(name = "TestClass", type = CodeElementType.CLASS)
        val classResults = queryEngine.queryElements(classQuery)
        
        // 验证类结果
        assertTrue(classResults.isNotEmpty())
        val classElement = classResults.first()
        
        // 创建关系查询
        val relationQuery = RelationQuery(
            sourceId = classElement.id,
            type = RelationType.IMPLEMENTS,
            direction = RelationDirection.OUTGOING
        )
        
        // 查询关系元素
        val query = ElementQuery(relationQuery = relationQuery)
        val results = queryEngine.queryElements(query)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("TestInterface", results.first().name)
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
             * 这是一个测试类，用于测试代码语义查询引擎。
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
             * 这是一个测试接口，用于测试代码语义查询引擎。
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
             * 这是一个测试子类，用于测试代码语义查询引擎。
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
