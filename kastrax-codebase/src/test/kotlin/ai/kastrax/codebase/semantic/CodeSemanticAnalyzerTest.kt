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

class CodeSemanticAnalyzerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var analyzer: CodeSemanticAnalyzer
    
    @BeforeEach
    fun setUp() {
        // 注册解析器
        CodeParserFactory.registerParser(ChapiJavaCodeParser())
        CodeParserFactory.registerParser(ChapiKotlinCodeParser())
        
        // 创建分析器
        analyzer = CodeSemanticAnalyzer(
            config = CodeSemanticAnalyzerConfig(
                excludePatterns = emptySet(),
                excludeDirectories = emptySet()
            )
        )
        
        // 创建测试文件
        createTestFiles()
    }
    
    @Test
    fun `test analyzing Java file`() = runBlocking {
        // 分析 Java 文件
        val javaFile = tempDir.resolve("TestClass.java")
        val fileElement = analyzer.analyzeFile(javaFile)
        
        // 验证文件元素
        assertEquals("TestClass.java", fileElement.name)
        assertEquals(CodeElementType.FILE, fileElement.type)
        
        // 验证包元素
        val packageElements = fileElement.children.filter { it.type == CodeElementType.PACKAGE }
        assertEquals(1, packageElements.size)
        assertEquals("ai.kastrax.codebase.test", packageElements.first().name)
        
        // 验证类元素
        val classElements = fileElement.children.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classElements.size)
        
        val classElement = classElements.first()
        assertEquals("TestClass", classElement.name)
        assertEquals("ai.kastrax.codebase.test.TestClass", classElement.qualifiedName)
        
        // 验证方法元素
        val methodElements = classElement.children.filter { it.type == CodeElementType.METHOD }
        assertTrue(methodElements.isNotEmpty())
        
        // 验证字段元素
        val fieldElements = classElement.children.filter { it.type == CodeElementType.FIELD }
        assertTrue(fieldElements.isNotEmpty())
    }
    
    @Test
    fun `test analyzing Kotlin file`() = runBlocking {
        // 分析 Kotlin 文件
        val kotlinFile = tempDir.resolve("TestClass.kt")
        val fileElement = analyzer.analyzeFile(kotlinFile)
        
        // 验证文件元素
        assertEquals("TestClass.kt", fileElement.name)
        assertEquals(CodeElementType.FILE, fileElement.type)
        
        // 验证包元素
        val packageElements = fileElement.children.filter { it.type == CodeElementType.PACKAGE }
        assertEquals(1, packageElements.size)
        assertEquals("ai.kastrax.codebase.test", packageElements.first().name)
        
        // 验证类元素
        val classElements = fileElement.children.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classElements.size)
        
        val classElement = classElements.first()
        assertEquals("TestClass", classElement.name)
        
        // 验证方法元素
        val methodElements = classElement.children.filter { it.type == CodeElementType.METHOD }
        assertTrue(methodElements.isNotEmpty())
        
        // 验证属性元素
        val propertyElements = classElement.children.filter { it.type == CodeElementType.PROPERTY }
        assertTrue(propertyElements.isNotEmpty())
    }
    
    @Test
    fun `test analyzing codebase`() = runBlocking {
        // 分析整个代码库
        val codebaseElement = analyzer.analyzeCodebase(tempDir)
        
        // 验证代码库元素
        assertEquals(tempDir.fileName.toString(), codebaseElement.name)
        assertEquals(CodeElementType.FILE, codebaseElement.type)
        
        // 验证文件元素
        val fileElements = codebaseElement.children
        assertTrue(fileElements.isNotEmpty())
        
        // 验证是否包含 Java 和 Kotlin 文件
        val javaFiles = fileElements.filter { it.name.endsWith(".java") }
        val kotlinFiles = fileElements.filter { it.name.endsWith(".kt") }
        
        assertTrue(javaFiles.isNotEmpty())
        assertTrue(kotlinFiles.isNotEmpty())
        
        // 验证类元素
        val classElements = fileElements.flatMap { it.children }.filter { 
            it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE 
        }
        assertTrue(classElements.isNotEmpty())
        
        // 验证方法元素
        val methodElements = classElements.flatMap { it.children }.filter { 
            it.type == CodeElementType.METHOD 
        }
        assertTrue(methodElements.isNotEmpty())
    }
    
    @Test
    fun `test finding elements by name`() = runBlocking {
        // 分析整个代码库
        analyzer.analyzeCodebase(tempDir)
        
        // 查找类元素
        val classElements = analyzer.findElementsByName("TestClass", CodeElementType.CLASS)
        assertTrue(classElements.isNotEmpty())
        
        // 查找接口元素
        val interfaceElements = analyzer.findElementsByName("TestInterface", CodeElementType.INTERFACE)
        assertTrue(interfaceElements.isNotEmpty())
        
        // 查找方法元素
        val methodElements = analyzer.findElementsByName("testMethod", CodeElementType.METHOD)
        assertTrue(methodElements.isNotEmpty())
    }
    
    @Test
    fun `test finding elements by qualified name`() = runBlocking {
        // 分析整个代码库
        analyzer.analyzeCodebase(tempDir)
        
        // 查找类元素
        val classElement = analyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestClass", CodeElementType.CLASS)
        assertNotNull(classElement)
        
        // 查找接口元素
        val interfaceElement = analyzer.findElementByQualifiedName("ai.kastrax.codebase.test.TestInterface", CodeElementType.INTERFACE)
        assertNotNull(interfaceElement)
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
             * 这是一个测试类，用于测试代码语义分析器。
             */
            public class TestClass implements TestInterface {
                private String name;
                private int age;
                
                /**
                 * 构造函数
                 */
                public TestClass(String name, int age) {
                    this.name = name;
                    this.age = age;
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
             * 这是一个测试接口，用于测试代码语义分析器。
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
        
        // 创建 Kotlin 文件
        val kotlinFile = tempDir.resolve("TestClass.kt")
        kotlinFile.writeText("""
            package ai.kastrax.codebase.test
            
            import java.util.List
            import java.util.ArrayList
            
            /**
             * 这是一个测试类，用于测试代码语义分析器。
             */
            class TestClass(
                private var name: String,
                private var age: Int
            ) : TestInterface {
                
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
        
        // 创建 Kotlin 接口文件
        val kotlinInterfaceFile = tempDir.resolve("TestInterface.kt")
        kotlinInterfaceFile.writeText("""
            package ai.kastrax.codebase.test
            
            /**
             * 这是一个测试接口，用于测试代码语义分析器。
             */
            interface TestInterface {
                /**
                 * 获取名称
                 */
                fun getName(): String
                
                /**
                 * 获取年龄
                 */
                fun getAge(): Int
                
                /**
                 * 测试方法
                 */
                fun testMethod()
            }
        """.trimIndent())
    }
}
