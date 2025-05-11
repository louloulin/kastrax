package ai.kastrax.codebase.retrieval.context

import ai.kastrax.codebase.semantic.memory.SemanticMemory
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * 上下文级别
 */
enum class ContextLevel {
    GLOBAL,      // 全局上下文
    PROJECT,     // 项目上下文
    MODULE,      // 模块上下文
    PACKAGE,     // 包上下文
    FILE,        // 文件上下文
    CLASS,       // 类上下文
    METHOD,      // 方法上下文
    LOCAL        // 局部上下文
}

/**
 * 上下文类型
 */
enum class ContextType {
    CODE,           // 代码上下文
    DOCUMENTATION,  // 文档上下文
    COMMENT,        // 注释上下文
    QUERY,          // 查询上下文
    HISTORY,        // 历史上下文
    USER_FEEDBACK,  // 用户反馈上下文
    CUSTOM          // 自定义上下文
}

/**
 * 上下文元素
 *
 * @property id 唯一标识符
 * @property level 上下文级别
 * @property type 上下文类型
 * @property content 上下文内容
 * @property path 路径
 * @property parent 父上下文
 * @property children 子上下文列表
 * @property metadata 元数据
 */
data class ContextElement(
    val id: String,
    val level: ContextLevel,
    val type: ContextType,
    val content: String,
    val path: Path? = null,
    val parent: ContextElement? = null,
    val children: MutableList<ContextElement> = mutableListOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 获取上下文名称
     *
     * @return 上下文名称
     */
    fun getName(): String {
        return when {
            path != null -> path.name
            metadata.containsKey("name") -> metadata["name"].toString()
            else -> id
        }
    }
    
    /**
     * 获取完整路径
     *
     * @return 完整路径
     */
    fun getFullPath(): String {
        val parentPath = parent?.getFullPath()
        val name = getName()
        
        return if (parentPath != null && parentPath.isNotEmpty()) {
            "$parentPath/$name"
        } else {
            name
        }
    }
    
    /**
     * 添加子上下文
     *
     * @param child 子上下文
     */
    fun addChild(child: ContextElement) {
        children.add(child)
    }
    
    /**
     * 移除子上下文
     *
     * @param childId 子上下文 ID
     * @return 是否成功移除
     */
    fun removeChild(childId: String): Boolean {
        return children.removeIf { it.id == childId }
    }
    
    /**
     * 查找子上下文
     *
     * @param childId 子上下文 ID
     * @return 子上下文，如果不存在则返回 null
     */
    fun findChild(childId: String): ContextElement? {
        return children.find { it.id == childId }
    }
    
    /**
     * 获取所有子上下文
     *
     * @param recursive 是否递归获取
     * @return 子上下文列表
     */
    fun getAllChildren(recursive: Boolean = false): List<ContextElement> {
        return if (recursive) {
            children + children.flatMap { it.getAllChildren(true) }
        } else {
            children
        }
    }
    
    /**
     * 获取上下文层次结构
     *
     * @return 上下文层次结构
     */
    fun getHierarchy(): List<ContextElement> {
        val hierarchy = mutableListOf<ContextElement>()
        var current: ContextElement? = this
        
        while (current != null) {
            hierarchy.add(0, current)
            current = current.parent
        }
        
        return hierarchy
    }
}

/**
 * 上下文层次结构
 *
 * @property name 名称
 */
class ContextHierarchy(
    val name: String
) {
    // 根上下文
    private val rootContext = ContextElement(
        id = "root",
        level = ContextLevel.GLOBAL,
        type = ContextType.CODE,
        content = "Root Context"
    )
    
    // 上下文映射
    private val contextMap = ConcurrentHashMap<String, ContextElement>()
    
    // 路径映射
    private val pathMap = ConcurrentHashMap<String, ContextElement>()
    
    init {
        // 添加根上下文到映射
        contextMap[rootContext.id] = rootContext
    }
    
    /**
     * 添加上下文
     *
     * @param context 上下文元素
     * @param parentId 父上下文 ID，如果为 null 则添加到根上下文
     * @return 是否成功添加
     */
    fun addContext(context: ContextElement, parentId: String? = null): Boolean {
        try {
            // 检查上下文是否已存在
            if (contextMap.containsKey(context.id)) {
                return false
            }
            
            // 获取父上下文
            val parent = if (parentId != null) {
                contextMap[parentId] ?: return false
            } else {
                rootContext
            }
            
            // 创建新上下文（带父上下文）
            val newContext = context.copy(parent = parent)
            
            // 添加到父上下文
            parent.addChild(newContext)
            
            // 添加到映射
            contextMap[newContext.id] = newContext
            
            // 如果有路径，添加到路径映射
            if (newContext.path != null) {
                pathMap[newContext.path.toString()] = newContext
            }
            
            return true
        } catch (e: Exception) {
            logger.error(e) { "添加上下文失败: ${context.id}, ${e.message}" }
            return false
        }
    }
    
    /**
     * 移除上下文
     *
     * @param contextId 上下文 ID
     * @return 是否成功移除
     */
    fun removeContext(contextId: String): Boolean {
        try {
            // 检查上下文是否存在
            val context = contextMap[contextId] ?: return false
            
            // 如果是根上下文，不能移除
            if (context.id == rootContext.id) {
                return false
            }
            
            // 获取父上下文
            val parent = context.parent ?: return false
            
            // 从父上下文移除
            parent.removeChild(contextId)
            
            // 递归移除所有子上下文
            val allChildren = context.getAllChildren(true)
            allChildren.forEach { child ->
                contextMap.remove(child.id)
                if (child.path != null) {
                    pathMap.remove(child.path.toString())
                }
            }
            
            // 从映射中移除
            contextMap.remove(contextId)
            if (context.path != null) {
                pathMap.remove(context.path.toString())
            }
            
            return true
        } catch (e: Exception) {
            logger.error(e) { "移除上下文失败: $contextId, ${e.message}" }
            return false
        }
    }
    
    /**
     * 获取上下文
     *
     * @param contextId 上下文 ID
     * @return 上下文元素，如果不存在则返回 null
     */
    fun getContext(contextId: String): ContextElement? {
        return contextMap[contextId]
    }
    
    /**
     * 根据路径获取上下文
     *
     * @param path 路径
     * @return 上下文元素，如果不存在则返回 null
     */
    fun getContextByPath(path: Path): ContextElement? {
        return pathMap[path.toString()]
    }
    
    /**
     * 获取根上下文
     *
     * @return 根上下文
     */
    fun getRootContext(): ContextElement {
        return rootContext
    }
    
    /**
     * 获取所有上下文
     *
     * @return 上下文列表
     */
    fun getAllContexts(): List<ContextElement> {
        return contextMap.values.toList()
    }
    
    /**
     * 获取指定级别的上下文
     *
     * @param level 上下文级别
     * @return 上下文列表
     */
    fun getContextsByLevel(level: ContextLevel): List<ContextElement> {
        return contextMap.values.filter { it.level == level }
    }
    
    /**
     * 获取指定类型的上下文
     *
     * @param type 上下文类型
     * @return 上下文列表
     */
    fun getContextsByType(type: ContextType): List<ContextElement> {
        return contextMap.values.filter { it.type == type }
    }
    
    /**
     * 查找上下文
     *
     * @param predicate 断言函数
     * @return 上下文列表
     */
    fun findContexts(predicate: (ContextElement) -> Boolean): List<ContextElement> {
        return contextMap.values.filter(predicate)
    }
    
    /**
     * 获取上下文层次结构
     *
     * @param contextId 上下文 ID
     * @return 上下文层次结构
     */
    fun getContextHierarchy(contextId: String): List<ContextElement> {
        val context = contextMap[contextId] ?: return emptyList()
        return context.getHierarchy()
    }
    
    /**
     * 清空层次结构
     */
    fun clear() {
        // 清空子上下文
        rootContext.children.clear()
        
        // 清空映射
        contextMap.clear()
        pathMap.clear()
        
        // 重新添加根上下文
        contextMap[rootContext.id] = rootContext
    }
    
    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    fun getStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        stats["totalContexts"] = contextMap.size
        
        // 按级别统计
        val contextsByLevel = contextMap.values.groupBy { it.level }
            .mapValues { it.value.size }
        stats["contextsByLevel"] = contextsByLevel
        
        // 按类型统计
        val contextsByType = contextMap.values.groupBy { it.type }
            .mapValues { it.value.size }
        stats["contextsByType"] = contextsByType
        
        return stats
    }
}
