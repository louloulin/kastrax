package ai.kastrax.rag.document

import ai.kastrax.store.document.Document
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * JSON 文档加载器，用于加载 JSON 文件。
 *
 * @property path JSON 文件或目录路径
 * @property recursive 是否递归加载子目录
 */
class JsonDocumentLoader(
    private val path: String,
    private val recursive: Boolean = false
) : DocumentLoader {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    
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
                // 递归加载目录中的 JSON 文件
                val fileList = if (recursive) {
                    file.walkTopDown().filter { it.isFile && it.extension.lowercase() == "json" }.toList()
                } else {
                    file.listFiles()?.filter { it.isFile && it.extension.lowercase() == "json" }?.toList() ?: emptyList()
                }
                
                for (jsonFile in fileList) {
                    try {
                        val content = readJsonFile(jsonFile)
                        val doc = Document(
                            id = UUID.randomUUID().toString(),
                            content = content,
                            metadata = mapOf(
                                "source" to jsonFile.absolutePath,
                                "filename" to jsonFile.name,
                                "extension" to "json",
                                "size" to jsonFile.length(),
                                "last_modified" to jsonFile.lastModified()
                            )
                        )
                        documents.add(doc)
                    } catch (e: Exception) {
                        logger.error(e) { "Error loading JSON file: ${jsonFile.absolutePath}" }
                    }
                }
            } else if (file.extension.lowercase() == "json") {
                // 加载单个 JSON 文件
                val content = readJsonFile(file)
                val doc = Document(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    metadata = mapOf(
                        "source" to file.absolutePath,
                        "filename" to file.name,
                        "extension" to "json",
                        "size" to file.length(),
                        "last_modified" to file.lastModified()
                    )
                )
                documents.add(doc)
            } else {
                logger.warn { "Not a JSON file: ${file.absolutePath}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents" }
        }
        
        return@withContext documents
    }
    
    /**
     * 读取 JSON 文件。
     *
     * @param file JSON 文件
     * @return 读取的内容
     */
    private fun readJsonFile(file: File): String {
        val node = objectMapper.readTree(file)
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
    }
}
