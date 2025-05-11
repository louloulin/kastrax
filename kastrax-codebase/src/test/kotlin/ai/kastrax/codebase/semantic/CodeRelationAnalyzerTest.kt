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

class CodeRelationAnalyzerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var semanticAnalyzer: CodeSemanticAnalyzer
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    
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
        
        // 创建测试文件
        createTestFiles()
    }
    
    @Test
    fun `test analyzing inheritance relations`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        val relations = relationAnalyzer.analyzeRelations(codebase)
        
        // 验证关系数量
        assertTrue(relations.isNotEmpty())
        
        // 查找类和接口
        val javaClass = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestClass", CodeElementType.CLASS)
        val javaInterface = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestInterface", CodeElementType.INTERFACE)
        
        assertNotNull(javaClass)
        assertNotNull(javaInterface)
        
        // 验证实现关系
        val implementsRelations = relationAnalyzer.getOutgoingRelations(javaClass.id, RelationType.IMPLEMENTS)
        assertEquals(1, implementsRelations.size)
        assertEquals(javaInterface.id, implementsRelations.first().targetId)
        
        // 验证被实现关系
        val implementedByRelations = relationAnalyzer.getIncomingRelations(javaInterface.id, RelationType.IMPLEMENTS)
        assertTrue(implementedByRelations.isNotEmpty())
        assertTrue(implementedByRelations.any { it.sourceId == javaClass.id })
    }
    
    @Test
    fun `test analyzing override relations`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        val relations = relationAnalyzer.analyzeRelations(codebase)
        
        // 验证关系数量
        assertTrue(relations.isNotEmpty())
        
        // 查找类和接口
        val javaClass = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestClass", CodeElementType.CLASS)
        val javaInterface = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestInterface", CodeElementType.INTERFACE)
        
        assertNotNull(javaClass)
        assertNotNull(javaInterface)
        
        // 查找方法
        val classMethods = javaClass.children.filter { it.type == CodeElementType.METHOD }
        val interfaceMethods = javaInterface.children.filter { it.type == CodeElementType.METHOD }
        
        // 验证重写关系
        for (classMethod in classMethods) {
            if (classMethod.name == "getName" || classMethod.name == "getAge" || classMethod.name == "testMethod") {
                val overrideRelations = relationAnalyzer.getOutgoingRelations(classMethod.id, RelationType.OVERRIDES)
                assertTrue(overrideRelations.isNotEmpty(), "方法 ${classMethod.name} 应该有重写关系")
                
                val targetMethod = interfaceMethods.find { it.name == classMethod.name }
                assertNotNull(targetMethod, "接口中应该有同名方法 ${classMethod.name}")
                
                assertTrue(overrideRelations.any { it.targetId == targetMethod.id }, 
                    "方法 ${classMethod.name} 应该重写接口方法")
            }
        }
    }
    
    @Test
    fun `test analyzing dependency relations`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        val relations = relationAnalyzer.analyzeRelations(codebase)
        
        // 验证关系数量
        assertTrue(relations.isNotEmpty())
        
        // 查找类
        val javaClass = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestClass", CodeElementType.CLASS)
        assertNotNull(javaClass)
        
        // 验证导入关系
        val importRelations = relationAnalyzer.getOutgoingRelations(javaClass.id, RelationType.IMPORTS)
        assertTrue(importRelations.isNotEmpty())
        
        // 验证依赖关系
        val dependencyRelations = relationAnalyzer.getOutgoingRelations(javaClass.id, RelationType.DEPENDS_ON)
        assertTrue(dependencyRelations.isNotEmpty())
    }
    
    @Test
    fun `test getting incoming and outgoing relations`() = runBlocking {
        // 分析代码库
        val codebase = semanticAnalyzer.analyzeCodebase(tempDir)
        
        // 分析关系
        val relations = relationAnalyzer.analyzeRelations(codebase)
        
        // 验证关系数量
        assertTrue(relations.isNotEmpty())
        
        // 查找类和接口
        val javaClass = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestClass", CodeElementType.CLASS)
        val javaInterface = semanticAnalyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestInterface", CodeElementType.INTERFACE)
        
        assertNotNull(javaClass)
        assertNotNull(javaInterface)
        
        // 获取类的出关系
        val classOutgoingRelations = relationAnalyzer.getOutgoingRelations(javaClass.id)
        assertTrue(classOutgoingRelations.isNotEmpty())
        
        // 获取接口的入关系
        val interfaceIncomingRelations = relationAnalyzer.getIncomingRelations(javaInterface.id)
        assertTrue(interfaceIncomingRelations.isNotEmpty())
        
        // 验证关系的对应关系
        for (relation in classOutgoingRelations) {
            if (relation.targetId == javaInterface.id) {
                val correspondingRelation = interfaceIncomingRelations.find { it.sourceId == javaClass.id }
                assertNotNull(correspondingRelation, "应该存在对应的入关系")
                assertEquals(relation.type, correspondingRelation.type, "关系类型应该相同")
            }
        }
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
             * 这是一个测试类，用于测试代码关系分析器。
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
             * 这是一个测试接口，用于测试代码关系分析器。
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
             * 这是一个测试子类，用于测试代码关系分析器。
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
        val kotlinFile = tempDir.resolve("KotlinTestClass.kt")
        kotlinFile.writeText("""
            package ai.kastrax.codebase.test
            
            import java.util.List
            import java.util.ArrayList
            
            /**
             * 这是一个 Kotlin 测试类，用于测试代码关系分析器。
             */
            class KotlinTestClass(
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
                    
                    fun create(name: String, age: Int): KotlinTestClass {
                        return KotlinTestClass(name, age)
                    }
                }
            }
        """.trimIndent())
    }
}
