package ai.kastrax.rag.graph

import ai.kastrax.store.document.Document
import ai.kastrax.rag.embedding.cosineSimilarity
import ai.kastrax.rag.retrieval.SearchResult
import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * 图节点
 *
 * @property id 节点ID
 * @property content 节点内容
 * @property embedding 节点嵌入向量
 * @property metadata 节点元数据
 */
@Serializable
data class GraphNode(
    val id: String,
    val content: String,
    val embedding: FloatArray? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphNode

        if (id != other.id) return false
        if (content != other.content) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * 带有排名分数的图节点
 *
 * @property id 节点ID
 * @property content 节点内容
 * @property metadata 节点元数据
 * @property score 排名分数
 */
@Serializable
data class RankedNode(
    val id: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val score: Double
) {
    companion object {
        fun fromGraphNode(node: GraphNode, score: Double): RankedNode {
            return RankedNode(
                id = node.id,
                content = node.content,
                metadata = node.metadata,
                score = score
            )
        }
    }
}

/**
 * 图边类型
 */
enum class EdgeType {
    SEMANTIC,    // 语义相似性边
    SEQUENTIAL,  // 顺序关系边
    HIERARCHICAL, // 层次关系边
    CITATION     // 引用关系边
}

/**
 * 图边
 *
 * @property source 源节点ID
 * @property target 目标节点ID
 * @property weight 边权重
 * @property type 边类型
 */
@Serializable
data class GraphEdge(
    val source: String,
    val target: String,
    val weight: Double,
    val type: String
)

/**
 * GraphRAG 配置
 *
 * @property dimension 嵌入向量维度
 * @property threshold 相似度阈值
 * @property bidirectional 是否创建双向边
 */
@Serializable
data class GraphRAGConfig(
    val dimension: Int = 1536,
    val threshold: Double = 0.7,
    val bidirectional: Boolean = true
)

/**
 * GraphRAG 查询选项
 *
 * @property topK 返回的最大结果数
 * @property randomWalkSteps 随机游走步数
 * @property restartProb 重启概率
 */
@Serializable
data class GraphRAGQueryOptions(
    val topK: Int = 10,
    val randomWalkSteps: Int = 100,
    val restartProb: Double = 0.15
)

/**
 * GraphRAG 实现，基于图的检索增强生成
 *
 * GraphRAG 通过构建文档片段之间的语义图，并使用图算法进行检索，
 * 可以发现传统向量检索无法找到的隐含关系和上下文连接。
 *
 * @property config GraphRAG 配置
 */
class GraphRAG(
    private val config: GraphRAGConfig = GraphRAGConfig()
) {
    private val nodes = mutableMapOf<String, GraphNode>()
    private val edges = mutableListOf<GraphEdge>()

    /**
     * 添加节点
     *
     * @param node 图节点
     */
    fun addNode(node: GraphNode) {
        if (node.embedding == null) {
            throw IllegalArgumentException("节点必须包含嵌入向量")
        }
        if (node.embedding.size != config.dimension) {
            throw IllegalArgumentException("嵌入向量维度必须为 ${config.dimension}")
        }
        nodes[node.id] = node
    }

    /**
     * 添加边
     *
     * @param edge 图边
     */
    fun addEdge(edge: GraphEdge) {
        if (!nodes.containsKey(edge.source) || !nodes.containsKey(edge.target)) {
            throw IllegalArgumentException("源节点和目标节点必须存在")
        }
        edges.add(edge)

        // 如果配置为双向边，添加反向边
        if (config.bidirectional) {
            edges.add(
                GraphEdge(
                    source = edge.target,
                    target = edge.source,
                    weight = edge.weight,
                    type = edge.type
                )
            )
        }
    }

    /**
     * 获取所有节点
     *
     * @return 节点列表
     */
    fun getNodes(): List<GraphNode> {
        return nodes.values.toList()
    }

    /**
     * 获取所有边
     *
     * @return 边列表
     */
    fun getEdges(): List<GraphEdge> {
        return edges.toList()
    }

    /**
     * 获取指定类型的边
     *
     * @param type 边类型
     * @return 边列表
     */
    fun getEdgesByType(type: String): List<GraphEdge> {
        return edges.filter { it.type == type }
    }

    /**
     * 清空图
     */
    fun clear() {
        nodes.clear()
        edges.clear()
    }

    /**
     * 从文档和嵌入向量创建图
     *
     * @param documents 文档列表
     * @param embeddings 嵌入向量列表
     */
    fun createGraph(documents: List<Document>, embeddings: List<FloatArray>) {
        if (documents.isEmpty() || embeddings.isEmpty()) {
            throw IllegalArgumentException("文档和嵌入向量列表不能为空")
        }
        if (documents.size != embeddings.size) {
            throw IllegalArgumentException("文档和嵌入向量数量必须相同")
        }

        // 创建节点
        val nodeIds = mutableListOf<String>()
        for (index in documents.indices) {
            val document = documents[index]
            val nodeId = document.id
            nodeIds.add(nodeId)
            val node = GraphNode(
                id = nodeId,
                content = document.content,
                embedding = embeddings[index],
                metadata = document.metadata.mapValues { it.value.toString() }
            )
            addNode(node)
        }

        // 基于余弦相似度创建边
        for (i in documents.indices) {
            val firstEmbedding = embeddings[i]
            for (j in i + 1 until documents.size) {
                val secondEmbedding = embeddings[j]
                val similarity = firstEmbedding.cosineSimilarity(secondEmbedding)

                // 仅当相似度高于阈值时创建边
                if (similarity > config.threshold) {
                    val sourceId = nodeIds[i]
                    val targetId = nodeIds[j]

                    addEdge(
                        GraphEdge(
                            source = sourceId,
                            target = targetId,
                            weight = similarity,
                            type = EdgeType.SEMANTIC.name
                        )
                    )
                }
            }
        }
    }

    /**
     * 获取节点的邻居
     *
     * @param nodeId 节点ID
     * @return 邻居节点ID和权重的映射
     */
    fun getNeighbors(nodeId: String): Map<String, Double> {
        if (!nodes.containsKey(nodeId)) {
            throw IllegalArgumentException("节点不存在: $nodeId")
        }

        val neighbors = mutableMapOf<String, Double>()
        edges.filter { it.source == nodeId }.forEach { edge ->
            neighbors[edge.target] = edge.weight
        }
        return neighbors
    }

    /**
     * 选择加权随机邻居
     *
     * @param neighbors 邻居节点ID和权重的映射
     * @return 选择的邻居节点ID
     */
    private fun selectWeightedNeighbor(neighbors: Map<String, Double>): String {
        if (neighbors.isEmpty()) {
            throw IllegalArgumentException("邻居列表不能为空")
        }

        // 计算总权重
        val totalWeight = neighbors.values.sum()

        // 生成随机值
        val random = Math.random() * totalWeight

        // 选择邻居
        var cumulativeWeight = 0.0
        for ((nodeId, weight) in neighbors) {
            cumulativeWeight += weight
            if (random <= cumulativeWeight) {
                return nodeId
            }
        }

        // 如果由于浮点精度问题未选择任何邻居，返回最后一个
        return neighbors.keys.last()
    }

    /**
     * 带重启的随机游走算法
     *
     * @param startNodeId 起始节点ID
     * @param steps 步数
     * @param restartProb 重启概率
     * @return 节点访问次数映射
     */
    private fun randomWalkWithRestart(
        startNodeId: String,
        steps: Int,
        restartProb: Double
    ): Map<String, Double> {
        val visits = mutableMapOf<String, Int>()
        var currentNodeId = startNodeId

        for (step in 0 until steps) {
            // 记录访问
            visits[currentNodeId] = (visits[currentNodeId] ?: 0) + 1

            // 决定是否重启
            if (Math.random() < restartProb) {
                currentNodeId = startNodeId
                continue
            }

            // 获取邻居
            val neighbors = getNeighbors(currentNodeId)
            if (neighbors.isEmpty()) {
                currentNodeId = startNodeId
                continue
            }

            // 选择加权随机邻居作为当前节点
            currentNodeId = selectWeightedNeighbor(neighbors)
        }

        // 将访问次数转换为分数
        val totalVisits = visits.values.sum().toDouble()
        return visits.mapValues { (_, count) -> count.toDouble() / totalVisits }
    }

    /**
     * 查询图，使用混合方法检索相关节点
     *
     * @param query 查询嵌入向量
     * @param options 查询选项
     * @return 排序后的节点列表
     */
    fun query(
        query: FloatArray,
        options: GraphRAGQueryOptions = GraphRAGQueryOptions()
    ): List<RankedNode> {
        if (query.size != config.dimension) {
            throw IllegalArgumentException("查询嵌入向量维度必须为 ${config.dimension}")
        }
        if (options.topK < 1) {
            throw IllegalArgumentException("topK 必须大于 0")
        }
        if (options.randomWalkSteps < 1) {
            throw IllegalArgumentException("随机游走步数必须大于 0")
        }
        if (options.restartProb <= 0 || options.restartProb >= 1) {
            throw IllegalArgumentException("重启概率必须在 0 和 1 之间")
        }

        // 计算所有节点与查询的相似度
        val similarities = nodes.values.map { node ->
            node to query.cosineSimilarity(node.embedding!!)
        }

        // 按相似度排序
        val topNodes = similarities.sortedByDescending { it.second }.take(options.topK)

        // 使用带重启的随机游走重新排序节点
        val rerankedNodes = mutableMapOf<String, Pair<GraphNode, Double>>()

        // 对每个顶部节点执行随机游走
        for ((node, similarity) in topNodes) {
            val walkScores = randomWalkWithRestart(
                node.id,
                options.randomWalkSteps,
                options.restartProb
            )

            // 结合密集检索分数和图分数
            for ((nodeId, walkScore) in walkScores) {
                val node = nodes[nodeId]!!
                val existingScore = rerankedNodes[nodeId]?.second ?: 0.0
                val combinedScore = existingScore + similarity * walkScore
                rerankedNodes[nodeId] = node to combinedScore
            }
        }

        // 按最终分数排序并返回前 K 个节点
        return rerankedNodes.values
            .sortedByDescending { it.second }
            .take(options.topK)
            .map { (node, score) ->
                RankedNode.fromGraphNode(node, score)
            }
    }

    /**
     * 将排序节点转换为搜索结果
     *
     * @param rankedNodes 排序节点列表
     * @return 搜索结果列表
     */
    fun toSearchResults(rankedNodes: List<RankedNode>): List<SearchResult> {
        return rankedNodes.map { rankedNode ->
            SearchResult(
                document = Document(
                    id = rankedNode.id,
                    content = rankedNode.content,
                    metadata = rankedNode.metadata.mapValues { it.value }
                ),
                score = rankedNode.score
            )
        }
    }
}
