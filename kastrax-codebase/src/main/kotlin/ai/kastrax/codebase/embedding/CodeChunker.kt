package ai.kastrax.codebase.embedding

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

/**
 * 代码块
 *
 * @property content 内容
 * @property metadata 元数据
 */
data class CodeChunk(
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 代码分块配置
 *
 * @property maxChunkSize 最大块大小（字符数）
 * @property minChunkSize 最小块大小（字符数）
 * @property overlap 重叠大小（字符数）
 * @property preserveSemantics 是否保留语义
 */
data class CodeChunkerConfig(
    val maxChunkSize: Int = 1000,
    val minChunkSize: Int = 100,
    val overlap: Int = 50,
    val preserveSemantics: Boolean = true
)

/**
 * 代码分块器
 *
 * 将代码文件分割成语义完整的块
 *
 * @property config 配置
 */
class CodeChunker(private val config: CodeChunkerConfig = CodeChunkerConfig()) {
    
    /**
     * 分割代码文件
     *
     * @param path 文件路径
     * @return 代码块列表
     */
    fun chunkFile(path: Path): List<CodeChunk> {
        try {
            // 读取文件内容
            val content = path.readText()
            
            // 获取文件类型
            val fileType = getFileType(path)
            
            // 分割代码
            val chunks = chunkCode(content, fileType)
            
            // 添加元数据
            return chunks.mapIndexed { index, chunk ->
                CodeChunk(
                    content = chunk,
                    metadata = mapOf(
                        "path" to path.toString(),
                        "chunk_index" to index,
                        "total_chunks" to chunks.size,
                        "file_type" to fileType
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "分割代码文件时出错: $path" }
            return emptyList()
        }
    }
    
    /**
     * 分割代码文本
     *
     * @param code 代码文本
     * @param fileType 文件类型
     * @return 代码块列表
     */
    fun chunkCode(code: String, fileType: String): List<String> {
        return if (config.preserveSemantics) {
            // 使用语义感知的分块策略
            semanticChunking(code, fileType)
        } else {
            // 使用简单的分块策略
            simpleChunking(code)
        }
    }
    
    /**
     * 语义感知的分块策略
     *
     * @param code 代码文本
     * @param fileType 文件类型
     * @return 代码块列表
     */
    private fun semanticChunking(code: String, fileType: String): List<String> {
        // 根据文件类型选择不同的分块策略
        return when (fileType) {
            "java", "kotlin", "scala" -> chunkJvmCode(code)
            "python" -> chunkPythonCode(code)
            "javascript", "typescript" -> chunkJsCode(code)
            else -> simpleChunking(code)
        }
    }
    
    /**
     * 分割 JVM 语言代码（Java、Kotlin、Scala）
     *
     * @param code 代码文本
     * @return 代码块列表
     */
    private fun chunkJvmCode(code: String): List<String> {
        val chunks = mutableListOf<String>()
        
        // 使用正则表达式匹配类、接口、方法等
        val classPattern = Regex("(public|private|protected|\\s)\\s*(class|interface|enum)\\s+[\\w<>]+\\s*(extends|implements)?\\s*[\\w<>,\\s]*\\{")
        val methodPattern = Regex("(public|private|protected|\\s)\\s*(static)?\\s*[\\w<>\\[\\]]+\\s+[\\w]+\\s*\\([^)]*\\)\\s*(throws\\s+[\\w,\\s]+)?\\s*\\{")
        
        // 查找所有类定义
        val classMatches = classPattern.findAll(code)
        
        if (classMatches.count() > 0) {
            // 如果找到类定义，按类分块
            var lastEnd = 0
            
            for (classMatch in classMatches) {
                val classStart = classMatch.range.first
                
                // 如果类之前有内容，添加为一个块
                if (classStart > lastEnd && classStart - lastEnd > config.minChunkSize) {
                    chunks.add(code.substring(lastEnd, classStart))
                }
                
                // 查找类的结束位置（匹配花括号）
                val classEnd = findMatchingBrace(code, classMatch.range.last)
                
                if (classEnd > classStart) {
                    val classCode = code.substring(classStart, classEnd + 1)
                    
                    // 如果类太大，进一步分割
                    if (classCode.length > config.maxChunkSize) {
                        // 查找类中的方法
                        val methodMatches = methodPattern.findAll(classCode)
                        
                        if (methodMatches.count() > 0) {
                            // 如果找到方法，按方法分块
                            var lastMethodEnd = 0
                            
                            for (methodMatch in methodMatches) {
                                val methodStart = methodMatch.range.first
                                
                                // 如果方法之前有内容，添加为一个块
                                if (methodStart > lastMethodEnd && methodStart - lastMethodEnd > config.minChunkSize) {
                                    chunks.add(classCode.substring(lastMethodEnd, methodStart))
                                }
                                
                                // 查找方法的结束位置（匹配花括号）
                                val methodEnd = findMatchingBrace(classCode, methodMatch.range.last)
                                
                                if (methodEnd > methodStart) {
                                    val methodCode = classCode.substring(methodStart, methodEnd + 1)
                                    chunks.add(methodCode)
                                    lastMethodEnd = methodEnd + 1
                                }
                            }
                            
                            // 添加类中剩余的内容
                            if (lastMethodEnd < classCode.length) {
                                chunks.add(classCode.substring(lastMethodEnd))
                            }
                        } else {
                            // 如果没有找到方法，使用简单分块
                            chunks.addAll(simpleChunking(classCode))
                        }
                    } else {
                        // 如果类不太大，作为一个块
                        chunks.add(classCode)
                    }
                    
                    lastEnd = classEnd + 1
                }
            }
            
            // 添加剩余的内容
            if (lastEnd < code.length) {
                chunks.add(code.substring(lastEnd))
            }
        } else {
            // 如果没有找到类定义，尝试按方法分块
            val methodMatches = methodPattern.findAll(code)
            
            if (methodMatches.count() > 0) {
                var lastEnd = 0
                
                for (methodMatch in methodMatches) {
                    val methodStart = methodMatch.range.first
                    
                    // 如果方法之前有内容，添加为一个块
                    if (methodStart > lastEnd && methodStart - lastEnd > config.minChunkSize) {
                        chunks.add(code.substring(lastEnd, methodStart))
                    }
                    
                    // 查找方法的结束位置（匹配花括号）
                    val methodEnd = findMatchingBrace(code, methodMatch.range.last)
                    
                    if (methodEnd > methodStart) {
                        val methodCode = code.substring(methodStart, methodEnd + 1)
                        chunks.add(methodCode)
                        lastEnd = methodEnd + 1
                    }
                }
                
                // 添加剩余的内容
                if (lastEnd < code.length) {
                    chunks.add(code.substring(lastEnd))
                }
            } else {
                // 如果没有找到方法，使用简单分块
                chunks.addAll(simpleChunking(code))
            }
        }
        
        return chunks
    }
    
    /**
     * 分割 Python 代码
     *
     * @param code 代码文本
     * @return 代码块列表
     */
    private fun chunkPythonCode(code: String): List<String> {
        val chunks = mutableListOf<String>()
        
        // 使用正则表达式匹配类和函数
        val classPattern = Regex("class\\s+[\\w]+\\s*(\\([^)]*\\))?\\s*:")
        val functionPattern = Regex("def\\s+[\\w]+\\s*\\([^)]*\\)\\s*(->\\s*[\\w\\[\\],\\s]+)?\\s*:")
        
        // 查找所有类定义
        val classMatches = classPattern.findAll(code)
        
        if (classMatches.count() > 0) {
            // 如果找到类定义，按类分块
            var lastEnd = 0
            
            for (classMatch in classMatches) {
                val classStart = classMatch.range.first
                
                // 如果类之前有内容，添加为一个块
                if (classStart > lastEnd && classStart - lastEnd > config.minChunkSize) {
                    chunks.add(code.substring(lastEnd, classStart))
                }
                
                // 查找类的结束位置（下一个非缩进行）
                val classEnd = findPythonBlockEnd(code, classMatch.range.last)
                
                if (classEnd > classStart) {
                    val classCode = code.substring(classStart, classEnd)
                    
                    // 如果类太大，进一步分割
                    if (classCode.length > config.maxChunkSize) {
                        // 查找类中的函数
                        val functionMatches = functionPattern.findAll(classCode)
                        
                        if (functionMatches.count() > 0) {
                            // 如果找到函数，按函数分块
                            var lastFunctionEnd = 0
                            
                            for (functionMatch in functionMatches) {
                                val functionStart = functionMatch.range.first
                                
                                // 如果函数之前有内容，添加为一个块
                                if (functionStart > lastFunctionEnd && functionStart - lastFunctionEnd > config.minChunkSize) {
                                    chunks.add(classCode.substring(lastFunctionEnd, functionStart))
                                }
                                
                                // 查找函数的结束位置（下一个非缩进行）
                                val functionEnd = findPythonBlockEnd(classCode, functionMatch.range.last)
                                
                                if (functionEnd > functionStart) {
                                    val functionCode = classCode.substring(functionStart, functionEnd)
                                    chunks.add(functionCode)
                                    lastFunctionEnd = functionEnd
                                }
                            }
                            
                            // 添加类中剩余的内容
                            if (lastFunctionEnd < classCode.length) {
                                chunks.add(classCode.substring(lastFunctionEnd))
                            }
                        } else {
                            // 如果没有找到函数，使用简单分块
                            chunks.addAll(simpleChunking(classCode))
                        }
                    } else {
                        // 如果类不太大，作为一个块
                        chunks.add(classCode)
                    }
                    
                    lastEnd = classEnd
                }
            }
            
            // 添加剩余的内容
            if (lastEnd < code.length) {
                chunks.add(code.substring(lastEnd))
            }
        } else {
            // 如果没有找到类定义，尝试按函数分块
            val functionMatches = functionPattern.findAll(code)
            
            if (functionMatches.count() > 0) {
                var lastEnd = 0
                
                for (functionMatch in functionMatches) {
                    val functionStart = functionMatch.range.first
                    
                    // 如果函数之前有内容，添加为一个块
                    if (functionStart > lastEnd && functionStart - lastEnd > config.minChunkSize) {
                        chunks.add(code.substring(lastEnd, functionStart))
                    }
                    
                    // 查找函数的结束位置（下一个非缩进行）
                    val functionEnd = findPythonBlockEnd(code, functionMatch.range.last)
                    
                    if (functionEnd > functionStart) {
                        val functionCode = code.substring(functionStart, functionEnd)
                        chunks.add(functionCode)
                        lastEnd = functionEnd
                    }
                }
                
                // 添加剩余的内容
                if (lastEnd < code.length) {
                    chunks.add(code.substring(lastEnd))
                }
            } else {
                // 如果没有找到函数，使用简单分块
                chunks.addAll(simpleChunking(code))
            }
        }
        
        return chunks
    }
    
    /**
     * 分割 JavaScript/TypeScript 代码
     *
     * @param code 代码文本
     * @return 代码块列表
     */
    private fun chunkJsCode(code: String): List<String> {
        val chunks = mutableListOf<String>()
        
        // 使用正则表达式匹配类、函数等
        val classPattern = Regex("class\\s+[\\w]+\\s*(extends\\s+[\\w]+)?\\s*\\{")
        val functionPattern = Regex("(function\\s+[\\w]+\\s*\\([^)]*\\)|const\\s+[\\w]+\\s*=\\s*\\([^)]*\\)\\s*=>|[\\w]+\\s*\\([^)]*\\)\\s*\\{)")
        
        // 查找所有类定义
        val classMatches = classPattern.findAll(code)
        
        if (classMatches.count() > 0) {
            // 如果找到类定义，按类分块
            var lastEnd = 0
            
            for (classMatch in classMatches) {
                val classStart = classMatch.range.first
                
                // 如果类之前有内容，添加为一个块
                if (classStart > lastEnd && classStart - lastEnd > config.minChunkSize) {
                    chunks.add(code.substring(lastEnd, classStart))
                }
                
                // 查找类的结束位置（匹配花括号）
                val classEnd = findMatchingBrace(code, classMatch.range.last)
                
                if (classEnd > classStart) {
                    val classCode = code.substring(classStart, classEnd + 1)
                    
                    // 如果类太大，进一步分割
                    if (classCode.length > config.maxChunkSize) {
                        // 查找类中的方法
                        val methodMatches = functionPattern.findAll(classCode)
                        
                        if (methodMatches.count() > 0) {
                            // 如果找到方法，按方法分块
                            var lastMethodEnd = 0
                            
                            for (methodMatch in methodMatches) {
                                val methodStart = methodMatch.range.first
                                
                                // 如果方法之前有内容，添加为一个块
                                if (methodStart > lastMethodEnd && methodStart - lastMethodEnd > config.minChunkSize) {
                                    chunks.add(classCode.substring(lastMethodEnd, methodStart))
                                }
                                
                                // 查找方法的结束位置（匹配花括号）
                                val methodEnd = findMatchingBrace(classCode, methodMatch.range.last)
                                
                                if (methodEnd > methodStart) {
                                    val methodCode = classCode.substring(methodStart, methodEnd + 1)
                                    chunks.add(methodCode)
                                    lastMethodEnd = methodEnd + 1
                                }
                            }
                            
                            // 添加类中剩余的内容
                            if (lastMethodEnd < classCode.length) {
                                chunks.add(classCode.substring(lastMethodEnd))
                            }
                        } else {
                            // 如果没有找到方法，使用简单分块
                            chunks.addAll(simpleChunking(classCode))
                        }
                    } else {
                        // 如果类不太大，作为一个块
                        chunks.add(classCode)
                    }
                    
                    lastEnd = classEnd + 1
                }
            }
            
            // 添加剩余的内容
            if (lastEnd < code.length) {
                chunks.add(code.substring(lastEnd))
            }
        } else {
            // 如果没有找到类定义，尝试按函数分块
            val functionMatches = functionPattern.findAll(code)
            
            if (functionMatches.count() > 0) {
                var lastEnd = 0
                
                for (functionMatch in functionMatches) {
                    val functionStart = functionMatch.range.first
                    
                    // 如果函数之前有内容，添加为一个块
                    if (functionStart > lastEnd && functionStart - lastEnd > config.minChunkSize) {
                        chunks.add(code.substring(lastEnd, functionStart))
                    }
                    
                    // 查找函数的结束位置（匹配花括号）
                    val functionEnd = findMatchingBrace(code, functionMatch.range.last)
                    
                    if (functionEnd > functionStart) {
                        val functionCode = code.substring(functionStart, functionEnd + 1)
                        chunks.add(functionCode)
                        lastEnd = functionEnd + 1
                    }
                }
                
                // 添加剩余的内容
                if (lastEnd < code.length) {
                    chunks.add(code.substring(lastEnd))
                }
            } else {
                // 如果没有找到函数，使用简单分块
                chunks.addAll(simpleChunking(code))
            }
        }
        
        return chunks
    }
    
    /**
     * 简单的分块策略
     *
     * @param code 代码文本
     * @return 代码块列表
     */
    private fun simpleChunking(code: String): List<String> {
        val chunks = mutableListOf<String>()
        
        // 按行分割代码
        val lines = code.lines()
        
        var currentChunk = StringBuilder()
        var currentSize = 0
        
        for (line in lines) {
            // 如果当前块加上新行超过最大块大小，并且当前块大小已经达到最小块大小，则开始新块
            if (currentSize + line.length > config.maxChunkSize && currentSize >= config.minChunkSize) {
                chunks.add(currentChunk.toString())
                
                // 如果启用了重叠，保留一部分内容
                if (config.overlap > 0) {
                    val overlapText = currentChunk.takeLast(config.overlap).toString()
                    currentChunk = StringBuilder(overlapText)
                    currentSize = overlapText.length
                } else {
                    currentChunk = StringBuilder()
                    currentSize = 0
                }
            }
            
            // 添加新行到当前块
            currentChunk.append(line).append("\n")
            currentSize += line.length + 1
        }
        
        // 添加最后一个块
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        return chunks
    }
    
    /**
     * 查找匹配的花括号
     *
     * @param code 代码文本
     * @param openBraceIndex 开括号索引
     * @return 闭括号索引，如果没有找到则返回 -1
     */
    private fun findMatchingBrace(code: String, openBraceIndex: Int): Int {
        var braceCount = 1
        var i = openBraceIndex + 1
        
        while (i < code.length) {
            when (code[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        return i
                    }
                }
            }
            i++
        }
        
        return -1
    }
    
    /**
     * 查找 Python 代码块的结束位置
     *
     * @param code 代码文本
     * @param blockStart 代码块开始索引
     * @return 代码块结束索引
     */
    private fun findPythonBlockEnd(code: String, blockStart: Int): Int {
        // 查找冒号后的第一个换行符
        var i = blockStart
        while (i < code.length && code[i] != '\n') {
            i++
        }
        
        if (i >= code.length) {
            return code.length
        }
        
        // 跳过换行符
        i++
        
        // 获取缩进级别
        var indentLevel = 0
        var j = i
        while (j < code.length && (code[j] == ' ' || code[j] == '\t')) {
            indentLevel++
            j++
        }
        
        // 查找下一个缩进级别小于或等于当前块的行
        var lineStart = i
        
        while (i < code.length) {
            if (code[i] == '\n') {
                // 检查下一行的缩进级别
                var nextIndent = 0
                j = i + 1
                
                // 跳过空行
                var isEmptyLine = true
                var k = j
                while (k < code.length && code[k] != '\n') {
                    if (code[k] != ' ' && code[k] != '\t') {
                        isEmptyLine = false
                        break
                    }
                    k++
                }
                
                if (!isEmptyLine) {
                    while (j < code.length && (code[j] == ' ' || code[j] == '\t')) {
                        nextIndent++
                        j++
                    }
                    
                    // 如果下一行的缩进级别小于或等于当前块的缩进级别，则找到块的结束位置
                    if (nextIndent <= indentLevel && j < code.length && code[j] != '\n') {
                        return i
                    }
                }
                
                lineStart = i + 1
            }
            
            i++
        }
        
        return code.length
    }
    
    /**
     * 获取文件类型
     *
     * @param path 文件路径
     * @return 文件类型
     */
    private fun getFileType(path: Path): String {
        val extension = path.extension.lowercase()
        
        return when (extension) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "scala" -> "scala"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "jsx" -> "jsx"
            "tsx" -> "tsx"
            "html" -> "html"
            "css" -> "css"
            "c", "cpp", "cc" -> "cpp"
            "h", "hpp" -> "cpp_header"
            "cs" -> "csharp"
            "go" -> "go"
            "rs" -> "rust"
            "php" -> "php"
            "rb" -> "ruby"
            "swift" -> "swift"
            "m", "mm" -> "objective_c"
            "sh" -> "shell"
            "bat", "cmd" -> "batch"
            "ps1" -> "powershell"
            "sql" -> "sql"
            "xml" -> "xml"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "md" -> "markdown"
            else -> "text"
        }
    }
}
