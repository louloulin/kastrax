package ai.kastrax.rag.graph

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

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
 * @property embedding 节点嵌入向量
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
     * 更新节点内容
     *
     * @param id 节点ID
     * @param newContent 新内容
     */
    fun updateNodeContent(id: String, newContent: String) {
        val node = nodes[id] ?: throw IllegalArgumentException("节点 $id 不存在")
        nodes[id] = node.copy(content = newContent)
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
        documents.forEachIndexed { index, document ->
            val nodeId = document.metadata["id"]?.toString() ?: UUID.randomUUID().toString()
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
                val similarity = cosineSimilarity(firstEmbedding, secondEmbedding)

                // 仅当相似度高于阈值时创建边
                if (similarity > config.threshold) {
                    val sourceId = documents[i].metadata["id"]?.toString() ?: i.toString()
                    val targetId = documents[j].metadata["id"]?.toString() ?: j.toString()

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
     * 计算余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size) {
            throw IllegalArgumentException("向量维度必须相同: vec1(${vec1.size}) != vec2(${vec2.size})")
        }

        var dotProduct = 0.0
        var normVec1 = 0.0
        var normVec2 = 0.0

        for (i in vec1.indices) {
            val a = vec1[i].toDouble()
            val b = vec2[i].toDouble()

            dotProduct += a * b
            normVec1 += a * a
            normVec2 += b * b
        }

        val magnitudeProduct = sqrt(normVec1 * normVec2)

        if (magnitudeProduct == 0.0) {
            return 0.0
        }

        val similarity = dotProduct / magnitudeProduct
        return max(-1.0, min(1.0, similarity))
    }

    /**
     * 获取节点的邻居
     *
     * @param nodeId 节点ID
     * @param edgeType 边类型（可选）
     * @return 邻居节点ID和权重列表
     */
    private fun getNeighbors(nodeId: String, edgeType: String? = null): List<Pair<String, Double>> {
        return edges
            .filter { it.source == nodeId && (edgeType == null || it.type == edgeType) }
            .map { it.target to it.weight }
    }

    /**
     * 加权随机选择邻居
     *
     * @param neighbors 邻居节点ID和权重列表
     * @return 选中的邻居节点ID
     */
    private fun selectWeightedNeighbor(neighbors: List<Pair<String, Double>>): String {
        // 计算总权重
        val totalWeight = neighbors.sumOf { it.second }

        // 在总权重范围内随机选择一个点
        var remainingWeight = Math.random() * totalWeight

        // 从随机值中减去每个权重，直到低于0
        // 权重越高，越有可能被选中
        for ((id, weight) in neighbors) {
            remainingWeight -= weight
            if (remainingWeight <= 0) {
                return id
            }
        }

        return neighbors.last().first
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

        // 归一化访问次数
        val totalVisits = visits.values.sum().toDouble()
        return visits.mapValues { it.value / totalVisits }
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
            node to cosineSimilarity(query, node.embedding!!)
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
                rerankedNodes[nodeId] = node to (existingScore + similarity * walkScore)
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
     * 将查询结果转换为 SearchResult 列表
     *
     * @param rankedNodes 排序后的节点列表
     * @return SearchResult 列表
     */
    fun toSearchResults(rankedNodes: List<RankedNode>): List<SearchResult> {
        return rankedNodes.map { node ->
            SearchResult(
                document = ai.kastrax.rag.vectorstore.RagDocument(
                    id = node.id,
                    content = node.content,
                    metadata = node.metadata
                ),
                score = node.score
            )
        }
    }
}
