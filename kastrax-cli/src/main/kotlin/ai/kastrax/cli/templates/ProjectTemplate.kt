package ai.kastrax.cli.templates

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

private val logger = KotlinLogging.logger {}

/**
 * 项目模板接口。
 */
interface ProjectTemplate {
    /**
     * 生成项目文件。
     *
     * @param outputDir 输出目录
     * @return 生成的文件数量
     */
    fun generate(outputDir: Path): Int
}

/**
 * 基础项目模板实现。
 *
 * @property projectName 项目名称
 */
abstract class BaseProjectTemplate(protected val projectName: String) : ProjectTemplate {
    protected val templateEngine: Configuration = Configuration(Configuration.VERSION_2_3_32).apply {
        setClassLoaderForTemplateLoading(javaClass.classLoader, "templates")
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
    }

    protected val templateData: Map<String, Any> = mapOf(
        "projectName" to projectName,
        "projectNameLowerCase" to projectName.lowercase(),
        "projectNameCamelCase" to toCamelCase(projectName),
        "projectNamePascalCase" to toPascalCase(projectName),
        "kotlinVersion" to "1.9.0",
        "kastraxVersion" to "0.1.0"
    )

    /**
     * 渲染模板。
     *
     * @param templateName 模板名称
     * @param data 模板数据
     * @return 渲染后的内容
     */
    protected fun renderTemplate(templateName: String, data: Map<String, Any> = templateData): String {
        val template = templateEngine.getTemplate("$templateName.ftl")
        val writer = StringWriter()
        template.process(data, writer)
        return writer.toString()
    }

    /**
     * 创建文件。
     *
     * @param outputDir 输出目录
     * @param relativePath 相对路径
     * @param content 文件内容
     */
    protected fun createFile(outputDir: Path, relativePath: String, content: String) {
        val filePath = outputDir.resolve(relativePath)
        Files.createDirectories(filePath.parent)
        Files.writeString(
            filePath,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    /**
     * 复制资源文件。
     *
     * @param outputDir 输出目录
     * @param resourcePath 资源路径
     * @param relativePath 相对路径
     */
    protected fun copyResource(outputDir: Path, resourcePath: String, relativePath: String) {
        val resource = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        val filePath = outputDir.resolve(relativePath)
        Files.createDirectories(filePath.parent)
        Files.copy(resource, filePath)
    }

    /**
     * 将字符串转换为驼峰命名法。
     *
     * @param str 输入字符串
     * @return 驼峰命名法字符串
     */
    private fun toCamelCase(str: String): String {
        return str.split(Regex("[\\s-_]"))
            .filter { it.isNotEmpty() }
            .mapIndexed { index, s ->
                if (index == 0) s.lowercase()
                else s.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")
    }

    /**
     * 将字符串转换为帕斯卡命名法。
     *
     * @param str 输入字符串
     * @return 帕斯卡命名法字符串
     */
    private fun toPascalCase(str: String): String {
        return str.split(Regex("[\\s-_]"))
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

/**
 * 简单项目模板。
 *
 * @property projectName 项目名称
 */
class SimpleProjectTemplate(projectName: String) : BaseProjectTemplate(projectName) {
    override fun generate(outputDir: Path): Int {
        var fileCount = 0

        // 创建基本项目结构
        createFile(outputDir, "build.gradle.kts", renderTemplate("simple/build.gradle.kts"))
        fileCount++

        createFile(outputDir, "settings.gradle.kts", renderTemplate("simple/settings.gradle.kts"))
        fileCount++

        createFile(outputDir, "gradle.properties", renderTemplate("simple/gradle.properties"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/Main.kt", renderTemplate("simple/Main.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/SimpleAgent.kt", renderTemplate("simple/SimpleAgent.kt"))
        fileCount++

        createFile(outputDir, "src/main/resources/logback.xml", renderTemplate("simple/logback.xml"))
        fileCount++

        createFile(outputDir, "README.md", renderTemplate("simple/README.md"))
        fileCount++

        // 创建 Gradle 包装器
        createFile(outputDir, "gradlew", renderTemplate("common/gradlew"))
        File(outputDir.resolve("gradlew").toUri()).setExecutable(true)
        fileCount++

        createFile(outputDir, "gradlew.bat", renderTemplate("common/gradlew.bat"))
        fileCount++

        createFile(outputDir, "gradle/wrapper/gradle-wrapper.properties",
                  renderTemplate("common/gradle-wrapper.properties"))
        fileCount++

        // 复制 Gradle 包装器 JAR
        copyResource(outputDir, "templates/common/gradle-wrapper.jar",
                    "gradle/wrapper/gradle-wrapper.jar")
        fileCount++

        return fileCount
    }
}

/**
 * RAG 项目模板。
 *
 * @property projectName 项目名称
 */
class RagProjectTemplate(projectName: String) : BaseProjectTemplate(projectName) {
    override fun generate(outputDir: Path): Int {
        var fileCount = 0

        // 创建基本项目结构
        createFile(outputDir, "build.gradle.kts", renderTemplate("rag/build.gradle.kts"))
        fileCount++

        createFile(outputDir, "settings.gradle.kts", renderTemplate("rag/settings.gradle.kts"))
        fileCount++

        createFile(outputDir, "gradle.properties", renderTemplate("rag/gradle.properties"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/Main.kt", renderTemplate("rag/Main.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/RagSystem.kt", renderTemplate("rag/RagSystem.kt"))
        fileCount++

        createFile(outputDir, "src/main/resources/logback.xml", renderTemplate("rag/logback.xml"))
        fileCount++

        createFile(outputDir, "src/main/resources/documents/sample.txt",
                  renderTemplate("rag/sample.txt"))
        fileCount++

        createFile(outputDir, "README.md", renderTemplate("rag/README.md"))
        fileCount++

        // 创建 Gradle 包装器
        createFile(outputDir, "gradlew", renderTemplate("common/gradlew"))
        File(outputDir.resolve("gradlew").toUri()).setExecutable(true)
        fileCount++

        createFile(outputDir, "gradlew.bat", renderTemplate("common/gradlew.bat"))
        fileCount++

        createFile(outputDir, "gradle/wrapper/gradle-wrapper.properties",
                  renderTemplate("common/gradle-wrapper.properties"))
        fileCount++

        // 复制 Gradle 包装器 JAR
        copyResource(outputDir, "templates/common/gradle-wrapper.jar",
                    "gradle/wrapper/gradle-wrapper.jar")
        fileCount++

        return fileCount
    }
}

/**
 * Agent 项目模板。
 *
 * @property projectName 项目名称
 */
class AgentProjectTemplate(projectName: String) : BaseProjectTemplate(projectName) {
    override fun generate(outputDir: Path): Int {
        var fileCount = 0

        // 创建基本项目结构
        createFile(outputDir, "build.gradle.kts", renderTemplate("agent/build.gradle.kts"))
        fileCount++

        createFile(outputDir, "settings.gradle.kts", renderTemplate("agent/settings.gradle.kts"))
        fileCount++

        createFile(outputDir, "gradle.properties", renderTemplate("agent/gradle.properties"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/Main.kt", renderTemplate("agent/Main.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/CustomAgent.kt", renderTemplate("agent/CustomAgent.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/tools/CustomTool.kt",
                  renderTemplate("agent/CustomTool.kt"))
        fileCount++

        createFile(outputDir, "src/main/resources/logback.xml", renderTemplate("agent/logback.xml"))
        fileCount++

        createFile(outputDir, "README.md", renderTemplate("agent/README.md"))
        fileCount++

        // 创建 Gradle 包装器
        createFile(outputDir, "gradlew", renderTemplate("common/gradlew"))
        File(outputDir.resolve("gradlew").toUri()).setExecutable(true)
        fileCount++

        createFile(outputDir, "gradlew.bat", renderTemplate("common/gradlew.bat"))
        fileCount++

        createFile(outputDir, "gradle/wrapper/gradle-wrapper.properties",
                  renderTemplate("common/gradle-wrapper.properties"))
        fileCount++

        // 复制 Gradle 包装器 JAR
        copyResource(outputDir, "templates/common/gradle-wrapper.jar",
                    "gradle/wrapper/gradle-wrapper.jar")
        fileCount++

        return fileCount
    }
}

/**
 * Workflow 项目模板。
 *
 * @property projectName 项目名称
 */
class WorkflowProjectTemplate(projectName: String) : BaseProjectTemplate(projectName) {
    override fun generate(outputDir: Path): Int {
        var fileCount = 0

        // 创建基本项目结构
        createFile(outputDir, "build.gradle.kts", renderTemplate("workflow/build.gradle.kts"))
        fileCount++

        createFile(outputDir, "settings.gradle.kts", renderTemplate("workflow/settings.gradle.kts"))
        fileCount++

        createFile(outputDir, "gradle.properties", renderTemplate("workflow/gradle.properties"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/Main.kt", renderTemplate("workflow/Main.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/CustomWorkflow.kt",
                  renderTemplate("workflow/CustomWorkflow.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/agents/ResearchAgent.kt",
                  renderTemplate("workflow/ResearchAgent.kt"))
        fileCount++

        createFile(outputDir, "src/main/kotlin/agents/WritingAgent.kt",
                  renderTemplate("workflow/WritingAgent.kt"))
        fileCount++

        createFile(outputDir, "src/main/resources/logback.xml", renderTemplate("workflow/logback.xml"))
        fileCount++

        createFile(outputDir, "README.md", renderTemplate("workflow/README.md"))
        fileCount++

        // 创建 Gradle 包装器
        createFile(outputDir, "gradlew", renderTemplate("common/gradlew"))
        File(outputDir.resolve("gradlew").toUri()).setExecutable(true)
        fileCount++

        createFile(outputDir, "gradlew.bat", renderTemplate("common/gradlew.bat"))
        fileCount++

        createFile(outputDir, "gradle/wrapper/gradle-wrapper.properties",
                  renderTemplate("common/gradle-wrapper.properties"))
        fileCount++

        // 复制 Gradle 包装器 JAR
        copyResource(outputDir, "templates/common/gradle-wrapper.jar",
                    "gradle/wrapper/gradle-wrapper.jar")
        fileCount++

        return fileCount
    }
}
