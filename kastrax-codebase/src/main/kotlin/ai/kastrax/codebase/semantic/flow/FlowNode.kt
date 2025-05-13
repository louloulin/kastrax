package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.model.CodeElement
import java.util.UUID

/**
 * 流节点
 *
 * @property id 节点ID
 * @property type 节点类型
 * @property label 节点标签
 * @property element 关联的代码元素
 * @property metadata 元数据
 */
data class FlowNode(
    val id: String = UUID.randomUUID().toString(),
    val type: FlowNodeType,
    val label: String = "",
    val element: CodeElement? = null,
    val metadata: MutableMap<String, String> = mutableMapOf()
)
