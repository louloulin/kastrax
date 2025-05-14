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
import chapi.domain.core.CodePosition
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

            // 解析可见性和修饰符
            // 解析可见性和修饰符
            val modifiersList = try {
                val modifiersField = dataStruct::class.java.getDeclaredField("Modifiers")
                modifiersField.isAccessible = true
                val modifiers = modifiersField.get(dataStruct)
                if (modifiers is Array<*>) {
                    modifiers.filterNotNull().map { it.toString() }
                } else if (modifiers is Collection<*>) {
                    modifiers.filterNotNull().map { it.toString() }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<String>()
            }

            val visibility = parseVisibility(modifiersList)
            val modifiers = parseModifiers(modifiersList)

            val classElement = CodeElement(
                id = "${fileElement.id}:${elementType.name.lowercase()}:${dataStruct.NodeName}",
                name = dataStruct.NodeName,
                qualifiedName = if (dataStruct.Package.isNotEmpty()) {
                    "${dataStruct.Package}.${dataStruct.NodeName}"
                } else {
                    dataStruct.NodeName
                },
                type = elementType,
                location = createLocationFromPosition(fileElement.location.filePath, getPosition(dataStruct)),
                visibility = visibility,
                modifiers = modifiers,
                parent = fileElement,
                documentation = getDocString(dataStruct),
                language = getLanguageName()
            )

            // 添加继承和实现信息到元数据
            // 获取继承信息
            val extendList = try {
                val extendField = dataStruct::class.java.getDeclaredField("Extend")
                extendField.isAccessible = true
                val extend = extendField.get(dataStruct)
                if (extend is Array<*>) {
                    extend.filterNotNull().map { it.toString() }
                } else if (extend is Collection<*>) {
                    extend.filterNotNull().map { it.toString() }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<String>()
            }

            if (extendList.isNotEmpty()) {
                classElement.metadata["extends"] = extendList
            }

            // 获取实现信息
            val implementsList = try {
                val implementsField = dataStruct::class.java.getDeclaredField("Implements")
                implementsField.isAccessible = true
                val implements = implementsField.get(dataStruct)
                if (implements is Array<*>) {
                    implements.filterNotNull().map { it.toString() }
                } else if (implements is Collection<*>) {
                    implements.filterNotNull().map { it.toString() }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<String>()
            }

            if (implementsList.isNotEmpty()) {
                classElement.metadata["implements"] = implementsList.joinToString(", ")
            }

            // 处理字段
            val fieldsList = try {
                val fieldsField = dataStruct::class.java.getDeclaredField("Fields")
                fieldsField.isAccessible = true
                val fields = fieldsField.get(dataStruct)
                if (fields is Array<*>) {
                    fields.filterNotNull()
                } else if (fields is Collection<*>) {
                    fields.filterNotNull()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<Any>()
            }

            processFields(classElement, fieldsList)

            // 处理方法
            val functionsList = try {
                val functionsField = dataStruct::class.java.getDeclaredField("Functions")
                functionsField.isAccessible = true
                val functions = functionsField.get(dataStruct)
                if (functions is Array<*>) {
                    functions.filterNotNull()
                } else if (functions is Collection<*>) {
                    functions.filterNotNull()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<Any>()
            }

            processFunctions(classElement, functionsList)

            fileElement.addChild(classElement)
        }
    }

    /**
     * 处理字段
     *
     * @param classElement 类元素
     * @param fields 字段列表
     */
    private fun processFields(classElement: CodeElement, fields: List<Any>) {
        // 将 Any 类型的字段转换为 CodeField 类型
        fields.forEach { field ->
            try {
                // 使用反射获取字段的属性
                val typeValueField = field::class.java.getDeclaredField("TypeValue")
                typeValueField.isAccessible = true
                val typeValue = typeValueField.get(field) as? String ?: return@forEach

                val modifiersField = field::class.java.getDeclaredField("Modifiers")
                modifiersField.isAccessible = true
                val modifiers = modifiersField.get(field)
                val modifiersList = if (modifiers is Array<*>) {
                    modifiers.filterNotNull().map { it.toString() }
                } else if (modifiers is Collection<*>) {
                    modifiers.filterNotNull().map { it.toString() }
                } else {
                    emptyList<String>()
                }

                // 解析可见性和修饰符
                val visibility = parseVisibility(modifiersList)
                val fieldModifiers = parseModifiers(modifiersList)

                val fieldElement = CodeElement(
                    id = "${classElement.id}:field:${typeValue}",
                    name = typeValue,
                        qualifiedName = "${classElement.qualifiedName}.${typeValue}",
                    type = CodeElementType.FIELD,
                    location = createLocationFromPosition(classElement.location.filePath, getPosition(field)),
                    visibility = visibility,
                    modifiers = fieldModifiers,
                    parent = classElement,
                    documentation = getDocString(field),
                    language = getLanguageName()
                )

                // 添加字段类型信息到元数据
                try {
                    val typeTypeField = field::class.java.getDeclaredField("TypeType")
                    typeTypeField.isAccessible = true
                    val typeType = typeTypeField.get(field) as? String
                    if (typeType != null) {
                        fieldElement.metadata["type"] = typeType
                    }

                    val defaultValueField = field::class.java.getDeclaredField("DefaultValue")
                    defaultValueField.isAccessible = true
                    val defaultValue = defaultValueField.get(field)
                    if (defaultValue != null) {
                        fieldElement.metadata["defaultValue"] = defaultValue.toString()
                    } else {
                        fieldElement.metadata["defaultValue"] = ""
                    }
                } catch (e: Exception) {
                    logger.debug { "Failed to get field type or default value: ${e.message}" }
                }

                // 添加注解信息
                try {
                    val annotationsField = field::class.java.getDeclaredField("Annotations")
                    annotationsField.isAccessible = true
                    val annotations = annotationsField.get(field)
                    if (annotations is Array<*> && annotations.isNotEmpty()) {
                        fieldElement.metadata["annotations"] = annotations.filterNotNull().joinToString(", ")
                    } else if (annotations is Collection<*> && annotations.isNotEmpty()) {
                        fieldElement.metadata["annotations"] = annotations.filterNotNull().joinToString(", ")
                    }
                } catch (e: Exception) {
                    logger.debug { "Failed to get field annotations: ${e.message}" }
                }

                classElement.addChild(fieldElement)
            } catch (e: Exception) {
                logger.error { "Failed to process field: ${e.message}" }
            }
        }
    }

    /**
     * 处理方法
     *
     * @param classElement 类元素
     * @param functions 方法列表
     */
    private fun processFunctions(classElement: CodeElement, functions: List<Any>) {
        functions.forEach { function ->
            try {
                // 使用反射获取函数的属性
                val isConstructorField = function::class.java.getDeclaredField("IsConstructor")
                isConstructorField.isAccessible = true
                val isConstructor = isConstructorField.get(function) as? Boolean ?: false

                val nameField = function::class.java.getDeclaredField("Name")
                nameField.isAccessible = true
                val name = nameField.get(function) as? String ?: return@forEach

                val elementType = if (isConstructor) {
                    CodeElementType.CONSTRUCTOR
                } else if (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) {
                    // 识别getter和setter方法
                    CodeElementType.PROPERTY
                } else {
                    CodeElementType.METHOD
                }

                // 获取修饰符
                val modifiersField = function::class.java.getDeclaredField("Modifiers")
                modifiersField.isAccessible = true
                val modifiers = modifiersField.get(function)
                val modifiersList = if (modifiers is Array<*>) {
                    modifiers.filterNotNull().map { it.toString() }
                } else if (modifiers is Collection<*>) {
                    modifiers.filterNotNull().map { it.toString() }
                } else {
                    emptyList<String>()
                }

                // 解析可见性和修饰符
                val visibility = parseVisibility(modifiersList)
                val functionModifiers = parseModifiers(modifiersList)

                val methodElement = CodeElement(
                    id = "${classElement.id}:${elementType.name.lowercase()}:${name}",
                    name = name,
                    qualifiedName = "${classElement.qualifiedName}.${name}",
                    type = elementType,
                    location = createLocationFromPosition(classElement.location.filePath, getPosition(function)),
                    visibility = visibility,
                    modifiers = functionModifiers,
                    parent = classElement,
                    documentation = getDocString(function),
                    language = getLanguageName()
                )

                // 添加返回类型信息到元数据
                try {
                    val returnTypeField = function::class.java.getDeclaredField("ReturnType")
                    returnTypeField.isAccessible = true
                    val returnType = returnTypeField.get(function) as? String
                    if (returnType != null) {
                        methodElement.metadata["returnType"] = returnType
                    }
                } catch (e: Exception) {
                    logger.debug { "Failed to get function return type: ${e.message}" }
                }

                // 添加注解信息
                try {
                    val annotationsField = function::class.java.getDeclaredField("Annotations")
                    annotationsField.isAccessible = true
                    val annotations = annotationsField.get(function)
                    if (annotations is Array<*> && annotations.isNotEmpty()) {
                        methodElement.metadata["annotations"] = annotations.filterNotNull().joinToString(", ")
                    } else if (annotations is Collection<*> && annotations.isNotEmpty()) {
                        methodElement.metadata["annotations"] = annotations.filterNotNull().joinToString(", ")
                    }
                } catch (e: Exception) {
                    logger.debug { "Failed to get function annotations: ${e.message}" }
                }

                // 添加方法体信息
                try {
                    val functionCallsField = function::class.java.getDeclaredField("FunctionCalls")
                    functionCallsField.isAccessible = true
                    val functionCalls = functionCallsField.get(function)
                    if (functionCalls is Array<*> && functionCalls.isNotEmpty()) {
                        methodElement.metadata["functionCalls"] = functionCalls.filterNotNull().joinToString(", ") {
                            try {
                                val nameField = it::class.java.getDeclaredField("Name")
                                nameField.isAccessible = true
                                (nameField.get(it) as? String) ?: "unknown"
                            } catch (e: Exception) {
                                "unknown"
                            }
                        }
                    } else if (functionCalls is Collection<*> && functionCalls.isNotEmpty()) {
                        methodElement.metadata["functionCalls"] = functionCalls.filterNotNull().joinToString(", ") {
                            try {
                                val nameField = it::class.java.getDeclaredField("Name")
                                nameField.isAccessible = true
                                (nameField.get(it) as? String) ?: "unknown"
                            } catch (e: Exception) {
                                "unknown"
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.debug { "Failed to get function calls: ${e.message}" }
                }

                // 处理参数
                try {
                    val parametersField = function::class.java.getDeclaredField("Parameters")
                    parametersField.isAccessible = true
                    val parameters = parametersField.get(function)
                    if (parameters is Array<*>) {
                        parameters.filterNotNull().forEach { processParameter(it, methodElement) }
                    } else if (parameters is Collection<*>) {
                        parameters.filterNotNull().forEach { processParameter(it, methodElement) }
                    }
                } catch (e: Exception) {
                    logger.debug { "Failed to get function parameters: ${e.message}" }
                }

                classElement.addChild(methodElement)
            } catch (e: Exception) {
                logger.error { "Failed to process function: ${e.message}" }
            }
        }
    }

    /**
     * 处理参数
     *
     * @param param 参数对象
     * @param methodElement 方法元素
     */
    private fun processParameter(param: Any, methodElement: CodeElement) {
        try {
            // 使用反射获取参数的属性
            val nameField = param::class.java.getDeclaredField("Name")
            nameField.isAccessible = true
            val name = (nameField.get(param) as? String) ?: "unknown"

            val paramElement = CodeElement(
                id = "${methodElement.id}:parameter:${name}",
                name = name,
                qualifiedName = "${methodElement.qualifiedName}(${name})",
                type = CodeElementType.PARAMETER,
                location = methodElement.location,
                parent = methodElement,
                language = getLanguageName()
            )

            // 添加参数类型信息
            try {
                val typeTypeField = param::class.java.getDeclaredField("TypeType")
                typeTypeField.isAccessible = true
                val typeType = typeTypeField.get(param) as? String
                if (typeType != null) {
                    paramElement.metadata["type"] = typeType
                }

                val defaultValueField = param::class.java.getDeclaredField("DefaultValue")
                defaultValueField.isAccessible = true
                val defaultValue = defaultValueField.get(param)
                if (defaultValue != null) {
                    paramElement.metadata["defaultValue"] = defaultValue.toString()
                }
            } catch (e: Exception) {
                logger.debug { "Failed to get parameter type or default value: ${e.message}" }
            }

            // 添加参数元素到方法元素
            methodElement.addChild(paramElement)
        } catch (e: Exception) {
            logger.error { "Failed to process parameter: ${e.message}" }
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
            "package" in modifiers -> Visibility.PACKAGE_PRIVATE
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
            when (modifier.lowercase()) {
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
                "readonly" -> result.add(Modifier.READONLY)
                "async" -> result.add(Modifier.ASYNC)
                "classmethod" -> result.add(Modifier.CLASS_METHOD)
                "property" -> result.add(Modifier.PROPERTY)
            }
        }

        return result
    }





    /**
     * 获取对象的 Position 属性
     *
     * @param obj 对象
     * @return Position 对象
     */
    private fun getPosition(obj: Any): CodePosition? {
        return try {
            val positionField = obj::class.java.getDeclaredField("Position")
            positionField.isAccessible = true
            positionField.get(obj) as? CodePosition
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取对象的 DocString 属性
     *
     * @param obj 对象
     * @return 文档字符串
     */
    private fun getDocString(obj: Any): String {
        return try {
            val docStringField = obj::class.java.getDeclaredField("DocString")
            docStringField.isAccessible = true
            (docStringField.get(obj) as? String) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 从Chapi的位置信息创建Location对象
     *
     * @param filePath 文件路径
     * @param position Chapi位置信息
     * @return Location对象
     */
    private fun createLocationFromPosition(filePath: String, position: CodePosition?): Location {
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
            startLine = position.StartLine,
            startColumn = position.StartLinePosition,
            endLine = position.StopLine,
            endColumn = position.StopLinePosition
        )
    }
}
