package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryTag
import ai.kastrax.memory.api.MemoryTagManager
import ai.kastrax.memory.api.TagCategory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存中的标签管理器实现。
 */
class InMemoryTagManager : MemoryTagManager, KastraXBase(component = "TAG_MANAGER", name = "in-memory") {
    private val mutex = Mutex()

    // 消息ID -> 标签列表
    private val messageTags = mutableMapOf<String, MutableList<MemoryTag>>()

    // 线程ID -> 消息ID列表
    private val threadMessages = mutableMapOf<String, MutableList<String>>()

    // 分类名称 -> 分类
    private val categories = mutableMapOf<String, TagCategory>()

    // 标签名称 -> 分类名称
    private val tagCategories = mutableMapOf<String, String>()

    override suspend fun addTagToMessage(messageId: String, tag: MemoryTag): Boolean {
        return mutex.withLock {
            val tags = messageTags.getOrPut(messageId) { mutableListOf() }

            // 如果标签已存在，更新它
            val existingIndex = tags.indexOfFirst { it.name == tag.name }
            if (existingIndex >= 0) {
                tags[existingIndex] = tag
            } else {
                tags.add(tag)
            }

            true
        }
    }

    override suspend fun removeTagFromMessage(messageId: String, tagName: String): Boolean {
        return mutex.withLock {
            val tags = messageTags[messageId] ?: return@withLock false
            val removed = tags.removeIf { it.name == tagName }

            if (tags.isEmpty()) {
                messageTags.remove(messageId)
            }

            removed
        }
    }

    override suspend fun getMessageTags(messageId: String): List<MemoryTag> {
        return mutex.withLock {
            messageTags[messageId]?.toList() ?: emptyList()
        }
    }

    override suspend fun searchMessagesByTag(
        threadId: String,
        tagName: String,
        tagValue: String?,
        limit: Int
    ): List<MemoryMessage> {
        return mutex.withLock {
            val messageIds = threadMessages[threadId] ?: return@withLock emptyList()

            // 筛选具有指定标签的消息ID
            // 这里的filteredMessageIds变量应该被使用，但由于实现不完整，目前返回空列表
            val unusedIds = messageIds.filter { messageId ->
                val tags = messageTags[messageId] ?: return@filter false

                if (tagValue != null) {
                    tags.any { it.name == tagName && it.value == tagValue }
                } else {
                    tags.any { it.name == tagName }
                }
            }

            // 获取消息（这里需要外部提供消息存储）
            // 由于我们没有直接访问消息存储，这里返回空列表
            // 实际实现中，需要通过消息ID获取消息
            emptyList()
        }
    }

    override suspend fun getThreadTags(threadId: String): List<MemoryTag> {
        return mutex.withLock {
            val messageIds = threadMessages[threadId] ?: return@withLock emptyList()

            // 收集线程中所有消息的标签
            val tags = mutableSetOf<MemoryTag>()

            for (messageId in messageIds) {
                val messageTags = messageTags[messageId] ?: continue
                tags.addAll(messageTags)
            }

            tags.toList()
        }
    }

    override suspend fun createTagCategory(name: String, description: String): Boolean {
        return mutex.withLock {
            if (categories.containsKey(name)) {
                return@withLock false
            }

            categories[name] = TagCategory(name, description)
            true
        }
    }

    override suspend fun getTagCategories(): List<TagCategory> {
        return mutex.withLock {
            categories.values.toList()
        }
    }

    override suspend fun addTagToCategory(tagName: String, categoryName: String): Boolean {
        return mutex.withLock {
            val category = categories[categoryName] ?: return@withLock false

            if (category.tags.contains(tagName)) {
                return@withLock true
            }

            // 更新分类
            categories[categoryName] = category.copy(
                tags = category.tags + tagName
            )

            // 更新标签到分类的映射
            tagCategories[tagName] = categoryName

            true
        }
    }

    /**
     * 注册消息到线程。
     *
     * @param messageId 消息ID
     * @param threadId 线程ID
     */
    suspend fun registerMessageToThread(messageId: String, threadId: String) {
        mutex.withLock {
            val messages = threadMessages.getOrPut(threadId) { mutableListOf() }
            if (!messages.contains(messageId)) {
                messages.add(messageId)
            }
        }
    }
}
