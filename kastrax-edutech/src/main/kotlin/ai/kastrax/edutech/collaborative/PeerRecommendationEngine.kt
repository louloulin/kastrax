package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Week 23-24: 同伴学习推荐引擎
 * 
 * 功能：
 * - 智能匹配学习伙伴
 * - 推荐协作学习机会
 * - 分析学习互补性
 * - 提供个性化学习建议
 */
class PeerRecommendationEngine {
    
    private val profileService = CollaborativeProfileService()
    private val compatibilityCalculator = CompatibilityCalculator()
    private val learningAnalyzer = LearningPatternAnalyzer()
    
    /**
     * 生成同伴学习推荐
     */
    suspend fun generateRecommendations(
        studentId: StudentId,
        subject: Subject,
        topic: String
    ): List<PeerRecommendation> {
        val studentProfile = profileService.getCollaborativeProfile(studentId)
            ?: return emptyList()
        
        // 1. 获取潜在学习伙伴
        val potentialPeers = findPotentialPeers(studentId, subject, topic)
        
        // 2. 计算兼容性和互补性
        val peerAnalysis = analyzePeerCompatibility(studentProfile, potentialPeers)
        
        // 3. 生成不同类型的推荐
        val recommendations = mutableListOf<PeerRecommendation>()
        
        recommendations.addAll(generateStudyPartnerRecommendations(studentProfile, peerAnalysis))
        recommendations.addAll(generateMentorRecommendations(studentProfile, peerAnalysis))
        recommendations.addAll(generateMenteeRecommendations(studentProfile, peerAnalysis))
        recommendations.addAll(generateSkillExchangeRecommendations(studentProfile, peerAnalysis))
        recommendations.addAll(generateProjectPartnerRecommendations(studentProfile, peerAnalysis))
        
        // 4. 排序和过滤
        return recommendations
            .sortedByDescending { it.compatibilityScore }
            .take(10) // 返回前10个推荐
    }
    
    /**
     * 推荐协作学习机会
     */
    suspend fun recommendCollaborationOpportunities(
        studentId: StudentId,
        interests: List<String>,
        skillLevel: SkillLevel
    ): List<CollaborationOpportunity> {
        val opportunities = mutableListOf<CollaborationOpportunity>()
        
        // 1. 基于兴趣的学习小组
        opportunities.addAll(findInterestBasedGroups(studentId, interests))
        
        // 2. 技能互补的项目团队
        opportunities.addAll(findSkillComplementaryTeams(studentId, skillLevel))
        
        // 3. 同级别的学习圈
        opportunities.addAll(findPeerLearningCircles(studentId, skillLevel))
        
        // 4. 跨级别的导师关系
        opportunities.addAll(findMentorshipOpportunities(studentId, skillLevel))
        
        return opportunities.sortedByDescending { it.matchScore }
    }
    
    /**
     * 分析学习互补性
     */
    suspend fun analyzeLearningComplementarity(
        student1Id: StudentId,
        student2Id: StudentId
    ): LearningComplementarityAnalysis {
        val profile1 = profileService.getCollaborativeProfile(student1Id)
        val profile2 = profileService.getCollaborativeProfile(student2Id)
        
        if (profile1 == null || profile2 == null) {
            return LearningComplementarityAnalysis(
                overallComplementarity = 0.0,
                skillComplementarity = emptyMap(),
                styleComplementarity = 0.0,
                goalAlignment = 0.0,
                mutualBenefits = emptyList(),
                collaborationPotential = CollaborationPotential.LOW
            )
        }
        
        return LearningComplementarityAnalysis(
            overallComplementarity = calculateOverallComplementarity(profile1, profile2),
            skillComplementarity = analyzeSkillComplementarity(profile1, profile2),
            styleComplementarity = analyzeStyleComplementarity(profile1, profile2),
            goalAlignment = analyzeGoalAlignment(profile1, profile2),
            mutualBenefits = identifyMutualBenefits(profile1, profile2),
            collaborationPotential = assessCollaborationPotential(profile1, profile2)
        )
    }
    
    // ==================== 私有方法 ====================
    
    private suspend fun findPotentialPeers(
        studentId: StudentId,
        subject: Subject,
        topic: String
    ): List<CollaborativeProfile> {
        // 模拟获取潜在学习伙伴
        // 实际实现中应该从数据库查询
        return listOf(
            createSampleProfile("student_001"),
            createSampleProfile("student_002"),
            createSampleProfile("student_003"),
            createSampleProfile("student_004"),
            createSampleProfile("student_005")
        ).filter { it.studentId.value != studentId.value }
    }
    
    private fun createSampleProfile(studentIdValue: String): CollaborativeProfile {
        return CollaborativeProfile(
            studentId = StudentId(studentIdValue),
            collaborationStyle = CollaborationStyle(
                leadership = LeadershipStyle.values().random(),
                participation = ParticipationStyle.values().random(),
                conflictResolution = ConflictResolutionStyle.values().random(),
                workingStyle = WorkingStyle.values().random(),
                feedbackStyle = FeedbackStyle.values().random()
            ),
            communicationPreferences = CommunicationPreferences(
                preferredChannels = setOf(CommunicationChannel.TEXT_CHAT, CommunicationChannel.VIDEO_CALL),
                responseTimeExpectation = kotlin.time.Duration.parse("PT30M"),
                formalityLevel = FormalityLevel.SEMI_FORMAL,
                languagePreferences = listOf("zh-CN"),
                availabilityWindows = emptyList()
            ),
            skillContributions = mapOf(
                "数学" to SkillLevel.values().random(),
                "编程" to SkillLevel.values().random(),
                "写作" to SkillLevel.values().random()
            ),
            pastCollaborations = emptyList(),
            performanceMetrics = CollaborationMetrics(
                totalSessions = (1..20).random(),
                averagePerformance = (0.3..0.9).random(),
                preferredGroupSizeMin = 3,
                preferredGroupSizeMax = 5,
                successfulCollaborations = (0..15).random(),
                leadershipExperience = (0..5).random(),
                mentorshipExperience = (0..3).random(),
                conflictResolutionSuccess = (0.2..0.8).random(),
                peerRating = (0.4..0.9).random()
            ),
            preferences = StudentPreferences(
                preferredGroupSizeMin = 3,
                preferredGroupSizeMax = 5,
                preferredPartnerTypes = setOf(PartnerType.SIMILAR_SKILL, PartnerType.COMPLEMENTARY_SKILL),
                avoidedPartnerTypes = emptySet(),
                workingTimePreferences = emptyList(),
                subjectInterests = mapOf(
                    Subject.MATHEMATICS to InterestLevel.values().random(),
                    Subject.COMPUTER_SCIENCE to InterestLevel.values().random()
                ),
                collaborationGoals = listOf(CollaborationGoal.SKILL_DEVELOPMENT, CollaborationGoal.PEER_LEARNING)
            ),
            lastUpdated = Clock.System.now()
        )
    }
    
    private suspend fun analyzePeerCompatibility(
        studentProfile: CollaborativeProfile,
        potentialPeers: List<CollaborativeProfile>
    ): List<PeerCompatibilityAnalysis> {
        return potentialPeers.map { peerProfile ->
            val compatibility = compatibilityCalculator.calculateCompatibility(studentProfile, peerProfile)
            val complementarity = calculateComplementarity(studentProfile, peerProfile)
            val learningPotential = calculateLearningPotential(studentProfile, peerProfile)
            
            PeerCompatibilityAnalysis(
                peerProfile = peerProfile,
                compatibilityScore = compatibility,
                complementarityScore = complementarity,
                learningPotential = learningPotential,
                overallScore = (compatibility * 0.4 + complementarity * 0.4 + learningPotential * 0.2)
            )
        }
    }
    
    private fun calculateComplementarity(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Double {
        // 计算技能互补性
        val skillComplementarity = calculateSkillComplementarity(profile1, profile2).values.average()
        
        // 计算风格互补性
        val styleComplementarity = calculateStyleComplementarity(profile1, profile2)
        
        // 计算经验互补性
        val experienceComplementarity = calculateExperienceComplementarity(profile1, profile2)
        
        return (skillComplementarity + styleComplementarity + experienceComplementarity) / 3.0
    }
    
    private fun calculateLearningPotential(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Double {
        // 基于过往表现和学习目标计算学习潜力
        val performanceAlignment = 1.0 - abs(profile1.performanceMetrics.averagePerformance - 
                                           profile2.performanceMetrics.averagePerformance)
        
        val goalAlignment = profile1.preferences.collaborationGoals
            .intersect(profile2.preferences.collaborationGoals.toSet()).size.toDouble() /
            profile1.preferences.collaborationGoals.union(profile2.preferences.collaborationGoals).size
        
        return (performanceAlignment + goalAlignment) / 2.0
    }
    
    private fun generateStudyPartnerRecommendations(
        studentProfile: CollaborativeProfile,
        peerAnalysis: List<PeerCompatibilityAnalysis>
    ): List<PeerRecommendation> {
        return peerAnalysis
            .filter { it.compatibilityScore >= 0.6 }
            .take(3)
            .map { analysis ->
                PeerRecommendation(
                    recommendedPeerId = analysis.peerProfile.studentId,
                    reason = "高兼容性学习伙伴，适合日常学习交流",
                    compatibilityScore = analysis.compatibilityScore,
                    recommendationType = PeerRecommendationType.STUDY_PARTNER,
                    expectedBenefits = listOf(
                        "相似的学习风格便于协作",
                        "良好的沟通兼容性",
                        "共同的学习目标"
                    )
                )
            }
    }
    
    private fun generateMentorRecommendations(
        studentProfile: CollaborativeProfile,
        peerAnalysis: List<PeerCompatibilityAnalysis>
    ): List<PeerRecommendation> {
        return peerAnalysis
            .filter { it.peerProfile.performanceMetrics.averagePerformance > studentProfile.performanceMetrics.averagePerformance + 0.2 }
            .filter { it.peerProfile.performanceMetrics.mentorshipExperience > 0 }
            .take(2)
            .map { analysis ->
                PeerRecommendation(
                    recommendedPeerId = analysis.peerProfile.studentId,
                    reason = "经验丰富的学习导师，能提供指导和支持",
                    compatibilityScore = analysis.compatibilityScore,
                    recommendationType = PeerRecommendationType.MENTOR,
                    expectedBenefits = listOf(
                        "获得专业指导和建议",
                        "学习先进的学习方法",
                        "提升学习效率"
                    )
                )
            }
    }
    
    private fun generateMenteeRecommendations(
        studentProfile: CollaborativeProfile,
        peerAnalysis: List<PeerCompatibilityAnalysis>
    ): List<PeerRecommendation> {
        return peerAnalysis
            .filter { it.peerProfile.performanceMetrics.averagePerformance < studentProfile.performanceMetrics.averagePerformance - 0.2 }
            .filter { studentProfile.performanceMetrics.mentorshipExperience > 0 || 
                     studentProfile.preferences.collaborationGoals.contains(CollaborationGoal.LEADERSHIP_PRACTICE) }
            .take(2)
            .map { analysis ->
                PeerRecommendation(
                    recommendedPeerId = analysis.peerProfile.studentId,
                    reason = "适合指导的学习伙伴，有助于提升领导和教学能力",
                    compatibilityScore = analysis.compatibilityScore,
                    recommendationType = PeerRecommendationType.MENTEE,
                    expectedBenefits = listOf(
                        "锻炼领导和指导能力",
                        "通过教学加深理解",
                        "建立自信心"
                    )
                )
            }
    }
    
    private fun generateSkillExchangeRecommendations(
        studentProfile: CollaborativeProfile,
        peerAnalysis: List<PeerCompatibilityAnalysis>
    ): List<PeerRecommendation> {
        return peerAnalysis
            .filter { it.complementarityScore >= 0.7 }
            .take(2)
            .map { analysis ->
                PeerRecommendation(
                    recommendedPeerId = analysis.peerProfile.studentId,
                    reason = "技能互补的学习伙伴，适合技能交换学习",
                    compatibilityScore = analysis.compatibilityScore,
                    recommendationType = PeerRecommendationType.SKILL_EXCHANGE,
                    expectedBenefits = listOf(
                        "互相学习不同技能",
                        "扩展知识面",
                        "建立长期学习关系"
                    )
                )
            }
    }
    
    private fun generateProjectPartnerRecommendations(
        studentProfile: CollaborativeProfile,
        peerAnalysis: List<PeerCompatibilityAnalysis>
    ): List<PeerRecommendation> {
        return peerAnalysis
            .filter { it.overallScore >= 0.7 }
            .filter { it.peerProfile.preferences.collaborationGoals.contains(CollaborationGoal.PROJECT_COMPLETION) }
            .take(2)
            .map { analysis ->
                PeerRecommendation(
                    recommendedPeerId = analysis.peerProfile.studentId,
                    reason = "优秀的项目合作伙伴，适合共同完成学习项目",
                    compatibilityScore = analysis.compatibilityScore,
                    recommendationType = PeerRecommendationType.PROJECT_PARTNER,
                    expectedBenefits = listOf(
                        "高效的项目协作",
                        "优质的成果产出",
                        "丰富的项目经验"
                    )
                )
            }
    }
    
    private suspend fun findInterestBasedGroups(
        studentId: StudentId,
        interests: List<String>
    ): List<CollaborationOpportunity> {
        // 模拟查找基于兴趣的学习小组
        return listOf(
            CollaborationOpportunity(
                id = "group_math_001",
                title = "数学学习小组",
                description = "专注于高等数学学习的小组",
                type = CollaborativeSessionType.STUDY_GROUP,
                matchScore = 0.85,
                expectedBenefits = listOf("深入学习数学概念", "解决难题", "准备考试"),
                requirements = listOf("基础数学知识", "每周投入3小时"),
                currentMembers = 4,
                maxMembers = 8
            )
        )
    }
    
    private suspend fun findSkillComplementaryTeams(
        studentId: StudentId,
        skillLevel: SkillLevel
    ): List<CollaborationOpportunity> {
        return listOf(
            CollaborationOpportunity(
                id = "team_project_001",
                title = "编程项目团队",
                description = "开发学习管理系统的项目团队",
                type = CollaborativeSessionType.GROUP_PROJECT,
                matchScore = 0.78,
                expectedBenefits = listOf("实践编程技能", "团队协作经验", "完整项目经历"),
                requirements = listOf("基础编程能力", "团队合作精神"),
                currentMembers = 3,
                maxMembers = 6
            )
        )
    }
    
    private suspend fun findPeerLearningCircles(
        studentId: StudentId,
        skillLevel: SkillLevel
    ): List<CollaborationOpportunity> {
        return listOf(
            CollaborationOpportunity(
                id = "circle_peer_001",
                title = "同伴学习圈",
                description = "同水平学生的互助学习圈",
                type = CollaborativeSessionType.PEER_LEARNING,
                matchScore = 0.72,
                expectedBenefits = listOf("同伴支持", "共同进步", "学习动力"),
                requirements = listOf("积极参与", "互助精神"),
                currentMembers = 5,
                maxMembers = 10
            )
        )
    }
    
    private suspend fun findMentorshipOpportunities(
        studentId: StudentId,
        skillLevel: SkillLevel
    ): List<CollaborationOpportunity> {
        return listOf(
            CollaborationOpportunity(
                id = "mentor_program_001",
                title = "导师计划",
                description = "经验丰富的学生指导新手的计划",
                type = CollaborativeSessionType.PEER_LEARNING,
                matchScore = 0.68,
                expectedBenefits = listOf("专业指导", "经验分享", "快速成长"),
                requirements = listOf("学习积极性", "接受指导"),
                currentMembers = 2,
                maxMembers = 4
            )
        )
    }
    
    private fun calculateOverallComplementarity(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Double {
        val skillComp = calculateSkillComplementarity(profile1, profile2).values.average()
        val styleComp = calculateStyleComplementarity(profile1, profile2)
        val goalAlign = analyzeGoalAlignment(profile1, profile2)
        
        return (skillComp + styleComp + goalAlign) / 3.0
    }
    
    private fun calculateSkillComplementarity(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Map<String, Double> {
        val allSkills = profile1.skillContributions.keys.union(profile2.skillContributions.keys)
        
        return allSkills.associateWith { skill ->
            val skill1 = profile1.skillContributions[skill]?.ordinal?.toDouble() ?: 0.0
            val skill2 = profile2.skillContributions[skill]?.ordinal?.toDouble() ?: 0.0
            
            // 互补性：技能差异越大，互补性越高
            1.0 - abs(skill1 - skill2) / 4.0 // 假设技能等级0-4
        }
    }
    
    private fun calculateStyleComplementarity(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Double {
        // 分析协作风格的互补性
        val style1 = profile1.collaborationStyle
        val style2 = profile2.collaborationStyle
        
        val leadershipComp = if (style1.leadership != style2.leadership) 0.8 else 0.4
        val participationComp = if (style1.participation != style2.participation) 0.7 else 0.3
        val workingComp = if (style1.workingStyle != style2.workingStyle) 0.6 else 0.2
        
        return (leadershipComp + participationComp + workingComp) / 3.0
    }
    
    private fun calculateExperienceComplementarity(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Double {
        val exp1 = profile1.performanceMetrics.totalSessions
        val exp2 = profile2.performanceMetrics.totalSessions
        
        val expDiff = abs(exp1 - exp2).toDouble()
        val maxExp = maxOf(exp1, exp2).toDouble()
        
        return if (maxExp > 0) expDiff / maxExp else 0.0
    }
    
    private fun analyzeGoalAlignment(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): Double {
        val goals1 = profile1.preferences.collaborationGoals.toSet()
        val goals2 = profile2.preferences.collaborationGoals.toSet()
        
        val intersection = goals1.intersect(goals2).size
        val union = goals1.union(goals2).size
        
        return if (union > 0) intersection.toDouble() / union else 0.0
    }
    
    private fun identifyMutualBenefits(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): List<String> {
        val benefits = mutableListOf<String>()
        
        // 基于技能互补识别互惠利益
        val skillComp = calculateSkillComplementarity(profile1, profile2)
        skillComp.forEach { (skill, comp) ->
            if (comp > 0.6) {
                benefits.add("在${skill}方面互相学习")
            }
        }
        
        // 基于经验差异识别学习机会
        val exp1 = profile1.performanceMetrics.totalSessions
        val exp2 = profile2.performanceMetrics.totalSessions
        
        if (exp1 > exp2 + 5) {
            benefits.add("经验分享和指导机会")
        } else if (exp2 > exp1 + 5) {
            benefits.add("获得经验指导")
        }
        
        return benefits
    }
    
    private fun assessCollaborationPotential(
        profile1: CollaborativeProfile,
        profile2: CollaborativeProfile
    ): CollaborationPotential {
        val compatibility = compatibilityCalculator.calculateCompatibility(profile1, profile2)
        val complementarity = calculateComplementarity(profile1, profile2)
        val overallScore = (compatibility + complementarity) / 2.0
        
        return when {
            overallScore >= 0.8 -> CollaborationPotential.VERY_HIGH
            overallScore >= 0.6 -> CollaborationPotential.HIGH
            overallScore >= 0.4 -> CollaborationPotential.MEDIUM
            overallScore >= 0.2 -> CollaborationPotential.LOW
            else -> CollaborationPotential.VERY_LOW
        }
    }
}

// ==================== 数据模型 ====================

@Serializable
data class PeerCompatibilityAnalysis(
    val peerProfile: CollaborativeProfile,
    val compatibilityScore: Double,
    val complementarityScore: Double,
    val learningPotential: Double,
    val overallScore: Double
)

@Serializable
data class CollaborationOpportunity(
    val id: String,
    val title: String,
    val description: String,
    val type: CollaborativeSessionType,
    val matchScore: Double,
    val expectedBenefits: List<String>,
    val requirements: List<String>,
    val currentMembers: Int,
    val maxMembers: Int
)

@Serializable
data class LearningComplementarityAnalysis(
    val overallComplementarity: Double,
    val skillComplementarity: Map<String, Double>,
    val styleComplementarity: Double,
    val goalAlignment: Double,
    val mutualBenefits: List<String>,
    val collaborationPotential: CollaborationPotential
)

@Serializable
enum class CollaborationPotential {
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

/**
 * 学习模式分析器
 */
class LearningPatternAnalyzer {
    
    /**
     * 分析学习模式
     */
    fun analyzeLearningPattern(profile: CollaborativeProfile): LearningPattern {
        return LearningPattern(
            preferredLearningStyle = identifyPreferredLearningStyle(profile),
            collaborationPreference = analyzeCollaborationPreference(profile),
            communicationStyle = analyzeCommunicationStyle(profile),
            motivationFactors = identifyMotivationFactors(profile)
        )
    }
    
    private fun identifyPreferredLearningStyle(profile: CollaborativeProfile): LearningStyle {
        // 基于协作风格和偏好识别学习风格
        return when {
            profile.collaborationStyle.workingStyle == WorkingStyle.DETAIL_ORIENTED -> LearningStyle.ANALYTICAL
            profile.collaborationStyle.workingStyle == WorkingStyle.BIG_PICTURE -> LearningStyle.GLOBAL
            profile.collaborationStyle.participation == ParticipationStyle.HIGHLY_ACTIVE -> LearningStyle.ACTIVE
            else -> LearningStyle.REFLECTIVE
        }
    }
    
    private fun analyzeCollaborationPreference(profile: CollaborativeProfile): CollaborationPreference {
        val groupSize = profile.preferences.preferredGroupSize
        return when {
            groupSize.last <= 3 -> CollaborationPreference.SMALL_GROUP
            groupSize.first >= 6 -> CollaborationPreference.LARGE_GROUP
            else -> CollaborationPreference.MEDIUM_GROUP
        }
    }
    
    private fun analyzeCommunicationStyle(profile: CollaborativeProfile): CommunicationStyle {
        return when (profile.communicationPreferences.formalityLevel) {
            FormalityLevel.VERY_FORMAL, FormalityLevel.FORMAL -> CommunicationStyle.FORMAL
            FormalityLevel.VERY_INFORMAL, FormalityLevel.INFORMAL -> CommunicationStyle.CASUAL
            else -> CommunicationStyle.BALANCED
        }
    }
    
    private fun identifyMotivationFactors(profile: CollaborativeProfile): List<MotivationFactor> {
        val factors = mutableListOf<MotivationFactor>()
        
        profile.preferences.collaborationGoals.forEach { goal ->
            when (goal) {
                CollaborationGoal.SKILL_DEVELOPMENT -> factors.add(MotivationFactor.SKILL_IMPROVEMENT)
                CollaborationGoal.KNOWLEDGE_SHARING -> factors.add(MotivationFactor.KNOWLEDGE_SHARING)
                CollaborationGoal.PEER_LEARNING -> factors.add(MotivationFactor.SOCIAL_LEARNING)
                CollaborationGoal.LEADERSHIP_PRACTICE -> factors.add(MotivationFactor.LEADERSHIP_DEVELOPMENT)
                else -> factors.add(MotivationFactor.ACHIEVEMENT)
            }
        }
        
        return factors.distinct()
    }
}

@Serializable
data class LearningPattern(
    val preferredLearningStyle: LearningStyle,
    val collaborationPreference: CollaborationPreference,
    val communicationStyle: CommunicationStyle,
    val motivationFactors: List<MotivationFactor>
)

@Serializable
enum class LearningStyle {
    ANALYTICAL,     // 分析型
    GLOBAL,         // 整体型
    ACTIVE,         // 主动型
    REFLECTIVE      // 反思型
}

@Serializable
enum class CollaborationPreference {
    SMALL_GROUP,    // 小组偏好
    MEDIUM_GROUP,   // 中等组偏好
    LARGE_GROUP     // 大组偏好
}

@Serializable
enum class CommunicationStyle {
    FORMAL,         // 正式
    CASUAL,         // 随意
    BALANCED        // 平衡
}

@Serializable
enum class MotivationFactor {
    SKILL_IMPROVEMENT,      // 技能提升
    KNOWLEDGE_SHARING,      // 知识分享
    SOCIAL_LEARNING,        // 社交学习
    LEADERSHIP_DEVELOPMENT, // 领导力发展
    ACHIEVEMENT            // 成就感
}
