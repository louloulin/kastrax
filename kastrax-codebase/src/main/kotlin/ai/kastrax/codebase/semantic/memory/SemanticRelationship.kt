package ai.kastrax.codebase.semantic.memory

import java.time.Instant
import java.util.UUID

/**
 * 语义关系类型
 */
enum class SemanticRelationshipType {
    IMPORTS,           // 导入关系
    USES,              // 使用关系
    EXTENDS,           // 继承关系
    IMPLEMENTS,        // 实现关系
    CALLS,             // 调用关系
    REFERENCES,        // 引用关系
    SIMILAR_TO,        // 语义相似关系
    DEPENDS_ON,        // 依赖关系
    PART_OF,           // 组成关系
    RELATED_TO         // 一般相关关系
}

/**
 * 语义关系强度
 */
enum class RelationshipStrength {
    STRONG,    // 强关系
    MEDIUM,    // 中等关系
    WEAK       // 弱关系
}

/**
 * 语义关系
 *
 * 表示两个语义记忆之间的关系
 *
 * @property id 唯一标识符
 * @property sourceId 源记忆ID
 * @property targetId 目标记忆ID
 * @property type 关系类型
 * @property strength 关系强度
 * @property weight 关系权重（0.0-1.0）
 * @property bidirectional 是否双向关系
 * @property creationTime 创建时间
 * @property metadata 元数据
 */
data class SemanticRelationship(
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val type: SemanticRelationshipType,
    val strength: RelationshipStrength = RelationshipStrength.MEDIUM,
    val weight: Double = 0.5,
    val bidirectional: Boolean = false,
    val creationTime: Instant = Instant.now(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 获取关系的简短描述
     *
     * @return 简短描述
     */
    fun getShortDescription(): String {
        return "${type.name}: $sourceId -> $targetId (${strength.name}, $weight)"
    }
}
