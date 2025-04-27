package ai.kastrax.rag.document

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.InputStream

private val logger = KotlinLogging.logger {}

/**
 * JSON文档加载器，从JSON文件加载文档。
 *
 * @property file 要加载的JSON文件
 * @property encoding 文件编码，默认为UTF-8
 * @property metadata 要添加到文档的元数据
 * @property documentPerItem 是否为每个顶级数组项创建一个文档，默认为false（整个JSON作为一个文档）
 * @property contentFields 要包含在内容中的字段列表，默认为null（包含所有字段）
 * @property keyDelimiter 键值对之间的分隔符，用于构建文本内容，默认为": "
 * @property fieldDelimiter 字段之间的分隔符，用于构建文本内容，默认为"\n"
 * @property maxDepth 递归解析JSON的最大深度，默认为5
 */
class JsonDocumentLoader(
    private val file: File,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap(),
    private val documentPerItem: Boolean = false,
    private val contentFields: List<String>? = null,
    private val keyDelimiter: String = ": ",
    private val fieldDelimiter: String = "\n",
    private val maxDepth: Int = 5
) : DocumentLoader {

    constructor(
        filePath: String,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        documentPerItem: Boolean = false,
        contentFields: List<String>? = null,
        keyDelimiter: String = ": ",
        fieldDelimiter: String = "\n",
        maxDepth: Int = 5
    ) : this(
        File(filePath), encoding, metadata, documentPerItem,
        contentFields, keyDelimiter, fieldDelimiter, maxDepth
    )

    constructor(
        inputStream: InputStream,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        documentPerItem: Boolean = false,
        contentFields: List<String>? = null,
        keyDelimiter: String = ": ",
        fieldDelimiter: String = "\n",
        maxDepth: Int = 5
    ) : this(
        createTempFile(inputStream),
        encoding, metadata, documentPerItem,
        contentFields, keyDelimiter, fieldDelimiter, maxDepth
    )

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    override suspend fun load(): List<Document> {
        logger.debug { "Loading JSON file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            // 基础元数据
            val baseMetadata = mutableMapOf(
                "source" to file.absolutePath,
                "file_name" to file.name,
                "file_extension" to "json",
                "file_size" to file.length(),
                "file_last_modified" to file.lastModified()
            )
            
            // 合并用户提供的元数据
            val fullMetadata = baseMetadata + metadata
            
            // 解析JSON
            val rootNode = objectMapper.readTree(file)
            
            if (documentPerItem && rootNode.isArray) {
                // 为数组中的每个项创建一个文档
                val documents = mutableListOf<Document>()
                
                rootNode.forEachIndexed { index, item ->
                    val itemContent = nodeToText(item, 0)
                    val itemMetadata = fullMetadata + mapOf(
                        "item_index" to index,
                        "item_number" to (index + 1)
                    )
                    
                    documents.add(Document(itemContent, itemMetadata))
                }
                
                documents
            } else {
                // 整个JSON作为一个文档
                val content = nodeToText(rootNode, 0)
                listOf(Document(content, fullMetadata))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading JSON file: ${file.absolutePath}" }
            emptyList()
        }
    }

    /**
     * 将JSON节点转换为文本。
     */
    private fun nodeToText(node: JsonNode, depth: Int): String {
        if (depth > maxDepth) {
            return "[嵌套过深]"
        }
        
        return when {
            node.isObject -> {
                val fields = node.fields().asSequence().toList()
                val filteredFields = if (contentFields != null && depth == 0) {
                    fields.filter { contentFields.contains(it.key) }
                } else {
                    fields
                }
                
                filteredFields.joinToString(fieldDelimiter) { (key, value) ->
                    "$key$keyDelimiter${nodeToText(value, depth + 1)}"
                }
            }
            node.isArray -> {
                node.map { nodeToText(it, depth + 1) }.joinToString(", ")
            }
            node.isTextual -> node.asText()
            node.isNumber -> node.asText()
            node.isBoolean -> node.asText()
            node.isNull -> "null"
            else -> node.toString()
        }
    }

    companion object {
        /**
         * 从输入流创建临时文件。
         */
        private fun createTempFile(inputStream: InputStream): File {
            val tempFile = File.createTempFile("json_", ".json")
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
