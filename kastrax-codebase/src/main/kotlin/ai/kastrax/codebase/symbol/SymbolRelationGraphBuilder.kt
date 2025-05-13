package ai.kastrax.codebase.symbol

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.relation.CodeRelation
import ai.kastrax.codebase.semantic.relation.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.relation.RelationType
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelation
import ai.kastrax.codebase.symbol.model.SymbolRelationGraph
import ai.kastrax.codebase.symbol.model.SymbolRelationGraphConfig
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 符号关系图构建器配置
 *
 * @property maxDepth 最大分析深度
 * @property includeInheritance 是否包含继承关系
 * @property includeImplementation 是否包含实现关系
 * @property includeUsage 是否包含使用关系
 * @property includeDependency 是否包含依赖关系
 * @property includeOverride 是否包含重写关系
 * @property includeReference 是否包含引用关系
 * @property includeImport 是否包含导入关系
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 */
data class SymbolRelationGraphBuilderConfig(
    val maxDepth: Int = 3,
    val includeInheritance: Boolean = true,
    val includeImplementation: Boolean = true,
    val includeUsage: Boolean = true,
    val includeDependency: Boolean = true,
    val includeOverride: Boolean = true,
    val includeReference: Boolean = true,
    val includeImport: Boolean = true,
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100
)

/**
 * 符号关系图构建器
 *
 * 用于构建符号关系图
 *
 * @property relationAnalyzer 代码关系分析器
 * @property config 配置
 */
class SymbolRelationGraphBuilder(
    private val relationAnalyzer: CodeRelationAnalyzer,
    private val config: SymbolRelationGraphBuilderConfig = SymbolRelationGraphBuilderConfig()
) {
    // 缓存
    private val graphCache = ConcurrentHashMap<String, SymbolRelationGraph>()

    /**
     * 构建符号关系图
     *
     * @param rootElement 根代码元素
     * @return 符号关系图
     */
    suspend fun buildGraph(rootElement: CodeElement): SymbolRelationGraph = withContext(Dispatchers.Default) {
        logger.info { "开始构建符号关系图: ${rootElement.qualifiedName}" }

        // 检查缓存
        val cacheKey = rootElement.id
        if (config.enableCaching && graphCache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取符号关系图: ${rootElement.qualifiedName}" }
            return@withContext graphCache[cacheKey]!!
        }

        // 分析代码关系
        val codeRelations = relationAnalyzer.analyzeRelations(rootElement)
        logger.info { "分析到 ${codeRelations.size} 个代码关系" }

        // 收集所有元素
        val allElements = collectAllElements(rootElement)
        logger.info { "收集到 ${allElements.size} 个代码元素" }

        // 创建符号关系图
        val graph = createSymbolRelationGraph(allElements, codeRelations)
        logger.info { "符号关系图构建完成: ${graph.nodes.size} 个节点, ${graph.edges.size} 个边" }

        // 缓存图
        if (config.enableCaching) {
            // 维护缓存大小
            if (graphCache.size >= config.maxCacheSize) {
                // 简单的缓存淘汰策略：随机移除一个
                val keyToRemove = graphCache.keys.firstOrNull()
                if (keyToRemove != null) {
                    graphCache.remove(keyToRemove)
                }
            }
            graphCache[cacheKey] = graph
        }

        return@withContext graph
    }

    /**
     * 构建符号关系图
     *
     * @param element 代码元素
     * @param maxDepth 最大深度
     * @param relationTypes 关系类型集合
     * @return 符号关系图
     */
    suspend fun buildGraph(
        element: CodeElement,
        maxDepth: Int = config.maxDepth,
        relationTypes: Set<RelationType> = getRelationTypes()
    ): SymbolRelationGraph = withContext(Dispatchers.Default) {
        logger.info { "开始构建符号关系图: ${element.qualifiedName}, 最大深度: $maxDepth" }

        // 检查缓存
        val cacheKey = "${element.id}:$maxDepth:${relationTypes.joinToString(",")}"
        if (config.enableCaching && graphCache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取符号关系图: ${element.qualifiedName}" }
            return@withContext graphCache[cacheKey]!!
        }

        // 创建图
        val graph = SymbolRelationGraph(
            name = "Symbol Relation Graph for ${element.name}"
        )

        // 添加根节点
        val rootNode = createSymbolNode(element)
        graph.addNode(rootNode)

        // 递归构建图
        buildGraphRecursive(graph, element, maxDepth, relationTypes, mutableSetOf(element.id))

        logger.info { "符号关系图构建完成: ${graph.nodes.size} 个节点, ${graph.edges.size} 个边" }

        // 缓存图
        if (config.enableCaching) {
            // 维护缓存大小
            if (graphCache.size >= config.maxCacheSize) {
                // 简单的缓存淘汰策略：随机移除一个
                val keyToRemove = graphCache.keys.firstOrNull()
                if (keyToRemove != null) {
                    graphCache.remove(keyToRemove)
                }
            }
            graphCache[cacheKey] = graph
        }

        return@withContext graph
    }

    /**
     * 递归构建图
     *
     * @param graph 符号关系图
     * @param element 代码元素
     * @param depth 当前深度
     * @param relationTypes 关系类型集合
     * @param visited 已访问的元素ID集合
     */
    private suspend fun buildGraphRecursive(
        graph: SymbolRelationGraph,
        element: CodeElement,
        depth: Int,
        relationTypes: Set<RelationType>,
        visited: MutableSet<String>
    ) {
        if (depth <= 0) {
            return
        }

        // 获取元素的关系
        val relations = relationAnalyzer.getElementRelations(element.id)
            .filter { it.type in relationTypes }

        for (relation in relations) {
            // 获取相关元素
            val relatedId = if (relation.sourceId == element.id) relation.targetId else relation.sourceId
            if (relatedId in visited) {
                continue
            }

            // 获取相关元素
            val relatedElement = relationAnalyzer.getElementById(relatedId)
            if (relatedElement != null) {
                // 添加相关元素节点
                val relatedNode = createSymbolNode(relatedElement)
                graph.addNode(relatedNode)

                // 添加关系边
                val symbolRelation = createSymbolRelation(relation)
                graph.addEdge(symbolRelation)

                // 递归处理相关元素
                visited.add(relatedId)
                buildGraphRecursive(graph, relatedElement, depth - 1, relationTypes, visited)
            }
        }
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
     * 创建符号关系图
     *
     * @param elements 代码元素列表
     * @param relations 代码关系列表
     * @return 符号关系图
     */
    private fun createSymbolRelationGraph(
        elements: List<CodeElement>,
        relations: List<CodeRelation>
    ): SymbolRelationGraph {
        val graph = SymbolRelationGraph(
            name = "Symbol Relation Graph"
        )

        // 添加节点
        for (element in elements) {
            val node = createSymbolNode(element)
            graph.addNode(node)
        }

        // 添加边
        for (relation in relations) {
            // 根据配置过滤关系类型
            if (!shouldIncludeRelation(relation.type)) {
                continue
            }

            val edge = createSymbolRelation(relation)
            graph.addEdge(edge)
        }

        return graph
    }

    /**
     * 创建符号节点
     *
     * @param element 代码元素
     * @return 符号节点
     */
    private fun createSymbolNode(element: CodeElement): SymbolNode {
        return SymbolNode.fromCodeElement(element)
    }

    /**
     * 创建符号关系
     *
     * @param relation 代码关系
     * @return 符号关系
     */
    private fun createSymbolRelation(relation: CodeRelation): SymbolRelation {
        val symbolRelationType = mapRelationType(relation.type)
        return SymbolRelation(
            id = relation.id,
            sourceId = relation.sourceId,
            targetId = relation.targetId,
            type = symbolRelationType,
            metadata = relation.metadata.toMutableMap()
        )
    }

    /**
     * 映射关系类型
     *
     * @param type 代码关系类型
     * @return 符号关系类型
     */
    private fun mapRelationType(type: RelationType): SymbolRelationType {
        return when (type) {
            RelationType.INHERITANCE -> SymbolRelationType.EXTENDS
            RelationType.IMPLEMENTATION -> SymbolRelationType.IMPLEMENTS
            RelationType.USAGE -> SymbolRelationType.USES
            RelationType.DEPENDENCY -> SymbolRelationType.DEPENDS_ON
            RelationType.OVERRIDE -> SymbolRelationType.OVERRIDES
            RelationType.REFERENCE -> SymbolRelationType.REFERENCES
            RelationType.IMPORT -> SymbolRelationType.IMPORTS
        }
    }

    /**
     * 检查是否应该包含关系
     *
     * @param type 关系类型
     * @return 是否应该包含
     */
    private fun shouldIncludeRelation(type: RelationType): Boolean {
        return when (type) {
            RelationType.INHERITANCE -> config.includeInheritance
            RelationType.IMPLEMENTATION -> config.includeImplementation
            RelationType.USAGE -> config.includeUsage
            RelationType.DEPENDENCY -> config.includeDependency
            RelationType.OVERRIDE -> config.includeOverride
            RelationType.REFERENCE -> config.includeReference
            RelationType.IMPORT -> config.includeImport
        }
    }

    /**
     * 获取关系类型集合
     *
     * @return 关系类型集合
     */
    private fun getRelationTypes(): Set<RelationType> {
        val types = mutableSetOf<RelationType>()
        if (config.includeInheritance) types.add(RelationType.INHERITANCE)
        if (config.includeImplementation) types.add(RelationType.IMPLEMENTATION)
        if (config.includeUsage) types.add(RelationType.USAGE)
        if (config.includeDependency) types.add(RelationType.DEPENDENCY)
        if (config.includeOverride) types.add(RelationType.OVERRIDE)
        if (config.includeReference) types.add(RelationType.REFERENCE)
        if (config.includeImport) types.add(RelationType.IMPORT)
        return types
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        graphCache.clear()
        logger.info { "符号关系图构建器缓存已清除" }
    }
}
