package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Week 23-24: 协作学习分析相关数据模型
 */

// ==================== 分析结果模型 ====================

@Serializable
data class FinalCollaborationReport(
    val sessionId: SessionId,
    val sessionSummary: SessionSummary,
    val participantPerformance: Map<StudentId, ParticipantPerformance>,
    val groupEffectiveness: GroupEffectiveness,
    val learningAchievements: LearningAchievementSummary,
    val improvementAreas: List<ImprovementArea>,
    val successFactors: List<SuccessFactor>,
    val futureRecommendations: List<FutureRecommendation>,
    val generatedAt: Instant
)

@Serializable
data class SessionSummary(
    val title: String,
    val type: CollaborativeSessionType,
    val duration: Duration,
    val participantCount: Int,
    val activitiesCompleted: Int,
    val overallRating: Double
)

@Serializable
data class ParticipantPerformance(
    val participationScore: Double,
    val contributionQuality: Double,
    val collaborationEffectiveness: Double,
    val leadershipDemonstrated: Boolean,
    val areasOfStrength: List<String>,
    val areasForImprovement: List<String>
)

@Serializable
data class GroupEffectiveness(
    val cohesionLevel: Double,
    val communicationQuality: Double,
    val conflictManagement: Double,
    val leadershipDistribution: LeadershipDistributionEvaluation,
    val collaborationPatterns: List<CollaborationPattern>,
    val overallEffectiveness: Double
)

@Serializable
data class LeadershipDistributionEvaluation(
    val isBalanced: Boolean,
    val dominantLeader: StudentId?,
    val distributionScore: Double,
    val recommendation: String
)

@Serializable
data class LearningAchievementSummary(
    val objectivesAchieved: Int,
    val totalObjectives: Int,
    val averageSkillGain: Double,
    val knowledgeGainDistribution: KnowledgeGainDistribution,
    val collaborationSkillImprovement: Double
)

@Serializable
data class KnowledgeGainDistribution(
    val highGain: Int,      // >= 0.8
    val mediumGain: Int,    // 0.5-0.8
    val lowGain: Int,       // < 0.5
    val averageGain: Double
)

@Serializable
data class ImprovementArea(
    val area: String,
    val description: String,
    val affectedParticipants: List<StudentId>,
    val priority: RecommendationPriority,
    val suggestedActions: List<String>
)

@Serializable
data class SuccessFactor(
    val factor: String,
    val description: String,
    val contributingParticipants: List<StudentId>,
    val impact: String,
    val sustainabilityTips: List<String>
)

@Serializable
data class FutureRecommendation(
    val category: String,
    val recommendation: String,
    val rationale: String,
    val expectedBenefit: String,
    val implementationSteps: List<String>
)

// ==================== 实时分析模型 ====================

@Serializable
data class RealTimeAnalysis(
    val sessionId: SessionId,
    val currentEngagementLevel: EngagementLevel,
    val activeParticipants: List<StudentId>,
    val emergingPatterns: List<EmergingPattern>,
    val potentialIssues: List<PotentialIssue>,
    val interventionSuggestions: List<InterventionSuggestion>,
    val timestamp: Instant
)

@Serializable
data class EmergingPattern(
    val type: String,
    val description: String,
    val strength: Double,
    val participants: List<StudentId>
)

@Serializable
data class PotentialIssue(
    val type: String,
    val severity: IssueSeverity,
    val description: String,
    val affectedParticipants: List<StudentId>
)

@Serializable
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
data class InterventionSuggestion(
    val type: String,
    val urgency: InterventionUrgency,
    val description: String,
    val targetParticipants: List<StudentId>,
    val suggestedActions: List<String>
)

@Serializable
enum class InterventionUrgency {
    LOW,
    MEDIUM,
    HIGH,
    IMMEDIATE
}

// ==================== 分析器组件接口 ====================

/**
 * 交互分析器
 */
class InteractionAnalyzer {
    
    /**
     * 分析交互质量
     */
    fun analyzeInteractionQuality(interaction: CollaborativeInteraction): InteractionQuality {
        return InteractionQuality(
            relevance = calculateRelevance(interaction),
            constructiveness = calculateConstructiveness(interaction),
            clarity = calculateClarity(interaction),
            engagement = calculateEngagement(interaction),
            overallScore = 0.0 // 计算总分
        ).let { quality ->
            quality.copy(
                overallScore = (quality.relevance + quality.constructiveness + 
                               quality.clarity + quality.engagement) / 4.0
            )
        }
    }
    
    /**
     * 识别交互模式
     */
    fun identifyInteractionPatterns(interactions: List<CollaborativeInteraction>): List<InteractionPattern> {
        val patterns = mutableListOf<InteractionPattern>()
        
        // 分析问答模式
        val questionAnswerPairs = findQuestionAnswerPairs(interactions)
        if (questionAnswerPairs.isNotEmpty()) {
            patterns.add(
                InteractionPattern(
                    type = "问答互动",
                    frequency = questionAnswerPairs.size,
                    participants = questionAnswerPairs.flatMap { listOf(it.questioner, it.answerer) }.distinct(),
                    effectiveness = calculateQuestionAnswerEffectiveness(questionAnswerPairs)
                )
            )
        }
        
        // 分析协作编辑模式
        val editingSequences = findEditingSequences(interactions)
        if (editingSequences.isNotEmpty()) {
            patterns.add(
                InteractionPattern(
                    type = "协作编辑",
                    frequency = editingSequences.size,
                    participants = editingSequences.flatMap { it.participants }.distinct(),
                    effectiveness = calculateEditingEffectiveness(editingSequences)
                )
            )
        }
        
        return patterns
    }
    
    private fun calculateRelevance(interaction: CollaborativeInteraction): Double {
        // 简化实现：基于内容长度和类型
        val contentLength = interaction.content.text?.length ?: 0
        val typeRelevance = when (interaction.type) {
            InteractionType.QUESTION, InteractionType.ANSWER -> 0.9
            InteractionType.SHARE, InteractionType.COMMENT -> 0.8
            InteractionType.MESSAGE -> 0.7
            else -> 0.6
        }
        
        val lengthScore = minOf(1.0, contentLength / 100.0) // 假设100字符为满分
        return (typeRelevance + lengthScore) / 2.0
    }
    
    private fun calculateConstructiveness(interaction: CollaborativeInteraction): Double {
        // 简化实现：基于交互类型和响应数量
        val baseScore = when (interaction.type) {
            InteractionType.ANSWER, InteractionType.SHARE -> 0.9
            InteractionType.COMMENT -> 0.8
            InteractionType.QUESTION -> 0.7
            InteractionType.REACTION -> 0.5
            else -> 0.6
        }
        
        val responseBonus = minOf(0.2, interaction.responses.size * 0.05)
        return minOf(1.0, baseScore + responseBonus)
    }
    
    private fun calculateClarity(interaction: CollaborativeInteraction): Double {
        // 简化实现：基于内容结构
        val content = interaction.content.text ?: ""
        val hasStructure = content.contains("1.") || content.contains("-") || content.contains("•")
        val hasQuestions = content.contains("?")
        val hasExamples = content.contains("例如") || content.contains("比如")
        
        var score = 0.5
        if (hasStructure) score += 0.2
        if (hasQuestions) score += 0.15
        if (hasExamples) score += 0.15
        
        return minOf(1.0, score)
    }
    
    private fun calculateEngagement(interaction: CollaborativeInteraction): Double {
        // 基于提及、标签和响应
        val mentions = interaction.content.mentions.size
        val tags = interaction.content.tags.size
        val responses = interaction.responses.size
        
        return minOf(1.0, (mentions * 0.2 + tags * 0.1 + responses * 0.3) + 0.4)
    }
    
    private fun findQuestionAnswerPairs(interactions: List<CollaborativeInteraction>): List<QuestionAnswerPair> {
        val pairs = mutableListOf<QuestionAnswerPair>()
        val questions = interactions.filter { it.type == InteractionType.QUESTION }
        val answers = interactions.filter { it.type == InteractionType.ANSWER }
        
        for (question in questions) {
            val relatedAnswers = answers.filter { answer ->
                answer.timestamp > question.timestamp &&
                answer.timestamp <= question.timestamp + Duration.parse("PT30M") // 30分钟内
            }
            
            for (answer in relatedAnswers) {
                pairs.add(
                    QuestionAnswerPair(
                        questioner = question.participantId,
                        answerer = answer.participantId,
                        questionTime = question.timestamp,
                        answerTime = answer.timestamp,
                        responseTime = answer.timestamp - question.timestamp
                    )
                )
            }
        }
        
        return pairs
    }
    
    private fun findEditingSequences(interactions: List<CollaborativeInteraction>): List<EditingSequence> {
        val editInteractions = interactions.filter { it.type == InteractionType.EDIT }
        val sequences = mutableListOf<EditingSequence>()
        
        // 简化实现：将连续的编辑操作归为一个序列
        var currentSequence = mutableListOf<CollaborativeInteraction>()
        
        for (edit in editInteractions) {
            if (currentSequence.isEmpty() || 
                edit.timestamp - currentSequence.last().timestamp <= Duration.parse("PT5M")) {
                currentSequence.add(edit)
            } else {
                if (currentSequence.size > 1) {
                    sequences.add(
                        EditingSequence(
                            participants = currentSequence.map { it.participantId }.distinct(),
                            startTime = currentSequence.first().timestamp,
                            endTime = currentSequence.last().timestamp,
                            editCount = currentSequence.size
                        )
                    )
                }
                currentSequence = mutableListOf(edit)
            }
        }
        
        if (currentSequence.size > 1) {
            sequences.add(
                EditingSequence(
                    participants = currentSequence.map { it.participantId }.distinct(),
                    startTime = currentSequence.first().timestamp,
                    endTime = currentSequence.last().timestamp,
                    editCount = currentSequence.size
                )
            )
        }
        
        return sequences
    }
    
    private fun calculateQuestionAnswerEffectiveness(pairs: List<QuestionAnswerPair>): Double {
        if (pairs.isEmpty()) return 0.0
        
        val avgResponseTime = pairs.map { it.responseTime.inWholeMinutes }.average()
        val responseTimeScore = when {
            avgResponseTime <= 5 -> 1.0
            avgResponseTime <= 15 -> 0.8
            avgResponseTime <= 30 -> 0.6
            else -> 0.4
        }
        
        return responseTimeScore
    }
    
    private fun calculateEditingEffectiveness(sequences: List<EditingSequence>): Double {
        if (sequences.isEmpty()) return 0.0
        
        val avgParticipants = sequences.map { it.participants.size }.average()
        val collaborationScore = minOf(1.0, avgParticipants / 3.0) // 3人协作为满分
        
        return collaborationScore
    }
}

/**
 * 参与度跟踪器
 */
class ParticipationTracker {
    
    /**
     * 计算参与度指标
     */
    fun calculateMetrics(
        studentId: StudentId,
        sessionId: SessionId,
        sessionDuration: Duration,
        interactions: List<CollaborativeInteraction>
    ): ParticipationMetrics {
        val messageCount = interactions.count { it.type == InteractionType.MESSAGE }
        val activeTime = calculateActiveTime(interactions, sessionDuration)
        val contributionQuality = calculateContributionQuality(interactions)
        val engagementLevel = calculateEngagementLevel(interactions, sessionDuration)
        val leadershipMoments = countLeadershipMoments(interactions)
        val helpfulInteractions = countHelpfulInteractions(interactions)
        
        return ParticipationMetrics(
            messageCount = messageCount,
            activeTime = activeTime,
            contributionQuality = contributionQuality,
            engagementLevel = engagementLevel,
            leadershipMoments = leadershipMoments,
            helpfulInteractions = helpfulInteractions
        )
    }
    
    private fun calculateActiveTime(interactions: List<CollaborativeInteraction>, sessionDuration: Duration): Duration {
        if (interactions.isEmpty()) return Duration.ZERO
        
        // 简化计算：基于交互频率估算活跃时间
        val interactionSpan = if (interactions.size > 1) {
            interactions.last().timestamp - interactions.first().timestamp
        } else {
            Duration.parse("PT5M") // 默认5分钟
        }
        
        val activityRatio = interactions.size / 10.0 // 假设10个交互为高活跃
        return Duration.parse("PT${(sessionDuration.inWholeMinutes * minOf(1.0, activityRatio)).toLong()}M")
    }
    
    private fun calculateContributionQuality(interactions: List<CollaborativeInteraction>): Double {
        if (interactions.isEmpty()) return 0.0
        
        val qualityScores = interactions.map { interaction ->
            when (interaction.type) {
                InteractionType.ANSWER -> 0.9
                InteractionType.SHARE -> 0.8
                InteractionType.QUESTION -> 0.7
                InteractionType.COMMENT -> 0.6
                InteractionType.MESSAGE -> 0.5
                else -> 0.4
            }
        }
        
        return qualityScores.average()
    }
    
    private fun calculateEngagementLevel(interactions: List<CollaborativeInteraction>, sessionDuration: Duration): EngagementLevel {
        if (interactions.isEmpty()) return EngagementLevel.LOW
        
        val interactionRate = interactions.size.toDouble() / sessionDuration.inWholeHours.coerceAtLeast(1)
        
        return when {
            interactionRate >= 15 -> EngagementLevel.VERY_HIGH
            interactionRate >= 10 -> EngagementLevel.HIGH
            interactionRate >= 5 -> EngagementLevel.MEDIUM
            interactionRate >= 2 -> EngagementLevel.LOW
            else -> EngagementLevel.VERY_LOW
        }
    }
    
    private fun countLeadershipMoments(interactions: List<CollaborativeInteraction>): Int {
        // 识别领导行为：提问、分享、指导等
        return interactions.count { interaction ->
            when (interaction.type) {
                InteractionType.QUESTION -> interaction.content.text?.contains("大家") == true ||
                                          interaction.content.text?.contains("我们") == true
                InteractionType.SHARE -> true
                InteractionType.COMMENT -> interaction.content.text?.contains("建议") == true ||
                                         interaction.content.text?.contains("应该") == true
                else -> false
            }
        }
    }
    
    private fun countHelpfulInteractions(interactions: List<CollaborativeInteraction>): Int {
        return interactions.count { interaction ->
            interaction.responses.any { response ->
                response.type == ResponseType.HELPFUL || response.type == ResponseType.LIKE
            }
        }
    }
}

// ==================== 支持数据类 ====================

@Serializable
data class InteractionQuality(
    val relevance: Double,
    val constructiveness: Double,
    val clarity: Double,
    val engagement: Double,
    val overallScore: Double
)

@Serializable
data class InteractionPattern(
    val type: String,
    val frequency: Int,
    val participants: List<StudentId>,
    val effectiveness: Double
)

@Serializable
data class QuestionAnswerPair(
    val questioner: StudentId,
    val answerer: StudentId,
    val questionTime: Instant,
    val answerTime: Instant,
    val responseTime: Duration
)

@Serializable
data class EditingSequence(
    val participants: List<StudentId>,
    val startTime: Instant,
    val endTime: Instant,
    val editCount: Int
)
