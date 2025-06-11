package ai.kastrax.edutech.analytics

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 干预引擎
 * 
 * 负责生成学习干预建议和实时干预措施
 */
class InterventionEngine {
    
    /**
     * 生成干预建议
     */
    suspend fun generateInterventions(
        studentId: StudentId,
        learningPatterns: LearningPatterns,
        riskAssessment: RiskAssessmentResult
    ): List<InterventionRecommendation> {
        
        val interventions = mutableListOf<InterventionRecommendation>()
        
        // 基于风险等级生成干预
        when (riskAssessment.overallRiskLevel) {
            RiskLevel.CRITICAL -> {
                interventions.addAll(generateCriticalInterventions(studentId, riskAssessment))
            }
            RiskLevel.HIGH -> {
                interventions.addAll(generateHighRiskInterventions(studentId, riskAssessment))
            }
            RiskLevel.MODERATE -> {
                interventions.addAll(generateModerateRiskInterventions(studentId, riskAssessment))
            }
            RiskLevel.LOW -> {
                interventions.addAll(generateLowRiskInterventions(studentId, riskAssessment))
            }
            RiskLevel.MINIMAL -> {
                interventions.addAll(generatePreventiveInterventions(studentId, learningPatterns))
            }
        }
        
        // 基于学习模式生成个性化干预
        interventions.addAll(generatePatternBasedInterventions(studentId, learningPatterns))
        
        // 基于具体风险类型生成针对性干预
        riskAssessment.identifiedRisks.forEach { risk ->
            interventions.addAll(generateRiskSpecificInterventions(studentId, risk))
        }
        
        // 排序和优化干预建议
        return optimizeInterventions(interventions)
    }
    
    /**
     * 生成实时干预
     */
    suspend fun generateRealTimeInterventions(
        studentId: StudentId,
        currentSession: LearningSession,
        immediateRisks: List<ImmediateRisk>
    ): List<RealTimeIntervention> {
        
        val interventions = mutableListOf<RealTimeIntervention>()
        
        immediateRisks.forEach { risk ->
            when (risk.riskType) {
                "低参与度" -> {
                    interventions.add(
                        RealTimeIntervention(
                            interventionType = "参与度提升",
                            message = "检测到您的参与度较低，建议尝试以下方法提升学习效果",
                            action = "切换到互动内容或进行短暂休息",
                            priority = Priority.HIGH
                        )
                    )
                }
                "进度缓慢" -> {
                    interventions.add(
                        RealTimeIntervention(
                            interventionType = "进度调整",
                            message = "当前学习进度较慢，建议调整学习策略",
                            action = "简化当前内容或寻求帮助",
                            priority = Priority.MEDIUM
                        )
                    )
                }
                "困难应对不足" -> {
                    interventions.add(
                        RealTimeIntervention(
                            interventionType = "难度调整",
                            message = "检测到您在当前内容上遇到困难",
                            action = "提供额外解释或降低难度",
                            priority = Priority.HIGH
                        )
                    )
                }
                "学习疲劳" -> {
                    interventions.add(
                        RealTimeIntervention(
                            interventionType = "疲劳管理",
                            message = "您已经学习了较长时间，建议适当休息",
                            action = "保存进度并休息10-15分钟",
                            priority = Priority.MEDIUM
                        )
                    )
                }
            }
        }
        
        return interventions
    }
    
    /**
     * 生成关键风险干预
     */
    private fun generateCriticalInterventions(
        studentId: StudentId,
        riskAssessment: RiskAssessmentResult
    ): List<InterventionRecommendation> {
        
        return listOf(
            InterventionRecommendation(
                interventionId = "critical_academic_support_${generateId()}",
                type = InterventionType.SUPPORT_PROVISION,
                description = "紧急学术支持干预",
                targetArea = "学术表现",
                urgency = Urgency.IMMEDIATE,
                expectedOutcome = "防止学术失败，提升成绩",
                implementationSteps = listOf(
                    "立即联系学习顾问",
                    "安排一对一辅导",
                    "制定紧急学习计划",
                    "提供额外学习资源",
                    "每日进度监控"
                ),
                successMetrics = listOf(
                    "成绩提升至及格线以上",
                    "学习参与度恢复",
                    "完成率达到80%以上"
                ),
                timeframe = Duration.parse("P7D")
            ),
            InterventionRecommendation(
                interventionId = "critical_motivation_boost_${generateId()}",
                type = InterventionType.MOTIVATION_BOOST,
                description = "紧急动机重建",
                targetArea = "学习动机",
                urgency = Urgency.IMMEDIATE,
                expectedOutcome = "重建学习信心和动机",
                implementationSteps = listOf(
                    "重新设定可达成的短期目标",
                    "提供即时成功体验",
                    "连接学习与个人价值",
                    "寻求同伴和导师支持"
                ),
                successMetrics = listOf(
                    "学习时间增加",
                    "主动参与度提升",
                    "完成任务积极性恢复"
                ),
                timeframe = Duration.parse("P3D")
            )
        )
    }
    
    /**
     * 生成高风险干预
     */
    private fun generateHighRiskInterventions(
        studentId: StudentId,
        riskAssessment: RiskAssessmentResult
    ): List<InterventionRecommendation> {
        
        return listOf(
            InterventionRecommendation(
                interventionId = "high_risk_content_adjustment_${generateId()}",
                type = InterventionType.CONTENT_ADJUSTMENT,
                description = "学习内容和难度调整",
                targetArea = "学习内容",
                urgency = Urgency.HIGH,
                expectedOutcome = "提高学习效果和理解度",
                implementationSteps = listOf(
                    "评估当前知识水平",
                    "调整内容难度和节奏",
                    "增加基础知识复习",
                    "提供多样化学习材料"
                ),
                successMetrics = listOf(
                    "理解度测试分数提升",
                    "学习满意度增加",
                    "错误率下降"
                ),
                timeframe = Duration.parse("P5D")
            ),
            InterventionRecommendation(
                interventionId = "high_risk_pacing_modification_${generateId()}",
                type = InterventionType.PACING_MODIFICATION,
                description = "学习节奏优化",
                targetArea = "学习节奏",
                urgency = Urgency.HIGH,
                expectedOutcome = "改善学习效率和可持续性",
                implementationSteps = listOf(
                    "分析当前学习模式",
                    "制定个性化时间表",
                    "设置合理的学习间隔",
                    "建立进度检查点"
                ),
                successMetrics = listOf(
                    "学习一致性提升",
                    "疲劳程度降低",
                    "长期坚持性改善"
                ),
                timeframe = Duration.parse("P7D")
            )
        )
    }
    
    /**
     * 生成中等风险干预
     */
    private fun generateModerateRiskInterventions(
        studentId: StudentId,
        riskAssessment: RiskAssessmentResult
    ): List<InterventionRecommendation> {
        
        return listOf(
            InterventionRecommendation(
                interventionId = "moderate_skill_building_${generateId()}",
                type = InterventionType.SKILL_BUILDING,
                description = "技能强化训练",
                targetArea = "学习技能",
                urgency = Urgency.MEDIUM,
                expectedOutcome = "提升学习技能和策略运用",
                implementationSteps = listOf(
                    "识别技能缺口",
                    "提供针对性训练",
                    "练习学习策略",
                    "定期技能评估"
                ),
                successMetrics = listOf(
                    "技能测试分数提升",
                    "学习策略运用频率增加",
                    "自主学习能力增强"
                ),
                timeframe = Duration.parse("P14D")
            ),
            InterventionRecommendation(
                interventionId = "moderate_feedback_enhancement_${generateId()}",
                type = InterventionType.FEEDBACK_ENHANCEMENT,
                description = "反馈机制优化",
                targetArea = "学习反馈",
                urgency = Urgency.MEDIUM,
                expectedOutcome = "提供更有效的学习指导",
                implementationSteps = listOf(
                    "增加反馈频率",
                    "提供具体改进建议",
                    "设置进度里程碑",
                    "建立反馈循环"
                ),
                successMetrics = listOf(
                    "学习方向更明确",
                    "改进行动执行率提升",
                    "学习满意度增加"
                ),
                timeframe = Duration.parse("P7D")
            )
        )
    }
    
    /**
     * 生成低风险干预
     */
    private fun generateLowRiskInterventions(
        studentId: StudentId,
        riskAssessment: RiskAssessmentResult
    ): List<InterventionRecommendation> {
        
        return listOf(
            InterventionRecommendation(
                interventionId = "low_risk_optimization_${generateId()}",
                type = InterventionType.CONTENT_ADJUSTMENT,
                description = "学习体验优化",
                targetArea = "学习体验",
                urgency = Urgency.LOW,
                expectedOutcome = "进一步提升学习效果",
                implementationSteps = listOf(
                    "分析学习偏好",
                    "优化内容呈现方式",
                    "增加个性化元素",
                    "提供进阶挑战"
                ),
                successMetrics = listOf(
                    "学习参与度维持",
                    "学习效率提升",
                    "学习满意度增加"
                ),
                timeframe = Duration.parse("P14D")
            )
        )
    }
    
    /**
     * 生成预防性干预
     */
    private fun generatePreventiveInterventions(
        studentId: StudentId,
        learningPatterns: LearningPatterns
    ): List<InterventionRecommendation> {
        
        return listOf(
            InterventionRecommendation(
                interventionId = "preventive_maintenance_${generateId()}",
                type = InterventionType.CONTENT_ADJUSTMENT,
                description = "预防性学习维护",
                targetArea = "学习维护",
                urgency = Urgency.LOW,
                expectedOutcome = "维持良好的学习状态",
                implementationSteps = listOf(
                    "定期学习状态检查",
                    "保持学习多样性",
                    "设置新的学习目标",
                    "维护学习动机"
                ),
                successMetrics = listOf(
                    "学习状态稳定",
                    "持续进步",
                    "学习兴趣保持"
                ),
                timeframe = Duration.parse("P1M")
            )
        )
    }
    
    /**
     * 基于学习模式生成干预
     */
    private fun generatePatternBasedInterventions(
        studentId: StudentId,
        patterns: LearningPatterns
    ): List<InterventionRecommendation> {
        
        val interventions = mutableListOf<InterventionRecommendation>()
        
        // 基于时间模式的干预
        if (patterns.temporalPatterns.any { it.effectiveness < 0.6 }) {
            interventions.add(
                InterventionRecommendation(
                    interventionId = "temporal_optimization_${generateId()}",
                    type = InterventionType.PACING_MODIFICATION,
                    description = "时间模式优化",
                    targetArea = "学习时间安排",
                    urgency = Urgency.MEDIUM,
                    expectedOutcome = "提高时间利用效率",
                    implementationSteps = listOf(
                        "分析最佳学习时间",
                        "调整学习时间表",
                        "优化学习时长",
                        "建立时间管理习惯"
                    ),
                    successMetrics = listOf(
                        "学习效率提升",
                        "时间利用率增加",
                        "学习一致性改善"
                    ),
                    timeframe = Duration.parse("P7D")
                )
            )
        }
        
        // 基于行为模式的干预
        val negativePatterns = patterns.behavioralPatterns.filter { it.impact == BehaviorImpact.NEGATIVE }
        if (negativePatterns.isNotEmpty()) {
            interventions.add(
                InterventionRecommendation(
                    interventionId = "behavioral_correction_${generateId()}",
                    type = InterventionType.SKILL_BUILDING,
                    description = "行为模式纠正",
                    targetArea = "学习行为",
                    urgency = Urgency.MEDIUM,
                    expectedOutcome = "改善学习行为模式",
                    implementationSteps = listOf(
                        "识别负面行为模式",
                        "制定行为改进计划",
                        "建立正面行为习惯",
                        "监控行为变化"
                    ),
                    successMetrics = listOf(
                        "负面行为减少",
                        "正面行为增加",
                        "学习效果改善"
                    ),
                    timeframe = Duration.parse("P14D")
                )
            )
        }
        
        return interventions
    }
    
    /**
     * 基于特定风险生成干预
     */
    private fun generateRiskSpecificInterventions(
        studentId: StudentId,
        risk: IdentifiedRisk
    ): List<InterventionRecommendation> {
        
        return when (risk.type) {
            RiskType.ACADEMIC_FAILURE -> listOf(
                InterventionRecommendation(
                    interventionId = "academic_recovery_${generateId()}",
                    type = InterventionType.SUPPORT_PROVISION,
                    description = "学术恢复支持",
                    targetArea = "学术表现",
                    urgency = Urgency.HIGH,
                    expectedOutcome = "提升学术表现",
                    implementationSteps = listOf(
                        "诊断学习困难",
                        "提供针对性辅导",
                        "加强基础知识",
                        "增加练习机会"
                    ),
                    successMetrics = listOf("成绩提升", "理解度改善"),
                    timeframe = Duration.parse("P7D")
                )
            )
            RiskType.DISENGAGEMENT -> listOf(
                InterventionRecommendation(
                    interventionId = "engagement_recovery_${generateId()}",
                    type = InterventionType.MOTIVATION_BOOST,
                    description = "参与度恢复",
                    targetArea = "学习参与",
                    urgency = Urgency.MEDIUM,
                    expectedOutcome = "恢复学习参与度",
                    implementationSteps = listOf(
                        "增加互动内容",
                        "设置短期目标",
                        "提供即时反馈",
                        "引入竞争元素"
                    ),
                    successMetrics = listOf("参与时间增加", "互动频率提升"),
                    timeframe = Duration.parse("P5D")
                )
            )
            RiskType.TIME_MANAGEMENT -> listOf(
                InterventionRecommendation(
                    interventionId = "time_management_training_${generateId()}",
                    type = InterventionType.SKILL_BUILDING,
                    description = "时间管理训练",
                    targetArea = "时间管理",
                    urgency = Urgency.MEDIUM,
                    expectedOutcome = "改善时间管理能力",
                    implementationSteps = listOf(
                        "时间管理技能培训",
                        "制定学习计划",
                        "设置提醒机制",
                        "监控时间使用"
                    ),
                    successMetrics = listOf("计划执行率提升", "时间利用效率增加"),
                    timeframe = Duration.parse("P7D")
                )
            )
            else -> emptyList()
        }
    }
    
    /**
     * 优化干预建议
     */
    private fun optimizeInterventions(
        interventions: List<InterventionRecommendation>
    ): List<InterventionRecommendation> {
        
        // 去重
        val uniqueInterventions = interventions.distinctBy { it.type to it.targetArea }
        
        // 按紧急程度排序
        val sortedInterventions = uniqueInterventions.sortedWith(
            compareByDescending<InterventionRecommendation> { it.urgency.ordinal }
                .thenByDescending { it.type.ordinal }
        )
        
        // 限制数量，避免过多干预
        return sortedInterventions.take(5)
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString().take(8)
}
