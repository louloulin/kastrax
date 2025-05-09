package ai.kastrax.rag.document

import ai.kastrax.store.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 文本文件加载器，加载文本文件。
 *
 * @property path 文件或目录路径
 * @property fileExtensions 文件扩展名列表
 * @property encoding 文件编码
 */
class TextFileLoader(
    private val path: String,
    private val fileExtensions: List<String> = listOf("txt", "md", "html", "json", "csv"),
    private val encoding: String = "UTF-8"
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
        
        if (file.isDirectory) {
            // 递归加载目录中的文件
            file.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in fileExtensions }
                .forEach { documents.add(loadFile(it)) }
        } else {
            // 加载单个文件
            if (file.extension.lowercase() in fileExtensions) {
                documents.add(loadFile(file))
            } else {
                logger.warn { "Skipping file with unsupported extension: ${file.name}" }
            }
        }
        
        return@withContext documents
    }
    
    /**
     * 加载单个文件。
     *
     * @param file 文件
     * @return 文档
     */
    private fun loadFile(file: File): Document {
        logger.debug { "Loading file: ${file.absolutePath}" }
        
        val content = file.readText(Charset.forName(encoding))
        
        return Document(
            id = UUID.randomUUID().toString(),
            content = content,
            metadata = mapOf(
                "source" to file.absolutePath,
                "filename" to file.name,
                "extension" to file.extension,
                "size" to file.length(),
                "last_modified" to file.lastModified()
            )
        )
    }
}
