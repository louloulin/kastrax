package ai.kastrax.codebase.symbol

import ai.kastrax.codebase.symbol.model.SymbolGraph
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 符号查询引擎配置
 *
 * @property maxPathLength 最大路径长度
 * @property maxSearchResults 最大搜索结果数
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 */
data class SymbolQueryEngineConfig(
    val maxPathLength: Int = 10,
    val maxSearchResults: Int = 100,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000
)

/**
 * 符号查询引擎
 *
 * 用于执行复杂的符号查询
 *
 * @property indexer 符号索引器
 * @property config 配置
 */
class SymbolQueryEngine(
    private val indexer: SymbolIndexer,
    private val config: SymbolQueryEngineConfig = SymbolQueryEngineConfig()
) {
    // 查询缓存
    private val queryCache = ConcurrentHashMap<String, List<SymbolNode>>()
    
    /**
     * 查询符号
     *
     * @param query 查询条件
     * @return 符号节点列表
     */
    suspend fun querySymbols(query: SymbolQuery): List<SymbolNode> = withContext(Dispatchers.Default) {
        // 生成缓存键
        val cacheKey = query.toString()
        
        // 检查缓存
        if (config.enableCaching && queryCache.containsKey(cacheKey)) {
            return@withContext queryCache[cacheKey] ?: emptyList()
        }
        
        // 执行查询
        val result = executeQuery(query)
        
        // 缓存结果
        if (config.enableCaching) {
            // 如果缓存已满，移除最早的条目
            if (queryCache.size >= config.cacheSize) {
                val oldestKey = queryCache.keys.firstOrNull()
                if (oldestKey != null) {
                    queryCache.remove(oldestKey)
                }
            }
            
            queryCache[cacheKey] = result
        }
        
        return@withContext result
    }
    
    /**
     * 执行查询
     *
     * @param query 查询条件
     * @return 符号节点列表
     */
    private fun executeQuery(query: SymbolQuery): List<SymbolNode> {
        // 获取符号图
        val graph = indexer.getGraph() ?: return emptyList()
        
        // 根据 ID 查询
        if (query.id != null) {
            val node = graph.getNode(query.id)
            return if (node != null) listOf(node) else emptyList()
        }
        
        // 根据名称查询
        if (query.name != null) {
            return indexer.findSymbolsByName(query.name, query.type)
        }
        
        // 根据限定名查询
        if (query.qualifiedName != null) {
            return indexer.findSymbolsByQualifiedName(query.qualifiedName, query.type)
        }
        
        // 根据文件路径查询
        if (query.filePath != null) {
            return indexer.findSymbolsByFile(query.filePath, query.type)
        }
        
        // 根据类型查询
        if (query.type != null) {
            return indexer.findSymbolsByType(query.type)
        }
        
        // 全文搜索
        if (query.searchText != null) {
            return indexer.searchSymbols(query.searchText, query.type)
        }
        
        // 模糊搜索
        if (query.fuzzyText != null) {
            return indexer.fuzzySearchSymbols(query.fuzzyText, query.type)
        }
        
        // 关系查询
        if (query.relationQuery != null) {
            return executeRelationQuery(graph, query.relationQuery)
        }
        
        // 路径查询
        if (query.pathQuery != null) {
            return executePathQuery(graph, query.pathQuery)
        }
        
        return emptyList()
    }
    
    /**
     * 执行关系查询
     *
     * @param graph 符号图
     * @param query 关系查询条件
     * @return 符号节点列表
     */
    private fun executeRelationQuery(graph: SymbolGraph, query: RelationQuery): List<SymbolNode> {
        // 获取源节点
        val sourceNode = if (query.sourceId != null) {
            graph.getNode(query.sourceId)
        } else if (query.sourceName != null) {
            indexer.findSymbolsByName(query.sourceName, query.sourceType).firstOrNull()
        } else if (query.sourceQualifiedName != null) {
            indexer.findSymbolsByQualifiedName(query.sourceQualifiedName, query.sourceType).firstOrNull()
        } else {
            null
        }
        
        // 获取目标节点
        val targetNode = if (query.targetId != null) {
            graph.getNode(query.targetId)
        } else if (query.targetName != null) {
            indexer.findSymbolsByName(query.targetName, query.targetType).firstOrNull()
        } else if (query.targetQualifiedName != null) {
            indexer.findSymbolsByQualifiedName(query.targetQualifiedName, query.targetType).firstOrNull()
        } else {
            null
        }
        
        // 如果源节点和目标节点都为空，返回空列表
        if (sourceNode == null && targetNode == null) {
            return emptyList()
        }
        
        // 查询关系
        return when {
            sourceNode != null && query.direction == RelationDirection.OUTGOING -> {
                // 获取源节点的出关系
                graph.getOutgoingRelations(sourceNode.id, query.type).mapNotNull { relation ->
                    graph.getNode(relation.targetId)
                }
            }
            sourceNode != null && query.direction == RelationDirection.INCOMING -> {
                // 获取源节点的入关系
                graph.getIncomingRelations(sourceNode.id, query.type).mapNotNull { relation ->
                    graph.getNode(relation.sourceId)
                }
            }
            targetNode != null && query.direction == RelationDirection.OUTGOING -> {
                // 获取目标节点的入关系
                graph.getIncomingRelations(targetNode.id, query.type).mapNotNull { relation ->
                    graph.getNode(relation.sourceId)
                }
            }
            targetNode != null && query.direction == RelationDirection.INCOMING -> {
                // 获取目标节点的出关系
                graph.getOutgoingRelations(targetNode.id, query.type).mapNotNull { relation ->
                    graph.getNode(relation.targetId)
                }
            }
            else -> {
                emptyList()
            }
        }
    }
    
    /**
     * 执行路径查询
     *
     * @param graph 符号图
     * @param query 路径查询条件
     * @return 符号节点列表
     */
    private fun executePathQuery(graph: SymbolGraph, query: PathQuery): List<SymbolNode> {
        // 获取源节点
        val sourceNode = if (query.sourceId != null) {
            graph.getNode(query.sourceId)
        } else if (query.sourceName != null) {
            indexer.findSymbolsByName(query.sourceName, query.sourceType).firstOrNull()
        } else if (query.sourceQualifiedName != null) {
            indexer.findSymbolsByQualifiedName(query.sourceQualifiedName, query.sourceType).firstOrNull()
        } else {
            return emptyList()
        }
        
        // 获取目标节点
        val targetNode = if (query.targetId != null) {
            graph.getNode(query.targetId)
        } else if (query.targetName != null) {
            indexer.findSymbolsByName(query.targetName, query.targetType).firstOrNull()
        } else if (query.targetQualifiedName != null) {
            indexer.findSymbolsByQualifiedName(query.targetQualifiedName, query.targetType).firstOrNull()
        } else {
            return emptyList()
        }
        
        // 如果源节点或目标节点为空，返回空列表
        if (sourceNode == null || targetNode == null) {
            return emptyList()
        }
        
        // 查找路径
        return findPath(graph, sourceNode, targetNode, query.relationTypes, query.maxLength ?: config.maxPathLength)
    }
    
    /**
     * 查找路径
     *
     * @param graph 符号图
     * @param sourceNode 源节点
     * @param targetNode 目标节点
     * @param relationTypes 关系类型列表
     * @param maxLength 最大路径长度
     * @return 路径上的符号节点列表
     */
    private fun findPath(
        graph: SymbolGraph,
        sourceNode: SymbolNode,
        targetNode: SymbolNode,
        relationTypes: List<SymbolRelationType>?,
        maxLength: Int
    ): List<SymbolNode> {
        // 使用广度优先搜索查找最短路径
        val queue: Queue<Pair<String, List<String>>> = LinkedList()
        val visited = mutableSetOf<String>()
        
        // 初始化队列
        queue.add(sourceNode.id to listOf(sourceNode.id))
        visited.add(sourceNode.id)
        
        while (queue.isNotEmpty()) {
            val (nodeId, path) = queue.poll()
            
            // 如果找到目标节点，返回路径
            if (nodeId == targetNode.id) {
                return path.mapNotNull { graph.getNode(it) }
            }
            
            // 如果路径长度超过最大长度，跳过
            if (path.size >= maxLength) {
                continue
            }
            
            // 获取相邻节点
            val outgoingRelations = graph.getOutgoingRelations(nodeId)
                .filter { relationTypes == null || relationTypes.contains(it.type) }
            
            for (relation in outgoingRelations) {
                val nextNodeId = relation.targetId
                
                if (!visited.contains(nextNodeId)) {
                    visited.add(nextNodeId)
                    queue.add(nextNodeId to path + nextNodeId)
                }
            }
        }
        
        // 如果没有找到路径，返回空列表
        return emptyList()
    }
    
    /**
     * 查询类的所有方法
     *
     * @param classNode 类符号节点
     * @return 方法符号节点列表
     */
    fun queryClassMethods(classNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(classNode.id, true, SymbolRelationType.CONTAINS)
            .filter { it.type == SymbolType.METHOD || it.type == SymbolType.CONSTRUCTOR }
    }
    
    /**
     * 查询类的所有字段
     *
     * @param classNode 类符号节点
     * @return 字段符号节点列表
     */
    fun queryClassFields(classNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(classNode.id, true, SymbolRelationType.CONTAINS)
            .filter { it.type == SymbolType.FIELD || it.type == SymbolType.PROPERTY }
    }
    
    /**
     * 查询类的所有内部类
     *
     * @param classNode 类符号节点
     * @return 内部类符号节点列表
     */
    fun queryClassInnerClasses(classNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(classNode.id, true, SymbolRelationType.CONTAINS)
            .filter { it.type == SymbolType.CLASS || it.type == SymbolType.INTERFACE || it.type == SymbolType.ENUM }
    }
    
    /**
     * 查询类的父类
     *
     * @param classNode 类符号节点
     * @return 父类符号节点列表
     */
    fun queryClassSuperclasses(classNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(classNode.id, true, SymbolRelationType.EXTENDS)
    }
    
    /**
     * 查询类实现的接口
     *
     * @param classNode 类符号节点
     * @return 接口符号节点列表
     */
    fun queryClassInterfaces(classNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(classNode.id, true, SymbolRelationType.IMPLEMENTS)
    }
    
    /**
     * 查询类的子类
     *
     * @param classNode 类符号节点
     * @return 子类符号节点列表
     */
    fun queryClassSubclasses(classNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(classNode.id, false, SymbolRelationType.EXTENDS)
    }
    
    /**
     * 查询接口的实现类
     *
     * @param interfaceNode 接口符号节点
     * @return 实现类符号节点列表
     */
    fun queryInterfaceImplementations(interfaceNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(interfaceNode.id, false, SymbolRelationType.IMPLEMENTS)
    }
    
    /**
     * 查询方法的调用者
     *
     * @param methodNode 方法符号节点
     * @return 调用者符号节点列表
     */
    fun queryMethodCallers(methodNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(methodNode.id, false, SymbolRelationType.CALLS)
    }
    
    /**
     * 查询方法调用的方法
     *
     * @param methodNode 方法符号节点
     * @return 被调用的方法符号节点列表
     */
    fun queryMethodCalls(methodNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(methodNode.id, true, SymbolRelationType.CALLS)
    }
    
    /**
     * 查询方法重写的方法
     *
     * @param methodNode 方法符号节点
     * @return 被重写的方法符号节点列表
     */
    fun queryMethodOverrides(methodNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(methodNode.id, true, SymbolRelationType.OVERRIDES)
    }
    
    /**
     * 查询方法的重写方法
     *
     * @param methodNode 方法符号节点
     * @return 重写的方法符号节点列表
     */
    fun queryMethodOverriddenBy(methodNode: SymbolNode): List<SymbolNode> {
        val graph = indexer.getGraph() ?: return emptyList()
        
        return graph.getAdjacentNodes(methodNode.id, false, SymbolRelationType.OVERRIDES)
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        queryCache.clear()
    }
}

/**
 * 符号查询
 *
 * @property id 符号 ID
 * @property name 符号名称
 * @property qualifiedName 符号限定名
 * @property type 符号类型
 * @property filePath 文件路径
 * @property searchText 搜索文本
 * @property fuzzyText 模糊搜索文本
 * @property relationQuery 关系查询
 * @property pathQuery 路径查询
 */
data class SymbolQuery(
    val id: String? = null,
    val name: String? = null,
    val qualifiedName: String? = null,
    val type: SymbolType? = null,
    val filePath: Path? = null,
    val searchText: String? = null,
    val fuzzyText: String? = null,
    val relationQuery: RelationQuery? = null,
    val pathQuery: PathQuery? = null
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
 * @property sourceId 源符号 ID
 * @property sourceName 源符号名称
 * @property sourceQualifiedName 源符号限定名
 * @property sourceType 源符号类型
 * @property targetId 目标符号 ID
 * @property targetName 目标符号名称
 * @property targetQualifiedName 目标符号限定名
 * @property targetType 目标符号类型
 * @property type 关系类型
 * @property direction 关系方向
 */
data class RelationQuery(
    val sourceId: String? = null,
    val sourceName: String? = null,
    val sourceQualifiedName: String? = null,
    val sourceType: SymbolType? = null,
    val targetId: String? = null,
    val targetName: String? = null,
    val targetQualifiedName: String? = null,
    val targetType: SymbolType? = null,
    val type: SymbolRelationType? = null,
    val direction: RelationDirection = RelationDirection.OUTGOING
)

/**
 * 路径查询
 *
 * @property sourceId 源符号 ID
 * @property sourceName 源符号名称
 * @property sourceQualifiedName 源符号限定名
 * @property sourceType 源符号类型
 * @property targetId 目标符号 ID
 * @property targetName 目标符号名称
 * @property targetQualifiedName 目标符号限定名
 * @property targetType 目标符号类型
 * @property relationTypes 关系类型列表
 * @property maxLength 最大路径长度
 */
data class PathQuery(
    val sourceId: String? = null,
    val sourceName: String? = null,
    val sourceQualifiedName: String? = null,
    val sourceType: SymbolType? = null,
    val targetId: String? = null,
    val targetName: String? = null,
    val targetQualifiedName: String? = null,
    val targetType: SymbolType? = null,
    val relationTypes: List<SymbolRelationType>? = null,
    val maxLength: Int? = null
)
