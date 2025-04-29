package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 关系推理引擎，负责推理实体之间的关系和依赖
 */
class RelationshipInferenceEngine {
    /**
     * 关系类型映射
     */
    private val relationshipTypes = ConcurrentHashMap<String, RelationshipType>()

    /**
     * 关系规则映射
     */
    private val relationshipRules = ConcurrentHashMap<String, List<RelationshipRule>>()

    /**
     * 关系实例映射
     */
    private val relationships = ConcurrentHashMap<String, Relationship>()

    /**
     * 注册关系类型
     */
    fun registerRelationshipType(type: RelationshipType) {
        relationshipTypes[type.id] = type
    }

    /**
     * 注销关系类型
     */
    fun unregisterRelationshipType(typeId: String) {
        relationshipTypes.remove(typeId)
        relationshipRules.remove(typeId)
    }

    /**
     * 获取关系类型
     */
    fun getRelationshipType(typeId: String): RelationshipType? {
        return relationshipTypes[typeId]
    }

    /**
     * 获取所有关系类型
     */
    fun getAllRelationshipTypes(): List<RelationshipType> {
        return relationshipTypes.values.toList()
    }

    /**
     * 添加关系规则
     */
    fun addRelationshipRule(typeId: String, rule: RelationshipRule) {
        val rules = relationshipRules[typeId]?.toMutableList() ?: mutableListOf()
        rules.add(rule)
        relationshipRules[typeId] = rules
    }

    /**
     * 获取关系规则
     */
    fun getRelationshipRules(typeId: String): List<RelationshipRule> {
        return relationshipRules[typeId] ?: emptyList()
    }

    /**
     * 创建关系
     */
    fun createRelationship(
        sourceEntityId: String,
        targetEntityId: String,
        typeId: String,
        confidence: Double,
        properties: Map<String, JsonElement> = emptyMap()
    ): Relationship {
        val relationship = Relationship(
            id = "rel-${UUID.randomUUID()}",
            sourceEntityId = sourceEntityId,
            targetEntityId = targetEntityId,
            typeId = typeId,
            confidence = confidence,
            properties = properties,
            createdAt = System.currentTimeMillis()
        )

        relationships[relationship.id] = relationship
        return relationship
    }

    /**
     * 获取关系
     */
    fun getRelationship(relationshipId: String): Relationship? {
        return relationships[relationshipId]
    }

    /**
     * 删除关系
     */
    fun deleteRelationship(relationshipId: String) {
        relationships.remove(relationshipId)
    }

    /**
     * 获取实体的关系
     */
    fun getEntityRelationships(entityId: String): List<Relationship> {
        return relationships.values.filter {
            it.sourceEntityId == entityId || it.targetEntityId == entityId
        }
    }

    /**
     * 获取实体之间的关系
     */
    fun getRelationshipsBetweenEntities(sourceEntityId: String, targetEntityId: String): List<Relationship> {
        return relationships.values.filter {
            (it.sourceEntityId == sourceEntityId && it.targetEntityId == targetEntityId) ||
            (it.sourceEntityId == targetEntityId && it.targetEntityId == sourceEntityId)
        }
    }

    /**
     * 推理实体之间的关系
     */
    fun inferRelationships(
        entities: List<ResolvedEntity>,
        context: Context? = null
    ): List<Relationship> {
        val inferredRelationships = mutableListOf<Relationship>()

        // 对每对实体应用规则
        for (i in entities.indices) {
            for (j in i + 1 until entities.size) {
                val sourceEntity = entities[i]
                val targetEntity = entities[j]

                // 应用所有关系类型的规则
                relationshipTypes.forEach { (typeId, _) ->
                    val rules = relationshipRules[typeId] ?: emptyList()

                    // 应用每个规则
                    rules.forEach { rule ->
                        val result = applyRule(rule, sourceEntity, targetEntity, context)

                        if (result.isApplicable && result.confidence > 0.0) {
                            val relationship = createRelationship(
                                sourceEntityId = sourceEntity.id,
                                targetEntityId = targetEntity.id,
                                typeId = typeId,
                                confidence = result.confidence,
                                properties = result.properties
                            )

                            inferredRelationships.add(relationship)
                        }
                    }
                }
            }
        }

        return inferredRelationships
    }

    /**
     * 应用规则
     */
    private fun applyRule(
        rule: RelationshipRule,
        sourceEntity: ResolvedEntity,
        targetEntity: ResolvedEntity,
        context: Context?
    ): RuleApplicationResult {
        return when (rule.type) {
            "proximity" -> applyProximityRule(rule, sourceEntity, targetEntity)
            "semantic" -> applySemanticRule(rule, sourceEntity, targetEntity)
            "contextual" -> applyContextualRule(rule, sourceEntity, targetEntity, context)
            "temporal" -> applyTemporalRule(rule, sourceEntity, targetEntity)
            else -> RuleApplicationResult(isApplicable = false)
        }
    }

    /**
     * 应用邻近规则
     */
    private fun applyProximityRule(
        rule: RelationshipRule,
        sourceEntity: ResolvedEntity,
        targetEntity: ResolvedEntity
    ): RuleApplicationResult {
        // 计算实体在文本中的距离
        val distance = 5 // 简化实现，使用固定值

        // 根据距离计算置信度
        val maxDistance = rule.parameters["maxDistance"]?.toString()?.toIntOrNull() ?: 10
        val confidence = if (distance <= maxDistance) {
            1.0 - (distance.toDouble() / maxDistance.toDouble())
        } else {
            0.0
        }

        return RuleApplicationResult(
            isApplicable = confidence > 0.0,
            confidence = confidence,
            properties = buildJsonObject {
                put("distance", JsonPrimitive(distance))
            }
        )
    }

    /**
     * 应用语义规则
     */
    private fun applySemanticRule(
        rule: RelationshipRule,
        sourceEntity: ResolvedEntity,
        targetEntity: ResolvedEntity
    ): RuleApplicationResult {
        // 检查实体类型是否匹配规则
        val sourceTypeMatches = rule.parameters["sourceType"]?.toString() == sourceEntity.type
        val targetTypeMatches = rule.parameters["targetType"]?.toString() == targetEntity.type

        // 如果类型匹配，则应用规则
        val isApplicable = sourceTypeMatches && targetTypeMatches
        val confidence = if (isApplicable) {
            rule.parameters["confidence"]?.toString()?.toDoubleOrNull() ?: 0.5
        } else {
            0.0
        }

        return RuleApplicationResult(
            isApplicable = isApplicable,
            confidence = confidence,
            properties = buildJsonObject {
                put("sourceType", JsonPrimitive(sourceEntity.type))
                put("targetType", JsonPrimitive(targetEntity.type))
            }
        )
    }

    /**
     * 应用上下文规则
     */
    private fun applyContextualRule(
        rule: RelationshipRule,
        sourceEntity: ResolvedEntity,
        targetEntity: ResolvedEntity,
        context: Context?
    ): RuleApplicationResult {
        // 如果没有上下文，则规则不适用
        if (context == null) {
            return RuleApplicationResult(isApplicable = false)
        }

        // 检查上下文类型是否匹配规则
        val contextTypeMatches = rule.parameters["contextType"]?.toString() == context.type

        // 如果上下文类型匹配，则应用规则
        val isApplicable = contextTypeMatches
        val confidence = if (isApplicable) {
            rule.parameters["confidence"]?.toString()?.toDoubleOrNull() ?: 0.5
        } else {
            0.0
        }

        return RuleApplicationResult(
            isApplicable = isApplicable,
            confidence = confidence,
            properties = buildJsonObject {
                put("contextType", JsonPrimitive(context.type))
                put("contextId", JsonPrimitive(context.id))
            }
        )
    }

    /**
     * 应用时间规则
     */
    private fun applyTemporalRule(
        rule: RelationshipRule,
        sourceEntity: ResolvedEntity,
        targetEntity: ResolvedEntity
    ): RuleApplicationResult {
        // 获取实体的时间属性
        val sourceTime = 1000L // 简化实现，使用固定值
        val targetTime = 2000L // 简化实现，使用固定值

        // 简化实现，不需要检查空值

        // 计算时间差
        val timeDiff = Math.abs(sourceTime - targetTime)

        // 根据时间差计算置信度
        val maxTimeDiff = rule.parameters["maxTimeDiff"]?.toString()?.toLongOrNull() ?: 3600000L // 默认1小时
        val confidence = if (timeDiff <= maxTimeDiff) {
            1.0 - (timeDiff.toDouble() / maxTimeDiff)
        } else {
            0.0
        }

        return RuleApplicationResult(
            isApplicable = confidence > 0.0,
            confidence = confidence,
            properties = buildJsonObject {
                put("timeDiff", JsonPrimitive(timeDiff))
            }
        )
    }

    /**
     * 分析关系路径
     */
    fun analyzeRelationshipPath(
        startEntityId: String,
        endEntityId: String,
        maxDepth: Int = 5
    ): List<RelationshipPath> {
        // 使用广度优先搜索查找路径
        val queue = mutableListOf<RelationshipPath>()
        val visited = mutableSetOf<String>()
        val paths = mutableListOf<RelationshipPath>()

        // 初始路径
        queue.add(RelationshipPath(entities = listOf(startEntityId), relationships = emptyList()))
        visited.add(startEntityId)

        while (queue.isNotEmpty() && paths.size < 10) { // 限制返回的路径数量
            val currentPath = queue.removeAt(0)
            val currentEntityId = currentPath.entities.last()

            // 如果到达目标实体，则添加到路径列表
            if (currentEntityId == endEntityId) {
                paths.add(currentPath)
                continue
            }

            // 如果达到最大深度，则跳过
            if (currentPath.entities.size > maxDepth) {
                continue
            }

            // 获取当前实体的所有关系
            val entityRelationships = getEntityRelationships(currentEntityId)

            // 遍历关系
            entityRelationships.forEach { relationship ->
                val nextEntityId = if (relationship.sourceEntityId == currentEntityId) {
                    relationship.targetEntityId
                } else {
                    relationship.sourceEntityId
                }

                // 如果下一个实体未访问过，则添加到队列
                if (!visited.contains(nextEntityId) && !currentPath.entities.contains(nextEntityId)) {
                    val newPath = RelationshipPath(
                        entities = currentPath.entities + nextEntityId,
                        relationships = currentPath.relationships + relationship.id
                    )

                    queue.add(newPath)
                    visited.add(nextEntityId)
                }
            }
        }

        return paths
    }

    /**
     * 计算关系强度
     */
    fun calculateRelationshipStrength(
        sourceEntityId: String,
        targetEntityId: String
    ): Double {
        // 获取实体之间的直接关系
        val directRelationships = getRelationshipsBetweenEntities(sourceEntityId, targetEntityId)

        // 如果有直接关系，则计算强度
        if (directRelationships.isNotEmpty()) {
            return directRelationships.maxOf { it.confidence }
        }

        // 如果没有直接关系，则分析路径
        val paths = analyzeRelationshipPath(sourceEntityId, targetEntityId)

        // 如果没有路径，则强度为0
        if (paths.isEmpty()) {
            return 0.0
        }

        // 计算最短路径的强度
        val shortestPath = paths.minByOrNull { it.entities.size }!!
        val pathRelationships = shortestPath.relationships.mapNotNull { getRelationship(it) }

        // 路径强度为路径上所有关系强度的乘积
        return pathRelationships.fold(1.0) { acc, relationship -> acc * relationship.confidence }
    }
}

/**
 * 关系类型
 */
@Serializable
data class RelationshipType(
    /**
     * 类型 ID
     */
    val id: String,

    /**
     * 类型名称
     */
    val name: String,

    /**
     * 类型描述
     */
    val description: String,

    /**
     * 是否对称
     */
    val symmetric: Boolean = false,

    /**
     * 是否传递
     */
    val transitive: Boolean = false,

    /**
     * 类型属性
     */
    val properties: Map<String, String> = emptyMap()
)

/**
 * 关系规则
 */
@Serializable
data class RelationshipRule(
    /**
     * 规则 ID
     */
    val id: String,

    /**
     * 规则类型
     */
    val type: String,

    /**
     * 规则描述
     */
    val description: String,

    /**
     * 规则参数
     */
    val parameters: Map<String, JsonElement> = emptyMap()
)

/**
 * 关系
 */
@Serializable
data class Relationship(
    /**
     * 关系 ID
     */
    val id: String,

    /**
     * 源实体 ID
     */
    val sourceEntityId: String,

    /**
     * 目标实体 ID
     */
    val targetEntityId: String,

    /**
     * 关系类型 ID
     */
    val typeId: String,

    /**
     * 置信度
     */
    val confidence: Double,

    /**
     * 关系属性
     */
    val properties: Map<String, JsonElement> = emptyMap(),

    /**
     * 创建时间
     */
    val createdAt: Long
)

/**
 * 规则应用结果
 */
data class RuleApplicationResult(
    /**
     * 规则是否适用
     */
    val isApplicable: Boolean,

    /**
     * 置信度
     */
    val confidence: Double = 0.0,

    /**
     * 属性
     */
    val properties: JsonObject = JsonObject(emptyMap())
)

/**
 * 关系路径
 */
data class RelationshipPath(
    /**
     * 实体 ID 列表
     */
    val entities: List<String>,

    /**
     * 关系 ID 列表
     */
    val relationships: List<String>
)
