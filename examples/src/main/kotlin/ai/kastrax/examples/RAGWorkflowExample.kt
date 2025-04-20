package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.workflow
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.document.WebPageDocumentLoader
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * RAG 工作流示例
 *
 * 这个示例展示了如何将 RAG 系统与工作流引擎集成，创建一个多步骤的研究和报告生成流程。
 */
fun main() = runBlocking {
    // 创建向量存储和嵌入服务
    val vectorStore = InMemoryVectorStore()
    val embeddingService = RandomEmbeddingService(dimensions = 1536)

    // 创建 RAG 系统
    val rag = RAG(vectorStore, embeddingService)

    // 创建文档分割器
    val splitter = RecursiveCharacterTextSplitter(
        chunkSize = 500,
        chunkOverlap = 100
    )

    // 从目录加载文档
    println("从目录加载文档...")
    val docsDir = File("docs")
    if (docsDir.exists() && docsDir.isDirectory) {
        val directoryLoader = DirectoryDocumentLoader(
            directory = docsDir,
            recursive = true,
            fileExtensions = listOf("txt", "md", "html")
        )
        rag.loadDocuments(directoryLoader, splitter)
    }

    // 从网页加载文档
    println("从网页加载文档...")
    val urls = listOf(
        "https://en.wikipedia.org/wiki/Artificial_intelligence",
        "https://en.wikipedia.org/wiki/Machine_learning",
        "https://en.wikipedia.org/wiki/Natural_language_processing"
    )

    for (url in urls) {
        try {
            val webLoader = WebPageDocumentLoader(url)
            rag.loadDocuments(webLoader, splitter)
        } catch (e: Exception) {
            println("无法加载网页: $url - ${e.message}")
        }
    }

    // 创建 OpenAI 提供者
    val openai = openAi(
        model = "gpt-3.5-turbo"
        // API 密钥从环境变量 OPENAI_API_KEY 获取
    )

    // 创建研究代理
    val researchAgent = agent {
        name = "Research Agent"
        instructions = """
            你是一个研究助手，负责从提供的上下文中提取相关信息，回答研究问题。

            请遵循以下准则：
            1. 仅使用提供的上下文信息
            2. 提供详细、全面的研究结果
            3. 引用信息的来源
            4. 组织信息，使其易于理解
            5. 如果上下文中没有足够的信息，请明确指出

            上下文信息：
            {{context}}

            研究问题：
            {{question}}
        """.trimIndent()
        model = openai
    }

    // 创建分析代理
    val analysisAgent = agent {
        name = "Analysis Agent"
        instructions = """
            你是一个分析助手，负责分析研究结果，提供见解和观点。

            请遵循以下准则：
            1. 分析提供的研究结果
            2. 识别关键趋势、模式和见解
            3. 提供深入的分析和观点
            4. 指出研究结果中的任何局限性或不足
            5. 保持客观和公正

            研究结果：
            {{research}}

            研究问题：
            {{question}}
        """.trimIndent()
        model = openai
    }

    // 创建报告生成代理
    val reportAgent = agent {
        name = "Report Generation Agent"
        instructions = """
            你是一个报告生成助手，负责将研究结果和分析整合成一份完整的报告。

            请遵循以下准则：
            1. 创建一份结构良好的报告
            2. 包含执行摘要、引言、研究结果、分析和结论
            3. 使用清晰、专业的语言
            4. 引用信息的来源
            5. 确保报告全面且易于理解

            研究结果：
            {{research}}

            分析：
            {{analysis}}

            研究问题：
            {{question}}
        """.trimIndent()
        model = openai
    }

    // 创建研究工作流
    val researchWorkflow = workflow {
        name = "research-workflow"
        description = "研究和报告生成工作流"

        step(researchAgent) {
            id = "research"
            name = "研究"
            description = "从上下文中提取相关信息"
            variables = mapOf(
                "context" to variable("$.input.context"),
                "question" to variable("$.input.question")
            )
        }

        step(analysisAgent) {
            id = "analysis"
            name = "分析"
            description = "分析研究结果"
            after("research")
            variables = mapOf(
                "research" to variable("$.steps.research.output.text"),
                "question" to variable("$.input.question")
            )
        }

        step(reportAgent) {
            id = "report"
            name = "报告生成"
            description = "生成最终报告"
            after("research", "analysis")
            variables = mapOf(
                "research" to variable("$.steps.research.output.text"),
                "analysis" to variable("$.steps.analysis.output.text"),
                "question" to variable("$.input.question")
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

    // 使用 RAG 工作流回答问题
    while (true) {
        print("\n请输入研究问题（输入 'exit' 退出）: ")
        val question = readLine() ?: ""

        if (question.equals("exit", ignoreCase = true)) {
            break
        }

        // 检索相关上下文
        println("检索相关信息...")
        val context = rag.generateContextWithMetadata(
            query = question,
            limit = 10,
            minScore = 0.5,
            includeMetadata = true
        )

        if (context.isEmpty()) {
            println("没有找到相关信息。")
            continue
        }

        // 准备工作流输入
        val input = mapOf(
            "context" to context,
            "question" to question
        )

        // 执行工作流
        println("\n开始执行研究工作流...")

        // 流式执行并显示进度
        researchWorkflow.streamExecute(input).collect { update ->
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

        // 获取工作流结果
        val result = researchWorkflow.execute(input)

        if (result.success) {
            println("\n研究报告已生成")
            println("报告保存在: ${result.steps["report"]?.output?.get("filePath")}")

            // 显示报告摘要
            val report = result.steps["report"]?.output?.get("text") as? String
            if (!report.isNullOrEmpty()) {
                val summary = report.split("\n\n").firstOrNull() ?: ""
                println("\n报告摘要:")
                println(summary)
            }
        } else {
            println("\n工作流执行失败: ${result.error}")
        }
    }
}
