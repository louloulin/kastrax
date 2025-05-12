package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.parser.ChapiCodeParserRegistrar
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChapiCodeFlowAnalyzerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var flowAnalyzer: ChapiCodeFlowAnalyzer
    
    @BeforeAll
    fun setup() {
        // 注册所有解析器
        ChapiCodeParserRegistrar.registerAllParsers()
        
        // 创建流分析器
        flowAnalyzer = ChapiCodeFlowAnalyzer()
    }
    
    @Test
    fun `test method flow analysis`() = runBlocking {
        val javaCode = """
            package com.example;
            
            public class Calculator {
                private int result;
                
                public Calculator() {
                    this.result = 0;
                }
                
                public int add(int a, int b) {
                    this.result = a + b;
                    return this.result;
                }
                
                public int subtract(int a, int b) {
                    this.result = a - b;
                    return this.result;
                }
                
                public int getResult() {
                    return this.result;
                }
            }
        """.trimIndent()
        
        val filePath = tempDir.resolve("Calculator.java")
        Files.writeString(filePath, javaCode)
        
        // 解析代码
        val fileElement = CodeParserFactory.parseFile(filePath, javaCode)
        assertNotNull(fileElement, "应该成功解析文件")
        
        // 获取类元素
        val classElement = fileElement.findChild { it.name == "Calculator" }
        assertNotNull(classElement, "应该找到 Calculator 类")
        
        // 获取方法元素
        val addMethod = classElement.findChild { it.name == "add" }
        assertNotNull(addMethod, "应该找到 add 方法")
        
        // 分析方法流
        val flowGraph = flowAnalyzer.analyzeFlow(addMethod)
        
        // 验证流图
        assertNotNull(flowGraph, "应该生成流图")
        assertTrue(flowGraph.nodes.isNotEmpty(), "流图应该包含节点")
        assertTrue(flowGraph.edges.isNotEmpty(), "流图应该包含边")
        
        // 验证入口和出口节点
        val entryNode = flowGraph.nodes.find { it.type == FlowNodeType.ENTRY }
        assertNotNull(entryNode, "流图应该包含入口节点")
        
        val exitNode = flowGraph.nodes.find { it.type == FlowNodeType.EXIT }
        assertNotNull(exitNode, "流图应该包含出口节点")
        
        // 验证参数节点
        val paramNodes = flowGraph.nodes.filter { it.type == FlowNodeType.DECLARATION }
        assertTrue(paramNodes.isNotEmpty(), "流图应该包含参数节点")
    }
    
    @Test
    fun `test class flow analysis`() = runBlocking {
        val javaCode = """
            package com.example;
            
            public class Person {
                private String name;
                private int age;
                
                public Person(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
                
                public String getName() {
                    return name;
                }
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public int getAge() {
                    return age;
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
                
                public boolean isAdult() {
                    return age >= 18;
                }
                
                public String getDescription() {
                    return name + ", " + age + " years old";
                }
            }
        """.trimIndent()
        
        val filePath = tempDir.resolve("Person.java")
        Files.writeString(filePath, javaCode)
        
        // 解析代码
        val fileElement = CodeParserFactory.parseFile(filePath, javaCode)
        assertNotNull(fileElement, "应该成功解析文件")
        
        // 获取类元素
        val classElement = fileElement.findChild { it.name == "Person" }
        assertNotNull(classElement, "应该找到 Person 类")
        
        // 分析类流
        val flowGraph = flowAnalyzer.analyzeFlow(classElement)
        
        // 验证流图
        assertNotNull(flowGraph, "应该生成流图")
        assertTrue(flowGraph.nodes.isNotEmpty(), "流图应该包含节点")
        assertTrue(flowGraph.edges.isNotEmpty(), "流图应该包含边")
        
        // 验证类声明节点
        val classNode = flowGraph.nodes.find { it.type == FlowNodeType.DECLARATION && it.element?.id == classElement.id }
        assertNotNull(classNode, "流图应该包含类声明节点")
        
        // 验证字段节点
        val fieldNodes = flowGraph.nodes.filter { 
            it.type == FlowNodeType.DECLARATION && 
            it.element?.type?.name == "FIELD" 
        }
        assertEquals(2, fieldNodes.size, "流图应该包含 2 个字段节点")
        
        // 验证方法节点
        val methodNodes = flowGraph.nodes.filter { 
            it.type == FlowNodeType.DECLARATION && 
            (it.element?.type?.name == "METHOD" || it.element?.type?.name == "CONSTRUCTOR")
        }
        assertTrue(methodNodes.size >= 7, "流图应该包含至少 7 个方法节点")
    }
    
    @Test
    fun `test file flow analysis`() = runBlocking {
        val javaCode = """
            package com.example;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class Student {
                private String name;
                private int age;
                private List<Course> courses;
                
                public Student(String name, int age) {
                    this.name = name;
                    this.age = age;
                    this.courses = new ArrayList<>();
                }
                
                public void addCourse(Course course) {
                    courses.add(course);
                }
                
                public List<Course> getCourses() {
                    return courses;
                }
            }
            
            class Course {
                private String name;
                private int credits;
                
                public Course(String name, int credits) {
                    this.name = name;
                    this.credits = credits;
                }
                
                public String getName() {
                    return name;
                }
                
                public int getCredits() {
                    return credits;
                }
            }
        """.trimIndent()
        
        val filePath = tempDir.resolve("Student.java")
        Files.writeString(filePath, javaCode)
        
        // 解析代码
        val fileElement = CodeParserFactory.parseFile(filePath, javaCode)
        assertNotNull(fileElement, "应该成功解析文件")
        
        // 分析文件流
        val flowGraph = flowAnalyzer.analyzeFlow(fileElement)
        
        // 验证流图
        assertNotNull(flowGraph, "应该生成流图")
        assertTrue(flowGraph.nodes.isNotEmpty(), "流图应该包含节点")
        assertTrue(flowGraph.edges.isNotEmpty(), "流图应该包含边")
        
        // 验证文件入口节点
        val fileNode = flowGraph.nodes.find { it.type == FlowNodeType.ENTRY && it.element?.id == fileElement.id }
        assertNotNull(fileNode, "流图应该包含文件入口节点")
        
        // 验证导入节点
        val importNodes = flowGraph.nodes.filter { 
            it.type == FlowNodeType.DECLARATION && 
            it.element?.type?.name == "IMPORT" 
        }
        assertEquals(2, importNodes.size, "流图应该包含 2 个导入节点")
        
        // 验证类型节点
        val typeNodes = flowGraph.nodes.filter { 
            it.type == FlowNodeType.DECLARATION && 
            (it.element?.type?.name == "CLASS" || 
             it.element?.type?.name == "INTERFACE" || 
             it.element?.type?.name == "ENUM")
        }
        assertEquals(2, typeNodes.size, "流图应该包含 2 个类型节点")
    }
}
