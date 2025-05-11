package ai.kastrax.codebase.semantic

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.parser.ChapiGoCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiKotlinCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiPythonCodeParser
import ai.kastrax.codebase.semantic.parser.ChapiTypeScriptCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

/**
 * 代码语义分析器配置
 *
 * @property excludePatterns 排除的文件模式
 * @property excludeDirectories 排除的目录
 * @property maxFileSizeBytes 最大文件大小（字节）
 * @property maxConcurrentFiles 最大并发文件数
 */
data class CodeSemanticAnalyzerConfig(
    val excludePatterns: Set<Regex> = setOf(
        Regex("\\.git/.*"),
        Regex("\\.idea/.*"),
        Regex("build/.*"),
        Regex("target/.*"),
        Regex("node_modules/.*"),
        Regex("\\.gradle/.*")
    ),
    val excludeDirectories: Set<String> = setOf(
        ".git", ".idea", "build", "target", "node_modules", ".gradle"
    ),
    val maxFileSizeBytes: Long = 1024 * 1024, // 1MB
    val maxConcurrentFiles: Int = 10
)

/**
 * 代码语义分析器
 *
 * 分析代码库中的所有文件，构建语义模型
 *
 * @property config 配置
 */
class CodeSemanticAnalyzer(
    private val config: CodeSemanticAnalyzerConfig = CodeSemanticAnalyzerConfig()
) {
    // 代码元素缓存
    private val elementCache = ConcurrentHashMap<String, CodeElement>()
    
    init {
        // 注册代码解析器
        registerParsers()
    }
    
    /**
     * 注册代码解析器
     */
    private fun registerParsers() {
        CodeParserFactory.registerParser(ChapiJavaCodeParser())
        CodeParserFactory.registerParser(ChapiKotlinCodeParser())
        CodeParserFactory.registerParser(ChapiPythonCodeParser())
        CodeParserFactory.registerParser(ChapiTypeScriptCodeParser())
        CodeParserFactory.registerParser(ChapiGoCodeParser())
    }
    
    /**
     * 分析代码库
     *
     * @param rootPath 代码库根路径
     * @return 代码库元素
     */
    suspend fun analyzeCodebase(rootPath: Path): CodeElement = withContext(Dispatchers.IO) {
        logger.info { "开始分析代码库: $rootPath" }
        
        // 创建代码库元素
        val codebaseElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = rootPath.fileName.toString(),
            qualifiedName = rootPath.toString(),
            type = CodeElementType.FILE,
            location = Location(
                filePath = rootPath,
                startLine = 1,
                startColumn = 1,
                endLine = 1,
                endColumn = 1
            )
        )
        
        // 查找所有代码文件
        val codeFiles = findCodeFiles(rootPath)
        logger.info { "找到 ${codeFiles.size} 个代码文件" }
        
        // 并行分析代码文件
        val fileElements = coroutineScope {
            codeFiles.chunked(config.maxConcurrentFiles).flatMap { chunk ->
                chunk.map { file ->
                    async {
                        analyzeFile(file)
                    }
                }.awaitAll()
            }
        }
        
        // 添加文件元素到代码库元素
        fileElements.forEach { fileElement ->
            codebaseElement.addChild(fileElement)
            
            // 缓存代码元素
            cacheElements(fileElement)
        }
        
        logger.info { "代码库分析完成: $rootPath" }
        
        return@withContext codebaseElement
    }
    
    /**
     * 分析代码文件
     *
     * @param filePath 文件路径
     * @return 文件元素
     */
    suspend fun analyzeFile(filePath: Path): CodeElement = withContext(Dispatchers.IO) {
        try {
            // 检查文件大小
            val fileSize = Files.size(filePath)
            if (fileSize > config.maxFileSizeBytes) {
                logger.warn { "文件过大，跳过分析: $filePath ($fileSize 字节)" }
                return@withContext createEmptyFileElement(filePath)
            }
            
            // 读取文件内容
            val content = filePath.readText()
            
            // 获取适合的解析器
            val parser = CodeParserFactory.getParser(filePath)
            
            if (parser != null) {
                // 解析文件
                val fileElement = parser.parseFile(filePath, content)
                logger.debug { "文件解析完成: $filePath" }
                return@withContext fileElement
            } else {
                logger.warn { "没有适合的解析器: $filePath" }
                return@withContext createEmptyFileElement(filePath)
            }
        } catch (e: Exception) {
            logger.error(e) { "分析文件时出错: $filePath" }
            return@withContext createEmptyFileElement(filePath)
        }
    }
    
    /**
     * 查找代码文件
     *
     * @param rootPath 根路径
     * @return 代码文件列表
     */
    private fun findCodeFiles(rootPath: Path): List<Path> {
        return Files.walk(rootPath)
            .filter { it.isRegularFile() }
            .filter { isCodeFile(it) }
            .filter { !isExcluded(it, rootPath) }
            .toList()
    }
    
    /**
     * 检查是否为代码文件
     *
     * @param filePath 文件路径
     * @return 是否为代码文件
     */
    private fun isCodeFile(filePath: Path): Boolean {
        val extension = filePath.extension.lowercase()
        
        // 检查是否有支持的解析器
        val parser = CodeParserFactory.getParser(filePath)
        return parser != null
    }
    
    /**
     * 检查是否排除文件
     *
     * @param filePath 文件路径
     * @param rootPath 根路径
     * @return 是否排除
     */
    private fun isExcluded(filePath: Path, rootPath: Path): Boolean {
        val relativePath = rootPath.relativize(filePath).toString()
        
        // 检查排除模式
        for (pattern in config.excludePatterns) {
            if (pattern.matches(relativePath)) {
                return true
            }
        }
        
        // 检查排除目录
        for (dir in config.excludeDirectories) {
            if (relativePath.startsWith("$dir/") || relativePath == dir) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 创建空文件元素
     *
     * @param filePath 文件路径
     * @return 文件元素
     */
    private fun createEmptyFileElement(filePath: Path): CodeElement {
        return CodeElement(
            id = UUID.randomUUID().toString(),
            name = filePath.fileName.toString(),
            qualifiedName = filePath.toString(),
            type = CodeElementType.FILE,
            location = Location(
                filePath = filePath,
                startLine = 1,
                startColumn = 1,
                endLine = 1,
                endColumn = 1
            )
        )
    }
    
    /**
     * 缓存代码元素
     *
     * @param element 代码元素
     */
    private fun cacheElements(element: CodeElement) {
        // 缓存当前元素
        elementCache[element.id] = element
        
        // 递归缓存子元素
        element.children.forEach { child ->
            cacheElements(child)
        }
    }
    
    /**
     * 根据 ID 获取代码元素
     *
     * @param id 元素 ID
     * @return 代码元素，如果不存在则返回 null
     */
    fun getElementById(id: String): CodeElement? {
        return elementCache[id]
    }
    
    /**
     * 根据位置获取代码元素
     *
     * @param location 位置
     * @return 代码元素，如果不存在则返回 null
     */
    fun getElementAtLocation(location: Location): CodeElement? {
        // 首先找到文件元素
        val fileElements = elementCache.values.filter { 
            it.type == CodeElementType.FILE && it.location.filePath == location.filePath 
        }
        
        if (fileElements.isEmpty()) {
            return null
        }
        
        // 在文件元素中查找包含指定位置的最深元素
        return fileElements.first().getDeepestElementAtPosition(location)
    }
    
    /**
     * 根据名称查找代码元素
     *
     * @param name 名称
     * @param type 元素类型，如果为 null 则不限制类型
     * @return 代码元素列表
     */
    fun findElementsByName(name: String, type: CodeElementType? = null): List<CodeElement> {
        return elementCache.values.filter { 
            it.name == name && (type == null || it.type == type)
        }
    }
    
    /**
     * 根据限定名查找代码元素
     *
     * @param qualifiedName 限定名
     * @param type 元素类型，如果为 null 则不限制类型
     * @return 代码元素，如果不存在则返回 null
     */
    fun findElementByQualifiedName(qualifiedName: String, type: CodeElementType? = null): CodeElement? {
        return elementCache.values.find { 
            it.qualifiedName == qualifiedName && (type == null || it.type == type)
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        elementCache.clear()
    }
}
