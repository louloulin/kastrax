package ai.kastrax.codebase.semantic.flow

import java.util.UUID

/**
 * 流边
 *
 * @property id 边ID
 * @property type 边类型
 * @property source 源节点ID
 * @property target 目标节点ID
 * @property label 边标签
 * @property metadata 元数据
 */
data class FlowEdge(
    val id: String = UUID.randomUUID().toString(),
    val type: FlowEdgeType,
    val source: String,
    val target: String,
    val label: String = "",
    val metadata: MutableMap<String, String> = mutableMapOf()
)
