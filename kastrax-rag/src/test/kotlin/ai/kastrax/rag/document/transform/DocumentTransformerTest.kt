package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocumentTransformerTest {

    @Test
    fun `test TextReplaceTransformer`() {
        val transformer = TextReplaceTransformer("test", "example")

        val document = Document(
            content = "This is a test document for testing.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is a example document for exampleing.", transformedDocument.content)
        assertEquals("test", transformedDocument.metadata["source"])
    }

    @Test
    fun `test TextReplaceTransformer with case insensitive`() {
        val transformer = TextReplaceTransformer("TEST", "example", caseSensitive = false)

        val document = Document(
            content = "This is a test document for testing.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is a example document for exampleing.", transformedDocument.content)
    }

    @Test
    fun `test TextReplaceTransformer with replace first only`() {
        val transformer = TextReplaceTransformer("test", "example", replaceAll = false)

        val document = Document(
            content = "This is a test document for test purposes.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is a example document for test purposes.", transformedDocument.content)
    }

    @Test
    fun `test TextNormalizeTransformer`() {
        val transformer = TextNormalizeTransformer(
            normalizeWhitespace = true,
            normalizePunctuation = true,
            normalizeCase = true,
            removeEmptyLines = true,
            trimLines = true
        )

        val document = Document(
            content = "This is a \"test\" document with   extra   spaces.\n\nAnd UPPERCASE text.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("this is a \"test\" document with extra spaces. and uppercase text.", transformedDocument.content)
        assertEquals("test", transformedDocument.metadata["source"])
    }

    @Test
    fun `test TextTruncateTransformer with END truncation`() {
        val transformer = TextTruncateTransformer(
            maxLength = 20,
            truncateFrom = TextTruncateTransformer.TruncateFrom.END,
            addEllipsis = true
        )

        val document = Document(
            content = "This is a test document that is longer than the maximum length.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is a test d ...", transformedDocument.content)
        assertEquals("test", transformedDocument.metadata["source"])
    }

    @Test
    fun `test TextTruncateTransformer with START truncation`() {
        val transformer = TextTruncateTransformer(
            maxLength = 20,
            truncateFrom = TextTruncateTransformer.TruncateFrom.START,
            addEllipsis = true
        )

        val document = Document(
            content = "This is a test document that is longer than the maximum length.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("...  maximum length.", transformedDocument.content)
    }

    @Test
    fun `test TextTruncateTransformer with MIDDLE truncation`() {
        val transformer = TextTruncateTransformer(
            maxLength = 20,
            truncateFrom = TextTruncateTransformer.TruncateFrom.MIDDLE,
            addEllipsis = true
        )

        val document = Document(
            content = "This is a test document that is longer than the maximum length.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is  ... length.", transformedDocument.content)
    }

    @Test
    fun `test MetadataTransformer`() {
        val transformer = MetadataTransformer(
            addMetadata = mapOf("processed" to true, "timestamp" to 123456789L),
            removeKeys = listOf("temporary"),
            renameKeys = mapOf("source" to "origin")
        )

        val document = Document(
            content = "This is a test document.",
            metadata = mapOf("source" to "test", "temporary" to "value")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is a test document.", transformedDocument.content)
        assertEquals("test", transformedDocument.metadata["origin"])
        assertEquals(true, transformedDocument.metadata["processed"])
        assertEquals(123456789L, transformedDocument.metadata["timestamp"])
        assertFalse(transformedDocument.metadata.containsKey("temporary"))
        assertFalse(transformedDocument.metadata.containsKey("source"))
    }

    @Test
    fun `test MetadataTransformer with key transformation`() {
        val transformer = MetadataTransformer(
            transformKeys = true,
            keysToLowercase = true,
            replaceSpacesInKeys = true
        )

        val document = Document(
            content = "This is a test document.",
            metadata = mapOf("Source Type" to "test", "CATEGORY" to "example")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is a test document.", transformedDocument.content)
        assertEquals("test", transformedDocument.metadata["source_type"])
        assertEquals("example", transformedDocument.metadata["category"])
    }

    @Test
    fun `test MetadataExtractorTransformer`() {
        val transformer = MetadataExtractorTransformer.fromRegexPatterns(
            patterns = mapOf(
                "title" to "Title:\\s*(.+)".toRegex(),
                "author" to "Author:\\s*(.+)".toRegex(),
                "date" to "Date:\\s*(.+)".toRegex()
            ),
            removeExtracted = true
        )

        val document = Document(
            content = """
                Title: Test Document
                Author: John Doe
                Date: 2023-01-01

                This is the content of the document.
            """.trimIndent(),
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = transformer.transform(document)

        assertEquals("This is the content of the document.", transformedDocument.content.trim())
        assertEquals("test", transformedDocument.metadata["source"])
        assertEquals("Test Document", transformedDocument.metadata["title"])
        assertEquals("John Doe", transformedDocument.metadata["author"])
        assertEquals("2023-01-01", transformedDocument.metadata["date"])
    }

    @Test
    fun `test MetadataFilterTransformer with INCLUDE mode`() {
        val transformer = MetadataFilterTransformer(
            filter = mapOf("category" to "test"),
            mode = MetadataFilterTransformer.FilterMode.INCLUDE
        )

        val document1 = Document(
            content = "Document 1",
            metadata = mapOf("category" to "test")
        )

        val document2 = Document(
            content = "Document 2",
            metadata = mapOf("category" to "other")
        )

        val transformedDocument1 = transformer.transform(document1)
        val transformedDocument2 = transformer.transform(document2)

        assertEquals("Document 1", transformedDocument1.content)
        assertEquals("", transformedDocument2.content)
    }

    @Test
    fun `test MetadataFilterTransformer with EXCLUDE mode`() {
        val transformer = MetadataFilterTransformer(
            filter = mapOf("category" to "test"),
            mode = MetadataFilterTransformer.FilterMode.EXCLUDE
        )

        val document1 = Document(
            content = "Document 1",
            metadata = mapOf("category" to "test")
        )

        val document2 = Document(
            content = "Document 2",
            metadata = mapOf("category" to "other")
        )

        val transformedDocument1 = transformer.transform(document1)
        val transformedDocument2 = transformer.transform(document2)

        assertEquals("", transformedDocument1.content)
        assertEquals("Document 2", transformedDocument2.content)
    }

    @Test
    fun `test CompositeDocumentTransformer`() {
        val replaceTransformer = TextReplaceTransformer("test", "sample")
        val normalizeTransformer = TextNormalizeTransformer(normalizeCase = true)
        val metadataTransformer = MetadataTransformer(
            addMetadata = mapOf("processed" to true),
            removeKeys = listOf()
        )

        val compositeTransformer = CompositeDocumentTransformer(
            replaceTransformer,
            normalizeTransformer,
            metadataTransformer
        )

        val document = Document(
            content = "This is a test document with UPPERCASE text.",
            metadata = mapOf("source" to "test")
        )

        val transformedDocument = compositeTransformer.transform(document)

        assertEquals("this is a sample document with uppercase text.", transformedDocument.content)
        assertEquals("test", transformedDocument.metadata["source"])
        assertEquals(true, transformedDocument.metadata["processed"])
    }

    @Test
    fun `test ConditionalDocumentTransformer`() {
        val condition: (Document) -> Boolean = { doc -> doc.metadata["category"] == "test" }
        val transformer = TextReplaceTransformer("test", "sample")
        val elseTransformer = TextReplaceTransformer("test", "example")

        val conditionalTransformer = ConditionalDocumentTransformer(
            condition = condition,
            transformer = transformer,
            elseTransformer = elseTransformer
        )

        val document1 = Document(
            content = "This is a test document.",
            metadata = mapOf("category" to "test")
        )

        val document2 = Document(
            content = "This is a test document.",
            metadata = mapOf("category" to "other")
        )

        val transformedDocument1 = conditionalTransformer.transform(document1)
        val transformedDocument2 = conditionalTransformer.transform(document2)

        assertEquals("This is a sample document.", transformedDocument1.content)
        assertEquals("This is a example document.", transformedDocument2.content)
    }
}
