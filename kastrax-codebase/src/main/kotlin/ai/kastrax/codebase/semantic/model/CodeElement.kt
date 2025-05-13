package ai.kastrax.codebase.semantic.model

import java.nio.file.Path

/**
 * 代码元素类型
 */
enum class CodeElementType {
    FILE,
    PACKAGE,
    IMPORT,
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    METHOD,
    CONSTRUCTOR,
    FIELD,
    PROPERTY,
    PARAMETER,
    LOCAL_VARIABLE,
    LAMBDA,
    BLOCK,
    STATEMENT,
    EXPRESSION,
    COMMENT,
    FUNCTION,
    UNKNOWN
}

/**
 * 代码元素可见性
 */
enum class Visibility {
    PUBLIC,
    PROTECTED,
    PRIVATE,
    PACKAGE_PRIVATE,
    INTERNAL,
    UNKNOWN
}

/**
 * 代码元素修饰符
 */
enum class Modifier {
    STATIC,
    FINAL,
    ABSTRACT,
    SYNCHRONIZED,
    VOLATILE,
    TRANSIENT,
    NATIVE,
    STRICTFP,
    DEFAULT,
    SEALED,
    OPEN,
    CONST,
    OVERRIDE,
    LATEINIT,
    SUSPEND,
    TAILREC,
    EXTERNAL,
    INLINE,
    INFIX,
    OPERATOR,
    DATA,
    INNER,
    COMPANION,
    FUN,
    VALUE,
    VIRTUAL,
    CLASS_METHOD,
    PROPERTY,
    READONLY,
    ASYNC,
    UNKNOWN
}

/**
 * 代码元素位置
 *
 * @property filePath 文件路径
 * @property startLine 起始行
 * @property startColumn 起始列
 * @property endLine 结束行
 * @property endColumn 结束列
 */
data class Location(
    val filePath: Path,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
) {
    /**
     * 检查位置是否有效
     *
     * @return 是否有效
     */
    fun isValid(): Boolean {
        return startLine > 0 && startColumn > 0 && endLine >= startLine &&
               (endLine > startLine || endColumn >= startColumn)
    }

    /**
     * 检查位置是否包含另一个位置
     *
     * @param other 另一个位置
     * @return 是否包含
     */
    fun contains(other: Location): Boolean {
        if (filePath != other.filePath) {
            return false
        }

        return (startLine < other.startLine || (startLine == other.startLine && startColumn <= other.startColumn)) &&
               (endLine > other.endLine || (endLine == other.endLine && endColumn >= other.endColumn))
    }

    /**
     * 检查位置是否与另一个位置重叠
     *
     * @param other 另一个位置
     * @return 是否重叠
     */
    fun overlaps(other: Location): Boolean {
        if (filePath != other.filePath) {
            return false
        }

        return !(endLine < other.startLine || (endLine == other.startLine && endColumn < other.startColumn) ||
                startLine > other.endLine || (startLine == other.endLine && startColumn > other.endColumn))
    }

    override fun toString(): String {
        return "$filePath:$startLine:$startColumn-$endLine:$endColumn"
    }
}

/**
 * 代码元素
 *
 * 代表代码中的一个语义单元，如类、方法、字段等
 *
 * @property id 唯一标识符
 * @property name 名称
 * @property qualifiedName 限定名
 * @property type 类型
 * @property location 位置
 * @property visibility 可见性
 * @property modifiers 修饰符集合
 * @property parent 父元素
 * @property children 子元素列表
 * @property documentation 文档注释
 * @property language 编程语言
 * @property metadata 元数据
 */
data class CodeElement(
    val id: String,
    val name: String,
    val qualifiedName: String,
    val type: CodeElementType,
    val location: Location,
    val visibility: Visibility = Visibility.UNKNOWN,
    val modifiers: Set<Modifier> = emptySet(),
    val parent: CodeElement? = null,
    val children: MutableList<CodeElement> = mutableListOf(),
    val documentation: String = "",
    val language: String = "",
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 添加子元素
     *
     * @param child 子元素
     */
    fun addChild(child: CodeElement) {
        children.add(child)
    }

    /**
     * 移除子元素
     *
     * @param child 子元素
     * @return 是否成功移除
     */
    fun removeChild(child: CodeElement): Boolean {
        return children.remove(child)
    }

    /**
     * 查找子元素
     *
     * @param predicate 谓词函数
     * @return 符合条件的子元素，如果没有则返回 null
     */
    fun findChild(predicate: (CodeElement) -> Boolean): CodeElement? {
        return children.find(predicate)
    }

    /**
     * 查找所有符合条件的子元素
     *
     * @param predicate 谓词函数
     * @return 符合条件的子元素列表
     */
    fun findChildren(predicate: (CodeElement) -> Boolean): List<CodeElement> {
        return children.filter(predicate)
    }

    /**
     * 获取所有子元素（递归）
     *
     * @return 所有子元素列表
     */
    fun getAllChildren(): List<CodeElement> {
        val result = mutableListOf<CodeElement>()

        for (child in children) {
            result.add(child)
            result.addAll(child.getAllChildren())
        }

        return result
    }

    /**
     * 获取所有祖先元素
     *
     * @return 所有祖先元素列表
     */
    fun getAllAncestors(): List<CodeElement> {
        val result = mutableListOf<CodeElement>()
        var current = parent

        while (current != null) {
            result.add(current)
            current = current.parent
        }

        return result
    }

    /**
     * 获取根元素
     *
     * @return 根元素
     */
    fun getRoot(): CodeElement {
        var current = this

        while (current.parent != null) {
            current = current.parent!!
        }

        return current
    }

    /**
     * 检查是否包含指定位置
     *
     * @param position 位置
     * @return 是否包含
     */
    fun containsPosition(position: Location): Boolean {
        return location.contains(position)
    }

    /**
     * 获取最深的包含指定位置的子元素
     *
     * @param position 位置
     * @return 最深的包含指定位置的子元素，如果没有则返回 null
     */
    fun getDeepestElementAtPosition(position: Location): CodeElement? {
        if (!containsPosition(position)) {
            return null
        }

        for (child in children) {
            val deepest = child.getDeepestElementAtPosition(position)
            if (deepest != null) {
                return deepest
            }
        }

        return this
    }

    /**
     * 获取元素的简短描述
     *
     * @return 简短描述
     */
    fun getShortDescription(): String {
        val visibilityStr = if (visibility != Visibility.UNKNOWN) visibility.name.lowercase() + " " else ""
        val modifiersStr = if (modifiers.isNotEmpty()) modifiers.joinToString(" ") { it.name.lowercase() } + " " else ""

        return "$visibilityStr$modifiersStr${type.name.lowercase()} $name"
    }

    /**
     * 获取元素的详细描述
     *
     * @return 详细描述
     */
    fun getDetailedDescription(): String {
        val sb = StringBuilder()

        sb.append(getShortDescription())
        sb.append("\n")
        sb.append("Qualified name: $qualifiedName\n")
        sb.append("Location: $location\n")

        if (documentation.isNotEmpty()) {
            sb.append("Documentation: $documentation\n")
        }

        if (metadata.isNotEmpty()) {
            sb.append("Metadata: $metadata\n")
        }

        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CodeElement

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
