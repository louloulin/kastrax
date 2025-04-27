package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.file.FileOperationToolFactory
import ai.kastrax.core.tools.file.ZodFileOperationTool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 文件操作工具示例，展示如何使用文件操作工具执行文件操作。
 */
fun main() = runBlocking {
    println("KastraX 文件操作工具示例")
    println("-------------------")

    // 创建文件操作工具
    val fileOperationTool = FileOperationToolFactory.createZodTool()

    println("\n直接使用 ZodTool 接口:")

    // 创建临时目录和文件
    val tempDir = createTempDirectory()
    val testFile = File(tempDir, "test.txt")
    testFile.writeText("这是测试文件的内容")
    println("创建了测试文件: ${testFile.absolutePath}")

    // 读取文件
    val readInput = ZodFileOperationTool.FileOperationInput(
        operation = "read",
        path = testFile.absolutePath
    )
    val readResult = fileOperationTool.execute(readInput)
    println("读取文件内容: ${readResult.result}")

    // 获取文件信息
    val infoInput = ZodFileOperationTool.FileOperationInput(
        operation = "info",
        path = testFile.absolutePath
    )
    val infoResult = fileOperationTool.execute(infoInput)
    println("文件信息: 名称=${infoResult.fileInfo?.name}, 大小=${infoResult.fileInfo?.size}字节")

    // 创建新目录
    val newDir = File(tempDir, "newDir")
    val mkdirInput = ZodFileOperationTool.FileOperationInput(
        operation = "mkdir",
        path = newDir.absolutePath
    )
    val mkdirResult = fileOperationTool.execute(mkdirInput)
    println("创建目录结果: ${mkdirResult.result}")

    // 写入新文件
    val newFile = File(newDir, "new.txt")
    val writeInput = ZodFileOperationTool.FileOperationInput(
        operation = "write",
        path = newFile.absolutePath,
        content = "这是新文件的内容"
    )
    val writeResult = fileOperationTool.execute(writeInput)
    println("写入文件结果: ${writeResult.result}")

    // 列出目录内容
    val listInput = ZodFileOperationTool.FileOperationInput(
        operation = "list",
        path = tempDir.absolutePath
    )
    val listResult = fileOperationTool.execute(listInput)
    println("目录内容:")
    listResult.fileList?.forEach { item ->
        println("  ${item.name} (${if (item.isDirectory) "目录" else "文件"}, ${item.size}字节)")
    }

    // 复制文件
    val copyFile = File(tempDir, "copy.txt")
    val copyInput = ZodFileOperationTool.FileOperationInput(
        operation = "copy",
        source = testFile.absolutePath,
        destination = copyFile.absolutePath
    )
    val copyResult = fileOperationTool.execute(copyInput)
    println("复制文件结果: ${copyResult.result}")

    // 创建代理
    val fileOperationAgent = agent {
        name = "文件操作助手"
        instructions = """
            你是一个文件操作助手，可以帮助用户处理各种文件相关的操作。
            
            你可以使用文件操作工具执行以下操作：
            - 读取文件内容（read）
            - 写入文件内容（write）
            - 检查文件是否存在（exists）
            - 获取文件信息（info）
            - 列出目录内容（list）
            - 创建目录（mkdir）
            - 删除文件或目录（delete）
            - 复制文件（copy）
            - 移动文件（move）
            
            当用户要求你执行文件操作时，使用文件操作工具。
            始终以友好、专业的方式回应用户。
            解释你的操作过程，并提供清晰的结果。
            
            注意：所有路径都是相对于当前工作目录的。
        """.trimIndent()

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 显式设置 API 密钥
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your_api_key_here")
        }

        // 添加工具
        tools {
            tool(FileOperationToolFactory.createTool())
        }
    }

    // 使用代理
    println("\n开始与代理交互...")

    // 定义示例问题列表
    val exampleQuestions = listOf(
        "请读取文件 ${testFile.absolutePath} 的内容",
        "请创建一个名为 'example' 的目录在 ${tempDir.absolutePath} 下",
        "请在新创建的目录中写入一个名为 'hello.txt' 的文件，内容为 '你好，世界！'",
        "请列出 ${tempDir.absolutePath} 目录中的所有文件和目录",
        "请获取 ${testFile.absolutePath} 文件的详细信息"
    )

    println("\n正在使用示例问题进行演示...")

    // 使用示例问题
    for (question in exampleQuestions) {
        println("\n示例问题: $question")
        println("思考中...")

        try {
            val response = fileOperationAgent.generate(question)
            println("代理: ${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }

    // 清理临时文件
    println("\n清理临时文件...")
    tempDir.deleteRecursively()
    println("临时文件已清理")
}

/**
 * 创建临时目录。
 */
private fun createTempDirectory(): File {
    val tempDir = File(System.getProperty("java.io.tmpdir"), "kastrax-file-example-${System.currentTimeMillis()}")
    tempDir.mkdirs()
    return tempDir
}
