package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.dom4j.Document as Dom4jDocument
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.SAXReader
import java.io.File
import java.io.InputStream
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

/**
 * XML文档加载器，从XML文件加载文档。
 *
 * @property file 要加载的XML文件
 * @property encoding 文件编码，默认为UTF-8
 * @property metadata 要添加到文档的元数据
 * @property documentPerElement 是否为每个指定元素创建一个文档，默认为false（整个XML作为一个文档）
 * @property elementXPath 当documentPerElement为true时，用于选择要为其创建文档的元素的XPath表达式
 * @property includeAttributes 是否在内容中包含属性，默认为true
 * @property maxDepth 递归解析XML的最大深度，默认为10
 * @property prettyPrint 是否美化输出，默认为false
 */
class XmlDocumentLoader(
    private val file: File,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap(),
    private val documentPerElement: Boolean = false,
    private val elementXPath: String? = null,
    private val includeAttributes: Boolean = true,
    private val maxDepth: Int = 10,
    private val prettyPrint: Boolean = false
) : DocumentLoader {

    constructor(
        filePath: String,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        documentPerElement: Boolean = false,
        elementXPath: String? = null,
        includeAttributes: Boolean = true,
        maxDepth: Int = 10,
        prettyPrint: Boolean = false
    ) : this(
        File(filePath), encoding, metadata, documentPerElement,
        elementXPath, includeAttributes, maxDepth, prettyPrint
    )

    constructor(
        inputStream: InputStream,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        documentPerElement: Boolean = false,
        elementXPath: String? = null,
        includeAttributes: Boolean = true,
        maxDepth: Int = 10,
        prettyPrint: Boolean = false
    ) : this(
        createTempFile(inputStream),
        encoding, metadata, documentPerElement,
        elementXPath, includeAttributes, maxDepth, prettyPrint
    )

    override suspend fun load(): List<Document> {
        logger.debug { "Loading XML file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            val reader = SAXReader()
            reader.encoding = encoding
            val xmlDocument = reader.read(file)
            
            // 基础元数据
            val baseMetadata = mutableMapOf(
                "source" to file.absolutePath,
                "file_name" to file.name,
                "file_extension" to "xml",
                "file_size" to file.length(),
                "file_last_modified" to file.lastModified(),
                "xml_root_element" to xmlDocument.rootElement.name
            )
            
            // 合并用户提供的元数据
            val fullMetadata = baseMetadata + metadata
            
            if (documentPerElement && elementXPath != null) {
                // 为每个匹配的元素创建一个文档
                val elements = xmlDocument.selectNodes(elementXPath)
                val documents = mutableListOf<Document>()
                
                elements.forEachIndexed { index, node ->
                    if (node is Element) {
                        val elementContent = if (prettyPrint) {
                            elementToPrettyString(node)
                        } else {
                            elementToText(node, 0)
                        }
                        
                        val elementMetadata = fullMetadata + mapOf(
                            "element_name" to node.name,
                            "element_index" to index,
                            "element_path" to node.path
                        )
                        
                        documents.add(Document(elementContent, elementMetadata))
                    }
                }
                
                documents
            } else {
                // 整个XML作为一个文档
                val content = if (prettyPrint) {
                    xmlDocumentToPrettyString(xmlDocument)
                } else {
                    elementToText(xmlDocument.rootElement, 0)
                }
                
                listOf(Document(content, fullMetadata))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading XML file: ${file.absolutePath}" }
            emptyList()
        }
    }

    /**
     * 将元素转换为文本。
     */
    private fun elementToText(element: Element, depth: Int): String {
        if (depth > maxDepth) {
            return "[嵌套过深]"
        }
        
        val indent = "  ".repeat(depth)
        val builder = StringBuilder()
        
        builder.append("$indent${element.name}")
        
        // 添加属性
        if (includeAttributes && element.attributes().isNotEmpty()) {
            builder.append(" {")
            element.attributes().joinTo(builder, ", ") { "${it.name}=\"${it.value}\"" }
            builder.append("}")
        }
        
        // 添加文本内容
        val text = element.textTrim
        if (text.isNotEmpty()) {
            builder.append(": $text")
        }
        
        builder.append("\n")
        
        // 递归处理子元素
        element.elements().forEach { child ->
            builder.append(elementToText(child, depth + 1))
        }
        
        return builder.toString()
    }

    /**
     * 将元素转换为格式化的XML字符串。
     */
    private fun elementToPrettyString(element: Element): String {
        val document = DocumentHelper.createDocument()
        document.add(element.createCopy())
        return xmlDocumentToPrettyString(document)
    }

    /**
     * 将XML文档转换为格式化的XML字符串。
     */
    private fun xmlDocumentToPrettyString(document: Dom4jDocument): String {
        val writer = StringWriter()
        val format = org.dom4j.io.OutputFormat.createPrettyPrint()
        format.encoding = encoding
        val xmlWriter = org.dom4j.io.XMLWriter(writer, format)
        xmlWriter.write(document)
        return writer.toString()
    }

    companion object {
        /**
         * 从输入流创建临时文件。
         */
        private fun createTempFile(inputStream: InputStream): File {
            val tempFile = File.createTempFile("xml_", ".xml")
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
