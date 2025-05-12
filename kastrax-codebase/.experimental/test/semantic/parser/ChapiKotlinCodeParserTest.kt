package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElementType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Paths

class ChapiKotlinCodeParserTest {

    @Test
    fun `should parse Kotlin file correctly`() {
        // 准备
        val parser = ChapiKotlinCodeParser()
        val filePath = Paths.get("TestClass.kt")
        val content = """
            package ai.kastrax.test
            
            import ai.kastrax.core.Agent
            
            /**
             * 测试类
             */
            class TestClass(private val name: String) {
                /**
                 * 测试方法
                 */
                fun testMethod(param: String): String {
                    return "Hello, ${'$'}param!"
                }
                
                /**
                 * 测试扩展函数
                 */
                fun String.testExtension(): String {
                    return "Extension: ${'$'}this"
                }
                
                /**
                 * 测试委托属性
                 */
                val lazyProperty by lazy { "Lazy value" }
                
                /**
                 * 测试挂起函数
                 */
                suspend fun suspendFunction() {
                    // 挂起函数实现
                }
            }
            
            /**
             * 测试顶层函数
             */
            fun topLevelFunction() {
                println("Top level function")
            }
        """.trimIndent()
        
        // 执行
        val result = parser.parseFile(filePath, content)
        
        // 验证
        assertNotNull(result)
        assertEquals(CodeElementType.FILE, result.type)
        assertEquals("TestClass.kt", result.name)
        
        // 验证类
        val classElement = result.children.find { it.type == CodeElementType.CLASS && it.name == "TestClass" }
        assertNotNull(classElement)
        
        // 验证方法
        val methodElement = classElement?.children?.find { it.type == CodeElementType.METHOD && it.name == "testMethod" }
        assertNotNull(methodElement)
        
        // 验证扩展函数
        val extensionMethodElement = classElement?.children?.find { 
            it.type == CodeElementType.METHOD && it.name == "testExtension" 
        }
        assertNotNull(extensionMethodElement)
        assertTrue(extensionMethodElement?.metadata?.get("isExtensionFunction") as? Boolean ?: false)
        
        // 验证委托属性
        val delegatedPropertyElement = classElement?.children?.find { 
            it.type == CodeElementType.PROPERTY && it.name == "lazyProperty" 
        }
        assertNotNull(delegatedPropertyElement)
        assertTrue(delegatedPropertyElement?.metadata?.get("isDelegated") as? Boolean ?: false)
        
        // 验证挂起函数
        val suspendFunctionElement = classElement?.children?.find { 
            it.type == CodeElementType.METHOD && it.name == "suspendFunction" 
        }
        assertNotNull(suspendFunctionElement)
        assertTrue(suspendFunctionElement?.metadata?.get("isSuspendFunction") as? Boolean ?: false)
        
        // 验证顶层函数
        val topLevelFunctionElement = result.children.find { 
            it.type == CodeElementType.METHOD && it.name == "topLevelFunction" 
        }
        assertNotNull(topLevelFunctionElement)
        assertTrue(topLevelFunctionElement?.metadata?.get("isTopLevel") as? Boolean ?: false)
    }
}
