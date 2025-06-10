package ai.kastrax.edutech.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*

/**
 * 内容ID
 */
@Serializable
data class ContentId(val value: String) {
    companion object {
        fun generate(): ContentId = ContentId("content_${UUID.randomUUID()}")
    }
}

/**
 * 学习内容
 */
@Serializable
data class LearningContent(
    val id: ContentId = ContentId.generate(),
    val title: String,
    val description: String,
    val content: String,
    val type: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val estimatedDuration: Int, // 分钟
    val learningObjectives: List<String>,
    val prerequisites: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Int = 1,
    val status: ContentStatus = ContentStatus.DRAFT
) {
    /**
     * 创建内容摘要
     *
     * @param maxLength 最大长度
     * @return 内容摘要
     */
    fun createSummary(maxLength: Int = 200): String {
        val contentText = content.replace(Regex("<.*?>"), "")
        return if (contentText.length <= maxLength) {
            contentText
        } else {
            "${contentText.substring(0, maxLength)}..."
        }
    }
    
    /**
     * 获取内容大小
     *
     * @return 内容大小（字节）
     */
    fun getSize(): Long {
        return content.length.toLong()
    }
    
    /**
     * 检查内容是否包含关键词
     *
     * @param keywords 关键词列表
     * @return 是否包含关键词
     */
    fun containsKeywords(keywords: List<String>): Boolean {
        val contentText = content.lowercase()
        return keywords.any { keyword ->
            contentText.contains(keyword.lowercase()) ||
            title.lowercase().contains(keyword.lowercase()) ||
            description.lowercase().contains(keyword.lowercase()) ||
            tags.any { it.lowercase().contains(keyword.lowercase()) }
        }
    }
    
    /**
     * 检查内容是否适合指定的学习风格
     *
     * @param learningStyle 学习风格
     * @return 是否适合
     */
    fun isSuitableForLearningStyle(learningStyle: LearningStyle): Boolean {
        return when (learningStyle) {
            LearningStyle.VISUAL -> type in listOf(ContentType.VIDEO, ContentType.IMAGE, ContentType.INTERACTIVE)
            LearningStyle.AUDITORY -> type in listOf(ContentType.AUDIO, ContentType.VIDEO)
            LearningStyle.KINESTHETIC -> type in listOf(ContentType.INTERACTIVE, ContentType.SIMULATION, ContentType.GAME)
            LearningStyle.READING_WRITING -> type in listOf(ContentType.TEXT, ContentType.DOCUMENT)
            LearningStyle.BALANCED -> true
        }
    }
    
    /**
     * 检查内容是否适合指定的难度级别
     *
     * @param targetDifficulty 目标难度级别
     * @param allowAdjacent 是否允许相邻难度
     * @return 是否适合
     */
    fun isSuitableForDifficulty(
        targetDifficulty: DifficultyLevel,
        allowAdjacent: Boolean = true
    ): Boolean {
        return if (allowAdjacent) {
            val difficultyOrdinal = difficulty.ordinal
            val targetOrdinal = targetDifficulty.ordinal
            Math.abs(difficultyOrdinal - targetOrdinal) <= 1
        } else {
            difficulty == targetDifficulty
        }
    }
}

/**
 * 内容状态
 */
enum class ContentStatus {
    DRAFT,      // 草稿
    REVIEW,     // 审核中
    PUBLISHED,  // 已发布
    ARCHIVED,   // 已归档
    DELETED     // 已删除
}

/**
 * 内容评分
 */
@Serializable
data class ContentRating(
    val contentId: ContentId,
    val userId: String,
    val rating: Int, // 1-5
    val comment: String? = null,
    val timestamp: Instant = Clock.System.now()
)

/**
 * 内容使用统计
 */
@Serializable
data class ContentUsageStatistics(
    val contentId: ContentId,
    val viewCount: Int = 0,
    val completionCount: Int = 0,
    val averageRating: Double = 0.0,
    val averageCompletionTime: Double = 0.0, // 分钟
    val popularityScore: Double = 0.0,
    val lastUpdated: Instant = Clock.System.now()
)

/**
 * 内容集合
 */
@Serializable
data class ContentCollection(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val contentIds: List<ContentId>,
    val createdBy: String,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

/**
 * 内容推荐
 */
@Serializable
data class ContentRecommendation(
    val contentId: ContentId,
    val score: Double,
    val reason: String,
    val timestamp: Instant = Clock.System.now()
)

/**
 * 内容关系类型
 */
enum class ContentRelationType {
    PREREQUISITE,    // 前置内容
    NEXT,            // 后续内容
    RELATED,         // 相关内容
    ALTERNATIVE,     // 替代内容
    SUPPLEMENT       // 补充内容
}

/**
 * 内容关系
 */
@Serializable
data class ContentRelation(
    val sourceContentId: ContentId,
    val targetContentId: ContentId,
    val relationType: ContentRelationType,
    val strength: Double = 1.0, // 关系强度 0.0-1.0
    val metadata: Map<String, String> = emptyMap()
)
