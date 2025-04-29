package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 已解析的实体
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
    val value: String,
    
    /**
     * 实体在文本中的位置
     */
    val position: Int,
    
    /**
     * 实体长度
     */
    val length: Int,
    
    /**
     * 置信度
     */
    val confidence: Double,
    
    /**
     * 实体属性
     */
    val properties: Map<String, JsonElement> = emptyMap()
)
