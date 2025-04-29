package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * 上下文
 */
@Serializable
data class Context(
    /**
     * 上下文 ID
     */
    val id: String,
    
    /**
     * 上下文名称
     */
    val name: String,
    
    /**
     * 上下文描述
     */
    val description: String,
    
    /**
     * 上下文类型
     */
    val type: String,
    
    /**
     * 创建时间
     */
    val createdAt: Long,
    
    /**
     * 更新时间
     */
    val updatedAt: Long,
    
    /**
     * 上下文数据
     */
    val data: JsonObject = buildJsonObject {}
)
