package ai.kastrax.edutech

import ai.kastrax.edutech.integration.*
import ai.kastrax.edutech.recommendation.*
import ai.kastrax.edutech.models.*
import ai.kastrax.rag.RAG
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * 第二阶段功能测试
 * 
 * 验证ed2.md第二阶段Week 5-6的LMS集成、个性化推荐和学习档案功能
 */
class Phase2FunctionalityTest {
    
    @Test
    fun `should create and manage LMS connections`() = runTest {
        // Given
        val lmsService = LMSIntegrationService()
        val moodleConnector = MoodleConnector()
        
        // 注册连接器
        lmsService.registerConnector(LMSType.MOODLE, moodleConnector)
        
        val config = LMSConnectionConfig(
            lmsType = LMSType.MOODLE,
            serverUrl = "https://moodle.example.com",
            apiKey = "test_api_key_123"
        )
        
        // When
        val connectionResult = lmsService.createConnection(config)
        
        // Then
        assertTrue(connectionResult is LMSConnectionResult.Success)
        val successResult = connectionResult as LMSConnectionResult.Success
        assertNotNull(successResult.connectionId)
        assertEquals(LMSType.MOODLE, successResult.lmsType)
        assertEquals(ConnectionStatus.CONNECTED, successResult.status)
    }
    
    @Test
    fun `should sync courses from LMS`() = runTest {
        // Given
        val lmsService = LMSIntegrationService()
        val moodleConnector = MoodleConnector()
        lmsService.registerConnector(LMSType.MOODLE, moodleConnector)
        
        val config = LMSConnectionConfig(
            lmsType = LMSType.MOODLE,
            serverUrl = "https://moodle.example.com",
            apiKey = "test_api_key_123"
        )
        
        val connectionResult = lmsService.createConnection(config)
        assertTrue(connectionResult is LMSConnectionResult.Success)
        val connectionId = (connectionResult as LMSConnectionResult.Success).connectionId
        
        // When
        val syncResult = lmsService.syncCourses(connectionId)
        
        // Then
        assertTrue(syncResult is SyncResult.Success)
        val successResult = syncResult as SyncResult.Success
        assertTrue(successResult.syncedCount > 0)
        assertEquals("同步完成", successResult.message)
    }
    
    @Test
    fun `should sync students from LMS`() = runTest {
        // Given
        val lmsService = LMSIntegrationService()
        val moodleConnector = MoodleConnector()
        lmsService.registerConnector(LMSType.MOODLE, moodleConnector)
        
        val config = LMSConnectionConfig(
            lmsType = LMSType.MOODLE,
            serverUrl = "https://moodle.example.com",
            apiKey = "test_api_key_123"
        )
        
        val connectionResult = lmsService.createConnection(config)
        assertTrue(connectionResult is LMSConnectionResult.Success)
        val connectionId = (connectionResult as LMSConnectionResult.Success).connectionId
        
        // When
        val syncResult = lmsService.syncStudents(connectionId)
        
        // Then
        assertTrue(syncResult is SyncResult.Success)
        val successResult = syncResult as SyncResult.Success
        assertTrue(successResult.syncedCount > 0)
        assertEquals("学生数据同步完成", successResult.message)
    }
    
    @Test
    fun `should sync grades from LMS`() = runTest {
        // Given
        val lmsService = LMSIntegrationService()
        val moodleConnector = MoodleConnector()
        lmsService.registerConnector(LMSType.MOODLE, moodleConnector)
        
        val config = LMSConnectionConfig(
            lmsType = LMSType.MOODLE,
            serverUrl = "https://moodle.example.com",
            apiKey = "test_api_key_123"
        )
        
        val connectionResult = lmsService.createConnection(config)
        assertTrue(connectionResult is LMSConnectionResult.Success)
        val connectionId = (connectionResult as LMSConnectionResult.Success).connectionId
        
        val courseId = CourseId.generate()
        
        // When
        val syncResult = lmsService.syncGrades(connectionId, courseId)
        
        // Then
        assertTrue(syncResult is SyncResult.Success)
        val successResult = syncResult as SyncResult.Success
        assertTrue(successResult.syncedCount > 0)
        assertEquals("成绩数据同步完成", successResult.message)
    }
    
    @Test
    fun `should create and manage learning profiles`() = runTest {
        // Given
        val profileService = LearningProfileService()
        val studentId = StudentId.generate()
        
        val initialData = ProfileInitialData(
            learningStyle = LearningStyle.VISUAL,
            interests = listOf("数学", "编程"),
            goals = listOf("提高数学成绩", "学会编程")
        )
        
        // When
        val createResult = profileService.createProfile(studentId, initialData)
        
        // Then
        assertTrue(createResult is ProfileResult.Success)
        val profile = (createResult as ProfileResult.Success).profile
        assertEquals(studentId, profile.studentId)
        assertEquals(LearningStyle.VISUAL, profile.learningStyle)
        assertEquals(listOf("提高数学成绩", "学会编程"), profile.goals)
        
        // Test profile retrieval
        val retrievedProfile = profileService.getProfile(studentId)
        assertNotNull(retrievedProfile)
        assertEquals(profile.studentId, retrievedProfile.studentId)
    }
    
    @Test
    fun `should update learning profiles`() = runTest {
        // Given
        val profileService = LearningProfileService()
        val studentId = StudentId.generate()
        
        // Create initial profile
        profileService.createProfile(studentId)
        
        val updates = ProfileUpdates(
            learningStyle = LearningStyle.KINESTHETIC,
            skillLevels = mapOf(Skill.LOGICAL_REASONING to SkillLevel.INTERMEDIATE),
            preferences = mapOf("preferredTime" to "morning"),
            goals = listOf("掌握高级数学")
        )
        
        // When
        val updateResult = profileService.updateProfile(studentId, updates)
        
        // Then
        assertTrue(updateResult is ProfileResult.Success)
        val updatedProfile = (updateResult as ProfileResult.Success).profile
        assertEquals(LearningStyle.KINESTHETIC, updatedProfile.learningStyle)
        assertEquals(SkillLevel.INTERMEDIATE, updatedProfile.skillLevels[Skill.LOGICAL_REASONING])
        assertEquals("morning", updatedProfile.preferences["preferredTime"])
        assertEquals(listOf("掌握高级数学"), updatedProfile.goals)
    }
    
    @Test
    fun `should assess learning style`() = runTest {
        // Given
        val profileService = LearningProfileService()
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        val responses = listOf(
            StyleAssessmentResponse(
                questionId = "q1",
                answer = "visual",
                styleWeights = mapOf(
                    LearningStyle.VISUAL to 3,
                    LearningStyle.AUDITORY to 1
                )
            ),
            StyleAssessmentResponse(
                questionId = "q2",
                answer = "diagrams",
                styleWeights = mapOf(
                    LearningStyle.VISUAL to 2,
                    LearningStyle.KINESTHETIC to 1
                )
            )
        )
        
        // When
        val assessmentResult = profileService.assessLearningStyle(studentId, responses)
        
        // Then
        assertTrue(assessmentResult is StyleAssessmentResult.Success)
        val result = assessmentResult as StyleAssessmentResult.Success
        assertEquals(LearningStyle.VISUAL, result.learningStyle)
        assertTrue(result.confidence > 0.0)
        assertTrue(result.recommendations.isNotEmpty())
    }
    
    @Test
    fun `should assess skill levels`() = runTest {
        // Given
        val profileService = LearningProfileService()
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        val assessmentData = SkillAssessmentData(
            scores = listOf(0.8, 0.75, 0.85, 0.9),
            timeSpent = 300,
            attempts = 4
        )
        
        // When
        val assessmentResult = profileService.assessSkillLevel(
            studentId,
            Skill.LOGICAL_REASONING,
            assessmentData
        )
        
        // Then
        assertTrue(assessmentResult is SkillAssessmentResult.Success)
        val result = assessmentResult as SkillAssessmentResult.Success
        assertEquals(Skill.LOGICAL_REASONING, result.skill)
        assertEquals(SkillLevel.ADVANCED, result.level)
        assertTrue(result.confidence > 0.0)
        assertTrue(result.recommendations.isNotEmpty())
    }
    
    @Test
    fun `should generate personalized learning plans`() = runTest {
        // Given
        val ragSystem = mockk<RAG>(relaxed = true)
        val profileService = LearningProfileService()
        val personalizationEngine = PersonalizationEngine(ragSystem, profileService)
        
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        val courseId = CourseId.generate()
        val objectives = listOf("学习基础数学", "提高解题能力")
        
        // When
        val planResult = personalizationEngine.generateLearningPlan(
            studentId = studentId,
            courseId = courseId,
            objectives = objectives
        )
        
        // Then
        assertTrue(planResult is LearningPlanResult.Success)
        val plan = (planResult as LearningPlanResult.Success).plan
        assertEquals(studentId, plan.studentId)
        assertEquals(courseId, plan.courseId)
        assertEquals(2, plan.objectives.size)
        assertTrue(plan.estimatedDuration > 0)
    }
    
    @Test
    fun `should generate content recommendations`() = runTest {
        // Given
        val ragSystem = mockk<RAG>(relaxed = true)
        val profileService = LearningProfileService()
        val personalizationEngine = PersonalizationEngine(ragSystem, profileService)
        
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        // Mock RAG search results
        val mockSearchResults = listOf(
            ai.kastrax.store.document.DocumentSearchResult(
                document = ai.kastrax.store.document.Document(
                    id = "content_1",
                    content = "数学基础内容",
                    metadata = mapOf(
                        "contentId" to "content_1",
                        "title" to "数学基础",
                        "type" to "TEXT",
                        "difficulty" to "BEGINNER"
                    )
                ),
                score = 0.9
            )
        )
        coEvery { ragSystem.search(any(), any()) } returns mockSearchResults
        
        val context = RecommendationContext(
            currentTopic = Topic("数学"),
            learningObjectives = listOf("学习基础数学"),
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.BEGINNER
        )
        
        // When
        val recommendationResult = personalizationEngine.generateRecommendations(
            studentId = studentId,
            context = context,
            limit = 5
        )
        
        // Then
        assertTrue(recommendationResult is RecommendationResult.Success)
        val recommendations = (recommendationResult as RecommendationResult.Success).recommendations
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.first().score > 0.0)
        assertTrue(recommendations.first().reason.isNotBlank())
    }
    
    @Test
    fun `should generate learning advice`() = runTest {
        // Given
        val ragSystem = mockk<RAG>(relaxed = true)
        val profileService = LearningProfileService()
        val personalizationEngine = PersonalizationEngine(ragSystem, profileService)
        
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        // When
        val adviceResult = personalizationEngine.getLearningAdvice(studentId)
        
        // Then
        assertTrue(adviceResult is LearningAdviceResult.Success)
        val advice = (adviceResult as LearningAdviceResult.Success).advice
        assertTrue(advice.recommendations.isNotEmpty())
        assertTrue(advice.tips.isNotEmpty())
    }
    
    @Test
    fun `should record learning activities in profile`() = runTest {
        // Given
        val profileService = LearningProfileService()
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        val activity = LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("数学基础"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING)
        )
        
        val performance = ActivityPerformance(
            accuracy = 0.85,
            completionTime = 300,
            engagementLevel = 0.9,
            completed = true
        )
        
        // When
        val recordResult = profileService.recordLearningActivity(studentId, activity, performance)
        
        // Then
        assertTrue(recordResult is ProfileUpdateResult.Success)
        
        // Verify activity was recorded
        val profile = profileService.getProfile(studentId)
        assertNotNull(profile)
        assertEquals(1, profile.activityHistory.size)
        assertEquals(activity.id, profile.activityHistory.first().activityId)
        assertEquals(performance.accuracy, profile.activityHistory.first().performance.accuracy)
    }
    
    @Test
    fun `should generate learning statistics`() = runTest {
        // Given
        val profileService = LearningProfileService()
        val studentId = StudentId.generate()
        profileService.createProfile(studentId)
        
        // Record some activities
        val activity1 = LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("数学"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING)
        )
        
        val activity2 = LearningActivity.create(
            type = ActivityType.PRACTICE,
            topic = Topic("物理"),
            difficulty = DifficultyLevel.INTERMEDIATE,
            skillsInvolved = setOf(Skill.PROBLEM_SOLVING)
        )
        
        val performance1 = ActivityPerformance(0.8, 300, 0.9, true)
        val performance2 = ActivityPerformance(0.9, 250, 0.85, true)
        
        profileService.recordLearningActivity(studentId, activity1, performance1)
        profileService.recordLearningActivity(studentId, activity2, performance2)
        
        // When
        val statisticsResult = profileService.getLearningStatistics(studentId)
        
        // Then
        assertTrue(statisticsResult is StatisticsResult.Success)
        val statistics = (statisticsResult as StatisticsResult.Success).statistics
        assertEquals(2, statistics.totalActivities)
        assertEquals(550, statistics.totalLearningTime)
        assertEquals(0.85, statistics.averagePerformance, 0.001)
        assertEquals(1.0, statistics.completionRate)
        assertTrue(statistics.preferredActivityTypes.isNotEmpty())
    }
}
