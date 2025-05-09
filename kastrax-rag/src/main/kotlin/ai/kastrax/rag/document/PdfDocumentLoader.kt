package ai.kastrax.rag.document

import ai.kastrax.store.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * PDF 文档加载器，用于加载 PDF 文件。
 *
 * @property path PDF 文件或目录路径
 * @property recursive 是否递归加载子目录
 * @property extractImages 是否提取图像
 * @property extractMetadata 是否提取元数据
 */
class PdfDocumentLoader(
    private val path: String,
    private val recursive: Boolean = false,
    private val extractImages: Boolean = false,
    private val extractMetadata: Boolean = true
) : DocumentLoader {
    /**
     * 加载文档。
     *
     * @return 加载的文档列表
     */
    override suspend fun load(): List<Document> = withContext(Dispatchers.IO) {
        val file = File(path)

        if (!file.exists()) {
            logger.error { "File or directory does not exist: $path" }
            return@withContext emptyList()
        }

        val documents = mutableListOf<Document>()

        try {
            if (file.isDirectory) {
                // 递归加载目录中的 PDF 文件
                val fileList = if (recursive) {
                    file.walkTopDown().filter { it.isFile && it.extension.lowercase() == "pdf" }.toList()
                } else {
                    file.listFiles()?.filter { it.isFile && it.extension.lowercase() == "pdf" }?.toList() ?: emptyList()
                }

                for (pdfFile in fileList) {
                    try {
                        val content = extractTextFromPdf(pdfFile)
                        val doc = Document(
                            id = UUID.randomUUID().toString(),
                            content = content,
                            metadata = mapOf(
                                "source" to pdfFile.absolutePath,
                                "filename" to pdfFile.name,
                                "extension" to "pdf",
                                "size" to pdfFile.length(),
                                "last_modified" to pdfFile.lastModified()
                            )
                        )
                        documents.add(doc)
                    } catch (e: Exception) {
                        logger.error(e) { "Error loading PDF file: ${pdfFile.absolutePath}" }
                    }
                }
            } else if (file.extension.lowercase() == "pdf") {
                // 加载单个 PDF 文件
                val content = extractTextFromPdf(file)
                val doc = Document(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    metadata = mapOf(
                        "source" to file.absolutePath,
                        "filename" to file.name,
                        "extension" to "pdf",
                        "size" to file.length(),
                        "last_modified" to file.lastModified()
                    )
                )
                documents.add(doc)
            } else {
                logger.warn { "Not a PDF file: ${file.absolutePath}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents" }
        }

        return@withContext documents
    }

    /**
     * 从 PDF 文件中提取文本。
     *
     * @param file PDF 文件
     * @return 提取的文本
     */
    private fun extractTextFromPdf(file: File): String {
        PDDocument.load(file).use { document ->
            val stripper = PDFTextStripper()
            return stripper.getText(document)
        }
    }
}
