package ai.kastrax.edutech.generation

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*

/**
 * 生成内容ID
 */
@Serializable
@JvmInline
value class GeneratedContentId(val value: String) {
    companion object {
        fun generate(): GeneratedContentId = GeneratedContentId("gen_content_${UUID.randomUUID()}")
    }
}

/**
 * 内容生成请求
 */
@Serializable
data class ContentGenerationRequest(
    val requestId: String = "req_${UUID.randomUUID()}",
    val contentType: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val topic: Topic,
    val learningObjectives: List<String>,
    val targetAudience: String = "学生",
    val parameters: Map<String, String> = emptyMap(),
    val constraints: GenerationConstraints = GenerationConstraints(),
    val requestedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 生成约束条件
 */
@Serializable
data class GenerationConstraints(
    val maxLength: Int = 2000,
    val minLength: Int = 100,
    val includeExamples: Boolean = true,
    val includeQuestions: Boolean = false,
    val language: String = "zh-CN",
    val tone: ContentTone = ContentTone.EDUCATIONAL,
    val complexity: ContentComplexity = ContentComplexity.MODERATE
)

/**
 * 内容语调
 */
enum class ContentTone {
    FORMAL,         // 正式
    CASUAL,         // 随意
    EDUCATIONAL,    // 教育性
    ENCOURAGING,    // 鼓励性
    PROFESSIONAL    // 专业
}

/**
 * 内容复杂度
 */
enum class ContentComplexity {
    SIMPLE,         // 简单
    MODERATE,       // 中等
    COMPLEX,        // 复杂
    ADVANCED        // 高级
}

/**
 * 生成的内容
 */
@Serializable
data class GeneratedContent(
    val id: GeneratedContentId,
    val requestId: String,
    val contentType: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val topic: Topic,
    val title: String,
    val content: String,
    val learningObjectives: List<String>,
    val estimatedDuration: Int, // 分钟
    val generatedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList()
)

/**
 * 内容模态
 */
enum class ContentModality {
    TEXT,           // 文本
    IMAGE,          // 图像
    AUDIO,          // 音频
    VIDEO,          // 视频
    INTERACTIVE     // 交互式
}

/**
 * 多模态生成请求
 */
@Serializable
data class MultimodalGenerationRequest(
    val requestId: String = "multimodal_${UUID.randomUUID()}",
    val contentType: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val topic: Topic,
    val learningObjectives: List<String>,
    val modalities: Set<ContentModality>,
    val targetAudience: String = "学生",
    val parameters: Map<String, String> = emptyMap(),
    val requestedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 内容质量评分
 */
@Serializable
data class ContentQualityScore(
    val overallScore: Double, // 0.0 - 1.0
    val accuracyScore: Double,
    val clarityScore: Double,
    val relevanceScore: Double,
    val engagementScore: Double,
    val educationalValueScore: Double,
    val feedback: String,
    val suggestions: List<String> = emptyList(),
    val assessedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 内容模板
 */
@Serializable
data class ContentTemplate(
    val id: String,
    val name: String,
    val contentType: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val promptTemplate: String,
    val parameters: Map<String, String> = emptyMap(),
    val createdAt: Instant = kotlinx.datetime.Clock.System.now()
) {
    companion object {
        fun getDefault(contentType: ContentType): ContentTemplate {
            return when (contentType) {
                ContentType.TEXT -> ContentTemplate(
                    id = "default_text",
                    name = "默认文本模板",
                    contentType = ContentType.TEXT,
                    subject = Subject.GENERAL,
                    difficulty = DifficultyLevel.INTERMEDIATE,
                    promptTemplate = """
                        请为以下主题创建一个教育性的文本内容：
                        主题: {topic}
                        学习目标: {objectives}
                        难度级别: {difficulty}
                        目标受众: {audience}
                        
                        内容应该：
                        1. 清晰易懂
                        2. 结构化组织
                        3. 包含实例说明
                        4. 适合目标受众
                        
                        请生成内容：
                    """.trimIndent()
                )
                ContentType.VIDEO -> ContentTemplate(
                    id = "default_video",
                    name = "默认视频模板",
                    contentType = ContentType.VIDEO,
                    subject = Subject.GENERAL,
                    difficulty = DifficultyLevel.INTERMEDIATE,
                    promptTemplate = """
                        请为以下主题创建一个视频脚本：
                        主题: {topic}
                        学习目标: {objectives}
                        难度级别: {difficulty}
                        目标受众: {audience}
                        
                        视频脚本应包括：
                        1. 引人入胜的开场
                        2. 清晰的内容讲解
                        3. 视觉提示和说明
                        4. 总结和要点回顾
                        
                        请生成脚本：
                    """.trimIndent()
                )
                else -> ContentTemplate(
                    id = "default_generic",
                    name = "默认通用模板",
                    contentType = contentType,
                    subject = Subject.GENERAL,
                    difficulty = DifficultyLevel.INTERMEDIATE,
                    promptTemplate = """
                        请为以下主题创建教育内容：
                        主题: {topic}
                        学习目标: {objectives}
                        难度级别: {difficulty}
                        目标受众: {audience}
                        
                        请生成适合的内容：
                    """.trimIndent()
                )
            }
        }
    }
    
    /**
     * 构建提示词
     */
    fun buildPrompt(
        topic: String,
        objectives: List<String>,
        difficulty: String,
        audience: String,
        parameters: Map<String, String> = emptyMap()
    ): String {
        var prompt = promptTemplate
            .replace("{topic}", topic)
            .replace("{objectives}", objectives.joinToString(", "))
            .replace("{difficulty}", difficulty)
            .replace("{audience}", audience)
        
        // 应用额外参数
        parameters.forEach { (key, value) ->
            prompt = prompt.replace("{$key}", value.toString())
        }
        
        return prompt
    }
}

/**
 * 生成反馈
 */
@Serializable
data class GenerationFeedback(
    val contentId: GeneratedContentId,
    val rating: Int, // 1-5
    val positiveAspects: List<String>,
    val improvementSuggestions: List<String>,
    val overallComment: String,
    val providedBy: String,
    val providedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 反馈分析
 */
@Serializable
data class FeedbackAnalysis(
    val averageRating: Double,
    val positiveAspects: List<String>,
    val improvementAreas: List<String>,
    val confidenceScore: Double
)

// 结果类型定义

/**
 * 内容生成结果
 */
sealed class ContentGenerationResult {
    data class Success(
        val content: GeneratedContent,
        val qualityScore: ContentQualityScore,
        val generationTime: Instant
    ) : ContentGenerationResult()
    
    data class Failure(val error: String) : ContentGenerationResult()
}

/**
 * 批量生成结果
 */
sealed class BatchGenerationResult {
    data class Success(
        val results: List<ContentGenerationResult>,
        val successCount: Int,
        val failureCount: Int,
        val totalCount: Int
    ) : BatchGenerationResult()
    
    data class Failure(val error: String) : BatchGenerationResult()
}

/**
 * 多模态生成结果
 */
sealed class MultimodalGenerationResult {
    data class Success(
        val components: Map<ContentModality, GeneratedContent>,
        val generatedAt: Instant
    ) : MultimodalGenerationResult()
    
    data class Failure(val error: String) : MultimodalGenerationResult()
}

/**
 * 参数优化结果
 */
sealed class ParameterOptimizationResult {
    data class Success(
        val optimizedParameters: Map<String, String>,
        val improvementAreas: List<String>,
        val confidenceScore: Double
    ) : ParameterOptimizationResult()

    data class Failure(val error: String) : ParameterOptimizationResult()
}

/**
 * 内容模板仓库接口
 */
interface ContentTemplateRepository {
    suspend fun findBestTemplate(
        contentType: ContentType,
        subject: Subject,
        difficulty: DifficultyLevel
    ): ContentTemplate?
    
    suspend fun saveTemplate(template: ContentTemplate)
    suspend fun getAllTemplates(): List<ContentTemplate>
    suspend fun getTemplateById(id: String): ContentTemplate?
    suspend fun deleteTemplate(id: String)
}

/**
 * 内容质量评估接口
 */
interface ContentQualityAssessment {
    suspend fun assessContent(content: GeneratedContent): ContentQualityScore
    suspend fun assessBatch(contents: List<GeneratedContent>): List<ContentQualityScore>
    suspend fun getQualityMetrics(): QualityMetrics
}

/**
 * 质量指标
 */
@Serializable
data class QualityMetrics(
    val averageQualityScore: Double,
    val totalAssessments: Int,
    val highQualityCount: Int, // 评分 >= 0.8
    val lowQualityCount: Int,  // 评分 < 0.5
    val improvementTrends: List<QualityTrend>
)

/**
 * 质量趋势
 */
@Serializable
data class QualityTrend(
    val date: kotlinx.datetime.LocalDate,
    val averageScore: Double,
    val assessmentCount: Int
)
