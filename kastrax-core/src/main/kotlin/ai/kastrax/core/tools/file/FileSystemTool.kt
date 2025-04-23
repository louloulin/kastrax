package ai.kastrax.core.tools.file

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * 文件系统工具，用于读取和写入文件。
 */
class FileSystemTool(
    private val rootPath: String = ".",
    private val allowAbsolutePaths: Boolean = false
) : Tool, KastraXBase(name = "文件系统",component = "TOOL") {

    override val id: String = "file_system"
    override val description: String = "读取和写入文件"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("action") {
                put("type", "string")
                put("description", "要执行的操作: read, write, append, list, exists, delete")
                putJsonArray("enum") {
                    add(JsonPrimitive("read"))
                    add(JsonPrimitive("write"))
                    add(JsonPrimitive("append"))
                    add(JsonPrimitive("list"))
                    add(JsonPrimitive("exists"))
                    add(JsonPrimitive("delete"))
                }
            }
            putJsonObject("path") {
                put("type", "string")
                put("description", "文件或目录路径")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "写入或追加的内容")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("action"))
            add(JsonPrimitive("path"))
        }
    }

    override val outputSchema: JsonObject? = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("success") {
                put("type", "boolean")
                put("description", "操作是否成功")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "文件内容（读取操作）")
            }
            putJsonObject("message") {
                put("type", "string")
                put("description", "操作结果消息")
            }
            putJsonObject("error") {
                put("type", "string")
                put("description", "错误消息（如果操作失败）")
            }
            putJsonObject("files") {
                put("type", "array")
                put("description", "目录中的文件列表（列出操作）")
            }
            putJsonObject("exists") {
                put("type", "boolean")
                put("description", "文件是否存在（检查操作）")
            }
            putJsonObject("isDirectory") {
                put("type", "boolean")
                put("description", "是否是目录（检查操作）")
            }
            putJsonObject("deleted") {
                put("type", "boolean")
                put("description", "文件是否被删除（删除操作）")
            }
        }
    }

    override suspend fun execute(input: JsonElement): JsonElement {
        logger.info { "执行文件系统操作: $input" }

        val inputObj = input as? JsonObject ?: throw IllegalArgumentException("Input must be a JSON object")

        val action = inputObj["action"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> it.toString()
            }
        } ?: throw IllegalArgumentException("Action is required")

        val path = inputObj["path"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> it.toString()
            }
        } ?: throw IllegalArgumentException("Path is required")

        val content = inputObj["content"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> it.toString()
            }
        } ?: ""

        val resolvedPath = resolvePath(path)

        return when (action) {
            "read" -> readFile(resolvedPath)
            "write" -> writeFile(resolvedPath, content)
            "append" -> appendFile(resolvedPath, content)
            "list" -> listDirectory(resolvedPath)
            "exists" -> checkExists(resolvedPath)
            "delete" -> deleteFile(resolvedPath)
            else -> buildJsonObject {
                put("success", false)
                put("error", "不支持的操作: $action")
            }
        }
    }

    private fun resolvePath(path: String): Path {
        if (allowAbsolutePaths && Paths.get(path).isAbsolute) {
            return Paths.get(path)
        }
        return Paths.get(rootPath, path).normalize()
    }

    private suspend fun readFile(path: Path): JsonObject = withContext(Dispatchers.IO) {
        try {
            logger.info { "读取文件: $path" }
            val content = Files.readString(path)
            buildJsonObject {
                put("content", content)
                put("success", true)
            }
        } catch (e: Exception) {
            logger.error(e) { "读取文件失败: $path" }
            buildJsonObject {
                put("error", "读取文件失败: ${e.message}")
                put("success", false)
            }
        }
    }

    private suspend fun writeFile(path: Path, content: String): JsonObject = withContext(Dispatchers.IO) {
        try {
            logger.info { "写入文件: $path" }
            Files.createDirectories(path.parent)
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            buildJsonObject {
                put("message", "文件写入成功")
                put("success", true)
            }
        } catch (e: Exception) {
            logger.error(e) { "写入文件失败: $path" }
            buildJsonObject {
                put("error", "写入文件失败: ${e.message}")
                put("success", false)
            }
        }
    }

    private suspend fun appendFile(path: Path, content: String): JsonObject = withContext(Dispatchers.IO) {
        try {
            logger.info { "追加文件: $path" }
            Files.createDirectories(path.parent)
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            buildJsonObject {
                put("message", "文件追加成功")
                put("success", true)
            }
        } catch (e: Exception) {
            logger.error(e) { "追加文件失败: $path" }
            buildJsonObject {
                put("error", "追加文件失败: ${e.message}")
                put("success", false)
            }
        }
    }

    private suspend fun listDirectory(path: Path): JsonObject = withContext(Dispatchers.IO) {
        try {
            logger.info { "列出目录: $path" }
            val files = Files.list(path).use { stream ->
                stream.map { p ->
                    buildJsonObject {
                        put("name", p.fileName.toString())
                        put("isDirectory", Files.isDirectory(p))
                        put("size", Files.size(p))
                        put("lastModified", Files.getLastModifiedTime(p).toMillis())
                    }
                }.toList()
            }

            buildJsonObject {
                putJsonArray("files") {
                    files.forEach { add(it) }
                }
                put("success", true)
            }
        } catch (e: Exception) {
            logger.error(e) { "列出目录失败: $path" }
            buildJsonObject {
                put("error", "列出目录失败: ${e.message}")
                put("success", false)
            }
        }
    }

    private suspend fun checkExists(path: Path): JsonObject = withContext(Dispatchers.IO) {
        try {
            logger.info { "检查文件是否存在: $path" }
            val exists = Files.exists(path)
            buildJsonObject {
                put("exists", exists)
                put("isDirectory", if (exists) Files.isDirectory(path) else false)
                put("success", true)
            }
        } catch (e: Exception) {
            logger.error(e) { "检查文件是否存在失败: $path" }
            buildJsonObject {
                put("error", "检查文件是否存在失败: ${e.message}")
                put("success", false)
            }
        }
    }

    private suspend fun deleteFile(path: Path): JsonObject = withContext(Dispatchers.IO) {
        try {
            logger.info { "删除文件: $path" }
            val deleted = Files.deleteIfExists(path)
            buildJsonObject {
                put("deleted", deleted)
                put("success", true)
            }
        } catch (e: Exception) {
            logger.error(e) { "删除文件失败: $path" }
            buildJsonObject {
                put("error", "删除文件失败: ${e.message}")
                put("success", false)
            }
        }
    }

    companion object {
        /**
         * 创建文件系统工具。
         */
        fun create(
            rootPath: String = ".",
            allowAbsolutePaths: Boolean = false
        ): Tool {
            return FileSystemTool(
                rootPath = rootPath,
                allowAbsolutePaths = allowAbsolutePaths
            )
        }
    }
}
