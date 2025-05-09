package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

/**
 * CSV文档加载器，从CSV文件加载文档。
 *
 * @property file 要加载的CSV文件
 * @property encoding 文件编码，默认为UTF-8
 * @property metadata 要添加到文档的元数据
 * @property delimiter 分隔符，默认为逗号
 * @property hasHeaderRow 是否有标题行，默认为true
 * @property includeHeaderInContent 是否在内容中包含标题，默认为true
 * @property documentPerRow 是否为每行创建一个文档，默认为false（整个CSV作为一个文档）
 * @property columnDelimiter 列之间的分隔符，用于构建文本内容，默认为": "
 * @property rowDelimiter 行之间的分隔符，用于构建文本内容，默认为"\n"
 */
class CsvDocumentLoader(
    private val file: File,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap(),
    private val delimiter: Char = ',',
    private val hasHeaderRow: Boolean = true,
    private val includeHeaderInContent: Boolean = true,
    private val documentPerRow: Boolean = false,
    private val columnDelimiter: String = ": ",
    private val rowDelimiter: String = "\n"
) : DocumentLoader {

    constructor(
        filePath: String,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        delimiter: Char = ',',
        hasHeaderRow: Boolean = true,
        includeHeaderInContent: Boolean = true,
        documentPerRow: Boolean = false,
        columnDelimiter: String = ": ",
        rowDelimiter: String = "\n"
    ) : this(
        File(filePath), encoding, metadata, delimiter, hasHeaderRow,
        includeHeaderInContent, documentPerRow, columnDelimiter, rowDelimiter
    )

    constructor(
        inputStream: InputStream,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        delimiter: Char = ',',
        hasHeaderRow: Boolean = true,
        includeHeaderInContent: Boolean = true,
        documentPerRow: Boolean = false,
        columnDelimiter: String = ": ",
        rowDelimiter: String = "\n"
    ) : this(
        createTempFile(inputStream),
        encoding, metadata, delimiter, hasHeaderRow,
        includeHeaderInContent, documentPerRow, columnDelimiter, rowDelimiter
    )

    override suspend fun load(): List<Document> {
        logger.debug { "Loading CSV file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            val charset = Charset.forName(encoding)
            val csvFormat = if (hasHeaderRow) {
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(delimiter)
            } else {
                CSVFormat.DEFAULT.withDelimiter(delimiter)
            }

            FileReader(file, charset).use { reader ->
                val parser = CSVParser(reader, csvFormat)
                
                // 基础元数据
                val baseMetadata = mutableMapOf(
                    "source" to file.absolutePath,
                    "file_name" to file.name,
                    "file_extension" to "csv",
                    "file_size" to file.length(),
                    "file_last_modified" to file.lastModified(),
                    "csv_delimiter" to delimiter.toString()
                )
                
                // 合并用户提供的元数据
                val fullMetadata = baseMetadata + metadata
                
                if (documentPerRow) {
                    // 为每行创建一个文档
                    val documents = mutableListOf<Document>()
                    val headers = if (hasHeaderRow) parser.headerNames else emptyList()
                    
                    parser.records.forEachIndexed { index, record ->
                        val rowContent = buildRowContent(record, headers)
                        val rowMetadata = fullMetadata + mapOf(
                            "row_index" to index,
                            "row_number" to (index + 1)
                        )
                        
                        documents.add(Document(rowContent, rowMetadata))
                    }
                    
                    documents
                } else {
                    // 整个CSV作为一个文档
                    val headers = if (hasHeaderRow) parser.headerNames else emptyList()
                    val content = buildCsvContent(parser, headers)
                    
                    val csvMetadata = fullMetadata + mapOf(
                        "row_count" to parser.recordNumber,
                        "has_header" to hasHeaderRow
                    )
                    
                    if (hasHeaderRow) {
                        csvMetadata + mapOf("headers" to headers)
                    }
                    
                    listOf(Document(content, csvMetadata))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading CSV file: ${file.absolutePath}" }
            emptyList()
        }
    }

    /**
     * 为单行构建内容。
     */
    private fun buildRowContent(record: org.apache.commons.csv.CSVRecord, headers: List<String>): String {
        val builder = StringBuilder()
        
        if (headers.isNotEmpty()) {
            // 使用标题构建内容
            for (i in 0 until record.size()) {
                if (i < headers.size) {
                    builder.append(headers[i])
                    builder.append(columnDelimiter)
                    builder.append(record[i])
                    if (i < record.size() - 1) {
                        builder.append(rowDelimiter)
                    }
                }
            }
        } else {
            // 没有标题，只使用值
            for (i in 0 until record.size()) {
                builder.append(record[i])
                if (i < record.size() - 1) {
                    builder.append(columnDelimiter)
                }
            }
        }
        
        return builder.toString()
    }

    /**
     * 为整个CSV构建内容。
     */
    private fun buildCsvContent(parser: CSVParser, headers: List<String>): String {
        val builder = StringBuilder()
        
        // 如果需要，添加标题行
        if (includeHeaderInContent && headers.isNotEmpty()) {
            builder.append(headers.joinToString(columnDelimiter))
            builder.append(rowDelimiter)
        }
        
        // 添加所有数据行
        parser.records.forEach { record ->
            val rowContent = if (headers.isNotEmpty()) {
                // 使用标题构建行内容
                headers.mapIndexed { i, header ->
                    if (i < record.size()) {
                        "$header$columnDelimiter${record[i]}"
                    } else {
                        "$header$columnDelimiter"
                    }
                }.joinToString(rowDelimiter)
            } else {
                // 没有标题，只使用值
                record.map { it }.joinToString(columnDelimiter)
            }
            
            builder.append(rowContent)
            builder.append(rowDelimiter)
        }
        
        return builder.toString()
    }

    companion object {
        /**
         * 从输入流创建临时文件。
         */
        private fun createTempFile(inputStream: InputStream): File {
            val tempFile = File.createTempFile("csv_", ".csv")
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
