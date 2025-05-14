package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SearchResultHighlighterTest {

    private lateinit var highlighter: SearchResultHighlighter
    private lateinit var testElement: CodeElement
    private lateinit var testResult: RetrievalResult
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // 创建测试文件
        val testFile = tempDir.resolve("test.kt")
        Files.writeString(testFile, """
            package com.example
            
            /**
             * This is a test class
             */
            class TestClass {
                /**
                 * This is a test method
                 */
                fun testMethod() {
                    // This is a test comment
                    val testVariable = "test value"
                    println(testVariable)
                }
            }
        """.trimIndent())
        
        // 创建测试元素
        testElement = CodeElement(
            id = "1",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = CodeElementType.CLASS,
            visibility = Visibility.PUBLIC,
            location = Location(
                filePath = testFile.toString(),
                startLine = 5,
                endLine = 14,
                startColumn = 0,
                endColumn = 0
            ),
            content = "class TestClass { ... }",
            documentation = "This is a test class",
            metadata = mutableMapOf()
        )
        
        // 创建测试结果
        testResult = RetrievalResult(
            element = testElement,
            score = 0.8,
            explanation = "Test result"
        )
        
        // 创建高亮器
        highlighter = SearchResultHighlighter()
    }
    
    @Test
    fun testHighlight() {
        // 执行高亮
        val results = highlighter.highlight(listOf(testResult), "test")
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals(testElement, results.first().element)
        assertTrue(results.first().highlights.isNotEmpty())
        
        // 验证高亮信息
        val highlights = results.first().highlights
        assertTrue(highlights.any { it.lineContent.contains("test") })
    }
    
    @Test
    fun testGenerateHighlightedText() {
        // 执行高亮
        val results = highlighter.highlight(listOf(testResult), "test")
        
        // 生成高亮文本
        val highlightedText = highlighter.generateHighlightedText(results.first())
        
        // 验证结果
        assertTrue(highlightedText.contains("**test**"))
        assertTrue(highlightedText.contains("TestClass"))
    }
    
    @Test
    fun testGenerateHighlightedHtml() {
        // 执行高亮
        val results = highlighter.highlight(listOf(testResult), "test")
        
        // 生成高亮HTML
        val highlightedHtml = highlighter.generateHighlightedHtml(results.first())
        
        // 验证结果
        assertTrue(highlightedHtml.contains("<span class=\"highlight-match\">"))
        assertTrue(highlightedHtml.contains("TestClass"))
    }
}
