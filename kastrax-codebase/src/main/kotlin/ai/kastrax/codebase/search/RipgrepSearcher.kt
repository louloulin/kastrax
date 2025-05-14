package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Ripgrep 搜索器配置
 *
 * @property contextLines 上下文行数
 * @property ignoreCase 是否忽略大小写
 * @property caseSensitive 是否大小写敏感
 * @property wordMatch 是否全词匹配
 * @property useRegex 是否使用正则表达式
 * @property followSymlinks 是否跟随符号链接
 * @property maxFileSize 最大文件大小（字节）
 * @property timeout 超时时间（秒）
 * @property defaultTimeout 默认超时时间（秒）
 * @property excludePatterns 排除模式列表
 * @property includePatterns 包含模式列表
 * @property fileTypes 文件类型列表
 * @property excludeFileTypes 排除文件类型列表
 * @property respectGitignore 是否尊重 .gitignore
 * @property hiddenFiles 是否搜索隐藏文件
 * @property binaryFiles 是否搜索二进制文件
 * @property excludeBinary 是否排除二进制文件
 * @property multiline 是否支持多行匹配
 * @property maxDepth 最大搜索深度
 * @property maxCount 最大计数
 * @property maxMatches 最大匹配数
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 */
data class RipgrepConfig(
    val contextLines: Int = 2,
    val ignoreCase: Boolean = false,
    val caseSensitive: Boolean = true,
    val wordMatch: Boolean = false,
    val useRegex: Boolean = true,
    val followSymlinks: Boolean = false,
    val maxFileSize: Long = 1024 * 1024, // 1MB
    val timeout: Long = 30, // 30 seconds
    val defaultTimeout: Long = 30, // 30 seconds
    val excludePatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList(),
    val fileTypes: List<String> = emptyList(),
    val excludeFileTypes: List<String> = emptyList(),
    val respectGitignore: Boolean = true,
    val hiddenFiles: Boolean = false,
    val binaryFiles: Boolean = false,
    val excludeBinary: Boolean = true,
    val multiline: Boolean = false,
    val maxDepth: Int = 0,
    val maxCount: Int = 0,
    val maxMatches: Int = 0,
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100
)

/**
 * Ripgrep 搜索结果
 *
 * @property filePath 文件路径
 * @property lineNumber 行号
 * @property columnNumber 列号
 * @property matchText 匹配文本
 * @property lineText 行文本
 * @property beforeContext 前置上下文
 * @property afterContext 后置上下文
 * @property submatches 子匹配列表
 */
data class RipgrepSearchResult(
    val filePath: String,
    val lineNumber: Int,
    val columnNumber: Int,
    val matchText: String,
    val lineText: String,
    val beforeContext: List<String> = emptyList(),
    val afterContext: List<String> = emptyList(),
    val submatches: List<RipgrepSubmatch> = emptyList()
)

/**
 * Ripgrep 子匹配
 *
 * @property text 匹配文本
 * @property start 开始位置
 * @property end 结束位置
 */
data class RipgrepSubmatch(
    val text: String,
    val start: Int,
    val end: Int
)

/**
 * Ripgrep 搜索异常
 *
 * @property message 异常消息
 * @property cause 异常原因
 */
class RipgrepSearchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Ripgrep 搜索器
 *
 * 使用 ripgrep 进行高性能代码搜索
 *
 * @property config 配置
 */
class RipgrepSearcher(
    private val config: RipgrepConfig = RipgrepConfig()
) {
    /**
     * 搜索代码
     *
     * @param query 查询字符串
     * @param paths 搜索路径列表
     * @param options 搜索选项
     * @return 搜索结果流
     */
    suspend fun search(
        query: String,
        paths: List<Path>,
        options: Map<String, Any> = emptyMap()
    ): Flow<RipgrepSearchResult> = flow {
        logger.info { "开始 ripgrep 搜索: $query" }

        // 查找 ripgrep 二进制文件
        val rgPath = findRipgrepBinary() ?: throw RipgrepSearchException("Ripgrep 二进制文件未找到，请先安装: https://github.com/BurntSushi/ripgrep#installation")

        // 构建命令
        val command = buildCommand(rgPath, query, paths, options)
        logger.debug { "执行命令: ${command.joinToString(" ")}" }

        try {
            // 执行命令
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            // 设置超时
            val timeout = options["timeout"] as? Long ?: config.timeout

            // 处理输出
            val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
            val processor = RipgrepOutputProcessor()

            // 读取输出
            reader.useLines { lines ->
                lines.forEach { line ->
                    processor.processLine(line)?.let { result ->
                        emit(result)
                    }
                }
            }

            // 等待进程完成
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                throw RipgrepSearchException("搜索超时（${timeout}秒）")
            }

            // 检查退出码
            val exitCode = process.exitValue()
            if (exitCode != 0 && exitCode != 1) { // 1 表示没有匹配
                throw RipgrepSearchException("Ripgrep 搜索失败，退出码: $exitCode")
            }

            logger.info { "Ripgrep 搜索完成: $query" }
        } catch (e: Exception) {
            if (e is RipgrepSearchException) throw e
            throw RipgrepSearchException("Ripgrep 搜索失败: ${e.message}", e)
        }
    }

    /**
     * 构建 ripgrep 命令
     *
     * @param rgPath ripgrep 路径
     * @param query 查询字符串
     * @param paths 搜索路径列表
     * @param options 搜索选项
     * @return 命令列表
     */
    private fun buildCommand(
        rgPath: Path,
        query: String,
        paths: List<Path>,
        options: Map<String, Any>
    ): List<String> {
        val command = mutableListOf(rgPath.toString())

        // 添加 JSON 输出格式
        command.add("--json")

        // 上下文行数
        val contextLines = options["contextLines"] as? Int ?: config.contextLines
        if (contextLines > 0) {
            command.add("--context")
            command.add(contextLines.toString())
        }

        // 大小写敏感
        val ignoreCase = options["ignoreCase"] as? Boolean ?: config.ignoreCase
        if (ignoreCase) {
            command.add("--ignore-case")
        }

        // 全词匹配
        val wordMatch = options["wordMatch"] as? Boolean ?: config.wordMatch
        if (wordMatch) {
            command.add("--word-regexp")
        }

        // 正则表达式
        val useRegex = options["useRegex"] as? Boolean ?: config.useRegex
        if (useRegex) {
            command.add("--regexp")
        } else {
            command.add("--fixed-strings")
        }
        command.add(query)

        // 跟随符号链接
        val followSymlinks = options["followSymlinks"] as? Boolean ?: config.followSymlinks
        if (followSymlinks) {
            command.add("--follow")
        }

        // 最大文件大小
        val maxFileSize = options["maxFileSize"] as? Long ?: config.maxFileSize
        if (maxFileSize > 0) {
            command.add("--max-filesize")
            command.add("${maxFileSize}b")
        }

        // 排除模式
        val excludePatterns = options["excludePatterns"] as? List<String> ?: config.excludePatterns
        excludePatterns.forEach { pattern ->
            command.add("--glob")
            command.add("!$pattern")
        }

        // 包含模式
        val includePatterns = options["includePatterns"] as? List<String> ?: config.includePatterns
        includePatterns.forEach { pattern ->
            command.add("--glob")
            command.add(pattern)
        }

        // 文件类型
        val fileTypes = options["fileTypes"] as? List<String> ?: config.fileTypes
        fileTypes.forEach { type ->
            command.add("--type")
            command.add(type)
        }

        // 排除文件类型
        val excludeFileTypes = options["excludeFileTypes"] as? List<String> ?: config.excludeFileTypes
        excludeFileTypes.forEach { type ->
            command.add("--type-not")
            command.add(type)
        }

        // 尊重 .gitignore
        val respectGitignore = options["respectGitignore"] as? Boolean ?: config.respectGitignore
        if (!respectGitignore) {
            command.add("--no-ignore")
        }

        // 隐藏文件
        val hiddenFiles = options["hiddenFiles"] as? Boolean ?: config.hiddenFiles
        if (hiddenFiles) {
            command.add("--hidden")
        }

        // 二进制文件
        val binaryFiles = options["binaryFiles"] as? Boolean ?: config.binaryFiles
        if (!binaryFiles) {
            command.add("--no-binary")
        }

        // 添加搜索路径
        paths.forEach { path ->
            command.add(path.toString())
        }

        return command
    }

    /**
     * 搜索文件
     *
     * @param query 查询字符串
     * @param basePath 基础路径
     * @param filePattern 文件模式
     * @param options 搜索选项
     * @return 搜索结果字符串
     */
    suspend fun searchFiles(
        query: String,
        basePath: Path,
        filePattern: String? = null,
        options: Map<String, Any> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        logger.info { "搜索文件: $query, 模式: $filePattern" }

        try {
            // 查找 ripgrep 二进制文件
            val rgPath = findRipgrepBinary() ?: throw RipgrepSearchException("Ripgrep 二进制文件未找到，请先安装: https://github.com/BurntSushi/ripgrep#installation")

            // 构建命令行
            val cmd = mutableListOf<String>()
            cmd.add(rgPath.toString())

            // 添加查询
            cmd.add("-e")
            cmd.add(query)

            // 添加文件模式
            if (filePattern != null) {
                cmd.add("--glob")
                cmd.add(filePattern)
            }

            // 添加上下文行数
            val contextLines = options["contextLines"] as? Int ?: config.contextLines
            cmd.add("--context")
            cmd.add(contextLines.toString())

            // 添加大小写敏感选项
            val caseSensitive = options["caseSensitive"] as? Boolean ?: config.caseSensitive
            if (!caseSensitive) {
                cmd.add("-i")
            }

            // 添加多行匹配选项
            val multiline = options["multiline"] as? Boolean ?: config.multiline
            if (multiline) {
                cmd.add("-U")
            }

            // 添加全词匹配选项
            val wordMatch = options["wordMatch"] as? Boolean ?: config.wordMatch
            if (wordMatch) {
                cmd.add("-w")
            }

            // 添加隐藏文件选项
            val hiddenFiles = options["hiddenFiles"] as? Boolean ?: config.hiddenFiles
            if (hiddenFiles) {
                cmd.add("--hidden")
            }

            // 添加尊重 .gitignore 选项
            val respectGitignore = options["respectGitignore"] as? Boolean ?: config.respectGitignore
            if (!respectGitignore) {
                cmd.add("--no-ignore")
            }

            // 添加排除二进制文件选项
            val excludeBinary = options["excludeBinary"] as? Boolean ?: config.excludeBinary
            if (excludeBinary) {
                cmd.add("--text")
            }

            // 添加搜索路径
            cmd.add(basePath.toString())

            // 执行命令
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.directory(basePath.toFile())
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            // 设置超时
            val timeout = options["timeout"] as? Long ?: config.defaultTimeout
            val completed = process.waitFor(timeout, TimeUnit.SECONDS)

            if (!completed) {
                process.destroy()
                throw RipgrepSearchException("搜索超时")
            }

            // 读取输出
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()

            // 格式化结果
            return@withContext formatSearchResults(output, query, basePath, options)
        } catch (e: Exception) {
            logger.error(e) { "搜索文件失败: ${e.message}" }
            throw RipgrepSearchException("搜索文件失败: ${e.message}")
        }
    }

    /**
     * 格式化搜索结果
     *
     * @param output 输出
     * @param query 查询字符串
     * @param basePath 基础路径
     * @param options 搜索选项
     * @return 格式化后的结果
     */
    private fun formatSearchResults(
        output: String,
        query: String,
        basePath: Path,
        options: Map<String, Any> = emptyMap()
    ): String {
        if (output.isBlank()) {
            return "未找到匹配的结果。"
        }

        val formatAsMarkdown = options["formatAsMarkdown"] as? Boolean ?: true
        val contextLines = options["contextLines"] as? Int ?: config.contextLines

        // 解析输出
        val lines = output.lines()
        val results = mutableMapOf<String, MutableList<Pair<Int, String>>>()

        var currentFile: String? = null
        var lineNumber = 0

        for (line in lines) {
            if (line.startsWith(basePath.toString())) {
                // 文件行
                val parts = line.split(":", limit = 2)
                if (parts.size >= 2) {
                    currentFile = parts[0]
                    lineNumber = parts[1].toIntOrNull() ?: 0
                    val content = if (parts.size > 2) parts[2] else ""
                    results.computeIfAbsent(currentFile) { mutableListOf() }.add(lineNumber to content)
                }
            } else if (currentFile != null && line.isNotBlank()) {
                // 内容行
                results[currentFile]?.add(lineNumber to line)
            }
        }

        // 格式化结果
        val sb = StringBuilder()

        if (formatAsMarkdown) {
            sb.appendLine("搜索结果: $query")
            sb.appendLine("找到 ${results.size} 个匹配文件")
            sb.appendLine()

            for ((file, matches) in results) {
                val relativePath = basePath.relativize(Paths.get(file)).toString()
                sb.appendLine("### $relativePath")
                sb.appendLine()
                sb.appendLine("```")

                // 添加匹配行及上下文
                val fileContent = try {
                    Files.readAllLines(Paths.get(file))
                } catch (e: Exception) {
                    emptyList()
                }

                val displayLines = mutableSetOf<Int>()
                for ((lineNum, _) in matches) {
                    val start = maxOf(1, lineNum - contextLines)
                    val end = minOf(fileContent.size, lineNum + contextLines)
                    for (i in start..end) {
                        displayLines.add(i)
                    }
                }

                for (lineNum in displayLines.sorted()) {
                    val content = fileContent.getOrNull(lineNum - 1) ?: continue
                    val isMatch = matches.any { it.first == lineNum }
                    if (isMatch) {
                        sb.appendLine("$lineNum: $content  <- 匹配")
                    } else {
                        sb.appendLine("$lineNum: $content")
                    }
                }

                sb.appendLine("```")
                sb.appendLine()
            }
        } else {
            sb.appendLine("搜索结果: $query")
            sb.appendLine("找到 ${results.size} 个匹配文件")
            sb.appendLine()

            for ((file, matches) in results) {
                val relativePath = basePath.relativize(Paths.get(file)).toString()
                sb.appendLine(relativePath)

                // 添加匹配行
                for ((lineNum, content) in matches) {
                    sb.appendLine("$lineNum: $content")
                }

                sb.appendLine()
            }
        }

        return sb.toString()
    }

    /**
     * 查找 ripgrep 二进制文件
     *
     * @return ripgrep 路径，如果未找到则返回 null
     */
    private fun findRipgrepBinary(): Path? {
        val osName = System.getProperty("os.name").lowercase()
        val binName = if (osName.contains("win")) "rg.exe" else "rg"

        // 检查环境变量
        val pathEnv = System.getenv("PATH") ?: return null
        val pathSeparator = if (osName.contains("win")) ";" else ":"

        for (dir in pathEnv.split(pathSeparator)) {
            val path = Paths.get(dir, binName)
            if (Files.exists(path) && Files.isExecutable(path)) {
                return path
            }
        }

        // 检查常见安装位置
        val commonPaths = when {
            osName.contains("mac") -> listOf(
                Paths.get("/usr/local/bin/rg"),
                Paths.get("/opt/homebrew/bin/rg")
            )
            osName.contains("win") -> listOf(
                Paths.get(System.getenv("ProgramFiles"), "ripgrep", binName),
                Paths.get(System.getenv("ProgramFiles(x86)"), "ripgrep", binName),
                Paths.get(System.getenv("USERPROFILE"), ".cargo", "bin", binName)
            )
            else -> listOf(
                Paths.get("/usr/bin/rg"),
                Paths.get("/usr/local/bin/rg")
            )
        }

        for (path in commonPaths) {
            if (Files.exists(path) && Files.isExecutable(path)) {
                return path
            }
        }

        return null
    }

    /**
     * 下载并安装 ripgrep
     *
     * @return 安装路径，如果安装失败则返回 null
     */
    suspend fun installRipgrep(): Path? = withContext(Dispatchers.IO) {
        logger.info { "开始安装 ripgrep..." }

        try {
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()

            // 确定安装目录
            val installDir = when {
                osName.contains("win") -> Paths.get(System.getenv("USERPROFILE"), ".cargo", "bin")
                else -> Paths.get(System.getProperty("user.home"), ".local", "bin")
            }

            // 创建安装目录
            Files.createDirectories(installDir)

            // 确定下载 URL
            val version = "13.0.0" // 最新稳定版本
            val downloadUrl = when {
                osName.contains("mac") && osArch.contains("aarch64") ->
                    "https://github.com/BurntSushi/ripgrep/releases/download/$version/ripgrep-$version-aarch64-apple-darwin.tar.gz"
                osName.contains("mac") ->
                    "https://github.com/BurntSushi/ripgrep/releases/download/$version/ripgrep-$version-x86_64-apple-darwin.tar.gz"
                osName.contains("win") ->
                    "https://github.com/BurntSushi/ripgrep/releases/download/$version/ripgrep-$version-x86_64-pc-windows-msvc.zip"
                osName.contains("linux") && osArch.contains("aarch64") ->
                    "https://github.com/BurntSushi/ripgrep/releases/download/$version/ripgrep-$version-aarch64-unknown-linux-gnu.tar.gz"
                else ->
                    "https://github.com/BurntSushi/ripgrep/releases/download/$version/ripgrep-$version-x86_64-unknown-linux-musl.tar.gz"
            }

            // 下载并解压
            val tempDir = Files.createTempDirectory("ripgrep-install")
            val archivePath = tempDir.resolve(if (osName.contains("win")) "ripgrep.zip" else "ripgrep.tar.gz")

            // 下载
            logger.info { "下载 ripgrep 从 $downloadUrl" }
            val downloadProcess = ProcessBuilder(
                if (osName.contains("win")) {
                    listOf("powershell", "-Command", "Invoke-WebRequest -Uri '$downloadUrl' -OutFile '${archivePath}'")
                } else {
                    listOf("curl", "-L", downloadUrl, "-o", archivePath.toString())
                }
            ).start()

            if (!downloadProcess.waitFor(300, TimeUnit.SECONDS)) {
                downloadProcess.destroyForcibly()
                throw RipgrepSearchException("下载 ripgrep 超时")
            }

            if (downloadProcess.exitValue() != 0) {
                throw RipgrepSearchException("下载 ripgrep 失败，退出码: ${downloadProcess.exitValue()}")
            }

            // 解压
            logger.info { "解压 ripgrep 到 $tempDir" }
            val extractProcess = ProcessBuilder(
                if (osName.contains("win")) {
                    listOf("powershell", "-Command", "Expand-Archive -Path '${archivePath}' -DestinationPath '$tempDir'")
                } else {
                    listOf("tar", "-xzf", archivePath.toString(), "-C", tempDir.toString())
                }
            ).start()

            if (!extractProcess.waitFor(60, TimeUnit.SECONDS)) {
                extractProcess.destroyForcibly()
                throw RipgrepSearchException("解压 ripgrep 超时")
            }

            if (extractProcess.exitValue() != 0) {
                throw RipgrepSearchException("解压 ripgrep 失败，退出码: ${extractProcess.exitValue()}")
            }

            // 查找 rg 二进制文件
            val rgBinary = Files.walk(tempDir)
                .filter { it.fileName.toString() == (if (osName.contains("win")) "rg.exe" else "rg") }
                .findFirst()
                .orElseThrow { RipgrepSearchException("在解压后的文件中未找到 ripgrep 二进制文件") }

            // 复制到安装目录
            val installPath = installDir.resolve(rgBinary.fileName.toString())
            Files.copy(rgBinary, installPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)

            // 设置可执行权限
            if (!osName.contains("win")) {
                installPath.toFile().setExecutable(true)
            }

            // 清理临时文件
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }

            logger.info { "ripgrep 安装成功: $installPath" }
            return@withContext installPath
        } catch (e: Exception) {
            logger.error(e) { "安装 ripgrep 失败: ${e.message}" }
            return@withContext null
        }
    }

    /**
     * 将 RipgrepSearchResult 转换为 RetrievalResult
     *
     * @param result Ripgrep 搜索结果
     * @return 检索结果
     */
    fun convertToRetrievalResult(result: RipgrepSearchResult): RetrievalResult {
        // 这里需要创建一个 CodeElement 对象
        // 实际实现中，应该从 CodeIndexer 中查找对应的 CodeElement
        // 或者创建一个临时的 CodeElement
        // 这里简化处理，返回 null
        return RetrievalResult(
            element = createTempCodeElement(result),
            score = 1.0,
            explanation = "Ripgrep 搜索结果: ${result.filePath}:${result.lineNumber}"
        )
    }

    /**
     * 创建临时 CodeElement
     *
     * @param result Ripgrep 搜索结果
     * @return 临时 CodeElement
     */
    private fun createTempCodeElement(result: RipgrepSearchResult): ai.kastrax.codebase.semantic.model.CodeElement {
        // 创建一个临时的 CodeElement
        // 实际实现中，应该从 CodeIndexer 中查找对应的 CodeElement
        // 这里简化处理，创建一个最小的 CodeElement
        return ai.kastrax.codebase.semantic.model.CodeElement(
            id = "${result.filePath}:${result.lineNumber}",
            name = Paths.get(result.filePath).fileName.toString(),
            qualifiedName = result.filePath,
            type = ai.kastrax.codebase.semantic.model.CodeElementType.FILE,
            location = ai.kastrax.codebase.semantic.model.Location(
                filePath = result.filePath,
                startLine = result.lineNumber,
                endLine = result.lineNumber,
                startColumn = result.columnNumber,
                endColumn = result.columnNumber + result.matchText.length
            ),
            documentation = "",
            metadata = mutableMapOf(
                "matchText" to result.matchText,
                "lineText" to result.lineText,
                "beforeContext" to result.beforeContext.joinToString("\n"),
                "afterContext" to result.afterContext.joinToString("\n")
            )
        )
    }
}

/**
 * Ripgrep 输出处理器
 */
class RipgrepOutputProcessor {
    private var currentResult: RipgrepSearchResult? = null
    private val beforeContextMap = mutableMapOf<String, MutableList<String>>()
    private val afterContextMap = mutableMapOf<String, MutableList<String>>()

    /**
     * 处理一行输出
     *
     * @param line 输出行
     * @return 处理后的搜索结果，如果不是完整的结果则返回 null
     */
    fun processLine(line: String): RipgrepSearchResult? {
        if (line.isBlank()) {
            return null
        }

        try {
            try {
                val jsonObject = Json.parseToJsonElement(line).jsonObject
                if (!jsonObject.containsKey("type")) {
                    return null
                }
                val type = jsonObject["type"]?.jsonPrimitive?.content ?: return null

                when (type) {
                "match" -> {
                    val data = jsonObject["data"]?.jsonObject ?: return null
                    val pathObj = data["path"]?.jsonObject ?: return null
                    val path = pathObj["text"]?.jsonPrimitive?.content ?: return null
                    val lineNumber = data["line_number"]?.jsonPrimitive?.int ?: return null
                    val absoluteOffset = data["absolute_offset"]?.jsonPrimitive?.int ?: return null
                    val linesObj = data["lines"]?.jsonObject ?: return null
                    val lineText = linesObj["text"]?.jsonPrimitive?.content?.trim() ?: return null

                    // 处理子匹配
                    val submatches = mutableListOf<RipgrepSubmatch>()
                    val submatchesArray = data["submatches"]?.jsonArray ?: return null
                    var matchText = lineText

                    if (submatchesArray.size > 0) {
                        val submatchObj = submatchesArray[0].jsonObject
                        val match = submatchObj["match"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: return null
                        val start = submatchObj["start"]?.jsonPrimitive?.int ?: return null
                        val end = submatchObj["end"]?.jsonPrimitive?.int ?: return null

                        submatches.add(RipgrepSubmatch(match, start, end))
                        matchText = match
                    }

                    // 获取前置上下文
                    val beforeContext = beforeContextMap.getOrDefault("$path:$lineNumber", mutableListOf())
                    beforeContextMap.remove("$path:$lineNumber")

                    // 创建结果
                    currentResult = RipgrepSearchResult(
                        filePath = path,
                        lineNumber = lineNumber,
                        columnNumber = absoluteOffset,
                        matchText = matchText,
                        lineText = lineText,
                        beforeContext = beforeContext,
                        submatches = submatches
                    )

                    return null // 等待收集后置上下文
                }
                "context" -> {
                    val data = jsonObject["data"]?.jsonObject ?: return null
                    val pathObj = data["path"]?.jsonObject ?: return null
                    val path = pathObj["text"]?.jsonPrimitive?.content ?: return null
                    val lineNumber = data["line_number"]?.jsonPrimitive?.int ?: return null
                    val linesObj = data["lines"]?.jsonObject ?: return null
                    val lineText = linesObj["text"]?.jsonPrimitive?.content?.trim() ?: return null

                    if (currentResult != null && currentResult!!.filePath == path) {
                        if (lineNumber < currentResult!!.lineNumber) {
                            // 前置上下文
                            beforeContextMap.getOrPut("$path:${currentResult!!.lineNumber}") { mutableListOf() }
                                .add(lineText)
                        } else {
                            // 后置上下文
                            afterContextMap.getOrPut("$path:${currentResult!!.lineNumber}") { mutableListOf() }
                                .add(lineText)

                            // 如果是最后一个后置上下文，返回完整结果
                            if (lineNumber == currentResult!!.lineNumber + 1) {
                                val result = currentResult!!.copy(
                                    afterContext = afterContextMap.getOrDefault("$path:${currentResult!!.lineNumber}", mutableListOf())
                                )
                                afterContextMap.remove("$path:${currentResult!!.lineNumber}")
                                currentResult = null
                                return result
                            }
                        }
                    } else {
                        // 存储上下文，可能是下一个匹配的前置上下文
                        beforeContextMap.getOrPut("$path:${lineNumber + 1}") { mutableListOf() }
                            .add(lineText)
                    }
                }
                "end" -> {
                    // 如果有未处理的结果，返回它
                    if (currentResult != null) {
                        val path = currentResult!!.filePath
                        val lineNumber = currentResult!!.lineNumber
                        val result = currentResult!!.copy(
                            afterContext = afterContextMap.getOrDefault("$path:$lineNumber", mutableListOf())
                        )
                        afterContextMap.remove("$path:$lineNumber")
                        currentResult = null
                        return result
                    }
                }
            }

                return null
            } catch (e: Exception) {
                logger.error(e) { "解析 ripgrep 输出时发生错误: ${e.message}" }
                return null
            }
        } catch (e: Exception) {
            logger.error(e) { "处理 ripgrep 输出时发生错误: ${e.message}" }
            return null
        }
    }
}
