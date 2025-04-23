package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryTag
import ai.kastrax.memory.api.MemoryTagManager
import ai.kastrax.memory.api.TagCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import java.util.concurrent.TimeUnit

/**
 * Redis实现的标签管理器。
 *
 * @property jedisPool Redis连接池
 * @property keyPrefix Redis键前缀
 * @property expireTime 过期时间（秒）
 */
class RedisTagManager(
    private val jedisPool: JedisPool,
    private val keyPrefix: String = "kastrax:tags:",
    private val expireTime: Long = TimeUnit.DAYS.toSeconds(30) // 默认30天过期
) : MemoryTagManager, KastraXBase(component = "TAG_MANAGER", name = "redis") {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 构建消息标签键。
     */
    private fun buildMessageTagsKey(messageId: String): String {
        return "${keyPrefix}message:$messageId:tags"
    }

    /**
     * 构建线程消息键。
     */
    private fun buildThreadMessagesKey(threadId: String): String {
        return "${keyPrefix}thread:$threadId:messages"
    }

    /**
     * 构建标签分类键。
     */
    private fun buildCategoryKey(categoryName: String): String {
        return "${keyPrefix}category:$categoryName"
    }

    /**
     * 构建所有分类键。
     */
    private fun buildAllCategoriesKey(): String {
        return "${keyPrefix}categories"
    }

    /**
     * 构建标签到分类映射键。
     */
    private fun buildTagCategoryKey(tagName: String): String {
        return "${keyPrefix}tag:$tagName:category"
    }

    override suspend fun addTagToMessage(messageId: String, tag: MemoryTag): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val key = buildMessageTagsKey(messageId)

                    // 获取现有标签
                    val existingTagsJson = jedis.get(key)
                    val existingTags = if (existingTagsJson != null) {
                        json.decodeFromString<List<MemoryTag>>(existingTagsJson).toMutableList()
                    } else {
                        mutableListOf()
                    }

                    // 检查标签是否已存在
                    val existingIndex = existingTags.indexOfFirst { it.name == tag.name && it.value == tag.value }
                    if (existingIndex >= 0) {
                        // 标签已存在，不需要更新
                        return@withContext true
                    }

                    // 如果有相同名称的标签，删除它
                    existingTags.removeIf { it.name == tag.name }

                    // 添加新标签
                    existingTags.add(tag)

                    // 保存更新后的标签
                    val updatedTagsJson = json.encodeToString(existingTags)
                    jedis.setex(key, expireTime, updatedTagsJson)

                    true
                }
            } catch (e: Exception) {
                logger.error("添加标签到消息失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun removeTagFromMessage(messageId: String, tagName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val key = buildMessageTagsKey(messageId)

                    // 获取现有标签
                    val existingTagsJson = jedis.get(key) ?: return@withContext false
                    val existingTags = json.decodeFromString<List<MemoryTag>>(existingTagsJson).toMutableList()

                    // 移除标签
                    val removed = existingTags.removeIf { it.name == tagName }

                    if (!removed) {
                        return@withContext false
                    }

                    // 保存更新后的标签
                    if (existingTags.isEmpty()) {
                        jedis.del(key)
                    } else {
                        val updatedTagsJson = json.encodeToString(existingTags)
                        jedis.setex(key, expireTime, updatedTagsJson)
                    }

                    true
                }
            } catch (e: Exception) {
                logger.error("从消息中移除标签失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getMessageTags(messageId: String): List<MemoryTag> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val key = buildMessageTagsKey(messageId)
                    val tagsJson = jedis.get(key) ?: return@withContext emptyList()

                    json.decodeFromString<List<MemoryTag>>(tagsJson)
                }
            } catch (e: Exception) {
                logger.error("获取消息标签失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun searchMessagesByTag(
        threadId: String,
        tagName: String,
        tagValue: String?,
        limit: Int
    ): List<MemoryMessage> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val threadMessagesKey = buildThreadMessagesKey(threadId)
                    val messageIds = jedis.smembers(threadMessagesKey)

                    if (messageIds.isEmpty()) {
                        return@withContext emptyList()
                    }

                    // 筛选具有指定标签的消息ID
                    val filteredMessageIds = messageIds.filter { messageId ->
                        val messageTagsKey = buildMessageTagsKey(messageId)
                        val tagsJson = jedis.get(messageTagsKey) ?: return@filter false

                        val tags = json.decodeFromString<List<MemoryTag>>(tagsJson)

                        if (tagValue != null) {
                            tags.any { it.name == tagName && it.value == tagValue }
                        } else {
                            tags.any { it.name == tagName }
                        }
                    }

                    // 创建模拟消息对象返回
                    // 在实际实现中，应该从消息存储中获取真实消息
                    filteredMessageIds.map { messageId ->
                        // 获取消息的标签
                        val messageTagsKey = buildMessageTagsKey(messageId)
                        val tagsJson = jedis.get(messageTagsKey)
                        val tags = if (tagsJson != null) {
                            json.decodeFromString<List<MemoryTag>>(tagsJson)
                        } else {
                            emptyList()
                        }

                        val tagContent = tags.joinToString(", ") { "${it.name}=${it.value}" }

                        MemoryMessage(
                            id = messageId,
                            threadId = threadId,
                            message = SimpleMessage(
                                role = ai.kastrax.memory.api.MessageRole.SYSTEM,
                                content = "Tagged message with: $tagContent"
                            ),
                            createdAt = kotlinx.datetime.Clock.System.now()
                        )
                    }.take(limit)
                }
            } catch (e: Exception) {
                logger.error("根据标签搜索消息失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun getThreadTags(threadId: String): List<MemoryTag> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val threadMessagesKey = buildThreadMessagesKey(threadId)
                    val messageIds = jedis.smembers(threadMessagesKey)

                    if (messageIds.isEmpty()) {
                        return@withContext emptyList()
                    }

                    // 收集所有消息的标签
                    val allTags = mutableSetOf<MemoryTag>()

                    for (messageId in messageIds) {
                        val messageTagsKey = buildMessageTagsKey(messageId)
                        val tagsJson = jedis.get(messageTagsKey) ?: continue

                        val tags = json.decodeFromString<List<MemoryTag>>(tagsJson)
                        allTags.addAll(tags)
                    }

                    allTags.toList()
                }
            } catch (e: Exception) {
                logger.error("获取线程标签失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun createTagCategory(name: String, description: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val categoryKey = buildCategoryKey(name)
                    val allCategoriesKey = buildAllCategoriesKey()

                    // 检查分类是否已存在
                    if (jedis.exists(categoryKey)) {
                        return@withContext false
                    }

                    // 创建分类
                    val category = TagCategory(name, description)
                    val categoryJson = json.encodeToString(category)

                    jedis.setex(categoryKey, expireTime, categoryJson)
                    jedis.sadd(allCategoriesKey, name)

                    true
                }
            } catch (e: Exception) {
                logger.error("创建标签分类失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getTagCategories(): List<TagCategory> {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val allCategoriesKey = buildAllCategoriesKey()
                    val categoryNames = jedis.smembers(allCategoriesKey)

                    if (categoryNames.isEmpty()) {
                        return@withContext emptyList()
                    }

                    // 获取所有分类
                    val categories = mutableListOf<TagCategory>()

                    for (categoryName in categoryNames) {
                        val categoryKey = buildCategoryKey(categoryName)
                        val categoryJson = jedis.get(categoryKey) ?: continue

                        val category = json.decodeFromString<TagCategory>(categoryJson)
                        categories.add(category)
                    }

                    categories
                }
            } catch (e: Exception) {
                logger.error("获取标签分类失败: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun addTagToCategory(tagName: String, categoryName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val categoryKey = buildCategoryKey(categoryName)
                    val tagCategoryKey = buildTagCategoryKey(tagName)

                    // 检查分类是否存在
                    val categoryJson = jedis.get(categoryKey) ?: return@withContext false
                    val category = json.decodeFromString<TagCategory>(categoryJson)

                    // 检查标签是否已在分类中
                    if (category.tags.contains(tagName)) {
                        return@withContext true
                    }

                    // 更新分类
                    val updatedCategory = category.copy(
                        tags = category.tags + tagName
                    )
                    val updatedCategoryJson = json.encodeToString(updatedCategory)

                    jedis.setex(categoryKey, expireTime, updatedCategoryJson)
                    jedis.setex(tagCategoryKey, expireTime, categoryName)

                    true
                }
            } catch (e: Exception) {
                logger.error("添加标签到分类失败: ${e.message}")
                false
            }
        }
    }

    /**
     * 注册消息到线程。
     *
     * @param messageId 消息ID
     * @param threadId 线程ID
     */
    suspend fun registerMessageToThread(messageId: String, threadId: String) {
        withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val threadMessagesKey = buildThreadMessagesKey(threadId)
                    jedis.sadd(threadMessagesKey, messageId)
                    jedis.expire(threadMessagesKey, expireTime)
                }
            } catch (e: Exception) {
                logger.error("注册消息到线程失败: ${e.message}")
            }
        }
    }
}
