package ai.kastrax.codebase.context

import ai.kastrax.codebase.flow.CodeFlowAnalyzer
import ai.kastrax.codebase.flow.FlowGraph
import ai.kastrax.codebase.flow.FlowType
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.relation.CodeRelationAnalyzer
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 流感知上下文构建器配置
 *
 * @property maxCacheSize 最大缓存大小
 * @property defaultMaxElements 默认最大元素数量
 * @property defaultMinScore 默认最小分数
 * @property includeRelatedElements 是否包含相关元素
 * @property maxRelatedElements 最大相关元素数量
 * @property includeControlFlow 是否包含控制流
 * @property includeDataFlow 是否包含数据流
 * @property maxFlowDepth 最大流深度
 */
data class FlowAwareContextBuilderConfig(
    val maxCacheSize: Int = 100,
    val defaultMaxElements: Int = 20,
    val defaultMinScore: Float = 0.5f,
    val includeRelatedElements: Boolean = true,
    val maxRelatedElements: Int = 10,
    val includeControlFlow: Boolean = true,
    val includeDataFlow: Boolean = true,
    val maxFlowDepth: Int = 3
)

/**
 * 流感知上下文构建器
 *
 * 用于构建包含代码流信息的上下文
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property flowAnalyzer 代码流分析器
 * @property config 配置
 * @property contextCache 上下文缓存
 * @property relationAnalyzer 代码关系分析器
 */
class FlowAwareContextBuilder(
    private val vectorStore: CodeVectorStore,
    private val embeddingService: EmbeddingService,
    private val flowAnalyzer: CodeFlowAnalyzer,
    private val config: FlowAwareContextBuilderConfig = FlowAwareContextBuilderConfig(),
    private val contextCache: ConcurrentHashMap<String, Context> = ConcurrentHashMap(),
    private val relationAnalyzer: CodeRelationAnalyzer? = null
) {
    // 基础上下文构建器
    private val baseContextBuilder = ContextBuilder(
        vectorStore = vectorStore,
        embeddingService = embeddingService,
        config = ContextBuilderConfig(
            maxCacheSize = config.maxCacheSize,
            defaultMaxElements = config.defaultMaxElements,
            defaultMinScore = config.defaultMinScore,
            includeRelatedElements = config.includeRelatedElements,
            maxRelatedElements = config.maxRelatedElements
        ),
        contextCache = contextCache,
        relationAnalyzer = relationAnalyzer
    )
    
    /**
     * 构建流感知上下文
     *
     * @param query 查询
     * @param position 位置
     * @param maxElements 最大元素数量
     * @param minScore 最小相似度分数
     * @param includeLevels 包含的级别
     * @param excludeTypes 排除的类型
     * @return 上下文
     */
    suspend fun buildContext(
        query: String,
        position: Location? = null,
        maxElements: Int = config.defaultMaxElements,
        minScore: Float = config.defaultMinScore,
        includeLevels: Set<ContextLevel> = ContextLevel.values().toSet(),
        excludeTypes: Set<CodeElementType> = emptySet()
    ): Context = withContext(Dispatchers.IO) {
        // 生成缓存键
        val cacheKey = generateCacheKey(query, position, maxElements, minScore, includeLevels, excludeTypes)
        
        // 检查缓存
        val cachedContext = contextCache[cacheKey]
        if (cachedContext != null) {
            return@withContext cachedContext
        }
        
        try {
            // 首先使用基础上下文构建器构建上下文
            val baseContext = baseContextBuilder.buildContext(
                query = query,
                position = position,
                maxElements = maxElements,
                minScore = minScore,
                includeLevels = includeLevels,
                excludeTypes = excludeTypes
            )
            
            // 如果不需要包含流信息，则直接返回基础上下文
            if (!config.includeControlFlow && !config.includeDataFlow) {
                return@withContext baseContext
            }
            
            // 增强上下文元素，添加流信息
            val enhancedElements = enhanceContextWithFlowInfo(baseContext.elements)
            
            // 创建增强的上下文
            val enhancedContext = Context(
                elements = enhancedElements,
                query = query,
                metadata = baseContext.metadata + mapOf(
                    "includeControlFlow" to config.includeControlFlow,
                    "includeDataFlow" to config.includeDataFlow,
                    "maxFlowDepth" to config.maxFlowDepth
                )
            )
            
            // 缓存上下文
            if (contextCache.size < config.maxCacheSize) {
                contextCache[cacheKey] = enhancedContext
            }
            
            return@withContext enhancedContext
        } catch (e: Exception) {
            logger.error(e) { "构建流感知上下文时出错: $query" }
            return@withContext Context(
                elements = emptyList(),
                query = query,
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }
    
    /**
     * 构建流感知文件上下文
     *
     * @param filePath 文件路径
     * @param maxElements 最大元素数量
     * @return 上下文
     */
    suspend fun buildFileContext(
        filePath: Path,
        maxElements: Int = config.defaultMaxElements
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 首先使用基础上下文构建器构建文件上下文
            val baseContext = baseContextBuilder.buildFileContext(
                filePath = filePath,
                maxElements = maxElements
            )
            
            // 如果不需要包含流信息，则直接返回基础上下文
            if (!config.includeControlFlow && !config.includeDataFlow) {
                return@withContext baseContext
            }
            
            // 增强上下文元素，添加流信息
            val enhancedElements = enhanceContextWithFlowInfo(baseContext.elements)
            
            // 创建增强的上下文
            val enhancedContext = Context(
                elements = enhancedElements,
                query = baseContext.query,
                metadata = baseContext.metadata + mapOf(
                    "includeControlFlow" to config.includeControlFlow,
                    "includeDataFlow" to config.includeDataFlow,
                    "maxFlowDepth" to config.maxFlowDepth
                )
            )
            
            return@withContext enhancedContext
        } catch (e: Exception) {
            logger.error(e) { "构建流感知文件上下文时出错: $filePath" }
            return@withContext Context(
                elements = emptyList(),
                query = "file:$filePath",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }
    
    /**
     * 构建流感知符号上下文
     *
     * @param symbolName 符号名称
     * @param maxElements 最大元素数量
     * @param minScore 最小分数
     * @return 上下文
     */
    suspend fun buildSymbolContext(
        symbolName: String,
        maxElements: Int = config.defaultMaxElements,
        minScore: Float = config.defaultMinScore
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 首先使用基础上下文构建器构建符号上下文
            val baseContext = baseContextBuilder.buildSymbolContext(
                symbolName = symbolName,
                maxElements = maxElements,
                minScore = minScore
            )
            
            // 如果不需要包含流信息，则直接返回基础上下文
            if (!config.includeControlFlow && !config.includeDataFlow) {
                return@withContext baseContext
            }
            
            // 增强上下文元素，添加流信息
            val enhancedElements = enhanceContextWithFlowInfo(baseContext.elements)
            
            // 创建增强的上下文
            val enhancedContext = Context(
                elements = enhancedElements,
                query = baseContext.query,
                metadata = baseContext.metadata + mapOf(
                    "includeControlFlow" to config.includeControlFlow,
                    "includeDataFlow" to config.includeDataFlow,
                    "maxFlowDepth" to config.maxFlowDepth
                )
            )
            
            return@withContext enhancedContext
        } catch (e: Exception) {
            logger.error(e) { "构建流感知符号上下文时出错: $symbolName" }
            return@withContext Context(
                elements = emptyList(),
                query = "symbol:$symbolName",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }
    
    /**
     * 增强上下文元素，添加流信息
     *
     * @param elements 上下文元素列表
     * @return 增强后的上下文元素列表
     */
    private suspend fun enhanceContextWithFlowInfo(elements: List<ContextElement>): List<ContextElement> {
        val enhancedElements = mutableListOf<ContextElement>()
        
        for (element in elements) {
            // 添加原始元素
            enhancedElements.add(element)
            
            // 只为方法和构造函数添加流信息
            if (element.element.type == CodeElementType.METHOD || element.element.type == CodeElementType.CONSTRUCTOR) {
                // 添加控制流信息
                if (config.includeControlFlow) {
                    val controlFlowGraph = flowAnalyzer.analyzeControlFlow(element.element)
                    if (controlFlowGraph != null) {
                        val controlFlowContent = formatControlFlowGraph(controlFlowGraph)
                        enhancedElements.add(
                            ContextElement(
                                element = element.element,
                                level = element.level,
                                relevance = element.relevance * 0.9f, // 控制流的相关性稍低
                                content = controlFlowContent,
                                metadata = mapOf("flowType" to "CONTROL_FLOW")
                            )
                        )
                    }
                }
                
                // 添加数据流信息
                if (config.includeDataFlow) {
                    val dataFlowGraph = flowAnalyzer.analyzeDataFlow(element.element)
                    if (dataFlowGraph != null) {
                        val dataFlowContent = formatDataFlowGraph(dataFlowGraph)
                        enhancedElements.add(
                            ContextElement(
                                element = element.element,
                                level = element.level,
                                relevance = element.relevance * 0.8f, // 数据流的相关性更低
                                content = dataFlowContent,
                                metadata = mapOf("flowType" to "DATA_FLOW")
                            )
                        )
                    }
                }
            }
        }
        
        return enhancedElements
    }
    
    /**
     * 格式化控制流图
     *
     * @param graph 控制流图
     * @return 格式化后的控制流文本
     */
    private fun formatControlFlowGraph(graph: ControlFlowGraph): String {
        val sb = StringBuilder()
        
        sb.append("// 控制流图: ${graph.name}\n")
        
        // 添加入口节点
        val entryNode = graph.entryNode
        if (entryNode != null) {
            sb.append("ENTRY\n")
            
            // 使用深度优先搜索遍历图
            val visited = mutableSetOf<String>()
            formatFlowNode(graph, entryNode.id, sb, visited, 1)
        }
        
        return sb.toString()
    }
    
    /**
     * 格式化流节点
     *
     * @param graph 控制流图
     * @param nodeId 节点ID
     * @param sb 字符串构建器
     * @param visited 已访问节点集合
     * @param depth 当前深度
     */
    private fun formatFlowNode(
        graph: ControlFlowGraph,
        nodeId: String,
        sb: StringBuilder,
        visited: MutableSet<String>,
        depth: Int
    ) {
        // 检查深度限制
        if (depth > config.maxFlowDepth) {
            sb.append("  ".repeat(depth)).append("...\n")
            return
        }
        
        // 检查是否已访问
        if (nodeId in visited) {
            sb.append("  ".repeat(depth)).append("(循环回到 ${nodeId})\n")
            return
        }
        
        // 标记为已访问
        visited.add(nodeId)
        
        // 获取节点
        val node = graph.getNode(nodeId) ?: return
        
        // 格式化节点
        sb.append("  ".repeat(depth)).append("${node.type}: ${node.label}\n")
        
        // 获取后继节点
        val successors = graph.getSuccessors(nodeId)
        
        // 递归处理后继节点
        successors.forEach { successor ->
            // 获取边
            val edge = graph.getOutEdges(nodeId).find { it.target == successor.id }
            
            // 添加边信息
            if (edge != null) {
                sb.append("  ".repeat(depth + 1)).append("${edge.type}")
                if (edge.label.isNotEmpty()) {
                    sb.append(" (${edge.label})")
                }
                sb.append("\n")
            }
            
            // 递归处理后继节点
            formatFlowNode(graph, successor.id, sb, visited, depth + 2)
        }
        
        // 如果是出口节点，则添加出口标记
        if (node.id in graph.exitNodes.map { it.id }) {
            sb.append("  ".repeat(depth)).append("EXIT\n")
        }
    }
    
    /**
     * 格式化数据流图
     *
     * @param graph 数据流图
     * @return 格式化后的数据流文本
     */
    private fun formatDataFlowGraph(graph: DataFlowGraph): String {
        val sb = StringBuilder()
        
        sb.append("// 数据流图: ${graph.name}\n")
        
        // 添加节点信息
        graph.nodes.forEach { node ->
            sb.append("${node.type}: ${node.label}\n")
            
            // 获取出边
            val outEdges = graph.edges.filter { it.source == node.id }
            
            // 添加边信息
            outEdges.forEach { edge ->
                val targetNode = graph.getNode(edge.target)
                if (targetNode != null) {
                    sb.append("  -> ${targetNode.label}\n")
                }
            }
            
            sb.append("\n")
        }
        
        return sb.toString()
    }
    
    /**
     * 生成缓存键
     *
     * @param query 查询
     * @param position 位置
     * @param maxElements 最大元素数量
     * @param minScore 最小分数
     * @param includeLevels 包含的级别
     * @param excludeTypes 排除的类型
     * @return 缓存键
     */
    private fun generateCacheKey(
        query: String,
        position: Location?,
        maxElements: Int,
        minScore: Float,
        includeLevels: Set<ContextLevel>,
        excludeTypes: Set<CodeElementType>
    ): String {
        val sb = StringBuilder()
        
        sb.append("flow:query:$query")
        sb.append("|position:${position?.toString() ?: "null"}")
        sb.append("|maxElements:$maxElements")
        sb.append("|minScore:$minScore")
        sb.append("|includeLevels:${includeLevels.joinToString(",") { it.name }}")
        sb.append("|excludeTypes:${excludeTypes.joinToString(",") { it.name }}")
        sb.append("|includeControlFlow:${config.includeControlFlow}")
        sb.append("|includeDataFlow:${config.includeDataFlow}")
        sb.append("|maxFlowDepth:${config.maxFlowDepth}")
        
        return sb.toString()
    }
    
    /**
     * 清空上下文缓存
     */
    fun clearContextCache() {
        contextCache.clear()
    }
}
