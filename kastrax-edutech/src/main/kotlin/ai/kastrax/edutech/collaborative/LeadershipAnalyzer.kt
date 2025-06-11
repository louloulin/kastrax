package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Week 23-24: 领导力分析器
 * 
 * 功能：
 * - 分析学生的领导行为
 * - 识别领导力类型和风格
 * - 评估领导效果
 * - 提供领导力发展建议
 */
class LeadershipAnalyzer {
    
    /**
     * 计算领导力分数
     */
    fun calculateLeadershipScore(
        studentId: StudentId,
        interactions: List<CollaborativeInteraction>
    ): Double {
        val studentInteractions = interactions.filter { it.participantId == studentId }
        
        if (studentInteractions.isEmpty()) return 0.0
        
        // 1. 主动性分数
        val initiativeScore = calculateInitiativeScore(studentInteractions, interactions)
        
        // 2. 影响力分数
        val influenceScore = calculateInfluenceScore(studentId, interactions)
        
        // 3. 协调能力分数
        val coordinationScore = calculateCoordinationScore(studentInteractions)
        
        // 4. 支持他人分数
        val supportScore = calculateSupportScore(studentInteractions)
        
        // 5. 决策参与分数
        val decisionMakingScore = calculateDecisionMakingScore(studentInteractions)
        
        // 加权计算总分
        return (initiativeScore * 0.25 + 
                influenceScore * 0.25 + 
                coordinationScore * 0.2 + 
                supportScore * 0.15 + 
                decisionMakingScore * 0.15)
    }
    
    /**
     * 分析领导力类型
     */
    fun analyzeLeadershipType(
        studentId: StudentId,
        interactions: List<CollaborativeInteraction>
    ): LeadershipTypeAnalysis {
        val studentInteractions = interactions.filter { it.participantId == studentId }
        
        val typeScores = mapOf(
            LeadershipType.TASK_ORIENTED to calculateTaskOrientedScore(studentInteractions),
            LeadershipType.RELATIONSHIP_ORIENTED to calculateRelationshipOrientedScore(studentInteractions),
            LeadershipType.TRANSFORMATIONAL to calculateTransformationalScore(studentInteractions),
            LeadershipType.SERVANT to calculateServantLeadershipScore(studentInteractions),
            LeadershipType.DEMOCRATIC to calculateDemocraticScore(studentInteractions),
            LeadershipType.SITUATIONAL to calculateSituationalScore(studentInteractions, interactions)
        )
        
        val dominantType = typeScores.maxByOrNull { it.value }?.key ?: LeadershipType.TASK_ORIENTED
        
        return LeadershipTypeAnalysis(
            dominantType = dominantType,
            typeScores = typeScores,
            leadershipStrengths = identifyLeadershipStrengths(typeScores),
            developmentAreas = identifyDevelopmentAreas(typeScores),
            recommendations = generateLeadershipRecommendations(dominantType, typeScores)
        )
    }
    
    /**
     * 评估领导效果
     */
    fun evaluateLeadershipEffectiveness(
        studentId: StudentId,
        interactions: List<CollaborativeInteraction>,
        groupPerformance: GroupPerformanceMetrics
    ): LeadershipEffectivenessEvaluation {
        val leadershipScore = calculateLeadershipScore(studentId, interactions)
        val followerResponse = analyzeFollowerResponse(studentId, interactions)
        val goalAchievement = evaluateGoalAchievement(studentId, interactions, groupPerformance)
        val teamCohesion = evaluateTeamCohesionImpact(studentId, interactions)
        
        return LeadershipEffectivenessEvaluation(
            overallEffectiveness = (leadershipScore + followerResponse + goalAchievement + teamCohesion) / 4.0,
            leadershipScore = leadershipScore,
            followerResponse = followerResponse,
            goalAchievement = goalAchievement,
            teamCohesion = teamCohesion,
            effectivenessLevel = categorizeEffectiveness((leadershipScore + followerResponse + goalAchievement + teamCohesion) / 4.0),
            improvementSuggestions = generateImprovementSuggestions(leadershipScore, followerResponse, goalAchievement, teamCohesion)
        )
    }
    
    // ==================== 私有计算方法 ====================
    
    private fun calculateInitiativeScore(
        studentInteractions: List<CollaborativeInteraction>,
        allInteractions: List<CollaborativeInteraction>
    ): Double {
        // 计算主动发起讨论、提出问题、分享资源的比例
        val initiativeActions = studentInteractions.count { interaction ->
            when (interaction.type) {
                InteractionType.QUESTION -> true
                InteractionType.SHARE -> true
                InteractionType.MESSAGE -> interaction.content.text?.contains("我们应该") == true ||
                                         interaction.content.text?.contains("建议") == true ||
                                         interaction.content.text?.contains("开始") == true
                else -> false
            }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            initiativeActions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateInfluenceScore(studentId: StudentId, interactions: List<CollaborativeInteraction>): Double {
        // 计算其他人对该学生的响应率和积极响应比例
        val studentInteractions = interactions.filter { it.participantId == studentId }
        
        if (studentInteractions.isEmpty()) return 0.0
        
        val totalResponses = studentInteractions.sumOf { it.responses.size }
        val positiveResponses = studentInteractions.sumOf { interaction ->
            interaction.responses.count { response ->
                response.type in setOf(ResponseType.LIKE, ResponseType.HELPFUL, ResponseType.AGREE)
            }
        }
        
        val responseRate = totalResponses.toDouble() / studentInteractions.size
        val positiveRate = if (totalResponses > 0) positiveResponses.toDouble() / totalResponses else 0.0
        
        return (responseRate * 0.6 + positiveRate * 0.4).coerceAtMost(1.0)
    }
    
    private fun calculateCoordinationScore(studentInteractions: List<CollaborativeInteraction>): Double {
        // 分析协调和组织行为
        val coordinationKeywords = listOf("安排", "组织", "分配", "计划", "协调", "统一", "整理")
        
        val coordinationActions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            coordinationKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            (coordinationActions.toDouble() / studentInteractions.size).coerceAtMost(1.0)
        } else 0.0
    }
    
    private fun calculateSupportScore(studentInteractions: List<CollaborativeInteraction>): Double {
        // 分析支持和帮助他人的行为
        val supportActions = studentInteractions.count { interaction ->
            when (interaction.type) {
                InteractionType.ANSWER -> true
                InteractionType.SHARE -> true
                InteractionType.COMMENT -> {
                    val content = interaction.content.text?.lowercase() ?: ""
                    listOf("帮助", "支持", "鼓励", "加油", "不错", "很好").any { content.contains(it) }
                }
                else -> false
            }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            supportActions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateDecisionMakingScore(studentInteractions: List<CollaborativeInteraction>): Double {
        // 分析参与决策的程度
        val decisionKeywords = listOf("决定", "选择", "确定", "同意", "反对", "投票", "建议")
        
        val decisionActions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            decisionKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            (decisionActions.toDouble() / studentInteractions.size).coerceAtMost(1.0)
        } else 0.0
    }
    
    private fun calculateTaskOrientedScore(studentInteractions: List<CollaborativeInteraction>): Double {
        val taskKeywords = listOf("任务", "目标", "完成", "进度", "计划", "安排", "工作")
        
        val taskFocusedInteractions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            taskKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            taskFocusedInteractions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateRelationshipOrientedScore(studentInteractions: List<CollaborativeInteraction>): Double {
        val relationshipKeywords = listOf("大家", "团队", "合作", "一起", "感谢", "辛苦", "支持")
        
        val relationshipFocusedInteractions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            relationshipKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            relationshipFocusedInteractions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateTransformationalScore(studentInteractions: List<CollaborativeInteraction>): Double {
        val transformationalKeywords = listOf("创新", "改进", "提升", "发展", "学习", "成长", "挑战")
        
        val transformationalInteractions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            transformationalKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            transformationalInteractions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateServantLeadershipScore(studentInteractions: List<CollaborativeInteraction>): Double {
        val servantKeywords = listOf("帮助", "服务", "支持", "需要", "协助", "解决")
        
        val servantInteractions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            servantKeywords.any { keyword -> content.contains(keyword) } ||
            interaction.type == InteractionType.ANSWER
        }
        
        return if (studentInteractions.isNotEmpty()) {
            servantInteractions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateDemocraticScore(studentInteractions: List<CollaborativeInteraction>): Double {
        val democraticKeywords = listOf("大家觉得", "意见", "建议", "讨论", "投票", "共同")
        
        val democraticInteractions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            democraticKeywords.any { keyword -> content.contains(keyword) } ||
            interaction.type == InteractionType.QUESTION
        }
        
        return if (studentInteractions.isNotEmpty()) {
            democraticInteractions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun calculateSituationalScore(
        studentInteractions: List<CollaborativeInteraction>,
        allInteractions: List<CollaborativeInteraction>
    ): Double {
        // 分析是否能根据情况调整领导风格
        // 简化实现：检查在不同类型活动中的表现变化
        val activityTypes = allInteractions.map { it.activityId }.distinct()
        
        if (activityTypes.size < 2) return 0.0
        
        val styleVariations = activityTypes.map { activityId ->
            val activityInteractions = studentInteractions.filter { it.activityId == activityId }
            if (activityInteractions.isNotEmpty()) {
                calculateTaskOrientedScore(activityInteractions) - calculateRelationshipOrientedScore(activityInteractions)
            } else 0.0
        }
        
        val styleVariance = if (styleVariations.isNotEmpty()) {
            val mean = styleVariations.average()
            styleVariations.map { (it - mean) * (it - mean) }.average()
        } else 0.0
        
        return kotlin.math.sqrt(styleVariance).coerceAtMost(1.0)
    }
    
    private fun identifyLeadershipStrengths(typeScores: Map<LeadershipType, Double>): List<String> {
        val strengths = mutableListOf<String>()
        
        typeScores.forEach { (type, score) ->
            if (score >= 0.7) {
                strengths.add(when (type) {
                    LeadershipType.TASK_ORIENTED -> "任务导向能力强"
                    LeadershipType.RELATIONSHIP_ORIENTED -> "关系建设能力强"
                    LeadershipType.TRANSFORMATIONAL -> "变革领导能力强"
                    LeadershipType.SERVANT -> "服务型领导特质"
                    LeadershipType.DEMOCRATIC -> "民主参与式领导"
                    LeadershipType.SITUATIONAL -> "情境适应能力强"
                })
            }
        }
        
        return strengths
    }
    
    private fun identifyDevelopmentAreas(typeScores: Map<LeadershipType, Double>): List<String> {
        val developmentAreas = mutableListOf<String>()
        
        typeScores.forEach { (type, score) ->
            if (score < 0.3) {
                developmentAreas.add(when (type) {
                    LeadershipType.TASK_ORIENTED -> "任务管理能力"
                    LeadershipType.RELATIONSHIP_ORIENTED -> "团队关系建设"
                    LeadershipType.TRANSFORMATIONAL -> "创新变革思维"
                    LeadershipType.SERVANT -> "服务他人意识"
                    LeadershipType.DEMOCRATIC -> "民主决策参与"
                    LeadershipType.SITUATIONAL -> "情境判断能力"
                })
            }
        }
        
        return developmentAreas
    }
    
    private fun generateLeadershipRecommendations(
        dominantType: LeadershipType,
        typeScores: Map<LeadershipType, Double>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 基于主导类型的建议
        when (dominantType) {
            LeadershipType.TASK_ORIENTED -> {
                recommendations.add("继续发挥任务管理优势")
                if (typeScores[LeadershipType.RELATIONSHIP_ORIENTED]!! < 0.5) {
                    recommendations.add("加强团队关系建设")
                }
            }
            LeadershipType.RELATIONSHIP_ORIENTED -> {
                recommendations.add("继续发挥团队凝聚力优势")
                if (typeScores[LeadershipType.TASK_ORIENTED]!! < 0.5) {
                    recommendations.add("提升任务执行效率")
                }
            }
            LeadershipType.TRANSFORMATIONAL -> {
                recommendations.add("继续推动创新和变革")
                recommendations.add("帮助团队成员成长发展")
            }
            LeadershipType.SERVANT -> {
                recommendations.add("继续发挥服务他人的精神")
                recommendations.add("平衡服务与领导的关系")
            }
            LeadershipType.DEMOCRATIC -> {
                recommendations.add("继续促进民主参与")
                recommendations.add("在必要时展现决断力")
            }
            LeadershipType.SITUATIONAL -> {
                recommendations.add("继续发挥适应性优势")
                recommendations.add("深化特定情境下的领导技能")
            }
        }
        
        return recommendations
    }
    
    private fun analyzeFollowerResponse(studentId: StudentId, interactions: List<CollaborativeInteraction>): Double {
        val studentInteractions = interactions.filter { it.participantId == studentId }
        val otherInteractions = interactions.filter { it.participantId != studentId }
        
        // 分析其他人对该学生的响应情况
        val responsesToStudent = otherInteractions.count { interaction ->
            interaction.content.mentions.contains(studentId.value) ||
            interaction.responses.any { it.participantId == studentId }
        }
        
        val totalOtherInteractions = otherInteractions.size
        
        return if (totalOtherInteractions > 0) {
            responsesToStudent.toDouble() / totalOtherInteractions
        } else 0.0
    }
    
    private fun evaluateGoalAchievement(
        studentId: StudentId,
        interactions: List<CollaborativeInteraction>,
        groupPerformance: GroupPerformanceMetrics
    ): Double {
        // 简化实现：基于小组整体表现评估
        return groupPerformance.overallScore
    }
    
    private fun evaluateTeamCohesionImpact(studentId: StudentId, interactions: List<CollaborativeInteraction>): Double {
        val studentInteractions = interactions.filter { it.participantId == studentId }
        
        // 分析该学生对团队凝聚力的贡献
        val cohesionContributions = studentInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            listOf("团队", "一起", "合作", "支持", "帮助", "共同").any { content.contains(it) }
        }
        
        return if (studentInteractions.isNotEmpty()) {
            cohesionContributions.toDouble() / studentInteractions.size
        } else 0.0
    }
    
    private fun categorizeEffectiveness(score: Double): EffectivenessLevel {
        return when {
            score >= 0.8 -> EffectivenessLevel.EXCELLENT
            score >= 0.6 -> EffectivenessLevel.GOOD
            score >= 0.4 -> EffectivenessLevel.AVERAGE
            score >= 0.2 -> EffectivenessLevel.BELOW_AVERAGE
            else -> EffectivenessLevel.POOR
        }
    }
    
    private fun generateImprovementSuggestions(
        leadershipScore: Double,
        followerResponse: Double,
        goalAchievement: Double,
        teamCohesion: Double
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (leadershipScore < 0.5) suggestions.add("加强主动性和影响力")
        if (followerResponse < 0.5) suggestions.add("改善与团队成员的沟通")
        if (goalAchievement < 0.5) suggestions.add("提升目标导向和执行力")
        if (teamCohesion < 0.5) suggestions.add("增强团队凝聚力建设")
        
        return suggestions
    }
}

// ==================== 领导力分析数据模型 ====================

@Serializable
enum class LeadershipType {
    TASK_ORIENTED,          // 任务导向型
    RELATIONSHIP_ORIENTED,  // 关系导向型
    TRANSFORMATIONAL,       // 变革型
    SERVANT,               // 服务型
    DEMOCRATIC,            // 民主型
    SITUATIONAL           // 情境型
}

@Serializable
data class LeadershipTypeAnalysis(
    val dominantType: LeadershipType,
    val typeScores: Map<LeadershipType, Double>,
    val leadershipStrengths: List<String>,
    val developmentAreas: List<String>,
    val recommendations: List<String>
)

@Serializable
data class LeadershipEffectivenessEvaluation(
    val overallEffectiveness: Double,
    val leadershipScore: Double,
    val followerResponse: Double,
    val goalAchievement: Double,
    val teamCohesion: Double,
    val effectivenessLevel: EffectivenessLevel,
    val improvementSuggestions: List<String>
)

@Serializable
enum class EffectivenessLevel {
    EXCELLENT,
    GOOD,
    AVERAGE,
    BELOW_AVERAGE,
    POOR
}

@Serializable
data class GroupPerformanceMetrics(
    val overallScore: Double,
    val taskCompletion: Double,
    val qualityScore: Double,
    val collaborationScore: Double,
    val timeEfficiency: Double
)
