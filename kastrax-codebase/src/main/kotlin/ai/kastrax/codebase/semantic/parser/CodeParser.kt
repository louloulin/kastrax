package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Modifier
import ai.kastrax.codebase.semantic.model.Visibility
import java.nio.file.Path

/**
 * 代码解析器接口
 *
 * 定义代码解析器的通用接口，用于解析不同语言的代码文件
 */
interface CodeParser {
    /**
     * 解析代码文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 代码元素（文件级别）
     */
    fun parseFile(filePath: Path, content: String): CodeElement

    /**
     * 获取支持的文件扩展名
     *
     * @return 支持的文件扩展名集合
     */
    fun getSupportedExtensions(): Set<String>

    /**
     * 检查是否支持指定文件
     *
     * @param filePath 文件路径
     * @return 是否支持
     */
    fun supportsFile(filePath: Path): Boolean {
        val extension = filePath.fileName.toString().substringAfterLast('.', "")
        return extension in getSupportedExtensions()
    }
}

/**
 * 代码解析器工厂
 *
 * 用于创建适合特定文件的代码解析器
 */
object CodeParserFactory {
    private val parsers = mutableListOf<CodeParser>()

    /**
     * 注册解析器
     *
     * @param parser 代码解析器
     */
    fun registerParser(parser: CodeParser) {
        parsers.add(parser)
    }

    /**
     * 获取适合指定文件的解析器
     *
     * @param filePath 文件路径
     * @return 代码解析器，如果没有适合的解析器则返回 null
     */
    fun getParser(filePath: Path): CodeParser? {
        return parsers.find { it.supportsFile(filePath) }
    }

    /**
     * 解析代码文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 代码元素（文件级别），如果没有适合的解析器则返回 null
     */
    fun parseFile(filePath: Path, content: String): CodeElement? {
        val parser = getParser(filePath)
        return parser?.parseFile(filePath, content)
    }
}

/**
 * 抽象代码解析器
 *
 * 提供代码解析器的通用实现
 */
abstract class AbstractCodeParser : CodeParser {
    /**
     * 创建文件元素
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 文件元素
     */
    protected fun createFileElement(filePath: Path, content: String): CodeElement {
        val lines = content.lines()
        val endLine = lines.size
        val endColumn = if (endLine > 0) lines.last().length + 1 else 1

        val location = Location(
            filePath = filePath.toString(),
            startLine = 1,
            startColumn = 1,
            endLine = endLine,
            endColumn = endColumn
        )

        return CodeElement(
            id = filePath.toString(),
            name = filePath.fileName.toString(),
            qualifiedName = filePath.toString(),
            type = CodeElementType.FILE,
            location = location,
            language = getLanguageName()
        )
    }

    /**
     * 获取语言名称
     *
     * @return 语言名称
     */
    protected abstract fun getLanguageName(): String
}

/**
 * 解析结果
 *
 * @property element 代码元素
 * @property issues 解析过程中的问题列表
 */
data class ParseResult(
    val element: CodeElement,
    val issues: List<ParseIssue> = emptyList()
)

/**
 * 解析问题
 *
 * @property message 问题描述
 * @property location 问题位置
 * @property severity 问题严重程度
 */
data class ParseIssue(
    val message: String,
    val location: Location,
    val severity: IssueSeverity
)

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    ERROR,
    WARNING,
    INFO
}
