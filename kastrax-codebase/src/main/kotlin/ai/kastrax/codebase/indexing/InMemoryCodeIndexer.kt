package ai.kastrax.codebase.indexing

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.codebase.vector.CodeVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.streams.asSequence

private val logger = KotlinLogging.logger {}

/**
 * 内存代码索引器配置
 *
 * @property batchSize 批处理大小
 * @property excludePatterns 排除模式列表
 * @property includePatterns 包含模式列表
 * @property maxFileSizeBytes 最大文件大小（字节）
 * @property supportedExtensions 支持的文件扩展名
 */
data class InMemoryCodeIndexerConfig(
    val batchSize: Int = 50,
    val excludePatterns: List<Regex> = listOf(
        Regex("node_modules"),
        Regex("\\.git"),
        Regex("\\.idea"),
        Regex("\\.gradle"),
        Regex("build"),
        Regex("dist"),
        Regex("target"),
        Regex("out"),
        Regex("bin"),
        Regex("obj"),
        Regex("\\.class$"),
        Regex("\\.jar$"),
        Regex("\\.war$"),
        Regex("\\.zip$"),
        Regex("\\.tar$"),
        Regex("\\.gz$"),
        Regex("\\.rar$"),
        Regex("\\.exe$"),
        Regex("\\.dll$"),
        Regex("\\.so$"),
        Regex("\\.dylib$"),
        Regex("\\.png$"),
        Regex("\\.jpg$"),
        Regex("\\.jpeg$"),
        Regex("\\.gif$"),
        Regex("\\.bmp$"),
        Regex("\\.ico$"),
        Regex("\\.svg$"),
        Regex("\\.pdf$"),
        Regex("\\.doc$"),
        Regex("\\.docx$"),
        Regex("\\.xls$"),
        Regex("\\.xlsx$"),
        Regex("\\.ppt$"),
        Regex("\\.pptx$")
    ),
    val includePatterns: List<Regex> = emptyList(),
    val maxFileSizeBytes: Long = 1024 * 1024, // 1MB
    val supportedExtensions: Set<String> = setOf(
        "java", "kt", "kts", "scala", "groovy",
        "py", "rb", "js", "ts", "jsx", "tsx",
        "c", "cpp", "h", "hpp", "cs", "go",
        "rs", "swift", "m", "mm", "php", "pl",
        "sh", "bash", "zsh", "fish", "bat", "cmd",
        "html", "htm", "css", "scss", "sass", "less",
        "xml", "json", "yaml", "yml", "toml", "ini",
        "md", "markdown", "txt", "rst", "adoc",
        "sql", "graphql", "proto", "thrift", "avsc"
    )
)

/**
 * 内存代码索引器
 *
 * 在内存中索引代码元素，支持向量搜索和关键词搜索
 *
 * @property embeddingService 代码嵌入服务
 * @property vectorStore 代码向量存储
 * @property config 配置
 */
class InMemoryCodeIndexer(
    private val embeddingService: CodeEmbeddingService,
    private val vectorStore: CodeVectorStore,
    private val config: InMemoryCodeIndexerConfig = InMemoryCodeIndexerConfig()
) : CodeIndexer, Closeable {
    // 存储所有索引的代码元素
    private val indexedElements = ConcurrentHashMap<String, CodeElement>()

    // 存储文件路径到代码元素的映射
    private val filePathToElements = ConcurrentHashMap<Path, List<CodeElement>>()

    // 存储元素类型到代码元素的映射
    private val typeToElements = ConcurrentHashMap<CodeElementType, MutableList<CodeElement>>()

    // 索引状态
    private val indexingStatus = ConcurrentHashMap<Path, IndexingStatus>()

    // 索引计数器
    private val indexedFilesCount = AtomicInteger(0)
    private val indexedElementsCount = AtomicInteger(0)

    /**
     * 索引状态
     */
    enum class IndexingStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    /**
     * 索引代码元素
     *
     * @param element 代码元素
     */
    override suspend fun indexElement(element: CodeElement) {
        indexedElements[element.id] = element

        // 将元素添加到类型映射
        typeToElements.getOrPut(element.type) { mutableListOf() }.add(element)

        // 生成嵌入向量并添加到向量存储
        val content = element.toString()
        val embedding = embeddingService.embed(content)
        vectorStore.addElement(element, embedding)
    }

    /**
     * 批量索引代码元素
     *
     * @param elements 代码元素集合
     */
    override suspend fun indexElements(elements: Collection<CodeElement>) {
        elements.forEach { indexElement(it) }
    }

    /**
     * 索引代码库
     *
     * @param rootPath 代码库根路径
     * @return 索引的元素数量
     */
    suspend fun indexCodebase(rootPath: Path): Int = withContext(Dispatchers.IO) {
        logger.info { "开始索引代码库: $rootPath" }

        // 重置索引状态
        reset()

        // 查找所有符合条件的文件
        val files = findCodeFiles(rootPath)
        logger.info { "找到 ${files.size} 个代码文件" }

        // 批量处理文件
        val batchSize = config.batchSize
        val batches = files.chunked(batchSize)

        var totalElements = 0

        for ((batchIndex, batch) in batches.withIndex()) {
            logger.info { "处理批次 ${batchIndex + 1}/${batches.size} (${batch.size} 个文件)" }

            val batchElements = coroutineScope {
                batch.map { file ->
                    async {
                        try {
                            indexingStatus[file] = IndexingStatus.IN_PROGRESS
                            val elements = indexFile(file)
                            indexingStatus[file] = IndexingStatus.COMPLETED
                            indexedFilesCount.incrementAndGet()
                            file to elements
                        } catch (e: Exception) {
                            logger.error(e) { "索引文件失败: $file" }
                            indexingStatus[file] = IndexingStatus.FAILED
                            file to emptyList<CodeElement>()
                        }
                    }
                }.awaitAll()
            }

            // 更新索引
            for ((file, elements) in batchElements) {
                if (elements.isNotEmpty()) {
                    filePathToElements[file] = elements

                    for (element in elements) {
                        indexedElements[element.id] = element
                        typeToElements.getOrPut(element.type) { mutableListOf() }.add(element)

                        // 递归处理子元素
                        val allChildren = element.getAllChildren()
                        for (child in allChildren) {
                            indexedElements[child.id] = child
                            typeToElements.getOrPut(child.type) { mutableListOf() }.add(child)
                        }
                    }

                    totalElements += elements.size
                    indexedElementsCount.addAndGet(elements.size)
                }
            }
        }

        logger.info { "代码库索引完成: $rootPath, 索引了 $totalElements 个元素" }
        return@withContext totalElements
    }

    /**
     * 索引单个文件
     *
     * @param filePath 文件路径
     * @return 索引的元素列表
     */
    suspend fun indexFile(filePath: Path): List<CodeElement> = withContext(Dispatchers.IO) {
        try {
            if (!shouldIndexFile(filePath)) {
                return@withContext emptyList()
            }

            val content = filePath.readText()

            // 使用代码解析器解析文件
            val parser = CodeParserFactory.getParser(filePath)
            if (parser == null) {
                logger.warn { "没有找到适合 $filePath 的解析器" }
                return@withContext emptyList()
            }

            val fileElement = parser.parseFile(filePath, content)

            // 为文件元素生成嵌入向量
            val embedding = embeddingService.embed(content)

            // 将元素添加到向量存储
            vectorStore.addElement(fileElement, embedding)

            // 为重要子元素生成嵌入向量
            val importantElements = fileElement.getAllChildren().filter {
                it.type in setOf(
                    CodeElementType.CLASS,
                    CodeElementType.INTERFACE,
                    CodeElementType.METHOD,
                    CodeElementType.FUNCTION,
                    CodeElementType.ENUM
                )
            }

            for (element in importantElements) {
                val elementText = extractElementText(element, content)
                if (elementText.isNotEmpty()) {
                    val elementEmbedding = embeddingService.embed(elementText)
                    vectorStore.addElement(element, elementEmbedding)
                }
            }

            return@withContext listOf(fileElement)
        } catch (e: Exception) {
            logger.error(e) { "索引文件失败: $filePath" }
            return@withContext emptyList()
        }
    }

    /**
     * 从文件内容中提取元素的文本
     *
     * @param element 代码元素
     * @param fileContent 文件内容
     * @return 元素的文本
     */
    private fun extractElementText(element: CodeElement, fileContent: String): String {
        val location = element.location
        if (!location.isValid()) {
            return ""
        }

        val lines = fileContent.lines()
        if (location.startLine > lines.size || location.endLine > lines.size) {
            return ""
        }

        return try {
            val startLine = location.startLine - 1
            val endLine = location.endLine - 1

            if (startLine == endLine) {
                val line = lines[startLine]
                val startCol = maxOf(0, location.startColumn - 1)
                val endCol = minOf(line.length, location.endColumn)
                if (startCol < endCol) {
                    line.substring(startCol, endCol)
                } else {
                    ""
                }
            } else {
                val result = StringBuilder()

                // 第一行
                val firstLine = lines[startLine]
                val startCol = maxOf(0, location.startColumn - 1)
                if (startCol < firstLine.length) {
                    result.append(firstLine.substring(startCol))
                }
                result.append("\n")

                // 中间行
                for (i in startLine + 1 until endLine) {
                    result.append(lines[i])
                    result.append("\n")
                }

                // 最后一行
                if (endLine < lines.size) {
                    val lastLine = lines[endLine]
                    val endCol = minOf(lastLine.length, location.endColumn)
                    if (endCol > 0) {
                        result.append(lastLine.substring(0, endCol))
                    }
                }

                result.toString()
            }
        } catch (e: Exception) {
            logger.error(e) { "提取元素文本失败: ${element.id}" }
            ""
        }
    }

    /**
     * 查找所有符合条件的代码文件
     *
     * @param rootPath 根路径
     * @return 文件路径列表
     */
    private fun findCodeFiles(rootPath: Path): List<Path> {
        if (!Files.exists(rootPath)) {
            logger.warn { "路径不存在: $rootPath" }
            return emptyList()
        }

        if (!rootPath.isDirectory()) {
            if (shouldIndexFile(rootPath)) {
                return listOf(rootPath)
            }
            return emptyList()
        }

        return Files.walk(rootPath)
            .asSequence()
            .filter { it.isRegularFile() && shouldIndexFile(it) }
            .toList()
    }

    /**
     * 判断是否应该索引文件
     *
     * @param filePath 文件路径
     * @return 是否应该索引
     */
    private fun shouldIndexFile(filePath: Path): Boolean {
        // 检查文件大小
        try {
            val size = Files.size(filePath)
            if (size > config.maxFileSizeBytes) {
                return false
            }
        } catch (e: Exception) {
            logger.warn(e) { "获取文件大小失败: $filePath" }
            return false
        }

        // 检查文件扩展名
        val extension = filePath.extension.lowercase()
        if (extension !in config.supportedExtensions) {
            return false
        }

        // 检查排除模式
        val pathString = filePath.toString()
        for (pattern in config.excludePatterns) {
            if (pattern.containsMatchIn(pathString)) {
                return false
            }
        }

        // 检查包含模式
        if (config.includePatterns.isNotEmpty()) {
            var included = false
            for (pattern in config.includePatterns) {
                if (pattern.containsMatchIn(pathString)) {
                    included = true
                    break
                }
            }
            if (!included) {
                return false
            }
        }

        return true
    }

    /**
     * 获取所有代码元素
     *
     * @return 代码元素集合
     */
    override suspend fun getAllElements(): Collection<CodeElement> {
        return indexedElements.values
    }

    /**
     * 获取代码元素流
     *
     * @return 代码元素流
     */
    override suspend fun getElementsFlow(): kotlinx.coroutines.flow.Flow<CodeElement> {
        return kotlinx.coroutines.flow.flow {
            indexedElements.values.forEach { emit(it) }
        }
    }

    /**
     * 根据类型获取代码元素
     *
     * @param type 元素类型
     * @return 代码元素集合
     */
    override suspend fun getElementsByType(type: String): Collection<CodeElement> {
        val elementType = try {
            CodeElementType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            return emptyList()
        }
        return typeToElements[elementType]?.toList() ?: emptyList()
    }

    /**
     * 根据ID获取代码元素
     *
     * @param id 元素ID
     * @return 代码元素，如果不存在则返回null
     */
    override suspend fun getElementById(id: String): CodeElement? {
        return indexedElements[id]
    }

    /**
     * 根据名称获取代码元素
     *
     * @param name 元素名称
     * @return 代码元素集合
     */
    override suspend fun getElementsByName(name: String): Collection<CodeElement> {
        return indexedElements.values.filter { it.name == name }
    }

    /**
     * 根据关键词搜索代码元素
     *
     * @param keywords 关键词
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 代码元素集合
     */
    override suspend fun searchByKeywords(keywords: List<String>, limit: Int, minScore: Float): List<Pair<CodeElement, Float>> {
        val results = mutableListOf<Pair<CodeElement, Float>>()

        for (element in indexedElements.values) {
            var score = 0f
            val elementText = element.toString().lowercase()

            for (keyword in keywords) {
                if (elementText.contains(keyword.lowercase())) {
                    score += 1f / keywords.size
                }
            }

            if (score >= minScore) {
                results.add(element to score)
            }
        }

        return results.sortedByDescending { it.second }.take(limit)
    }

    /**
     * 清除索引
     */
    override suspend fun clearIndex() {
        reset()
    }

    /**
     * 获取指定文件的所有元素
     *
     * @param filePath 文件路径
     * @return 指定文件的元素列表
     */
    fun getElementsByFilePath(filePath: Path): List<CodeElement> {
        return filePathToElements[filePath]?.toList() ?: emptyList()
    }

    /**
     * 获取指定类型的所有元素
     *
     * @param type 元素类型
     * @return 指定类型的元素列表
     */
    fun getElementsByType(type: CodeElementType): List<CodeElement> {
        return typeToElements[type]?.toList() ?: emptyList()
    }

    /**
     * 获取索引状态
     *
     * @return 索引状态信息
     */
    fun getIndexingStatus(): Map<String, Any> {
        return mapOf(
            "indexedFilesCount" to indexedFilesCount.get(),
            "indexedElementsCount" to indexedElementsCount.get(),
            "fileStatusCounts" to indexingStatus.values.groupingBy { it }.eachCount()
        )
    }

    /**
     * 重置索引
     */
    suspend fun reset() {
        indexedElements.clear()
        filePathToElements.clear()
        typeToElements.clear()
        indexingStatus.clear()
        indexedFilesCount.set(0)
        indexedElementsCount.set(0)
        vectorStore.clear()
    }

    /**
     * 关闭资源
     */
    override fun close() {
        embeddingService.close()
    }
}
