package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.math.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Week 23-24: 智能小组匹配器
 * 
 * 功能：
 * - 基于多维度算法的智能分组
 * - 考虑学习风格、技能水平、协作历史
 * - 优化小组兼容性和互补性
 * - 支持多种分组策略
 */
class IntelligentGroupMatcher {
    
    private val profileService = CollaborativeProfileService()
    private val compatibilityCalculator = CompatibilityCalculator()
    private val groupOptimizer = GroupOptimizer()
    
    /**
     * 执行智能小组匹配
     */
    suspend fun matchGroups(request: GroupMatchingRequest): GroupMatchingResult {
        return try {
            // 1. 获取参与者的协作档案
            val profiles = getParticipantProfiles(request.participants)
            
            // 2. 计算兼容性矩阵
            val compatibilityMatrix = calculateCompatibilityMatrix(profiles)
            
            // 3. 根据策略生成候选分组
            val candidateGroupings = generateCandidateGroupings(
                request, profiles, compatibilityMatrix
            )
            
            // 4. 评估和优化分组
            val optimizedGrouping = optimizeGrouping(
                candidateGroupings, request.preferences, compatibilityMatrix
            )
            
            // 5. 生成分组结果
            GroupMatchingResult(
                requestId = generateRequestId(),
                groups = optimizedGrouping.groups,
                matchingScore = optimizedGrouping.score,
                explanation = generateExplanation(optimizedGrouping, request),
                alternatives = generateAlternatives(candidateGroupings, optimizedGrouping),
                generatedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            throw GroupMatchingException("Failed to match groups: ${e.message}", e)
        }
    }
    
    /**
     * 获取参与者协作档案
     */
    private suspend fun getParticipantProfiles(participants: List<StudentId>): Map<StudentId, CollaborativeProfile> {
        return participants.associateWith { studentId ->
            profileService.getCollaborativeProfile(studentId)
                ?: createDefaultProfile(studentId)
        }
    }
    
    /**
     * 计算兼容性矩阵
     */
    private suspend fun calculateCompatibilityMatrix(
        profiles: Map<StudentId, CollaborativeProfile>
    ): Map<Pair<StudentId, StudentId>, Double> {
        val matrix = mutableMapOf<Pair<StudentId, StudentId>, Double>()
        val studentIds = profiles.keys.toList()
        
        for (i in studentIds.indices) {
            for (j in i + 1 until studentIds.size) {
                val student1 = studentIds[i]
                val student2 = studentIds[j]
                val profile1 = profiles[student1]!!
                val profile2 = profiles[student2]!!
                
                val compatibility = compatibilityCalculator.calculateCompatibility(profile1, profile2)
                
                matrix[Pair(student1, student2)] = compatibility
                matrix[Pair(student2, student1)] = compatibility
            }
        }
        
        return matrix
    }
    
    /**
     * 生成候选分组方案
     */
    private suspend fun generateCandidateGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>,
        compatibilityMatrix: Map<Pair<StudentId, StudentId>, Double>
    ): List<GroupingCandidate> {
        val candidates = mutableListOf<GroupingCandidate>()
        
        // 根据不同策略生成候选方案
        when (request.groupConfiguration.groupingStrategy) {
            GroupingStrategy.RANDOM -> {
                candidates.addAll(generateRandomGroupings(request, profiles))
            }
            GroupingStrategy.ABILITY_BASED -> {
                candidates.addAll(generateAbilityBasedGroupings(request, profiles))
            }
            GroupingStrategy.INTEREST_BASED -> {
                candidates.addAll(generateInterestBasedGroupings(request, profiles))
            }
            GroupingStrategy.COMPLEMENTARY -> {
                candidates.addAll(generateComplementaryGroupings(request, profiles, compatibilityMatrix))
            }
            GroupingStrategy.MIXED_ABILITY -> {
                candidates.addAll(generateMixedAbilityGroupings(request, profiles))
            }
            GroupingStrategy.SELF_SELECTED -> {
                candidates.addAll(generateSelfSelectedGroupings(request, profiles))
            }
        }
        
        // 应用约束条件
        return candidates.filter { candidate ->
            satisfiesConstraints(candidate, request.constraints)
        }
    }
    
    /**
     * 生成随机分组
     */
    private fun generateRandomGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>
    ): List<GroupingCandidate> {
        val candidates = mutableListOf<GroupingCandidate>()
        val participants = profiles.keys.toList()
        val groupSize = request.groupConfiguration.groupSize
        
        // 生成多个随机分组候选
        repeat(5) {
            val shuffled = participants.shuffled()
            val groups = mutableListOf<CollaborativeGroup>()
            
            var index = 0
            while (index < shuffled.size) {
                val currentGroupSize = minOf(
                    groupSize.last,
                    maxOf(groupSize.first, shuffled.size - index)
                )
                
                val groupMembers = shuffled.subList(index, minOf(index + currentGroupSize, shuffled.size))
                    .map { studentId ->
                        GroupMember(
                            studentId = studentId,
                            profile = profiles[studentId]!!,
                            contributionPotential = calculateContributionPotential(profiles[studentId]!!),
                            compatibilityScores = emptyMap()
                        )
                    }
                
                groups.add(
                    CollaborativeGroup(
                        id = generateGroupId(),
                        members = groupMembers,
                        compatibilityScore = 0.5, // 随机分组的默认兼容性
                        balanceScore = 0.5,
                        predictedPerformance = 0.5,
                        recommendedRoles = assignRandomRoles(groupMembers)
                    )
                )
                
                index += currentGroupSize
            }
            
            candidates.add(
                GroupingCandidate(
                    groups = groups,
                    score = calculateGroupingScore(groups),
                    strategy = GroupingStrategy.RANDOM
                )
            )
        }
        
        return candidates
    }
    
    /**
     * 生成基于能力的分组
     */
    private fun generateAbilityBasedGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>
    ): List<GroupingCandidate> {
        val candidates = mutableListOf<GroupingCandidate>()
        
        // 按能力水平排序参与者
        val sortedByAbility = profiles.toList().sortedByDescending { (_, profile) ->
            profile.performanceMetrics.averagePerformance
        }
        
        val groupSize = request.groupConfiguration.groupSize
        val groups = mutableListOf<CollaborativeGroup>()
        
        // 创建同质能力小组
        var index = 0
        while (index < sortedByAbility.size) {
            val currentGroupSize = minOf(
                groupSize.last,
                maxOf(groupSize.first, sortedByAbility.size - index)
            )
            
            val groupMembers = sortedByAbility.subList(index, minOf(index + currentGroupSize, sortedByAbility.size))
                .map { (studentId, profile) ->
                    GroupMember(
                        studentId = studentId,
                        profile = profile,
                        contributionPotential = calculateContributionPotential(profile),
                        compatibilityScores = emptyMap()
                    )
                }
            
            groups.add(
                CollaborativeGroup(
                    id = generateGroupId(),
                    members = groupMembers,
                    compatibilityScore = calculateGroupCompatibility(groupMembers),
                    balanceScore = calculateGroupBalance(groupMembers),
                    predictedPerformance = calculatePredictedPerformance(groupMembers),
                    recommendedRoles = assignRolesByAbility(groupMembers)
                )
            )
            
            index += currentGroupSize
        }
        
        candidates.add(
            GroupingCandidate(
                groups = groups,
                score = calculateGroupingScore(groups),
                strategy = GroupingStrategy.ABILITY_BASED
            )
        )
        
        return candidates
    }
    
    /**
     * 生成基于兴趣的分组
     */
    private fun generateInterestBasedGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>
    ): List<GroupingCandidate> {
        val candidates = mutableListOf<GroupingCandidate>()
        
        // 根据学科兴趣聚类
        val interestClusters = clusterByInterests(profiles)
        val groupSize = request.groupConfiguration.groupSize
        
        for (cluster in interestClusters) {
            val groups = mutableListOf<CollaborativeGroup>()
            
            var index = 0
            while (index < cluster.size) {
                val currentGroupSize = minOf(
                    groupSize.last,
                    maxOf(groupSize.first, cluster.size - index)
                )
                
                val groupMembers = cluster.subList(index, minOf(index + currentGroupSize, cluster.size))
                    .map { studentId ->
                        GroupMember(
                            studentId = studentId,
                            profile = profiles[studentId]!!,
                            contributionPotential = calculateContributionPotential(profiles[studentId]!!),
                            compatibilityScores = emptyMap()
                        )
                    }
                
                groups.add(
                    CollaborativeGroup(
                        id = generateGroupId(),
                        members = groupMembers,
                        compatibilityScore = calculateGroupCompatibility(groupMembers),
                        balanceScore = calculateGroupBalance(groupMembers),
                        predictedPerformance = calculatePredictedPerformance(groupMembers),
                        recommendedRoles = assignRolesByInterest(groupMembers)
                    )
                )
                
                index += currentGroupSize
            }
            
            candidates.add(
                GroupingCandidate(
                    groups = groups,
                    score = calculateGroupingScore(groups),
                    strategy = GroupingStrategy.INTEREST_BASED
                )
            )
        }
        
        return candidates
    }
    
    /**
     * 生成互补性分组
     */
    private fun generateComplementaryGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>,
        compatibilityMatrix: Map<Pair<StudentId, StudentId>, Double>
    ): List<GroupingCandidate> {
        val candidates = mutableListOf<GroupingCandidate>()
        
        // 使用遗传算法或模拟退火算法优化互补性
        val optimizedGrouping = groupOptimizer.optimizeForComplementarity(
            participants = profiles.keys.toList(),
            profiles = profiles,
            compatibilityMatrix = compatibilityMatrix,
            groupSize = request.groupConfiguration.groupSize
        )
        
        candidates.add(
            GroupingCandidate(
                groups = optimizedGrouping,
                score = calculateGroupingScore(optimizedGrouping),
                strategy = GroupingStrategy.COMPLEMENTARY
            )
        )
        
        return candidates
    }
    
    /**
     * 生成混合能力分组
     */
    private fun generateMixedAbilityGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>
    ): List<GroupingCandidate> {
        val candidates = mutableListOf<GroupingCandidate>()
        
        // 按能力水平分层
        val abilityLevels = categorizeByAbility(profiles)
        val groupSize = request.groupConfiguration.groupSize
        
        // 确保每个小组都有不同能力水平的学生
        val groups = createBalancedGroups(abilityLevels, groupSize)
        
        candidates.add(
            GroupingCandidate(
                groups = groups,
                score = calculateGroupingScore(groups),
                strategy = GroupingStrategy.MIXED_ABILITY
            )
        )
        
        return candidates
    }
    
    /**
     * 生成自选分组
     */
    private fun generateSelfSelectedGroupings(
        request: GroupMatchingRequest,
        profiles: Map<StudentId, CollaborativeProfile>
    ): List<GroupingCandidate> {
        // 基于学生偏好生成分组建议
        // 这里简化实现，实际应该考虑学生的偏好设置
        return generateRandomGroupings(request, profiles)
    }
    
    // ==================== 辅助方法 ====================
    
    private fun createDefaultProfile(studentId: StudentId): CollaborativeProfile {
        return CollaborativeProfile(
            studentId = studentId,
            collaborationStyle = CollaborationStyle(
                leadership = LeadershipStyle.COLLABORATIVE,
                participation = ParticipationStyle.MODERATELY_ACTIVE,
                conflictResolution = ConflictResolutionStyle.COLLABORATIVE,
                workingStyle = WorkingStyle.FLEXIBLE,
                feedbackStyle = FeedbackStyle.CONSTRUCTIVE
            ),
            communicationPreferences = CommunicationPreferences(
                preferredChannels = setOf(CommunicationChannel.TEXT_CHAT),
                responseTimeExpectation = 30.minutes,
                formalityLevel = FormalityLevel.SEMI_FORMAL,
                languagePreferences = listOf("zh-CN"),
                availabilityWindows = emptyList()
            ),
            skillContributions = emptyMap(),
            pastCollaborations = emptyList(),
            performanceMetrics = CollaborationMetrics(
                totalSessions = 0,
                averagePerformance = 0.5,
                preferredGroupSizeMin = 3,
                preferredGroupSizeMax = 5,
                successfulCollaborations = 0,
                leadershipExperience = 0,
                mentorshipExperience = 0,
                conflictResolutionSuccess = 0.5,
                peerRating = 0.5
            ),
            preferences = StudentPreferences(
                preferredGroupSizeMin = 3,
                preferredGroupSizeMax = 5,
                preferredPartnerTypes = setOf(PartnerType.SIMILAR_SKILL),
                avoidedPartnerTypes = emptySet(),
                workingTimePreferences = emptyList(),
                subjectInterests = emptyMap(),
                collaborationGoals = listOf(CollaborationGoal.SKILL_DEVELOPMENT)
            ),
            lastUpdated = Clock.System.now()
        )
    }
    
    private fun calculateContributionPotential(profile: CollaborativeProfile): Double {
        return (profile.performanceMetrics.averagePerformance + 
                profile.performanceMetrics.peerRating) / 2.0
    }
    
    private fun calculateGroupCompatibility(members: List<GroupMember>): Double {
        if (members.size < 2) return 1.0
        
        var totalCompatibility = 0.0
        var pairCount = 0
        
        for (i in members.indices) {
            for (j in i + 1 until members.size) {
                val compatibility = compatibilityCalculator.calculateCompatibility(
                    members[i].profile, members[j].profile
                )
                totalCompatibility += compatibility
                pairCount++
            }
        }
        
        return if (pairCount > 0) totalCompatibility / pairCount else 1.0
    }
    
    private fun calculateGroupBalance(members: List<GroupMember>): Double {
        // 计算小组的平衡性（技能、风格等的多样性）
        val skillVariance = calculateSkillVariance(members)
        val styleVariance = calculateStyleVariance(members)
        
        return (skillVariance + styleVariance) / 2.0
    }
    
    private fun calculatePredictedPerformance(members: List<GroupMember>): Double {
        val avgPerformance = members.map { it.profile.performanceMetrics.averagePerformance }.average()
        val compatibilityBonus = calculateGroupCompatibility(members) * 0.2
        
        return minOf(1.0, avgPerformance + compatibilityBonus)
    }
    
    private fun calculateSkillVariance(members: List<GroupMember>): Double {
        // 简化实现：计算平均表现的方差
        val performances = members.map { it.profile.performanceMetrics.averagePerformance }
        val mean = performances.average()
        val variance = performances.map { (it - mean).pow(2) }.average()
        
        return minOf(1.0, sqrt(variance) * 2) // 归一化到0-1
    }
    
    private fun calculateStyleVariance(members: List<GroupMember>): Double {
        // 计算协作风格的多样性
        val leadershipStyles = members.map { it.profile.collaborationStyle.leadership }.distinct().size
        val participationStyles = members.map { it.profile.collaborationStyle.participation }.distinct().size
        val workingStyles = members.map { it.profile.collaborationStyle.workingStyle }.distinct().size
        
        val maxDiversity = minOf(members.size, 5) // 假设最多5种不同风格
        val actualDiversity = (leadershipStyles + participationStyles + workingStyles) / 3.0
        
        return actualDiversity / maxDiversity
    }
    
    private fun assignRandomRoles(members: List<GroupMember>): Map<StudentId, ParticipantRole> {
        val roles = listOf(ParticipantRole.LEADER, ParticipantRole.MEMBER)
        return members.mapIndexed { index, member ->
            member.studentId to if (index == 0) ParticipantRole.LEADER else ParticipantRole.MEMBER
        }.toMap()
    }
    
    private fun assignRolesByAbility(members: List<GroupMember>): Map<StudentId, ParticipantRole> {
        val sortedMembers = members.sortedByDescending { it.profile.performanceMetrics.averagePerformance }
        return sortedMembers.mapIndexed { index, member ->
            member.studentId to if (index == 0) ParticipantRole.LEADER else ParticipantRole.MEMBER
        }.toMap()
    }
    
    private fun assignRolesByInterest(members: List<GroupMember>): Map<StudentId, ParticipantRole> {
        // 基于兴趣和经验分配角色
        return assignRolesByAbility(members) // 简化实现
    }
    
    private fun clusterByInterests(profiles: Map<StudentId, CollaborativeProfile>): List<List<StudentId>> {
        // 简化实现：随机分组
        return listOf(profiles.keys.toList())
    }
    
    private fun categorizeByAbility(profiles: Map<StudentId, CollaborativeProfile>): Map<String, List<StudentId>> {
        val categories = mutableMapOf<String, MutableList<StudentId>>()
        
        profiles.forEach { (studentId, profile) ->
            val performance = profile.performanceMetrics.averagePerformance
            val category = when {
                performance >= 0.8 -> "high"
                performance >= 0.6 -> "medium"
                else -> "low"
            }
            categories.getOrPut(category) { mutableListOf() }.add(studentId)
        }
        
        return categories
    }
    
    private fun createBalancedGroups(
        abilityLevels: Map<String, List<StudentId>>,
        groupSize: IntRange
    ): List<CollaborativeGroup> {
        // 简化实现：创建平衡的小组
        val allStudents = abilityLevels.values.flatten()
        return listOf(
            CollaborativeGroup(
                id = generateGroupId(),
                members = allStudents.take(groupSize.last).map { studentId ->
                    GroupMember(
                        studentId = studentId,
                        profile = createDefaultProfile(studentId),
                        contributionPotential = 0.5,
                        compatibilityScores = emptyMap()
                    )
                },
                compatibilityScore = 0.7,
                balanceScore = 0.8,
                predictedPerformance = 0.75,
                recommendedRoles = emptyMap()
            )
        )
    }
    
    private fun calculateGroupingScore(groups: List<CollaborativeGroup>): Double {
        return groups.map { group ->
            (group.compatibilityScore + group.balanceScore + group.predictedPerformance) / 3.0
        }.average()
    }
    
    private fun optimizeGrouping(
        candidates: List<GroupingCandidate>,
        preferences: MatchingPreferences,
        compatibilityMatrix: Map<Pair<StudentId, StudentId>, Double>
    ): GroupingCandidate {
        return candidates.maxByOrNull { candidate ->
            calculateWeightedScore(candidate, preferences)
        } ?: candidates.first()
    }
    
    private fun calculateWeightedScore(
        candidate: GroupingCandidate,
        preferences: MatchingPreferences
    ): Double {
        var score = candidate.score
        
        if (preferences.prioritizeCompatibility) {
            score += candidate.groups.map { it.compatibilityScore }.average() * 0.3
        }
        
        if (preferences.prioritizeComplementarity) {
            score += candidate.groups.map { it.balanceScore }.average() * 0.3
        }
        
        return score
    }
    
    private fun satisfiesConstraints(
        candidate: GroupingCandidate,
        constraints: List<MatchingConstraint>
    ): Boolean {
        // 检查分组是否满足约束条件
        return constraints.all { constraint ->
            when (constraint.type) {
                ConstraintType.MUST_BE_TOGETHER -> {
                    candidate.groups.any { group ->
                        constraint.participants.all { studentId ->
                            group.members.any { it.studentId == studentId }
                        }
                    }
                }
                ConstraintType.CANNOT_BE_TOGETHER -> {
                    candidate.groups.none { group ->
                        constraint.participants.all { studentId ->
                            group.members.any { it.studentId == studentId }
                        }
                    }
                }
                else -> true // 其他约束类型的简化实现
            }
        }
    }
    
    private fun generateExplanation(
        grouping: GroupingCandidate,
        request: GroupMatchingRequest
    ): String {
        return "基于${request.groupConfiguration.groupingStrategy}策略生成的分组，" +
                "总体匹配分数：${String.format("%.2f", grouping.score)}，" +
                "共${grouping.groups.size}个小组。"
    }
    
    private fun generateAlternatives(
        candidates: List<GroupingCandidate>,
        selected: GroupingCandidate
    ): List<GroupAlternative> {
        return candidates.filter { it != selected }
            .take(3)
            .map { candidate ->
                GroupAlternative(
                    groups = candidate.groups,
                    score = candidate.score,
                    reason = "基于${candidate.strategy}策略的替代方案"
                )
            }
    }
    
    private fun generateRequestId(): String = "req_${System.currentTimeMillis()}"
    private fun generateGroupId(): String = "group_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
}

/**
 * 分组候选方案
 */
@Serializable
data class GroupingCandidate(
    val groups: List<CollaborativeGroup>,
    val score: Double,
    val strategy: GroupingStrategy
)

/**
 * 分组匹配异常
 */
class GroupMatchingException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 兼容性计算器
 */
class CompatibilityCalculator {

    /**
     * 计算两个学生的兼容性分数
     */
    fun calculateCompatibility(profile1: CollaborativeProfile, profile2: CollaborativeProfile): Double {
        val styleCompatibility = calculateStyleCompatibility(profile1.collaborationStyle, profile2.collaborationStyle)
        val communicationCompatibility = calculateCommunicationCompatibility(
            profile1.communicationPreferences, profile2.communicationPreferences
        )
        val performanceCompatibility = calculatePerformanceCompatibility(
            profile1.performanceMetrics, profile2.performanceMetrics
        )
        val preferenceCompatibility = calculatePreferenceCompatibility(
            profile1.preferences, profile2.preferences
        )

        // 加权平均
        return (styleCompatibility * 0.3 +
                communicationCompatibility * 0.25 +
                performanceCompatibility * 0.25 +
                preferenceCompatibility * 0.2)
    }

    private fun calculateStyleCompatibility(style1: CollaborationStyle, style2: CollaborationStyle): Double {
        val leadershipCompatibility = calculateLeadershipCompatibility(style1.leadership, style2.leadership)
        val participationCompatibility = calculateParticipationCompatibility(style1.participation, style2.participation)
        val conflictCompatibility = calculateConflictCompatibility(style1.conflictResolution, style2.conflictResolution)
        val workingCompatibility = calculateWorkingCompatibility(style1.workingStyle, style2.workingStyle)
        val feedbackCompatibility = calculateFeedbackCompatibility(style1.feedbackStyle, style2.feedbackStyle)

        return (leadershipCompatibility + participationCompatibility + conflictCompatibility +
                workingCompatibility + feedbackCompatibility) / 5.0
    }

    private fun calculateLeadershipCompatibility(leadership1: LeadershipStyle, leadership2: LeadershipStyle): Double {
        return when {
            leadership1 == leadership2 -> 0.8 // 相同风格有一定兼容性
            (leadership1 == LeadershipStyle.NATURAL_LEADER && leadership2 == LeadershipStyle.FOLLOWER) ||
            (leadership1 == LeadershipStyle.FOLLOWER && leadership2 == LeadershipStyle.NATURAL_LEADER) -> 0.9 // 互补
            leadership1 == LeadershipStyle.COLLABORATIVE || leadership2 == LeadershipStyle.COLLABORATIVE -> 0.85 // 协作型与其他兼容
            else -> 0.6
        }
    }

    private fun calculateParticipationCompatibility(participation1: ParticipationStyle, participation2: ParticipationStyle): Double {
        return when {
            participation1 == participation2 -> 0.7
            (participation1 == ParticipationStyle.HIGHLY_ACTIVE && participation2 == ParticipationStyle.OBSERVER) ||
            (participation1 == ParticipationStyle.OBSERVER && participation2 == ParticipationStyle.HIGHLY_ACTIVE) -> 0.6 // 可能不平衡
            else -> 0.8
        }
    }

    private fun calculateConflictCompatibility(conflict1: ConflictResolutionStyle, conflict2: ConflictResolutionStyle): Double {
        return when {
            conflict1 == ConflictResolutionStyle.COLLABORATIVE || conflict2 == ConflictResolutionStyle.COLLABORATIVE -> 0.9
            conflict1 == conflict2 -> 0.7
            (conflict1 == ConflictResolutionStyle.COMPETITIVE && conflict2 == ConflictResolutionStyle.COMPETITIVE) -> 0.3 // 可能冲突
            else -> 0.6
        }
    }

    private fun calculateWorkingCompatibility(working1: WorkingStyle, working2: WorkingStyle): Double {
        return when {
            working1 == working2 -> 0.8
            (working1 == WorkingStyle.STRUCTURED && working2 == WorkingStyle.FLEXIBLE) ||
            (working1 == WorkingStyle.FLEXIBLE && working2 == WorkingStyle.STRUCTURED) -> 0.7 // 可以互补
            (working1 == WorkingStyle.DETAIL_ORIENTED && working2 == WorkingStyle.BIG_PICTURE) ||
            (working1 == WorkingStyle.BIG_PICTURE && working2 == WorkingStyle.DETAIL_ORIENTED) -> 0.9 // 很好的互补
            else -> 0.6
        }
    }

    private fun calculateFeedbackCompatibility(feedback1: FeedbackStyle, feedback2: FeedbackStyle): Double {
        return when {
            feedback1 == feedback2 -> 0.8
            feedback1 == FeedbackStyle.CONSTRUCTIVE || feedback2 == FeedbackStyle.CONSTRUCTIVE -> 0.9
            (feedback1 == FeedbackStyle.DIRECT && feedback2 == FeedbackStyle.DIPLOMATIC) ||
            (feedback1 == FeedbackStyle.DIPLOMATIC && feedback2 == FeedbackStyle.DIRECT) -> 0.6
            else -> 0.7
        }
    }

    private fun calculateCommunicationCompatibility(
        comm1: CommunicationPreferences,
        comm2: CommunicationPreferences
    ): Double {
        val channelOverlap = comm1.preferredChannels.intersect(comm2.preferredChannels).size.toDouble() /
                            comm1.preferredChannels.union(comm2.preferredChannels).size.toDouble()

        val formalityCompatibility = when {
            comm1.formalityLevel == comm2.formalityLevel -> 1.0
            abs(comm1.formalityLevel.ordinal - comm2.formalityLevel.ordinal) <= 1 -> 0.8
            else -> 0.5
        }

        val languageCompatibility = if (comm1.languagePreferences.intersect(comm2.languagePreferences).isNotEmpty()) 1.0 else 0.3

        return (channelOverlap * 0.4 + formalityCompatibility * 0.3 + languageCompatibility * 0.3)
    }

    private fun calculatePerformanceCompatibility(
        perf1: CollaborationMetrics,
        perf2: CollaborationMetrics
    ): Double {
        val performanceDiff = abs(perf1.averagePerformance - perf2.averagePerformance)
        val performanceCompatibility = 1.0 - performanceDiff // 性能差异越小，兼容性越高

        val experienceDiff = abs(perf1.totalSessions - perf2.totalSessions).toDouble() /
                            maxOf(perf1.totalSessions, perf2.totalSessions, 1).toDouble()
        val experienceCompatibility = 1.0 - experienceDiff

        return (performanceCompatibility * 0.6 + experienceCompatibility * 0.4)
    }

    private fun calculatePreferenceCompatibility(
        pref1: StudentPreferences,
        pref2: StudentPreferences
    ): Double {
        val groupSizeCompatibility = if (pref1.preferredGroupSize.intersect(pref2.preferredGroupSize).isNotEmpty()) 1.0 else 0.5

        val partnerTypeCompatibility = calculatePartnerTypeCompatibility(pref1, pref2)

        val goalCompatibility = pref1.collaborationGoals.intersect(pref2.collaborationGoals).size.toDouble() /
                               pref1.collaborationGoals.union(pref2.collaborationGoals).size.toDouble()

        return (groupSizeCompatibility * 0.3 + partnerTypeCompatibility * 0.4 + goalCompatibility * 0.3)
    }

    private fun calculatePartnerTypeCompatibility(pref1: StudentPreferences, pref2: StudentPreferences): Double {
        // 检查是否互相符合对方的偏好伙伴类型
        val mutualCompatibility = when {
            pref1.preferredPartnerTypes.contains(PartnerType.SIMILAR_SKILL) &&
            pref2.preferredPartnerTypes.contains(PartnerType.SIMILAR_SKILL) -> 0.9

            pref1.preferredPartnerTypes.contains(PartnerType.COMPLEMENTARY_SKILL) &&
            pref2.preferredPartnerTypes.contains(PartnerType.COMPLEMENTARY_SKILL) -> 0.8

            (pref1.preferredPartnerTypes.contains(PartnerType.HIGHER_SKILL) &&
             pref2.preferredPartnerTypes.contains(PartnerType.LOWER_SKILL)) ||
            (pref1.preferredPartnerTypes.contains(PartnerType.LOWER_SKILL) &&
             pref2.preferredPartnerTypes.contains(PartnerType.HIGHER_SKILL)) -> 0.95 // 完美互补

            else -> 0.6
        }

        // 检查是否在对方的避免列表中
        val avoidanceCompatibility = when {
            pref1.avoidedPartnerTypes.isEmpty() && pref2.avoidedPartnerTypes.isEmpty() -> 1.0
            // 简化实现：假设没有严重冲突
            else -> 0.8
        }

        return (mutualCompatibility * 0.7 + avoidanceCompatibility * 0.3)
    }
}

/**
 * 小组优化器
 */
class GroupOptimizer {

    /**
     * 为互补性优化分组
     */
    fun optimizeForComplementarity(
        participants: List<StudentId>,
        profiles: Map<StudentId, CollaborativeProfile>,
        compatibilityMatrix: Map<Pair<StudentId, StudentId>, Double>,
        groupSize: IntRange
    ): List<CollaborativeGroup> {
        // 使用简化的贪心算法
        val groups = mutableListOf<CollaborativeGroup>()
        val remaining = participants.toMutableList()

        while (remaining.isNotEmpty()) {
            val currentGroupSize = minOf(groupSize.last, maxOf(groupSize.first, remaining.size))
            val group = createOptimalGroup(remaining, profiles, compatibilityMatrix, currentGroupSize)

            groups.add(group)
            remaining.removeAll(group.members.map { it.studentId })
        }

        return groups
    }

    private fun createOptimalGroup(
        candidates: List<StudentId>,
        profiles: Map<StudentId, CollaborativeProfile>,
        compatibilityMatrix: Map<Pair<StudentId, StudentId>, Double>,
        targetSize: Int
    ): CollaborativeGroup {
        val selectedMembers = mutableListOf<StudentId>()
        val remainingCandidates = candidates.toMutableList()

        // 选择第一个成员（随机或基于某种策略）
        val firstMember = remainingCandidates.removeAt(0)
        selectedMembers.add(firstMember)

        // 贪心选择剩余成员
        while (selectedMembers.size < targetSize && remainingCandidates.isNotEmpty()) {
            val nextMember = remainingCandidates.maxByOrNull { candidate ->
                selectedMembers.sumOf { selected ->
                    compatibilityMatrix[Pair(selected, candidate)] ?: 0.0
                } / selectedMembers.size
            }

            if (nextMember != null) {
                selectedMembers.add(nextMember)
                remainingCandidates.remove(nextMember)
            } else {
                break
            }
        }

        val groupMembers = selectedMembers.map { studentId ->
            GroupMember(
                studentId = studentId,
                profile = profiles[studentId]!!,
                contributionPotential = calculateContributionPotential(profiles[studentId]!!),
                compatibilityScores = selectedMembers.filter { it != studentId }.associateWith { otherId ->
                    compatibilityMatrix[Pair(studentId, otherId)] ?: 0.0
                }
            )
        }

        return CollaborativeGroup(
            id = "group_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
            members = groupMembers,
            compatibilityScore = calculateGroupCompatibility(groupMembers, compatibilityMatrix),
            balanceScore = calculateGroupBalance(groupMembers),
            predictedPerformance = calculatePredictedPerformance(groupMembers),
            recommendedRoles = assignOptimalRoles(groupMembers)
        )
    }

    private fun calculateContributionPotential(profile: CollaborativeProfile): Double {
        return (profile.performanceMetrics.averagePerformance + profile.performanceMetrics.peerRating) / 2.0
    }

    private fun calculateGroupCompatibility(
        members: List<GroupMember>,
        compatibilityMatrix: Map<Pair<StudentId, StudentId>, Double>
    ): Double {
        if (members.size < 2) return 1.0

        var totalCompatibility = 0.0
        var pairCount = 0

        for (i in members.indices) {
            for (j in i + 1 until members.size) {
                val compatibility = compatibilityMatrix[Pair(members[i].studentId, members[j].studentId)] ?: 0.5
                totalCompatibility += compatibility
                pairCount++
            }
        }

        return if (pairCount > 0) totalCompatibility / pairCount else 1.0
    }

    private fun calculateGroupBalance(members: List<GroupMember>): Double {
        // 计算技能和风格的多样性
        val leadershipStyles = members.map { it.profile.collaborationStyle.leadership }.distinct().size
        val participationStyles = members.map { it.profile.collaborationStyle.participation }.distinct().size
        val workingStyles = members.map { it.profile.collaborationStyle.workingStyle }.distinct().size

        val maxDiversity = minOf(members.size, 5)
        val actualDiversity = (leadershipStyles + participationStyles + workingStyles) / 3.0

        return actualDiversity / maxDiversity
    }

    private fun calculatePredictedPerformance(members: List<GroupMember>): Double {
        val avgPerformance = members.map { it.profile.performanceMetrics.averagePerformance }.average()
        val experienceBonus = members.map { it.profile.performanceMetrics.totalSessions }.average() / 100.0

        return minOf(1.0, avgPerformance + experienceBonus * 0.1)
    }

    private fun assignOptimalRoles(members: List<GroupMember>): Map<StudentId, ParticipantRole> {
        val sortedByLeadership = members.sortedByDescending { member ->
            when (member.profile.collaborationStyle.leadership) {
                LeadershipStyle.NATURAL_LEADER -> 1.0
                LeadershipStyle.SUPPORTIVE_LEADER -> 0.8
                LeadershipStyle.COLLABORATIVE -> 0.6
                LeadershipStyle.SITUATIONAL -> 0.4
                LeadershipStyle.FOLLOWER -> 0.2
            }
        }

        return sortedByLeadership.mapIndexed { index, member ->
            member.studentId to if (index == 0) ParticipantRole.LEADER else ParticipantRole.MEMBER
        }.toMap()
    }
}

/**
 * 协作档案服务
 */
class CollaborativeProfileService {

    private val profiles = mutableMapOf<StudentId, CollaborativeProfile>()

    /**
     * 获取学生的协作档案
     */
    suspend fun getCollaborativeProfile(studentId: StudentId): CollaborativeProfile? {
        return profiles[studentId]
    }

    /**
     * 更新学生的协作档案
     */
    suspend fun updateCollaborativeProfile(profile: CollaborativeProfile) {
        profiles[profile.studentId] = profile
    }

    /**
     * 创建默认协作档案
     */
    fun createDefaultProfile(studentId: StudentId): CollaborativeProfile {
        return CollaborativeProfile(
            studentId = studentId,
            collaborationStyle = CollaborationStyle(
                leadership = LeadershipStyle.COLLABORATIVE,
                participation = ParticipationStyle.MODERATELY_ACTIVE,
                conflictResolution = ConflictResolutionStyle.COLLABORATIVE,
                workingStyle = WorkingStyle.FLEXIBLE,
                feedbackStyle = FeedbackStyle.CONSTRUCTIVE
            ),
            communicationPreferences = CommunicationPreferences(
                preferredChannels = setOf(CommunicationChannel.TEXT_CHAT),
                responseTimeExpectation = 30.minutes,
                formalityLevel = FormalityLevel.SEMI_FORMAL,
                languagePreferences = listOf("zh-CN"),
                availabilityWindows = emptyList()
            ),
            skillContributions = emptyMap(),
            pastCollaborations = emptyList(),
            performanceMetrics = CollaborationMetrics(
                totalSessions = 0,
                averagePerformance = 0.5,
                preferredGroupSizeMin = 3,
                preferredGroupSizeMax = 5,
                successfulCollaborations = 0,
                leadershipExperience = 0,
                mentorshipExperience = 0,
                conflictResolutionSuccess = 0.5,
                peerRating = 0.5
            ),
            preferences = StudentPreferences(
                preferredGroupSizeMin = 3,
                preferredGroupSizeMax = 5,
                preferredPartnerTypes = setOf(PartnerType.SIMILAR_SKILL),
                avoidedPartnerTypes = emptySet(),
                workingTimePreferences = emptyList(),
                subjectInterests = emptyMap(),
                collaborationGoals = listOf(CollaborationGoal.SKILL_DEVELOPMENT)
            ),
            lastUpdated = Clock.System.now()
        )
    }
}
