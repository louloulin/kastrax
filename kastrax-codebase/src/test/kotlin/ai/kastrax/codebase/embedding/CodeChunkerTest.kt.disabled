package ai.kastrax.codebase.embedding

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeChunkerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var chunker: CodeChunker
    
    @BeforeEach
    fun setUp() {
        chunker = CodeChunker(
            config = CodeChunkerConfig(
                maxChunkSize = 500,
                minChunkSize = 50,
                overlap = 20,
                preserveSemantics = true
            )
        )
    }
    
    @Test
    fun `test chunking Java code`() {
        // 创建测试文件
        val javaFile = tempDir.resolve("TestClass.java")
        javaFile.writeText("""
            package ai.kastrax.codebase.test;
            
            /**
             * This is a test class.
             */
            public class TestClass {
                private String name;
                private int age;
                
                /**
                 * Constructor.
                 */
                public TestClass(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
                
                /**
                 * Get name.
                 */
                public String getName() {
                    return name;
                }
                
                /**
                 * Set name.
                 */
                public void setName(String name) {
                    this.name = name;
                }
                
                /**
                 * Get age.
                 */
                public int getAge() {
                    return age;
                }
                
                /**
                 * Set age.
                 */
                public void setAge(int age) {
                    this.age = age;
                }
                
                /**
                 * A more complex method.
                 */
                public void complexMethod() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println("Iteration: " + i);
                        if (i % 2 == 0) {
                            System.out.println("Even number");
                        } else {
                            System.out.println("Odd number");
                        }
                    }
                }
            }
        """.trimIndent())
        
        // 分割代码
        val chunks = chunker.chunkFile(javaFile)
        
        // 验证分块结果
        assertTrue(chunks.isNotEmpty())
        
        // 验证每个块的大小在限制范围内
        chunks.forEach { chunk ->
            assertTrue(chunk.content.length <= 500, "块大小应该小于等于最大块大小")
            assertTrue(chunk.content.length >= 50, "块大小应该大于等于最小块大小")
        }
        
        // 验证元数据
        chunks.forEach { chunk ->
            assertEquals(javaFile.toString(), chunk.metadata["path"])
            assertTrue(chunk.metadata.containsKey("chunk_index"))
            assertTrue(chunk.metadata.containsKey("total_chunks"))
            assertEquals("java", chunk.metadata["file_type"])
        }
        
        // 验证方法被正确分块
        val methodChunks = chunks.filter { it.content.contains("public") && it.content.contains("{") }
        assertTrue(methodChunks.isNotEmpty())
        
        // 验证至少有一个块包含 getName 方法
        assertTrue(chunks.any { it.content.contains("getName") })
        
        // 验证至少有一个块包含 complexMethod 方法
        assertTrue(chunks.any { it.content.contains("complexMethod") })
    }
    
    @Test
    fun `test chunking Python code`() {
        // 创建测试文件
        val pythonFile = tempDir.resolve("test_class.py")
        pythonFile.writeText("""
            """
            This is a test module.
            """
            
            class TestClass:
                """
                This is a test class.
                """
                
                def __init__(self, name, age):
                    """
                    Constructor.
                    """
                    self.name = name
                    self.age = age
                
                def get_name(self):
                    """
                    Get name.
                    """
                    return self.name
                
                def set_name(self, name):
                    """
                    Set name.
                    """
                    self.name = name
                
                def get_age(self):
                    """
                    Get age.
                    """
                    return self.age
                
                def set_age(self, age):
                    """
                    Set age.
                    """
                    self.age = age
                
                def complex_method(self):
                    """
                    A more complex method.
                    """
                    for i in range(10):
                        print(f"Iteration: {i}")
                        if i % 2 == 0:
                            print("Even number")
                        else:
                            print("Odd number")
            
            def standalone_function():
                """
                A standalone function.
                """
                print("This is a standalone function")
        """.trimIndent())
        
        // 分割代码
        val chunks = chunker.chunkFile(pythonFile)
        
        // 验证分块结果
        assertTrue(chunks.isNotEmpty())
        
        // 验证每个块的大小在限制范围内
        chunks.forEach { chunk ->
            assertTrue(chunk.content.length <= 500, "块大小应该小于等于最大块大小")
            assertTrue(chunk.content.length >= 50, "块大小应该大于等于最小块大小")
        }
        
        // 验证元数据
        chunks.forEach { chunk ->
            assertEquals(pythonFile.toString(), chunk.metadata["path"])
            assertTrue(chunk.metadata.containsKey("chunk_index"))
            assertTrue(chunk.metadata.containsKey("total_chunks"))
            assertEquals("python", chunk.metadata["file_type"])
        }
        
        // 验证类被正确分块
        val classChunks = chunks.filter { it.content.contains("class TestClass") }
        assertTrue(classChunks.isNotEmpty())
        
        // 验证至少有一个块包含 get_name 方法
        assertTrue(chunks.any { it.content.contains("def get_name") })
        
        // 验证至少有一个块包含 complex_method 方法
        assertTrue(chunks.any { it.content.contains("def complex_method") })
        
        // 验证至少有一个块包含独立函数
        assertTrue(chunks.any { it.content.contains("def standalone_function") })
    }
    
    @Test
    fun `test chunking JavaScript code`() {
        // 创建测试文件
        val jsFile = tempDir.resolve("test_class.js")
        jsFile.writeText("""
            /**
             * This is a test class.
             */
            class TestClass {
                /**
                 * Constructor.
                 */
                constructor(name, age) {
                    this.name = name;
                    this.age = age;
                }
                
                /**
                 * Get name.
                 */
                getName() {
                    return this.name;
                }
                
                /**
                 * Set name.
                 */
                setName(name) {
                    this.name = name;
                }
                
                /**
                 * Get age.
                 */
                getAge() {
                    return this.age;
                }
                
                /**
                 * Set age.
                 */
                setAge(age) {
                    this.age = age;
                }
                
                /**
                 * A more complex method.
                 */
                complexMethod() {
                    for (let i = 0; i < 10; i++) {
                        console.log(`Iteration: ${i}`);
                        if (i % 2 === 0) {
                            console.log("Even number");
                        } else {
                            console.log("Odd number");
                        }
                    }
                }
            }
            
            /**
             * A standalone function.
             */
            function standaloneFunction() {
                console.log("This is a standalone function");
            }
            
            /**
             * An arrow function.
             */
            const arrowFunction = () => {
                console.log("This is an arrow function");
            };
        """.trimIndent())
        
        // 分割代码
        val chunks = chunker.chunkFile(jsFile)
        
        // 验证分块结果
        assertTrue(chunks.isNotEmpty())
        
        // 验证每个块的大小在限制范围内
        chunks.forEach { chunk ->
            assertTrue(chunk.content.length <= 500, "块大小应该小于等于最大块大小")
            assertTrue(chunk.content.length >= 50, "块大小应该大于等于最小块大小")
        }
        
        // 验证元数据
        chunks.forEach { chunk ->
            assertEquals(jsFile.toString(), chunk.metadata["path"])
            assertTrue(chunk.metadata.containsKey("chunk_index"))
            assertTrue(chunk.metadata.containsKey("total_chunks"))
            assertEquals("javascript", chunk.metadata["file_type"])
        }
        
        // 验证类被正确分块
        val classChunks = chunks.filter { it.content.contains("class TestClass") }
        assertTrue(classChunks.isNotEmpty())
        
        // 验证至少有一个块包含 getName 方法
        assertTrue(chunks.any { it.content.contains("getName()") })
        
        // 验证至少有一个块包含 complexMethod 方法
        assertTrue(chunks.any { it.content.contains("complexMethod()") })
        
        // 验证至少有一个块包含独立函数
        assertTrue(chunks.any { it.content.contains("function standaloneFunction") })
        
        // 验证至少有一个块包含箭头函数
        assertTrue(chunks.any { it.content.contains("const arrowFunction") })
    }
    
    @Test
    fun `test simple chunking with overlap`() {
        // 创建一个简单的文本文件
        val textFile = tempDir.resolve("test.txt")
        val text = StringBuilder()
        for (i in 1..20) {
            text.append("Line $i: This is a test line with some content.\n")
        }
        textFile.writeText(text.toString())
        
        // 使用简单分块策略
        val simpleChunker = CodeChunker(
            config = CodeChunkerConfig(
                maxChunkSize = 200,
                minChunkSize = 50,
                overlap = 30,
                preserveSemantics = false // 禁用语义保留
            )
        )
        
        // 分割文件
        val chunks = simpleChunker.chunkFile(textFile)
        
        // 验证分块结果
        assertTrue(chunks.isNotEmpty())
        
        // 验证每个块的大小在限制范围内
        chunks.forEach { chunk ->
            assertTrue(chunk.content.length <= 200, "块大小应该小于等于最大块大小")
            assertTrue(chunk.content.length >= 50, "块大小应该大于等于最小块大小")
        }
        
        // 验证重叠
        for (i in 0 until chunks.size - 1) {
            val currentChunk = chunks[i].content
            val nextChunk = chunks[i + 1].content
            
            // 当前块的末尾应该与下一个块的开头重叠
            val currentEnd = currentChunk.takeLast(30)
            val nextStart = nextChunk.take(30)
            
            assertTrue(currentEnd.isNotEmpty() && nextStart.isNotEmpty())
            assertTrue(currentEnd == nextStart || nextStart.contains(currentEnd) || currentEnd.contains(nextStart),
                "块之间应该有重叠: \n当前块末尾: $currentEnd\n下一块开头: $nextStart")
        }
    }
}
