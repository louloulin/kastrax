package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream

private val logger = KotlinLogging.logger {}

/**
 * PDF文档加载器，从PDF文件加载文档。
 *
 * @property file 要加载的PDF文件
 * @property metadata 要添加到文档的元数据
 * @property extractTitle 是否从PDF中提取标题，默认为true
 * @property extractBookmarks 是否从PDF中提取书签，默认为true
 * @property extractMetadata 是否从PDF中提取元数据，默认为true
 * @property startPage 开始提取的页码（从1开始），默认为1
 * @property endPage 结束提取的页码，默认为-1（表示最后一页）
 */
class PdfDocumentLoader(
    private val file: File,
    private val metadata: Map<String, Any> = emptyMap(),
    private val extractTitle: Boolean = true,
    private val extractBookmarks: Boolean = true,
    private val extractMetadata: Boolean = true,
    private val startPage: Int = 1,
    private val endPage: Int = -1
) : DocumentLoader {

    constructor(
        filePath: String,
        metadata: Map<String, Any> = emptyMap(),
        extractTitle: Boolean = true,
        extractBookmarks: Boolean = true,
        extractMetadata: Boolean = true,
        startPage: Int = 1,
        endPage: Int = -1
    ) : this(File(filePath), metadata, extractTitle, extractBookmarks, extractMetadata, startPage, endPage)

    constructor(
        inputStream: InputStream,
        metadata: Map<String, Any> = emptyMap(),
        extractTitle: Boolean = true,
        extractBookmarks: Boolean = true,
        extractMetadata: Boolean = true,
        startPage: Int = 1,
        endPage: Int = -1
    ) : this(
        createTempFile(inputStream),
        metadata,
        extractTitle,
        extractBookmarks,
        extractMetadata,
        startPage,
        endPage
    )

    override suspend fun load(): List<Document> {
        logger.debug { "Loading PDF file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            PDDocument.load(file).use { document ->
                val totalPages = document.numberOfPages
                
                // 验证页码范围
                val validStartPage = startPage.coerceIn(1, totalPages)
                val validEndPage = if (endPage <= 0) totalPages else endPage.coerceIn(validStartPage, totalPages)
                
                // 提取文本
                val textStripper = PDFTextStripper()
                textStripper.startPage = validStartPage
                textStripper.endPage = validEndPage
                val content = textStripper.getText(document)
                
                // 构建元数据
                val documentMetadata = mutableMapOf<String, Any>(
                    "source" to file.absolutePath,
                    "file_name" to file.name,
                    "file_extension" to "pdf",
                    "file_size" to file.length(),
                    "file_last_modified" to file.lastModified(),
                    "page_count" to totalPages,
                    "extracted_pages" to "${validStartPage}-${validEndPage}"
                )
                
                // 提取PDF元数据
                if (extractMetadata) {
                    val pdfInfo = document.documentInformation
                    pdfInfo?.title?.let { documentMetadata["pdf_title"] = it }
                    pdfInfo?.author?.let { documentMetadata["pdf_author"] = it }
                    pdfInfo?.subject?.let { documentMetadata["pdf_subject"] = it }
                    pdfInfo?.keywords?.let { documentMetadata["pdf_keywords"] = it }
                    pdfInfo?.creator?.let { documentMetadata["pdf_creator"] = it }
                    pdfInfo?.producer?.let { documentMetadata["pdf_producer"] = it }
                    pdfInfo?.creationDate?.let { documentMetadata["pdf_creation_date"] = it.time }
                    pdfInfo?.modificationDate?.let { documentMetadata["pdf_modification_date"] = it.time }
                }
                
                // 如果提取标题但未从PDF元数据中获取到，则使用文件名作为标题
                if (extractTitle && !documentMetadata.containsKey("pdf_title")) {
                    documentMetadata["title"] = file.nameWithoutExtension
                }
                
                // 合并用户提供的元数据
                val fullMetadata = documentMetadata + metadata
                
                listOf(Document(content, fullMetadata))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading PDF file: ${file.absolutePath}" }
            emptyList()
        }
    }

    companion object {
        /**
         * 从输入流创建临时文件。
         */
        private fun createTempFile(inputStream: InputStream): File {
            val tempFile = File.createTempFile("pdf_", ".pdf")
            tempFile.deleteOnExit()
            
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            return tempFile
        }
    }
}
