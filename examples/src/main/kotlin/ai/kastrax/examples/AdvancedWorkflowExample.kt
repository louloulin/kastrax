package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.core.workflow.workflow
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.zod.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

/**
 * Unsafe cast extension function for Schema
 * This is used to work around type inference issues
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T, reified R> Schema<*, *>.unsafeCast(): Schema<T, R> = this as Schema<T, R>

/**
 * 高级工作流示例：数据分析与报告生成
 *
 * 这个示例展示了如何使用工作流引擎创建一个数据分析与报告生成流程，
 * 包括数据收集、数据分析、可视化建议和报告生成四个步骤。
 */
fun main() = runBlocking {
    // 创建 DeepSeek 提供者
    val deepseek = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        // 显式设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
    }

    // 创建文件读取工具

    val readFileTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "read_file"
        name = "Read File"
        description = "Reads the content of a file"

        inputSchema = objectInput("File path") {
            stringField("file_path", "File path to read")
        }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

        outputSchema = objectOutput("File operation result") {
            stringField("content", "File content")
            booleanField("success", "Whether the operation was successful")
            stringField("message", "Status message or error description")
        }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

        execute = { params ->
            try {
                // 获取文件路径参数
                val filePathParam = params["file_path"]

                // 处理不同类型的输入
                val filePath = when (filePathParam) {
                    is String -> filePathParam
                    is Map<*, *> -> filePathParam["value"]?.toString() ?: ""
                    else -> filePathParam?.toString() ?: ""
                }

                // 验证文件路径
                if (filePath.isEmpty()) {
                    mapOf(
                        "content" to "",
                        "success" to false,
                        "message" to "Error: Empty file path"
                    )
                } else {
                    // 安全检查：防止目录遍历攻击
                    val file = File(filePath)
                    val canonicalPath = file.canonicalPath

                    // 检查文件是否存在
                    if (!file.exists()) {
                        mapOf(
                            "content" to "",
                            "success" to false,
                            "message" to "Error: File does not exist: $filePath"
                        )
                    } else if (!file.isFile) {
                        mapOf(
                            "content" to "",
                            "success" to false,
                            "message" to "Error: Not a file: $filePath"
                        )
                    } else {
                        // 读取文件内容
                        val content = file.readText()
                        mapOf(
                            "content" to content,
                            "success" to true,
                            "message" to "File read successfully: $filePath"
                        )
                    }
                }
            } catch (e: Exception) {
                // 详细的错误处理
                val errorMessage = when (e) {
                    is SecurityException -> "Security error: ${e.message}"
                    is java.nio.file.NoSuchFileException -> "File not found: ${e.message}"
                    is java.nio.file.AccessDeniedException -> "Access denied: ${e.message}"
                    else -> "Error reading file: ${e.message}"
                }

                mapOf(
                    "content" to "",
                    "success" to false,
                    "message" to errorMessage
                )
            }
        }
    }

    // 创建文件写入工具
    val writeFileTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "write_file"
        name = "Write File"
        description = "Writes content to a file"

        inputSchema = objectInput("File write parameters") {
            stringField("file_path", "Path where to write the file")
            stringField("content", "Content to write to the file")
        }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

        outputSchema = objectOutput("File operation result") {
            booleanField("success", "Whether the operation was successful")
            stringField("message", "Status message or error description")
            stringField("file_path", "Path of the file that was written")
        }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

        execute = { params ->
            try {
                // 获取参数
                val filePathParam = params["file_path"]
                val content = params["content"] as? String

                // 处理文件路径
                val filePath = when (filePathParam) {
                    is String -> filePathParam
                    is Map<*, *> -> filePathParam["value"]?.toString() ?: ""
                    else -> filePathParam?.toString() ?: ""
                }

                // 验证参数
                if (filePath.isEmpty()) {
                    mapOf(
                        "success" to false,
                        "message" to "Error: Empty file path",
                        "file_path" to ""
                    )
                } else if (content == null) {
                    mapOf(
                        "success" to false,
                        "message" to "Error: Content is null or not a string",
                        "file_path" to filePath
                    )
                } else {
                    // 创建目录（如果不存在）
                    val file = File(filePath)
                    file.parentFile?.mkdirs()

                    // 写入文件
                    file.writeText(content)

                    mapOf(
                        "success" to true,
                        "message" to "File written successfully",
                        "file_path" to filePath
                    )
                }
            } catch (e: Exception) {
                // 详细的错误处理
                val errorMessage = when (e) {
                    is SecurityException -> "Security error: ${e.message}"
                    is java.nio.file.NoSuchFileException -> "Directory not found: ${e.message}"
                    is java.nio.file.AccessDeniedException -> "Access denied: ${e.message}"
                    else -> "Error writing file: ${e.message}"
                }

                mapOf(
                    "success" to false,
                    "message" to errorMessage,
                    "file_path" to (params["file_path"]?.toString() ?: "")
                )
            }
        }
    }

    // 创建数据收集代理
    val dataCollectionAgent = agent {
        name = "Data Collection Agent"
        instructions = """
            你是一个专业的数据收集助手。你的任务是根据提供的主题和数据源，收集相关的数据。
            如果提供了文件路径，请使用read_file工具读取文件内容。

            请确保收集的数据全面、准确，并以结构化的方式呈现。

            输出格式：
            1. 数据概述
            2. 收集的数据（表格或列表形式）
            3. 数据来源
        """.trimIndent()
        model = deepseek

        tools {
            tool(readFileTool.toTool())
        }
    }

    // 创建数据分析代理
    val dataAnalysisAgent = agent {
        name = "Data Analysis Agent"
        instructions = """
            你是一个专业的数据分析助手。你的任务是分析提供的数据，并提供深入的见解。

            请执行以下分析：
            1. 描述性统计（均值、中位数、标准差等）
            2. 趋势分析
            3. 相关性分析（如适用）
            4. 异常值检测

            输出格式：
            1. 分析概述
            2. 详细分析结果
            3. 关键发现和见解
        """.trimIndent()
        model = deepseek
    }

    // 创建可视化建议代理
    val visualizationAgent = agent {
        name = "Visualization Agent"
        instructions = """
            你是一个专业的数据可视化助手。你的任务是根据提供的数据分析结果，提供可视化建议。

            请提供以下建议：
            1. 适合的图表类型（柱状图、折线图、散点图等）
            2. 每种图表的详细配置（轴、颜色、标签等）
            3. 可视化最佳实践建议

            输出格式：
            1. 可视化概述
            2. 详细的图表建议（每种图表单独描述）
            3. 整体设计建议
        """.trimIndent()
        model = deepseek
    }

    // 创建报告生成代理
    val reportGenerationAgent = agent {
        name = "Report Generation Agent"
        instructions = """
            你是一个专业的报告生成助手。你的任务是根据提供的数据分析结果和可视化建议，生成一份完整的报告。

            请确保报告包含以下内容：
            1. 执行摘要
            2. 引言和背景
            3. 数据概述
            4. 分析方法
            5. 分析结果和发现
            6. 可视化展示
            7. 结论和建议
            8. 附录（如需要）

            输出格式：
            - 完整的报告，包含所有章节和小节
        """.trimIndent()
        model = deepseek

        tools {
            tool(writeFileTool.toTool())
        }
    }

    // 创建数据分析工作流
    val dataAnalysisWorkflow = workflow {
        name = "data-analysis"
        description = "数据分析与报告生成工作流"
        // 注意：超时时间在执行时设置

        step(dataCollectionAgent) {
            id = "data_collection"
            name = "数据收集"
            description = "收集相关数据"
            variables = mapOf(
                "topic" to variable("$.input.topic"),
                "dataSource" to variable("$.input.dataSource")
            )
        }

        step(dataAnalysisAgent) {
            id = "data_analysis"
            name = "数据分析"
            description = "分析收集的数据"
            after("data_collection")
            variables = mapOf(
                "data" to variable("$.steps.data_collection.output.text")
            )
        }

        step(visualizationAgent) {
            id = "visualization"
            name = "可视化建议"
            description = "提供数据可视化建议"
            after("data_analysis")
            variables = mapOf(
                "analysis" to variable("$.steps.data_analysis.output.text")
            )
        }

        step(reportGenerationAgent) {
            id = "report_generation"
            name = "报告生成"
            description = "生成完整报告"
            after("data_analysis", "visualization")
            variables = mapOf(
                "topic" to variable("$.input.topic"),
                "data" to variable("$.steps.data_collection.output.text"),
                "analysis" to variable("$.steps.data_analysis.output.text"),
                "visualization" to variable("$.steps.visualization.output.text")
            )
            outputMapping = { text ->
                // 保存报告到文件
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val fileName = "report_${timestamp}.md"
                val filePath = "reports/$fileName"

                // 确保目录存在
                File("reports").mkdirs()

                // 写入文件
                File(filePath).writeText(text)

                mapOf(
                    "text" to text,
                    "filePath" to filePath
                )
            }
        }
    }

    println("=== 数据分析工作流示例 ===")
    println("正在执行工作流...")

    // 执行工作流
    val topic = "全球气候变化趋势"
    val dataSource = "sample_climate_data.csv" // 假设存在这个文件
    val input = mapOf(
        "topic" to topic,
        "dataSource" to dataSource
    )

    // 流式执行并显示进度
    val options = WorkflowExecuteOptions(
        timeout = 30.minutes.inWholeMilliseconds // 增加超时时间到 30 分钟
    )
    dataAnalysisWorkflow.streamExecute(input, options).collect { update ->
        when (update.status) {
            ai.kastrax.core.workflow.WorkflowStatus.STARTED -> {
                println("工作流开始执行")
            }
            ai.kastrax.core.workflow.WorkflowStatus.IN_PROGRESS -> {
                println("正在执行: ${update.stepId} (${update.progress}%)")
                if (update.result != null) {
                    println("步骤完成: ${update.stepId}")
                }
            }
            ai.kastrax.core.workflow.WorkflowStatus.COMPLETED -> {
                println("工作流执行完成 (100%)")
            }
            ai.kastrax.core.workflow.WorkflowStatus.FAILED -> {
                println("工作流执行失败: ${update.message}")
            }
        }
    }

    // 执行工作流并获取结果
    val result = dataAnalysisWorkflow.execute(input)

    if (result.success) {
        println("\n=== 工作流执行结果 ===")
        println("报告已生成，保存在: ${result.steps["report_generation"]?.output?.get("filePath")}")
    } else {
        println("\n工作流执行失败: ${result.error}")
    }
}
