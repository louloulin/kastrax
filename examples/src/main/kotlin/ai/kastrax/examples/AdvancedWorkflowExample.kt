package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.zod.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    // 创建 OpenAI 提供者
    val openai = openAi(
        model = "gpt-4"
        // API 密钥从环境变量 OPENAI_API_KEY 获取
    )

    // 创建文件读取工具
    val readFileTool = zodTool<String, String> {
        id = "read_file"
        name = "Read File"
        description = "Reads the content of a file"

        inputSchema = stringInput("File path to read").unsafeCast<String, String>()
        outputSchema = stringOutput("File content").unsafeCast<String, String>()

        execute = { filePath ->
            try {
                File(filePath).readText()
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
        }
    }

    // 创建文件写入工具
    val writeFileTool = zodTool<Map<String, String>, String> {
        id = "write_file"
        name = "Write File"
        description = "Writes content to a file"

        inputSchema = objectInput("File write parameters") {
            stringField("filePath", "Path where to write the file")
            stringField("content", "Content to write to the file")
        }.unsafeCast<Map<String, String>, Map<String, String>>()

        outputSchema = stringOutput("Result of the write operation").unsafeCast<String, String>()

        execute = { params ->
            try {
                File(params["filePath"]!!).writeText(params["content"]!!)
                "Successfully wrote to ${params["filePath"]}"
            } catch (e: Exception) {
                "Error writing file: ${e.message}"
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
        model = openai

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
        model = openai
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
        model = openai
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
        model = openai

        tools {
            tool(writeFileTool.toTool())
        }
    }

    // 创建数据分析工作流
    val dataAnalysisWorkflow = workflow {
        name = "data-analysis"
        description = "数据分析与报告生成工作流"

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
    dataAnalysisWorkflow.streamExecute(input).collect { update ->
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
