package ai.kastrax.codebase.semantic

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 代码关系类型
 */
enum class RelationType {
    EXTENDS,
    IMPLEMENTS,
    USES,
    CALLS,
    REFERENCES,
    CONTAINS,
    IMPORTS,
    OVERRIDES,
    DEPENDS_ON,
    UNKNOWN
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
 * 代码关系分析器配置
 *
 * @property analyzeInheritance 是否分析继承关系
 * @property analyzeUsage 是否分析使用关系
 * @property analyzeDependency 是否分析依赖关系
 * @property analyzeOverride 是否分析重写关系
 */
data class CodeRelationAnalyzerConfig(
    val analyzeInheritance: Boolean = true,
    val analyzeUsage: Boolean = true,
    val analyzeDependency: Boolean = true,
    val analyzeOverride: Boolean = true
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
    private val sourceRelations = ConcurrentHashMap<String, MutableSet<String>>()
    private val targetRelations = ConcurrentHashMap<String, MutableSet<String>>()
    
    /**
     * 分析代码关系
     *
     * @param codebase 代码库元素
     * @return 代码关系列表
     */
    suspend fun analyzeRelations(codebase: CodeElement): List<CodeRelation> = withContext(Dispatchers.Default) {
        logger.info { "开始分析代码关系" }
        
        // 清除缓存
        clearCache()
        
        // 获取所有代码元素
        val allElements = codebase.getAllChildren() + codebase
        
        // 分析继承关系
        if (config.analyzeInheritance) {
            analyzeInheritanceRelations(allElements)
        }
        
        // 分析使用关系
        if (config.analyzeUsage) {
            analyzeUsageRelations(allElements)
        }
        
        // 分析依赖关系
        if (config.analyzeDependency) {
            analyzeDependencyRelations(allElements)
        }
        
        // 分析重写关系
        if (config.analyzeOverride) {
            analyzeOverrideRelations(allElements)
        }
        
        logger.info { "代码关系分析完成，共 ${relationCache.size} 个关系" }
        
        return@withContext relationCache.values.toList()
    }
    
    /**
     * 分析继承关系
     *
     * @param elements 代码元素列表
     */
    private fun analyzeInheritanceRelations(elements: List<CodeElement>) {
        logger.debug { "分析继承关系" }
        
        // 查找所有类和接口
        val classElements = elements.filter { 
            it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE || it.type == CodeElementType.ENUM 
        }
        
        // 分析继承关系
        for (element in classElements) {
            // 分析 extends 关系
            val extendsList = element.metadata["extends"] as? List<*> ?: emptyList<String>()
            for (extend in extendsList) {
                val extendName = extend.toString()
                
                // 查找目标元素
                val targetElement = findElementByName(elements, extendName)
                if (targetElement != null) {
                    // 创建关系
                    createRelation(
                        sourceId = element.id,
                        targetId = targetElement.id,
                        type = RelationType.EXTENDS
                    )
                }
            }
            
            // 分析 implements 关系
            val implementsList = element.metadata["implements"] as? List<*> ?: emptyList<String>()
            for (implement in implementsList) {
                val implementName = implement.toString()
                
                // 查找目标元素
                val targetElement = findElementByName(elements, implementName)
                if (targetElement != null) {
                    // 创建关系
                    createRelation(
                        sourceId = element.id,
                        targetId = targetElement.id,
                        type = RelationType.IMPLEMENTS
                    )
                }
            }
        }
    }
    
    /**
     * 分析使用关系
     *
     * @param elements 代码元素列表
     */
    private fun analyzeUsageRelations(elements: List<CodeElement>) {
        logger.debug { "分析使用关系" }
        
        // 查找所有方法
        val methodElements = elements.filter { 
            it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR 
        }
        
        // 分析方法调用关系
        for (method in methodElements) {
            // 获取方法体中的调用
            val calls = method.metadata["calls"] as? List<*> ?: emptyList<String>()
            for (call in calls) {
                val callName = call.toString()
                
                // 查找目标元素
                val targetElement = findElementByName(elements, callName)
                if (targetElement != null) {
                    // 创建关系
                    createRelation(
                        sourceId = method.id,
                        targetId = targetElement.id,
                        type = RelationType.CALLS
                    )
                }
            }
            
            // 获取方法体中的引用
            val references = method.metadata["references"] as? List<*> ?: emptyList<String>()
            for (reference in references) {
                val referenceName = reference.toString()
                
                // 查找目标元素
                val targetElement = findElementByName(elements, referenceName)
                if (targetElement != null) {
                    // 创建关系
                    createRelation(
                        sourceId = method.id,
                        targetId = targetElement.id,
                        type = RelationType.REFERENCES
                    )
                }
            }
        }
    }
    
    /**
     * 分析依赖关系
     *
     * @param elements 代码元素列表
     */
    private fun analyzeDependencyRelations(elements: List<CodeElement>) {
        logger.debug { "分析依赖关系" }
        
        // 查找所有类和接口
        val classElements = elements.filter { 
            it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE || it.type == CodeElementType.ENUM 
        }
        
        // 分析导入关系
        val fileElements = elements.filter { it.type == CodeElementType.FILE }
        for (file in fileElements) {
            // 获取导入
            val importElements = file.children.filter { it.type == CodeElementType.IMPORT }
            
            // 获取文件中的类和接口
            val fileClasses = file.children.filter { 
                it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE || it.type == CodeElementType.ENUM 
            }
            
            // 为每个导入创建关系
            for (import in importElements) {
                val importName = import.name
                
                // 查找目标元素
                val targetElement = findElementByName(elements, importName)
                if (targetElement != null) {
                    // 为文件中的每个类创建导入关系
                    for (fileClass in fileClasses) {
                        createRelation(
                            sourceId = fileClass.id,
                            targetId = targetElement.id,
                            type = RelationType.IMPORTS
                        )
                    }
                }
            }
        }
        
        // 分析字段和参数类型依赖
        for (element in classElements) {
            // 分析字段
            val fieldElements = element.children.filter { it.type == CodeElementType.FIELD || it.type == CodeElementType.PROPERTY }
            for (field in fieldElements) {
                val fieldType = field.metadata["type"] as? String ?: continue
                
                // 查找目标元素
                val targetElement = findElementByName(elements, fieldType)
                if (targetElement != null) {
                    // 创建关系
                    createRelation(
                        sourceId = element.id,
                        targetId = targetElement.id,
                        type = RelationType.DEPENDS_ON
                    )
                }
            }
            
            // 分析方法参数和返回类型
            val methodElements = element.children.filter { 
                it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR 
            }
            for (method in methodElements) {
                // 分析返回类型
                val returnType = method.metadata["returnType"] as? String ?: continue
                if (returnType != "void" && returnType != "Unit") {
                    // 查找目标元素
                    val targetElement = findElementByName(elements, returnType)
                    if (targetElement != null) {
                        // 创建关系
                        createRelation(
                            sourceId = element.id,
                            targetId = targetElement.id,
                            type = RelationType.DEPENDS_ON
                        )
                    }
                }
                
                // 分析参数类型
                val parameterElements = method.children.filter { it.type == CodeElementType.PARAMETER }
                for (parameter in parameterElements) {
                    val parameterType = parameter.metadata["type"] as? String ?: continue
                    
                    // 查找目标元素
                    val targetElement = findElementByName(elements, parameterType)
                    if (targetElement != null) {
                        // 创建关系
                        createRelation(
                            sourceId = element.id,
                            targetId = targetElement.id,
                            type = RelationType.DEPENDS_ON
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 分析重写关系
     *
     * @param elements 代码元素列表
     */
    private fun analyzeOverrideRelations(elements: List<CodeElement>) {
        logger.debug { "分析重写关系" }
        
        // 查找所有方法
        val methodElements = elements.filter { it.type == CodeElementType.METHOD }
        
        // 分析重写关系
        for (method in methodElements) {
            // 检查是否有 override 修饰符
            if (method.modifiers.contains(ai.kastrax.codebase.semantic.model.Modifier.OVERRIDE)) {
                // 获取方法所属的类
                val parentClass = method.parent
                if (parentClass != null) {
                    // 获取父类和接口
                    val extendsList = parentClass.metadata["extends"] as? List<*> ?: emptyList<String>()
                    val implementsList = parentClass.metadata["implements"] as? List<*> ?: emptyList<String>()
                    
                    // 合并父类和接口列表
                    val superTypes = extendsList + implementsList
                    
                    // 查找父类和接口中的同名方法
                    for (superType in superTypes) {
                        val superTypeName = superType.toString()
                        
                        // 查找目标元素
                        val targetClass = findElementByName(elements, superTypeName)
                        if (targetClass != null) {
                            // 查找同名方法
                            val targetMethod = targetClass.children.find { 
                                it.type == CodeElementType.METHOD && it.name == method.name 
                            }
                            
                            if (targetMethod != null) {
                                // 创建关系
                                createRelation(
                                    sourceId = method.id,
                                    targetId = targetMethod.id,
                                    type = RelationType.OVERRIDES
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 根据名称查找元素
     *
     * @param elements 元素列表
     * @param name 名称
     * @return 元素，如果不存在则返回 null
     */
    private fun findElementByName(elements: List<CodeElement>, name: String): CodeElement? {
        // 首先尝试精确匹配
        val exactMatch = elements.find { it.name == name }
        if (exactMatch != null) {
            return exactMatch
        }
        
        // 尝试匹配简单名称（不包含包名）
        val simpleName = name.substringAfterLast('.')
        return elements.find { it.name == simpleName }
    }
    
    /**
     * 创建关系
     *
     * @param sourceId 源元素 ID
     * @param targetId 目标元素 ID
     * @param type 关系类型
     * @param metadata 元数据
     * @return 关系
     */
    private fun createRelation(
        sourceId: String,
        targetId: String,
        type: RelationType,
        metadata: Map<String, Any> = emptyMap()
    ): CodeRelation {
        // 检查是否已存在相同关系
        val existingRelation = relationCache.values.find { 
            it.sourceId == sourceId && it.targetId == targetId && it.type == type 
        }
        
        if (existingRelation != null) {
            return existingRelation
        }
        
        // 创建新关系
        val relation = CodeRelation(
            id = UUID.randomUUID().toString(),
            sourceId = sourceId,
            targetId = targetId,
            type = type,
            metadata = metadata.toMutableMap()
        )
        
        // 缓存关系
        relationCache[relation.id] = relation
        
        // 更新映射
        sourceRelations.computeIfAbsent(sourceId) { mutableSetOf() }.add(relation.id)
        targetRelations.computeIfAbsent(targetId) { mutableSetOf() }.add(relation.id)
        
        return relation
    }
    
    /**
     * 获取元素的出关系
     *
     * @param elementId 元素 ID
     * @param type 关系类型，如果为 null 则不限制类型
     * @return 关系列表
     */
    fun getOutgoingRelations(elementId: String, type: RelationType? = null): List<CodeRelation> {
        val relationIds = sourceRelations[elementId] ?: emptySet()
        return relationIds.mapNotNull { relationCache[it] }
            .filter { type == null || it.type == type }
    }
    
    /**
     * 获取元素的入关系
     *
     * @param elementId 元素 ID
     * @param type 关系类型，如果为 null 则不限制类型
     * @return 关系列表
     */
    fun getIncomingRelations(elementId: String, type: RelationType? = null): List<CodeRelation> {
        val relationIds = targetRelations[elementId] ?: emptySet()
        return relationIds.mapNotNull { relationCache[it] }
            .filter { type == null || it.type == type }
    }
    
    /**
     * 获取所有关系
     *
     * @return 关系列表
     */
    fun getAllRelations(): List<CodeRelation> {
        return relationCache.values.toList()
    }
    
    /**
     * 根据 ID 获取关系
     *
     * @param id 关系 ID
     * @return 关系，如果不存在则返回 null
     */
    fun getRelationById(id: String): CodeRelation? {
        return relationCache[id]
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        relationCache.clear()
        sourceRelations.clear()
        targetRelations.clear()
    }
}
