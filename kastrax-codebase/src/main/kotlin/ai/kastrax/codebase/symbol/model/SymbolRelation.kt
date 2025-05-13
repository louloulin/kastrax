package ai.kastrax.codebase.symbol.model

import java.util.UUID

// 符号关系类型已移至 SymbolRelationType.kt

/**
 * 符号关系
 *
 * 代表两个符号之间的关系
 *
 * @property id 唯一标识符
 * @property sourceId 源符号 ID
 * @property targetId 目标符号 ID
 * @property type 关系类型
 * @property metadata 元数据
 */
data class SymbolRelation(
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val type: SymbolRelationType,
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 获取关系的简短描述
     *
     * @return 简短描述
     */
    fun getShortDescription(): String {
        return "${type.name.lowercase()} ($sourceId -> $targetId)"
    }

    /**
     * 获取关系的详细描述
     *
     * @param sourceNode 源符号节点
     * @param targetNode 目标符号节点
     * @return 详细描述
     */
    fun getDetailedDescription(sourceNode: SymbolNode?, targetNode: SymbolNode?): String {
        val sb = StringBuilder()

        sb.append("Relation: ${type.name.lowercase()}\n")

        if (sourceNode != null) {
            sb.append("Source: ${sourceNode.qualifiedName} (${sourceNode.type.name.lowercase()})\n")
        } else {
            sb.append("Source ID: $sourceId\n")
        }

        if (targetNode != null) {
            sb.append("Target: ${targetNode.qualifiedName} (${targetNode.type.name.lowercase()})\n")
        } else {
            sb.append("Target ID: $targetId\n")
        }

        if (metadata.isNotEmpty()) {
            sb.append("Metadata: $metadata\n")
        }

        return sb.toString()
    }
}
