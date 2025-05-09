package ai.kastrax.rag.document

import ai.kastrax.store.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.nio.charset.Charset
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * CSV 文档加载器，用于加载 CSV 文件。
 *
 * @property path CSV 文件或目录路径
 * @property recursive 是否递归加载子目录
 * @property delimiter 分隔符
 * @property encoding 文件编码
 */
class CsvDocumentLoader(
    private val path: String,
    private val recursive: Boolean = false,
    private val delimiter: Char = ',',
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
        
        try {
            if (file.isDirectory) {
                // 递归加载目录中的 CSV 文件
                val fileList = if (recursive) {
                    file.walkTopDown().filter { it.isFile && it.extension.lowercase() == "csv" }.toList()
                } else {
                    file.listFiles()?.filter { it.isFile && it.extension.lowercase() == "csv" }?.toList() ?: emptyList()
                }
                
                for (csvFile in fileList) {
                    try {
                        val content = readCsvFile(csvFile)
                        val doc = Document(
                            id = UUID.randomUUID().toString(),
                            content = content,
                            metadata = mapOf(
                                "source" to csvFile.absolutePath,
                                "filename" to csvFile.name,
                                "extension" to "csv",
                                "size" to csvFile.length(),
                                "last_modified" to csvFile.lastModified()
                            )
                        )
                        documents.add(doc)
                    } catch (e: Exception) {
                        logger.error(e) { "Error loading CSV file: ${csvFile.absolutePath}" }
                    }
                }
            } else if (file.extension.lowercase() == "csv") {
                // 加载单个 CSV 文件
                val content = readCsvFile(file)
                val doc = Document(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    metadata = mapOf(
                        "source" to file.absolutePath,
                        "filename" to file.name,
                        "extension" to "csv",
                        "size" to file.length(),
                        "last_modified" to file.lastModified()
                    )
                )
                documents.add(doc)
            } else {
                logger.warn { "Not a CSV file: ${file.absolutePath}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents" }
        }
        
        return@withContext documents
    }
    
    /**
     * 读取 CSV 文件。
     *
     * @param file CSV 文件
     * @return 读取的内容
     */
    private fun readCsvFile(file: File): String {
        val charset = Charset.forName(encoding)
        val format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
        
        val result = StringBuilder()
        
        CSVParser.parse(file, charset, format).use { parser ->
            val headers = parser.headerNames
            result.append(headers.joinToString(",")).append("\n")
            
            for (record in parser) {
                val values = headers.map { header -> record.get(header) }
                result.append(values.joinToString(",")).append("\n")
            }
        }
        
        return result.toString()
    }
}
