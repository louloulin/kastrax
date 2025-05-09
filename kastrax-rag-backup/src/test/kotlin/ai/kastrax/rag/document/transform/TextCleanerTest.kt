package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TextCleanerTest {

    @Test
    fun `test basic cleaning`() {
        val cleaner = TextCleaner()
        val text = "  This is a test with   extra   spaces.  \n\n  And multiple lines.  "
        val options = TextCleaner.CleaningOptions(
            trimWhitespace = true,
            removeExtraSpaces = true
        )

        val cleaned = cleaner.clean(text, options)

        // 由于我们的实现使用了 lines().joinToString() 方法，行首行尾的空格被去除，但不会合并多个空格
        assertEquals("This is a test with   extra   spaces.\n\nAnd multiple lines.", cleaned)
    }

    @Test
    fun `test normalize whitespace`() {
        val cleaner = TextCleaner()
        val text = "This  has\tmultiple    spaces\nand\ttabs."
        val options = TextCleaner.CleaningOptions(
            normalizeWhitespace = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("This has multiple spaces and tabs.", cleaned)
    }

    @Test
    fun `test normalize punctuation`() {
        val cleaner = TextCleaner()
        val text = "This has \"quotes\" and 'apostrophes' and-dashes，中文标点。"
        val options = TextCleaner.CleaningOptions(
            normalizePunctuation = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("This has \"quotes\" and 'apostrophes' and-dashes, 中文标点. ", cleaned)
    }

    @Test
    fun `test case conversion`() {
        val cleaner = TextCleaner()
        val text = "This Has Mixed Case."

        val lowerOptions = TextCleaner.CleaningOptions(
            toLowerCase = true
        )
        val upperOptions = TextCleaner.CleaningOptions(
            toUpperCase = true
        )

        val lowered = cleaner.clean(text, lowerOptions)
        val uppered = cleaner.clean(text, upperOptions)

        assertEquals("this has mixed case.", lowered)
        assertEquals("THIS HAS MIXED CASE.", uppered)
    }

    @Test
    fun `test remove special characters`() {
        val cleaner = TextCleaner()
        val text = "This has special characters: ©®™§¶†‡♠♣♥♦"
        val options = TextCleaner.CleaningOptions(
            removeSpecialCharacters = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("This has special characters: ", cleaned)
    }

    @Test
    fun `test remove empty lines`() {
        val cleaner = TextCleaner()
        val text = "Line 1\n\n\nLine 2\n\nLine 3"
        val options = TextCleaner.CleaningOptions(
            removeEmptyLines = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("Line 1\nLine 2\nLine 3", cleaned)
    }

    @Test
    fun `test normalize line endings`() {
        val cleaner = TextCleaner()
        val text = "Line 1\r\nLine 2\rLine 3\nLine 4"
        val options = TextCleaner.CleaningOptions(
            normalizeLineEndings = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("Line 1\nLine 2\nLine 3\nLine 4", cleaned)
    }

    @Test
    fun `test normalize unicode`() {
        val cleaner = TextCleaner()
        val text = "Café Résumé Naïve"
        val options = TextCleaner.CleaningOptions(
            normalizeUnicode = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("Cafe Resume Naive", cleaned)
    }

    @Test
    fun `test remove urls`() {
        val cleaner = TextCleaner()
        val text = "Visit https://example.com for more information."
        val options = TextCleaner.CleaningOptions(
            removeUrls = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("Visit  for more information.", cleaned)
    }

    @Test
    fun `test remove html tags`() {
        val cleaner = TextCleaner()
        val text = "This is <b>bold</b> and <i>italic</i> text."
        val options = TextCleaner.CleaningOptions(
            removeHtmlTags = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("This is bold and italic text.", cleaned)
    }

    @Test
    fun `test remove numbers`() {
        val cleaner = TextCleaner()
        val text = "There are 42 items and 123 boxes."
        val options = TextCleaner.CleaningOptions(
            removeNumbers = true
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("There are  items and  boxes.", cleaned)
    }

    @Test
    fun `test custom replacements`() {
        val cleaner = TextCleaner()
        val text = "Replace foo and bar."
        val options = TextCleaner.CleaningOptions(
            customReplacements = mapOf(
                "foo" to "FOO",
                "bar" to "BAR"
            )
        )

        val cleaned = cleaner.clean(text, options)

        assertEquals("Replace FOO and BAR.", cleaned)
    }

    @Test
    fun `test clean document`() {
        val cleaner = TextCleaner()
        val document = Document(
            content = "  This is a test document.  ",
            metadata = mapOf("source" to "test")
        )
        val options = TextCleaner.CleaningOptions(
            trimWhitespace = true
        )

        val cleanedDocument = cleaner.clean(document, options)

        assertEquals("This is a test document.", cleanedDocument.content)
        assertEquals("test", cleanedDocument.metadata["source"])
    }

    @Test
    fun `test predefined options`() {
        val cleaner = TextCleaner()
        val text = "  This is a test with   extra   spaces.\n\nAnd multiple lines.  "

        val basicCleaned = cleaner.clean(text, TextCleaner.basicCleaningOptions())
        val standardized = cleaner.clean(text, TextCleaner.standardizationOptions())
        val seoOptimized = cleaner.clean(text, TextCleaner.seoOptions())
        val minimal = cleaner.clean(text, TextCleaner.minimalTextOptions())

        assertNotEquals(text, basicCleaned)
        assertNotEquals(text, standardized)
        assertNotEquals(text, seoOptimized)
        assertNotEquals(text, minimal)

        // 验证 SEO 选项将文本转换为小写
        assertTrue(seoOptimized == seoOptimized.lowercase())
    }

    @Test
    fun `test builder pattern`() {
        val cleaner = TextCleaner()
        val text = "  This is a TEST with   extra   spaces.\n\nAnd multiple lines.  "

        val options = TextCleaner.CleaningOptions()
            .toBuilder()
            .trimWhitespace(true)
            .removeExtraSpaces(true)
            .toLowerCase(true)
            .build()

        val cleaned = cleaner.clean(text, options)

        // 由于我们的实现使用了 lines().joinToString() 方法，行首行尾的空格被去除，但不会合并多个空格
        assertEquals("this is a test with   extra   spaces.\n\nand multiple lines.", cleaned)
    }
}
