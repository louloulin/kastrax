package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

/**
 * Week 23-24: 智能协作学习平台测试
 * 
 * 测试场景：
 * - CL-001: 协作学习会话管理测试
 * - CL-002: 智能小组匹配测试
 * - CL-003: 协作学习分析测试
 * - CL-004: 同伴学习推荐测试
 * - CL-005: 冲突检测和解决测试
 * - CL-006: 领导力分析测试
 */
class CollaborativeLearningTest {
    
    private val collaborativeLearningService = CollaborativeLearningService()
    private val groupMatcher = IntelligentGroupMatcher()
    private val collaborationAnalyzer = CollaborationAnalyzer()
    private val peerRecommendationEngine = PeerRecommendationEngine()
    private val conflictDetector = ConflictDetector()
    private val leadershipAnalyzer = LeadershipAnalyzer()
    
    @Test
    fun testCL001_CollaborativeSessionManagement() = runBlocking {
        println("=== CL-001: 协作学习会话管理测试 ===")
        
        // 1. 创建协作学习会话
        val sessionResult = collaborativeLearningService.createCollaborativeSession(
            facilitatorId = "teacher_001",
            title = "数学协作学习",
            description = "高等数学概念讨论和问题解决",
            subject = Subject.MATHEMATICS,
            topic = "微积分基础",
            sessionType = CollaborativeSessionType.STUDY_GROUP,
            learningObjectives = listOf(
                "理解导数概念",
                "掌握求导法则",
                "应用导数解决实际问题"
            ),
            settings = SessionSettings(
                maxParticipants = 8,
                enableVoiceChat = true,
                enableVideoChat = false,
                enableScreenShare = true,
                moderationLevel = ModerationLevel.MEDIUM
            )
        )
        
        assertTrue(sessionResult is CollaborativeSessionResult.Success)
        val session = (sessionResult as CollaborativeSessionResult.Success).session
        
        println("✅ 成功创建协作学习会话: ${session.title}")
        println("   会话ID: ${session.id}")
        println("   类型: ${session.sessionType}")
        println("   学习目标: ${session.learningObjectives.joinToString(", ")}")
        
        // 2. 学生加入会话
        val students = listOf("student_001", "student_002", "student_003", "student_004")
        
        for (studentId in students) {
            val joinResult = collaborativeLearningService.joinSession(
                sessionId = session.id,
                studentId = StudentId(studentId),
                role = if (studentId == "student_001") ParticipantRole.LEADER else ParticipantRole.MEMBER
            )
            
            assertTrue(joinResult is CollaborativeSessionResult.Success)
            println("✅ 学生 $studentId 成功加入会话")
        }
        
        // 3. 开始会话
        val startResult = collaborativeLearningService.startSession(session.id)
        assertTrue(startResult is CollaborativeSessionResult.Success)
        
        val activeSession = (startResult as CollaborativeSessionResult.Success).session
        assertEquals(SessionStatus.ACTIVE, activeSession.status)
        assertNotNull(activeSession.startTime)
        
        println("✅ 会话成功开始，状态: ${activeSession.status}")
        println("   参与者数量: ${activeSession.participants.size}")
        
        // 4. 创建协作活动
        val activityResult = collaborativeLearningService.createCollaborativeActivity(
            sessionId = session.id,
            type = ActivityType.GROUP_DISCUSSION,
            title = "导数概念讨论",
            description = "小组讨论导数的几何意义和物理意义",
            instructions = "每个小组选择一个导数应用场景进行深入讨论",
            timeline = ActivityTimeline(
                startTime = Clock.System.now(),
                endTime = Clock.System.now() + 1.hours,
                phases = listOf(
                    ActivityPhase("准备阶段", "阅读材料和准备", 15.minutes, listOf("阅读指定材料")),
                    ActivityPhase("讨论阶段", "小组讨论", 30.minutes, listOf("积极参与讨论")),
                    ActivityPhase("总结阶段", "总结和分享", 15.minutes, listOf("准备总结发言"))
                )
            ),
            groupConfiguration = GroupConfiguration(
                groupSizeMin = 2,
                groupSizeMax = 3,
                groupingStrategy = GroupingStrategy.MIXED_ABILITY,
                allowSelfSelection = false,
                balancingCriteria = listOf(
                    BalancingCriterion(BalancingType.SKILL_LEVEL, 0.6, "数学能力"),
                    BalancingCriterion(BalancingType.LEARNING_STYLE, 0.4, "学习风格")
                )
            )
        )
        
        assertTrue(activityResult is CollaborativeSessionResult.Success)
        println("✅ 成功创建协作活动: 导数概念讨论")
        
        // 5. 处理协作交互
        val interactions = listOf(
            Triple("student_001", InteractionType.QUESTION, "大家对导数的几何意义有什么理解？"),
            Triple("student_002", InteractionType.ANSWER, "导数表示函数在某点的切线斜率"),
            Triple("student_003", InteractionType.COMMENT, "我觉得还可以理解为瞬时变化率"),
            Triple("student_004", InteractionType.SHARE, "我找到了一个很好的可视化工具")
        )
        
        for ((studentId, type, content) in interactions) {
            val interactionResult = collaborativeLearningService.processCollaborativeInteraction(
                sessionId = session.id,
                participantId = StudentId(studentId),
                type = type,
                content = InteractionContent(
                    text = content,
                    attachments = emptyList(),
                    mentions = emptyList(),
                    tags = listOf("导数", "几何意义")
                )
            )
            
            assertTrue(interactionResult is CollaborativeSessionResult.Success)
        }
        
        println("✅ 成功处理 ${interactions.size} 个协作交互")
        
        // 6. 结束会话
        val endResult = collaborativeLearningService.endSession(session.id)
        assertTrue(endResult is CollaborativeSessionResult.Success)
        
        val endedSession = (endResult as CollaborativeSessionResult.Success).session
        assertEquals(SessionStatus.COMPLETED, endedSession.status)
        assertNotNull(endedSession.endTime)
        
        println("✅ 会话成功结束，状态: ${endedSession.status}")
        println("=== CL-001 测试完成 ===\n")
    }
    
    @Test
    fun testCL002_IntelligentGroupMatching() = runBlocking {
        println("=== CL-002: 智能小组匹配测试 ===")
        
        // 1. 准备测试数据
        val participants = listOf(
            StudentId("student_001"), StudentId("student_002"), 
            StudentId("student_003"), StudentId("student_004"),
            StudentId("student_005"), StudentId("student_006")
        )
        
        val matchingRequest = GroupMatchingRequest(
            sessionId = SessionId.generate(),
            activityId = "activity_001",
            participants = participants,
            groupConfiguration = GroupConfiguration(
                groupSizeMin = 2,
                groupSizeMax = 3,
                groupingStrategy = GroupingStrategy.COMPLEMENTARY,
                allowSelfSelection = false,
                balancingCriteria = listOf(
                    BalancingCriterion(BalancingType.SKILL_LEVEL, 0.5, "技能水平"),
                    BalancingCriterion(BalancingType.LEARNING_STYLE, 0.3, "学习风格"),
                    BalancingCriterion(BalancingType.PERSONALITY, 0.2, "性格特征")
                )
            ),
            preferences = MatchingPreferences(
                prioritizeCompatibility = true,
                prioritizeComplementarity = true,
                considerPastCollaborations = true,
                balanceSkillLevels = true
            ),
            constraints = listOf(
                MatchingConstraint(
                    type = ConstraintType.MUST_BE_TOGETHER,
                    participants = listOf(StudentId("student_001"), StudentId("student_002")),
                    description = "这两个学生需要在同一组"
                )
            )
        )
        
        // 2. 执行智能分组
        val matchingResult = groupMatcher.matchGroups(matchingRequest)
        
        assertNotNull(matchingResult)
        assertTrue(matchingResult.groups.isNotEmpty())
        assertTrue(matchingResult.matchingScore > 0.0)
        
        println("✅ 智能分组成功完成")
        println("   分组数量: ${matchingResult.groups.size}")
        println("   匹配分数: ${String.format("%.2f", matchingResult.matchingScore)}")
        println("   分组说明: ${matchingResult.explanation}")
        
        // 3. 验证分组结果
        for ((index, group) in matchingResult.groups.withIndex()) {
            println("   小组 ${index + 1}:")
            println("     成员: ${group.members.map { it.studentId.value }.joinToString(", ")}")
            println("     兼容性分数: ${String.format("%.2f", group.compatibilityScore)}")
            println("     平衡性分数: ${String.format("%.2f", group.balanceScore)}")
            println("     预期表现: ${String.format("%.2f", group.predictedPerformance)}")
            
            // 验证小组大小
            assertTrue(group.members.size in matchingRequest.groupConfiguration.groupSize)
        }
        
        // 4. 验证约束条件
        val constraintSatisfied = matchingResult.groups.any { group ->
            group.members.any { it.studentId == StudentId("student_001") } &&
            group.members.any { it.studentId == StudentId("student_002") }
        }
        assertTrue(constraintSatisfied, "约束条件未满足")
        
        println("✅ 约束条件验证通过")
        
        // 5. 检查替代方案
        assertTrue(matchingResult.alternatives.isNotEmpty())
        println("✅ 提供了 ${matchingResult.alternatives.size} 个替代方案")
        
        println("=== CL-002 测试完成 ===\n")
    }
    
    @Test
    fun testCL003_CollaborationAnalysis() = runBlocking {
        println("=== CL-003: 协作学习分析测试 ===")
        
        // 1. 创建测试会话
        val session = createTestSession()
        
        // 2. 执行协作分析
        val analysisResult = collaborationAnalyzer.analyzeSession(session)
        
        assertNotNull(analysisResult)
        assertTrue(analysisResult.participationMetrics.isNotEmpty())
        assertNotNull(analysisResult.groupDynamics)
        assertNotNull(analysisResult.learningOutcomes)
        assertTrue(analysisResult.recommendations.isNotEmpty())
        
        println("✅ 协作分析成功完成")
        println("   分析的参与者数量: ${analysisResult.participationMetrics.size}")
        println("   小组凝聚力分数: ${String.format("%.2f", analysisResult.groupDynamics.cohesionScore)}")
        println("   沟通效果分数: ${String.format("%.2f", analysisResult.groupDynamics.communicationEffectiveness)}")
        println("   冲突水平: ${analysisResult.groupDynamics.conflictLevel}")
        
        // 3. 验证参与度指标
        for ((studentId, metrics) in analysisResult.participationMetrics) {
            println("   学生 ${studentId.value}:")
            println("     消息数量: ${metrics.messageCount}")
            println("     贡献质量: ${String.format("%.2f", metrics.contributionQuality)}")
            println("     参与水平: ${metrics.engagementLevel}")
            println("     领导时刻: ${metrics.leadershipMoments}")
            
            assertTrue(metrics.contributionQuality >= 0.0 && metrics.contributionQuality <= 1.0)
            assertTrue(metrics.messageCount >= 0)
        }
        
        // 4. 验证小组动态分析
        val groupDynamics = analysisResult.groupDynamics
        assertTrue(groupDynamics.cohesionScore >= 0.0 && groupDynamics.cohesionScore <= 1.0)
        assertTrue(groupDynamics.communicationEffectiveness >= 0.0 && groupDynamics.communicationEffectiveness <= 1.0)
        assertTrue(groupDynamics.leadershipDistribution.isNotEmpty())
        
        println("✅ 小组动态分析验证通过")
        
        // 5. 验证学习成果评估
        val learningOutcomes = analysisResult.learningOutcomes
        assertTrue(learningOutcomes.objectiveAchievement.isNotEmpty())
        assertTrue(learningOutcomes.skillDevelopment.isNotEmpty())
        assertTrue(learningOutcomes.knowledgeGain.isNotEmpty())
        
        println("✅ 学习成果评估验证通过")
        
        // 6. 验证改进建议
        assertTrue(analysisResult.recommendations.isNotEmpty())
        for (recommendation in analysisResult.recommendations) {
            assertNotNull(recommendation.type)
            assertNotNull(recommendation.description)
            assertTrue(recommendation.targetParticipants.isNotEmpty())
            assertTrue(recommendation.actionItems.isNotEmpty())
        }
        
        println("✅ 生成了 ${analysisResult.recommendations.size} 条改进建议")
        
        // 7. 生成最终报告
        val finalReport = collaborationAnalyzer.generateFinalReport(session)
        
        assertNotNull(finalReport)
        assertNotNull(finalReport.sessionSummary)
        assertTrue(finalReport.participantPerformance.isNotEmpty())
        assertNotNull(finalReport.groupEffectiveness)
        assertTrue(finalReport.improvementAreas.isNotEmpty())
        assertTrue(finalReport.futureRecommendations.isNotEmpty())
        
        println("✅ 最终报告生成成功")
        println("   会话评分: ${String.format("%.2f", finalReport.sessionSummary.overallRating)}")
        println("   改进领域: ${finalReport.improvementAreas.size} 个")
        println("   未来建议: ${finalReport.futureRecommendations.size} 条")
        
        println("=== CL-003 测试完成 ===\n")
    }
    
    @Test
    fun testCL004_PeerLearningRecommendation() = runBlocking {
        println("=== CL-004: 同伴学习推荐测试 ===")
        
        val studentId = StudentId("student_001")
        val subject = Subject.MATHEMATICS
        val topic = "线性代数"
        
        // 1. 生成同伴学习推荐
        val recommendations = peerRecommendationEngine.generateRecommendations(
            studentId = studentId,
            subject = subject,
            topic = topic
        )
        
        assertTrue(recommendations.isNotEmpty())
        println("✅ 生成了 ${recommendations.size} 个同伴学习推荐")
        
        // 2. 验证推荐内容
        for (recommendation in recommendations) {
            assertNotNull(recommendation.recommendedPeerId)
            assertNotNull(recommendation.reason)
            assertTrue(recommendation.compatibilityScore >= 0.0 && recommendation.compatibilityScore <= 1.0)
            assertNotNull(recommendation.recommendationType)
            assertTrue(recommendation.expectedBenefits.isNotEmpty())
            
            println("   推荐类型: ${recommendation.recommendationType}")
            println("   推荐对象: ${recommendation.recommendedPeerId.value}")
            println("   兼容性分数: ${String.format("%.2f", recommendation.compatibilityScore)}")
            println("   推荐理由: ${recommendation.reason}")
            println("   预期收益: ${recommendation.expectedBenefits.joinToString(", ")}")
            println()
        }
        
        // 3. 验证推荐类型多样性
        val recommendationTypes = recommendations.map { it.recommendationType }.distinct()
        assertTrue(recommendationTypes.size >= 2, "推荐类型应该多样化")
        
        println("✅ 推荐类型多样性验证通过: ${recommendationTypes.joinToString(", ")}")
        
        // 4. 推荐协作学习机会
        val opportunities = peerRecommendationEngine.recommendCollaborationOpportunities(
            studentId = studentId,
            interests = listOf("数学", "编程", "算法"),
            skillLevel = SkillLevel.INTERMEDIATE
        )
        
        assertTrue(opportunities.isNotEmpty())
        println("✅ 推荐了 ${opportunities.size} 个协作学习机会")
        
        for (opportunity in opportunities) {
            assertNotNull(opportunity.title)
            assertNotNull(opportunity.description)
            assertTrue(opportunity.matchScore >= 0.0 && opportunity.matchScore <= 1.0)
            assertTrue(opportunity.expectedBenefits.isNotEmpty())
            assertTrue(opportunity.currentMembers < opportunity.maxMembers)
            
            println("   机会: ${opportunity.title}")
            println("   类型: ${opportunity.type}")
            println("   匹配分数: ${String.format("%.2f", opportunity.matchScore)}")
            println("   当前成员: ${opportunity.currentMembers}/${opportunity.maxMembers}")
            println()
        }
        
        // 5. 分析学习互补性
        val complementarityAnalysis = peerRecommendationEngine.analyzeLearningComplementarity(
            student1Id = StudentId("student_001"),
            student2Id = StudentId("student_002")
        )
        
        assertNotNull(complementarityAnalysis)
        assertTrue(complementarityAnalysis.overallComplementarity >= 0.0)
        assertTrue(complementarityAnalysis.styleComplementarity >= 0.0)
        assertTrue(complementarityAnalysis.goalAlignment >= 0.0)
        
        println("✅ 学习互补性分析完成")
        println("   整体互补性: ${String.format("%.2f", complementarityAnalysis.overallComplementarity)}")
        println("   风格互补性: ${String.format("%.2f", complementarityAnalysis.styleComplementarity)}")
        println("   目标一致性: ${String.format("%.2f", complementarityAnalysis.goalAlignment)}")
        println("   协作潜力: ${complementarityAnalysis.collaborationPotential}")
        println("   互惠利益: ${complementarityAnalysis.mutualBenefits.joinToString(", ")}")
        
        println("=== CL-004 测试完成 ===\n")
    }

    @Test
    fun testCL005_ConflictDetectionAndResolution() = runBlocking {
        println("=== CL-005: 冲突检测和解决测试 ===")

        // 1. 创建包含冲突的交互数据
        val conflictInteractions = listOf(
            CollaborativeInteraction(
                id = "interaction_001",
                sessionId = SessionId.generate(),
                activityId = "activity_001",
                participantId = StudentId("student_001"),
                type = InteractionType.MESSAGE,
                content = InteractionContent(
                    text = "我不同意这个观点，这完全是错误的",
                    attachments = emptyList(),
                    mentions = listOf("student_002"),
                    tags = emptyList()
                ),
                timestamp = Clock.System.now() - 30.minutes,
                responses = listOf(
                    InteractionResponse(
                        participantId = StudentId("student_002"),
                        type = ResponseType.DISAGREE,
                        content = "我坚持我的观点",
                        timestamp = Clock.System.now() - 25.minutes
                    )
                )
            ),
            CollaborativeInteraction(
                id = "interaction_002",
                sessionId = SessionId.generate(),
                activityId = "activity_001",
                participantId = StudentId("student_002"),
                type = InteractionType.COMMENT,
                content = InteractionContent(
                    text = "你的想法太荒谬了，根本不可能",
                    attachments = emptyList(),
                    mentions = listOf("student_001"),
                    tags = emptyList()
                ),
                timestamp = Clock.System.now() - 20.minutes,
                responses = listOf(
                    InteractionResponse(
                        participantId = StudentId("student_001"),
                        type = ResponseType.DISAGREE,
                        content = "我不接受这种说法",
                        timestamp = Clock.System.now() - 15.minutes
                    )
                )
            ),
            CollaborativeInteraction(
                id = "interaction_003",
                sessionId = SessionId.generate(),
                activityId = "activity_001",
                participantId = StudentId("student_003"),
                type = InteractionType.QUESTION,
                content = InteractionContent(
                    text = "有人能帮我解答这个问题吗？",
                    attachments = emptyList(),
                    mentions = emptyList(),
                    tags = emptyList()
                ),
                timestamp = Clock.System.now() - 60.minutes,
                responses = emptyList() // 无人回应
            )
        )

        // 2. 检测冲突指标
        val conflictIndicators = conflictDetector.detectConflictIndicators(conflictInteractions)

        assertTrue(conflictIndicators.isNotEmpty())
        println("✅ 检测到 ${conflictIndicators.size} 个冲突指标")

        for (indicator in conflictIndicators) {
            assertNotNull(indicator.type)
            assertTrue(indicator.severity >= 0.0 && indicator.severity <= 1.0)
            assertNotNull(indicator.description)
            assertTrue(indicator.involvedParticipants.isNotEmpty())
            assertTrue(indicator.resolutionSuggestions.isNotEmpty())

            println("   冲突类型: ${indicator.type}")
            println("   严重程度: ${String.format("%.2f", indicator.severity)}")
            println("   描述: ${indicator.description}")
            println("   涉及参与者: ${indicator.involvedParticipants.map { it.value }.joinToString(", ")}")
            println("   解决建议: ${indicator.resolutionSuggestions.joinToString(", ")}")
            println()
        }

        // 3. 分析冲突类型
        val conflictTypeAnalysis = conflictDetector.analyzeConflictTypes(conflictIndicators)

        assertNotNull(conflictTypeAnalysis)
        assertTrue(conflictTypeAnalysis.overallSeverity >= 0.0)

        println("✅ 冲突类型分析完成")
        println("   任务冲突: ${conflictTypeAnalysis.taskConflicts}")
        println("   过程冲突: ${conflictTypeAnalysis.processConflicts}")
        println("   关系冲突: ${conflictTypeAnalysis.relationshipConflicts}")
        println("   主要冲突类型: ${conflictTypeAnalysis.dominantConflictType}")
        println("   整体严重程度: ${String.format("%.2f", conflictTypeAnalysis.overallSeverity)}")

        // 4. 预测冲突风险
        val participants = listOf(
            SessionParticipant(
                studentId = StudentId("student_001"),
                role = ParticipantRole.MEMBER,
                joinedAt = Clock.System.now() - 1.hours,
                status = ParticipantStatus.ACTIVE,
                contributionScore = 5.0,
                engagementLevel = EngagementLevel.LOW
            ),
            SessionParticipant(
                studentId = StudentId("student_002"),
                role = ParticipantRole.MEMBER,
                joinedAt = Clock.System.now() - 1.hours,
                status = ParticipantStatus.ACTIVE,
                contributionScore = 3.0,
                engagementLevel = EngagementLevel.LOW
            )
        )

        val riskAssessment = conflictDetector.predictConflictRisk(conflictInteractions, participants)

        assertNotNull(riskAssessment)
        assertTrue(riskAssessment.riskScore >= 0.0 && riskAssessment.riskScore <= 1.0)
        assertTrue(riskAssessment.preventionRecommendations.isNotEmpty())

        println("✅ 冲突风险评估完成")
        println("   风险等级: ${riskAssessment.riskLevel}")
        println("   风险分数: ${String.format("%.2f", riskAssessment.riskScore)}")
        println("   风险因素: ${riskAssessment.riskFactors.size} 个")
        println("   预防建议: ${riskAssessment.preventionRecommendations.joinToString(", ")}")
        println("   监控要点: ${riskAssessment.monitoringPoints.joinToString(", ")}")

        println("=== CL-005 测试完成 ===\n")
    }

    @Test
    fun testCL006_LeadershipAnalysis() = runBlocking {
        println("=== CL-006: 领导力分析测试 ===")

        // 1. 创建领导力相关的交互数据
        val leadershipInteractions = listOf(
            CollaborativeInteraction(
                id = "interaction_001",
                sessionId = SessionId.generate(),
                activityId = "activity_001",
                participantId = StudentId("student_001"),
                type = InteractionType.QUESTION,
                content = InteractionContent(
                    text = "大家觉得我们应该如何安排这个项目的进度？",
                    attachments = emptyList(),
                    mentions = emptyList(),
                    tags = listOf("项目管理", "协调")
                ),
                timestamp = Clock.System.now() - 60.minutes,
                responses = listOf(
                    InteractionResponse(
                        participantId = StudentId("student_002"),
                        type = ResponseType.HELPFUL,
                        content = "好的建议",
                        timestamp = Clock.System.now() - 55.minutes
                    ),
                    InteractionResponse(
                        participantId = StudentId("student_003"),
                        type = ResponseType.AGREE,
                        content = "同意",
                        timestamp = Clock.System.now() - 50.minutes
                    )
                )
            ),
            CollaborativeInteraction(
                id = "interaction_002",
                sessionId = SessionId.generate(),
                activityId = "activity_001",
                participantId = StudentId("student_001"),
                type = InteractionType.SHARE,
                content = InteractionContent(
                    text = "我整理了一个项目计划，大家可以参考",
                    attachments = listOf(
                        Attachment(
                            id = "att_001",
                            type = AttachmentType.FILE,
                            name = "项目计划.pdf",
                            url = "https://example.com/plan.pdf",
                            size = 1024,
                            mimeType = "application/pdf"
                        )
                    ),
                    mentions = emptyList(),
                    tags = listOf("计划", "组织")
                ),
                timestamp = Clock.System.now() - 45.minutes,
                responses = listOf(
                    InteractionResponse(
                        participantId = StudentId("student_002"),
                        type = ResponseType.LIKE,
                        content = "很有用",
                        timestamp = Clock.System.now() - 40.minutes
                    )
                )
            ),
            CollaborativeInteraction(
                id = "interaction_003",
                sessionId = SessionId.generate(),
                activityId = "activity_001",
                participantId = StudentId("student_001"),
                type = InteractionType.COMMENT,
                content = InteractionContent(
                    text = "如果大家有困难，我可以帮助解决",
                    attachments = emptyList(),
                    mentions = emptyList(),
                    tags = listOf("支持", "帮助")
                ),
                timestamp = Clock.System.now() - 30.minutes,
                responses = emptyList()
            )
        )

        val studentId = StudentId("student_001")

        // 2. 计算领导力分数
        val leadershipScore = leadershipAnalyzer.calculateLeadershipScore(studentId, leadershipInteractions)

        assertTrue(leadershipScore >= 0.0 && leadershipScore <= 1.0)
        println("✅ 领导力分数计算完成: ${String.format("%.2f", leadershipScore)}")

        // 3. 分析领导力类型
        val leadershipTypeAnalysis = leadershipAnalyzer.analyzeLeadershipType(studentId, leadershipInteractions)

        assertNotNull(leadershipTypeAnalysis)
        assertNotNull(leadershipTypeAnalysis.dominantType)
        assertTrue(leadershipTypeAnalysis.typeScores.isNotEmpty())
        assertTrue(leadershipTypeAnalysis.leadershipStrengths.isNotEmpty())
        assertTrue(leadershipTypeAnalysis.recommendations.isNotEmpty())

        println("✅ 领导力类型分析完成")
        println("   主导类型: ${leadershipTypeAnalysis.dominantType}")
        println("   类型分数:")
        for ((type, score) in leadershipTypeAnalysis.typeScores) {
            println("     ${type}: ${String.format("%.2f", score)}")
        }
        println("   领导力优势: ${leadershipTypeAnalysis.leadershipStrengths.joinToString(", ")}")
        println("   发展领域: ${leadershipTypeAnalysis.developmentAreas.joinToString(", ")}")
        println("   建议: ${leadershipTypeAnalysis.recommendations.joinToString(", ")}")

        // 4. 评估领导效果
        val groupPerformance = GroupPerformanceMetrics(
            overallScore = 0.85,
            taskCompletion = 0.90,
            qualityScore = 0.80,
            collaborationScore = 0.85,
            timeEfficiency = 0.85
        )

        val effectivenessEvaluation = leadershipAnalyzer.evaluateLeadershipEffectiveness(
            studentId = studentId,
            interactions = leadershipInteractions,
            groupPerformance = groupPerformance
        )

        assertNotNull(effectivenessEvaluation)
        assertTrue(effectivenessEvaluation.overallEffectiveness >= 0.0 && effectivenessEvaluation.overallEffectiveness <= 1.0)
        assertTrue(effectivenessEvaluation.leadershipScore >= 0.0 && effectivenessEvaluation.leadershipScore <= 1.0)
        assertTrue(effectivenessEvaluation.followerResponse >= 0.0 && effectivenessEvaluation.followerResponse <= 1.0)
        assertNotNull(effectivenessEvaluation.effectivenessLevel)

        println("✅ 领导效果评估完成")
        println("   整体效果: ${String.format("%.2f", effectivenessEvaluation.overallEffectiveness)}")
        println("   领导力分数: ${String.format("%.2f", effectivenessEvaluation.leadershipScore)}")
        println("   跟随者响应: ${String.format("%.2f", effectivenessEvaluation.followerResponse)}")
        println("   目标达成: ${String.format("%.2f", effectivenessEvaluation.goalAchievement)}")
        println("   团队凝聚力: ${String.format("%.2f", effectivenessEvaluation.teamCohesion)}")
        println("   效果等级: ${effectivenessEvaluation.effectivenessLevel}")
        println("   改进建议: ${effectivenessEvaluation.improvementSuggestions.joinToString(", ")}")

        println("=== CL-006 测试完成 ===\n")
    }

    @Test
    fun testCollaborativeLearningIntegration() = runBlocking {
        println("=== 智能协作学习平台集成测试 ===")

        // 1. 创建完整的协作学习场景
        val sessionResult = collaborativeLearningService.createCollaborativeSession(
            facilitatorId = "teacher_001",
            title = "算法设计协作学习",
            description = "通过小组协作学习算法设计和分析",
            subject = Subject.COMPUTER_SCIENCE,
            topic = "动态规划算法",
            sessionType = CollaborativeSessionType.GROUP_PROJECT,
            learningObjectives = listOf(
                "理解动态规划思想",
                "掌握状态转移方程",
                "解决实际算法问题"
            )
        )

        assertTrue(sessionResult is CollaborativeSessionResult.Success)
        val session = (sessionResult as CollaborativeSessionResult.Success).session

        // 2. 多个学生加入并开始协作
        val students = listOf("alice", "bob", "charlie", "diana", "eve", "frank")
        for (studentId in students) {
            collaborativeLearningService.joinSession(
                sessionId = session.id,
                studentId = StudentId(studentId)
            )
        }

        collaborativeLearningService.startSession(session.id)

        // 3. 智能分组
        val groupingResult = collaborativeLearningService.performIntelligentGrouping(
            sessionId = session.id,
            activityId = "dp_project",
            groupConfiguration = GroupConfiguration(
                groupSizeMin = 2,
                groupSizeMax = 3,
                groupingStrategy = GroupingStrategy.MIXED_ABILITY
            )
        )

        assertTrue(groupingResult is GroupMatchingResultType.Success)
        println("✅ 智能分组完成")

        // 4. 模拟协作交互
        val collaborationInteractions = listOf(
            Triple("alice", InteractionType.QUESTION, "我们应该从哪个问题开始？"),
            Triple("bob", InteractionType.ANSWER, "建议从斐波那契数列开始，比较简单"),
            Triple("charlie", InteractionType.SHARE, "我找到了一些相关资料"),
            Triple("diana", InteractionType.COMMENT, "我们可以分工合作，每人负责一个子问题"),
            Triple("eve", InteractionType.QUESTION, "状态转移方程怎么写？"),
            Triple("frank", InteractionType.ANSWER, "dp[i] = dp[i-1] + dp[i-2]")
        )

        for ((studentId, type, content) in collaborationInteractions) {
            collaborativeLearningService.processCollaborativeInteraction(
                sessionId = session.id,
                participantId = StudentId(studentId),
                type = type,
                content = InteractionContent(text = content)
            )
        }

        println("✅ 协作交互处理完成")

        // 5. 实时分析和监控
        val analysisResult = collaborativeLearningService.generateCollaborationAnalysis(session.id)
        assertTrue(analysisResult is CollaborationAnalysisResult.Success)

        val analysis = (analysisResult as CollaborationAnalysisResult.Success).analysis
        println("✅ 实时分析完成")
        println("   参与者数量: ${analysis.participationMetrics.size}")
        println("   小组凝聚力: ${String.format("%.2f", analysis.groupDynamics.cohesionScore)}")
        println("   建议数量: ${analysis.recommendations.size}")

        // 6. 同伴学习推荐
        val peerRecommendations = collaborativeLearningService.getPeerLearningRecommendations(
            studentId = StudentId("alice"),
            subject = Subject.COMPUTER_SCIENCE,
            topic = "动态规划"
        )

        assertTrue(peerRecommendations.isNotEmpty())
        println("✅ 同伴学习推荐生成: ${peerRecommendations.size} 个")

        // 7. 结束会话并生成报告
        collaborativeLearningService.endSession(session.id)

        println("✅ 智能协作学习平台集成测试完成")
        println("   ✓ 会话管理")
        println("   ✓ 智能分组")
        println("   ✓ 协作交互")
        println("   ✓ 实时分析")
        println("   ✓ 同伴推荐")
        println("   ✓ 报告生成")

        println("=== 集成测试完成 ===\n")
    }

    // ==================== 辅助方法 ====================
    
    private fun createTestSession(): CollaborativeSession {
        return CollaborativeSession(
            id = SessionId.generate(),
            title = "测试协作会话",
            description = "用于测试的协作学习会话",
            subject = Subject.MATHEMATICS,
            topic = "微积分",
            facilitatorId = "teacher_001",
            participants = listOf(
                SessionParticipant(
                    studentId = StudentId("student_001"),
                    role = ParticipantRole.LEADER,
                    joinedAt = Clock.System.now() - 1.hours,
                    status = ParticipantStatus.ACTIVE,
                    contributionScore = 15.0,
                    engagementLevel = EngagementLevel.HIGH
                ),
                SessionParticipant(
                    studentId = StudentId("student_002"),
                    role = ParticipantRole.MEMBER,
                    joinedAt = Clock.System.now() - 1.hours,
                    status = ParticipantStatus.ACTIVE,
                    contributionScore = 12.0,
                    engagementLevel = EngagementLevel.MEDIUM
                ),
                SessionParticipant(
                    studentId = StudentId("student_003"),
                    role = ParticipantRole.MEMBER,
                    joinedAt = Clock.System.now() - 1.hours,
                    status = ParticipantStatus.ACTIVE,
                    contributionScore = 8.0,
                    engagementLevel = EngagementLevel.MEDIUM
                )
            ),
            sessionType = CollaborativeSessionType.STUDY_GROUP,
            learningObjectives = listOf("理解导数", "掌握积分", "应用微积分"),
            activities = listOf(
                CollaborativeActivity(
                    id = "activity_001",
                    sessionId = SessionId.generate(),
                    type = ActivityType.GROUP_DISCUSSION,
                    title = "导数讨论",
                    description = "讨论导数的概念和应用",
                    instructions = "每个人分享自己的理解",
                    resources = emptyList(),
                    timeline = ActivityTimeline(
                        startTime = Clock.System.now() - 30.minutes,
                        endTime = Clock.System.now(),
                        phases = emptyList()
                    ),
                    groupConfiguration = GroupConfiguration(
                        groupSizeMin = 2,
                        groupSizeMax = 3,
                        groupingStrategy = GroupingStrategy.RANDOM
                    ),
                    assessmentCriteria = emptyList(),
                    status = ActivityStatus.COMPLETED,
                    createdAt = Clock.System.now() - 1.hours
                )
            ),
            status = SessionStatus.COMPLETED,
            settings = SessionSettings(),
            createdAt = Clock.System.now() - 2.hours,
            startTime = Clock.System.now() - 1.hours,
            endTime = Clock.System.now()
        )
    }
}
