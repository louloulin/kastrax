package ai.kastrax.codebase.filesystem

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes

/**
 * 文件过滤器配置
 *
 * @property includeExtensions 包含的文件扩展名
 * @property excludeExtensions 排除的文件扩展名
 * @property excludePatterns 排除的文件模式（正则表达式）
 * @property excludeDirectories 排除的目录名
 * @property maxFileSizeBytes 最大文件大小（字节）
 * @property excludeBinaryFiles 是否排除二进制文件
 */
data class FileFilterConfig(
    val includeExtensions: Set<String> = setOf(
        // 代码文件
        "java", "kt", "kts", "scala", "groovy",
        "py", "js", "ts", "jsx", "tsx",
        "html", "css", "scss", "less",
        "c", "cpp", "h", "hpp", "cs", "go", "rs",
        "php", "rb", "swift", "m", "mm",
        // 配置文件
        "xml", "json", "yaml", "yml", "toml", "properties",
        // 文档文件
        "md", "txt", "rst"
    ),
    val excludeExtensions: Set<String> = setOf(
        "class", "jar", "war", "zip", "tar", "gz", "rar",
        "jpg", "jpeg", "png", "gif", "bmp", "ico", "svg",
        "mp3", "mp4", "avi", "mov", "wmv", "flv", "wav",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "o", "obj", "a", "so", "dll", "exe", "bin"
    ),
    val excludePatterns: Set<Regex> = setOf(
        Regex("\\.git/.*"),
        Regex("\\.idea/.*"),
        Regex("build/.*"),
        Regex("target/.*"),
        Regex("node_modules/.*"),
        Regex("\\.gradle/.*"),
        Regex(".*\\.min\\.js"),
        Regex(".*\\.min\\.css")
    ),
    val excludeDirectories: Set<String> = setOf(
        ".git", ".idea", "build", "target", "node_modules", ".gradle",
        "dist", "out", "bin", "obj", "vendor", "bower_components"
    ),
    val maxFileSizeBytes: Long = 1024 * 1024, // 1MB
    val excludeBinaryFiles: Boolean = true
)

/**
 * 文件过滤器
 *
 * 用于过滤需要索引的文件
 *
 * @property config 配置
 */
class FileFilter(private val config: FileFilterConfig = FileFilterConfig()) {
    
    /**
     * 检查文件是否应该被索引
     *
     * @param path 文件路径
     * @return 如果文件应该被索引，则返回 true
     */
    fun shouldIndexFile(path: Path): Boolean {
        // 检查文件是否存在且是常规文件
        if (!path.isRegularFile()) {
            return false
        }
        
        // 检查文件扩展名
        val extension = path.extension.lowercase()
        
        // 如果在排除列表中，则排除
        if (extension in config.excludeExtensions) {
            return false
        }
        
        // 如果设置了包含列表，且不在包含列表中，则排除
        if (config.includeExtensions.isNotEmpty() && extension !in config.includeExtensions) {
            return false
        }
        
        // 检查文件路径是否匹配排除模式
        val pathString = path.toString().replace('\\', '/')
        if (config.excludePatterns.any { it.matches(pathString) }) {
            return false
        }
        
        // 检查文件大小
        try {
            val fileSize = path.toFile().length()
            if (fileSize > config.maxFileSizeBytes) {
                return false
            }
            
            // 检查是否为二进制文件
            if (config.excludeBinaryFiles && isBinaryFile(path)) {
                return false
            }
        } catch (e: Exception) {
            // 如果无法访问文件，则排除
            return false
        }
        
        return true
    }
    
    /**
     * 检查目录是否应该被索引
     *
     * @param path 目录路径
     * @return 如果目录应该被索引，则返回 true
     */
    fun shouldIndexDirectory(path: Path): Boolean {
        // 检查是否是目录
        if (!path.isDirectory()) {
            return false
        }
        
        // 检查目录名是否在排除列表中
        val dirName = path.name
        if (dirName in config.excludeDirectories) {
            return false
        }
        
        // 检查目录路径是否匹配排除模式
        val pathString = path.toString().replace('\\', '/')
        if (config.excludePatterns.any { it.matches(pathString) }) {
            return false
        }
        
        return true
    }
    
    /**
     * 检查文件是否为二进制文件
     *
     * @param path 文件路径
     * @return 如果文件是二进制文件，则返回 true
     */
    private fun isBinaryFile(path: Path): Boolean {
        try {
            // 读取文件的前 8KB 进行检查
            val bytes = path.readBytes().take(8192).toByteArray()
            
            // 检查是否包含空字节（通常表示二进制文件）
            return bytes.contains(0)
        } catch (e: Exception) {
            // 如果无法读取文件，则假设为二进制文件
            return true
        }
    }
}
