package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentTransformerTest {

    @Test
    fun `test text cleaning transformer`() {
        val document = Document(
            content = "  This is a test with   extra   spaces.  ",
            metadata = mapOf("source" to "test")
        )

        val options = TextCleaner.CleaningOptions(
            trimWhitespace = true,
            removeExtraSpaces = true
        )

        val transformer = TextCleaningTransformer(options = options)
        val transformed = transformer.transform(document)

        assertEquals("This is a test with extra spaces.", transformed.content)
        assertEquals("test", transformed.metadata["source"])
    }

    @Test
    fun `test html to text transformer`() {
        val document = Document(
            content = "<h1>Test</h1><p>This is a <b>test</b>.</p>",
            metadata = mapOf("source" to "test.html")
        )

        val options = HtmlToTextConverter.ConversionOptions(
            preserveHeaderFormatting = true,
            preserveTextFormatting = true
        )

        val transformer = HtmlToTextTransformer(options = options)
        val transformed = transformer.transform(document)

        assertTrue(transformed.content.contains("# Test"))
        assertTrue(transformed.content.contains("**test**"))
        assertEquals("test.html", transformed.metadata["source"])
    }

    @Test
    fun `test table extraction transformer`() {
        val document = Document(
            content = """
                <table>
                    <tr><th>Header</th></tr>
                    <tr><td>Cell</td></tr>
                </table>
            """.trimIndent(),
            metadata = mapOf("source" to "test.html")
        )

        val transformer = TableExtractionTransformer(
            outputFormat = "markdown",
            extractAsDocuments = true
        )

        val transformed = transformer.transform(document)

        assertTrue(transformed.content.contains("| Header |"))
        assertTrue(transformed.content.contains("| Cell |"))
        assertEquals("html", transformed.metadata["table_source"])
    }

    @Test
    fun `test table extraction transformer with multiple tables`() {
        val document = Document(
            content = """
                <table>
                    <tr><th>Table 1 Header</th></tr>
                    <tr><td>Table 1 Cell</td></tr>
                </table>
                <table>
                    <tr><th>Table 2 Header</th></tr>
                    <tr><td>Table 2 Cell</td></tr>
                </table>
            """.trimIndent(),
            metadata = mapOf("source" to "test.html")
        )

        // 不提取为单独文档，而是合并为一个文档
        val transformer = TableExtractionTransformer(
            outputFormat = "markdown",
            extractAsDocuments = false
        )

        val transformed = transformer.transform(document)

        assertTrue(transformed.content.contains("Table 1"))
        assertTrue(transformed.content.contains("Table 2"))
        assertEquals(2, transformed.metadata["table_count"])
    }

    @Test
    fun `test composite transformer`() {
        val document = Document(
            content = "<h1>Test</h1><p>This is a <b>test</b> with   extra   spaces.</p>",
            metadata = mapOf("source" to "test.html")
        )

        // 创建复合转换器，先将 HTML 转换为文本，然后清理文本
        val htmlToTextTransformer = HtmlToTextTransformer(
            options = HtmlToTextConverter.ConversionOptions(
                preserveHeaderFormatting = true,
                preserveTextFormatting = true
            )
        )

        val textCleaningTransformer = TextCleaningTransformer(
            options = TextCleaner.CleaningOptions(
                removeExtraSpaces = true
            )
        )

        val compositeTransformer = CompositeTransformer(
            htmlToTextTransformer,
            textCleaningTransformer
        )

        val transformed = compositeTransformer.transform(document)

        assertTrue(transformed.content.contains("# Test"))
        assertTrue(transformed.content.contains("**test**"))
        assertFalse(transformed.content.contains("   extra   spaces"))
        assertEquals("test.html", transformed.metadata["source"])
    }

    @Test
    fun `test transform multiple documents`() {
        val documents = listOf(
            Document("Document 1", mapOf("id" to 1)),
            Document("Document 2", mapOf("id" to 2)),
            Document("Document 3", mapOf("id" to 3))
        )

        val transformer = object : DocumentTransformer {
            override fun transform(document: Document): Document {
                return Document(
                    content = "Transformed: ${document.content}",
                    metadata = document.metadata
                )
            }
        }

        val transformed = transformer.transform(documents)

        assertEquals(3, transformed.size)
        assertEquals("Transformed: Document 1", transformed[0].content)
        assertEquals("Transformed: Document 2", transformed[1].content)
        assertEquals("Transformed: Document 3", transformed[2].content)
        assertEquals(1, transformed[0].metadata["id"])
        assertEquals(2, transformed[1].metadata["id"])
        assertEquals(3, transformed[2].metadata["id"])
    }
}
