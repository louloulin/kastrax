package ai.kastrax.rag.document.transform

import ai.kastrax.store.document.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocumentCleanerTest {

    @Test
    fun `test remove HTML tags`() {
        // Create document with HTML content
        val document = Document(
            id = "test-id",
            content = "<p>This is a <b>test</b> document with <a href='https://example.com'>HTML</a> tags.</p>",
            metadata = mapOf("source" to "test")
        )
        
        // Create cleaner
        val cleaner = DocumentCleaner(
            removeExtraWhitespace = true,
            removeUrls = false,
            removeEmails = false,
            removeHtmlTags = true,
            removeSpecialChars = false,
            toLowerCase = false
        )
        
        // Transform document
        val transformed = cleaner.transform(document)
        
        // Verify results
        assertEquals("This is a test document with HTML tags.", transformed.content)
        assertEquals("test-id", transformed.id)
        assertTrue(transformed.metadata.containsKey("cleaned"))
        assertEquals(true, transformed.metadata["cleaned"])
        assertEquals("test", transformed.metadata["source"])
    }
    
    @Test
    fun `test remove URLs`() {
        // Create document with URLs
        val document = Document(
            id = "test-id",
            content = "This is a document with https://example.com and www.example.org URLs.",
            metadata = mapOf("source" to "test")
        )
        
        // Create cleaner
        val cleaner = DocumentCleaner(
            removeExtraWhitespace = true,
            removeUrls = true,
            removeEmails = false,
            removeHtmlTags = false,
            removeSpecialChars = false,
            toLowerCase = false
        )
        
        // Transform document
        val transformed = cleaner.transform(document)
        
        // Verify results
        assertEquals("This is a document with and URLs.", transformed.content)
    }
    
    @Test
    fun `test remove emails`() {
        // Create document with emails
        val document = Document(
            id = "test-id",
            content = "Contact us at test@example.com or support@example.org for help.",
            metadata = mapOf("source" to "test")
        )
        
        // Create cleaner
        val cleaner = DocumentCleaner(
            removeExtraWhitespace = true,
            removeUrls = false,
            removeEmails = true,
            removeHtmlTags = false,
            removeSpecialChars = false,
            toLowerCase = false
        )
        
        // Transform document
        val transformed = cleaner.transform(document)
        
        // Verify results
        assertEquals("Contact us at or for help.", transformed.content)
    }
    
    @Test
    fun `test remove special characters`() {
        // Create document with special characters
        val document = Document(
            id = "test-id",
            content = "This is a document with special characters: !@#$%^&*()_+",
            metadata = mapOf("source" to "test")
        )
        
        // Create cleaner
        val cleaner = DocumentCleaner(
            removeExtraWhitespace = true,
            removeUrls = false,
            removeEmails = false,
            removeHtmlTags = false,
            removeSpecialChars = true,
            toLowerCase = false
        )
        
        // Transform document
        val transformed = cleaner.transform(document)
        
        // Verify results
        assertEquals("This is a document with special characters", transformed.content)
    }
    
    @Test
    fun `test convert to lowercase`() {
        // Create document with mixed case
        val document = Document(
            id = "test-id",
            content = "This Is A Document With MIXED case.",
            metadata = mapOf("source" to "test")
        )
        
        // Create cleaner
        val cleaner = DocumentCleaner(
            removeExtraWhitespace = true,
            removeUrls = false,
            removeEmails = false,
            removeHtmlTags = false,
            removeSpecialChars = false,
            toLowerCase = true
        )
        
        // Transform document
        val transformed = cleaner.transform(document)
        
        // Verify results
        assertEquals("this is a document with mixed case.", transformed.content)
    }
    
    @Test
    fun `test remove extra whitespace`() {
        // Create document with extra whitespace
        val document = Document(
            id = "test-id",
            content = "  This   is  a   document   with  extra   whitespace.  ",
            metadata = mapOf("source" to "test")
        )
        
        // Create cleaner
        val cleaner = DocumentCleaner(
            removeExtraWhitespace = true,
            removeUrls = false,
            removeEmails = false,
            removeHtmlTags = false,
            removeSpecialChars = false,
            toLowerCase = false
        )
        
        // Transform document
        val transformed = cleaner.transform(document)
        
        // Verify results
        assertEquals("This is a document with extra whitespace.", transformed.content)
    }
}
