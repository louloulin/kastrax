package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 实体解析器，负责解析消息中的实体引用
 */
class EntityResolver {
    /**
     * 实体类型映射
     */
    private val entityTypes = ConcurrentHashMap<String, EntityType>()
    
    /**
     * 实体提取器映射
     */
    private val entityExtractors = ConcurrentHashMap<String, EntityExtractor>()
    
    /**
     * 注册实体类型
     */
    fun registerEntityType(entityType: EntityType) {
        entityTypes[entityType.id] = entityType
    }
    
    /**
     * 注销实体类型
     */
    fun unregisterEntityType(entityTypeId: String) {
        entityTypes.remove(entityTypeId)
    }
    
    /**
     * 获取实体类型
     */
    fun getEntityType(entityTypeId: String): EntityType? {
        return entityTypes[entityTypeId]
    }
    
    /**
     * 获取所有实体类型
     */
    fun getAllEntityTypes(): List<EntityType> {
        return entityTypes.values.toList()
    }
    
    /**
     * 注册实体提取器
     */
    fun registerEntityExtractor(entityExtractor: EntityExtractor) {
        entityExtractors[entityExtractor.id] = entityExtractor
    }
    
    /**
     * 注销实体提取器
     */
    fun unregisterEntityExtractor(entityExtractorId: String) {
        entityExtractors.remove(entityExtractorId)
    }
    
    /**
     * 获取实体提取器
     */
    fun getEntityExtractor(entityExtractorId: String): EntityExtractor? {
        return entityExtractors[entityExtractorId]
    }
    
    /**
     * 获取所有实体提取器
     */
    fun getAllEntityExtractors(): List<EntityExtractor> {
        return entityExtractors.values.toList()
    }
    
    /**
     * 解析实体
     */
    fun resolveEntities(text: String): Map<String, List<ResolvedEntity>> {
        val result = mutableMapOf<String, MutableList<ResolvedEntity>>()
        
        // 对每个实体类型进行解析
        entityTypes.forEach { (entityTypeId, entityType) ->
            val extractors = entityExtractors.values.filter { it.entityTypeId == entityTypeId }
            
            // 对每个提取器进行解析
            extractors.forEach { extractor ->
                val entities = extractEntities(text, extractor)
                
                // 添加到结果
                if (entities.isNotEmpty()) {
                    val typeEntities = result.getOrPut(entityTypeId) { mutableListOf() }
                    typeEntities.addAll(entities)
                }
            }
        }
        
        return result
    }
    
    /**
     * 提取实体
     */
    private fun extractEntities(text: String, extractor: EntityExtractor): List<ResolvedEntity> {
        return when (extractor.type) {
            "regex" -> extractRegexEntities(text, extractor)
            "dictionary" -> extractDictionaryEntities(text, extractor)
            "rule" -> extractRuleEntities(text, extractor)
            else -> emptyList()
        }
    }
    
    /**
     * 提取正则表达式实体
     */
    private fun extractRegexEntities(text: String, extractor: EntityExtractor): List<ResolvedEntity> {
        val result = mutableListOf<ResolvedEntity>()
        
        val pattern = Pattern.compile(extractor.pattern, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            val value = matcher.group()
            val startIndex = matcher.start()
            val endIndex = matcher.end()
            
            result.add(
                ResolvedEntity(
                    id = "entity-${UUID.randomUUID()}",
                    type = extractor.entityTypeId,
                    value = JsonPrimitive(value),
                    text = value,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    confidence = 1.0
                )
            )
        }
        
        return result
    }
    
    /**
     * 提取字典实体
     */
    private fun extractDictionaryEntities(text: String, extractor: EntityExtractor): List<ResolvedEntity> {
        val result = mutableListOf<ResolvedEntity>()
        
        val dictionary = extractor.pattern.split(",").map { it.trim() }
        
        dictionary.forEach { entry ->
            val regex = "\\b${Pattern.quote(entry)}\\b"
            val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            
            while (matcher.find()) {
                val value = matcher.group()
                val startIndex = matcher.start()
                val endIndex = matcher.end()
                
                result.add(
                    ResolvedEntity(
                        id = "entity-${UUID.randomUUID()}",
                        type = extractor.entityTypeId,
                        value = JsonPrimitive(value),
                        text = value,
                        startIndex = startIndex,
                        endIndex = endIndex,
                        confidence = 1.0
                    )
                )
            }
        }
        
        return result
    }
    
    /**
     * 提取规则实体
     */
    private fun extractRuleEntities(text: String, extractor: EntityExtractor): List<ResolvedEntity> {
        // 规则实体提取需要更复杂的逻辑
        // 这里只是一个简单的实现
        return emptyList()
    }
    
    /**
     * 创建实体类型
     */
    fun createEntityType(name: String, description: String): EntityType {
        val entityType = EntityType(
            id = "entity-type-${UUID.randomUUID()}",
            name = name,
            description = description
        )
        
        entityTypes[entityType.id] = entityType
        return entityType
    }
    
    /**
     * 创建正则表达式提取器
     */
    fun createRegexExtractor(entityTypeId: String, name: String, pattern: String): EntityExtractor? {
        val entityType = entityTypes[entityTypeId] ?: return null
        
        val extractor = EntityExtractor(
            id = "extractor-${UUID.randomUUID()}",
            name = name,
            entityTypeId = entityTypeId,
            type = "regex",
            pattern = pattern
        )
        
        entityExtractors[extractor.id] = extractor
        return extractor
    }
    
    /**
     * 创建字典提取器
     */
    fun createDictionaryExtractor(entityTypeId: String, name: String, entries: List<String>): EntityExtractor? {
        val entityType = entityTypes[entityTypeId] ?: return null
        
        val extractor = EntityExtractor(
            id = "extractor-${UUID.randomUUID()}",
            name = name,
            entityTypeId = entityTypeId,
            type = "dictionary",
            pattern = entries.joinToString(",")
        )
        
        entityExtractors[extractor.id] = extractor
        return extractor
    }
}

/**
 * 实体类型
 */
@Serializable
data class EntityType(
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
     * 类型元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 实体提取器
 */
@Serializable
data class EntityExtractor(
    /**
     * 提取器 ID
     */
    val id: String,
    
    /**
     * 提取器名称
     */
    val name: String,
    
    /**
     * 实体类型 ID
     */
    val entityTypeId: String,
    
    /**
     * 提取器类型
     */
    val type: String,
    
    /**
     * 提取器模式
     */
    val pattern: String,
    
    /**
     * 提取器元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 已解析实体
 */
@Serializable
data class ResolvedEntity(
    /**
     * 实体 ID
     */
    val id: String,
    
    /**
     * 实体类型
     */
    val type: String,
    
    /**
     * 实体值
     */
    val value: JsonElement,
    
    /**
     * 实体文本
     */
    val text: String,
    
    /**
     * 开始索引
     */
    val startIndex: Int,
    
    /**
     * 结束索引
     */
    val endIndex: Int,
    
    /**
     * 置信度
     */
    val confidence: Double,
    
    /**
     * 实体元数据
     */
    val metadata: Map<String, String> = emptyMap()
)
