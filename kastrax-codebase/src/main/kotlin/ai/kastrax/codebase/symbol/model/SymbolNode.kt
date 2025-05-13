package ai.kastrax.codebase.symbol.model

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import java.nio.file.Path
import java.util.UUID

// 符号类型已移至 SymbolType.kt

/**
 * 符号种类
 */
enum class SymbolKind {
    DEFINITION,
    DECLARATION,
    REFERENCE,
    IMPLEMENTATION,
    OVERRIDE,
    IMPORT,
    UNKNOWN
}

/**
 * 符号节点
 *
 * 代表代码库中的一个符号，如类、方法、字段等
 *
 * @property id 唯一标识符
 * @property name 符号名称
 * @property qualifiedName 限定名
 * @property type 符号类型
 * @property kind 符号种类
 * @property location 位置
 * @property visibility 可见性
 * @property codeElement 关联的代码元素
 * @property metadata 元数据
 */
data class SymbolNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val qualifiedName: String,
    val type: SymbolType,
    val kind: SymbolKind,
    val location: Location,
    val visibility: Visibility = Visibility.UNKNOWN,
    val codeElement: CodeElement? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 构造函数，接受字符串类型的 kind 参数
     */
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        qualifiedName: String,
        type: SymbolType,
        kind: String,
        location: Location,
        visibility: Visibility = Visibility.UNKNOWN,
        codeElement: CodeElement? = null,
        metadata: MutableMap<String, Any> = mutableMapOf()
    ) : this(
        id = id,
        name = name,
        qualifiedName = qualifiedName,
        type = type,
        kind = try { SymbolKind.valueOf(kind.uppercase()) } catch (e: Exception) { SymbolKind.UNKNOWN },
        location = location,
        visibility = visibility,
        codeElement = codeElement,
        metadata = metadata
    )

    /**
     * 构造函数，接受 CodeElement 和字符串类型的 kind 参数
     */
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        qualifiedName: String,
        type: SymbolType,
        kind: String,
        location: Location,
        visibility: Visibility = Visibility.UNKNOWN,
        codeElement: CodeElement? = null,
        metadata: MutableMap<String, Any> = mutableMapOf()
    ) : this(
        id = id,
        name = name,
        qualifiedName = qualifiedName,
        type = type,
        kind = try { SymbolKind.valueOf(kind.uppercase()) } catch (e: Exception) { SymbolKind.UNKNOWN },
        location = location,
        visibility = visibility,
        codeElement = codeElement,
        metadata = metadata
    )
    /**
     * 获取符号的简短描述
     *
     * @return 简短描述
     */
    fun getShortDescription(): String {
        val visibilityStr = if (visibility != Visibility.UNKNOWN) visibility.name.lowercase() + " " else ""
        return "$visibilityStr${type.name.lowercase()} $name"
    }

    /**
     * 获取符号的详细描述
     *
     * @return 详细描述
     */
    fun getDetailedDescription(): String {
        val sb = StringBuilder()

        sb.append(getShortDescription())
        sb.append("\n")
        sb.append("Qualified name: $qualifiedName\n")
        sb.append("Kind: ${kind.name.lowercase()}\n")
        sb.append("Location: $location\n")

        if (metadata.isNotEmpty()) {
            sb.append("Metadata: $metadata\n")
        }

        return sb.toString()
    }

    /**
     * 从代码元素创建符号节点
     *
     * @param element 代码元素
     * @param kind 符号种类
     * @return 符号节点
     */
    companion object {
        fun fromCodeElement(element: CodeElement, kind: SymbolKind = SymbolKind.DEFINITION): SymbolNode {
            val type = when (element.type) {
                CodeElementType.PACKAGE -> SymbolType.PACKAGE
                CodeElementType.CLASS -> SymbolType.CLASS
                CodeElementType.INTERFACE -> SymbolType.INTERFACE
                CodeElementType.ENUM -> SymbolType.ENUM
                CodeElementType.ANNOTATION -> SymbolType.ANNOTATION
                CodeElementType.METHOD -> SymbolType.METHOD
                CodeElementType.CONSTRUCTOR -> SymbolType.CONSTRUCTOR
                CodeElementType.FIELD -> SymbolType.FIELD
                CodeElementType.PROPERTY -> SymbolType.PROPERTY
                CodeElementType.PARAMETER -> SymbolType.PARAMETER
                CodeElementType.LOCAL_VARIABLE -> SymbolType.LOCAL_VARIABLE
                else -> SymbolType.UNKNOWN
            }

            return SymbolNode(
                name = element.name,
                qualifiedName = element.qualifiedName,
                type = type,
                kind = kind,
                location = element.location,
                visibility = element.visibility,
                codeElement = element,
                metadata = element.metadata.toMutableMap()
            )
        }
    }
}
