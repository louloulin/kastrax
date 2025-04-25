package ai.kastrax.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

/**
 * 创建新的 KastraX 应用程序的命令。
 */
class CreateCommand : CliktCommand(
    name = "create",
    help = "创建一个新的 KastraX 应用程序"
) {
    private val appName by argument()
        .help("应用程序名称")

    private val template by option("-t", "--template")
        .choice(
            "basic" to "basic",
            "agent" to "agent",
            "workflow" to "workflow",
            "rag" to "rag",
            "zodtool" to "zodtool",
            "network" to "network",
            "api" to "api",
            "advancedtool" to "advancedtool",
            "specializedtool" to "specializedtool",
            "full" to "full"
        )
        .default("basic")
        .help("应用程序模板类型 (basic, agent, workflow, rag, zodtool, network, api, advancedtool, specializedtool, full)")

    private val outputDir by option("-o", "--output-dir")
        .default(".")
        .help("输出目录")

    private val force by option("-f", "--force")
        .flag()
        .help("强制覆盖现有目录")

    private val terminal = Terminal()

    override fun run() {
        val projectDir = File(outputDir, appName)

        // 检查目录是否已存在
        if (projectDir.exists() && !force) {
            terminal.println(TextColors.red("错误: 目录 '$projectDir' 已存在。使用 --force 选项覆盖。"))
            return
        }

        terminal.println(TextColors.brightBlue("创建 KastraX 应用程序: $appName"))
        terminal.println("模板: $template")
        terminal.println("输出目录: ${projectDir.absolutePath}")
        terminal.println()

        // 显示进度条
        val progress = terminal.progressAnimation {
            text("创建应用程序...")
            percentage()
            completed()
            speed("文件/秒")
        }

        progress.start()

        try {
            // 确保目录存在
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }

            // 生成项目文件
            val fileCount = generateProjectFiles(projectDir.toPath(), template)

            progress.update(completed = 100, total = 100)
            progress.stop()

            terminal.println(TextColors.green("✓ 成功创建应用程序 '$appName'"))
            terminal.println("  生成了 $fileCount 个文件")
            terminal.println("  应用程序位置: ${projectDir.absolutePath}")
            terminal.println()
            terminal.println("下一步:")
            terminal.println("  cd ${projectDir.name}")
            terminal.println("  ./gradlew build")
            terminal.println("  kastrax dev")
        } catch (e: Exception) {
            progress.stop()
            logger.error(e) { "创建应用程序时发生错误" }
            terminal.println(TextColors.red("错误: 创建应用程序时发生错误: ${e.message}"))
        }
    }

    /**
     * 生成项目文件。
     */
    private fun generateProjectFiles(projectDir: Path, template: String): Int {
        var fileCount = 0

        // 创建项目结构
        val srcDir = projectDir.resolve("src/main/kotlin")
        val resourcesDir = projectDir.resolve("src/main/resources")
        val testDir = projectDir.resolve("src/test/kotlin")
        val mastraDir = srcDir.resolve("ai/kastrax/app/kastrax")

        srcDir.createDirectories()
        resourcesDir.createDirectories()
        testDir.createDirectories()
        mastraDir.createDirectories()

        // 转换为 File 对象
        val srcDirFile = srcDir.toFile()
        val resourcesDirFile = resourcesDir.toFile()
        val testDirFile = testDir.toFile()
        val mastraDirFile = mastraDir.toFile()

        // 创建 build.gradle.kts
        val buildGradle = projectDir.resolve("build.gradle.kts").toFile()
        buildGradle.writeText("""
            plugins {
                kotlin("jvm") version "1.9.0"
                application
            }

            group = "ai.kastrax.app"
            version = "0.1.0"

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation("ai.kastrax:kastrax-core:0.1.0")
                implementation("ai.kastrax:kastrax-memory-api:0.1.0")
                implementation("ai.kastrax:kastrax-memory-impl:0.1.0")
                implementation("ai.kastrax:kastrax-rag:0.1.0")
                implementation("ai.kastrax:kastrax-zod:0.1.0")
                implementation("ai.kastrax:kastrax-server:0.1.0")

                // 集成
                implementation("ai.kastrax.integrations:kastrax-openai:0.1.0")
                implementation("ai.kastrax.integrations:kastrax-anthropic:0.1.0")

                testImplementation(kotlin("test"))
            }

            tasks.test {
                useJUnitPlatform()
            }

            kotlin {
                jvmToolchain(17)
            }

            application {
                mainClass.set("ai.kastrax.app.MainKt")
            }
        """.trimIndent())
        fileCount++

        // 创建 settings.gradle.kts
        val settingsGradle = projectDir.resolve("settings.gradle.kts").toFile()
        settingsGradle.writeText("""
            rootProject.name = "$appName"
        """.trimIndent())
        fileCount++

        // 创建 .gitignore
        val gitignore = projectDir.resolve(".gitignore").toFile()
        gitignore.writeText("""
            .gradle
            build/
            !gradle/wrapper/gradle-wrapper.jar
            !**/src/main/**/build/
            !**/src/test/**/build/

            ### IntelliJ IDEA ###
            .idea/
            *.iws
            *.iml
            *.ipr
            out/
            !**/src/main/**/out/
            !**/src/test/**/out/

            ### VS Code ###
            .vscode/

            ### Mac OS ###
            .DS_Store

            ### KastraX ###
            .kastrax/
        """.trimIndent())
        fileCount++

        // 创建 Main.kt
        val mainKt = srcDir.resolve("ai/kastrax/app/Main.kt").toFile()
        mainKt.parentFile.mkdirs()
        mainKt.writeText("""
            package ai.kastrax.app

            import ai.kastrax.app.kastrax.agents
            import ai.kastrax.app.kastrax.tools
            import ai.kastrax.app.kastrax.workflows

            fun main() {
                println("KastraX 应用程序已启动")

                // 注册代理、工具和工作流
                agents()
                tools()
                workflows()

                println("准备就绪！使用 'kastrax dev' 启动开发服务器")
            }
        """.trimIndent())
        fileCount++

        // 创建 kastrax/index.kt
        val indexKt = mastraDir.resolve("index.kt").toFile()
        indexKt.writeText("""
            package ai.kastrax.app.kastrax

            // 导出所有代理、工具和工作流
            // 这个文件是 KastraX 的入口点
        """.trimIndent())
        fileCount++

        // 根据模板创建不同的文件
        when (template) {
            "basic" -> fileCount += createBasicTemplate(mastraDir)
            "agent" -> fileCount += createAgentTemplate(mastraDir)
            "workflow" -> fileCount += createWorkflowTemplate(mastraDir)
            "rag" -> fileCount += createRagTemplate(mastraDir)
            "zodtool" -> fileCount += createZodToolTemplate(mastraDir)
            "network" -> fileCount += createNetworkTemplate(mastraDir)
            "api" -> fileCount += createApiTemplate(mastraDir)
            "advancedtool" -> fileCount += createAdvancedToolTemplate(mastraDir)
            "specializedtool" -> fileCount += createSpecializedToolTemplate(mastraDir)
            "full" -> fileCount += createFullTemplate(mastraDir)
        }

        return fileCount
    }

    /**
     * 创建基本模板。
     */
    private fun createBasicTemplate(mastraDir: Path): Int {
        var fileCount = 0

        // 创建 agents/index.kt
        val agentsDir = mastraDir.resolve("agents")
        agentsDir.createDirectories()

        val agentsIndexKt = agentsDir.resolve("index.kt").toFile()
        agentsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent

            /**
             * 注册所有代理。
             */
            fun agents() {
                // 在这里注册你的代理
            }
        """.trimIndent())
        fileCount++

        // 创建 tools/index.kt
        val toolsDir = mastraDir.resolve("tools")
        toolsDir.createDirectories()

        val toolsIndexKt = toolsDir.resolve("index.kt").toFile()
        toolsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool

            /**
             * 注册所有工具。
             */
            fun tools() {
                // 在这里注册你的工具
            }
        """.trimIndent())
        fileCount++

        // 创建 workflows/index.kt
        val workflowsDir = mastraDir.resolve("workflows")
        workflowsDir.createDirectories()

        val workflowsIndexKt = workflowsDir.resolve("index.kt").toFile()
        workflowsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.workflows

            import ai.kastrax.core.workflow.Workflow

            /**
             * 注册所有工作流。
             */
            fun workflows() {
                // 在这里注册你的工作流
            }
        """.trimIndent())
        fileCount++

        return fileCount
    }

    /**
     * 创建代理模板。
     */
    private fun createAgentTemplate(mastraDir: Path): Int {
        var fileCount = createBasicTemplate(mastraDir)

        // 创建示例代理
        val agentsDir = mastraDir.resolve("agents")
        val exampleAgentKt = agentsDir.resolve("ExampleAgent.kt").toFile()
        exampleAgentKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent
            import ai.kastrax.core.agent.agent
            import ai.kastrax.app.kastrax.tools.calculatorTool

            /**
             * 示例代理。
             */
            val exampleAgent = agent("example") {
                description = "一个示例代理"

                tools(calculatorTool)

                systemPrompt("You are an example agent that can use the calculator tool. When users ask math questions, use the calculator tool to compute the result.")
            }
        """.trimIndent())
        fileCount++

        // 更新 agents/index.kt
        val agentsIndexKt = agentsDir.resolve("index.kt").toFile()
        agentsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent

            /**
             * 注册所有代理。
             */
            fun agents() {
                // 注册示例代理
                exampleAgent
            }
        """.trimIndent())

        // 创建示例工具
        val toolsDir = mastraDir.resolve("tools")
        val calculatorToolKt = toolsDir.resolve("CalculatorTool.kt").toFile()
        calculatorToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.tool

            /**
             * 计算器工具。
             */
            val calculatorTool = tool("calculator") {
                description = "一个简单的计算器工具"

                execute { input ->
                    val expression = input.get("expression").asString()
                    val result = evaluateExpression(expression)
                    mapOf("result" to result)
                }
            }

            /**
             * 计算表达式。
             */
            private fun evaluateExpression(expression: String): Double {
                // 简单实现，仅支持加减乘除
                return 42.0
            }
        """.trimIndent())
        fileCount++

        // 更新 tools/index.kt
        val toolsIndexKt = toolsDir.resolve("index.kt").toFile()
        toolsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool

            /**
             * 注册所有工具。
             */
            fun tools() {
                // 注册计算器工具
                calculatorTool
            }
        """.trimIndent())

        return fileCount
    }

    /**
     * 创建工作流模板。
     */
    private fun createWorkflowTemplate(mastraDir: Path): Int {
        var fileCount = createAgentTemplate(mastraDir)

        // 创建示例工作流
        val workflowsDir = mastraDir.resolve("workflows")
        val exampleWorkflowKt = workflowsDir.resolve("ExampleWorkflow.kt").toFile()
        exampleWorkflowKt.writeText("""
            package ai.kastrax.app.kastrax.workflows

            import ai.kastrax.core.workflow.Workflow
            import ai.kastrax.core.workflow.workflow
            import ai.kastrax.app.kastrax.agents.exampleAgent

            /**
             * 示例工作流。
             */
            val exampleWorkflow = workflow("example") {
                description = "一个示例工作流"

                input {
                    field("query", "string", "用户查询")
                }

                step("agent-step") {
                    agent = exampleAgent
                    input = { context ->
                        mapOf("query" to context.input["query"])
                    }
                }

                output { context ->
                    mapOf("result" to context.steps["agent-step"].output)
                }
            }
        """.trimIndent())
        fileCount++

        // 更新 workflows/index.kt
        val workflowsIndexKt = workflowsDir.resolve("index.kt").toFile()
        workflowsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.workflows

            import ai.kastrax.core.workflow.Workflow

            /**
             * 注册所有工作流。
             */
            fun workflows() {
                // 注册示例工作流
                exampleWorkflow
            }
        """.trimIndent())

        return fileCount
    }

    /**
     * 创建 RAG 模板。
     */
    private fun createRagTemplate(mastraDir: Path): Int {
        var fileCount = createAgentTemplate(mastraDir)

        // 创建 rag 目录
        val ragDir = mastraDir.resolve("rag")
        ragDir.createDirectories()

        // 创建示例 RAG 实现
        val ragIndexKt = ragDir.resolve("index.kt").toFile()
        ragIndexKt.writeText("""
            package ai.kastrax.app.kastrax.rag

            import ai.kastrax.rag.RAG
            import ai.kastrax.rag.embedding.OpenAIEmbeddingService
            import ai.kastrax.rag.vectorstore.InMemoryVectorStore

            /**
             * 初始化 RAG 系统。
             */
            fun initRag() {
                // 创建嵌入服务
                val embeddingService = OpenAIEmbeddingService(
                    apiKey = System.getenv("OPENAI_API_KEY") ?: "",
                    model = "text-embedding-ada-002"
                )

                // 创建向量存储
                val vectorStore = InMemoryVectorStore()

                // 创建 RAG 实例
                val rag = RAG(
                    embeddingService = embeddingService,
                    vectorStore = vectorStore
                )

                // 返回 RAG 实例
                return rag
            }
        """.trimIndent())
        fileCount++

        // 创建 RAG 工具
        val toolsDir = mastraDir.resolve("tools")
        val ragToolKt = toolsDir.resolve("RagTool.kt").toFile()
        ragToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.tool
            import ai.kastrax.app.kastrax.rag.initRag

            /**
             * RAG 工具。
             */
            val ragTool = tool("rag") {
                description = "使用 RAG 系统查询知识库"

                execute { input ->
                    val query = input.get("query").asString()
                    val rag = initRag()
                    val results = rag.query(query, topK = 3)

                    mapOf(
                        "results" to results.map { result ->
                            mapOf(
                                "content" to result.content,
                                "score" to result.score
                            )
                        }
                    )
                }
            }
        """.trimIndent())
        fileCount++

        // 更新 tools/index.kt
        val toolsIndexKt = toolsDir.resolve("index.kt").toFile()
        toolsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool

            /**
             * 注册所有工具。
             */
            fun tools() {
                // 注册计算器工具
                calculatorTool

                // 注册 RAG 工具
                ragTool
            }
        """.trimIndent())

        // 创建 RAG 代理
        val agentsDir = mastraDir.resolve("agents")
        val ragAgentKt = agentsDir.resolve("RagAgent.kt").toFile()
        ragAgentKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent
            import ai.kastrax.core.agent.agent
            import ai.kastrax.app.kastrax.tools.ragTool

            /**
             * RAG 代理。
             */
            val ragAgent = agent("rag") {
                description = "一个使用 RAG 系统的代理"

                tools(ragTool)

                systemPrompt("You are a knowledgeable assistant who can use the RAG system to query the knowledge base. When users ask questions, use the RAG tool to query relevant information, then provide accurate answers.")
            }
        """.trimIndent())
        fileCount++

        // 更新 agents/index.kt
        val agentsIndexKt = agentsDir.resolve("index.kt").toFile()
        agentsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent

            /**
             * 注册所有代理。
             */
            fun agents() {
                // 注册示例代理
                exampleAgent

                // 注册 RAG 代理
                ragAgent
            }
        """.trimIndent())

        return fileCount
    }

    /**
     * 创建 ZodTool 模板。
     */
    private fun createZodToolTemplate(mastraDir: Path): Int {
        var fileCount = createBasicTemplate(mastraDir)

        // 创建 zod 目录
        val zodDir = mastraDir.resolve("zod")
        zodDir.createDirectories()

        val zodIndexKt = zodDir.resolve("index.kt").toFile()
        zodIndexKt.writeText("""
            package ai.kastrax.app.kastrax.zod

            import ai.kastrax.zod.*

            /**
             * 导出所有 Zod 模式。
             */

            // 用户模式
            val userSchema = objectInput("User") {
                stringField("id", "User ID") {
                    minLength = 3
                    maxLength = 50
                }
                stringField("name", "User name") {
                    minLength = 2
                    maxLength = 100
                }
                numberField("age", "User age") {
                    min = 0.0
                    max = 120.0
                }
                stringField("email", "Email address", required = false) {
                    email = true
                }
            }

            // 搜索参数模式
            val searchParamsSchema = objectInput("Search parameters") {
                stringField("query", "Search query", required = false)
                numberField("page", "Page number", required = false) {
                    min = 1.0
                }
                numberField("pageSize", "Page size", required = false) {
                    min = 1.0
                    max = 100.0
                }
            }

            // 搜索结果模式
            val searchResultSchema = objectOutput("Search result") {
                arrayField("users", userSchema, "List of users")
                numberField("totalCount", "Total number of users")
                numberField("page", "Current page")
                numberField("pageSize", "Page size")
            }
        """.trimIndent())
        fileCount++

        // 创建 ZodTool 示例
        val toolsDir = mastraDir.resolve("tools")
        val calculatorZodToolKt = toolsDir.resolve("CalculatorZodTool.kt").toFile()
        calculatorZodToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.zodTool
            import ai.kastrax.core.tools.zodToolAsLegacy
            import ai.kastrax.zod.*

            /**
             * 计算器 ZodTool。
             */
            val calculatorZodTool = zodTool<Map<String, Any?>, Double> {
                id = "calculator"
                name = "Calculator"
                description = "A simple calculator tool that evaluates mathematical expressions"

                // 定义输入模式
                inputSchema = objectInput("Calculator input") {
                    stringField("expression", "Mathematical expression to evaluate") {
                        minLength = 1
                        maxLength = 100
                    }
                }

                // 定义输出模式
                outputSchema = numberOutput("Calculation result")

                // 实现执行逻辑
                execute = { input ->
                    val expression = input["expression"] as String
                    evaluateExpression(expression)
                }
            }

            /**
             * 将 ZodTool 转换为传统工具。
             */
            val calculatorLegacyTool = calculatorZodTool.toTool()

            /**
             * 直接创建传统工具。
             */
            val directLegacyTool = zodToolAsLegacy<Map<String, Any?>, Double> {
                id = "direct_calculator"
                name = "Direct Calculator"
                description = "A calculator tool created directly as legacy tool"

                inputSchema = objectInput("Calculator input") {
                    stringField("expression", "Mathematical expression to evaluate")
                }

                outputSchema = numberOutput("Calculation result")

                execute = { input ->
                    val expression = input["expression"] as String
                    evaluateExpression(expression)
                }
            }

            /**
             * 计算表达式。
             */
            private fun evaluateExpression(expression: String): Double {
                // 简单实现，仅支持加减乘除
                return 42.0
            }
        """.trimIndent())
        fileCount++

        // 创建用户搜索 ZodTool
        val userSearchZodToolKt = toolsDir.resolve("UserSearchZodTool.kt").toFile()
        userSearchZodToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.zodTool
            import ai.kastrax.zod.*
            import ai.kastrax.app.kastrax.zod.searchParamsSchema
            import ai.kastrax.app.kastrax.zod.searchResultSchema

            /**
             * 用户数据类。
             */
            data class User(
                val id: String,
                val name: String,
                val age: Int,
                val email: String?
            )

            /**
             * 搜索结果数据类。
             */
            data class SearchResult(
                val users: List<Map<String, Any?>>,
                val totalCount: Int,
                val page: Int,
                val pageSize: Int
            )

            /**
             * 用户搜索 ZodTool。
             */
            val userSearchZodTool = zodTool<Map<String, Any?>, SearchResult> {
                id = "user_search"
                name = "User Search"
                description = "Search for users based on various criteria"

                // 使用共享模式
                inputSchema = searchParamsSchema
                outputSchema = searchResultSchema.transform { output ->
                    SearchResult(
                        users = output["users"] as List<Map<String, Any?>>,
                        totalCount = (output["totalCount"] as Number).toInt(),
                        page = (output["page"] as Number).toInt(),
                        pageSize = (output["pageSize"] as Number).toInt()
                    )
                }

                // 实现执行逻辑
                execute = { params ->
                    // 模拟用户数据
                    val allUsers = listOf(
                        User("user1", "John Doe", 30, "john.doe@example.com"),
                        User("user2", "Jane Smith", 25, null),
                        User("user3", "Bob Johnson", 45, "bob.johnson@example.com")
                    )

                    // 应用搜索过滤器
                    var filteredUsers = allUsers

                    // 按查询过滤
                    val query = params["query"] as String?
                    if (query != null && query.isNotEmpty()) {
                        filteredUsers = filteredUsers.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.id.contains(query, ignoreCase = true)
                        }
                    }

                    // 分页
                    val page = (params["page"] as? Number)?.toInt() ?: 1
                    val pageSize = (params["pageSize"] as? Number)?.toInt() ?: 10

                    val totalCount = filteredUsers.size
                    val paginatedUsers = filteredUsers
                        .drop((page - 1) * pageSize)
                        .take(pageSize)

                    // 转换为输出格式
                    val userMaps = paginatedUsers.map { user ->
                        mapOf(
                            "id" to user.id,
                            "name" to user.name,
                            "age" to user.age,
                            "email" to user.email
                        )
                    }

                    SearchResult(
                        users = userMaps,
                        totalCount = totalCount,
                        page = page,
                        pageSize = pageSize
                    )
                }
            }
        """.trimIndent())
        fileCount++

        // 更新 tools/index.kt
        val toolsIndexKt = toolsDir.resolve("index.kt").toFile()
        toolsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool

            /**
             * 注册所有工具。
             */
            fun tools() {
                // 注册 ZodTool
                calculatorZodTool
                userSearchZodTool

                // 注册传统工具
                calculatorLegacyTool
                directLegacyTool
            }
        """.trimIndent())

        return fileCount
    }

    /**
     * 创建代理网络模板。
     */
    private fun createNetworkTemplate(mastraDir: Path): Int {
        var fileCount = createAgentTemplate(mastraDir)

        // 创建 network 目录
        val networkDir = mastraDir.resolve("network")
        networkDir.createDirectories()

        // 创建网络管理器
        val networkManagerKt = networkDir.resolve("NetworkManager.kt").toFile()
        networkManagerKt.writeText("""
            package ai.kastrax.app.kastrax.network

            import ai.kastrax.core.agent.Agent
            import ai.kastrax.core.agent.AgentNetwork
            import ai.kastrax.app.kastrax.agents.exampleAgent
            import ai.kastrax.app.kastrax.agents.specialistAgent

            /**
             * 代理网络管理器。
             * 负责创建和管理代理网络。
             */
            object NetworkManager {
                /**
                 * 创建主网络。
                 */
                fun createMainNetwork(): AgentNetwork {
                    return AgentNetwork.builder()
                        .addAgent(exampleAgent)
                        .addAgent(specialistAgent)
                        .build()
                }

                /**
                 * 创建专家网络。
                 */
                fun createSpecialistNetwork(): AgentNetwork {
                    return AgentNetwork.builder()
                        .addAgent(specialistAgent)
                        .build()
                }

                /**
                 * 创建动态网络。
                 */
                fun createDynamicNetwork(agents: List<Agent>): AgentNetwork {
                    val builder = AgentNetwork.builder()

                    for (agent in agents) {
                        builder.addAgent(agent)
                    }

                    return builder.build()
                }
            }
        """.trimIndent())
        fileCount++

        // 创建网络路由器
        val networkRouterKt = networkDir.resolve("NetworkRouter.kt").toFile()
        networkRouterKt.writeText("""
            package ai.kastrax.app.kastrax.network

            import ai.kastrax.core.agent.Agent
            import ai.kastrax.core.agent.AgentGenerateOptions
            import ai.kastrax.core.agent.AgentGenerateResponse
            import ai.kastrax.app.kastrax.agents.exampleAgent
            import ai.kastrax.app.kastrax.agents.specialistAgent

            /**
             * 代理网络路由器。
             * 负责将请求路由到适当的代理。
             */
            class NetworkRouter {
                private val agents = mapOf(
                    "example" to exampleAgent,
                    "specialist" to specialistAgent
                )

                /**
                 * 根据输入选择适当的代理。
                 */
                fun selectAgent(input: String): Agent {
                    // 简单实现：根据关键词选择代理
                    return when {
                        input.contains("specialist", ignoreCase = true) -> specialistAgent
                        else -> exampleAgent
                    }
                }

                /**
                 * 路由请求到适当的代理。
                 */
                suspend fun route(input: String, options: AgentGenerateOptions = AgentGenerateOptions()): AgentGenerateResponse {
                    val agent = selectAgent(input)
                    return agent.generate(input, options)
                }

                /**
                 * 根据 ID 获取代理。
                 */
                fun getAgentById(id: String): Agent? {
                    return agents[id]
                }

                /**
                 * 获取所有可用代理。
                 */
                fun getAllAgents(): Map<String, Agent> {
                    return agents
                }
            }
        """.trimIndent())
        fileCount++

        // 创建网络索引
        val networkIndexKt = networkDir.resolve("index.kt").toFile()
        networkIndexKt.writeText("""
            package ai.kastrax.app.kastrax.network

            /**
             * 初始化网络组件。
             */
            fun initNetwork() {
                // 创建主网络
                val mainNetwork = NetworkManager.createMainNetwork()

                // 创建路由器
                val router = NetworkRouter()

                println("Network components initialized")
            }
        """.trimIndent())
        fileCount++

        // 创建专家代理
        val agentsDir = mastraDir.resolve("agents")
        val specialistAgentKt = agentsDir.resolve("SpecialistAgent.kt").toFile()
        val agentTemplate = "package ai.kastrax.app.kastrax.agents\n\n" +
                "import ai.kastrax.core.agent.Agent\n" +
                "import ai.kastrax.core.agent.agent\n" +
                "import ai.kastrax.app.kastrax.tools.calculatorTool\n\n" +
                "/**\n" +
                " * 专家代理。\n" +
                " */\n" +
                "val specialistAgent = agent {\n" +
                "    name = \"专家代理\"\n\n" +
                "    instructions = \"\"\"\n" +
                "        You are a specialist agent with expertise in a specific domain.\n" +
                "        You can provide detailed and accurate information in your area of expertise.\n" +
                "        When users ask questions in your domain, provide comprehensive answers.\n" +
                "    \"\"\".trimIndent()\n\n" +
                "    tools {\n" +
                "        tool(calculatorTool)\n" +
                "    }\n\n" +
                "    defaultGenerateOptions {\n" +
                "        temperature(0.7)\n" +
                "        maxTokens(1000)\n" +
                "    }\n" +
                "}\n"
        specialistAgentKt.writeText(agentTemplate)
        fileCount++

        // 更新 agents/index.kt
        val agentsIndexKt = agentsDir.resolve("index.kt").toFile()
        agentsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent

            /**
             * 注册所有代理。
             */
            fun agents() {
                // 注册示例代理
                exampleAgent

                // 注册专家代理
                specialistAgent
            }
        """.trimIndent())

        // 更新 Main.kt 以初始化网络
        val mainKt = mastraDir.resolve("../Main.kt").toFile()
        if (mainKt.exists()) {
            val mainContent = mainKt.readText()
            val updatedContent = mainContent.replace(
                "import ai.kastrax.app.kastrax.agents\nimport ai.kastrax.app.kastrax.tools\nimport ai.kastrax.app.kastrax.workflows",
                "import ai.kastrax.app.kastrax.agents\nimport ai.kastrax.app.kastrax.tools\nimport ai.kastrax.app.kastrax.workflows\nimport ai.kastrax.app.kastrax.network.initNetwork"
            ).replace(
                "// 注册代理、工具和工作流\n    agents()\n    tools()\n    workflows()",
                "// 注册代理、工具和工作流\n    agents()\n    tools()\n    workflows()\n    \n    // 初始化网络\n    initNetwork()"
            )
            mainKt.writeText(updatedContent)
        }

        return fileCount
    }

    /**
     * 创建 API 模板。
     */
    private fun createApiTemplate(mastraDir: Path): Int {
        var fileCount = createBasicTemplate(mastraDir)

        // 创建 api 目录
        val apiDir = mastraDir.resolve("api")
        apiDir.createDirectories()

        // 创建 API 控制器
        val apiControllerKt = apiDir.resolve("ApiController.kt").toFile()
        apiControllerKt.writeText("""
            package ai.kastrax.app.kastrax.api

            import ai.kastrax.core.agent.AgentGenerateOptions
            import ai.kastrax.app.kastrax.agents.exampleAgent
            import kotlinx.serialization.Serializable

            /**
             * API 控制器。
             * 提供 REST API 端点。
             */
            class ApiController {
                /**
                 * 生成文本响应。
                 */
                suspend fun generateText(request: GenerateTextRequest): GenerateTextResponse {
                    val options = AgentGenerateOptions(
                        temperature = request.temperature,
                        maxTokens = request.maxTokens
                    )

                    val response = exampleAgent.generate(request.prompt, options)

                    return GenerateTextResponse(
                        text = response.text,
                        usage = UsageInfo(
                            promptTokens = response.usage?.promptTokens ?: 0,
                            completionTokens = response.usage?.completionTokens ?: 0,
                            totalTokens = response.usage?.totalTokens ?: 0
                        )
                    )
                }

                /**
                 * 验证请求。
                 */
                fun validateRequest(request: GenerateTextRequest): ValidationResult {
                    val errors = mutableListOf<String>()

                    if (request.prompt.isBlank()) {
                        errors.add("Prompt cannot be empty")
                    }

                    if (request.temperature < 0.0 || request.temperature > 1.0) {
                        errors.add("Temperature must be between 0.0 and 1.0")
                    }

                    if (request.maxTokens <= 0) {
                        errors.add("Max tokens must be greater than 0")
                    }

                    return ValidationResult(
                        valid = errors.isEmpty(),
                        errors = errors
                    )
                }
            }

            /**
             * 生成文本请求。
             */
            @Serializable
            data class GenerateTextRequest(
                val prompt: String,
                val temperature: Double = 0.7,
                val maxTokens: Int = 1000
            )

            /**
             * 生成文本响应。
             */
            @Serializable
            data class GenerateTextResponse(
                val text: String,
                val usage: UsageInfo
            )

            /**
             * 使用信息。
             */
            @Serializable
            data class UsageInfo(
                val promptTokens: Int,
                val completionTokens: Int,
                val totalTokens: Int
            )

            /**
             * 验证结果。
             */
            @Serializable
            data class ValidationResult(
                val valid: Boolean,
                val errors: List<String>
            )
        """.trimIndent())
        fileCount++

        // 创建 API 路由
        val apiRoutesKt = apiDir.resolve("ApiRoutes.kt").toFile()
        apiRoutesKt.writeText("""
            package ai.kastrax.app.kastrax.api

            import io.ktor.server.application.*
            import io.ktor.server.request.*
            import io.ktor.server.response.*
            import io.ktor.server.routing.*
            import io.ktor.http.*

            /**
             * 注册 API 路由。
             */
            fun Application.registerApiRoutes() {
                val apiController = ApiController()

                routing {
                    route("/api") {
                        // 生成文本端点
                        post("/generate") {
                            val request = call.receive<GenerateTextRequest>()

                            // 验证请求
                            val validationResult = apiController.validateRequest(request)
                            if (!validationResult.valid) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("errors" to validationResult.errors)
                                )
                                return@post
                            }

                            // 处理请求
                            val response = apiController.generateText(request)
                            call.respond(response)
                        }

                        // 健康检查端点
                        get("/health") {
                            call.respond(mapOf("status" to "ok"))
                        }
                    }
                }
            }
        """.trimIndent())
        fileCount++

        // 创建 API 索引
        val apiIndexKt = apiDir.resolve("index.kt").toFile()
        apiIndexKt.writeText("""
            package ai.kastrax.app.kastrax.api

            /**
             * 初始化 API 组件。
             */
            fun initApi() {
                // 创建 API 控制器
                val apiController = ApiController()

                println("API components initialized")
            }
        """.trimIndent())
        fileCount++

        // 更新 Main.kt 以初始化 API
        val mainKt = mastraDir.resolve("../Main.kt").toFile()
        if (mainKt.exists()) {
            val mainContent = mainKt.readText()
            val updatedContent = mainContent.replace(
                "import ai.kastrax.app.kastrax.agents\nimport ai.kastrax.app.kastrax.tools\nimport ai.kastrax.app.kastrax.workflows",
                "import ai.kastrax.app.kastrax.agents\nimport ai.kastrax.app.kastrax.tools\nimport ai.kastrax.app.kastrax.workflows\nimport ai.kastrax.app.kastrax.api.initApi"
            ).replace(
                "// 注册代理、工具和工作流\n    agents()\n    tools()\n    workflows()",
                "// 注册代理、工具和工作流\n    agents()\n    tools()\n    workflows()\n    \n    // 初始化 API\n    initApi()"
            )
            mainKt.writeText(updatedContent)
        }

        return fileCount
    }

    /**
     * 创建高级工具模板。
     */
    private fun createAdvancedToolTemplate(mastraDir: Path): Int {
        var fileCount = createBasicTemplate(mastraDir)

        // 创建 tools 目录
        val toolsDir = mastraDir.resolve("tools")
        toolsDir.createDirectories()

        // 创建复合工具
        val compositeToolKt = toolsDir.resolve("CompositeTool.kt").toFile()
        compositeToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.compositeTool

            /**
             * 复合工具示例。
             * 复合工具将多个工具组合成一个工具。
             */
            val compositeToolExample = compositeTool("composite_example") {
                description = "一个复合工具示例"

                // 添加组件工具
                addComponent("calculator") { input ->
                    val expression = input["expression"] as? String ?: "0"
                    val result = evaluateExpression(expression)
                    mapOf("result" to result)
                }

                addComponent("translator") { input ->
                    val text = input["text"] as? String ?: ""
                    val targetLanguage = input["targetLanguage"] as? String ?: "en"
                    val translatedText = translate(text, targetLanguage)
                    mapOf("translatedText" to translatedText)
                }

                // 定义如何处理输入并调用组件
                execute { input ->
                    val operation = input["operation"] as? String ?: "calculate"

                    when (operation) {
                        "calculate" -> {
                            val expression = input["expression"] as? String ?: "0"
                            val calculatorInput = mapOf("expression" to expression)
                            executeComponent("calculator", calculatorInput)
                        }
                        "translate" -> {
                            val text = input["text"] as? String ?: ""
                            val targetLanguage = input["targetLanguage"] as? String ?: "en"
                            val translatorInput = mapOf(
                                "text" to text,
                                "targetLanguage" to targetLanguage
                            )
                            executeComponent("translator", translatorInput as Map<String, Any?>)
                        }
                        else -> mapOf("error" to "不支持的操作: " + (operation ?: "unknown"))
                    }
                }
            }

            /**
             * 计算表达式。
             */
            private fun evaluateExpression(expression: String): Double {
                // 简单实现，仅支持加减乘除
                return 42.0
            }

            /**
             * 翻译文本。
             */
            private fun translate(text: String, targetLanguage: String): String {
                // 简单实现，模拟翻译
                val textValue = text as? String ?: ""
                val targetLanguageValue = targetLanguage as? String ?: "English"
                return "Translated: " + textValue + " (to " + targetLanguageValue + ")"
            }
        """.trimIndent())
        fileCount++

        // 创建链式工具
        val chainToolKt = toolsDir.resolve("ChainTool.kt").toFile()
        chainToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.chainTool

            /**
             * 链式工具示例。
             * 链式工具将多个工具串联起来，前一个工具的输出作为后一个工具的输入。
             */
            val chainToolExample = chainTool("chain_example") {
                description = "一个链式工具示例"

                // 添加链中的步骤
                addStep("extract_numbers") { input ->
                    val text = input["text"] as? String ?: ""
                    val numbers = extractNumbers(text)
                    mapOf("numbers" to numbers)
                }

                addStep("calculate_sum") { input ->
                    val numbers = input["numbers"] as? List<Double> ?: emptyList()
                    val sum = numbers.sum()
                    mapOf("sum" to sum)
                }

                addStep("format_result") { input ->
                    val sum = input["sum"] as? Double ?: 0.0
                    val formattedResult = formatNumber(sum)
                    mapOf("result" to formattedResult)
                }

                // 定义如何处理输入并启动链
                execute { input ->
                    val text = input["text"] as? String ?: ""
                    val initialInput = mapOf("text" to text)
                    executeChain(initialInput)
                }
            }

            /**
             * 从文本中提取数字。
             */
            private fun extractNumbers(text: String): List<Double> {
                // 简单实现，模拟提取数字
                return listOf(1.0, 2.0, 3.0, 4.0, 5.0)
            }

            /**
             * 格式化数字。
             */
            private fun formatNumber(number: Double): String {
                // 简单实现，格式化数字
                return String.format("%.2f", number)
            }
        """.trimIndent())
        fileCount++

        // 创建并行工具
        val parallelToolKt = toolsDir.resolve("ParallelTool.kt").toFile()
        parallelToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.parallelTool

            /**
             * 并行工具示例。
             * 并行工具同时执行多个工具，然后汇总结果。
             */
            val parallelToolExample = parallelTool("parallel_example") {
                description = "一个并行工具示例"

                // 添加并行任务
                addTask("weather") { input ->
                    val location = input["location"] as? String ?: "Beijing"
                    val weather = getWeather(location)
                    mapOf("weather" to weather)
                }

                addTask("news") { input ->
                    val category = input["category"] as? String ?: "technology"
                    val news = getNews(category)
                    mapOf("news" to news)
                }

                addTask("stocks") { input ->
                    val symbol = input["symbol"] as? String ?: "AAPL"
                    val stockPrice = getStockPrice(symbol)
                    mapOf("stockPrice" to stockPrice)
                }

                // 定义如何处理输入并汇总结果
                execute { input ->
                    val location = input["location"] as? String ?: "Beijing"
                    val category = input["category"] as? String ?: "technology"
                    val symbol = input["symbol"] as? String ?: "AAPL"

                    val taskInput = mapOf(
                        "location" to location,
                        "category" to category,
                        "symbol" to symbol
                    )

                    val results = executeParallel(taskInput)

                    // 汇总结果
                    mapOf(
                        "summary" to "Information Dashboard",
                        "weather" to (results["weather"]?.get("weather") ?: "Unknown"),
                        "news" to (results["news"]?.get("news") ?: emptyList<String>()),
                        "stockPrice" to (results["stocks"]?.get("stockPrice") ?: 0.0)
                    )
                }
            }

            /**
             * 获取天气信息。
             */
            private fun getWeather(location: String): String {
                // 简单实现，模拟天气信息
                val locationValue = location as? String ?: "Unknown"
                return "Sunny, 25°C in " + locationValue
            }

            /**
             * 获取新闻。
             */
            private fun getNews(category: String): List<String> {
                // 简单实现，模拟新闻
                return listOf(
                    "Breaking news in " + (category as? String ?: "general"),
                    "Another important story in " + (category as? String ?: "general"),
                    "Latest developments in " + (category as? String ?: "general")
                )
            }

            /**
             * 获取股票价格。
             */
            private fun getStockPrice(symbol: String): Double {
                // 简单实现，模拟股票价格
                return 150.42
            }
        """.trimIndent())
        fileCount++

        // 创建条件工具
        val conditionalToolKt = toolsDir.resolve("ConditionalTool.kt").toFile()
        conditionalToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.conditionalTool

            /**
             * 条件工具示例。
             * 条件工具根据条件选择不同的工具执行。
             */
            val conditionalToolExample = conditionalTool("conditional_example") {
                description = "一个条件工具示例"

                // 添加条件分支
                addCondition("is_number") { input ->
                    val query = input["query"] as? String ?: ""
                    query.toDoubleOrNull() != null
                }

                addCondition("is_question") { input ->
                    val query = input["query"] as? String ?: ""
                    query.trim().endsWith("?")
                }

                // 添加处理器
                addHandler("number_handler") { input ->
                    val query = input["query"] as? String ?: ""
                    val number = query.toDoubleOrNull() ?: 0.0
                    val squared = number * number
                    mapOf("result" to "The square of " + (number ?: 0) + " is " + (squared ?: 0))
                }

                addHandler("question_handler") { input ->
                    val query = input["query"] as? String ?: ""
                    mapOf("result" to "You asked: " + (query ?: "") + " Here's the answer...")
                }

                addHandler("default_handler") { input ->
                    val query = input["query"] as? String ?: ""
                    mapOf("result" to "I received your input: " + (query ?: ""))
                }

                // 定义条件逻辑
                execute { input ->
                    val query = input["query"] as? String ?: ""

                    when {
                        checkCondition("is_number", input) -> executeHandler("number_handler", input)
                        checkCondition("is_question", input) -> executeHandler("question_handler", input)
                        else -> executeHandler("default_handler", input)
                    }
                }
            }
        """.trimIndent())
        fileCount++

        // 更新 tools/index.kt
        val toolsIndexKt = toolsDir.resolve("index.kt").toFile()
        toolsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool

            /**
             * 注册所有工具。
             */
            fun tools() {
                // 注册高级工具
                compositeToolExample
                chainToolExample
                parallelToolExample
                conditionalToolExample
            }
        """.trimIndent())

        return fileCount
    }

    /**
     * 创建专业工具模板。
     */
    private fun createSpecializedToolTemplate(mastraDir: Path): Int {
        var fileCount = createBasicTemplate(mastraDir)

        // 创建 tools 目录
        val toolsDir = mastraDir.resolve("tools")
        toolsDir.createDirectories()

        // 创建数据处理工具
        val dataProcessingToolKt = toolsDir.resolve("DataProcessingTool.kt").toFile()
        dataProcessingToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.dataProcessingTool

            /**
             * 数据处理工具示例。
             * 数据处理工具用于处理和转换数据。
             */
            val dataProcessingToolExample = dataProcessingTool("data_processor") {
                description = "一个数据处理工具示例"

                // 定义数据模式
                defineSchema {
                    inputSchema {
                        field("data", "array", "要处理的数据数组", true)
                        field("operation", "string", "操作类型（sort, filter, transform）", true)
                        field("options", "object", "处理选项", false)
                    }

                    outputSchema {
                        field("result", "array", "处理后的数据数组")
                        field("count", "number", "结果数量")
                        field("processingTime", "number", "处理时间（毫秒）")
                    }
                }

                // 实现执行逻辑
                execute { input ->
                    val data = input["data"] as? List<*> ?: emptyList<Any?>()
                    val operation = input["operation"] as? String ?: "sort"
                    val options = input["options"] as? Map<String, Any?> ?: emptyMap()

                    val startTime = System.currentTimeMillis()

                    val result = when (operation) {
                        "sort" -> sortData(data, options)
                        "filter" -> filterData(data, options)
                        "transform" -> transformData(data, options)
                        else -> data
                    }

                    val endTime = System.currentTimeMillis()
                    val processingTime = endTime - startTime

                    mapOf(
                        "result" to result,
                        "count" to result.size,
                        "processingTime" to processingTime
                    )
                }

                // 添加验证逻辑
                validate { input ->
                    val validationErrors = mutableListOf<String>()

                    if (input["data"] !is List<*>) {
                        validationErrors.add("data 必须是数组")
                    }

                    val operation = input["operation"] as? String
                    if (operation !in listOf("sort", "filter", "transform")) {
                        validationErrors.add("operation 必须是 'sort', 'filter' 或 'transform'")
                    }

                    validationErrors.isEmpty()
                }
            }

            /**
             * 排序数据。
             */
            private fun sortData(data: List<*>, options: Map<String, Any?>): List<*> {
                // 简单实现，模拟数据排序
                val ascending = options["ascending"] as? Boolean ?: true

                return if (data.all { it is Number }) {
                    val numberList = data.map { it as Number }
                    if (ascending) {
                        numberList.sortedBy { it.toDouble() }
                    } else {
                        numberList.sortedByDescending { it.toDouble() }
                    }
                } else if (data.all { it is String }) {
                    val stringList = data.map { it as String }
                    if (ascending) {
                        stringList.sorted()
                    } else {
                        stringList.sortedDescending()
                    }
                } else {
                    data
                }
            }

            /**
             * 过滤数据。
             */
            private fun filterData(data: List<*>, options: Map<String, Any?>): List<*> {
                // 简单实现，模拟数据过滤
                val minValue = options["minValue"] as? Number
                val maxValue = options["maxValue"] as? Number

                return if (data.all { it is Number }) {
                    val numberList = data.map { it as Number }
                    numberList.filter { number ->
                        val value = number.toDouble()
                        (minValue == null || value >= minValue.toDouble()) &&
                        (maxValue == null || value <= maxValue.toDouble())
                    }
                } else {
                    data
                }
            }

            /**
             * 转换数据。
             */
            private fun transformData(data: List<*>, options: Map<String, Any?>): List<*> {
                // 简单实现，模拟数据转换
                val operation = options["transformOperation"] as? String ?: "multiply"
                val factor = options["factor"] as? Number ?: 1

                return if (data.all { it is Number }) {
                    val numberList = data.map { it as Number }
                    when (operation) {
                        "multiply" -> numberList.map { it.toDouble() * factor.toDouble() }
                        "add" -> numberList.map { it.toDouble() + factor.toDouble() }
                        "power" -> numberList.map { Math.pow(it.toDouble(), factor.toDouble()) }
                        else -> numberList
                    }
                } else {
                    data
                }
            }
        """.trimIndent())
        fileCount++

        // 创建 API 集成工具
        val apiIntegrationToolKt = toolsDir.resolve("ApiIntegrationTool.kt").toFile()
        apiIntegrationToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.apiIntegrationTool

            /**
             * API 集成工具示例。
             * API 集成工具用于调用外部 API。
             */
            val apiIntegrationToolExample = apiIntegrationTool("weather_api") {
                description = "一个天气 API 集成工具示例"

                // 定义 API 配置
                baseUrl = "https://api.example.com/weather"
                defaultHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json"
                )

                // 定义端点
                endpoint("current") {
                    path = "/current"
                    method = "GET"
                    description = "获取当前天气"

                    params {
                        param("location", "string", "位置（城市名或坐标）", true)
                        param("units", "string", "单位（metric 或 imperial）", false)
                    }
                }

                endpoint("forecast") {
                    path = "/forecast"
                    method = "GET"
                    description = "获取天气预报"

                    params {
                        param("location", "string", "位置（城市名或坐标）", true)
                        param("days", "number", "预报天数", false)
                        param("units", "string", "单位（metric 或 imperial）", false)
                    }
                }

                // 实现执行逻辑
                execute { input ->
                    val endpointName = input["endpoint"] as? String ?: "current"
                    val params = input["params"] as? Map<String, Any?> ?: emptyMap()

                    // 模拟 API 调用
                    when (endpointName) {
                        "current" -> {
                            val location = params["location"] as? String ?: "Beijing"
                            val units = params["units"] as? String ?: "metric"

                            mapOf(
                                "location" to location,
                                "temperature" to 25.5,
                                "humidity" to 60,
                                "conditions" to "Sunny",
                                "units" to units
                            )
                        }
                        "forecast" -> {
                            val location = params["location"] as? String ?: "Beijing"
                            val days = (params["days"] as? Number)?.toInt() ?: 5
                            val units = params["units"] as? String ?: "metric"

                            mapOf(
                                "location" to location,
                                "forecast" to List(days) { day ->
                                    mapOf(
                                        "day" to day + 1,
                                        "temperature" to (20 + day * 0.5),
                                        "conditions" to if (day % 2 == 0) "Sunny" else "Cloudy"
                                    )
                                },
                                "units" to units
                            )
                        }
                        else -> mapOf("error" to "未知端点: " + (endpointName ?: "unknown"))
                    }
                }

                // 实现验证逻辑
                validate { input ->
                    val endpointName = input["endpoint"] as? String
                    val params = input["params"] as? Map<String, Any?>

                    if (endpointName !in listOf("current", "forecast")) {
                        return@validate false
                    }

                    if (params == null) {
                        return@validate false
                    }

                    if (params["location"] == null) {
                        return@validate false
                    }

                    true
                }
            }
        """.trimIndent())
        fileCount++

        // 创建文件操作工具
        val fileOperationToolKt = toolsDir.resolve("FileOperationTool.kt").toFile()
        fileOperationToolKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool
            import ai.kastrax.core.tools.fileOperationTool
            import java.io.File
            import java.nio.file.Files
            import java.nio.file.Paths

            /**
             * 文件操作工具示例。
             * 文件操作工具用于读取、写入和管理文件。
             */
            val fileOperationToolExample = fileOperationTool("file_manager") {
                description = "一个文件操作工具示例"

                // 定义根目录
                rootDirectory = "./data"

                // 定义允许的操作
                allowedOperations = listOf("read", "write", "list", "delete", "info")

                // 实现执行逻辑
                execute { input ->
                    val operation = input["operation"] as? String ?: "list"
                    val path = input["path"] as? String ?: "/"
                    val content = input["content"] as? String
                    val options = input["options"] as? Map<String, Any?> ?: emptyMap()

                    // 解析路径，确保安全
                    val safePath = getSafePath(path)

                    when (operation) {
                        "read" -> readFile(safePath)
                        "write" -> writeFile(safePath, content ?: "")
                        "list" -> listFiles(safePath, options)
                        "delete" -> deleteFile(safePath)
                        "info" -> getFileInfo(safePath)
                        else -> mapOf("error" to "不支持的操作: " + (operation ?: "unknown"))
                    }
                }

                // 实现验证逻辑
                validate { input ->
                    val operation = input["operation"] as? String
                    val path = input["path"] as? String

                    if (operation !in allowedOperations) {
                        return@validate false
                    }

                    if (path == null) {
                        return@validate false
                    }

                    if (operation == "write" && input["content"] == null) {
                        return@validate false
                    }

                    true
                }
            }

            /**
             * 获取安全的文件路径。
             */
            private fun getSafePath(path: String): String {
                // 防止路径遍历攻击
                val normalizedPath = path.replace("\\..", "").replace("../", "")
                return normalizedPath.trim('/')
            }

            /**
             * 读取文件。
             */
            private fun readFile(path: String): Map<String, Any?> {
                // 简单实现，模拟文件读取
                return mapOf(
                    "content" to "This is the content of file: " + (path ?: "unknown"),
                    "path" to (path ?: "unknown"),
                    "size" to 1024,
                    "lastModified" to System.currentTimeMillis()
                )
            }

            /**
             * 写入文件。
             */
            private fun writeFile(path: String, content: String): Map<String, Any?> {
                // 简单实现，模拟文件写入
                return mapOf(
                    "success" to true,
                    "path" to (path ?: "unknown"),
                    "size" to content.length,
                    "lastModified" to System.currentTimeMillis()
                )
            }

            /**
             * 列出文件。
             */
            private fun listFiles(path: String, options: Map<String, Any?>): Map<String, Any?> {
                // 简单实现，模拟文件列表
                val recursive = options["recursive"] as? Boolean ?: false

                val files = listOf(
                    mapOf(
                        "name" to "file1.txt",
                        "path" to (path ?: "") + "/file1.txt",
                        "type" to "file",
                        "size" to 1024
                    ),
                    mapOf(
                        "name" to "file2.txt",
                        "path" to (path ?: "") + "/file2.txt",
                        "type" to "file",
                        "size" to 2048
                    ),
                    mapOf(
                        "name" to "subdir",
                        "path" to (path ?: "") + "/subdir",
                        "type" to "directory"
                    )
                )

                return mapOf(
                    "files" to files,
                    "path" to path,
                    "count" to files.size
                )
            }

            /**
             * 删除文件。
             */
            private fun deleteFile(path: String): Map<String, Any?> {
                // 简单实现，模拟文件删除
                return mapOf(
                    "success" to true,
                    "path" to path
                )
            }

            /**
             * 获取文件信息。
             */
            private fun getFileInfo(path: String): Map<String, Any?> {
                // 简单实现，模拟文件信息
                return mapOf(
                    "name" to path.substringAfterLast('/'),
                    "path" to path,
                    "type" to "file",
                    "size" to 1024,
                    "lastModified" to System.currentTimeMillis(),
                    "isReadable" to true,
                    "isWritable" to true
                )
            }
        """.trimIndent())
        fileCount++

        // 更新 tools/index.kt
        val toolsIndexKt = toolsDir.resolve("index.kt").toFile()
        toolsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.tools

            import ai.kastrax.core.tools.Tool

            /**
             * 注册所有工具。
             */
            fun tools() {
                // 注册专业工具
                dataProcessingToolExample
                apiIntegrationToolExample
                fileOperationToolExample
            }
        """.trimIndent())

        return fileCount
    }

    /**
     * 创建完整模板。
     */
    private fun createFullTemplate(mastraDir: Path): Int {
        var fileCount = createRagTemplate(mastraDir)

        // 创建 ZodTool
        fileCount += createZodToolTemplate(mastraDir)

        // 创建高级工具
        fileCount += createAdvancedToolTemplate(mastraDir)

        // 创建专业工具
        fileCount += createSpecializedToolTemplate(mastraDir)

        // 创建代理网络
        fileCount += createNetworkTemplate(mastraDir)

        // 创建 API
        fileCount += createApiTemplate(mastraDir)

        // 创建工作流
        fileCount += createWorkflowTemplate(mastraDir)

        // 创建记忆系统
        val memoryDir = mastraDir.resolve("memory")
        memoryDir.createDirectories()

        val memoryIndexKt = memoryDir.resolve("index.kt").toFile()
        memoryIndexKt.writeText("""
            package ai.kastrax.app.kastrax.memory

            import ai.kastrax.memory.api.Memory
            import ai.kastrax.memory.impl.MemoryBuilderImpl

            /**
             * 初始化记忆系统。
             */
            fun initMemory(): Memory {
                return MemoryBuilderImpl()
                    .withInMemoryStorage()
                    .build()
            }
        """.trimIndent())
        fileCount++

        // 创建记忆代理
        val agentsDir = mastraDir.resolve("agents")
        val memoryAgentKt = agentsDir.resolve("MemoryAgent.kt").toFile()
        memoryAgentKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent
            import ai.kastrax.core.agent.agent
            import ai.kastrax.app.kastrax.memory.initMemory

            /**
             * 记忆代理。
             */
            val memoryAgent = agent("memory") {
                description = "一个带有记忆系统的代理"

                memory(initMemory())

                systemPrompt("You are an assistant with memory capabilities who can remember conversation history with users. Use your memory system to provide a coherent conversation experience.")
            }
        """.trimIndent())
        fileCount++

        // 更新 agents/index.kt
        val agentsIndexKt = agentsDir.resolve("index.kt").toFile()
        agentsIndexKt.writeText("""
            package ai.kastrax.app.kastrax.agents

            import ai.kastrax.core.agent.Agent

            /**
             * 注册所有代理。
             */
            fun agents() {
                // 注册示例代理
                exampleAgent

                // 注册 RAG 代理
                ragAgent

                // 注册记忆代理
                memoryAgent
            }
        """.trimIndent())

        // 创建代理网络
        val networkDir = mastraDir.resolve("network")
        networkDir.createDirectories()

        val networkIndexKt = networkDir.resolve("index.kt").toFile()
        networkIndexKt.writeText("""
            package ai.kastrax.app.kastrax.network

            import ai.kastrax.core.agent.AgentNetwork
            import ai.kastrax.app.kastrax.agents.exampleAgent
            import ai.kastrax.app.kastrax.agents.ragAgent
            import ai.kastrax.app.kastrax.agents.memoryAgent

            /**
             * 创建代理网络。
             */
            val agentNetwork = AgentNetwork.builder()
                .addAgent(exampleAgent)
                .addAgent(ragAgent)
                .addAgent(memoryAgent)
                .build()
        """.trimIndent())
        fileCount++

        return fileCount
    }
}
