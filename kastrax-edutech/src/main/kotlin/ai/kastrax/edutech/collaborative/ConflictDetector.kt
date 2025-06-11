package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Week 23-24: 冲突检测器
 * 
 * 功能：
 * - 检测协作中的冲突指标
 * - 分析冲突类型和严重程度
 * - 提供冲突解决建议
 * - 预测潜在冲突风险
 */
class ConflictDetector {
    
    /**
     * 检测冲突指标
     */
    fun detectConflictIndicators(interactions: List<CollaborativeInteraction>): List<ConflictIndicator> {
        val indicators = mutableListOf<ConflictIndicator>()
        
        // 1. 检测直接冲突语言
        indicators.addAll(detectDirectConflictLanguage(interactions))
        
        // 2. 检测消极响应模式
        indicators.addAll(detectNegativeResponsePatterns(interactions))
        
        // 3. 检测沟通中断
        indicators.addAll(detectCommunicationBreakdowns(interactions))
        
        // 4. 检测参与度急剧下降
        indicators.addAll(detectEngagementDrops(interactions))
        
        // 5. 检测重复争议
        indicators.addAll(detectRepeatedDisputes(interactions))
        
        return indicators
    }
    
    /**
     * 分析冲突类型
     */
    fun analyzeConflictTypes(indicators: List<ConflictIndicator>): ConflictTypeAnalysis {
        val typeDistribution = indicators.groupBy { it.type }.mapValues { it.value.size }
        
        return ConflictTypeAnalysis(
            taskConflicts = typeDistribution[ConflictType.TASK] ?: 0,
            processConflicts = typeDistribution[ConflictType.PROCESS] ?: 0,
            relationshipConflicts = typeDistribution[ConflictType.RELATIONSHIP] ?: 0,
            dominantConflictType = typeDistribution.maxByOrNull { it.value }?.key ?: ConflictType.TASK,
            overallSeverity = calculateOverallSeverity(indicators)
        )
    }
    
    /**
     * 预测冲突风险
     */
    fun predictConflictRisk(
        interactions: List<CollaborativeInteraction>,
        participants: List<SessionParticipant>
    ): ConflictRiskAssessment {
        val riskFactors = identifyRiskFactors(interactions, participants)
        val riskScore = calculateRiskScore(riskFactors)
        
        return ConflictRiskAssessment(
            riskLevel = categorizeRiskLevel(riskScore),
            riskScore = riskScore,
            riskFactors = riskFactors,
            preventionRecommendations = generatePreventionRecommendations(riskFactors),
            monitoringPoints = identifyMonitoringPoints(riskFactors)
        )
    }
    
    // ==================== 私有检测方法 ====================
    
    private fun detectDirectConflictLanguage(interactions: List<CollaborativeInteraction>): List<ConflictIndicator> {
        val indicators = mutableListOf<ConflictIndicator>()
        
        val conflictKeywords = mapOf(
            "不同意" to 0.8,
            "反对" to 0.9,
            "错误" to 0.7,
            "不对" to 0.6,
            "问题" to 0.5,
            "不行" to 0.7,
            "不可能" to 0.8,
            "荒谬" to 0.9,
            "愚蠢" to 1.0
        )
        
        for (interaction in interactions) {
            val content = interaction.content.text?.lowercase() ?: ""
            
            for ((keyword, severity) in conflictKeywords) {
                if (content.contains(keyword)) {
                    indicators.add(
                        ConflictIndicator(
                            id = "conflict_${interaction.id}_$keyword",
                            type = ConflictType.TASK,
                            severity = severity,
                            description = "检测到冲突语言: $keyword",
                            involvedParticipants = listOf(interaction.participantId),
                            timestamp = interaction.timestamp,
                            evidence = content.take(100),
                            resolutionSuggestions = listOf("鼓励建设性讨论", "重新表述观点", "寻找共同点")
                        )
                    )
                }
            }
        }
        
        return indicators
    }
    
    private fun detectNegativeResponsePatterns(interactions: List<CollaborativeInteraction>): List<ConflictIndicator> {
        val indicators = mutableListOf<ConflictIndicator>()
        
        for (interaction in interactions) {
            val negativeResponses = interaction.responses.filter { response ->
                response.type in setOf(ResponseType.DISAGREE, ResponseType.DISLIKE, ResponseType.UNCLEAR)
            }
            
            if (negativeResponses.size > interaction.responses.size * 0.6) {
                indicators.add(
                    ConflictIndicator(
                        id = "negative_pattern_${interaction.id}",
                        type = ConflictType.RELATIONSHIP,
                        severity = 0.7,
                        description = "检测到消极响应模式",
                        involvedParticipants = negativeResponses.map { it.participantId },
                        timestamp = interaction.timestamp,
                        evidence = "消极响应比例: ${negativeResponses.size}/${interaction.responses.size}",
                        resolutionSuggestions = listOf("促进积极交流", "澄清误解", "建立信任")
                    )
                )
            }
        }
        
        return indicators
    }
    
    private fun detectCommunicationBreakdowns(interactions: List<CollaborativeInteraction>): List<ConflictIndicator> {
        val indicators = mutableListOf<ConflictIndicator>()
        
        // 检测长时间无响应的问题
        val questions = interactions.filter { it.type == InteractionType.QUESTION }
        
        for (question in questions) {
            val responses = interactions.filter { interaction ->
                interaction.timestamp > question.timestamp &&
                interaction.timestamp <= question.timestamp + Duration.parse("PT60M") &&
                interaction.type == InteractionType.ANSWER
            }
            
            if (responses.isEmpty()) {
                indicators.add(
                    ConflictIndicator(
                        id = "no_response_${question.id}",
                        type = ConflictType.PROCESS,
                        severity = 0.6,
                        description = "问题未得到回应",
                        involvedParticipants = listOf(question.participantId),
                        timestamp = question.timestamp,
                        evidence = "问题: ${question.content.text?.take(50)}",
                        resolutionSuggestions = listOf("鼓励回应", "重新提问", "分配回答责任")
                    )
                )
            }
        }
        
        return indicators
    }
    
    private fun detectEngagementDrops(interactions: List<CollaborativeInteraction>): List<ConflictIndicator> {
        val indicators = mutableListOf<ConflictIndicator>()
        
        // 按时间窗口分析参与度变化
        val timeWindows = groupInteractionsByTimeWindow(interactions, 15) // 15分钟窗口
        val participantActivity = mutableMapOf<StudentId, MutableList<Int>>()
        
        for ((_, windowInteractions) in timeWindows) {
            val activityCount = windowInteractions.groupBy { it.participantId }.mapValues { it.value.size }
            
            for (participant in windowInteractions.map { it.participantId }.distinct()) {
                participantActivity.getOrPut(participant) { mutableListOf() }
                    .add(activityCount[participant] ?: 0)
            }
        }
        
        // 检测活动度急剧下降
        for ((participant, activities) in participantActivity) {
            if (activities.size >= 3) {
                val recentAvg = activities.takeLast(2).average()
                val earlierAvg = activities.dropLast(2).average()
                
                if (earlierAvg > 0 && recentAvg / earlierAvg < 0.3) {
                    indicators.add(
                        ConflictIndicator(
                            id = "engagement_drop_$participant",
                            type = ConflictType.RELATIONSHIP,
                            severity = 0.7,
                            description = "参与度急剧下降",
                            involvedParticipants = listOf(participant),
                            timestamp = Clock.System.now(),
                            evidence = "活动度从 ${earlierAvg.format(1)} 降至 ${recentAvg.format(1)}",
                            resolutionSuggestions = listOf("私下沟通", "重新激发兴趣", "调整角色")
                        )
                    )
                }
            }
        }
        
        return indicators
    }
    
    private fun detectRepeatedDisputes(interactions: List<CollaborativeInteraction>): List<ConflictIndicator> {
        val indicators = mutableListOf<ConflictIndicator>()
        
        // 检测重复的争议话题
        val disputePatterns = mutableMapOf<Pair<StudentId, StudentId>, MutableList<CollaborativeInteraction>>()
        
        for (interaction in interactions) {
            for (response in interaction.responses) {
                if (response.type == ResponseType.DISAGREE) {
                    val pair = Pair(interaction.participantId, response.participantId)
                    disputePatterns.getOrPut(pair) { mutableListOf() }.add(interaction)
                }
            }
        }
        
        for ((participantPair, disputes) in disputePatterns) {
            if (disputes.size >= 3) {
                indicators.add(
                    ConflictIndicator(
                        id = "repeated_dispute_${participantPair.first}_${participantPair.second}",
                        type = ConflictType.RELATIONSHIP,
                        severity = 0.8,
                        description = "重复争议模式",
                        involvedParticipants = listOf(participantPair.first, participantPair.second),
                        timestamp = disputes.last().timestamp,
                        evidence = "争议次数: ${disputes.size}",
                        resolutionSuggestions = listOf("调解干预", "重新分组", "建立沟通规则")
                    )
                )
            }
        }
        
        return indicators
    }
    
    private fun groupInteractionsByTimeWindow(
        interactions: List<CollaborativeInteraction>,
        windowMinutes: Int
    ): Map<Int, List<CollaborativeInteraction>> {
        return interactions.groupBy { interaction ->
            (interaction.timestamp.epochSeconds / (windowMinutes * 60)).toInt()
        }
    }
    
    private fun calculateOverallSeverity(indicators: List<ConflictIndicator>): Double {
        return if (indicators.isNotEmpty()) {
            indicators.map { it.severity }.average()
        } else 0.0
    }
    
    private fun identifyRiskFactors(
        interactions: List<CollaborativeInteraction>,
        participants: List<SessionParticipant>
    ): List<ConflictRiskFactor> {
        val riskFactors = mutableListOf<ConflictRiskFactor>()
        
        // 1. 参与不平衡
        val participationCounts = interactions.groupBy { it.participantId }.mapValues { it.value.size }
        val maxParticipation = participationCounts.values.maxOrNull() ?: 0
        val minParticipation = participationCounts.values.minOrNull() ?: 0
        
        if (maxParticipation > 0 && minParticipation.toDouble() / maxParticipation < 0.3) {
            riskFactors.add(
                ConflictRiskFactor(
                    factor = "参与不平衡",
                    riskLevel = 0.7,
                    description = "部分成员参与度显著低于其他成员",
                    affectedParticipants = participationCounts.filter { it.value == minParticipation }.keys.toList()
                )
            )
        }
        
        // 2. 沟通频率过低
        val avgInteractionsPerHour = interactions.size.toDouble() / 
            maxOf(1, (interactions.maxOfOrNull { it.timestamp }?.epochSeconds ?: 0) - 
                     (interactions.minOfOrNull { it.timestamp }?.epochSeconds ?: 0)) * 3600
        
        if (avgInteractionsPerHour < 2) {
            riskFactors.add(
                ConflictRiskFactor(
                    factor = "沟通频率低",
                    riskLevel = 0.5,
                    description = "小组沟通频率较低，可能导致误解积累",
                    affectedParticipants = participants.map { it.studentId }
                )
            )
        }
        
        // 3. 缺乏积极反馈
        val positiveResponses = interactions.flatMap { it.responses }.count { response ->
            response.type in setOf(ResponseType.LIKE, ResponseType.HELPFUL, ResponseType.AGREE)
        }
        val totalResponses = interactions.flatMap { it.responses }.size
        
        if (totalResponses > 0 && positiveResponses.toDouble() / totalResponses < 0.3) {
            riskFactors.add(
                ConflictRiskFactor(
                    factor = "缺乏积极反馈",
                    riskLevel = 0.6,
                    description = "积极反馈比例较低，可能影响团队氛围",
                    affectedParticipants = participants.map { it.studentId }
                )
            )
        }
        
        return riskFactors
    }
    
    private fun calculateRiskScore(riskFactors: List<ConflictRiskFactor>): Double {
        return if (riskFactors.isNotEmpty()) {
            riskFactors.map { it.riskLevel }.average()
        } else 0.0
    }
    
    private fun categorizeRiskLevel(riskScore: Double): ConflictRiskLevel {
        return when {
            riskScore >= 0.8 -> ConflictRiskLevel.VERY_HIGH
            riskScore >= 0.6 -> ConflictRiskLevel.HIGH
            riskScore >= 0.4 -> ConflictRiskLevel.MEDIUM
            riskScore >= 0.2 -> ConflictRiskLevel.LOW
            else -> ConflictRiskLevel.VERY_LOW
        }
    }
    
    private fun generatePreventionRecommendations(riskFactors: List<ConflictRiskFactor>): List<String> {
        val recommendations = mutableListOf<String>()
        
        for (factor in riskFactors) {
            when (factor.factor) {
                "参与不平衡" -> recommendations.add("鼓励低参与度成员发言，分配具体任务")
                "沟通频率低" -> recommendations.add("设置定期检查点，增加互动活动")
                "缺乏积极反馈" -> recommendations.add("培养积极反馈文化，设置反馈提醒")
            }
        }
        
        return recommendations.distinct()
    }
    
    private fun identifyMonitoringPoints(riskFactors: List<ConflictRiskFactor>): List<String> {
        val monitoringPoints = mutableListOf<String>()
        
        if (riskFactors.any { it.factor == "参与不平衡" }) {
            monitoringPoints.add("监控各成员的参与频率")
        }
        
        if (riskFactors.any { it.factor == "沟通频率低" }) {
            monitoringPoints.add("跟踪小组整体活跃度")
        }
        
        if (riskFactors.any { it.factor == "缺乏积极反馈" }) {
            monitoringPoints.add("观察反馈类型分布")
        }
        
        return monitoringPoints
    }
    
    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
}

// ==================== 冲突检测数据模型 ====================

@Serializable
data class ConflictIndicator(
    val id: String,
    val type: ConflictType,
    val severity: Double, // 0.0 - 1.0
    val description: String,
    val involvedParticipants: List<StudentId>,
    val timestamp: Instant,
    val evidence: String,
    val resolutionSuggestions: List<String>
)

@Serializable
enum class ConflictType {
    TASK,           // 任务冲突
    PROCESS,        // 过程冲突
    RELATIONSHIP    // 关系冲突
}

@Serializable
data class ConflictTypeAnalysis(
    val taskConflicts: Int,
    val processConflicts: Int,
    val relationshipConflicts: Int,
    val dominantConflictType: ConflictType,
    val overallSeverity: Double
)

@Serializable
data class ConflictRiskAssessment(
    val riskLevel: ConflictRiskLevel,
    val riskScore: Double,
    val riskFactors: List<ConflictRiskFactor>,
    val preventionRecommendations: List<String>,
    val monitoringPoints: List<String>
)

@Serializable
enum class ConflictRiskLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

@Serializable
data class ConflictRiskFactor(
    val factor: String,
    val riskLevel: Double,
    val description: String,
    val affectedParticipants: List<StudentId>
)
