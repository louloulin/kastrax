package ai.kastrax.core.tools.file

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ZodTool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/**
 * 文件操作工具，用于执行文件相关操作。
 *
 * 支持的操作包括：
 * - 读取文件内容
 * - 写入文件内容
 * - 检查文件是否存在
 * - 获取文件信息
 * - 列出目录内容
 * - 创建目录
 * - 删除文件或目录
 * - 复制文件
 * - 移动文件
 */
class ZodFileOperationTool : KastraXBase(component = "TOOL", name = "文件操作工具") {

    /**
     * 文件操作工具的输入参数。
     */
    @Serializable
    data class FileOperationInput(
        val operation: String,
        val path: String? = null,
        val content: String? = null,
        val source: String? = null,
        val destination: String? = null,
        val encoding: String? = null,
        val recursive: Boolean? = null
    )

    /**
     * 文件信息。
     */
    @Serializable
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val isDirectory: Boolean,
        val isFile: Boolean,
        val lastModified: Long,
        val canRead: Boolean,
        val canWrite: Boolean,
        val canExecute: Boolean
    )

    /**
     * 文件列表项。
     */
    @Serializable
    data class FileListItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    /**
     * 文件操作工具的输出结果。
     */
    @Serializable
    data class FileOperationOutput(
        val success: Boolean,
        val result: String? = null,
        val error: String? = null,
        val fileInfo: FileInfo? = null,
        val fileList: List<FileListItem>? = null
    )

    /**
     * 创建文件操作工具的 ZodTool 实例。
     */
    fun createZodTool(): ZodTool<FileOperationInput, FileOperationOutput> {
        return zodTool {
            id = "file_operation"
            name = "文件操作工具"
            description = """
                执行文件相关操作。

                支持的操作包括：
                - read: 读取文件内容，必需参数 path（文件路径）
                - write: 写入文件内容，必需参数 path（文件路径）和 content（文件内容）
                - exists: 检查文件是否存在，必需参数 path（文件路径）
                - info: 获取文件信息，必需参数 path（文件路径）
                - list: 列出目录内容，必需参数 path（目录路径）
                - mkdir: 创建目录，必需参数 path（目录路径）
                - delete: 删除文件或目录，必需参数 path（文件或目录路径）
                - copy: 复制文件，必需参数 source（源文件路径）和 destination（目标文件路径）
                - move: 移动文件，必需参数 source（源文件路径）和 destination（目标文件路径）
                
                注意：所有路径都是相对于当前工作目录的。
            """.trimIndent()

            // 使用类型安全的方式设置输入和输出模式
            @Suppress("UNCHECKED_CAST")
            this.inputSchema = any() as Schema<FileOperationInput, FileOperationInput>

            @Suppress("UNCHECKED_CAST")
            this.outputSchema = any() as Schema<FileOperationOutput, FileOperationOutput>

            execute = { input ->
                when (input.operation) {
                    "read" -> readFile(input)
                    "write" -> writeFile(input)
                    "exists" -> checkFileExists(input)
                    "info" -> getFileInfo(input)
                    "list" -> listDirectory(input)
                    "mkdir" -> createDirectory(input)
                    "delete" -> deleteFileOrDirectory(input)
                    "copy" -> copyFile(input)
                    "move" -> moveFile(input)
                    else -> throw IllegalArgumentException("不支持的操作: ${input.operation}")
                }
            }
        }
    }

    /**
     * 读取文件内容。
     */
    private fun readFile(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("read 操作需要 path 参数")
        val encoding = input.encoding ?: "utf-8"

        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: $path")
        }
        if (!file.isFile) {
            throw IllegalArgumentException("路径不是文件: $path")
        }

        val content = when (encoding.lowercase()) {
            "utf-8" -> file.readText(Charsets.UTF_8)
            "base64" -> Base64.getEncoder().encodeToString(file.readBytes())
            else -> throw IllegalArgumentException("不支持的编码: $encoding")
        }

        return FileOperationOutput(
            success = true,
            result = content
        )
    }

    /**
     * 写入文件内容。
     */
    private fun writeFile(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("write 操作需要 path 参数")
        val content = input.content ?: throw IllegalArgumentException("write 操作需要 content 参数")
        val encoding = input.encoding ?: "utf-8"

        val file = File(path)
        
        // 确保父目录存在
        file.parentFile?.mkdirs()

        when (encoding.lowercase()) {
            "utf-8" -> file.writeText(content, Charsets.UTF_8)
            "base64" -> file.writeBytes(Base64.getDecoder().decode(content))
            else -> throw IllegalArgumentException("不支持的编码: $encoding")
        }

        return FileOperationOutput(
            success = true,
            result = "文件写入成功: $path"
        )
    }

    /**
     * 检查文件是否存在。
     */
    private fun checkFileExists(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("exists 操作需要 path 参数")

        val file = File(path)
        val exists = file.exists()

        return FileOperationOutput(
            success = true,
            result = exists.toString()
        )
    }

    /**
     * 获取文件信息。
     */
    private fun getFileInfo(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("info 操作需要 path 参数")

        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("文件或目录不存在: $path")
        }

        val fileInfo = FileInfo(
            name = file.name,
            path = file.absolutePath,
            size = file.length(),
            isDirectory = file.isDirectory,
            isFile = file.isFile,
            lastModified = file.lastModified(),
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute()
        )

        return FileOperationOutput(
            success = true,
            fileInfo = fileInfo
        )
    }

    /**
     * 列出目录内容。
     */
    private fun listDirectory(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("list 操作需要 path 参数")
        val recursive = input.recursive ?: false

        val directory = File(path)
        if (!directory.exists()) {
            throw IllegalArgumentException("目录不存在: $path")
        }
        if (!directory.isDirectory) {
            throw IllegalArgumentException("路径不是目录: $path")
        }

        val files = if (recursive) {
            directory.walkTopDown().toList()
        } else {
            directory.listFiles()?.toList() ?: emptyList()
        }

        val fileList = files.map { file ->
            FileListItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = file.length(),
                lastModified = file.lastModified()
            )
        }

        return FileOperationOutput(
            success = true,
            fileList = fileList
        )
    }

    /**
     * 创建目录。
     */
    private fun createDirectory(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("mkdir 操作需要 path 参数")

        val directory = File(path)
        val success = directory.mkdirs()

        return FileOperationOutput(
            success = success,
            result = if (success) "目录创建成功: $path" else "目录创建失败: $path"
        )
    }

    /**
     * 删除文件或目录。
     */
    private fun deleteFileOrDirectory(input: FileOperationInput): FileOperationOutput {
        val path = input.path ?: throw IllegalArgumentException("delete 操作需要 path 参数")
        val recursive = input.recursive ?: false

        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("文件或目录不存在: $path")
        }

        val success = if (file.isDirectory && recursive) {
            file.deleteRecursively()
        } else {
            file.delete()
        }

        return FileOperationOutput(
            success = success,
            result = if (success) "删除成功: $path" else "删除失败: $path"
        )
    }

    /**
     * 复制文件。
     */
    private fun copyFile(input: FileOperationInput): FileOperationOutput {
        val source = input.source ?: throw IllegalArgumentException("copy 操作需要 source 参数")
        val destination = input.destination ?: throw IllegalArgumentException("copy 操作需要 destination 参数")

        val sourceFile = File(source)
        val destinationFile = File(destination)

        if (!sourceFile.exists()) {
            throw IllegalArgumentException("源文件不存在: $source")
        }
        if (!sourceFile.isFile) {
            throw IllegalArgumentException("源路径不是文件: $source")
        }

        // 确保目标文件的父目录存在
        destinationFile.parentFile?.mkdirs()

        Files.copy(
            sourceFile.toPath(),
            destinationFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )

        return FileOperationOutput(
            success = true,
            result = "文件复制成功: $source -> $destination"
        )
    }

    /**
     * 移动文件。
     */
    private fun moveFile(input: FileOperationInput): FileOperationOutput {
        val source = input.source ?: throw IllegalArgumentException("move 操作需要 source 参数")
        val destination = input.destination ?: throw IllegalArgumentException("move 操作需要 destination 参数")

        val sourceFile = File(source)
        val destinationFile = File(destination)

        if (!sourceFile.exists()) {
            throw IllegalArgumentException("源文件不存在: $source")
        }

        // 确保目标文件的父目录存在
        destinationFile.parentFile?.mkdirs()

        Files.move(
            sourceFile.toPath(),
            destinationFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )

        return FileOperationOutput(
            success = true,
            result = "文件移动成功: $source -> $destination"
        )
    }

    /**
     * 创建文件操作工具的 Tool 实例。
     */
    fun createTool(): Tool {
        return createZodTool().toTool()
    }

    companion object {
        /**
         * 创建文件操作工具。
         *
         * @return 文件操作工具实例
         */
        fun create(): Tool {
            return ZodFileOperationTool().createTool()
        }

        /**
         * 创建文件操作 ZodTool 实例。
         *
         * @return 文件操作 ZodTool 实例
         */
        fun createZodTool(): ZodTool<FileOperationInput, FileOperationOutput> {
            return ZodFileOperationTool().createZodTool()
        }
    }
}
