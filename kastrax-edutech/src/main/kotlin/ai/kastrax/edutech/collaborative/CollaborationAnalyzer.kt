package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

/**
 * Week 23-24: 协作学习分析器
 * 
 * 功能：
 * - 实时协作模式分析
 * - 参与度和贡献度评估
 * - 小组动态分析
 * - 学习成果评估
 * - 个性化建议生成
 */
class CollaborationAnalyzer {
    
    private val interactionAnalyzer = InteractionAnalyzer()
    private val participationTracker = ParticipationTracker()
    private val groupDynamicsAnalyzer = GroupDynamicsAnalyzer()
    private val learningOutcomesEvaluator = LearningOutcomesEvaluator()
    private val recommendationGenerator = RecommendationGenerator()
    
    /**
     * 分析协作学习会话
     */
    suspend fun analyzeSession(session: CollaborativeSession): CollaborationAnalysis {
        return try {
            // 1. 分析参与度指标
            val participationMetrics = analyzeParticipation(session)
            
            // 2. 分析小组动态
            val groupDynamics = analyzeGroupDynamics(session)
            
            // 3. 评估学习成果
            val learningOutcomes = evaluateLearningOutcomes(session)
            
            // 4. 生成改进建议
            val recommendations = generateRecommendations(session, participationMetrics, groupDynamics)
            
            CollaborationAnalysis(
                sessionId = session.id,
                participationMetrics = participationMetrics,
                groupDynamics = groupDynamics,
                learningOutcomes = learningOutcomes,
                recommendations = recommendations,
                generatedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            throw CollaborationAnalysisException("Failed to analyze session: ${e.message}", e)
        }
    }
    
    /**
     * 生成最终报告
     */
    suspend fun generateFinalReport(session: CollaborativeSession): FinalCollaborationReport {
        val analysis = analyzeSession(session)
        
        return FinalCollaborationReport(
            sessionId = session.id,
            sessionSummary = generateSessionSummary(session),
            participantPerformance = generateParticipantPerformance(analysis.participationMetrics),
            groupEffectiveness = evaluateGroupEffectiveness(analysis.groupDynamics),
            learningAchievements = summarizeLearningAchievements(analysis.learningOutcomes),
            improvementAreas = identifyImprovementAreas(analysis),
            successFactors = identifySuccessFactors(analysis),
            futureRecommendations = generateFutureRecommendations(analysis),
            generatedAt = Clock.System.now()
        )
    }
    
    /**
     * 实时分析协作模式
     */
    suspend fun analyzeRealTimePatterns(
        sessionId: SessionId,
        recentInteractions: List<CollaborativeInteraction>
    ): RealTimeAnalysis {
        return RealTimeAnalysis(
            sessionId = sessionId,
            currentEngagementLevel = calculateCurrentEngagement(recentInteractions),
            activeParticipants = identifyActiveParticipants(recentInteractions),
            emergingPatterns = identifyEmergingPatterns(recentInteractions),
            potentialIssues = detectPotentialIssues(recentInteractions),
            interventionSuggestions = suggestInterventions(recentInteractions),
            timestamp = Clock.System.now()
        )
    }
    
    // ==================== 私有分析方法 ====================
    
    private suspend fun analyzeParticipation(session: CollaborativeSession): Map<StudentId, ParticipationMetrics> {
        return session.participants.associate { participant ->
            participant.studentId to participationTracker.calculateMetrics(
                studentId = participant.studentId,
                sessionId = session.id,
                sessionDuration = calculateSessionDuration(session),
                interactions = getStudentInteractions(session.id, participant.studentId)
            )
        }
    }
    
    private suspend fun analyzeGroupDynamics(session: CollaborativeSession): GroupDynamicsAnalysis {
        return groupDynamicsAnalyzer.analyze(
            sessionId = session.id,
            participants = session.participants,
            interactions = getAllSessionInteractions(session.id),
            activities = session.activities
        )
    }
    
    private suspend fun evaluateLearningOutcomes(session: CollaborativeSession): LearningOutcomesAnalysis {
        return learningOutcomesEvaluator.evaluate(
            sessionId = session.id,
            learningObjectives = session.learningObjectives,
            participants = session.participants,
            activities = session.activities,
            interactions = getAllSessionInteractions(session.id)
        )
    }
    
    private suspend fun generateRecommendations(
        session: CollaborativeSession,
        participationMetrics: Map<StudentId, ParticipationMetrics>,
        groupDynamics: GroupDynamicsAnalysis
    ): List<CollaborationRecommendation> {
        return recommendationGenerator.generate(
            session = session,
            participationMetrics = participationMetrics,
            groupDynamics = groupDynamics
        )
    }
    
    private fun calculateSessionDuration(session: CollaborativeSession): Duration {
        return if (session.startTime != null && session.endTime != null) {
            session.endTime - session.startTime
        } else if (session.startTime != null) {
            Clock.System.now() - session.startTime
        } else {
            Duration.ZERO
        }
    }
    
    private suspend fun getStudentInteractions(sessionId: SessionId, studentId: StudentId): List<CollaborativeInteraction> {
        // 模拟获取学生交互数据
        return emptyList()
    }
    
    private suspend fun getAllSessionInteractions(sessionId: SessionId): List<CollaborativeInteraction> {
        // 模拟获取所有会话交互数据
        return emptyList()
    }
    
    private fun generateSessionSummary(session: CollaborativeSession): SessionSummary {
        return SessionSummary(
            title = session.title,
            type = session.sessionType,
            duration = calculateSessionDuration(session),
            participantCount = session.participants.size,
            activitiesCompleted = session.activities.count { it.status == ActivityStatus.COMPLETED },
            overallRating = calculateOverallRating(session)
        )
    }
    
    private fun generateParticipantPerformance(
        participationMetrics: Map<StudentId, ParticipationMetrics>
    ): Map<StudentId, ParticipantPerformance> {
        return participationMetrics.mapValues { (_, metrics) ->
            ParticipantPerformance(
                participationScore = calculateParticipationScore(metrics),
                contributionQuality = metrics.contributionQuality,
                collaborationEffectiveness = calculateCollaborationEffectiveness(metrics),
                leadershipDemonstrated = metrics.leadershipMoments > 0,
                areasOfStrength = identifyStrengths(metrics),
                areasForImprovement = identifyImprovements(metrics)
            )
        }
    }
    
    private fun evaluateGroupEffectiveness(groupDynamics: GroupDynamicsAnalysis): GroupEffectiveness {
        return GroupEffectiveness(
            cohesionLevel = groupDynamics.cohesionScore,
            communicationQuality = groupDynamics.communicationEffectiveness,
            conflictManagement = calculateConflictManagement(groupDynamics.conflictLevel),
            leadershipDistribution = evaluateLeadershipDistribution(groupDynamics.leadershipDistribution),
            collaborationPatterns = groupDynamics.collaborationPatterns,
            overallEffectiveness = calculateOverallEffectiveness(groupDynamics)
        )
    }
    
    private fun summarizeLearningAchievements(learningOutcomes: LearningOutcomesAnalysis): LearningAchievementSummary {
        return LearningAchievementSummary(
            objectivesAchieved = learningOutcomes.objectiveAchievement.count { it.value >= 0.8 },
            totalObjectives = learningOutcomes.objectiveAchievement.size,
            averageSkillGain = learningOutcomes.skillDevelopment.values.map { it.values.average() }.average(),
            knowledgeGainDistribution = categorizeKnowledgeGains(learningOutcomes.knowledgeGain),
            collaborationSkillImprovement = learningOutcomes.collaborationSkillImprovement.values.average()
        )
    }
    
    private fun identifyImprovementAreas(analysis: CollaborationAnalysis): List<ImprovementArea> {
        val areas = mutableListOf<ImprovementArea>()
        
        // 检查参与度问题
        val lowParticipation = analysis.participationMetrics.filter { (_, metrics) ->
            metrics.engagementLevel == EngagementLevel.LOW
        }
        if (lowParticipation.isNotEmpty()) {
            areas.add(
                ImprovementArea(
                    area = "参与度提升",
                    description = "部分学生参与度较低，需要增强互动",
                    affectedParticipants = lowParticipation.keys.toList(),
                    priority = RecommendationPriority.HIGH,
                    suggestedActions = listOf("增加互动活动", "提供个性化鼓励", "调整小组角色")
                )
            )
        }
        
        // 检查沟通问题
        if (analysis.groupDynamics.communicationEffectiveness < 0.6) {
            areas.add(
                ImprovementArea(
                    area = "沟通改善",
                    description = "小组沟通效果需要改善",
                    affectedParticipants = analysis.participationMetrics.keys.toList(),
                    priority = RecommendationPriority.MEDIUM,
                    suggestedActions = listOf("沟通技巧培训", "建立沟通规范", "使用协作工具")
                )
            )
        }
        
        // 检查冲突问题
        if (analysis.groupDynamics.conflictLevel == ConflictLevel.HIGH || 
            analysis.groupDynamics.conflictLevel == ConflictLevel.SEVERE) {
            areas.add(
                ImprovementArea(
                    area = "冲突管理",
                    description = "存在较高级别的冲突需要干预",
                    affectedParticipants = analysis.participationMetrics.keys.toList(),
                    priority = RecommendationPriority.CRITICAL,
                    suggestedActions = listOf("冲突调解", "重新分组", "建立协作规则")
                )
            )
        }
        
        return areas
    }
    
    private fun identifySuccessFactors(analysis: CollaborationAnalysis): List<SuccessFactor> {
        val factors = mutableListOf<SuccessFactor>()
        
        // 高参与度
        val highParticipation = analysis.participationMetrics.filter { (_, metrics) ->
            metrics.engagementLevel == EngagementLevel.HIGH
        }
        if (highParticipation.isNotEmpty()) {
            factors.add(
                SuccessFactor(
                    factor = "高参与度",
                    description = "多数学生表现出高度参与",
                    contributingParticipants = highParticipation.keys.toList(),
                    impact = "促进了知识分享和协作学习",
                    sustainabilityTips = listOf("保持活动多样性", "及时给予正面反馈", "设置适当挑战")
                )
            )
        }
        
        // 良好的协作模式
        val positivePatterns = analysis.groupDynamics.collaborationPatterns.filter { 
            it.impact == PatternImpact.POSITIVE || it.impact == PatternImpact.VERY_POSITIVE 
        }
        if (positivePatterns.isNotEmpty()) {
            factors.add(
                SuccessFactor(
                    factor = "有效协作模式",
                    description = "建立了良好的协作模式",
                    contributingParticipants = positivePatterns.flatMap { it.participants }.distinct(),
                    impact = "提高了学习效率和团队凝聚力",
                    sustainabilityTips = listOf("强化成功模式", "分享最佳实践", "建立协作文化")
                )
            )
        }
        
        return factors
    }
    
    private fun generateFutureRecommendations(analysis: CollaborationAnalysis): List<FutureRecommendation> {
        val recommendations = mutableListOf<FutureRecommendation>()
        
        // 基于分析结果生成未来建议
        recommendations.add(
            FutureRecommendation(
                category = "小组优化",
                recommendation = "基于本次协作表现调整未来分组策略",
                rationale = "分析显示某些学生组合效果更好",
                expectedBenefit = "提高协作效率和学习成果",
                implementationSteps = listOf(
                    "分析兼容性数据",
                    "更新学生协作档案",
                    "应用智能分组算法"
                )
            )
        )
        
        recommendations.add(
            FutureRecommendation(
                category = "技能发展",
                recommendation = "针对性地发展协作技能",
                rationale = "识别出需要提升的协作技能",
                expectedBenefit = "增强整体协作能力",
                implementationSteps = listOf(
                    "设计技能训练活动",
                    "提供个性化指导",
                    "建立同伴学习机制"
                )
            )
        )
        
        return recommendations
    }
    
    // ==================== 计算辅助方法 ====================
    
    private fun calculateCurrentEngagement(interactions: List<CollaborativeInteraction>): EngagementLevel {
        if (interactions.isEmpty()) return EngagementLevel.LOW
        
        val recentInteractions = interactions.takeLast(10)
        val avgTimeBetween = if (recentInteractions.size > 1) {
            val timeSpan = recentInteractions.last().timestamp - recentInteractions.first().timestamp
            timeSpan.inWholeMinutes / (recentInteractions.size - 1)
        } else 0
        
        return when {
            avgTimeBetween <= 2 -> EngagementLevel.HIGH
            avgTimeBetween <= 5 -> EngagementLevel.MEDIUM
            else -> EngagementLevel.LOW
        }
    }
    
    private fun identifyActiveParticipants(interactions: List<CollaborativeInteraction>): List<StudentId> {
        val recentWindow = Clock.System.now() - 10.minutes
        return interactions
            .filter { it.timestamp >= recentWindow }
            .map { it.participantId }
            .distinct()
    }
    
    private fun identifyEmergingPatterns(interactions: List<CollaborativeInteraction>): List<EmergingPattern> {
        // 简化实现：识别基本模式
        val patterns = mutableListOf<EmergingPattern>()
        
        // 检查知识分享模式
        val sharingInteractions = interactions.filter { it.type == InteractionType.SHARE }
        if (sharingInteractions.size >= 3) {
            patterns.add(
                EmergingPattern(
                    type = "知识分享",
                    description = "频繁的知识分享活动",
                    strength = sharingInteractions.size / interactions.size.toDouble(),
                    participants = sharingInteractions.map { it.participantId }.distinct()
                )
            )
        }
        
        return patterns
    }
    
    private fun detectPotentialIssues(interactions: List<CollaborativeInteraction>): List<PotentialIssue> {
        val issues = mutableListOf<PotentialIssue>()
        
        // 检查参与不平衡
        val participationCounts = interactions.groupBy { it.participantId }.mapValues { it.value.size }
        val maxParticipation = participationCounts.values.maxOrNull() ?: 0
        val minParticipation = participationCounts.values.minOrNull() ?: 0
        
        if (maxParticipation > 0 && minParticipation.toDouble() / maxParticipation < 0.3) {
            issues.add(
                PotentialIssue(
                    type = "参与不平衡",
                    severity = IssueSeverity.MEDIUM,
                    description = "部分学生参与度明显低于其他人",
                    affectedParticipants = participationCounts.filter { it.value == minParticipation }.keys.toList()
                )
            )
        }
        
        return issues
    }
    
    private fun suggestInterventions(interactions: List<CollaborativeInteraction>): List<InterventionSuggestion> {
        val suggestions = mutableListOf<InterventionSuggestion>()
        
        // 基于检测到的问题建议干预措施
        val lowParticipants = identifyLowParticipants(interactions)
        if (lowParticipants.isNotEmpty()) {
            suggestions.add(
                InterventionSuggestion(
                    type = "参与度提升",
                    urgency = InterventionUrgency.MEDIUM,
                    description = "鼓励低参与度学生更多参与",
                    targetParticipants = lowParticipants,
                    suggestedActions = listOf("直接提问", "分配特定任务", "提供鼓励")
                )
            )
        }
        
        return suggestions
    }
    
    private fun identifyLowParticipants(interactions: List<CollaborativeInteraction>): List<StudentId> {
        val participationCounts = interactions.groupBy { it.participantId }.mapValues { it.value.size }
        val avgParticipation = participationCounts.values.average()
        
        return participationCounts.filter { it.value < avgParticipation * 0.5 }.keys.toList()
    }
    
    private fun calculateOverallRating(session: CollaborativeSession): Double {
        // 简化计算：基于参与者数量和活动完成情况
        val participationScore = minOf(1.0, session.participants.size / 10.0)
        val activityScore = if (session.activities.isNotEmpty()) {
            session.activities.count { it.status == ActivityStatus.COMPLETED }.toDouble() / session.activities.size
        } else 0.5
        
        return (participationScore + activityScore) / 2.0
    }
    
    private fun calculateParticipationScore(metrics: ParticipationMetrics): Double {
        return (metrics.contributionQuality + 
                when (metrics.engagementLevel) {
                    EngagementLevel.HIGH -> 1.0
                    EngagementLevel.MEDIUM -> 0.7
                    EngagementLevel.LOW -> 0.3
                }) / 2.0
    }
    
    private fun calculateCollaborationEffectiveness(metrics: ParticipationMetrics): Double {
        return (metrics.helpfulInteractions.toDouble() / maxOf(1, metrics.messageCount)) * 
               (if (metrics.leadershipMoments > 0) 1.2 else 1.0)
    }
    
    private fun identifyStrengths(metrics: ParticipationMetrics): List<String> {
        val strengths = mutableListOf<String>()
        
        if (metrics.contributionQuality >= 0.8) strengths.add("高质量贡献")
        if (metrics.engagementLevel == EngagementLevel.HIGH) strengths.add("高度参与")
        if (metrics.leadershipMoments > 0) strengths.add("领导能力")
        if (metrics.helpfulInteractions >= 3) strengths.add("乐于助人")
        
        return strengths
    }
    
    private fun identifyImprovements(metrics: ParticipationMetrics): List<String> {
        val improvements = mutableListOf<String>()
        
        if (metrics.contributionQuality < 0.5) improvements.add("提高贡献质量")
        if (metrics.engagementLevel == EngagementLevel.LOW) improvements.add("增加参与度")
        if (metrics.helpfulInteractions == 0) improvements.add("更多帮助他人")
        if (metrics.messageCount < 5) improvements.add("增加交流频率")
        
        return improvements
    }
    
    private fun calculateConflictManagement(conflictLevel: ConflictLevel): Double {
        return when (conflictLevel) {
            ConflictLevel.NONE -> 1.0
            ConflictLevel.LOW -> 0.8
            ConflictLevel.MODERATE -> 0.6
            ConflictLevel.HIGH -> 0.3
            ConflictLevel.SEVERE -> 0.1
        }
    }
    
    private fun evaluateLeadershipDistribution(distribution: Map<StudentId, Double>): LeadershipDistributionEvaluation {
        val values = distribution.values
        val maxLeadership = values.maxOrNull() ?: 0.0
        val avgLeadership = values.average()
        val variance = values.map { (it - avgLeadership) * (it - avgLeadership) }.average()
        
        return LeadershipDistributionEvaluation(
            isBalanced = variance < 0.1,
            dominantLeader = distribution.entries.find { it.value == maxLeadership }?.key,
            distributionScore = 1.0 - variance,
            recommendation = if (variance > 0.2) "鼓励更多学生承担领导角色" else "领导角色分布良好"
        )
    }
    
    private fun calculateOverallEffectiveness(groupDynamics: GroupDynamicsAnalysis): Double {
        return (groupDynamics.cohesionScore + 
                groupDynamics.communicationEffectiveness + 
                calculateConflictManagement(groupDynamics.conflictLevel)) / 3.0
    }
    
    private fun categorizeKnowledgeGains(knowledgeGain: Map<StudentId, Double>): KnowledgeGainDistribution {
        val gains = knowledgeGain.values
        return KnowledgeGainDistribution(
            highGain = gains.count { it >= 0.8 },
            mediumGain = gains.count { it >= 0.5 && it < 0.8 },
            lowGain = gains.count { it < 0.5 },
            averageGain = gains.average()
        )
    }
}

/**
 * 协作分析异常
 */
class CollaborationAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
