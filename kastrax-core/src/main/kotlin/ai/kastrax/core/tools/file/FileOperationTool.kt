package ai.kastrax.core.tools.file

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
class FileOperationTool : Tool {

    private val logger = KotlinLogging.logger {}

    override val id: String = "file_operation"
    override val name: String = "文件操作工具"
    override val description: String = """
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

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("operation") {
                put("type", "string")
                put("description", "要执行的操作")
                putJsonArray("enum") {
                    add("read")
                    add("write")
                    add("exists")
                    add("info")
                    add("list")
                    add("mkdir")
                    add("delete")
                    add("copy")
                    add("move")
                }
            }
            putJsonObject("path") {
                put("type", "string")
                put("description", "文件或目录路径")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "文件内容（用于write操作）")
            }
            putJsonObject("source") {
                put("type", "string")
                put("description", "源文件路径（用于copy和move操作）")
            }
            putJsonObject("destination") {
                put("type", "string")
                put("description", "目标文件路径（用于copy和move操作）")
            }
            putJsonObject("encoding") {
                put("type", "string")
                put("description", "文件编码（默认为UTF-8）")
                putJsonArray("enum") {
                    add("utf-8")
                    add("base64")
                }
            }
            putJsonObject("recursive") {
                put("type", "boolean")
                put("description", "是否递归操作（用于delete和list操作）")
            }
        }
        putJsonArray("required") {
            add("operation")
        }
    }

    override val outputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("success") {
                put("type", "boolean")
                put("description", "操作是否成功")
            }
            putJsonObject("result") {
                put("type", "string")
                put("description", "操作结果")
            }
            putJsonObject("error") {
                put("type", "string")
                put("description", "错误信息（如果操作失败）")
            }
        }
    }

    override suspend fun execute(input: JsonElement): JsonElement {
        logger.info { "执行文件操作: $input" }

        try {
            val inputObj = input.jsonObject
            val operation = inputObj["operation"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少必需的 operation 参数")

            return when (operation) {
                "read" -> readFile(inputObj)
                "write" -> writeFile(inputObj)
                "exists" -> checkFileExists(inputObj)
                "info" -> getFileInfo(inputObj)
                "list" -> listDirectory(inputObj)
                "mkdir" -> createDirectory(inputObj)
                "delete" -> deleteFileOrDirectory(inputObj)
                "copy" -> copyFile(inputObj)
                "move" -> moveFile(inputObj)
                else -> throw IllegalArgumentException("不支持的操作: $operation")
            }
        } catch (e: Exception) {
            logger.error(e) { "执行文件操作时发生错误" }
            return buildJsonObject {
                put("success", JsonPrimitive(false))
                put("error", JsonPrimitive(e.message ?: "未知错误"))
            }
        }
    }

    /**
     * 读取文件内容。
     */
    private fun readFile(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("read 操作需要 path 参数")
        val encoding = input["encoding"]?.jsonPrimitive?.contentOrNull ?: "utf-8"

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

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", JsonPrimitive(content))
        }
    }

    /**
     * 写入文件内容。
     */
    private fun writeFile(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("write 操作需要 path 参数")
        val content = input["content"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("write 操作需要 content 参数")
        val encoding = input["encoding"]?.jsonPrimitive?.contentOrNull ?: "utf-8"

        val file = File(path)
        
        // 确保父目录存在
        file.parentFile?.mkdirs()

        when (encoding.lowercase()) {
            "utf-8" -> file.writeText(content, Charsets.UTF_8)
            "base64" -> file.writeBytes(Base64.getDecoder().decode(content))
            else -> throw IllegalArgumentException("不支持的编码: $encoding")
        }

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", JsonPrimitive("文件写入成功: $path"))
        }
    }

    /**
     * 检查文件是否存在。
     */
    private fun checkFileExists(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("exists 操作需要 path 参数")

        val file = File(path)
        val exists = file.exists()

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", JsonPrimitive(exists.toString()))
        }
    }

    /**
     * 获取文件信息。
     */
    private fun getFileInfo(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("info 操作需要 path 参数")

        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("文件或目录不存在: $path")
        }

        val info = buildJsonObject {
            put("name", JsonPrimitive(file.name))
            put("path", JsonPrimitive(file.absolutePath))
            put("size", JsonPrimitive(file.length()))
            put("isDirectory", JsonPrimitive(file.isDirectory))
            put("isFile", JsonPrimitive(file.isFile))
            put("lastModified", JsonPrimitive(file.lastModified()))
            put("canRead", JsonPrimitive(file.canRead()))
            put("canWrite", JsonPrimitive(file.canWrite()))
            put("canExecute", JsonPrimitive(file.canExecute()))
        }

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", info)
        }
    }

    /**
     * 列出目录内容。
     */
    private fun listDirectory(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("list 操作需要 path 参数")
        val recursive = input["recursive"]?.jsonPrimitive?.booleanOrNull ?: false

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

        val fileList = buildJsonArray {
            files.forEach { file ->
                add(buildJsonObject {
                    put("name", JsonPrimitive(file.name))
                    put("path", JsonPrimitive(file.absolutePath))
                    put("isDirectory", JsonPrimitive(file.isDirectory))
                    put("size", JsonPrimitive(file.length()))
                    put("lastModified", JsonPrimitive(file.lastModified()))
                })
            }
        }

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", fileList)
        }
    }

    /**
     * 创建目录。
     */
    private fun createDirectory(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("mkdir 操作需要 path 参数")

        val directory = File(path)
        val success = directory.mkdirs()

        return buildJsonObject {
            put("success", JsonPrimitive(success))
            put("result", JsonPrimitive(if (success) "目录创建成功: $path" else "目录创建失败: $path"))
        }
    }

    /**
     * 删除文件或目录。
     */
    private fun deleteFileOrDirectory(input: JsonObject): JsonElement {
        val path = input["path"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("delete 操作需要 path 参数")
        val recursive = input["recursive"]?.jsonPrimitive?.booleanOrNull ?: false

        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("文件或目录不存在: $path")
        }

        val success = if (file.isDirectory && recursive) {
            file.deleteRecursively()
        } else {
            file.delete()
        }

        return buildJsonObject {
            put("success", JsonPrimitive(success))
            put("result", JsonPrimitive(if (success) "删除成功: $path" else "删除失败: $path"))
        }
    }

    /**
     * 复制文件。
     */
    private fun copyFile(input: JsonObject): JsonElement {
        val source = input["source"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("copy 操作需要 source 参数")
        val destination = input["destination"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("copy 操作需要 destination 参数")

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

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", JsonPrimitive("文件复制成功: $source -> $destination"))
        }
    }

    /**
     * 移动文件。
     */
    private fun moveFile(input: JsonObject): JsonElement {
        val source = input["source"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("move 操作需要 source 参数")
        val destination = input["destination"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("move 操作需要 destination 参数")

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

        return buildJsonObject {
            put("success", JsonPrimitive(true))
            put("result", JsonPrimitive("文件移动成功: $source -> $destination"))
        }
    }

    companion object {
        /**
         * 创建文件操作工具。
         *
         * @return 文件操作工具实例
         */
        fun create(): Tool {
            return FileOperationTool()
        }
    }
}
