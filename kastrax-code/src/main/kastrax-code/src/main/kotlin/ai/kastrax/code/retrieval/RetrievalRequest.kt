package ai.kastrax.code.retrieval

/**
 * 检索请求
 *
 * @property query 查询
 * @property options 选项
 */
data class RetrievalRequest(
    val query: String,
    val options: Map<String, Any> = emptyMap()
)

/**
 * 检索结果
 *
 * @property element 元素
 * @property score 分数
 */
data class RetrievalResult(
    val element: CodeElement,
    val score: Double
)

/**
 * 代码元素
 *
 * @property id 标识符
 * @property name 名称
 * @property type 类型
 * @property content 内容
 * @property location 位置
 */
data class CodeElement(
    val id: String,
    val name: String,
    val type: CodeElementType,
    val content: String,
    val location: CodeLocation? = null
)

/**
 * 代码位置
 *
 * @property filePath 文件路径
 * @property startLine 起始行
 * @property startColumn 起始列
 * @property endLine 结束行
 * @property endColumn 结束列
 */
data class CodeLocation(
    val filePath: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
)

/**
 * 代码元素类型
 */
enum class CodeElementType {
    CLASS,
    METHOD,
    FIELD,
    INTERFACE,
    ENUM,
    ANNOTATION,
    PACKAGE,
    FILE,
    FUNCTION,
    VARIABLE,
    PARAMETER,
    IMPORT,
    COMMENT,
    UNKNOWN
}
