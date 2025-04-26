package ai.kastrax.memory.api

import kotlinx.datetime.Instant

/**
 * 结构化记忆接口，用于存储和检索结构化数据。
 */
interface StructuredMemory {
    /**
     * 保存结构化数据。
     *
     * @param data 结构化数据
     * @param namespace 命名空间，用于组织数据
     * @return 保存的数据ID
     */
    suspend fun saveData(data: StructuredData, namespace: String = "default"): String
    
    /**
     * 根据ID获取结构化数据。
     *
     * @param id 数据ID
     * @param namespace 命名空间
     * @return 结构化数据，如果不存在则返回null
     */
    suspend fun getData(id: String, namespace: String = "default"): StructuredData?
    
    /**
     * 根据类型获取结构化数据列表。
     *
     * @param type 数据类型
     * @param namespace 命名空间
     * @param limit 最大返回数量
     * @return 结构化数据列表
     */
    suspend fun getDataByType(type: String, namespace: String = "default", limit: Int = 10): List<StructuredData>
    
    /**
     * 根据属性查询结构化数据。
     *
     * @param query 查询条件，键为属性名，值为属性值
     * @param namespace 命名空间
     * @param limit 最大返回数量
     * @return 结构化数据列表
     */
    suspend fun queryData(query: Map<String, Any>, namespace: String = "default", limit: Int = 10): List<StructuredData>
    
    /**
     * 更新结构化数据。
     *
     * @param id 数据ID
     * @param updates 更新的属性，键为属性名，值为属性值
     * @param namespace 命名空间
     * @return 是否更新成功
     */
    suspend fun updateData(id: String, updates: Map<String, Any>, namespace: String = "default"): Boolean
    
    /**
     * 删除结构化数据。
     *
     * @param id 数据ID
     * @param namespace 命名空间
     * @return 是否删除成功
     */
    suspend fun deleteData(id: String, namespace: String = "default"): Boolean
    
    /**
     * 列出命名空间中的所有数据。
     *
     * @param namespace 命名空间
     * @param limit 最大返回数量
     * @param offset 偏移量
     * @return 结构化数据列表
     */
    suspend fun listData(namespace: String = "default", limit: Int = 10, offset: Int = 0): List<StructuredData>
    
    /**
     * 获取命名空间中的数据类型列表。
     *
     * @param namespace 命名空间
     * @return 数据类型列表
     */
    suspend fun getDataTypes(namespace: String = "default"): List<String>
    
    /**
     * 创建数据之间的关系。
     *
     * @param sourceId 源数据ID
     * @param targetId 目标数据ID
     * @param relationType 关系类型
     * @param properties 关系属性
     * @param namespace 命名空间
     * @return 关系ID
     */
    suspend fun createRelation(
        sourceId: String,
        targetId: String,
        relationType: String,
        properties: Map<String, Any> = emptyMap(),
        namespace: String = "default"
    ): String
    
    /**
     * 获取与指定数据相关的数据。
     *
     * @param id 数据ID
     * @param relationType 关系类型，如果为null则返回所有关系类型
     * @param direction 关系方向，IN表示入边，OUT表示出边，BOTH表示两者都包括
     * @param namespace 命名空间
     * @param limit 最大返回数量
     * @return 相关数据列表
     */
    suspend fun getRelatedData(
        id: String,
        relationType: String? = null,
        direction: RelationDirection = RelationDirection.BOTH,
        namespace: String = "default",
        limit: Int = 10
    ): List<RelatedData>
    
    /**
     * 删除数据之间的关系。
     *
     * @param relationId 关系ID
     * @param namespace 命名空间
     * @return 是否删除成功
     */
    suspend fun deleteRelation(relationId: String, namespace: String = "default"): Boolean
}

/**
 * 结构化数据类，表示一个结构化的数据实体。
 *
 * @property id 数据ID
 * @property type 数据类型
 * @property properties 数据属性
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
data class StructuredData(
    val id: String,
    val type: String,
    val properties: Map<String, Any>,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * 关系方向枚举。
 */
enum class RelationDirection {
    /**
     * 入边，表示其他数据指向当前数据的关系。
     */
    IN,
    
    /**
     * 出边，表示当前数据指向其他数据的关系。
     */
    OUT,
    
    /**
     * 两者都包括。
     */
    BOTH
}

/**
 * 关系数据类，表示两个数据之间的关系。
 *
 * @property id 关系ID
 * @property sourceId 源数据ID
 * @property targetId 目标数据ID
 * @property type 关系类型
 * @property properties 关系属性
 * @property createdAt 创建时间
 */
data class Relation(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val type: String,
    val properties: Map<String, Any>,
    val createdAt: Instant
)

/**
 * 相关数据类，表示与指定数据相关的数据及其关系。
 *
 * @property data 相关数据
 * @property relation 关系
 * @property direction 关系方向
 */
data class RelatedData(
    val data: StructuredData,
    val relation: Relation,
    val direction: RelationDirection
)

/**
 * 结构化记忆配置类。
 *
 * @property enableRelations 是否启用关系功能
 * @property defaultNamespace 默认命名空间
 * @property maxPropertiesPerData 每个数据最大属性数量
 * @property maxRelationsPerData 每个数据最大关系数量
 */
data class StructuredMemoryConfig(
    val enableRelations: Boolean = true,
    val defaultNamespace: String = "default",
    val maxPropertiesPerData: Int = 100,
    val maxRelationsPerData: Int = 1000
)
