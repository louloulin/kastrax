package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.InputStream

private val logger = KotlinLogging.logger {}

/**
 * Markdown文档加载器，从Markdown文件加载文档。
 *
 * @property file 要加载的Markdown文件
 * @property encoding 文件编码，默认为UTF-8
 * @property metadata 要添加到文档的元数据
 * @property extractTitle 是否从Markdown中提取标题，默认为true
 * @property extractFrontMatter 是否提取前置元数据，默认为true
 * @property documentPerSection 是否为每个章节创建一个文档，默认为false（整个Markdown作为一个文档）
 * @property sectionLevel 当documentPerSection为true时，用于分割的标题级别，默认为1（# 一级标题）
 */
class MarkdownDocumentLoader(
    private val file: File,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap(),
    private val extractTitle: Boolean = true,
    private val extractFrontMatter: Boolean = true,
    private val documentPerSection: Boolean = false,
    private val sectionLevel: Int = 1
) : DocumentLoader {

    constructor(
        filePath: String,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        extractTitle: Boolean = true,
        extractFrontMatter: Boolean = true,
        documentPerSection: Boolean = false,
        sectionLevel: Int = 1
    ) : this(
        File(filePath), encoding, metadata, extractTitle,
        extractFrontMatter, documentPerSection, sectionLevel
    )

    constructor(
        inputStream: InputStream,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        extractTitle: Boolean = true,
        extractFrontMatter: Boolean = true,
        documentPerSection: Boolean = false,
        sectionLevel: Int = 1
    ) : this(
        createTempFile(inputStream),
        encoding, metadata, extractTitle,
        extractFrontMatter, documentPerSection, sectionLevel
    )

    override suspend fun load(): List<Document> {
        logger.debug { "Loading Markdown file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            val content = file.readText(Charsets.UTF_8)
            
            // 基础元数据
            val baseMetadata = mutableMapOf(
                "source" to file.absolutePath,
                "file_name" to file.name,
                "file_extension" to (file.extension.takeIf { it.isNotEmpty() } ?: "md"),
                "file_size" to file.length(),
                "file_last_modified" to file.lastModified()
            )
            
            // 提取前置元数据
            val (strippedContent, frontMatter) = if (extractFrontMatter) {
                extractFrontMatter(content)
            } else {
                Pair(content, emptyMap())
            }
            
            // 提取标题
            if (extractTitle) {
                val title = extractTitle(strippedContent)
                if (title != null) {
                    baseMetadata["title"] = title
                }
            }
            
            // 合并所有元数据
            val fullMetadata = baseMetadata + frontMatter + metadata
            
            if (documentPerSection) {
                // 为每个章节创建一个文档
                val sections = splitIntoSections(strippedContent, sectionLevel)
                
                sections.mapIndexed { index, (sectionTitle, sectionContent) ->
                    val sectionMetadata = fullMetadata + mapOf(
                        "section_title" to sectionTitle,
                        "section_index" to index,
                        "section_number" to (index + 1)
                    )
                    
                    Document(sectionContent, sectionMetadata)
                }
            } else {
                // 整个Markdown作为一个文档
                listOf(Document(strippedContent, fullMetadata))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading Markdown file: ${file.absolutePath}" }
            emptyList()
        }
    }

    /**
     * 提取Markdown文档的前置元数据。
     * 
     * @param content Markdown内容
     * @return 去除前置元数据的内容和提取的元数据
     */
    private fun extractFrontMatter(content: String): Pair<String, Map<String, Any>> {
        val frontMatterRegex = Regex("""^---\s*\n(.*?)\n---\s*\n""", RegexOption.DOT_MATCHES_ALL)
        val match = frontMatterRegex.find(content)
        
        if (match != null) {
            val frontMatterText = match.groupValues[1]
            val metadata = mutableMapOf<String, Any>()
            
            // 解析YAML格式的前置元数据
            frontMatterText.lines().forEach { line ->
                val keyValue = line.split(":", limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim()
                    val value = keyValue[1].trim()
                    if (key.isNotEmpty()) {
                        metadata[key] = value
                    }
                }
            }
            
            // 去除前置元数据
            val strippedContent = content.substring(match.range.last + 1)
            
            return Pair(strippedContent, metadata)
        }
        
        return Pair(content, emptyMap())
    }

    /**
     * 提取Markdown文档的标题。
     * 
     * @param content Markdown内容
     * @return 提取的标题，如果没有找到则返回null
     */
    private fun extractTitle(content: String): String? {
        // 查找第一个标题
        val titleRegex = Regex("""^#\s+(.+)$""", RegexOption.MULTILINE)
        val match = titleRegex.find(content)
        
        return match?.groupValues?.get(1)?.trim()
    }

    /**
     * 将Markdown内容分割为章节。
     * 
     * @param content Markdown内容
     * @param level 标题级别（1-6）
     * @return 章节列表，每个章节包含标题和内容
     */
    private fun splitIntoSections(content: String, level: Int): List<Pair<String, String>> {
        if (level < 1 || level > 6) {
            throw IllegalArgumentException("Section level must be between 1 and 6")
        }
        
        val headerPrefix = "#".repeat(level)
        val headerRegex = Regex("""^$headerPrefix\s+(.+)$""", RegexOption.MULTILINE)
        
        val sections = mutableListOf<Pair<String, String>>()
        val matches = headerRegex.findAll(content)
        
        if (matches.count() == 0) {
            // 没有找到章节，将整个内容作为一个章节
            return listOf(Pair("", content))
        }
        
        matches.forEachIndexed { index, match ->
            val title = match.groupValues[1].trim()
            val startPos = match.range.first
            
            val endPos = if (index < matches.count() - 1) {
                matches.elementAt(index + 1).range.first
            } else {
                content.length
            }
            
            val sectionContent = content.substring(startPos, endPos).trim()
            sections.add(Pair(title, sectionContent))
        }
        
        return sections
    }

    companion object {
        /**
         * 从输入流创建临时文件。
         */
        private fun createTempFile(inputStream: InputStream): File {
            val tempFile = File.createTempFile("markdown_", ".md")
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
