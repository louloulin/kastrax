package ai.kastrax.code.model

/**
 * 上下文类
 *
 * 表示代码上下文，包含上下文元素和元数据
 */
data class Context(
    /**
     * 上下文元素列表
     */
    val elements: List<ContextElement>,

    /**
     * 查询文本
     */
    val query: String,

    /**
     * 元数据
     */
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 上下文元素类
 *
 * 表示上下文中的一个元素，如代码片段、类、方法等
 */
data class ContextElement(
    /**
     * 代码元素
     */
    val element: CodeElement,

    /**
     * 上下文级别
     */
    val level: ContextLevel,

    /**
     * 相关性
     */
    val relevance: ContextRelevance = ContextRelevance.MEDIUM,

    /**
     * 相关性分数
     */
    val score: Float = 0.0f,

    /**
     * 内容文本
     */
    val content: String
)

/**
 * 上下文相关性枚举
 */
enum class ContextRelevance {
    /**
     * 高相关性
     */
    HIGH,

    /**
     * 中相关性
     */
    MEDIUM,

    /**
     * 低相关性
     */
    LOW,

    /**
     * 主要元素
     */
    PRIMARY
}

/**
 * 上下文级别枚举
 */
enum class ContextLevel {
    /**
     * 文件级别
     */
    FILE,

    /**
     * 类级别
     */
    CLASS,

    /**
     * 方法级别
     */
    METHOD,

    /**
     * 变量级别
     */
    VARIABLE,

    /**
     * 代码块级别
     */
    BLOCK
}

/**
 * 代码元素类
 *
 * 表示代码中的一个元素，如类、方法、变量等
 */
data class CodeElement(
    /**
     * 元素ID
     */
    val id: String,

    /**
     * 元素类型
     */
    val type: CodeElementType,

    /**
     * 元素名称
     */
    val name: String,

    /**
     * 元素路径
     */
    val path: String,

    /**
     * 元素位置
     */
    val location: Location?,

    /**
     * 元素内容
     */
    val content: String,

    /**
     * 元素元数据
     */
    val metadata: Map<String, Any> = emptyMap(),

    /**
     * 子元素
     */
    val children: List<CodeElement> = emptyList()
)

/**
 * 代码元素类型枚举
 */
enum class CodeElementType {
    FILE,
    PACKAGE,
    CLASS,
    INTERFACE,
    ENUM,
    METHOD,
    CONSTRUCTOR,
    FIELD,
    PROPERTY,
    PARAMETER,
    LOCAL_VARIABLE,
    ANNOTATION,
    COMMENT,
    IMPORT,
    BLOCK,
    STATEMENT,
    EXPRESSION
}


