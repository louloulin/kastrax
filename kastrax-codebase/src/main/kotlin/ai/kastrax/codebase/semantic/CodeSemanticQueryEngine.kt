package ai.kastrax.codebase.semantic

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Modifier
import ai.kastrax.codebase.semantic.model.Visibility
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 代码语义查询引擎
 *
 * 提供查询代码库中语义信息的功能
 *
 * @property analyzer 代码语义分析器
 * @property relationAnalyzer 代码关系分析器
 */
class CodeSemanticQueryEngine(
    private val analyzer: CodeSemanticAnalyzer,
    private val relationAnalyzer: CodeRelationAnalyzer
) {
    // 查询缓存
    private val queryCache = ConcurrentHashMap<String, List<CodeElement>>()
    
    /**
     * 查询代码元素
     *
     * @param query 查询条件
     * @return 代码元素列表
     */
    suspend fun queryElements(query: ElementQuery): List<CodeElement> = withContext(Dispatchers.Default) {
        // 生成缓存键
        val cacheKey = query.toString()
        
        // 检查缓存
        val cachedResult = queryCache[cacheKey]
        if (cachedResult != null) {
            return@withContext cachedResult
        }
        
        // 执行查询
        val result = executeQuery(query)
        
        // 缓存结果
        queryCache[cacheKey] = result
        
        return@withContext result
    }
    
    /**
     * 执行查询
     *
     * @param query 查询条件
     * @return 代码元素列表
     */
    private fun executeQuery(query: ElementQuery): List<CodeElement> {
        // 根据 ID 查询
        if (query.id != null) {
            val element = analyzer.getElementById(query.id)
            return if (element != null) listOf(element) else emptyList()
        }
        
        // 根据位置查询
        if (query.location != null) {
            val element = analyzer.getElementAtLocation(query.location)
            return if (element != null) listOf(element) else emptyList()
        }
        
        // 根据名称查询
        if (query.name != null) {
            return analyzer.findElementsByName(query.name, query.type)
        }
        
        // 根据限定名查询
        if (query.qualifiedName != null) {
            val element = analyzer.findElementByQualifiedName(query.qualifiedName, query.type)
            return if (element != null) listOf(element) else emptyList()
        }
        
        // 根据类型查询
        if (query.type != null) {
            // 这需要遍历所有元素，可能效率较低
            // 在实际应用中，应该使用索引来优化
            return emptyList()
        }
        
        // 根据关系查询
        if (query.relationQuery != null) {
            return executeRelationQuery(query.relationQuery)
        }
        
        return emptyList()
    }
    
    /**
     * 执行关系查询
     *
     * @param query 关系查询条件
     * @return 代码元素列表
     */
    private fun executeRelationQuery(query: RelationQuery): List<CodeElement> {
        // 获取源元素
        val sourceElement = if (query.sourceId != null) {
            analyzer.getElementById(query.sourceId)
        } else {
            null
        }
        
        // 获取目标元素
        val targetElement = if (query.targetId != null) {
            analyzer.getElementById(query.targetId)
        } else {
            null
        }
        
        // 查询关系
        val relations = when {
            sourceElement != null && query.direction == RelationDirection.OUTGOING -> {
                relationAnalyzer.getOutgoingRelations(sourceElement.id, query.type)
            }
            sourceElement != null && query.direction == RelationDirection.INCOMING -> {
                relationAnalyzer.getIncomingRelations(sourceElement.id, query.type)
            }
            targetElement != null && query.direction == RelationDirection.OUTGOING -> {
                relationAnalyzer.getIncomingRelations(targetElement.id, query.type)
            }
            targetElement != null && query.direction == RelationDirection.INCOMING -> {
                relationAnalyzer.getOutgoingRelations(targetElement.id, query.type)
            }
            else -> {
                emptyList()
            }
        }
        
        // 获取关联元素
        return relations.mapNotNull { relation ->
            when {
                sourceElement != null && query.direction == RelationDirection.OUTGOING -> {
                    analyzer.getElementById(relation.targetId)
                }
                sourceElement != null && query.direction == RelationDirection.INCOMING -> {
                    analyzer.getElementById(relation.sourceId)
                }
                targetElement != null && query.direction == RelationDirection.OUTGOING -> {
                    analyzer.getElementById(relation.sourceId)
                }
                targetElement != null && query.direction == RelationDirection.INCOMING -> {
                    analyzer.getElementById(relation.targetId)
                }
                else -> {
                    null
                }
            }
        }
    }
    
    /**
     * 查询类的所有方法
     *
     * @param classElement 类元素
     * @return 方法元素列表
     */
    fun queryClassMethods(classElement: CodeElement): List<CodeElement> {
        return classElement.children.filter { it.type == CodeElementType.METHOD }
    }
    
    /**
     * 查询类的所有字段
     *
     * @param classElement 类元素
     * @return 字段元素列表
     */
    fun queryClassFields(classElement: CodeElement): List<CodeElement> {
        return classElement.children.filter { 
            it.type == CodeElementType.FIELD || it.type == CodeElementType.PROPERTY 
        }
    }
    
    /**
     * 查询类的所有构造函数
     *
     * @param classElement 类元素
     * @return 构造函数元素列表
     */
    fun queryClassConstructors(classElement: CodeElement): List<CodeElement> {
        return classElement.children.filter { it.type == CodeElementType.CONSTRUCTOR }
    }
    
    /**
     * 查询类的所有内部类
     *
     * @param classElement 类元素
     * @return 内部类元素列表
     */
    fun queryClassInnerClasses(classElement: CodeElement): List<CodeElement> {
        return classElement.children.filter { 
            it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE || it.type == CodeElementType.ENUM 
        }
    }
    
    /**
     * 查询类的父类
     *
     * @param classElement 类元素
     * @return 父类元素列表
     */
    fun queryClassSuperclasses(classElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getOutgoingRelations(classElement.id, RelationType.EXTENDS)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.targetId)
        }
    }
    
    /**
     * 查询类实现的接口
     *
     * @param classElement 类元素
     * @return 接口元素列表
     */
    fun queryClassInterfaces(classElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getOutgoingRelations(classElement.id, RelationType.IMPLEMENTS)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.targetId)
        }
    }
    
    /**
     * 查询类的子类
     *
     * @param classElement 类元素
     * @return 子类元素列表
     */
    fun queryClassSubclasses(classElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getIncomingRelations(classElement.id, RelationType.EXTENDS)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.sourceId)
        }
    }
    
    /**
     * 查询接口的实现类
     *
     * @param interfaceElement 接口元素
     * @return 实现类元素列表
     */
    fun queryInterfaceImplementations(interfaceElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getIncomingRelations(interfaceElement.id, RelationType.IMPLEMENTS)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.sourceId)
        }
    }
    
    /**
     * 查询方法的调用者
     *
     * @param methodElement 方法元素
     * @return 调用者元素列表
     */
    fun queryMethodCallers(methodElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getIncomingRelations(methodElement.id, RelationType.CALLS)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.sourceId)
        }
    }
    
    /**
     * 查询方法调用的方法
     *
     * @param methodElement 方法元素
     * @return 被调用的方法元素列表
     */
    fun queryMethodCalls(methodElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getOutgoingRelations(methodElement.id, RelationType.CALLS)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.targetId)
        }
    }
    
    /**
     * 查询方法重写的方法
     *
     * @param methodElement 方法元素
     * @return 被重写的方法元素列表
     */
    fun queryMethodOverrides(methodElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getOutgoingRelations(methodElement.id, RelationType.OVERRIDES)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.targetId)
        }
    }
    
    /**
     * 查询方法的重写方法
     *
     * @param methodElement 方法元素
     * @return 重写的方法元素列表
     */
    fun queryMethodOverriddenBy(methodElement: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getIncomingRelations(methodElement.id, RelationType.OVERRIDES)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.sourceId)
        }
    }
    
    /**
     * 查询元素的依赖
     *
     * @param element 元素
     * @return 依赖元素列表
     */
    fun queryElementDependencies(element: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getOutgoingRelations(element.id, RelationType.DEPENDS_ON)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.targetId)
        }
    }
    
    /**
     * 查询依赖元素的元素
     *
     * @param element 元素
     * @return 依赖该元素的元素列表
     */
    fun queryElementDependents(element: CodeElement): List<CodeElement> {
        val relations = relationAnalyzer.getIncomingRelations(element.id, RelationType.DEPENDS_ON)
        return relations.mapNotNull { relation ->
            analyzer.getElementById(relation.sourceId)
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        queryCache.clear()
    }
}

/**
 * 元素查询
 *
 * @property id 元素 ID
 * @property name 元素名称
 * @property qualifiedName 元素限定名
 * @property type 元素类型
 * @property location 元素位置
 * @property visibility 元素可见性
 * @property modifiers 元素修饰符
 * @property relationQuery 关系查询
 */
data class ElementQuery(
    val id: String? = null,
    val name: String? = null,
    val qualifiedName: String? = null,
    val type: CodeElementType? = null,
    val location: Location? = null,
    val visibility: Visibility? = null,
    val modifiers: Set<Modifier>? = null,
    val relationQuery: RelationQuery? = null
)

/**
 * 关系方向
 */
enum class RelationDirection {
    OUTGOING,
    INCOMING
}

/**
 * 关系查询
 *
 * @property sourceId 源元素 ID
 * @property targetId 目标元素 ID
 * @property type 关系类型
 * @property direction 关系方向
 */
data class RelationQuery(
    val sourceId: String? = null,
    val targetId: String? = null,
    val type: RelationType? = null,
    val direction: RelationDirection = RelationDirection.OUTGOING
)
