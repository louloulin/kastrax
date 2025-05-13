package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Modifier
import ai.kastrax.codebase.semantic.model.Visibility
import chapi.domain.core.CodeContainer
import chapi.domain.core.CodeDataStruct
import chapi.domain.core.CodeField
import chapi.domain.core.CodeFunction
import chapi.domain.core.CodeImport
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * 基于 Chapi 的代码解析器基类
 *
 * 提供基于 Chapi 的代码解析器通用实现
 */
abstract class ChapiCodeParser : AbstractCodeParser() {
    /**
     * 解析代码文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 代码元素（文件级别）
     */
    override fun parseFile(filePath: Path, content: String): CodeElement {
        val fileElement = createFileElement(filePath, content)

        try {
            // 使用 Chapi 解析代码
            val container = parseCodeByChapi(content)

            // 处理导入语句
            processImports(fileElement, container.Imports.toList())

            // 处理数据结构（类、接口等）
            processDataStructs(fileElement, container.DataStructures.toList())

            return fileElement
        } catch (e: Exception) {
            logger.error(e) { "解析文件时发生错误: $filePath" }
            return fileElement
        }
    }

    /**
     * 使用 Chapi 解析代码
     *
     * @param content 代码内容
     * @return Chapi 代码容器
     */
    protected abstract fun parseCodeByChapi(content: String): CodeContainer

    /**
     * 处理导入语句
     *
     * @param fileElement 文件元素
     * @param imports 导入语句列表
     */
    private fun processImports(fileElement: CodeElement, imports: List<CodeImport>) {
        imports.forEach { import ->
            val importElement = CodeElement(
                id = "${fileElement.id}:import:${import.Source}",
                name = import.Source,
                qualifiedName = import.Source,
                type = CodeElementType.IMPORT,
                location = fileElement.location, // 导入语句的具体位置信息在 Chapi 中可能不可用
                parent = fileElement,
                language = getLanguageName()
            )

            fileElement.addChild(importElement)
        }
    }

    /**
     * 处理数据结构（类、接口等）
     *
     * @param fileElement 文件元素
     * @param dataStructs 数据结构列表
     */
    private fun processDataStructs(fileElement: CodeElement, dataStructs: List<CodeDataStruct>) {
        dataStructs.forEach { dataStruct ->
            val elementType = when (dataStruct.Type.toString()) {
                "interface" -> CodeElementType.INTERFACE
                "enum" -> CodeElementType.ENUM
                "annotation" -> CodeElementType.ANNOTATION
                else -> CodeElementType.CLASS
            }

            val visibility = Visibility.PUBLIC
            val modifiers = setOf<Modifier>()

            val classElement = CodeElement(
                id = "${fileElement.id}:${elementType.name.lowercase()}:${dataStruct.NodeName}",
                name = dataStruct.NodeName,
                qualifiedName = if (dataStruct.Package.isNotEmpty()) {
                    "${dataStruct.Package}.${dataStruct.NodeName}"
                } else {
                    dataStruct.NodeName
                },
                type = elementType,
                location = fileElement.location, // 类的具体位置信息在 Chapi 中可能不完全可用
                visibility = visibility,
                modifiers = modifiers,
                parent = fileElement,
                documentation = "",
                language = getLanguageName()
            )

            // 处理字段
            processFields(classElement, dataStruct.Fields.toList())

            // 处理方法
            processFunctions(classElement, dataStruct.Functions.toList())

            fileElement.addChild(classElement)
        }
    }

    /**
     * 处理字段
     *
     * @param classElement 类元素
     * @param fields 字段列表
     */
    private fun processFields(classElement: CodeElement, fields: List<CodeField>) {
        fields.forEach { field ->
            val visibility = Visibility.PUBLIC
            val modifiers = setOf<Modifier>()

            val fieldElement = CodeElement(
                id = "${classElement.id}:field:${field.TypeValue}",
                name = field.TypeValue,
                qualifiedName = "${classElement.qualifiedName}.${field.TypeValue}",
                type = CodeElementType.FIELD,
                location = classElement.location, // 字段的具体位置信息在 Chapi 中可能不完全可用
                visibility = visibility,
                modifiers = modifiers,
                parent = classElement,
                documentation = "",
                language = getLanguageName()
            )

            // 添加字段类型信息到元数据
            fieldElement.metadata["type"] = field.TypeType

            classElement.addChild(fieldElement)
        }
    }

    /**
     * 处理方法
     *
     * @param classElement 类元素
     * @param functions 方法列表
     */
    private fun processFunctions(classElement: CodeElement, functions: List<CodeFunction>) {
        functions.forEach { function ->
            val elementType = if (function.IsConstructor) {
                CodeElementType.CONSTRUCTOR
            } else {
                CodeElementType.METHOD
            }

            val visibility = Visibility.PUBLIC
            val modifiers = setOf<Modifier>()

            val methodElement = CodeElement(
                id = "${classElement.id}:${elementType.name.lowercase()}:${function.Name}",
                name = function.Name,
                qualifiedName = "${classElement.qualifiedName}.${function.Name}",
                type = elementType,
                location = classElement.location, // 方法的具体位置信息在 Chapi 中可能不完全可用
                visibility = visibility,
                modifiers = modifiers,
                parent = classElement,
                documentation = "",
                language = getLanguageName()
            )

            // 添加返回类型信息到元数据
            methodElement.metadata["returnType"] = function.ReturnType

            // 处理参数 - 简化处理以避免类型问题
            val paramElement = CodeElement(
                id = "${methodElement.id}:parameter:param",
                name = "param",
                qualifiedName = "${methodElement.qualifiedName}(param)",
                type = CodeElementType.PARAMETER,
                location = methodElement.location,
                parent = methodElement,
                language = getLanguageName()
            )

            // 添加参数元素到方法元素
            methodElement.addChild(paramElement)

            classElement.addChild(methodElement)
        }
    }

    /**
     * 解析可见性
     *
     * @param modifiers 修饰符列表
     * @return 可见性
     */
    private fun parseVisibility(modifiers: List<String>): Visibility {
        return when {
            "public" in modifiers -> Visibility.PUBLIC
            "protected" in modifiers -> Visibility.PROTECTED
            "private" in modifiers -> Visibility.PRIVATE
            "internal" in modifiers -> Visibility.INTERNAL
            else -> Visibility.UNKNOWN
        }
    }

    /**
     * 解析修饰符
     *
     * @param modifiers 修饰符列表
     * @return 修饰符集合
     */
    private fun parseModifiers(modifiers: List<String>): Set<Modifier> {
        val result = mutableSetOf<Modifier>()

        modifiers.forEach { modifier ->
            when (modifier) {
                "static" -> result.add(Modifier.STATIC)
                "final" -> result.add(Modifier.FINAL)
                "abstract" -> result.add(Modifier.ABSTRACT)
                "synchronized" -> result.add(Modifier.SYNCHRONIZED)
                "volatile" -> result.add(Modifier.VOLATILE)
                "transient" -> result.add(Modifier.TRANSIENT)
                "native" -> result.add(Modifier.NATIVE)
                "strictfp" -> result.add(Modifier.STRICTFP)
                "default" -> result.add(Modifier.DEFAULT)
                "sealed" -> result.add(Modifier.SEALED)
                "open" -> result.add(Modifier.OPEN)
                "const" -> result.add(Modifier.CONST)
                "override" -> result.add(Modifier.OVERRIDE)
                "lateinit" -> result.add(Modifier.LATEINIT)
                "suspend" -> result.add(Modifier.SUSPEND)
                "tailrec" -> result.add(Modifier.TAILREC)
                "external" -> result.add(Modifier.EXTERNAL)
                "inline" -> result.add(Modifier.INLINE)
                "infix" -> result.add(Modifier.INFIX)
                "operator" -> result.add(Modifier.OPERATOR)
                "data" -> result.add(Modifier.DATA)
                "inner" -> result.add(Modifier.INNER)
                "companion" -> result.add(Modifier.COMPANION)
                "fun" -> result.add(Modifier.FUN)
                "value" -> result.add(Modifier.VALUE)
                "virtual" -> result.add(Modifier.VIRTUAL)
            }
        }

        return result
    }
}
