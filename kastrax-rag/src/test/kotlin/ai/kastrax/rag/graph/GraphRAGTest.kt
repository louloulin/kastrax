package ai.kastrax.rag.graph

import ai.kastrax.rag.document.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class GraphRAGTest {
    
    private lateinit var graphRAG: GraphRAG
    
    @BeforeEach
    fun setup() {
        graphRAG = GraphRAG(
            GraphRAGConfig(
                dimension = 3,
                threshold = 0.7,
                bidirectional = true
            )
        )
    }
    
    @Test
    fun `test add node`() {
        // 创建节点
        val node = GraphNode(
            id = "1",
            content = "测试内容",
            embedding = floatArrayOf(1.0f, 0.0f, 0.0f)
        )
        
        // 添加节点
        graphRAG.addNode(node)
        
        // 验证节点已添加
        val nodes = graphRAG.getNodes()
        assertEquals(1, nodes.size)
        assertEquals("1", nodes[0].id)
        assertEquals("测试内容", nodes[0].content)
    }
    
    @Test
    fun `test add edge`() {
        // 创建节点
        val node1 = GraphNode(
            id = "1",
            content = "测试内容1",
            embedding = floatArrayOf(1.0f, 0.0f, 0.0f)
        )
        val node2 = GraphNode(
            id = "2",
            content = "测试内容2",
            embedding = floatArrayOf(0.0f, 1.0f, 0.0f)
        )
        
        // 添加节点
        graphRAG.addNode(node1)
        graphRAG.addNode(node2)
        
        // 添加边
        val edge = GraphEdge(
            source = "1",
            target = "2",
            weight = 0.8,
            type = EdgeType.SEMANTIC.name
        )
        graphRAG.addEdge(edge)
        
        // 验证边已添加
        val edges = graphRAG.getEdges()
        assertEquals(2, edges.size) // 双向边
        assertEquals("1", edges[0].source)
        assertEquals("2", edges[0].target)
        assertEquals(0.8, edges[0].weight)
        assertEquals(EdgeType.SEMANTIC.name, edges[0].type)
    }
    
    @Test
    fun `test create graph`() {
        // 创建文档
        val documents = listOf(
            Document(
                content = "苹果是一种常见的水果",
                metadata = mapOf("source" to "水果百科")
            ),
            Document(
                content = "香蕉是一种热带水果",
                metadata = mapOf("source" to "水果百科")
            ),
            Document(
                content = "电脑是一种电子设备",
                metadata = mapOf("source" to "电子百科")
            )
        )
        
        // 创建嵌入向量
        val embeddings = listOf(
            floatArrayOf(1.0f, 0.0f, 0.0f),
            floatArrayOf(0.8f, 0.2f, 0.0f),
            floatArrayOf(0.0f, 0.0f, 1.0f)
        )
        
        // 创建图
        graphRAG.createGraph(documents, embeddings)
        
        // 验证节点已添加
        val nodes = graphRAG.getNodes()
        assertEquals(3, nodes.size)
        
        // 验证边已添加（苹果和香蕉之间应该有边，因为它们的相似度高于阈值）
        val edges = graphRAG.getEdges()
        assertTrue(edges.isNotEmpty())
        
        // 验证边类型
        val semanticEdges = graphRAG.getEdgesByType(EdgeType.SEMANTIC.name)
        assertEquals(edges.size, semanticEdges.size)
    }
    
    @Test
    fun `test query`() {
        // 创建文档
        val documents = listOf(
            Document(
                content = "苹果是一种常见的水果",
                metadata = mapOf("source" to "水果百科")
            ),
            Document(
                content = "香蕉是一种热带水果",
                metadata = mapOf("source" to "水果百科")
            ),
            Document(
                content = "电脑是一种电子设备",
                metadata = mapOf("source" to "电子百科")
            )
        )
        
        // 创建嵌入向量
        val embeddings = listOf(
            floatArrayOf(1.0f, 0.0f, 0.0f),
            floatArrayOf(0.8f, 0.2f, 0.0f),
            floatArrayOf(0.0f, 0.0f, 1.0f)
        )
        
        // 创建图
        graphRAG.createGraph(documents, embeddings)
        
        // 查询
        val queryEmbedding = floatArrayOf(0.9f, 0.1f, 0.0f) // 更接近水果相关的文档
        val results = graphRAG.query(
            queryEmbedding,
            GraphRAGQueryOptions(topK = 2)
        )
        
        // 验证结果
        assertEquals(2, results.size)
        assertTrue(results[0].content.contains("水果"))
        assertTrue(results[1].content.contains("水果"))
    }
    
    @Test
    fun `test to search results`() {
        // 创建排序节点
        val rankedNodes = listOf(
            RankedNode(
                id = "1",
                content = "苹果是一种常见的水果",
                metadata = mapOf("source" to "水果百科"),
                score = 0.9
            ),
            RankedNode(
                id = "2",
                content = "香蕉是一种热带水果",
                metadata = mapOf("source" to "水果百科"),
                score = 0.8
            )
        )
        
        // 转换为搜索结果
        val searchResults = graphRAG.toSearchResults(rankedNodes)
        
        // 验证结果
        assertEquals(2, searchResults.size)
        assertEquals("苹果是一种常见的水果", searchResults[0].document.content)
        assertEquals(0.9, searchResults[0].score)
        assertEquals("水果百科", searchResults[0].document.metadata["source"])
    }
}
