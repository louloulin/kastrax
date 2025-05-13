package ai.kastrax.codebase.semantic.relation

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 代码关系分析器配置
 *
 * @property analyzeInheritance 是否分析继承关系
 * @property analyzeUsage 是否分析使用关系
 * @property analyzeDependency 是否分析依赖关系
 * @property analyzeOverride 是否分析重写关系
 * @property analyzeImplementation 是否分析实现关系
 * @property analyzeReference 是否分析引用关系
 * @property analyzeImport 是否分析导入关系
 * @property maxConcurrentTasks 最大并发任务数
 * @property enableParallelAnalysis 是否启用并行分析
 */
data class CodeRelationAnalyzerConfig(
    val analyzeInheritance: Boolean = true,
    val analyzeUsage: Boolean = true,
    val analyzeDependency: Boolean = true,
    val analyzeOverride: Boolean = true,
    val analyzeImplementation: Boolean = true,
    val analyzeReference: Boolean = true,
    val analyzeImport: Boolean = true,
    val maxConcurrentTasks: Int = 10,
    val enableParallelAnalysis: Boolean = true
)

/**
 * 关系类型
 */
enum class RelationType {
    INHERITANCE,      // 继承关系
    IMPLEMENTATION,   // 实现关系
    USAGE,            // 使用关系
    DEPENDENCY,       // 依赖关系
    OVERRIDE,         // 重写关系
    REFERENCE,        // 引用关系
    IMPORT            // 导入关系
}

/**
 * 代码关系
 *
 * @property id 唯一标识符
 * @property sourceId 源元素 ID
 * @property targetId 目标元素 ID
 * @property type 关系类型
 * @property metadata 元数据
 */
data class CodeRelation(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val type: RelationType,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * 代码关系分析器
 *
 * 分析代码元素之间的关系
 *
 * @property config 配置
 */
class CodeRelationAnalyzer(
    private val config: CodeRelationAnalyzerConfig = CodeRelationAnalyzerConfig()
) {
    // 关系缓存
    private val relationCache = ConcurrentHashMap<String, CodeRelation>()

    // 元素 ID 到关系 ID 的映射
    private val elementToRelations = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * 分析代码关系
     *
     * @param rootElement 根元素
     * @return 代码关系列表
     */
    suspend fun analyzeRelations(rootElement: CodeElement): List<CodeRelation> = withContext(Dispatchers.IO) {
        logger.info { "开始分析代码关系" }

        // 清除缓存
        relationCache.clear()
        elementToRelations.clear()

        // 收集所有元素
        val allElements = collectAllElements(rootElement)
        logger.info { "收集到 ${allElements.size} 个代码元素" }

        // 分析关系
        if (config.enableParallelAnalysis) {
            // 并行分析
            coroutineScope {
                allElements.chunked(config.maxConcurrentTasks).forEach { chunk ->
                    chunk.map { element ->
                        async {
                            analyzeElementRelations(element, allElements)
                        }
                    }.awaitAll()
                }
            }
        } else {
            // 串行分析
            allElements.forEach { element ->
                analyzeElementRelations(element, allElements)
            }
        }

        logger.info { "代码关系分析完成，共 ${relationCache.size} 个关系" }

        return@withContext relationCache.values.toList()
    }

    /**
     * 收集所有元素
     *
     * @param rootElement 根元素
     * @return 所有元素列表
     */
    private fun collectAllElements(rootElement: CodeElement): List<CodeElement> {
        val result = mutableListOf<CodeElement>()

        // 递归收集元素
        fun collect(element: CodeElement) {
            result.add(element)
            element.children.forEach { child ->
                collect(child)
            }
        }

        collect(rootElement)
        return result
    }

    /**
     * 分析元素关系
     *
     * @param element 元素
     * @param allElements 所有元素
     */
    private fun analyzeElementRelations(element: CodeElement, allElements: List<CodeElement>) {
        // 根据元素类型分析不同的关系
        when (element.type) {
            CodeElementType.CLASS -> analyzeClassRelations(element, allElements)
            CodeElementType.INTERFACE -> analyzeInterfaceRelations(element, allElements)
            CodeElementType.METHOD -> analyzeMethodRelations(element, allElements)
            CodeElementType.FIELD -> analyzeFieldRelations(element, allElements)
            CodeElementType.IMPORT -> analyzeImportRelations(element, allElements)
            else -> {} // 其他类型暂不处理
        }
    }

    /**
     * 分析类关系
     *
     * @param classElement 类元素
     * @param allElements 所有元素
     */
    private fun analyzeClassRelations(classElement: CodeElement, allElements: List<CodeElement>) {
        // 分析继承关系
        if (config.analyzeInheritance) {
            // 从 Chapi 解析器提取的继承信息
            val extends = classElement.metadata["extends"] as? String
            if (extends != null && extends.isNotEmpty()) {
                // 查找超类元素
                val superClassElement = findElementByName(allElements, extends)
                if (superClassElement != null) {
                    addRelation(
                        sourceId = classElement.id,
                        targetId = superClassElement.id,
                        type = RelationType.INHERITANCE,
                        metadata = mutableMapOf("description" to "${classElement.name} extends ${superClassElement.name}")
                    )
                }
            }
        }

        // 分析实现关系
        if (config.analyzeImplementation) {
            // 从 Chapi 解析器提取的实现信息
            val implements = classElement.metadata["implements"] as? String
            if (implements != null && implements.isNotEmpty()) {
                val interfaceNames = implements.split(", ")
                interfaceNames.forEach { interfaceName ->
                    // 查找接口元素
                    val interfaceElement = findElementByName(allElements, interfaceName.trim())
                    if (interfaceElement != null) {
                        addRelation(
                            sourceId = classElement.id,
                            targetId = interfaceElement.id,
                            type = RelationType.IMPLEMENTATION,
                            metadata = mutableMapOf("description" to "${classElement.name} implements ${interfaceElement.name}")
                        )
                    }
                }
            }
        }

        // 分析依赖关系
        if (config.analyzeDependency) {
            // 分析类中的字段类型依赖
            classElement.children.filter { it.type == CodeElementType.FIELD }.forEach { field ->
                val fieldType = field.metadata["type"] as? String
                if (fieldType != null && !isPrimitiveType(fieldType)) {
                    // 查找类型元素
                    val typeElement = findElementByName(allElements, fieldType)
                    if (typeElement != null) {
                        addRelation(
                            sourceId = classElement.id,
                            targetId = typeElement.id,
                            type = RelationType.DEPENDENCY,
                            metadata = mutableMapOf(
                                "description" to "${classElement.name} depends on ${typeElement.name} through field ${field.name}",
                                "fieldName" to field.name
                            )
                        )
                    }
                }
            }

            // 分析类中的方法参数和返回值类型依赖
            classElement.children.filter { it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR }.forEach { method ->
                // 分析返回类型
                val returnType = method.metadata["returnType"] as? String
                if (returnType != null && !isPrimitiveType(returnType) && returnType != "void") {
                    val typeElement = findElementByName(allElements, returnType)
                    if (typeElement != null) {
                        addRelation(
                            sourceId = classElement.id,
                            targetId = typeElement.id,
                            type = RelationType.DEPENDENCY,
                            metadata = mutableMapOf(
                                "description" to "${classElement.name} depends on ${typeElement.name} through method ${method.name} return type",
                                "methodName" to method.name
                            )
                        )
                    }
                }

                // 分析参数类型
                method.children.filter { it.type == CodeElementType.PARAMETER }.forEach { param ->
                    val paramType = param.metadata["type"] as? String
                    if (paramType != null && !isPrimitiveType(paramType)) {
                        val typeElement = findElementByName(allElements, paramType)
                        if (typeElement != null) {
                            addRelation(
                                sourceId = classElement.id,
                                targetId = typeElement.id,
                                type = RelationType.DEPENDENCY,
                                metadata = mutableMapOf(
                                    "description" to "${classElement.name} depends on ${typeElement.name} through method ${method.name} parameter ${param.name}",
                                    "methodName" to method.name,
                                    "paramName" to param.name
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 分析接口关系
     *
     * @param interfaceElement 接口元素
     * @param allElements 所有元素
     */
    private fun analyzeInterfaceRelations(interfaceElement: CodeElement, allElements: List<CodeElement>) {
        // 分析继承关系
        if (config.analyzeInheritance) {
            val superInterfaces = interfaceElement.metadata["superInterfaces"] as? List<String>
            superInterfaces?.forEach { superInterfaceName ->
                // 查找超接口元素
                val superInterfaceElement = findElementByQualifiedName(allElements, superInterfaceName)
                if (superInterfaceElement != null) {
                    addRelation(
                        sourceId = interfaceElement.id,
                        targetId = superInterfaceElement.id,
                        type = RelationType.INHERITANCE
                    )
                }
            }
        }

        // 分析依赖关系
        if (config.analyzeDependency) {
            // 分析接口中使用的其他类型
            val usedTypes = interfaceElement.metadata["usedTypes"] as? List<String>
            usedTypes?.forEach { typeName ->
                // 查找类型元素
                val typeElement = findElementByQualifiedName(allElements, typeName)
                if (typeElement != null) {
                    addRelation(
                        sourceId = interfaceElement.id,
                        targetId = typeElement.id,
                        type = RelationType.DEPENDENCY
                    )
                }
            }
        }
    }

    /**
     * 分析方法关系
     *
     * @param methodElement 方法元素
     * @param allElements 所有元素
     */
    private fun analyzeMethodRelations(methodElement: CodeElement, allElements: List<CodeElement>) {
        // 分析重写关系
        if (config.analyzeOverride) {
            // 检查方法上的Override注解
            val annotations = methodElement.metadata["annotations"] as? String
            if (annotations != null && annotations.contains("Override")) {
                // 查找父类中的同名方法
                val parentClass = methodElement.parent
                if (parentClass != null) {
                    // 查找父类的父类或实现的接口
                    val parentRelations = getOutgoingRelations(parentClass.id)
                        .filter { it.type == RelationType.INHERITANCE || it.type == RelationType.IMPLEMENTATION }

                    parentRelations.forEach { relation ->
                        val superType = allElements.find { it.id == relation.targetId }
                        if (superType != null) {
                            // 在超类型中查找同名方法
                            val overriddenMethod = superType.children.find {
                                it.type == CodeElementType.METHOD && it.name == methodElement.name
                            }

                            if (overriddenMethod != null) {
                                addRelation(
                                    sourceId = methodElement.id,
                                    targetId = overriddenMethod.id,
                                    type = RelationType.OVERRIDE,
                                    metadata = mutableMapOf(
                                        "description" to "${methodElement.name} in ${parentClass.name} overrides ${overriddenMethod.name} in ${superType.name}"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 分析使用关系
        if (config.analyzeUsage) {
            // 从 Chapi 解析器提取的方法调用信息
            val functionCalls = methodElement.metadata["functionCalls"] as? String
            if (functionCalls != null && functionCalls.isNotEmpty()) {
                val calledMethods = functionCalls.split(", ")
                calledMethods.forEach { methodName ->
                    // 查找被调用的方法元素
                    val calledMethodElements = allElements.filter {
                        it.type == CodeElementType.METHOD && it.name == methodName.trim()
                    }

                    calledMethodElements.forEach { calledMethod ->
                        addRelation(
                            sourceId = methodElement.id,
                            targetId = calledMethod.id,
                            type = RelationType.USAGE,
                            metadata = mutableMapOf(
                                "description" to "${methodElement.name} calls ${calledMethod.name}"
                            )
                        )
                    }
                }
            }
        }

        // 分析依赖关系
        if (config.analyzeDependency) {
            // 分析返回类型
            val returnType = methodElement.metadata["returnType"] as? String
            if (returnType != null && !isPrimitiveType(returnType) && returnType != "void") {
                val typeElement = findElementByName(allElements, returnType)
                if (typeElement != null) {
                    addRelation(
                        sourceId = methodElement.id,
                        targetId = typeElement.id,
                        type = RelationType.DEPENDENCY,
                        metadata = mutableMapOf(
                            "description" to "${methodElement.name} depends on ${typeElement.name} as return type"
                        )
                    )
                }
            }

            // 分析参数类型
            methodElement.children.filter { it.type == CodeElementType.PARAMETER }.forEach { param ->
                val paramType = param.metadata["type"] as? String
                if (paramType != null && !isPrimitiveType(paramType)) {
                    val typeElement = findElementByName(allElements, paramType)
                    if (typeElement != null) {
                        addRelation(
                            sourceId = methodElement.id,
                            targetId = typeElement.id,
                            type = RelationType.DEPENDENCY,
                            metadata = mutableMapOf(
                                "description" to "${methodElement.name} depends on ${typeElement.name} through parameter ${param.name}",
                                "paramName" to param.name
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * 分析字段关系
     *
     * @param fieldElement 字段元素
     * @param allElements 所有元素
     */
    private fun analyzeFieldRelations(fieldElement: CodeElement, allElements: List<CodeElement>) {
        // 分析依赖关系
        if (config.analyzeDependency) {
            // 获取字段类型
            val fieldType = fieldElement.metadata["type"] as? String
            if (fieldType != null) {
                // 查找类型元素
                val typeElement = findElementByQualifiedName(allElements, fieldType)
                if (typeElement != null) {
                    addRelation(
                        sourceId = fieldElement.id,
                        targetId = typeElement.id,
                        type = RelationType.DEPENDENCY
                    )
                }
            }
        }
    }

    /**
     * 分析导入关系
     *
     * @param importElement 导入元素
     * @param allElements 所有元素
     */
    private fun analyzeImportRelations(importElement: CodeElement, allElements: List<CodeElement>) {
        // 分析导入关系
        if (config.analyzeImport) {
            // 获取导入的类型名称
            val importName = importElement.name

            // 处理通配符导入
            if (importName.endsWith(".*")) {
                // 暂不处理通配符导入
                return
            }

            // 查找导入的类型元素
            val importedElement = findElementByQualifiedName(allElements, importName)
            if (importedElement != null) {
                // 获取导入所在的文件元素
                val fileElement = importElement.parent
                if (fileElement != null) {
                    addRelation(
                        sourceId = fileElement.id,
                        targetId = importedElement.id,
                        type = RelationType.IMPORT
                    )
                }
            }
        }
    }

    /**
     * 添加关系
     *
     * @param sourceId 源元素 ID
     * @param targetId 目标元素 ID
     * @param type 关系类型
     * @param metadata 元数据
     * @return 关系
     */
    private fun addRelation(
        sourceId: String,
        targetId: String,
        type: RelationType,
        metadata: MutableMap<String, Any> = mutableMapOf()
    ): CodeRelation {
        // 创建关系 ID
        val relationId = UUID.randomUUID().toString()

        // 创建关系
        val relation = CodeRelation(
            id = relationId,
            sourceId = sourceId,
            targetId = targetId,
            type = type,
            metadata = metadata
        )

        // 缓存关系
        relationCache[relationId] = relation

        // 更新元素到关系的映射
        elementToRelations.getOrPut(sourceId) { mutableSetOf() }.add(relationId)
        elementToRelations.getOrPut(targetId) { mutableSetOf() }.add(relationId)

        return relation
    }

    /**
     * 根据限定名查找元素
     *
     * @param elements 元素列表
     * @param qualifiedName 限定名
     * @return 元素，如果找不到则返回 null
     */
    private fun findElementByQualifiedName(elements: List<CodeElement>, qualifiedName: String): CodeElement? {
        return elements.find { it.qualifiedName == qualifiedName }
    }

    /**
     * 根据名称查找元素
     *
     * @param elements 元素列表
     * @param name 名称
     * @return 元素，如果找不到则返回 null
     */
    private fun findElementByName(elements: List<CodeElement>, name: String): CodeElement? {
        // 先尝试精确匹配
        val exactMatch = elements.find { it.name == name }
        if (exactMatch != null) {
            return exactMatch
        }

        // 如果没有精确匹配，尝试匹配简单名称（不包含包名）
        val simpleName = name.substringAfterLast('.')
        return elements.find {
            it.name == simpleName ||
            it.qualifiedName.endsWith(".$simpleName") ||
            it.qualifiedName == name
        }
    }

    /**
     * 检查是否为原始类型
     *
     * @param typeName 类型名称
     * @return 是否为原始类型
     */
    private fun isPrimitiveType(typeName: String): Boolean {
        val primitiveTypes = setOf(
            "int", "long", "short", "byte", "char", "boolean", "float", "double", "void",
            "Integer", "Long", "Short", "Byte", "Character", "Boolean", "Float", "Double", "Void",
            "String", "Object", "java.lang.String", "java.lang.Object",
            "Any", "Unit", "kotlin.Any", "kotlin.Unit", "kotlin.String"
        )

        return primitiveTypes.contains(typeName) ||
               typeName.startsWith("java.util.") ||
               typeName.startsWith("kotlin.collections.")
    }

    /**
     * 根据方法签名查找方法元素
     *
     * @param elements 元素列表
     * @param methodSignature 方法签名
     * @return 方法元素，如果找不到则返回 null
     */
    private fun findMethodBySignature(elements: List<CodeElement>, methodSignature: String): CodeElement? {
        return elements.find {
            it.type == CodeElementType.METHOD &&
            it.metadata["signature"] as? String == methodSignature
        }
    }

    /**
     * 根据字段名查找字段元素
     *
     * @param elements 元素列表
     * @param fieldName 字段名
     * @return 字段元素，如果找不到则返回 null
     */
    private fun findFieldByName(elements: List<CodeElement>, fieldName: String): CodeElement? {
        return elements.find {
            it.type == CodeElementType.FIELD &&
            it.name == fieldName
        }
    }

    /**
     * 获取元素的所有关系
     *
     * @param elementId 元素 ID
     * @return 关系列表
     */
    fun getElementRelations(elementId: String): List<CodeRelation> {
        val relationIds = elementToRelations[elementId] ?: return emptyList()
        return relationIds.mapNotNull { relationCache[it] }
    }

    /**
     * 获取元素的出向关系
     *
     * @param elementId 元素 ID
     * @return 关系列表
     */
    fun getOutgoingRelations(elementId: String): List<CodeRelation> {
        return getElementRelations(elementId).filter { it.sourceId == elementId }
    }

    /**
     * 获取元素的入向关系
     *
     * @param elementId 元素 ID
     * @return 关系列表
     */
    fun getIncomingRelations(elementId: String): List<CodeRelation> {
        return getElementRelations(elementId).filter { it.targetId == elementId }
    }

    /**
     * 获取特定类型的关系
     *
     * @param type 关系类型
     * @return 关系列表
     */
    fun getRelationsByType(type: RelationType): List<CodeRelation> {
        return relationCache.values.filter { it.type == type }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        relationCache.clear()
        elementToRelations.clear()
    }
}
