package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Modifier
import ai.kastrax.codebase.semantic.model.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ChapiCodeParserTest {
    
    @Test
    fun `test parse Java class with Chapi parser`() {
        // 创建Java代码解析器
        val parser = ChapiJavaCodeParser()
        
        // 测试Java代码
        val javaCode = """
            package com.example;
            
            import java.util.List;
            import java.util.ArrayList;
            
            /**
             * 测试类
             */
            public class TestClass extends BaseClass implements TestInterface {
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
                 * 测试方法
                 */
                @Override
                public void testMethod(String param1, int param2) {
                    System.out.println("Test method called");
                    otherMethod();
                }
                
                private void otherMethod() {
                    // 私有方法
                }
            }
        """.trimIndent()
        
        // 解析代码
        val filePath = Paths.get("TestClass.java")
        val elements = parser.parseCode(filePath, javaCode)
        
        // 验证文件元素
        val fileElement = elements.first()
        assertEquals(CodeElementType.FILE, fileElement.type)
        assertEquals("TestClass.java", fileElement.name)
        
        // 验证类元素
        val classElements = fileElement.children.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classElements.size)
        
        val classElement = classElements.first()
        assertEquals("TestClass", classElement.name)
        assertEquals("com.example.TestClass", classElement.qualifiedName)
        assertEquals(Visibility.PUBLIC, classElement.visibility)
        
        // 验证继承和实现信息
        assertEquals("BaseClass", classElement.metadata["extends"])
        assertEquals("TestInterface", classElement.metadata["implements"])
        
        // 验证字段
        val fieldElements = classElement.children.filter { it.type == CodeElementType.FIELD }
        assertEquals(2, fieldElements.size)
        
        val nameField = fieldElements.find { it.name == "name" }
        assertTrue(nameField != null)
        assertEquals(Visibility.PRIVATE, nameField?.visibility)
        assertEquals("String", nameField?.metadata?.get("type"))
        
        // 验证方法
        val methodElements = classElement.children.filter { it.type == CodeElementType.METHOD }
        assertTrue(methodElements.size >= 4)
        
        val testMethod = methodElements.find { it.name == "testMethod" }
        assertTrue(testMethod != null)
        assertEquals(Visibility.PUBLIC, testMethod?.visibility)
        
        // 验证方法参数
        val testMethodParams = testMethod?.children?.filter { it.type == CodeElementType.PARAMETER }
        assertEquals(2, testMethodParams?.size)
        
        // 验证方法注解
        val testMethodAnnotations = testMethod?.metadata?.get("annotations")
        assertTrue(testMethodAnnotations?.contains("Override") == true)
        
        // 验证方法调用
        val testMethodCalls = testMethod?.metadata?.get("functionCalls")
        assertTrue(testMethodCalls?.contains("otherMethod") == true)
    }
    
    @Test
    fun `test parse Kotlin class with Chapi parser`() {
        // 创建Kotlin代码解析器
        val parser = ChapiKotlinCodeParser()
        
        // 测试Kotlin代码
        val kotlinCode = """
            package com.example
            
            import kotlin.collections.List
            
            /**
             * 测试类
             */
            class TestClass(
                private val name: String,
                private var age: Int
            ) : BaseClass(), TestInterface {
                
                /**
                 * 测试属性
                 */
                val isActive: Boolean = true
                
                /**
                 * 测试方法
                 */
                override fun testMethod(param1: String, param2: Int) {
                    println("Test method called")
                    otherMethod()
                }
                
                private fun otherMethod() {
                    // 私有方法
                }
                
                /**
                 * 测试扩展函数
                 */
                fun String.toCustomFormat(): String {
                    return this.toUpperCase()
                }
            }
        """.trimIndent()
        
        // 解析代码
        val filePath = Paths.get("TestClass.kt")
        val elements = parser.parseCode(filePath, kotlinCode)
        
        // 验证文件元素
        val fileElement = elements.first()
        assertEquals(CodeElementType.FILE, fileElement.type)
        assertEquals("TestClass.kt", fileElement.name)
        
        // 验证类元素
        val classElements = fileElement.children.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classElements.size)
        
        val classElement = classElements.first()
        assertEquals("TestClass", classElement.name)
        assertEquals("com.example.TestClass", classElement.qualifiedName)
        
        // 验证继承和实现信息
        assertEquals("BaseClass", classElement.metadata["extends"])
        assertEquals("TestInterface", classElement.metadata["implements"])
        
        // 验证属性
        val propertyElements = classElement.children.filter { it.type == CodeElementType.FIELD || it.type == CodeElementType.PROPERTY }
        assertTrue(propertyElements.size >= 3) // name, age, isActive
        
        val isActiveProperty = propertyElements.find { it.name == "isActive" }
        assertTrue(isActiveProperty != null)
        assertEquals("Boolean", isActiveProperty?.metadata?.get("type"))
        
        // 验证方法
        val methodElements = classElement.children.filter { it.type == CodeElementType.METHOD }
        assertTrue(methodElements.size >= 2)
        
        val testMethod = methodElements.find { it.name == "testMethod" }
        assertTrue(testMethod != null)
        
        // 验证方法参数
        val testMethodParams = testMethod?.children?.filter { it.type == CodeElementType.PARAMETER }
        assertEquals(2, testMethodParams?.size)
        
        // 验证方法调用
        val testMethodCalls = testMethod?.metadata?.get("functionCalls")
        assertTrue(testMethodCalls?.contains("otherMethod") == true)
    }
}
