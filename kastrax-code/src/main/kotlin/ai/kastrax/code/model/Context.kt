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
     * 相关性分数
     */
    val relevance: Float,
    
    /**
     * 内容文本
     */
    val content: String
)

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
    val location: Location,
    
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

/**
 * 位置类
 * 
 * 表示代码中的位置，包含行号和列号
 */
data class Location(
    /**
     * 起始行号（从1开始）
     */
    val startLine: Int,
    
    /**
     * 起始列号（从1开始）
     */
    val startColumn: Int,
    
    /**
     * 结束行号（从1开始）
     */
    val endLine: Int,
    
    /**
     * 结束列号（从1开始）
     */
    val endColumn: Int
) {
    companion object {
        /**
         * 创建未知位置
         */
        fun unknown(): Location = Location(0, 0, 0, 0)
    }
}
