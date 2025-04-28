package ai.kastrax.examples.mcp

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 文件操作 MCP 应用案例
 * 
 * 这个示例展示了如何创建一个提供文件操作功能的 MCP 服务器，
 * 以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。
 */
fun main() = runBlocking {
    println("启动文件操作 MCP 应用案例...")
    
    // 创建临时目录作为工作目录
    val workDir = createTempWorkDir()
    println("创建临时工作目录: $workDir")
    
    // 在工作目录中创建一些示例文件
    createSampleFiles(workDir)
    
    // 启动服务器
    val serverJob = launch {
        val server = createFileServer(workDir)
        server.startSSE(port = 8085)
        println("文件操作 MCP 服务器已启动在端口 8085")
        
        // 保持服务器运行
        try {
            while (true) {
                delay(1000)
            }
        } finally {
            server.stop()
            println("文件操作 MCP 服务器已停止")
        }
    }
    
    // 等待服务器启动
    delay(2000)
    
    // 创建并使用客户端
    val client = createFileClient()
    
    try {
        // 连接到服务器
        println("连接到文件操作 MCP 服务器...")
        client.connect()
        println("已连接到文件操作 MCP 服务器")
        
        // 获取服务器能力
        val hasTools = client.supportsCapability("tools")
        println("服务器支持工具: $hasTools")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 列出目录内容
        println("\n列出根目录内容...")
        val listResult = client.callTool("list_files", mapOf("path" to "/"))
        println("目录内容: $listResult")
        
        // 创建新目录
        println("\n创建新目录...")
        val mkdirResult = client.callTool("create_directory", mapOf("path" to "/new_folder"))
        println("创建目录结果: $mkdirResult")
        
        // 创建文本文件
        println("\n创建文本文件...")
        val createResult = client.callTool("write_text_file", mapOf(
            "path" to "/new_folder/hello.txt",
            "content" to "Hello, MCP File Server!\n这是一个测试文件。\n创建于 ${LocalDateTime.now()}"
        ))
        println("创建文件结果: $createResult")
        
        // 读取文件内容
        println("\n读取文件内容...")
        val readResult = client.callTool("read_text_file", mapOf("path" to "/new_folder/hello.txt"))
        println("文件内容: $readResult")
        
        // 获取文件信息
        println("\n获取文件信息...")
        val infoResult = client.callTool("get_file_info", mapOf("path" to "/new_folder/hello.txt"))
        println("文件信息: $infoResult")
        
        // 复制文件
        println("\n复制文件...")
        val copyResult = client.callTool("copy_file", mapOf(
            "source" to "/new_folder/hello.txt",
            "destination" to "/hello_copy.txt"
        ))
        println("复制文件结果: $copyResult")
        
        // 移动文件
        println("\n移动文件...")
        val moveResult = client.callTool("move_file", mapOf(
            "source" to "/hello_copy.txt",
            "destination" to "/new_folder/hello_moved.txt"
        ))
        println("移动文件结果: $moveResult")
        
        // 再次列出目录内容
        println("\n列出新目录内容...")
        val listNewResult = client.callTool("list_files", mapOf("path" to "/new_folder"))
        println("目录内容: $listNewResult")
        
        // 删除文件
        println("\n删除文件...")
        val deleteResult = client.callTool("delete_file", mapOf("path" to "/new_folder/hello.txt"))
        println("删除文件结果: $deleteResult")
        
        // 最后列出目录内容
        println("\n最后列出目录内容...")
        val listFinalResult = client.callTool("list_files", mapOf("path" to "/new_folder"))
        println("目录内容: $listFinalResult")
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与文件操作 MCP 服务器的连接")
        
        // 停止服务器
        serverJob.cancel()
        
        // 清理临时目录
        println("清理临时工作目录...")
        cleanupTempWorkDir(workDir)
    }
}

/**
 * 创建文件操作 MCP 服务器
 */
private fun createFileServer(workDir: String) = mcpServer {
    name("FileMCPServer")
    version("1.0.0")
    
    // 添加列出文件工具
    tool {
        name = "list_files"
        description = "列出指定目录中的文件和子目录"
        
        // 添加参数
        parameters {
            parameter {
                name = "path"
                description = "要列出内容的目录路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val path = params["path"] as? String ?: "/"
            println("执行list_files工具，路径: $path")
            
            // 解析路径
            val resolvedPath = resolvePath(workDir, path)
            val directory = File(resolvedPath)
            
            // 检查目录是否存在
            if (!directory.exists()) {
                return@handler "错误: 目录不存在: $path"
            }
            
            // 检查是否是目录
            if (!directory.isDirectory) {
                return@handler "错误: 路径不是目录: $path"
            }
            
            // 列出目录内容
            val files = directory.listFiles() ?: emptyArray()
            
            // 如果目录为空
            if (files.isEmpty()) {
                return@handler "目录 $path 为空。"
            }
            
            // 格式化返回结果
            val filesList = files.sortedBy { !it.isDirectory }.joinToString("\n") { file ->
                val type = if (file.isDirectory) "目录" else "文件"
                val size = if (file.isFile) formatFileSize(file.length()) else ""
                val lastModified = formatTimestamp(file.lastModified())
                
                String.format("%-6s %-15s %-20s %s", type, size, lastModified, file.name)
            }
            
            """
            |目录: $path
            |
            |类型   大小           最后修改时间         名称
            |${"-".repeat(60)}
            |$filesList
            """.trimMargin()
        }
    }
    
    // 添加创建目录工具
    tool {
        name = "create_directory"
        description = "创建新目录"
        
        // 添加参数
        parameters {
            parameter {
                name = "path"
                description = "要创建的目录路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val path = params["path"] as? String ?: ""
            println("执行create_directory工具，路径: $path")
            
            // 解析路径
            val resolvedPath = resolvePath(workDir, path)
            val directory = File(resolvedPath)
            
            // 检查目录是否已存在
            if (directory.exists()) {
                return@handler "目录已存在: $path"
            }
            
            // 创建目录
            val success = directory.mkdirs()
            
            if (success) {
                "成功创建目录: $path"
            } else {
                "创建目录失败: $path"
            }
        }
    }
    
    // 添加写入文本文件工具
    tool {
        name = "write_text_file"
        description = "创建或覆盖文本文件"
        
        // 添加参数
        parameters {
            parameter {
                name = "path"
                description = "文件路径"
                type = "string"
                required = true
            }
            
            parameter {
                name = "content"
                description = "文件内容"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val path = params["path"] as? String ?: ""
            val content = params["content"] as? String ?: ""
            println("执行write_text_file工具，路径: $path")
            
            // 解析路径
            val resolvedPath = resolvePath(workDir, path)
            val file = File(resolvedPath)
            
            // 确保父目录存在
            file.parentFile?.mkdirs()
            
            try {
                // 写入文件
                file.writeText(content)
                "成功写入文件: $path (${content.length} 字符)"
            } catch (e: Exception) {
                "写入文件失败: $path, 错误: ${e.message}"
            }
        }
    }
    
    // 添加读取文本文件工具
    tool {
        name = "read_text_file"
        description = "读取文本文件内容"
        
        // 添加参数
        parameters {
            parameter {
                name = "path"
                description = "文件路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val path = params["path"] as? String ?: ""
            println("执行read_text_file工具，路径: $path")
            
            // 解析路径
            val resolvedPath = resolvePath(workDir, path)
            val file = File(resolvedPath)
            
            // 检查文件是否存在
            if (!file.exists()) {
                return@handler "错误: 文件不存在: $path"
            }
            
            // 检查是否是文件
            if (!file.isFile) {
                return@handler "错误: 路径不是文件: $path"
            }
            
            try {
                // 读取文件
                val content = file.readText()
                """
                |文件: $path
                |大小: ${formatFileSize(file.length())}
                |内容:
                |
                |$content
                """.trimMargin()
            } catch (e: Exception) {
                "读取文件失败: $path, 错误: ${e.message}"
            }
        }
    }
    
    // 添加获取文件信息工具
    tool {
        name = "get_file_info"
        description = "获取文件或目录的详细信息"
        
        // 添加参数
        parameters {
            parameter {
                name = "path"
                description = "文件或目录路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val path = params["path"] as? String ?: ""
            println("执行get_file_info工具，路径: $path")
            
            // 解析路径
            val resolvedPath = resolvePath(workDir, path)
            val file = File(resolvedPath)
            
            // 检查文件是否存在
            if (!file.exists()) {
                return@handler "错误: 文件或目录不存在: $path"
            }
            
            // 获取文件信息
            val type = if (file.isDirectory) "目录" else "文件"
            val size = if (file.isFile) formatFileSize(file.length()) else "N/A"
            val lastModified = formatTimestamp(file.lastModified())
            val permissions = getFilePermissions(file)
            
            // 如果是目录，计算子项数量
            val childrenInfo = if (file.isDirectory) {
                val children = file.listFiles() ?: emptyArray()
                val dirs = children.count { it.isDirectory }
                val files = children.count { it.isFile }
                "\n子目录数: $dirs\n文件数: $files"
            } else {
                ""
            }
            
            """
            |路径: $path
            |类型: $type
            |大小: $size
            |创建时间: ${formatTimestamp(getCreationTime(file))}
            |最后修改: $lastModified
            |权限: $permissions$childrenInfo
            """.trimMargin()
        }
    }
    
    // 添加复制文件工具
    tool {
        name = "copy_file"
        description = "复制文件或目录"
        
        // 添加参数
        parameters {
            parameter {
                name = "source"
                description = "源文件或目录路径"
                type = "string"
                required = true
            }
            
            parameter {
                name = "destination"
                description = "目标文件或目录路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val source = params["source"] as? String ?: ""
            val destination = params["destination"] as? String ?: ""
            println("执行copy_file工具，源: $source, 目标: $destination")
            
            // 解析路径
            val resolvedSource = resolvePath(workDir, source)
            val resolvedDestination = resolvePath(workDir, destination)
            
            val sourceFile = File(resolvedSource)
            val destinationFile = File(resolvedDestination)
            
            // 检查源文件是否存在
            if (!sourceFile.exists()) {
                return@handler "错误: 源文件或目录不存在: $source"
            }
            
            try {
                // 确保目标父目录存在
                destinationFile.parentFile?.mkdirs()
                
                if (sourceFile.isFile) {
                    // 复制文件
                    sourceFile.copyTo(destinationFile, overwrite = true)
                    "成功复制文件: $source -> $destination"
                } else {
                    // 目录复制需要递归实现，这里简化处理
                    "错误: 目录复制功能尚未实现"
                }
            } catch (e: Exception) {
                "复制失败: $source -> $destination, 错误: ${e.message}"
            }
        }
    }
    
    // 添加移动文件工具
    tool {
        name = "move_file"
        description = "移动或重命名文件或目录"
        
        // 添加参数
        parameters {
            parameter {
                name = "source"
                description = "源文件或目录路径"
                type = "string"
                required = true
            }
            
            parameter {
                name = "destination"
                description = "目标文件或目录路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val source = params["source"] as? String ?: ""
            val destination = params["destination"] as? String ?: ""
            println("执行move_file工具，源: $source, 目标: $destination")
            
            // 解析路径
            val resolvedSource = resolvePath(workDir, source)
            val resolvedDestination = resolvePath(workDir, destination)
            
            val sourceFile = File(resolvedSource)
            val destinationFile = File(resolvedDestination)
            
            // 检查源文件是否存在
            if (!sourceFile.exists()) {
                return@handler "错误: 源文件或目录不存在: $source"
            }
            
            try {
                // 确保目标父目录存在
                destinationFile.parentFile?.mkdirs()
                
                // 移动文件或目录
                val success = sourceFile.renameTo(destinationFile)
                
                if (success) {
                    "成功移动: $source -> $destination"
                } else {
                    "移动失败: $source -> $destination"
                }
            } catch (e: Exception) {
                "移动失败: $source -> $destination, 错误: ${e.message}"
            }
        }
    }
    
    // 添加删除文件工具
    tool {
        name = "delete_file"
        description = "删除文件或目录"
        
        // 添加参数
        parameters {
            parameter {
                name = "path"
                description = "要删除的文件或目录路径"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val path = params["path"] as? String ?: ""
            println("执行delete_file工具，路径: $path")
            
            // 解析路径
            val resolvedPath = resolvePath(workDir, path)
            val file = File(resolvedPath)
            
            // 检查文件是否存在
            if (!file.exists()) {
                return@handler "文件或目录不存在: $path"
            }
            
            try {
                // 删除文件或目录
                val success = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                
                if (success) {
                    "成功删除: $path"
                } else {
                    "删除失败: $path"
                }
            } catch (e: Exception) {
                "删除失败: $path, 错误: ${e.message}"
            }
        }
    }
}

/**
 * 创建文件操作 MCP 客户端
 */
private fun createFileClient() = mcpClient {
    name("FileMCPClient")
    version("1.0.0")
    
    server {
        sse {
            url = "http://localhost:8085"
        }
    }
}

/**
 * 创建临时工作目录
 */
private fun createTempWorkDir(): String {
    val tempDir = Files.createTempDirectory("mcp_file_example_").toFile()
    return tempDir.absolutePath
}

/**
 * 在工作目录中创建示例文件
 */
private fun createSampleFiles(workDir: String) {
    // 创建一些示例文件和目录
    val docsDir = File(workDir, "documents")
    docsDir.mkdirs()
    
    val imagesDir = File(workDir, "images")
    imagesDir.mkdirs()
    
    // 创建一些文本文件
    File(workDir, "readme.txt").writeText("""
        这是一个示例文件系统，用于演示 MCP 文件操作功能。
        
        目录结构:
        - documents/: 文档目录
        - images/: 图片目录
        
        创建于: ${LocalDateTime.now()}
    """.trimIndent())
    
    File(docsDir, "sample.txt").writeText("""
        这是一个示例文档文件。
        
        它包含一些文本内容，用于测试文件读取功能。
        
        创建于: ${LocalDateTime.now()}
    """.trimIndent())
    
    // 创建一个简单的二进制文件（模拟图片）
    val dummyImageData = ByteArray(1024) { it.toByte() }
    File(imagesDir, "sample.bin").writeBytes(dummyImageData)
}

/**
 * 清理临时工作目录
 */
private fun cleanupTempWorkDir(workDir: String) {
    val dir = File(workDir)
    if (dir.exists()) {
        dir.deleteRecursively()
    }
}

/**
 * 解析路径（将相对路径转换为绝对路径）
 */
private fun resolvePath(workDir: String, path: String): String {
    // 规范化路径
    var normalizedPath = path.replace('\\', '/')
    
    // 移除开头的斜杠
    if (normalizedPath.startsWith("/")) {
        normalizedPath = normalizedPath.substring(1)
    }
    
    // 组合工作目录和路径
    return if (normalizedPath.isEmpty()) {
        workDir
    } else {
        Paths.get(workDir, normalizedPath).toString()
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var unitIndex = 0
    
    while (value > 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    
    return String.format("%.2f %s", value, units[unitIndex])
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return formatter.format(
        LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault())
    )
}

/**
 * 获取文件创建时间
 */
private fun getCreationTime(file: File): Long {
    return try {
        Files.getAttribute(file.toPath(), "creationTime") as? java.nio.file.attribute.FileTime
    } catch (e: Exception) {
        null
    }?.toMillis() ?: file.lastModified()
}

/**
 * 获取文件权限
 */
private fun getFilePermissions(file: File): String {
    val canRead = if (file.canRead()) "r" else "-"
    val canWrite = if (file.canWrite()) "w" else "-"
    val canExecute = if (file.canExecute()) "x" else "-"
    return "$canRead$canWrite$canExecute"
}
