package ai.kastrax.edutech.analytics

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 风险评估引擎
 * 
 * 负责评估学习风险并生成预警
 */
class RiskAssessmentEngine {
    
    /**
     * 评估学习风险
     */
    suspend fun assessLearningRisks(
        studentId: StudentId,
        learningPatterns: LearningPatterns,
        predictions: LearningPredictions
    ): RiskAssessmentResult {
        
        // 识别各类风险
        val academicRisks = assessAcademicRisks(studentId, learningPatterns, predictions)
        val engagementRisks = assessEngagementRisks(studentId, learningPatterns)
        val motivationalRisks = assessMotivationalRisks(studentId, learningPatterns)
        val technicalRisks = assessTechnicalRisks(studentId, learningPatterns)
        
        // 合并所有风险
        val allRisks = mutableListOf<IdentifiedRisk>()
        allRisks.addAll(academicRisks)
        allRisks.addAll(engagementRisks)
        allRisks.addAll(motivationalRisks)
        allRisks.addAll(technicalRisks)
        
        // 计算风险因素
        val riskFactors = calculateRiskFactors(learningPatterns, predictions)
        
        // 确定整体风险等级
        val overallRiskLevel = calculateOverallRiskLevel(allRisks)
        
        // 生成缓解建议
        val mitigationRecommendations = generateMitigationRecommendations(allRisks, riskFactors)
        
        return RiskAssessmentResult(
            studentId = studentId,
            assessmentTimestamp = Clock.System.now(),
            overallRiskLevel = overallRiskLevel,
            identifiedRisks = allRisks,
            riskFactors = riskFactors,
            mitigationRecommendations = mitigationRecommendations
        )
    }
    
    /**
     * 评估即时风险
     */
    suspend fun assessImmediateRisks(
        studentId: StudentId,
        currentSession: LearningSession,
        currentPatterns: List<RealTimePattern>
    ): List<ImmediateRisk> {
        
        val immediateRisks = mutableListOf<ImmediateRisk>()
        
        // 检查参与度风险
        val engagementPattern = currentPatterns.find { it.patternType == "session_engagement" }
        if (engagementPattern != null && engagementPattern.strength < 0.4) {
            immediateRisks.add(
                ImmediateRisk(
                    riskType = "低参与度",
                    severity = RiskSeverity.HIGH,
                    probability = 0.8,
                    description = "当前学习会话参与度过低",
                    recommendedAction = "调整学习内容或休息片刻"
                )
            )
        }
        
        // 检查进度风险
        val progressPattern = currentPatterns.find { it.patternType == "progress_rate" }
        if (progressPattern != null && progressPattern.strength < 0.3) {
            immediateRisks.add(
                ImmediateRisk(
                    riskType = "进度缓慢",
                    severity = RiskSeverity.MEDIUM,
                    probability = 0.7,
                    description = "学习进度明显低于预期",
                    recommendedAction = "简化当前内容或提供额外指导"
                )
            )
        }
        
        // 检查困难应对风险
        val difficultyPattern = currentPatterns.find { it.patternType == "difficulty_response" }
        if (difficultyPattern != null && difficultyPattern.strength < 0.3) {
            immediateRisks.add(
                ImmediateRisk(
                    riskType = "困难应对不足",
                    severity = RiskSeverity.HIGH,
                    probability = 0.9,
                    description = "学生在应对当前难度时遇到困难",
                    recommendedAction = "提供即时帮助或降低难度"
                )
            )
        }
        
        // 检查疲劳风险
        if (currentSession.duration > Duration.parse("PT2H")) {
            immediateRisks.add(
                ImmediateRisk(
                    riskType = "学习疲劳",
                    severity = RiskSeverity.MEDIUM,
                    probability = 0.6,
                    description = "学习时间过长可能导致疲劳",
                    recommendedAction = "建议休息或结束当前会话"
                )
            )
        }
        
        return immediateRisks
    }
    
    /**
     * 评估学术风险
     */
    private fun assessAcademicRisks(
        studentId: StudentId,
        patterns: LearningPatterns,
        predictions: LearningPredictions
    ): List<IdentifiedRisk> {
        
        val risks = mutableListOf<IdentifiedRisk>()
        
        // 成绩风险
        if (predictions.expectedGrade < 60) {
            risks.add(
                IdentifiedRisk(
                    riskId = "academic_failure_${generateId()}",
                    type = RiskType.ACADEMIC_FAILURE,
                    severity = RiskSeverity.CRITICAL,
                    probability = 0.8,
                    description = "预期成绩低于及格线",
                    potentialImpact = "可能导致课程不及格",
                    timeframe = Duration.parse("P14D"),
                    confidence = 0.85
                )
            )
        } else if (predictions.expectedGrade < 70) {
            risks.add(
                IdentifiedRisk(
                    riskId = "academic_underperformance_${generateId()}",
                    type = RiskType.ACADEMIC_FAILURE,
                    severity = RiskSeverity.HIGH,
                    probability = 0.6,
                    description = "预期成绩偏低",
                    potentialImpact = "学习效果不理想",
                    timeframe = Duration.parse("P21D"),
                    confidence = 0.75
                )
            )
        }
        
        // 知识缺口风险
        val lowMasteryAreas = predictions.masteryPredictions.filter { it.value < 0.6 }
        if (lowMasteryAreas.isNotEmpty()) {
            risks.add(
                IdentifiedRisk(
                    riskId = "knowledge_gap_${generateId()}",
                    type = RiskType.KNOWLEDGE_GAP,
                    severity = RiskSeverity.MEDIUM,
                    probability = 0.7,
                    description = "存在知识缺口：${lowMasteryAreas.keys.joinToString(", ")}",
                    potentialImpact = "影响后续学习进展",
                    timeframe = Duration.parse("P7D"),
                    confidence = 0.8
                )
            )
        }

        return risks
    }

    /**
     * 评估参与度风险
     */
    private fun assessEngagementRisks(
        studentId: StudentId,
        patterns: LearningPatterns
    ): List<IdentifiedRisk> {

        val risks = mutableListOf<IdentifiedRisk>()

        // 参与度下降风险
        val engagementPatterns = patterns.performancePatterns.filter {
            it.performanceMetric == "engagement_level"
        }

        engagementPatterns.forEach { pattern ->
            if (pattern.trend == PerformanceTrend.DECLINING) {
                risks.add(
                    IdentifiedRisk(
                        riskId = "disengagement_${generateId()}",
                        type = RiskType.DISENGAGEMENT,
                        severity = RiskSeverity.HIGH,
                        probability = 0.7,
                        description = "学习参与度呈下降趋势",
                        potentialImpact = "可能导致学习效果显著下降",
                        timeframe = Duration.parse("P7D"),
                        confidence = 0.8
                    )
                )
            }
        }
        
        // 时间管理风险
        val temporalPatterns = patterns.temporalPatterns
        val irregularPatterns = temporalPatterns.filter { it.frequency < 0.5 }
        if (irregularPatterns.isNotEmpty()) {
            risks.add(
                IdentifiedRisk(
                    riskId = "time_management_${generateId()}",
                    type = RiskType.TIME_MANAGEMENT,
                    severity = RiskSeverity.MEDIUM,
                    probability = 0.6,
                    description = "学习时间安排不规律",
                    potentialImpact = "影响学习效率和进度",
                    timeframe = Duration.parse("P14D"),
                    confidence = 0.7
                )
            )
        }
        
        return risks
    }
    
    /**
     * 评估动机风险
     */
    private fun assessMotivationalRisks(
        studentId: StudentId,
        patterns: LearningPatterns
    ): List<IdentifiedRisk> {
        
        val risks = mutableListOf<IdentifiedRisk>()
        
        // 动机下降风险
        val behavioralPatterns = patterns.behavioralPatterns
        val negativePatterns = behavioralPatterns.filter { it.impact == BehaviorImpact.NEGATIVE }
        
        if (negativePatterns.size > behavioralPatterns.size / 2) {
            risks.add(
                IdentifiedRisk(
                    riskId = "motivation_loss_${generateId()}",
                    type = RiskType.MOTIVATION_LOSS,
                    severity = RiskSeverity.HIGH,
                    probability = 0.6,
                    description = "负面学习行为模式增多",
                    potentialImpact = "学习动机可能下降",
                    timeframe = Duration.parse("P7D"),
                    confidence = 0.7
                )
            )
        }

        // 学习倦怠风险
        if (patterns.patternConsistency < 0.5) {
            risks.add(
                IdentifiedRisk(
                    riskId = "burnout_${generateId()}",
                    type = RiskType.OVERLOAD,
                    severity = RiskSeverity.MEDIUM,
                    probability = 0.5,
                    description = "学习模式一致性较低，可能存在倦怠",
                    potentialImpact = "学习效率下降，需要调整策略",
                    timeframe = Duration.parse("P3D"),
                    confidence = 0.6
                )
            )
        }
        
        return risks
    }
    
    /**
     * 评估技术风险
     */
    private fun assessTechnicalRisks(
        studentId: StudentId,
        patterns: LearningPatterns
    ): List<IdentifiedRisk> {
        
        val risks = mutableListOf<IdentifiedRisk>()
        
        // 技能缺陷风险
        val performancePatterns = patterns.performancePatterns
        val volatilePatterns = performancePatterns.filter { it.volatility > 0.4 }
        
        if (volatilePatterns.isNotEmpty()) {
            risks.add(
                IdentifiedRisk(
                    riskId = "skill_deficiency_${generateId()}",
                    type = RiskType.SKILL_DEFICIENCY,
                    severity = RiskSeverity.MEDIUM,
                    probability = 0.5,
                    description = "技能掌握不稳定",
                    potentialImpact = "影响学习进展的稳定性",
                    timeframe = Duration.parse("P7D"),
                    confidence = 0.6
                )
            )
        }
        
        return risks
    }
    
    /**
     * 计算风险因素
     */
    private fun calculateRiskFactors(
        patterns: LearningPatterns,
        predictions: LearningPredictions
    ): List<RiskFactor> {
        
        val factors = mutableListOf<RiskFactor>()
        
        // 成绩因素
        factors.add(
            RiskFactor(
                factorId = "grade_factor",
                name = "预期成绩",
                category = RiskCategory.ACADEMIC,
                weight = 0.3,
                currentValue = predictions.expectedGrade,
                thresholdValue = 70.0,
                trend = if (predictions.expectedGrade >= 70) Trend.STABLE else Trend.DECREASING
            )
        )
        
        // 参与度因素
        factors.add(
            RiskFactor(
                factorId = "engagement_factor",
                name = "学习参与度",
                category = RiskCategory.BEHAVIORAL,
                weight = 0.25,
                currentValue = patterns.patternStrength * 100,
                thresholdValue = 70.0,
                trend = if (patterns.patternStrength > 0.7) Trend.STABLE else Trend.DECREASING
            )
        )
        
        // 一致性因素
        factors.add(
            RiskFactor(
                factorId = "consistency_factor",
                name = "学习一致性",
                category = RiskCategory.BEHAVIORAL,
                weight = 0.2,
                currentValue = patterns.patternConsistency * 100,
                thresholdValue = 60.0,
                trend = if (patterns.patternConsistency > 0.6) Trend.STABLE else Trend.DECREASING
            )
        )
        
        // 完成概率因素
        factors.add(
            RiskFactor(
                factorId = "completion_factor",
                name = "完成概率",
                category = RiskCategory.ACADEMIC,
                weight = 0.25,
                currentValue = predictions.completionProbability * 100,
                thresholdValue = 80.0,
                trend = if (predictions.completionProbability > 0.8) Trend.STABLE else Trend.DECREASING
            )
        )
        
        return factors
    }
    
    /**
     * 计算整体风险等级
     */
    private fun calculateOverallRiskLevel(risks: List<IdentifiedRisk>): RiskLevel {
        if (risks.isEmpty()) return RiskLevel.MINIMAL
        
        val criticalRisks = risks.count { it.severity == RiskSeverity.CRITICAL }
        val highRisks = risks.count { it.severity == RiskSeverity.HIGH }
        val mediumRisks = risks.count { it.severity == RiskSeverity.MEDIUM }
        
        return when {
            criticalRisks > 0 -> RiskLevel.CRITICAL
            highRisks >= 2 -> RiskLevel.HIGH
            highRisks == 1 || mediumRisks >= 3 -> RiskLevel.MODERATE
            mediumRisks > 0 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }
    
    /**
     * 生成缓解建议
     */
    private fun generateMitigationRecommendations(
        risks: List<IdentifiedRisk>,
        riskFactors: List<RiskFactor>
    ): List<MitigationRecommendation> {
        
        val recommendations = mutableListOf<MitigationRecommendation>()
        
        // 针对学术风险的建议
        val academicRisks = risks.filter { it.type == RiskType.ACADEMIC_FAILURE }
        if (academicRisks.isNotEmpty()) {
            recommendations.add(
                MitigationRecommendation(
                    recommendationId = "academic_mitigation_${generateId()}",
                    targetRisk = "学术表现风险",
                    strategy = "加强学习支持",
                    actions = listOf(
                        "安排额外辅导时间",
                        "提供个性化学习资源",
                        "调整学习计划和节奏",
                        "增加练习和复习时间"
                    ),
                    priority = Priority.HIGH,
                    estimatedEffectiveness = 0.8,
                    implementationTime = Duration.parse("P7D")
                )
            )
        }
        
        // 针对参与度风险的建议
        val engagementRisks = risks.filter { it.type == RiskType.DISENGAGEMENT }
        if (engagementRisks.isNotEmpty()) {
            recommendations.add(
                MitigationRecommendation(
                    recommendationId = "engagement_mitigation_${generateId()}",
                    targetRisk = "参与度下降风险",
                    strategy = "提升学习动机",
                    actions = listOf(
                        "增加互动性内容",
                        "设置短期可达成目标",
                        "提供及时正面反馈",
                        "引入游戏化元素"
                    ),
                    priority = Priority.MEDIUM,
                    estimatedEffectiveness = 0.7,
                    implementationTime = Duration.parse("P3D")
                )
            )
        }
        
        // 针对时间管理风险的建议
        val timeRisks = risks.filter { it.type == RiskType.TIME_MANAGEMENT }
        if (timeRisks.isNotEmpty()) {
            recommendations.add(
                MitigationRecommendation(
                    recommendationId = "time_mitigation_${generateId()}",
                    targetRisk = "时间管理风险",
                    strategy = "改善时间管理",
                    actions = listOf(
                        "制定详细学习计划",
                        "设置学习提醒",
                        "建立固定学习时间",
                        "提供时间管理工具"
                    ),
                    priority = Priority.MEDIUM,
                    estimatedEffectiveness = 0.75,
                    implementationTime = Duration.parse("P5D")
                )
            )
        }
        
        // 针对动机风险的建议
        val motivationRisks = risks.filter { it.type == RiskType.MOTIVATION_LOSS }
        if (motivationRisks.isNotEmpty()) {
            recommendations.add(
                MitigationRecommendation(
                    recommendationId = "motivation_mitigation_${generateId()}",
                    targetRisk = "学习动机风险",
                    strategy = "重建学习动机",
                    actions = listOf(
                        "重新设定学习目标",
                        "提供成就感体验",
                        "连接学习与个人兴趣",
                        "寻求同伴支持"
                    ),
                    priority = Priority.HIGH,
                    estimatedEffectiveness = 0.6,
                    implementationTime = Duration.parse("P7D")
                )
            )
        }
        
        return recommendations
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString().take(8)
}
