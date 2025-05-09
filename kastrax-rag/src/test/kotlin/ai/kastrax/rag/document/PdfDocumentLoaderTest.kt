package ai.kastrax.rag.document

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Calendar
import java.util.Date

class PdfDocumentLoaderTest {

    private lateinit var mockFile: File
    private lateinit var mockPdDocument: PDDocument
    private lateinit var mockTextStripper: PDFTextStripper
    private lateinit var mockDocInfo: PDDocumentInformation

    @BeforeEach
    fun setUp() {
        mockFile = mockk(relaxed = true)
        mockPdDocument = mockk(relaxed = true)
        mockTextStripper = mockk(relaxed = true)
        mockDocInfo = mockk(relaxed = true)

        // Mock File properties
        every { mockFile.exists() } returns true
        every { mockFile.isDirectory } returns false
        every { mockFile.extension } returns "pdf"
        every { mockFile.absolutePath } returns "/path/to/test.pdf"
        every { mockFile.name } returns "test.pdf"
        every { mockFile.length() } returns 1024L
        every { mockFile.lastModified() } returns 1234567890L

        // Mock PDDocument
        mockkStatic(PDDocument::class)
        every { PDDocument.load(any<File>()) } returns mockPdDocument
        every { mockPdDocument.numberOfPages } returns 10
        every { mockPdDocument.documentInformation } returns mockDocInfo
        every { mockPdDocument.close() } returns Unit

        // Mock PDFTextStripper
        mockkStatic(PDFTextStripper::class)
        every { PDFTextStripper().getText(mockPdDocument) } returns "This is a test PDF document."

        // Mock PDDocumentInformation
        every { mockDocInfo.title } returns "Test PDF"
        every { mockDocInfo.author } returns "Test Author"
        every { mockDocInfo.subject } returns "Test Subject"
        every { mockDocInfo.keywords } returns "test, pdf"
        every { mockDocInfo.creator } returns "Test Creator"
        every { mockDocInfo.producer } returns "Test Producer"
        val creationDate = Calendar.getInstance()
        creationDate.timeInMillis = 1234567890L
        val modificationDate = Calendar.getInstance()
        modificationDate.timeInMillis = 1234567890L
        every { mockDocInfo.creationDate } returns creationDate
        every { mockDocInfo.modificationDate } returns modificationDate
    }

    @Test
    fun `test load single PDF file`() = runBlocking {
        // Create loader
        val loader = PdfDocumentLoader("/path/to/test.pdf")

        // Load documents
        val documents = loader.load()

        // Verify results
        assertEquals(1, documents.size)
        val doc = documents[0]
        assertEquals("This is a test PDF document.", doc.content)

        // Verify metadata
        assertTrue(doc.metadata.containsKey("source"))
        assertEquals("/path/to/test.pdf", doc.metadata["source"])
        assertEquals("test.pdf", doc.metadata["filename"])
        assertEquals("pdf", doc.metadata["extension"])
        assertEquals(1024L, doc.metadata["size"])
        assertEquals(1234567890L, doc.metadata["last_modified"])
        assertEquals(10, doc.metadata["page_count"])

        // Verify document metadata
        assertEquals("Test PDF", doc.metadata["title"])
        assertEquals("Test Author", doc.metadata["author"])
        assertEquals("Test Subject", doc.metadata["subject"])
        assertEquals("test, pdf", doc.metadata["keywords"])
        assertEquals("Test Creator", doc.metadata["creator"])
        assertEquals("Test Producer", doc.metadata["producer"])
        assertEquals(1234567890L, doc.metadata["creation_date"])
        assertEquals(1234567890L, doc.metadata["modification_date"])
    }
}
