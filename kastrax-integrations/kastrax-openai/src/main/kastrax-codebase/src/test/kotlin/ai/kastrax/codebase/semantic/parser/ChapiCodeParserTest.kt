package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElementType
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
class ChapiCodeParserTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeAll
    fun setup() {
        // 注册所有解析器
        ChapiCodeParserRegistrar.registerAllParsers()
    }
    
    @Test
    fun `test Java code parser`() {
        val javaCode = """
            package com.example;
            
            import java.util.List;
            import java.util.ArrayList;
            
            /**
             * 示例类
             */
            public class Example {
                private String name;
                private int age;
                
                /**
                 * 构造函数
                 */
                public Example(String name, int age) {
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
                 * 获取年龄
                 */
                public int getAge() {
                    return age;
                }
                
                /**
                 * 设置年龄
                 */
                public void setAge(int age) {
                    this.age = age;
                }
            }
        """.trimIndent()
        
        val filePath = tempDir.resolve("Example.java")
        Files.writeString(filePath, javaCode)
        
        val parser = CodeParserFactory.getParser(filePath)
        assertNotNull(parser, "应该找到 Java 解析器")
        assertTrue(parser is ChapiJavaCodeParser, "解析器应该是 ChapiJavaCodeParser")
        
        val element = parser.parseFile(filePath, javaCode)
        assertEquals(CodeElementType.FILE, element.type)
        assertEquals("Java", element.language)
        
        // 验证导入
        val imports = element.findChildren { it.type == CodeElementType.IMPORT }
        assertEquals(2, imports.size)
        
        // 验证类
        val classes = element.findChildren { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size)
        
        val exampleClass = classes.first()
        assertEquals("Example", exampleClass.name)
        
        // 验证字段
        val fields = exampleClass.findChildren { it.type == CodeElementType.FIELD }
        assertEquals(2, fields.size)
        
        // 验证方法
        val methods = exampleClass.findChildren { it.type == CodeElementType.METHOD }
        assertEquals(4, methods.size)
        
        // 验证构造函数
        val constructors = exampleClass.findChildren { it.type == CodeElementType.CONSTRUCTOR }
        assertEquals(1, constructors.size)
    }
    
    @Test
    fun `test Kotlin code parser`() {
        val kotlinCode = """
            package com.example
            
            /**
             * 示例数据类
             */
            data class Person(
                val name: String,
                val age: Int
            ) {
                /**
                 * 判断是否成年
                 */
                fun isAdult(): Boolean {
                    return age >= 18
                }
                
                /**
                 * 获取描述
                 */
                fun getDescription(): String {
                    return "$name, $age years old"
                }
            }
        """.trimIndent()
        
        val filePath = tempDir.resolve("Person.kt")
        Files.writeString(filePath, kotlinCode)
        
        val parser = CodeParserFactory.getParser(filePath)
        assertNotNull(parser, "应该找到 Kotlin 解析器")
        assertTrue(parser is ChapiKotlinCodeParser, "解析器应该是 ChapiKotlinCodeParser")
        
        val element = parser.parseFile(filePath, kotlinCode)
        assertEquals(CodeElementType.FILE, element.type)
        assertEquals("Kotlin", element.language)
        
        // 验证类
        val classes = element.findChildren { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size)
        
        val personClass = classes.first()
        assertEquals("Person", personClass.name)
        
        // 验证方法
        val methods = personClass.findChildren { it.type == CodeElementType.METHOD }
        assertEquals(2, methods.size)
    }
    
    @Test
    fun `test Python code parser`() {
        val pythonCode = """
            class Calculator:
                """A simple calculator class"""
                
                def __init__(self):
                    """Initialize the calculator"""
                    self.result = 0
                
                def add(self, a, b):
                    """Add two numbers"""
                    self.result = a + b
                    return self.result
                
                def subtract(self, a, b):
                    """Subtract b from a"""
                    self.result = a - b
                    return self.result
                
                def multiply(self, a, b):
                    """Multiply two numbers"""
                    self.result = a * b
                    return self.result
                
                def divide(self, a, b):
                    """Divide a by b"""
                    if b == 0:
                        raise ValueError("Cannot divide by zero")
                    self.result = a / b
                    return self.result
        """.trimIndent()
        
        val filePath = tempDir.resolve("calculator.py")
        Files.writeString(filePath, pythonCode)
        
        val parser = CodeParserFactory.getParser(filePath)
        assertNotNull(parser, "应该找到 Python 解析器")
        assertTrue(parser is ChapiPythonCodeParser, "解析器应该是 ChapiPythonCodeParser")
        
        val element = parser.parseFile(filePath, pythonCode)
        assertEquals(CodeElementType.FILE, element.type)
        assertEquals("Python", element.language)
        
        // 验证类
        val classes = element.findChildren { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size)
        
        val calculatorClass = classes.first()
        assertEquals("Calculator", calculatorClass.name)
        
        // 验证方法
        val methods = calculatorClass.findChildren { it.type == CodeElementType.METHOD }
        assertTrue(methods.size >= 5, "应该至少有 5 个方法")
    }
    
    @Test
    fun `test TypeScript code parser`() {
        val tsCode = """
            interface User {
                id: number;
                name: string;
                email: string;
            }
            
            class UserService {
                private users: User[] = [];
                
                constructor() {
                    console.log("UserService initialized");
                }
                
                public addUser(user: User): void {
                    this.users.push(user);
                }
                
                public getUserById(id: number): User | undefined {
                    return this.users.find(user => user.id === id);
                }
                
                public getAllUsers(): User[] {
                    return [...this.users];
                }
            }
            
            export { User, UserService };
        """.trimIndent()
        
        val filePath = tempDir.resolve("user-service.ts")
        Files.writeString(filePath, tsCode)
        
        val parser = CodeParserFactory.getParser(filePath)
        assertNotNull(parser, "应该找到 TypeScript 解析器")
        assertTrue(parser is ChapiTypeScriptCodeParser, "解析器应该是 ChapiTypeScriptCodeParser")
        
        val element = parser.parseFile(filePath, tsCode)
        assertEquals(CodeElementType.FILE, element.type)
        assertEquals("TypeScript", element.language)
        
        // 验证接口
        val interfaces = element.findChildren { it.type == CodeElementType.INTERFACE }
        assertEquals(1, interfaces.size)
        
        // 验证类
        val classes = element.findChildren { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size)
        
        val userServiceClass = classes.first()
        assertEquals("UserService", userServiceClass.name)
        
        // 验证方法
        val methods = userServiceClass.findChildren { it.type == CodeElementType.METHOD }
        assertTrue(methods.size >= 3, "应该至少有 3 个方法")
    }
    
    @Test
    fun `test Go code parser`() {
        val goCode = """
            package main
            
            import (
                "fmt"
                "strings"
            )
            
            // Person represents a person with a name and age
            type Person struct {
                Name string
                Age  int
            }
            
            // NewPerson creates a new Person
            func NewPerson(name string, age int) *Person {
                return &Person{
                    Name: name,
                    Age:  age,
                }
            }
            
            // IsAdult checks if the person is an adult
            func (p *Person) IsAdult() bool {
                return p.Age >= 18
            }
            
            // GetDescription returns a description of the person
            func (p *Person) GetDescription() string {
                return fmt.Sprintf("%s, %d years old", p.Name, p.Age)
            }
            
            func main() {
                person := NewPerson("John", 30)
                fmt.Println(person.GetDescription())
                fmt.Println("Is adult:", person.IsAdult())
            }
        """.trimIndent()
        
        val filePath = tempDir.resolve("person.go")
        Files.writeString(filePath, goCode)
        
        val parser = CodeParserFactory.getParser(filePath)
        assertNotNull(parser, "应该找到 Go 解析器")
        assertTrue(parser is ChapiGoCodeParser, "解析器应该是 ChapiGoCodeParser")
        
        val element = parser.parseFile(filePath, goCode)
        assertEquals(CodeElementType.FILE, element.type)
        assertEquals("Go", element.language)
        
        // 验证导入
        val imports = element.findChildren { it.type == CodeElementType.IMPORT }
        assertTrue(imports.isNotEmpty(), "应该有导入语句")
        
        // 验证结构体（类）
        val structs = element.findChildren { it.type == CodeElementType.CLASS }
        assertTrue(structs.isNotEmpty(), "应该有结构体定义")
        
        // 验证函数
        val functions = element.findChildren { it.type == CodeElementType.METHOD }
        assertTrue(functions.isNotEmpty(), "应该有函数定义")
    }
}
