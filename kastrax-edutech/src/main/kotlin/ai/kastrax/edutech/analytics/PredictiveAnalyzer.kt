package ai.kastrax.edutech.analytics

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 预测性分析器
 * 
 * 负责基于学习模式和历史数据进行预测性分析
 */
class PredictiveAnalyzer {
    
    /**
     * 生成学习预测
     */
    suspend fun generatePredictions(
        studentId: StudentId,
        learningPatterns: LearningPatterns,
        predictionHorizon: Duration
    ): LearningPredictions {
        
        // 预测期望成绩
        val expectedGrade = predictExpectedGrade(studentId, learningPatterns, predictionHorizon)
        
        // 预测完成概率
        val completionProbability = predictCompletionProbability(studentId, learningPatterns)
        
        // 预测掌握度
        val masteryPredictions = predictMasteryLevels(studentId, learningPatterns, predictionHorizon)
        
        // 预测风险
        val riskPredictions = predictRisks(studentId, learningPatterns, predictionHorizon)
        
        // 生成推荐行动
        val recommendedActions = generateRecommendedActions(
            expectedGrade, completionProbability, riskPredictions
        )
        
        // 计算预测置信度
        val confidence = calculatePredictionConfidence(learningPatterns, predictionHorizon)
        
        return LearningPredictions(
            studentId = studentId,
            predictionHorizon = predictionHorizon,
            expectedGrade = expectedGrade,
            completionProbability = completionProbability,
            masteryPredictions = masteryPredictions,
            riskPredictions = riskPredictions,
            recommendedActions = recommendedActions,
            confidence = confidence,
            generatedAt = Clock.System.now()
        )
    }
    
    /**
     * 预测学习成果
     */
    suspend fun predictOutcomes(
        studentId: StudentId,
        courseId: CourseId,
        historicalData: HistoricalLearningData,
        currentPerformance: CurrentPerformance,
        predictionHorizon: Duration
    ): OutcomePredictions {
        
        // 基于历史数据的趋势分析
        val trendAnalysis = analyzeTrends(historicalData)
        
        // 当前表现评估
        val performanceAssessment = assessCurrentPerformance(currentPerformance)
        
        // 预测期望成绩
        val expectedGrade = predictGradeBasedOnTrends(
            trendAnalysis, performanceAssessment, predictionHorizon
        )
        
        // 预测完成概率
        val completionProbability = predictCompletionBasedOnEngagement(
            historicalData.engagementMetrics, currentPerformance.engagementLevel
        )
        
        // 识别风险因素
        val riskFactors = identifyRiskFactors(historicalData, currentPerformance)
        
        // 生成推荐行动
        val recommendedActions = generateOutcomeBasedActions(
            expectedGrade, completionProbability, riskFactors
        )
        
        // 计算置信度
        val confidence = calculateOutcomePredictionConfidence(
            historicalData, currentPerformance, predictionHorizon
        )
        
        return OutcomePredictions(
            expectedGrade = expectedGrade,
            completionProbability = completionProbability,
            identifiedRisks = riskFactors,
            recommendedActions = recommendedActions,
            confidence = confidence
        )
    }
    
    /**
     * 预测学习轨迹
     */
    suspend fun predictLearningTrajectory(
        studentId: StudentId,
        currentState: LearningState,
        targetGoals: List<LearningGoal>,
        timeframe: Duration
    ): LearningTrajectoryPrediction {
        
        // 分析当前学习状态
        val stateAnalysis = analyzeCurrentState(currentState)
        
        // 评估目标可达性
        val goalFeasibility = assessGoalFeasibility(currentState, targetGoals, timeframe)
        
        // 预测学习路径
        val predictedPath = predictOptimalPath(currentState, targetGoals, timeframe)
        
        // 识别关键里程碑
        val milestones = identifyKeyMilestones(predictedPath, targetGoals)
        
        // 预测潜在障碍
        val potentialObstacles = predictObstacles(currentState, predictedPath)
        
        // 生成适应性建议
        val adaptiveRecommendations = generateAdaptiveRecommendations(
            stateAnalysis, goalFeasibility, potentialObstacles
        )
        
        return LearningTrajectoryPrediction(
            studentId = studentId,
            currentState = currentState,
            targetGoals = targetGoals,
            predictedPath = predictedPath,
            milestones = milestones,
            potentialObstacles = potentialObstacles,
            adaptiveRecommendations = adaptiveRecommendations,
            feasibilityScore = goalFeasibility.overallFeasibility,
            confidence = calculateTrajectoryConfidence(stateAnalysis, goalFeasibility),
            generatedAt = Clock.System.now()
        )
    }
    
    // 预测方法实现
    
    private fun predictExpectedGrade(
        studentId: StudentId,
        patterns: LearningPatterns,
        horizon: Duration
    ): Double {
        // 基于学习模式预测成绩
        val baseGrade = 75.0 // 基础分数
        
        // 根据模式强度调整
        val patternBonus = patterns.patternStrength * 15.0
        
        // 根据一致性调整
        val consistencyBonus = patterns.patternConsistency * 10.0
        
        // 时间因素调整
        val timeAdjustment = calculateTimeAdjustment(horizon)
        
        return kotlin.math.min(100.0, baseGrade + patternBonus + consistencyBonus + timeAdjustment)
    }
    
    private fun predictCompletionProbability(
        studentId: StudentId,
        patterns: LearningPatterns
    ): Double {
        // 基于学习模式预测完成概率
        val baseProb = 0.7
        
        // 根据时间模式调整
        val temporalBonus = patterns.temporalPatterns.map { it.effectiveness }.average() * 0.2
        
        // 根据行为模式调整
        val behavioralBonus = patterns.behavioralPatterns
            .filter { it.impact == BehaviorImpact.POSITIVE }
            .size * 0.05
        
        return kotlin.math.min(1.0, baseProb + temporalBonus + behavioralBonus)
    }
    
    private fun predictMasteryLevels(
        studentId: StudentId,
        patterns: LearningPatterns,
        horizon: Duration
    ): Map<String, Double> {
        // 预测各知识点的掌握度
        return mapOf(
            "基础概念" to 0.85,
            "应用技能" to 0.78,
            "高级理论" to 0.65,
            "实践能力" to 0.72,
            "综合运用" to 0.68
        )
    }
    
    private fun predictRisks(
        studentId: StudentId,
        patterns: LearningPatterns,
        horizon: Duration
    ): List<RiskPrediction> {
        val risks = mutableListOf<RiskPrediction>()
        
        // 基于模式一致性预测风险
        if (patterns.patternConsistency < 0.6) {
            risks.add(
                RiskPrediction(
                    riskType = RiskType.DISENGAGEMENT,
                    probability = 0.4,
                    severity = RiskSeverity.MEDIUM,
                    timeframe = Duration.parse("P2W"),
                    mitigationStrategies = listOf("增加互动内容", "调整学习节奏")
                )
            )
        }
        
        // 基于表现模式预测风险
        val volatilePatterns = patterns.performancePatterns.filter { it.volatility > 0.3 }
        if (volatilePatterns.isNotEmpty()) {
            risks.add(
                RiskPrediction(
                    riskType = RiskType.ACADEMIC_FAILURE,
                    probability = 0.25,
                    severity = RiskSeverity.HIGH,
                    timeframe = Duration.parse("P1M"),
                    mitigationStrategies = listOf("加强基础训练", "提供额外支持")
                )
            )
        }
        
        return risks
    }
    
    private fun generateRecommendedActions(
        expectedGrade: Double,
        completionProbability: Double,
        risks: List<RiskPrediction>
    ): List<String> {
        val actions = mutableListOf<String>()
        
        if (expectedGrade < 70) {
            actions.add("加强基础知识学习")
            actions.add("增加练习时间")
        }
        
        if (completionProbability < 0.7) {
            actions.add("制定详细学习计划")
            actions.add("寻求学习支持")
        }
        
        if (risks.any { it.severity == RiskSeverity.HIGH }) {
            actions.add("立即寻求帮助")
            actions.add("调整学习策略")
        }
        
        return actions
    }
    
    private fun calculatePredictionConfidence(
        patterns: LearningPatterns,
        horizon: Duration
    ): Double {
        val patternReliability = patterns.patternConsistency
        val dataQuality = patterns.patternStrength
        val timeDecay = calculateTimeDecay(horizon)
        
        return (patternReliability + dataQuality) / 2 * timeDecay
    }
    
    // 趋势分析方法
    
    private fun analyzeTrends(historicalData: HistoricalLearningData): TrendAnalysis {
        val gradesTrend = analyzeGradesTrend(historicalData.pastGrades)
        val engagementTrend = analyzeEngagementTrend(historicalData.engagementMetrics)
        val velocityTrend = analyzeVelocityTrend(historicalData.learningVelocity)
        
        return TrendAnalysis(
            gradesTrend = gradesTrend,
            engagementTrend = engagementTrend,
            velocityTrend = velocityTrend,
            overallTrend = calculateOverallTrend(gradesTrend, engagementTrend, velocityTrend)
        )
    }
    
    private fun assessCurrentPerformance(performance: CurrentPerformance): PerformanceAssessment {
        return PerformanceAssessment(
            gradeLevel = categorizeGrade(performance.currentGrade),
            trendDirection = performance.recentTrend,
            engagementLevel = performance.engagementLevel,
            masteryLevel = performance.masteryLevel,
            riskLevel = assessPerformanceRisk(performance)
        )
    }
    
    private fun predictGradeBasedOnTrends(
        trends: TrendAnalysis,
        performance: PerformanceAssessment,
        horizon: Duration
    ): Double {
        val basePrediction = when (trends.overallTrend) {
            PerformanceTrend.IMPROVING -> performance.currentGrade + 5.0
            PerformanceTrend.DECLINING -> performance.currentGrade - 3.0
            PerformanceTrend.STABLE -> performance.currentGrade
            PerformanceTrend.VOLATILE -> performance.currentGrade + kotlin.random.Random.nextDouble(-2.0, 2.0)
            PerformanceTrend.CYCLICAL -> performance.currentGrade + 1.0
        }
        
        return kotlin.math.max(0.0, kotlin.math.min(100.0, basePrediction))
    }
    
    private fun predictCompletionBasedOnEngagement(
        engagementMetrics: EngagementMetrics,
        currentEngagement: EngagementLevel
    ): Double {
        val baseProb = when (currentEngagement) {
            EngagementLevel.VERY_HIGH -> 0.95
            EngagementLevel.HIGH -> 0.85
            EngagementLevel.MEDIUM -> 0.7
            EngagementLevel.LOW -> 0.5
            EngagementLevel.VERY_LOW -> 0.3
        }
        
        val completionRateBonus = engagementMetrics.completionRate * 0.2
        
        return kotlin.math.min(1.0, baseProb + completionRateBonus)
    }
    
    // 辅助方法
    
    private fun calculateTimeAdjustment(horizon: Duration): Double {
        val days = horizon.inWholeDays
        return when {
            days <= 7 -> 2.0
            days <= 30 -> 0.0
            days <= 90 -> -2.0
            else -> -5.0
        }
    }
    
    private fun calculateTimeDecay(horizon: Duration): Double {
        val days = horizon.inWholeDays
        return when {
            days <= 7 -> 0.95
            days <= 30 -> 0.85
            days <= 90 -> 0.7
            else -> 0.5
        }
    }
    
    private fun analyzeGradesTrend(grades: List<Double>): PerformanceTrend {
        if (grades.size < 2) return PerformanceTrend.STABLE
        
        val recent = grades.takeLast(3)
        val earlier = grades.dropLast(3).takeLast(3)
        
        if (recent.isEmpty() || earlier.isEmpty()) return PerformanceTrend.STABLE
        
        val recentAvg = recent.average()
        val earlierAvg = earlier.average()
        
        return when {
            recentAvg > earlierAvg + 2 -> PerformanceTrend.IMPROVING
            recentAvg < earlierAvg - 2 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }
    
    private fun analyzeEngagementTrend(metrics: EngagementMetrics): PerformanceTrend {
        // 简化实现
        return when {
            metrics.completionRate > 0.8 -> PerformanceTrend.IMPROVING
            metrics.completionRate < 0.6 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }
    
    private fun analyzeVelocityTrend(velocity: Double): PerformanceTrend {
        return when {
            velocity > 0.8 -> PerformanceTrend.IMPROVING
            velocity < 0.6 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }
    
    private fun calculateOverallTrend(
        gradesTrend: PerformanceTrend,
        engagementTrend: PerformanceTrend,
        velocityTrend: PerformanceTrend
    ): PerformanceTrend {
        val trends = listOf(gradesTrend, engagementTrend, velocityTrend)
        val improvingCount = trends.count { it == PerformanceTrend.IMPROVING }
        val decliningCount = trends.count { it == PerformanceTrend.DECLINING }
        
        return when {
            improvingCount >= 2 -> PerformanceTrend.IMPROVING
            decliningCount >= 2 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }
    
    private fun categorizeGrade(grade: Double): String {
        return when {
            grade >= 90 -> "优秀"
            grade >= 80 -> "良好"
            grade >= 70 -> "中等"
            grade >= 60 -> "及格"
            else -> "不及格"
        }
    }
    
    private fun assessPerformanceRisk(performance: CurrentPerformance): RiskLevel {
        return when {
            performance.currentGrade < 60 -> RiskLevel.HIGH
            performance.currentGrade < 70 && performance.recentTrend == PerformanceTrend.DECLINING -> RiskLevel.MODERATE
            performance.engagementLevel == EngagementLevel.VERY_LOW -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
    }
    
    private fun identifyRiskFactors(
        historicalData: HistoricalLearningData,
        currentPerformance: CurrentPerformance
    ): List<String> {
        val risks = mutableListOf<String>()
        
        if (currentPerformance.currentGrade < 70) {
            risks.add("成绩偏低")
        }
        
        if (currentPerformance.engagementLevel == EngagementLevel.LOW) {
            risks.add("参与度不足")
        }
        
        if (historicalData.engagementMetrics.completionRate < 0.7) {
            risks.add("完成率偏低")
        }
        
        return risks
    }
    
    private fun generateOutcomeBasedActions(
        expectedGrade: Double,
        completionProbability: Double,
        riskFactors: List<String>
    ): List<String> {
        val actions = mutableListOf<String>()
        
        if (expectedGrade < 75) {
            actions.add("加强重点知识复习")
        }
        
        if (completionProbability < 0.8) {
            actions.add("制定详细时间计划")
        }
        
        if (riskFactors.contains("参与度不足")) {
            actions.add("增加互动学习活动")
        }
        
        return actions
    }
    
    private fun calculateOutcomePredictionConfidence(
        historicalData: HistoricalLearningData,
        currentPerformance: CurrentPerformance,
        horizon: Duration
    ): Double {
        val dataQuality = if (historicalData.pastGrades.size >= 5) 0.9 else 0.7
        val performanceStability = if (currentPerformance.recentTrend == PerformanceTrend.STABLE) 0.8 else 0.6
        val timeReliability = calculateTimeDecay(horizon)
        
        return (dataQuality + performanceStability + timeReliability) / 3
    }
    
    // 简化的轨迹预测方法
    
    private fun analyzeCurrentState(state: LearningState): StateAnalysis {
        return StateAnalysis(
            competencyLevel = state.overallCompetency,
            motivationLevel = state.motivation,
            resourceAvailability = state.availableTime,
            supportLevel = state.supportAccess
        )
    }
    
    private fun assessGoalFeasibility(
        currentState: LearningState,
        goals: List<LearningGoal>,
        timeframe: Duration
    ): GoalFeasibility {
        val feasibilityScores = goals.map { goal ->
            assessIndividualGoalFeasibility(currentState, goal, timeframe)
        }
        
        return GoalFeasibility(
            individualFeasibility = feasibilityScores,
            overallFeasibility = feasibilityScores.average(),
            criticalConstraints = identifyCriticalConstraints(currentState, goals, timeframe)
        )
    }
    
    private fun predictOptimalPath(
        currentState: LearningState,
        goals: List<LearningGoal>,
        timeframe: Duration
    ): LearningPath {
        // 简化的路径预测
        return LearningPath(
            steps = goals.map { goal ->
                LearningStep(
                    goalId = goal.id,
                    estimatedDuration = timeframe.div(goals.size),
                    prerequisites = emptyList(),
                    resources = listOf("在线课程", "练习题", "项目实践")
                )
            },
            totalDuration = timeframe,
            difficulty = DifficultyLevel.INTERMEDIATE
        )
    }
    
    private fun identifyKeyMilestones(
        path: LearningPath,
        goals: List<LearningGoal>
    ): List<Milestone> {
        return goals.mapIndexed { index, goal ->
            Milestone(
                id = "milestone_${index + 1}",
                description = "完成${goal.description}",
                targetDate = Clock.System.now().plus(path.totalDuration.div(index + 1)),
                successCriteria = listOf("掌握核心概念", "通过相关测试")
            )
        }
    }
    
    private fun predictObstacles(
        currentState: LearningState,
        path: LearningPath
    ): List<PotentialObstacle> {
        return listOf(
            PotentialObstacle(
                type = "时间不足",
                probability = 0.3,
                impact = "延迟完成",
                mitigation = "优化时间管理"
            ),
            PotentialObstacle(
                type = "难度过高",
                probability = 0.2,
                impact = "理解困难",
                mitigation = "寻求额外支持"
            )
        )
    }
    
    private fun generateAdaptiveRecommendations(
        stateAnalysis: StateAnalysis,
        goalFeasibility: GoalFeasibility,
        obstacles: List<PotentialObstacle>
    ): List<AdaptiveRecommendation> {
        return listOf(
            AdaptiveRecommendation(
                type = "学习策略调整",
                description = "根据当前状态优化学习方法",
                priority = Priority.HIGH,
                implementation = "采用间隔重复学习法"
            ),
            AdaptiveRecommendation(
                type = "目标调整",
                description = "基于可行性分析调整目标",
                priority = Priority.MEDIUM,
                implementation = "分阶段实现长期目标"
            )
        )
    }
    
    private fun calculateTrajectoryConfidence(
        stateAnalysis: StateAnalysis,
        goalFeasibility: GoalFeasibility
    ): Double {
        return (stateAnalysis.competencyLevel + goalFeasibility.overallFeasibility) / 2
    }
    
    private fun assessIndividualGoalFeasibility(
        currentState: LearningState,
        goal: LearningGoal,
        timeframe: Duration
    ): Double {
        // 简化的可行性评估
        return 0.8
    }
    
    private fun identifyCriticalConstraints(
        currentState: LearningState,
        goals: List<LearningGoal>,
        timeframe: Duration
    ): List<String> {
        return listOf("时间限制", "基础知识不足", "学习资源有限")
    }
}

// 辅助数据类

@Serializable
data class OutcomePredictions(
    val expectedGrade: Double,
    val completionProbability: Double,
    val identifiedRisks: List<String>,
    val recommendedActions: List<String>,
    val confidence: Double
)

@Serializable
data class TrendAnalysis(
    val gradesTrend: PerformanceTrend,
    val engagementTrend: PerformanceTrend,
    val velocityTrend: PerformanceTrend,
    val overallTrend: PerformanceTrend
)

@Serializable
data class PerformanceAssessment(
    val gradeLevel: String,
    val trendDirection: PerformanceTrend,
    val engagementLevel: EngagementLevel,
    val masteryLevel: Double,
    val riskLevel: RiskLevel,
    val currentGrade: Double = 0.0
)

@Serializable
data class LearningTrajectoryPrediction(
    val studentId: StudentId,
    val currentState: LearningState,
    val targetGoals: List<LearningGoal>,
    val predictedPath: LearningPath,
    val milestones: List<Milestone>,
    val potentialObstacles: List<PotentialObstacle>,
    val adaptiveRecommendations: List<AdaptiveRecommendation>,
    val feasibilityScore: Double,
    val confidence: Double,
    val generatedAt: Instant
)

@Serializable
data class LearningState(
    val overallCompetency: Double,
    val motivation: Double,
    val availableTime: Duration,
    val supportAccess: Double
)

@Serializable
data class LearningGoal(
    val id: String,
    val description: String,
    val targetLevel: Double,
    val priority: Priority
)

@Serializable
data class StateAnalysis(
    val competencyLevel: Double,
    val motivationLevel: Double,
    val resourceAvailability: Duration,
    val supportLevel: Double
)

@Serializable
data class GoalFeasibility(
    val individualFeasibility: List<Double>,
    val overallFeasibility: Double,
    val criticalConstraints: List<String>
)

@Serializable
data class LearningPath(
    val steps: List<LearningStep>,
    val totalDuration: Duration,
    val difficulty: DifficultyLevel
)

@Serializable
data class LearningStep(
    val goalId: String,
    val estimatedDuration: Duration,
    val prerequisites: List<String>,
    val resources: List<String>
)

@Serializable
data class Milestone(
    val id: String,
    val description: String,
    val targetDate: Instant,
    val successCriteria: List<String>
)

@Serializable
data class PotentialObstacle(
    val type: String,
    val probability: Double,
    val impact: String,
    val mitigation: String
)

@Serializable
data class AdaptiveRecommendation(
    val type: String,
    val description: String,
    val priority: Priority,
    val implementation: String
)
