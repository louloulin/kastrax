package ai.kastrax.codebase.search

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

private val logger = KotlinLogging.logger {}

/**
 * 模式搜索器配置
 *
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 * @property followSymlinks 是否跟随符号链接
 * @property maxDepth 最大搜索深度
 * @property excludePatterns 排除模式列表
 * @property includePatterns 包含模式列表
 * @property respectGitignore 是否尊重 .gitignore
 * @property hiddenFiles 是否搜索隐藏文件
 */
data class PatternSearcherConfig(
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100,
    val followSymlinks: Boolean = false,
    val maxDepth: Int = Int.MAX_VALUE,
    val excludePatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList(),
    val respectGitignore: Boolean = true,
    val hiddenFiles: Boolean = false
)

/**
 * 模式搜索器
 *
 * 用于基于正则表达式的文件查找
 *
 * @property config 配置
 */
class PatternSearcher(
    private val config: PatternSearcherConfig = PatternSearcherConfig()
) {
    // 缓存
    private val cache = ConcurrentHashMap<String, List<Path>>()

    /**
     * 查找匹配正则表达式的文件
     *
     * @param baseDir 基础目录
     * @param regex 正则表达式
     * @param options 搜索选项
     * @return 匹配的文件列表
     */
    suspend fun findFilesByRegex(
        baseDir: Path,
        regex: String,
        options: Map<String, Any> = emptyMap()
    ): List<Path> = withContext(Dispatchers.IO) {
        logger.info { "开始查找匹配正则表达式的文件: $regex" }

        // 检查缓存
        val cacheKey = "$baseDir:$regex:${options.hashCode()}"
        if (config.enableCaching && cache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取匹配文件: $regex" }
            return@withContext cache[cacheKey]!!
        }

        try {
            // 编译正则表达式
            val pattern = try {
                Pattern.compile(regex)
            } catch (e: Exception) {
                logger.error(e) { "无效的正则表达式: $regex" }
                return@withContext emptyList<Path>()
            }

            // 获取搜索选项
            val followSymlinks = options["followSymlinks"] as? Boolean ?: config.followSymlinks
            val maxDepth = options["maxDepth"] as? Int ?: config.maxDepth
            val excludePatterns = options["excludePatterns"] as? List<String> ?: config.excludePatterns
            val includePatterns = options["includePatterns"] as? List<String> ?: config.includePatterns
            val respectGitignore = options["respectGitignore"] as? Boolean ?: config.respectGitignore
            val hiddenFiles = options["hiddenFiles"] as? Boolean ?: config.hiddenFiles

            // 编译排除模式
            val excludeRegexes = excludePatterns.map { Pattern.compile(it) }
            val includeRegexes = includePatterns.map { Pattern.compile(it) }

            // 查找 .gitignore 文件
            val gitignorePatterns = if (respectGitignore) {
                val gitignorePath = baseDir.resolve(".gitignore")
                if (Files.exists(gitignorePath)) {
                    Files.readAllLines(gitignorePath)
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                        .map { it.trim() }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // 编译 .gitignore 模式
            val gitignoreRegexes = gitignorePatterns.map {
                val regex = it.replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".")
                Pattern.compile(regex)
            }

            // 匹配的文件列表
            val matchingFiles = mutableListOf<Path>()

            // 遍历文件
            Files.walkFileTree(baseDir, object : SimpleFileVisitor<Path>() {
                private var currentDepth = 0

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    // 检查深度
                    if (currentDepth >= maxDepth) {
                        return FileVisitResult.SKIP_SUBTREE
                    }

                    // 检查隐藏文件
                    if (!hiddenFiles && dir.fileName.toString().startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE
                    }

                    // 检查符号链接
                    if (!followSymlinks && attrs.isSymbolicLink) {
                        return FileVisitResult.SKIP_SUBTREE
                    }

                    // 检查排除模式
                    val relativePath = baseDir.relativize(dir).toString()
                    if (excludeRegexes.any { it.matcher(relativePath).matches() }) {
                        return FileVisitResult.SKIP_SUBTREE
                    }

                    // 检查 .gitignore 模式
                    if (gitignoreRegexes.any { it.matcher(relativePath).matches() }) {
                        return FileVisitResult.SKIP_SUBTREE
                    }

                    currentDepth++
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    // 检查隐藏文件
                    if (!hiddenFiles && file.fileName.toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE
                    }

                    // 检查符号链接
                    if (!followSymlinks && attrs.isSymbolicLink) {
                        return FileVisitResult.CONTINUE
                    }

                    // 检查排除模式
                    val relativePath = baseDir.relativize(file).toString()
                    if (excludeRegexes.any { it.matcher(relativePath).matches() }) {
                        return FileVisitResult.CONTINUE
                    }

                    // 检查包含模式
                    if (includeRegexes.isNotEmpty() && includeRegexes.none { it.matcher(relativePath).matches() }) {
                        return FileVisitResult.CONTINUE
                    }

                    // 检查 .gitignore 模式
                    if (gitignoreRegexes.any { it.matcher(relativePath).matches() }) {
                        return FileVisitResult.CONTINUE
                    }

                    // 检查正则表达式
                    val fileName = file.fileName.toString()
                    if (pattern.matcher(fileName).matches()) {
                        matchingFiles.add(file)
                    }

                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: java.io.IOException?): FileVisitResult {
                    currentDepth--
                    return FileVisitResult.CONTINUE
                }
            })

            logger.info { "找到 ${matchingFiles.size} 个匹配文件: $regex" }

            // 缓存结果
            if (config.enableCaching) {
                // 维护缓存大小
                if (cache.size >= config.maxCacheSize) {
                    // 简单的缓存淘汰策略：随机移除一个
                    val keyToRemove = cache.keys.firstOrNull()
                    if (keyToRemove != null) {
                        cache.remove(keyToRemove)
                    }
                }
                cache[cacheKey] = matchingFiles
            }

            return@withContext matchingFiles
        } catch (e: Exception) {
            logger.error(e) { "查找匹配文件时发生错误: ${e.message}" }
            return@withContext emptyList<Path>()
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        logger.info { "模式搜索器缓存已清除" }
    }
}
