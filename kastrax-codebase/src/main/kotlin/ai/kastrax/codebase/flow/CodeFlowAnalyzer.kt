package ai.kastrax.codebase.flow

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 代码流分析器配置
 *
 * @property enableControlFlowAnalysis 是否启用控制流分析
 * @property enableDataFlowAnalysis 是否启用数据流分析
 * @property maxMethodSize 最大方法大小（行数）
 * @property maxFlowDepth 最大流程深度
 */
data class CodeFlowAnalyzerConfig(
    val enableControlFlowAnalysis: Boolean = true,
    val enableDataFlowAnalysis: Boolean = true,
    val maxMethodSize: Int = 1000,
    val maxFlowDepth: Int = 10
)

/**
 * 流节点类型
 */
enum class FlowNodeType {
    ENTRY,          // 入口节点
    EXIT,           // 出口节点
    STATEMENT,      // 语句节点
    CONDITION,      // 条件节点
    LOOP_START,     // 循环开始节点
    LOOP_END,       // 循环结束节点
    TRY_START,      // try 开始节点
    CATCH_START,    // catch 开始节点
    FINALLY_START,  // finally 开始节点
    EXCEPTION_EXIT, // 异常出口节点
    METHOD_CALL,    // 方法调用节点
    RETURN          // 返回节点
}

/**
 * 流边类型
 */
enum class FlowEdgeType {
    NORMAL,         // 普通边
    TRUE_BRANCH,    // 条件为真的分支
    FALSE_BRANCH,   // 条件为假的分支
    EXCEPTION,      // 异常边
    LOOP_BACK       // 循环回边
}

/**
 * 流节点
 *
 * @property id 节点ID
 * @property type 节点类型
 * @property element 代码元素
 * @property label 节点标签
 * @property metadata 元数据
 */
data class FlowNode(
    val id: String = UUID.randomUUID().toString(),
    val type: FlowNodeType,
    val element: CodeElement? = null,
    val label: String = "",
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * 流边
 *
 * @property id 边ID
 * @property source 源节点ID
 * @property target 目标节点ID
 * @property type 边类型
 * @property label 边标签
 * @property metadata 元数据
 */
data class FlowEdge(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val target: String,
    val type: FlowEdgeType = FlowEdgeType.NORMAL,
    val label: String = "",
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * 控制流图
 *
 * @property id 图ID
 * @property name 图名称
 * @property element 代码元素
 * @property nodes 节点列表
 * @property edges 边列表
 * @property entryNode 入口节点
 * @property exitNodes 出口节点列表
 * @property metadata 元数据
 */
data class ControlFlowGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val element: CodeElement,
    val nodes: MutableList<FlowNode> = mutableListOf(),
    val edges: MutableList<FlowEdge> = mutableListOf(),
    var entryNode: FlowNode? = null,
    val exitNodes: MutableList<FlowNode> = mutableListOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 添加节点
     *
     * @param node 节点
     * @return 添加的节点
     */
    fun addNode(node: FlowNode): FlowNode {
        nodes.add(node)
        return node
    }

    /**
     * 添加边
     *
     * @param edge 边
     * @return 添加的边
     */
    fun addEdge(edge: FlowEdge): FlowEdge {
        edges.add(edge)
        return edge
    }

    /**
     * 添加边
     *
     * @param source 源节点
     * @param target 目标节点
     * @param type 边类型
     * @param label 边标签
     * @return 添加的边
     */
    fun addEdge(
        source: FlowNode,
        target: FlowNode,
        type: FlowEdgeType = FlowEdgeType.NORMAL,
        label: String = ""
    ): FlowEdge {
        val edge = FlowEdge(
            source = source.id,
            target = target.id,
            type = type,
            label = label
        )
        return addEdge(edge)
    }

    /**
     * 获取节点
     *
     * @param id 节点ID
     * @return 节点，如果不存在则返回null
     */
    fun getNode(id: String): FlowNode? {
        return nodes.find { it.id == id }
    }

    /**
     * 获取边
     *
     * @param id 边ID
     * @return 边，如果不存在则返回null
     */
    fun getEdge(id: String): FlowEdge? {
        return edges.find { it.id == id }
    }

    /**
     * 获取出边
     *
     * @param nodeId 节点ID
     * @return 出边列表
     */
    fun getOutEdges(nodeId: String): List<FlowEdge> {
        return edges.filter { it.source == nodeId }
    }

    /**
     * 获取入边
     *
     * @param nodeId 节点ID
     * @return 入边列表
     */
    fun getInEdges(nodeId: String): List<FlowEdge> {
        return edges.filter { it.target == nodeId }
    }

    /**
     * 获取后继节点
     *
     * @param nodeId 节点ID
     * @return 后继节点列表
     */
    fun getSuccessors(nodeId: String): List<FlowNode> {
        return getOutEdges(nodeId).mapNotNull { getNode(it.target) }
    }

    /**
     * 获取前驱节点
     *
     * @param nodeId 节点ID
     * @return 前驱节点列表
     */
    fun getPredecessors(nodeId: String): List<FlowNode> {
        return getInEdges(nodeId).mapNotNull { getNode(it.source) }
    }
}

/**
 * 数据流图
 *
 * @property id 图ID
 * @property name 图名称
 * @property element 代码元素
 * @property nodes 节点列表
 * @property edges 边列表
 * @property metadata 元数据
 */
data class DataFlowGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val element: CodeElement,
    val nodes: MutableList<FlowNode> = mutableListOf(),
    val edges: MutableList<FlowEdge> = mutableListOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 添加节点
     *
     * @param node 节点
     * @return 添加的节点
     */
    fun addNode(node: FlowNode): FlowNode {
        nodes.add(node)
        return node
    }

    /**
     * 添加边
     *
     * @param edge 边
     * @return 添加的边
     */
    fun addEdge(edge: FlowEdge): FlowEdge {
        edges.add(edge)
        return edge
    }

    /**
     * 添加边
     *
     * @param source 源节点
     * @param target 目标节点
     * @param type 边类型
     * @param label 边标签
     * @return 添加的边
     */
    fun addEdge(
        source: FlowNode,
        target: FlowNode,
        type: FlowEdgeType = FlowEdgeType.NORMAL,
        label: String = ""
    ): FlowEdge {
        val edge = FlowEdge(
            source = source.id,
            target = target.id,
            type = type,
            label = label
        )
        return addEdge(edge)
    }

    /**
     * 获取节点
     *
     * @param id 节点ID
     * @return 节点，如果不存在则返回null
     */
    fun getNode(id: String): FlowNode? {
        return nodes.find { it.id == id }
    }

    /**
     * 获取边
     *
     * @param id 边ID
     * @return 边，如果不存在则返回null
     */
    fun getEdge(id: String): FlowEdge? {
        return edges.find { it.id == id }
    }
}

/**
 * 代码流分析器
 *
 * 分析代码的控制流和数据流
 *
 * @property config 配置
 */
class CodeFlowAnalyzer(
    private val config: CodeFlowAnalyzerConfig = CodeFlowAnalyzerConfig()
) {
    // 控制流图缓存
    private val controlFlowGraphCache = ConcurrentHashMap<String, ControlFlowGraph>()

    // 数据流图缓存
    private val dataFlowGraphCache = ConcurrentHashMap<String, DataFlowGraph>()

    /**
     * 分析控制流
     *
     * @param element 代码元素
     * @return 控制流图，如果无法分析则返回null
     */
    suspend fun analyzeControlFlow(element: CodeElement): ControlFlowGraph? = withContext(Dispatchers.Default) {
        try {
            // 检查缓存
            val cachedGraph = controlFlowGraphCache[element.id]
            if (cachedGraph != null) {
                return@withContext cachedGraph
            }

            // 只分析方法和构造函数
            if (element.type != CodeElementType.METHOD && element.type != CodeElementType.CONSTRUCTOR) {
                logger.warn { "只能分析方法和构造函数的控制流: ${element.id}, ${element.type}" }
                return@withContext null
            }

            // 创建控制流图
            val graph = ControlFlowGraph(
                name = element.name,
                element = element
            )

            // 创建入口节点
            val entryNode = FlowNode(
                type = FlowNodeType.ENTRY,
                label = "ENTRY"
            )
            graph.addNode(entryNode)
            graph.entryNode = entryNode

            // 创建出口节点
            val exitNode = FlowNode(
                type = FlowNodeType.EXIT,
                label = "EXIT"
            )
            graph.addNode(exitNode)
            graph.exitNodes.add(exitNode)

            // 创建异常出口节点
            val exceptionExitNode = FlowNode(
                type = FlowNodeType.EXCEPTION_EXIT,
                label = "EXCEPTION_EXIT"
            )
            graph.addNode(exceptionExitNode)
            graph.exitNodes.add(exceptionExitNode)

            // TODO: 实现控制流分析
            // 这里需要根据代码元素的具体内容进行分析
            // 由于我们没有完整的代码解析能力，这里只创建一个简单的控制流图

            // 添加一个语句节点
            val statementNode = FlowNode(
                type = FlowNodeType.STATEMENT,
                label = "STATEMENT",
                element = element
            )
            graph.addNode(statementNode)

            // 连接入口节点和语句节点
            graph.addEdge(entryNode, statementNode)

            // 连接语句节点和出口节点
            graph.addEdge(statementNode, exitNode)

            // 缓存控制流图
            controlFlowGraphCache[element.id] = graph

            return@withContext graph
        } catch (e: Exception) {
            logger.error(e) { "分析控制流失败: ${element.id}, ${e.message}" }
            return@withContext null
        }
    }

    /**
     * 分析数据流
     *
     * @param element 代码元素
     * @return 数据流图，如果无法分析则返回null
     */
    suspend fun analyzeDataFlow(element: CodeElement): DataFlowGraph? = withContext(Dispatchers.Default) {
        try {
            // 检查缓存
            val cachedGraph = dataFlowGraphCache[element.id]
            if (cachedGraph != null) {
                return@withContext cachedGraph
            }

            // 只分析方法和构造函数
            if (element.type != CodeElementType.METHOD && element.type != CodeElementType.CONSTRUCTOR) {
                logger.warn { "只能分析方法和构造函数的数据流: ${element.id}, ${element.type}" }
                return@withContext null
            }

            // 创建数据流图
            val graph = DataFlowGraph(
                name = element.name,
                element = element
            )

            // TODO: 实现数据流分析
            // 这里需要根据代码元素的具体内容进行分析
            // 由于我们没有完整的代码解析能力，这里只创建一个简单的数据流图

            // 添加一个节点
            val node = FlowNode(
                type = FlowNodeType.STATEMENT,
                label = "STATEMENT",
                element = element
            )
            graph.addNode(node)

            // 缓存数据流图
            dataFlowGraphCache[element.id] = graph

            return@withContext graph
        } catch (e: Exception) {
            logger.error(e) { "分析数据流失败: ${element.id}, ${e.message}" }
            return@withContext null
        }
    }

    /**
     * 获取控制流图
     *
     * @param elementId 元素ID
     * @return 控制流图，如果不存在则返回null
     */
    fun getControlFlowGraph(elementId: String): ControlFlowGraph? {
        return controlFlowGraphCache[elementId]
    }

    /**
     * 获取数据流图
     *
     * @param elementId 元素ID
     * @return 数据流图，如果不存在则返回null
     */
    fun getDataFlowGraph(elementId: String): DataFlowGraph? {
        return dataFlowGraphCache[elementId]
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        controlFlowGraphCache.clear()
        dataFlowGraphCache.clear()
    }
}
