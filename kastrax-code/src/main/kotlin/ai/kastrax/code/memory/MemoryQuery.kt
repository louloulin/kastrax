package ai.kastrax.code.memory

/**
 * 记忆查询
 *
 * @property type 查询类型
 * @property filters 过滤条件
 * @property limit 限制数量
 */
data class MemoryQuery(
    val type: MemoryQueryType,
    val filters: Map<String, Any> = emptyMap(),
    val limit: Int = 10
)

/**
 * 记忆查询类型
 */
enum class MemoryQueryType {
    /**
     * 按ID查询
     */
    BY_ID,
    
    /**
     * 按类型查询
     */
    BY_TYPE,
    
    /**
     * 按元数据查询
     */
    BY_METADATA,
    
    /**
     * 按内容查询
     */
    BY_CONTENT,
    
    /**
     * 按相似度查询
     */
    BY_SIMILARITY
}

/**
 * 记忆ID
 *
 * @property id ID
 */
data class MemoryId(
    val id: String
)
