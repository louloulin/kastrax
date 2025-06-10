package ai.kastrax.edutech.content

import ai.kastrax.edutech.models.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存内容仓库实现 - 用于开发和测试
 * 
 * 在生产环境中应该替换为基于数据库的实现
 */
class InMemoryContentRepository : ContentRepository {
    private val contents = mutableMapOf<ContentId, LearningContent>()
    private val contentVersions = mutableMapOf<ContentId, MutableList<LearningContent>>()
    private val mutex = Mutex()
    
    override suspend fun saveContent(content: LearningContent) {
        mutex.withLock {
            contents[content.id] = content
        }
    }
    
    override suspend fun getContent(contentId: ContentId): LearningContent? {
        return mutex.withLock {
            contents[contentId]
        }
    }
    
    override suspend fun deleteContent(contentId: ContentId) {
        mutex.withLock {
            contents.remove(contentId)
            contentVersions.remove(contentId)
        }
    }
    
    override suspend fun saveContentVersion(content: LearningContent) {
        mutex.withLock {
            val versions = contentVersions.getOrPut(content.id) { mutableListOf() }
            versions.add(content)
        }
    }
    
    override suspend fun getContentVersion(contentId: ContentId, version: Int): LearningContent? {
        return mutex.withLock {
            contentVersions[contentId]?.find { it.version == version }
        }
    }
    
    override suspend fun getContentVersions(contentId: ContentId): List<LearningContent> {
        return mutex.withLock {
            contentVersions[contentId]?.toList() ?: emptyList()
        }
    }
    
    override suspend fun listContents(
        filters: ContentFilters,
        limit: Int,
        offset: Int
    ): List<LearningContent> {
        return mutex.withLock {
            contents.values
                .filter { content ->
                    (filters.contentTypes.isEmpty() || content.type in filters.contentTypes) &&
                    (filters.subjects.isEmpty() || content.subject in filters.subjects) &&
                    (filters.difficultyLevels.isEmpty() || content.difficulty in filters.difficultyLevels) &&
                    (filters.tags.isEmpty() || content.tags.any { it in filters.tags }) &&
                    (filters.createdAfter == null || content.createdAt > filters.createdAfter) &&
                    (filters.createdBefore == null || content.createdAt < filters.createdBefore) &&
                    (filters.createdBy == null || content.createdBy == filters.createdBy)
                }
                .drop(offset)
                .take(limit)
        }
    }
}
