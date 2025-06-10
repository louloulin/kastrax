package ai.kastrax.edutech.content

import ai.kastrax.edutech.models.*
import ai.kastrax.rag.RAG
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.math.min

/**
 * 内容管理服务 - 实现ed2.md第一阶段Week 3-4内容管理基础功能
 * 
 * 提供内容上传和存储、内容元数据管理、基础搜索功能和内容版本控制
 */
class ContentManagementService(
    private val contentRepository: ContentRepository,
    private val ragSystem: RAG
) {
    private val mutex = Mutex()
    
    /**
     * 创建内容
     *
     * @param content 内容对象
     * @param creatorId 创建者ID
     * @return 创建结果
     */
    suspend fun createContent(content: LearningContent, creatorId: String): ContentResult {
        return try {
            // 生成内容ID
            val contentId = ContentId.generate()
            
            // 设置创建时间和版本
            val newContent = content.copy(
                id = contentId,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                createdBy = creatorId,
                version = 1
            )
            
            // 保存内容
            contentRepository.saveContent(newContent)
            
            // 索引内容到RAG系统
            indexContentToRag(newContent)
            
            ContentResult.Success(newContent)
        } catch (e: Exception) {
            ContentResult.Failure("创建内容失败: ${e.message}")
        }
    }
    
    /**
     * 更新内容
     *
     * @param contentId 内容ID
     * @param updatedContent 更新后的内容
     * @param updaterId 更新者ID
     * @return 更新结果
     */
    suspend fun updateContent(
        contentId: ContentId,
        updatedContent: LearningContent,
        updaterId: String
    ): ContentResult {
        return try {
            // 获取当前内容
            val currentContent = contentRepository.getContent(contentId)
                ?: return ContentResult.Failure("内容不存在")
            
            // 创建新版本
            val newVersion = currentContent.version + 1
            
            // 更新内容
            val newContent = updatedContent.copy(
                id = contentId,
                updatedAt = Clock.System.now(),
                updatedBy = updaterId,
                version = newVersion
            )
            
            // 保存内容
            contentRepository.saveContent(newContent)
            
            // 保存历史版本
            contentRepository.saveContentVersion(currentContent)
            
            // 更新RAG索引
            indexContentToRag(newContent)
            
            ContentResult.Success(newContent)
        } catch (e: Exception) {
            ContentResult.Failure("更新内容失败: ${e.message}")
        }
    }
    
    /**
     * 获取内容
     *
     * @param contentId 内容ID
     * @return 内容获取结果
     */
    suspend fun getContent(contentId: ContentId): ContentResult {
        return try {
            val content = contentRepository.getContent(contentId)
                ?: return ContentResult.Failure("内容不存在")
            
            ContentResult.Success(content)
        } catch (e: Exception) {
            ContentResult.Failure("获取内容失败: ${e.message}")
        }
    }
    
    /**
     * 获取内容历史版本
     *
     * @param contentId 内容ID
     * @param version 版本号 (可选，默认获取所有版本)
     * @return 内容版本结果
     */
    suspend fun getContentVersions(
        contentId: ContentId,
        version: Int? = null
    ): ContentVersionResult {
        return try {
            if (version != null) {
                val contentVersion = contentRepository.getContentVersion(contentId, version)
                    ?: return ContentVersionResult.Failure("指定版本不存在")
                
                ContentVersionResult.Success(listOf(contentVersion))
            } else {
                val versions = contentRepository.getContentVersions(contentId)
                ContentVersionResult.Success(versions)
            }
        } catch (e: Exception) {
            ContentVersionResult.Failure("获取内容版本失败: ${e.message}")
        }
    }
    
    /**
     * 删除内容
     *
     * @param contentId 内容ID
     * @return 删除结果
     */
    suspend fun deleteContent(contentId: ContentId): ContentOperationResult {
        return try {
            val content = contentRepository.getContent(contentId)
                ?: return ContentOperationResult.Failure("内容不存在")
            
            // 删除内容
            contentRepository.deleteContent(contentId)
            
            // 从RAG系统中移除索引
            removeContentFromRag(content)
            
            ContentOperationResult.Success("内容已成功删除")
        } catch (e: Exception) {
            ContentOperationResult.Failure("删除内容失败: ${e.message}")
        }
    }
    
    /**
     * 搜索内容
     *
     * @param query 搜索查询
     * @param filters 过滤条件
     * @param limit 结果限制
     * @return 搜索结果
     */
    suspend fun searchContent(
        query: String,
        filters: ContentFilters = ContentFilters(),
        limit: Int = 10
    ): ContentSearchResult {
        return try {
            // 使用RAG系统进行语义搜索
            val ragResults = ragSystem.search(query, limit)
            
            // 简化实现：直接从仓库搜索内容
            val allContents = contentRepository.listContents(filters, limit, 0)

            // 基于查询进行简单的文本匹配
            val filteredContents = if (query.isNotBlank()) {
                allContents.filter { content ->
                    content.title.contains(query, ignoreCase = true) ||
                    content.description.contains(query, ignoreCase = true) ||
                    content.content.contains(query, ignoreCase = true) ||
                    content.tags.any { it.contains(query, ignoreCase = true) }
                }
            } else {
                allContents
            }

            ContentSearchResult.Success(
                results = filteredContents.map { content ->
                    ContentSearchItem(content, 1.0) // 简化评分
                }
            )
        } catch (e: Exception) {
            ContentSearchResult.Failure("搜索内容失败: ${e.message}")
        }
    }
    
    /**
     * 获取内容元数据
     *
     * @param contentId 内容ID
     * @return 元数据结果
     */
    suspend fun getContentMetadata(contentId: ContentId): ContentMetadataResult {
        return try {
            val content = contentRepository.getContent(contentId)
                ?: return ContentMetadataResult.Failure("内容不存在")
            
            val metadata = ContentMetadata(
                id = content.id,
                title = content.title,
                type = content.type,
                subject = content.subject,
                difficulty = content.difficulty,
                tags = content.tags,
                createdAt = content.createdAt,
                updatedAt = content.updatedAt,
                createdBy = content.createdBy,
                updatedBy = content.updatedBy,
                version = content.version,
                size = content.content.length.toLong()
            )
            
            ContentMetadataResult.Success(metadata)
        } catch (e: Exception) {
            ContentMetadataResult.Failure("获取内容元数据失败: ${e.message}")
        }
    }
    
    /**
     * 将内容索引到RAG系统
     *
     * @param content 内容对象
     */
    private suspend fun indexContentToRag(content: LearningContent) {
        // 创建文档内容
        val documentContent = "${content.title}\n\n${content.description}\n\n${content.content}"

        // 索引到RAG系统 (简化实现)
        // 在实际实现中，这里应该调用RAG系统的索引方法
        // ragSystem.index(documentContent, metadata)
    }
    
    /**
     * 从RAG系统中移除内容索引
     *
     * @param content 内容对象
     */
    private suspend fun removeContentFromRag(content: LearningContent) {
        // 从RAG系统中删除索引 (简化实现)
        // 在实际实现中，这里应该调用RAG系统的删除方法
        // ragSystem.delete("content_${content.id.value}")
    }
}

/**
 * 内容仓库接口
 */
interface ContentRepository {
    suspend fun saveContent(content: LearningContent)
    suspend fun getContent(contentId: ContentId): LearningContent?
    suspend fun deleteContent(contentId: ContentId)
    suspend fun saveContentVersion(content: LearningContent)
    suspend fun getContentVersion(contentId: ContentId, version: Int): LearningContent?
    suspend fun getContentVersions(contentId: ContentId): List<LearningContent>
    suspend fun listContents(filters: ContentFilters, limit: Int, offset: Int): List<LearningContent>
}

/**
 * 内容过滤器
 */
@Serializable
data class ContentFilters(
    val contentTypes: Set<ContentType> = emptySet(),
    val subjects: Set<Subject> = emptySet(),
    val difficultyLevels: Set<DifficultyLevel> = emptySet(),
    val tags: Set<String> = emptySet(),
    val createdAfter: kotlinx.datetime.Instant? = null,
    val createdBefore: kotlinx.datetime.Instant? = null,
    val createdBy: String? = null
)

/**
 * 内容元数据
 */
@Serializable
data class ContentMetadata(
    val id: ContentId,
    val title: String,
    val type: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val tags: List<String>,
    val createdAt: kotlinx.datetime.Instant,
    val updatedAt: kotlinx.datetime.Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Int,
    val size: Long
)

/**
 * 内容搜索项
 */
@Serializable
data class ContentSearchItem(
    val content: LearningContent,
    val relevanceScore: Double
)

/**
 * 内容结果
 */
sealed class ContentResult {
    data class Success(val content: LearningContent) : ContentResult()
    data class Failure(val error: String) : ContentResult()
}

/**
 * 内容版本结果
 */
sealed class ContentVersionResult {
    data class Success(val versions: List<LearningContent>) : ContentVersionResult()
    data class Failure(val error: String) : ContentVersionResult()
}

/**
 * 内容操作结果
 */
sealed class ContentOperationResult {
    data class Success(val message: String) : ContentOperationResult()
    data class Failure(val error: String) : ContentOperationResult()
}

/**
 * 内容元数据结果
 */
sealed class ContentMetadataResult {
    data class Success(val metadata: ContentMetadata) : ContentMetadataResult()
    data class Failure(val error: String) : ContentMetadataResult()
}

/**
 * 内容搜索结果
 */
sealed class ContentSearchResult {
    data class Success(val results: List<ContentSearchItem>) : ContentSearchResult()
    data class Failure(val error: String) : ContentSearchResult()
}
