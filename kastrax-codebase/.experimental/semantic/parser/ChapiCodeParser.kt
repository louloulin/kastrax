package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Modifier
import ai.kastrax.codebase.semantic.model.Visibility
import chapi.domain.core.CodeAnnotation
import chapi.domain.core.CodeDataStruct
import chapi.domain.core.CodeField
import chapi.domain.core.CodeFunction
import chapi.domain.core.CodeImport
import chapi.domain.core.CodePackage
import chapi.domain.core.CodePosition
import chapi.domain.core.CodeProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 基于 Chapi 的代码解析器
 *
 * 使用 Chapi 库解析不同语言的代码文件
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
        try {
            // 创建文件元素
            val fileElement = createFileElement(filePath, content)

            // 使用 Chapi 解析代码
            val codeContainer = parseCodeByChapi(content)

            // 解析包声明
            if (codeContainer.packages.isNotEmpty()) {
                val packageDecl = codeContainer.packages.first()
                val packageElement = parsePackage(filePath, packageDecl, fileElement)
                fileElement.addChild(packageElement)
                fileElement.metadata["package"] = packageDecl.name
            }

            // 解析导入声明
            codeContainer.imports.forEach { importDecl ->
                val importElement = parseImport(filePath, importDecl, fileElement)
                fileElement.addChild(importElement)
            }

            // 解析类型声明
            codeContainer.dataStructures.forEach { dataStruct ->
                val classElement = parseDataStruct(filePath, dataStruct, fileElement)
                fileElement.addChild(classElement)
            }

            return fileElement
        } catch (e: Exception) {
            logger.error(e) { "解析代码文件时出错: $filePath" }
            return createFileElement(filePath, content)
        }
    }

    /**
     * 使用 Chapi 解析代码
     *
     * @param content 代码内容
     * @return Chapi 代码容器
     */
    protected abstract fun parseCodeByChapi(content: String): chapi.domain.core.CodeContainer

    /**
     * 解析包声明
     *
     * @param filePath 文件路径
     * @param packageDecl 包声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parsePackage(
        filePath: Path,
        packageDecl: CodePackage,
        parent: CodeElement
    ): CodeElement {
        val packageName = packageDecl.name
        val location = createLocation(filePath, packageDecl.Position)

        return CodeElement(
            id = UUID.randomUUID().toString(),
            name = packageName,
            qualifiedName = packageName,
            type = CodeElementType.PACKAGE,
            location = location,
            visibility = Visibility.PUBLIC,
            parent = parent,
            language = getLanguageName()
        )
    }

    /**
     * 解析导入声明
     *
     * @param filePath 文件路径
     * @param importDecl 导入声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseImport(
        filePath: Path,
        importDecl: CodeImport,
        parent: CodeElement
    ): CodeElement {
        val importName = importDecl.source
        val location = createLocation(filePath, importDecl.Position)

        val importElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = importName,
            qualifiedName = importName,
            type = CodeElementType.IMPORT,
            location = location,
            parent = parent,
            language = getLanguageName()
        )

        importElement.metadata["isStatic"] = importDecl.usageName.contains("static")
        importElement.metadata["isAsterisk"] = importDecl.source.endsWith("*")

        return importElement
    }

    /**
     * 解析数据结构（类、接口、枚举等）
     *
     * @param filePath 文件路径
     * @param dataStruct 数据结构
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseDataStruct(
        filePath: Path,
        dataStruct: CodeDataStruct,
        parent: CodeElement
    ): CodeElement {
        val name = dataStruct.nodeName
        val packageName = parent.metadata["package"] as? String ?: ""
        val qualifiedName = if (packageName.isNotEmpty()) "$packageName.$name" else name
        val location = createLocation(filePath, dataStruct.position)

        val type = when (dataStruct.type) {
            "interface" -> CodeElementType.INTERFACE
            "enum" -> CodeElementType.ENUM
            "annotation" -> CodeElementType.ANNOTATION
            else -> CodeElementType.CLASS
        }

        val visibility = parseVisibility(dataStruct.Modifiers)
        val modifiers = parseModifiers(dataStruct.Modifiers)

        val classElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = type,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = getLanguageName()
        )

        // 解析文档注释
        if (dataStruct.docString.isNotEmpty()) {
            classElement.documentation = dataStruct.docString
        }

        // 解析字段
        dataStruct.fields.forEach { field ->
            val fieldElement = parseField(filePath, field, classElement)
            classElement.addChild(fieldElement)
        }

        // 解析属性（Kotlin）
        dataStruct.properties.forEach { property ->
            val propertyElement = parseProperty(filePath, property, classElement)
            classElement.addChild(propertyElement)
        }

        // 解析方法
        dataStruct.functions.forEach { function ->
            val methodElement = parseFunction(filePath, function, classElement)
            classElement.addChild(methodElement)
        }

        // 解析内部类
        dataStruct.innerStructures.forEach { innerStruct ->
            val innerClassElement = parseDataStruct(filePath, innerStruct, classElement)
            classElement.addChild(innerClassElement)
        }

        // 添加元数据
        classElement.metadata["type"] = dataStruct.type

        // 解析注解
        if (dataStruct.annotations.isNotEmpty()) {
            classElement.metadata["annotations"] = dataStruct.annotations.map { it.name }
        }

        // 解析继承关系
        if (dataStruct.extend.isNotEmpty()) {
            classElement.metadata["extends"] = dataStruct.extend
        }

        // 解析实现关系
        if (dataStruct.implements.isNotEmpty()) {
            classElement.metadata["implements"] = dataStruct.implements
        }

        return classElement
    }

    /**
     * 解析字段
     *
     * @param filePath 文件路径
     * @param field 字段
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseField(
        filePath: Path,
        field: CodeField,
        parent: CodeElement
    ): CodeElement {
        val name = field.typeValue
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, field.position)

        val visibility = parseVisibility(field.modifiers)
        val modifiers = parseModifiers(field.modifiers)

        val fieldElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.FIELD,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = getLanguageName()
        )

        // 解析文档注释
        if (field.docString.isNotEmpty()) {
            fieldElement.documentation = field.docString
        }

        // 添加元数据
        fieldElement.metadata["type"] = field.typeType

        // 解析注解
        if (field.annotations.isNotEmpty()) {
            fieldElement.metadata["annotations"] = field.annotations.map { it.name }
        }

        // 解析初始化表达式
        if (field.typeValue.isNotEmpty()) {
            fieldElement.metadata["initializer"] = field.typeValue
        }

        return fieldElement
    }

    /**
     * 解析属性（Kotlin）
     *
     * @param filePath 文件路径
     * @param property 属性
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseProperty(
        filePath: Path,
        property: CodeProperty,
        parent: CodeElement
    ): CodeElement {
        val name = property.typeValue
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, property.position)

        val visibility = parseVisibility(property.modifiers)
        val modifiers = parseModifiers(property.modifiers)

        val propertyElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.PROPERTY,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = getLanguageName()
        )

        // 解析文档注释
        if (property.docString.isNotEmpty()) {
            propertyElement.documentation = property.docString
        }

        // 添加元数据
        propertyElement.metadata["type"] = property.typeType

        // 解析注解
        if (property.annotations.isNotEmpty()) {
            propertyElement.metadata["annotations"] = property.annotations.map { it.name }
        }

        // 解析初始化表达式
        if (property.typeValue.isNotEmpty()) {
            propertyElement.metadata["initializer"] = property.typeValue
        }

        return propertyElement
    }

    /**
     * 解析函数（方法或构造函数）
     *
     * @param filePath 文件路径
     * @param function 函数
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseFunction(
        filePath: Path,
        function: CodeFunction,
        parent: CodeElement
    ): CodeElement {
        val name = function.name
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, function.position)

        val type = if (function.isConstructor) CodeElementType.CONSTRUCTOR else CodeElementType.METHOD
        val visibility = parseVisibility(function.modifiers)
        val modifiers = parseModifiers(function.modifiers)

        val functionElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = type,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = getLanguageName()
        )

        // 解析文档注释
        if (function.docString.isNotEmpty()) {
            functionElement.documentation = function.docString
        }

        // 解析参数
        function.parameters.forEach { parameter ->
            val parameterElement = parseParameter(filePath, parameter, functionElement)
            functionElement.addChild(parameterElement)
        }

        // 添加元数据
        functionElement.metadata["returnType"] = function.returnType
        functionElement.metadata["parameters"] = function.parameters.map { it.typeValue }

        // 解析注解
        if (function.annotations.isNotEmpty()) {
            functionElement.metadata["annotations"] = function.annotations.map { it.name }
        }

        return functionElement
    }

    /**
     * 解析参数
     *
     * @param filePath 文件路径
     * @param parameter 参数
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseParameter(
        filePath: Path,
        parameter: chapi.domain.core.CodeParameter,
        parent: CodeElement
    ): CodeElement {
        val name = parameter.typeValue
        val qualifiedName = "${parent.qualifiedName}.$name"

        // Chapi 不提供参数的位置信息，使用父元素的位置
        val location = parent.location

        val parameterElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.PARAMETER,
            location = location,
            visibility = Visibility.UNKNOWN,
            parent = parent,
            language = getLanguageName()
        )

        // 添加元数据
        parameterElement.metadata["type"] = parameter.typeType

        // 解析注解
        if (parameter.annotations.isNotEmpty()) {
            parameterElement.metadata["annotations"] = parameter.annotations.map { it.name }
        }

        return parameterElement
    }

    /**
     * 解析注解
     *
     * @param filePath 文件路径
     * @param annotation 注解
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseAnnotation(
        filePath: Path,
        annotation: CodeAnnotation,
        parent: CodeElement
    ): CodeElement {
        val name = annotation.name
        val qualifiedName = "${parent.qualifiedName}.$name"

        // Chapi 不提供注解的位置信息，使用父元素的位置
        val location = parent.location

        val annotationElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.ANNOTATION,
            location = location,
            visibility = Visibility.UNKNOWN,
            parent = parent,
            language = getLanguageName()
        )

        // 添加元数据
        if (annotation.keyValues.isNotEmpty()) {
            annotationElement.metadata["keyValues"] = annotation.keyValues
        }

        return annotationElement
    }

    /**
     * 解析可见性
     *
     * @param modifiers 修饰符列表
     * @return 可见性
     */
    private fun parseVisibility(modifiers: List<String>): Visibility {
        return when {
            modifiers.contains("public") -> Visibility.PUBLIC
            modifiers.contains("protected") -> Visibility.PROTECTED
            modifiers.contains("private") -> Visibility.PRIVATE
            modifiers.contains("internal") -> Visibility.INTERNAL
            else -> Visibility.PACKAGE_PRIVATE
        }
    }

    /**
     * 获取支持的文件扩展名
     *
     * @return 支持的文件扩展名集合
     */
    override fun getSupportedExtensions(): Set<String> {
        // 由子类实现
        return emptySet()
    }

    /**
     * 获取语言名称
     *
     * @return 语言名称
     */
    override fun getLanguageName(): String {
        // 由子类实现
        return ""
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

    /**
     * 创建位置
     *
     * @param filePath 文件路径
     * @param position 位置
     * @return 位置
     */
    private fun createLocation(filePath: Path, position: CodePosition?): Location {
        if (position == null) {
            return Location(
                filePath = filePath,
                startLine = 1,
                startColumn = 1,
                endLine = 1,
                endColumn = 1
            )
        }

        return Location(
            filePath = filePath,
            startLine = position.startLine,
            startColumn = position.startLinePosition,
            endLine = position.stopLine,
            endColumn = position.stopLinePosition
        )
    }
}
