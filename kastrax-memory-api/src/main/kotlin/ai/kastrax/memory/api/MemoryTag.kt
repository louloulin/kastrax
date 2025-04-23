package ai.kastrax.memory.api

import kotlinx.serialization.Serializable

/**
 * 内存标签，用于对内存消息进行分类和标记。
 *
 * @property name 标签名称
 * @property value 标签值
 * @property color 标签颜色（可选）
 */
@Serializable
data class MemoryTag(
    val name: String,
    val value: String,
    val color: String? = null
)

/**
 * 内存标签管理器接口，用于管理内存标签。
 */
interface MemoryTagManager {
    /**
     * 添加标签到消息。
     *
     * @param messageId 消息ID
     * @param tag 标签
     * @return 是否成功
     */
    suspend fun addTagToMessage(messageId: String, tag: MemoryTag): Boolean
    
    /**
     * 从消息中移除标签。
     *
     * @param messageId 消息ID
     * @param tagName 标签名称
     * @return 是否成功
     */
    suspend fun removeTagFromMessage(messageId: String, tagName: String): Boolean
    
    /**
     * 获取消息的所有标签。
     *
     * @param messageId 消息ID
     * @return 标签列表
     */
    suspend fun getMessageTags(messageId: String): List<MemoryTag>
    
    /**
     * 根据标签搜索消息。
     *
     * @param threadId 线程ID
     * @param tagName 标签名称
     * @param tagValue 标签值（可选）
     * @param limit 限制返回的消息数量
     * @return 消息列表
     */
    suspend fun searchMessagesByTag(
        threadId: String,
        tagName: String,
        tagValue: String? = null,
        limit: Int = 10
    ): List<MemoryMessage>
    
    /**
     * 获取线程中的所有标签。
     *
     * @param threadId 线程ID
     * @return 标签列表
     */
    suspend fun getThreadTags(threadId: String): List<MemoryTag>
    
    /**
     * 创建标签分类。
     *
     * @param name 分类名称
     * @param description 分类描述
     * @return 是否成功
     */
    suspend fun createTagCategory(name: String, description: String): Boolean
    
    /**
     * 获取所有标签分类。
     *
     * @return 分类列表
     */
    suspend fun getTagCategories(): List<TagCategory>
    
    /**
     * 将标签添加到分类。
     *
     * @param tagName 标签名称
     * @param categoryName 分类名称
     * @return 是否成功
     */
    suspend fun addTagToCategory(tagName: String, categoryName: String): Boolean
}

/**
 * 标签分类，用于对标签进行分组。
 *
 * @property name 分类名称
 * @property description 分类描述
 * @property tags 分类中的标签名称列表
 */
@Serializable
data class TagCategory(
    val name: String,
    val description: String,
    val tags: List<String> = emptyList()
)
