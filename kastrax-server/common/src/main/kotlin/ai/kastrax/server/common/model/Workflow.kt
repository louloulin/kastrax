package ai.kastrax.server.common.model

import java.time.Instant

/**
 * 工作流模型
 */
data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val nodes: List<Node>,
    val edges: List<Edge>,
    val metadata: Map<String, Any>,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * 节点模型
 */
data class Node(
    val id: String,
    val type: String,
    val label: String,
    val position: Position,
    val data: Map<String, Any>,
    val style: Map<String, Any>
)

/**
 * 边模型
 */
data class Edge(
    val id: String,
    val source: String,
    val target: String,
    val label: String,
    val data: Map<String, Any>,
    val style: Map<String, Any>
)

/**
 * 位置模型
 */
data class Position(
    val x: Double,
    val y: Double
)
