package ai.kastrax.code.tools

import ai.kastrax.core.tools.Tool
import kotlinx.serialization.json.JsonElement

/**
 * 代码工具接口
 *
 * 所有代码相关工具的基础接口
 */
interface CodeTool : Tool {
    /**
     * 获取工具ID
     */
    override val id: String

    /**
     * 获取工具名称
     */
    override val name: String

    /**
     * 获取工具描述
     */
    override val description: String

    /**
     * 获取输入模式
     */
    override val inputSchema: JsonElement

    /**
     * 获取输出模式
     */
    override val outputSchema: JsonElement?

    /**
     * 执行工具
     *
     * @param input 工具输入参数
     * @return 工具执行结果
     */
    override suspend fun execute(input: JsonElement): JsonElement
}

/**
 * 代码分析工具接口
 */
interface CodeAnalysisTool : CodeTool {
    /**
     * 分析代码
     *
     * @param code 代码文本
     * @param language 编程语言
     * @return 分析结果
     */
    suspend fun analyzeCode(code: String, language: String): CodeAnalysisResult
}

/**
 * 代码生成工具接口
 */
interface CodeGenerationTool : CodeTool {
    /**
     * 生成代码
     *
     * @param spec 代码生成规格
     * @return 生成的代码
     */
    suspend fun generateCode(spec: CodeGenerationSpec): String
}

/**
 * 代码搜索工具接口
 */
interface CodeSearchTool : CodeTool {
    /**
     * 搜索代码
     *
     * @param query 查询文本
     * @param scope 搜索范围
     * @return 搜索结果列表
     */
    suspend fun searchCode(query: String, scope: SearchScope): List<CodeSearchResult>
}

/**
 * 代码分析结果
 */
data class CodeAnalysisResult(
    /**
     * 分析概述
     */
    val summary: String,

    /**
     * 详细分析
     */
    val details: Map<String, Any>,

    /**
     * 问题列表
     */
    val issues: List<CodeIssue> = emptyList(),

    /**
     * 建议列表
     */
    val suggestions: List<CodeSuggestion> = emptyList()
)

/**
 * 代码生成规格
 */
data class CodeGenerationSpec(
    /**
     * 描述
     */
    val description: String,

    /**
     * 编程语言
     */
    val language: String,

    /**
     * 上下文代码
     */
    val contextCode: String? = null,

    /**
     * 约束条件
     */
    val constraints: List<String> = emptyList(),

    /**
     * 参数
     */
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * 搜索范围
 */
data class SearchScope(
    /**
     * 包含的文件路径模式
     */
    val includePatterns: List<String> = emptyList(),

    /**
     * 排除的文件路径模式
     */
    val excludePatterns: List<String> = emptyList(),

    /**
     * 限制搜索的文件类型
     */
    val fileTypes: List<String> = emptyList(),

    /**
     * 是否搜索注释
     */
    val searchComments: Boolean = true,

    /**
     * 是否区分大小写
     */
    val caseSensitive: Boolean = false
)

/**
 * 代码搜索结果
 */
data class CodeSearchResult(
    /**
     * 文件路径
     */
    val filePath: String,

    /**
     * 匹配的代码片段
     */
    val snippet: String,

    /**
     * 行号
     */
    val lineNumber: Int,

    /**
     * 相关性分数
     */
    val score: Double,

    /**
     * 匹配的上下文
     */
    val context: String? = null
)

/**
 * 代码问题
 */
data class CodeIssue(
    /**
     * 问题类型
     */
    val type: String,

    /**
     * 问题描述
     */
    val description: String,

    /**
     * 问题位置
     */
    val location: String,

    /**
     * 严重程度
     */
    val severity: IssueSeverity
)

/**
 * 代码建议
 */
data class CodeSuggestion(
    /**
     * 建议类型
     */
    val type: String,

    /**
     * 建议描述
     */
    val description: String,

    /**
     * 建议位置
     */
    val location: String,

    /**
     * 建议代码
     */
    val suggestedCode: String? = null
)

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}
