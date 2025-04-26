package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.RelatedData
import ai.kastrax.memory.api.Relation
import ai.kastrax.memory.api.RelationDirection
import ai.kastrax.memory.api.StructuredData
import ai.kastrax.memory.api.StructuredMemory
import ai.kastrax.memory.api.StructuredMemoryConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存中的结构化记忆实现。
 *
 * @property config 结构化记忆配置
 */
class InMemoryStructuredMemory(
    private val config: StructuredMemoryConfig = StructuredMemoryConfig()
) : StructuredMemory, KastraXBase(component = "STRUCTURED_MEMORY", name = "in-memory") {
    
    // 数据存储，按命名空间组织
    private val dataMutex = Mutex()
    private val dataStore = ConcurrentHashMap<String, ConcurrentHashMap<String, StructuredData>>()
    
    // 关系存储，按命名空间组织
    private val relationMutex = Mutex()
    private val relationStore = ConcurrentHashMap<String, ConcurrentHashMap<String, Relation>>()
    
    // 数据类型索引，按命名空间组织
    private val typeIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    
    // 关系索引，按命名空间组织
    private val sourceIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    private val targetIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    private val relationTypeIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    
    override suspend fun saveData(data: StructuredData, namespace: String): String {
        return dataMutex.withLock {
            // 获取或创建命名空间存储
            val namespaceStore = dataStore.getOrPut(namespace) { ConcurrentHashMap() }
            
            // 保存数据
            namespaceStore[data.id] = data
            
            // 更新类型索引
            val typeIdx = typeIndex.getOrPut(namespace) { ConcurrentHashMap() }
            val typeSet = typeIdx.getOrPut(data.type) { mutableSetOf() }
            typeSet.add(data.id)
            
            data.id
        }
    }
    
    override suspend fun getData(id: String, namespace: String): StructuredData? {
        return dataMutex.withLock {
            dataStore[namespace]?.get(id)
        }
    }
    
    override suspend fun getDataByType(type: String, namespace: String, limit: Int): List<StructuredData> {
        return dataMutex.withLock {
            val typeIdx = typeIndex[namespace] ?: return@withLock emptyList()
            val idSet = typeIdx[type] ?: return@withLock emptyList()
            val namespaceStore = dataStore[namespace] ?: return@withLock emptyList()
            
            idSet.mapNotNull { namespaceStore[it] }
                .sortedByDescending { it.updatedAt }
                .take(limit)
        }
    }
    
    override suspend fun queryData(query: Map<String, Any>, namespace: String, limit: Int): List<StructuredData> {
        return dataMutex.withLock {
            val namespaceStore = dataStore[namespace] ?: return@withLock emptyList()
            
            // 简单实现：遍历所有数据，检查是否匹配查询条件
            namespaceStore.values
                .filter { data ->
                    query.all { (key, value) ->
                        data.properties[key] == value
                    }
                }
                .sortedByDescending { it.updatedAt }
                .take(limit)
        }
    }
    
    override suspend fun updateData(id: String, updates: Map<String, Any>, namespace: String): Boolean {
        return dataMutex.withLock {
            val namespaceStore = dataStore[namespace] ?: return@withLock false
            val data = namespaceStore[id] ?: return@withLock false
            
            // 创建更新后的属性
            val updatedProperties = data.properties.toMutableMap()
            updatedProperties.putAll(updates)
            
            // 创建更新后的数据
            val updatedData = data.copy(
                properties = updatedProperties,
                updatedAt = Clock.System.now()
            )
            
            // 保存更新后的数据
            namespaceStore[id] = updatedData
            
            true
        }
    }
    
    override suspend fun deleteData(id: String, namespace: String): Boolean {
        return dataMutex.withLock {
            val namespaceStore = dataStore[namespace] ?: return@withLock false
            val data = namespaceStore[id] ?: return@withLock false
            
            // 删除数据
            namespaceStore.remove(id)
            
            // 更新类型索引
            val typeIdx = typeIndex[namespace]
            typeIdx?.get(data.type)?.remove(id)
            
            // 删除相关的关系
            deleteRelationsForData(id, namespace)
            
            true
        }
    }
    
    override suspend fun listData(namespace: String, limit: Int, offset: Int): List<StructuredData> {
        return dataMutex.withLock {
            val namespaceStore = dataStore[namespace] ?: return@withLock emptyList()
            
            namespaceStore.values
                .sortedByDescending { it.updatedAt }
                .drop(offset)
                .take(limit)
        }
    }
    
    override suspend fun getDataTypes(namespace: String): List<String> {
        return dataMutex.withLock {
            typeIndex[namespace]?.keys?.toList() ?: emptyList()
        }
    }
    
    override suspend fun createRelation(
        sourceId: String,
        targetId: String,
        relationType: String,
        properties: Map<String, Any>,
        namespace: String
    ): String {
        if (!config.enableRelations) {
            throw UnsupportedOperationException("Relations are disabled in the configuration")
        }
        
        return relationMutex.withLock {
            // 检查源数据和目标数据是否存在
            val namespaceStore = dataStore[namespace] ?: throw IllegalArgumentException("Namespace not found: $namespace")
            val sourceData = namespaceStore[sourceId] ?: throw IllegalArgumentException("Source data not found: $sourceId")
            val targetData = namespaceStore[targetId] ?: throw IllegalArgumentException("Target data not found: $targetId")
            
            // 创建关系
            val relationId = UUID.randomUUID().toString()
            val relation = Relation(
                id = relationId,
                sourceId = sourceId,
                targetId = targetId,
                type = relationType,
                properties = properties,
                createdAt = Clock.System.now()
            )
            
            // 保存关系
            val relationNamespaceStore = relationStore.getOrPut(namespace) { ConcurrentHashMap() }
            relationNamespaceStore[relationId] = relation
            
            // 更新索引
            updateRelationIndices(relation, namespace)
            
            relationId
        }
    }
    
    override suspend fun getRelatedData(
        id: String,
        relationType: String?,
        direction: RelationDirection,
        namespace: String,
        limit: Int
    ): List<RelatedData> {
        if (!config.enableRelations) {
            return emptyList()
        }
        
        return relationMutex.withLock {
            val result = mutableListOf<RelatedData>()
            
            // 获取数据存储和关系存储
            val namespaceDataStore = dataStore[namespace] ?: return@withLock emptyList()
            val namespaceRelationStore = relationStore[namespace] ?: return@withLock emptyList()
            
            // 获取相关关系
            val relationIds = mutableSetOf<String>()
            
            // 处理入边
            if (direction == RelationDirection.IN || direction == RelationDirection.BOTH) {
                val targetIdx = targetIndex[namespace]
                if (targetIdx != null) {
                    val targetRelations = targetIdx[id] ?: emptySet()
                    if (relationType != null) {
                        // 过滤特定类型的关系
                        val typeIdx = relationTypeIndex[namespace]
                        if (typeIdx != null) {
                            val typeRelations = typeIdx[relationType] ?: emptySet()
                            relationIds.addAll(targetRelations.intersect(typeRelations))
                        }
                    } else {
                        relationIds.addAll(targetRelations)
                    }
                }
            }
            
            // 处理出边
            if (direction == RelationDirection.OUT || direction == RelationDirection.BOTH) {
                val sourceIdx = sourceIndex[namespace]
                if (sourceIdx != null) {
                    val sourceRelations = sourceIdx[id] ?: emptySet()
                    if (relationType != null) {
                        // 过滤特定类型的关系
                        val typeIdx = relationTypeIndex[namespace]
                        if (typeIdx != null) {
                            val typeRelations = typeIdx[relationType] ?: emptySet()
                            relationIds.addAll(sourceRelations.intersect(typeRelations))
                        }
                    } else {
                        relationIds.addAll(sourceRelations)
                    }
                }
            }
            
            // 获取关系和相关数据
            for (relationId in relationIds) {
                val relation = namespaceRelationStore[relationId] ?: continue
                
                val relatedDataId = if (relation.sourceId == id) relation.targetId else relation.sourceId
                val relatedData = namespaceDataStore[relatedDataId] ?: continue
                
                val relatedDirection = if (relation.sourceId == id) RelationDirection.OUT else RelationDirection.IN
                
                result.add(RelatedData(
                    data = relatedData,
                    relation = relation,
                    direction = relatedDirection
                ))
                
                if (result.size >= limit) {
                    break
                }
            }
            
            result
        }
    }
    
    override suspend fun deleteRelation(relationId: String, namespace: String): Boolean {
        if (!config.enableRelations) {
            return false
        }
        
        return relationMutex.withLock {
            val namespaceRelationStore = relationStore[namespace] ?: return@withLock false
            val relation = namespaceRelationStore[relationId] ?: return@withLock false
            
            // 删除关系
            namespaceRelationStore.remove(relationId)
            
            // 更新索引
            removeRelationFromIndices(relation, namespace)
            
            true
        }
    }
    
    /**
     * 删除与指定数据相关的所有关系。
     *
     * @param dataId 数据ID
     * @param namespace 命名空间
     */
    private suspend fun deleteRelationsForData(dataId: String, namespace: String) {
        if (!config.enableRelations) {
            return
        }
        
        relationMutex.withLock {
            val namespaceRelationStore = relationStore[namespace] ?: return@withLock
            
            // 获取与数据相关的关系
            val sourceIdx = sourceIndex[namespace]
            val targetIdx = targetIndex[namespace]
            
            val sourceRelations = sourceIdx?.get(dataId) ?: emptySet()
            val targetRelations = targetIdx?.get(dataId) ?: emptySet()
            
            val relationsToDelete = sourceRelations.union(targetRelations)
            
            // 删除关系
            for (relationId in relationsToDelete) {
                val relation = namespaceRelationStore[relationId] ?: continue
                namespaceRelationStore.remove(relationId)
                removeRelationFromIndices(relation, namespace)
            }
        }
    }
    
    /**
     * 更新关系索引。
     *
     * @param relation 关系
     * @param namespace 命名空间
     */
    private fun updateRelationIndices(relation: Relation, namespace: String) {
        // 更新源索引
        val sourceIdx = sourceIndex.getOrPut(namespace) { ConcurrentHashMap() }
        val sourceSet = sourceIdx.getOrPut(relation.sourceId) { mutableSetOf() }
        sourceSet.add(relation.id)
        
        // 更新目标索引
        val targetIdx = targetIndex.getOrPut(namespace) { ConcurrentHashMap() }
        val targetSet = targetIdx.getOrPut(relation.targetId) { mutableSetOf() }
        targetSet.add(relation.id)
        
        // 更新类型索引
        val typeIdx = relationTypeIndex.getOrPut(namespace) { ConcurrentHashMap() }
        val typeSet = typeIdx.getOrPut(relation.type) { mutableSetOf() }
        typeSet.add(relation.id)
    }
    
    /**
     * 从索引中删除关系。
     *
     * @param relation 关系
     * @param namespace 命名空间
     */
    private fun removeRelationFromIndices(relation: Relation, namespace: String) {
        // 更新源索引
        val sourceIdx = sourceIndex[namespace]
        sourceIdx?.get(relation.sourceId)?.remove(relation.id)
        
        // 更新目标索引
        val targetIdx = targetIndex[namespace]
        targetIdx?.get(relation.targetId)?.remove(relation.id)
        
        // 更新类型索引
        val typeIdx = relationTypeIndex[namespace]
        typeIdx?.get(relation.type)?.remove(relation.id)
    }
}
