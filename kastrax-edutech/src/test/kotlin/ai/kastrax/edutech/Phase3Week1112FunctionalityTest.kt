package ai.kastrax.edutech

import ai.kastrax.edutech.analytics.*
import ai.kastrax.edutech.optimization.*
import ai.kastrax.edutech.multimodal.*
import ai.kastrax.edutech.multimodal.ContentType as MultimodalContentType
import ai.kastrax.core.llm.*
import ai.kastrax.edutech.models.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration

/**
 * 第三阶段Week 11-12功能测试
 * 测试学习分析和系统优化功能
 */
class Phase3Week1112FunctionalityTest {
    
    private lateinit var mockLlmProvider: LlmProvider
    private lateinit var learningAnalyticsService: LearningAnalyticsService
    private lateinit var systemOptimizationService: SystemOptimizationService
    private lateinit var multimodalContentProcessor: MultimodalContentProcessor
    
    @BeforeTest
    fun setup() {
        mockLlmProvider = mockk<LlmProvider>()
        
        // Mock LLM responses
        coEvery { mockLlmProvider.generate(any(), any()) } returns LlmResponse(
            content = "分析结果：关键概念包括机器学习、深度学习、神经网络。语言：中文。情感：积极。复杂度：7。教育价值：8。",
            usage = LlmUsage(100, 50, 150),
            finishReason = "completed"
        )
        
        // 设置学习分析服务
        val mockPatternRecognizer = mockk<LearningPatternRecognizer>()
        val mockPredictiveAnalyzer = mockk<PredictiveAnalyzer>()
        val mockRiskAssessment = mockk<RiskAssessmentEngine>()
        val mockInterventionEngine = mockk<InterventionEngine>()
        
        learningAnalyticsService = LearningAnalyticsService(
            llmProvider = mockLlmProvider,
            patternRecognizer = mockPatternRecognizer,
            predictiveAnalyzer = mockPredictiveAnalyzer,
            riskAssessment = mockRiskAssessment,
            interventionEngine = mockInterventionEngine
        )
        
        // Mock 学习分析组件
        coEvery { mockPatternRecognizer.identifyLearningPatterns(any(), any()) } returns createMockLearningPatterns()
        coEvery { mockPatternRecognizer.identifyRealTimePatterns(any(), any()) } returns createMockRealTimePatterns()
        coEvery { mockPredictiveAnalyzer.generatePredictions(any(), any(), any()) } returns createMockPredictions()
        coEvery { mockRiskAssessment.assessLearningRisks(any(), any(), any()) } returns createMockRiskAssessment()
        coEvery { mockRiskAssessment.assessImmediateRisks(any(), any(), any()) } returns createMockImmediateRisks()
        coEvery { mockInterventionEngine.generateInterventions(any(), any(), any()) } returns createMockInterventions()
        coEvery { mockInterventionEngine.generateRealTimeInterventions(any(), any(), any()) } returns createMockRealTimeInterventions()
        
        // 设置系统优化服务
        val mockDatabaseOptimizer = mockk<DatabaseOptimizer>()
        val mockCacheOptimizer = mockk<CacheOptimizer>()
        val mockActorSystemOptimizer = mockk<ActorSystemOptimizer>()
        val mockMemoryOptimizer = mockk<MemoryOptimizer>()
        val mockPerformanceMonitor = mockk<PerformanceMonitor>()
        
        systemOptimizationService = SystemOptimizationService(
            databaseOptimizer = mockDatabaseOptimizer,
            cacheOptimizer = mockCacheOptimizer,
            actorSystemOptimizer = mockActorSystemOptimizer,
            memoryOptimizer = mockMemoryOptimizer,
            performanceMonitor = mockPerformanceMonitor
        )
        
        // Mock 系统优化组件
        coEvery { mockPerformanceMonitor.captureBaselineMetrics() } returns createMockPerformanceMetrics()
        coEvery { mockPerformanceMonitor.captureOptimizedMetrics() } returns createMockOptimizedPerformanceMetrics()
        coEvery { mockPerformanceMonitor.startRealTimeSession(any()) } returns createMockMonitoringSessionData()
        coEvery { mockDatabaseOptimizer.optimizeDatabase() } returns createMockDatabaseOptimization()
        coEvery { mockCacheOptimizer.optimizeCache() } returns createMockCacheOptimization()
        coEvery { mockActorSystemOptimizer.optimizeActorSystem() } returns createMockActorOptimization()
        coEvery { mockMemoryOptimizer.optimizeMemoryUsage() } returns createMockMemoryOptimization()
        
        // 设置多模态内容处理器
        val mockVideoAnalyzer = mockk<VideoAnalyzer>()
        val mockAudioProcessor = mockk<AudioProcessor>()
        val mockImageRecognizer = mockk<ImageRecognizer>()
        val mockInteractiveContentEngine = mockk<InteractiveContentEngine>()
        
        multimodalContentProcessor = MultimodalContentProcessor(
            llmProvider = mockLlmProvider,
            videoAnalyzer = mockVideoAnalyzer,
            audioProcessor = mockAudioProcessor,
            imageRecognizer = mockImageRecognizer,
            interactiveContentEngine = mockInteractiveContentEngine
        )
        
        // Mock 多模态处理组件
        coEvery { mockVideoAnalyzer.analyzeBasicInfo(any()) } returns createMockVideoBasicInfo()
        coEvery { mockVideoAnalyzer.analyzeContent(any()) } returns createMockVideoContentAnalysis()
        coEvery { mockVideoAnalyzer.segmentScenes(any()) } returns createMockVideoScenes()
        coEvery { mockVideoAnalyzer.extractSubtitles(any()) } returns listOf("字幕1", "字幕2", "字幕3")
        
        coEvery { mockAudioProcessor.analyzeBasicInfo(any()) } returns createMockAudioBasicInfo()
        coEvery { mockAudioProcessor.recognizeSpeech(any()) } returns createMockSpeechRecognition()
        coEvery { mockAudioProcessor.analyzeQuality(any()) } returns createMockAudioQuality()
        coEvery { mockAudioProcessor.detectEmotion(any()) } returns createMockEmotionAnalysis()
        
        coEvery { mockImageRecognizer.analyzeBasicInfo(any()) } returns createMockImageBasicInfo()
        coEvery { mockImageRecognizer.recognizeObjects(any()) } returns createMockRecognizedObjects()
        coEvery { mockImageRecognizer.recognizeText(any()) } returns createMockTextRecognition()
        coEvery { mockImageRecognizer.analyzeScene(any()) } returns createMockSceneAnalysis()
        
        coEvery { mockInteractiveContentEngine.analyzeElements(any()) } returns createMockElementAnalysis()
        coEvery { mockInteractiveContentEngine.analyzeUserExperience(any()) } returns createMockUXAnalysis()
        coEvery { mockInteractiveContentEngine.analyzeLearningEffectiveness(any()) } returns createMockLearningEffectiveness()
    }
    
    @Test
    fun `should perform comprehensive learning analysis successfully`() = runTest {
        // Given
        val studentId = StudentId("student_001")
        val analysisRequest = LearningAnalysisRequest(
            timeRange = TimeRange(
                start = Clock.System.now().minus(Duration.parse("P30D")),
                end = Clock.System.now()
            ),
            analysisTypes = listOf(
                AnalysisType.PATTERN_RECOGNITION,
                AnalysisType.PREDICTIVE_ANALYSIS,
                AnalysisType.RISK_ASSESSMENT,
                AnalysisType.INTERVENTION_PLANNING
            ),
            predictionHorizon = Duration.parse("P30D")
        )
        
        // When
        val result = learningAnalyticsService.performComprehensiveAnalysis(studentId, analysisRequest)
        
        // Then
        assertTrue(result is LearningAnalysisResult.Success)
        val successResult = result as LearningAnalysisResult.Success
        
        assertEquals(studentId, successResult.studentId)
        assertNotNull(successResult.learningPatterns)
        assertNotNull(successResult.predictions)
        assertNotNull(successResult.riskAssessment)
        assertTrue(successResult.interventions.isNotEmpty())
        assertNotNull(successResult.analyticsReport)
    }
    
    @Test
    fun `should perform batch learning analysis successfully`() = runTest {
        // Given
        val studentIds = listOf(
            StudentId("student_001"),
            StudentId("student_002"),
            StudentId("student_003")
        )
        val analysisRequest = LearningAnalysisRequest(
            timeRange = TimeRange(
                start = Clock.System.now().minus(Duration.parse("P7D")),
                end = Clock.System.now()
            ),
            analysisTypes = listOf(AnalysisType.PATTERN_RECOGNITION),
            predictionHorizon = Duration.parse("P7D")
        )
        
        // When
        val result = learningAnalyticsService.performBatchAnalysis(studentIds, analysisRequest)
        
        // Then
        assertEquals(3, result.totalStudents)
        assertEquals(3, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(3, result.results.size)
        assertNotNull(result.batchSummary)
    }
    
    @Test
    fun `should perform real-time learning monitoring successfully`() = runTest {
        // Given
        val studentId = StudentId("student_001")
        val currentSession = ai.kastrax.edutech.analytics.LearningSession(
            id = "session_001",
            studentId = studentId.value,
            startTime = Clock.System.now(),
            duration = Duration.parse("PT30M"),
            contentType = "video",
            completed = false
        )
        
        // When
        val result = learningAnalyticsService.performRealTimeMonitoring(studentId, currentSession)
        
        // Then
        assertEquals(studentId, result.studentId)
        assertEquals(currentSession.id, result.sessionId)
        assertTrue(result.currentPatterns.isNotEmpty())
        assertNotNull(result.alertLevel)
    }
    
    @Test
    fun `should perform comprehensive system optimization successfully`() = runTest {
        // When
        val result = systemOptimizationService.performComprehensiveOptimization()
        
        // Then
        assertTrue(result is SystemOptimizationResult.Success)
        val successResult = result as SystemOptimizationResult.Success
        
        assertNotNull(successResult.optimizationId)
        assertNotNull(successResult.baselineMetrics)
        assertNotNull(successResult.optimizedMetrics)
        assertNotNull(successResult.databaseOptimization)
        assertNotNull(successResult.cacheOptimization)
        assertNotNull(successResult.actorOptimization)
        assertNotNull(successResult.memoryOptimization)
        assertNotNull(successResult.performanceImprovement)
        assertNotNull(successResult.optimizationReport)
    }
    
    @Test
    fun `should optimize specific system component successfully`() = runTest {
        // Given
        val component = SystemComponent.DATABASE
        
        // When
        val result = systemOptimizationService.optimizeComponent(component)
        
        // Then
        assertEquals(component, result.component)
        assertTrue(result.success)
        assertTrue(result.improvements.isNotEmpty())
        assertTrue(result.metrics.isNotEmpty())
    }
    
    @Test
    fun `should start real-time performance monitoring successfully`() = runTest {
        // When
        val session = systemOptimizationService.startRealTimeMonitoring()
        
        // Then
        assertNotNull(session.sessionId)
        assertTrue(session.monitoringTargets.isNotEmpty())
        assertNotNull(session.alertThresholds)
        assertNotNull(session.session)
    }
    
    @Test
    fun `should process multimodal content successfully`() = runTest {
        // Given
        val multimodalContent = createMockMultimodalContent()
        val processingOptions = ProcessingOptions(
            enableVideoProcessing = true,
            enableAudioProcessing = true,
            enableImageProcessing = true,
            enableInteractiveProcessing = true
        )
        
        // When
        val result = multimodalContentProcessor.processMultimodalContent(multimodalContent, processingOptions)
        
        // Then
        assertTrue(result is MultimodalProcessingResult.Success)
        val successResult = result as MultimodalProcessingResult.Success
        
        assertNotNull(successResult.processingId)
        assertEquals(multimodalContent, successResult.originalContent)
        assertTrue(successResult.processingResults.isNotEmpty())
        assertNotNull(successResult.comprehensiveAnalysis)
        assertTrue(successResult.learningRecommendations.isNotEmpty())
        assertTrue(successResult.qualityScore > 0.0)
    }
    
    @Test
    fun `should process video content successfully`() = runTest {
        // Given
        val videoContent = createMockVideoContent()
        val multimodalContent = MultimodalContent(
            contentId = "content_001",
            title = "测试视频",
            description = "测试视频内容",
            videoContent = videoContent,
            audioContent = null,
            imageContent = null,
            interactiveContent = null,
            metadata = createMockContentMetadata()
        )
        val processingOptions = ProcessingOptions(
            enableVideoProcessing = true,
            enableAudioProcessing = false,
            enableImageProcessing = false,
            enableInteractiveProcessing = false
        )
        
        // When
        val result = multimodalContentProcessor.processMultimodalContent(multimodalContent, processingOptions)
        
        // Then
        assertTrue(result is MultimodalProcessingResult.Success)
        val successResult = result as MultimodalProcessingResult.Success
        
        assertTrue(successResult.processingResults.containsKey(MultimodalContentType.VIDEO))
        val videoResult = successResult.processingResults[MultimodalContentType.VIDEO]!!
        assertEquals(MultimodalContentType.VIDEO, videoResult.contentType)
        assertTrue(videoResult.analysisResults.isNotEmpty())
        assertTrue(videoResult.extractedFeatures.isNotEmpty())
    }
    
    @Test
    fun `should process audio content successfully`() = runTest {
        // Given
        val audioContent = createMockAudioContent()
        val multimodalContent = MultimodalContent(
            contentId = "content_002",
            title = "测试音频",
            description = "测试音频内容",
            videoContent = null,
            audioContent = audioContent,
            imageContent = null,
            interactiveContent = null,
            metadata = createMockContentMetadata()
        )
        val processingOptions = ProcessingOptions(
            enableVideoProcessing = false,
            enableAudioProcessing = true,
            enableImageProcessing = false,
            enableInteractiveProcessing = false
        )
        
        // When
        val result = multimodalContentProcessor.processMultimodalContent(multimodalContent, processingOptions)
        
        // Then
        assertTrue(result is MultimodalProcessingResult.Success)
        val successResult = result as MultimodalProcessingResult.Success
        
        assertTrue(successResult.processingResults.containsKey(MultimodalContentType.AUDIO))
        val audioResult = successResult.processingResults[MultimodalContentType.AUDIO]!!
        assertEquals(MultimodalContentType.AUDIO, audioResult.contentType)
        assertTrue(audioResult.analysisResults.isNotEmpty())
    }
    
    @Test
    fun `should process image content successfully`() = runTest {
        // Given
        val imageContent = listOf(createMockImageContent())
        val multimodalContent = MultimodalContent(
            contentId = "content_003",
            title = "测试图像",
            description = "测试图像内容",
            videoContent = null,
            audioContent = null,
            imageContent = imageContent,
            interactiveContent = null,
            metadata = createMockContentMetadata()
        )
        val processingOptions = ProcessingOptions(
            enableVideoProcessing = false,
            enableAudioProcessing = false,
            enableImageProcessing = true,
            enableInteractiveProcessing = false
        )
        
        // When
        val result = multimodalContentProcessor.processMultimodalContent(multimodalContent, processingOptions)
        
        // Then
        assertTrue(result is MultimodalProcessingResult.Success)
        val successResult = result as MultimodalProcessingResult.Success
        
        assertTrue(successResult.processingResults.containsKey(MultimodalContentType.IMAGE))
        val imageResult = successResult.processingResults[MultimodalContentType.IMAGE]!!
        assertEquals(MultimodalContentType.IMAGE, imageResult.contentType)
        assertTrue(imageResult.analysisResults.isNotEmpty())
    }
    
    @Test
    fun `should process interactive content successfully`() = runTest {
        // Given
        val interactiveContent = createMockInteractiveContent()
        val multimodalContent = MultimodalContent(
            contentId = "content_004",
            title = "测试交互内容",
            description = "测试交互式内容",
            videoContent = null,
            audioContent = null,
            imageContent = null,
            interactiveContent = interactiveContent,
            metadata = createMockContentMetadata()
        )
        val processingOptions = ProcessingOptions(
            enableVideoProcessing = false,
            enableAudioProcessing = false,
            enableImageProcessing = false,
            enableInteractiveProcessing = true
        )
        
        // When
        val result = multimodalContentProcessor.processMultimodalContent(multimodalContent, processingOptions)
        
        // Then
        assertTrue(result is MultimodalProcessingResult.Success)
        val successResult = result as MultimodalProcessingResult.Success
        
        assertTrue(successResult.processingResults.containsKey(MultimodalContentType.INTERACTIVE))
        val interactiveResult = successResult.processingResults[MultimodalContentType.INTERACTIVE]!!
        assertEquals(MultimodalContentType.INTERACTIVE, interactiveResult.contentType)
        assertTrue(interactiveResult.analysisResults.isNotEmpty())
    }
    
    // 辅助方法创建Mock数据
    
    private fun createMockLearningPatterns(): LearningPatterns {
        return LearningPatterns(
            studentId = StudentId("student_001"),
            identifiedPatterns = listOf(
                LearningPattern(
                    id = "pattern_001",
                    type = PatternType.TEMPORAL,
                    description = "晚间学习模式",
                    frequency = 0.8,
                    strength = 0.9,
                    confidence = 0.85,
                    firstObserved = Clock.System.now(),
                    lastObserved = Clock.System.now(),
                    associatedOutcomes = listOf("高效学习", "良好专注度")
                )
            ),
            patternStrength = 0.85,
            patternConsistency = 0.8,
            temporalPatterns = emptyList(),
            behavioralPatterns = emptyList(),
            performancePatterns = emptyList()
        )
    }
    
    private fun createMockPredictions(): LearningPredictions {
        return LearningPredictions(
            studentId = StudentId("student_001"),
            predictionHorizon = Duration.parse("P30D"),
            expectedGrade = 85.0,
            completionProbability = 0.9,
            masteryPredictions = mapOf("数学" to 0.8, "编程" to 0.85),
            riskPredictions = emptyList(),
            recommendedActions = listOf("继续当前学习策略", "增加练习时间"),
            confidence = 0.8,
            generatedAt = Clock.System.now()
        )
    }
    
    private fun createMockRiskAssessment(): RiskAssessmentResult {
        return RiskAssessmentResult(
            studentId = StudentId("student_001"),
            assessmentTimestamp = Clock.System.now(),
            overallRiskLevel = RiskLevel.LOW,
            identifiedRisks = emptyList(),
            riskFactors = emptyList(),
            mitigationRecommendations = emptyList()
        )
    }
    
    private fun createMockInterventions(): List<InterventionRecommendation> {
        return listOf(
            InterventionRecommendation(
                interventionId = "intervention_001",
                type = InterventionType.CONTENT_ADJUSTMENT,
                description = "内容难度调整",
                targetArea = "学习内容",
                urgency = Urgency.LOW,
                expectedOutcome = "提高学习效果",
                implementationSteps = listOf("评估当前水平", "调整内容难度"),
                successMetrics = listOf("理解度提升", "完成率增加"),
                timeframe = Duration.parse("P7D")
            )
        )
    }

    private fun createMockPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            averageResponseTime = Duration.parse("PT0.3S"),
            throughput = 100.0,
            memoryUsage = 0.7,
            cpuUsage = 0.6,
            databasePerformance = DatabaseMetrics(
                queryTime = Duration.parse("PT0.05S"),
                connectionPoolUtilization = 0.6,
                transactionRate = 50.0,
                lockWaitTime = Duration.parse("PT0.005S")
            ),
            cachePerformance = CacheMetrics(
                hitRate = 0.8,
                evictionRate = 0.1,
                memoryUtilization = 0.7,
                averageAccessTime = Duration.parse("PT0.002S")
            ),
            actorSystemPerformance = ActorMetrics(
                messageQueueSize = 100,
                processingTime = Duration.parse("PT0.01S"),
                throughput = 200.0,
                errorRate = 0.01
            )
        )
    }

    private fun createMockOptimizedPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            averageResponseTime = Duration.parse("PT0.2S"),
            throughput = 150.0,
            memoryUsage = 0.5,
            cpuUsage = 0.4,
            databasePerformance = DatabaseMetrics(
                queryTime = Duration.parse("PT0.03S"),
                connectionPoolUtilization = 0.4,
                transactionRate = 75.0,
                lockWaitTime = Duration.parse("PT0.002S")
            ),
            cachePerformance = CacheMetrics(
                hitRate = 0.9,
                evictionRate = 0.05,
                memoryUtilization = 0.5,
                averageAccessTime = Duration.parse("PT0.001S")
            ),
            actorSystemPerformance = ActorMetrics(
                messageQueueSize = 50,
                processingTime = Duration.parse("PT0.005S"),
                throughput = 300.0,
                errorRate = 0.005
            )
        )
    }

    private fun createMockDatabaseOptimization(): DatabaseOptimizationResult {
        return DatabaseOptimizationResult(
            success = true,
            improvements = listOf("查询优化", "索引优化", "连接池调优"),
            performanceMetrics = mapOf(
                "query_time_improvement" to 40.0,
                "connection_pool_efficiency" to 33.0
            ),
            optimizedQueries = listOf("SELECT优化", "JOIN优化"),
            indexOptimizations = listOf("添加复合索引", "删除无用索引")
        )
    }

    private fun createMockCacheOptimization(): CacheOptimizationResult {
        return CacheOptimizationResult(
            success = true,
            improvements = listOf("缓存策略优化", "缓存容量调整", "淘汰策略优化"),
            performanceMetrics = mapOf(
                "hit_rate_improvement" to 12.5,
                "eviction_rate_reduction" to 50.0
            ),
            cacheStrategyChanges = listOf("LRU策略调整", "TTL优化"),
            hitRateImprovement = 12.5
        )
    }

    private fun createMockActorOptimization(): ActorOptimizationResult {
        return ActorOptimizationResult(
            success = true,
            improvements = listOf("Actor池大小优化", "消息处理优化", "负载均衡改进"),
            performanceMetrics = mapOf(
                "processing_time_improvement" to 50.0,
                "throughput_improvement" to 50.0
            ),
            actorPoolAdjustments = listOf("增加Actor实例", "优化分发策略"),
            messageProcessingOptimizations = listOf("批处理优化", "异步处理改进")
        )
    }

    private fun createMockMemoryOptimization(): MemoryOptimizationResult {
        return MemoryOptimizationResult(
            success = true,
            improvements = listOf("内存泄漏修复", "GC优化", "对象池优化"),
            performanceMetrics = mapOf(
                "memory_usage_reduction" to 28.6,
                "gc_frequency_reduction" to 33.3
            ),
            memoryLeakFixes = listOf("修复循环引用", "优化缓存清理"),
            gcOptimizations = listOf("G1GC参数调优", "堆大小优化")
        )
    }

    private fun createMockMultimodalContent(): MultimodalContent {
        return MultimodalContent(
            contentId = "content_multimodal_001",
            title = "综合学习内容",
            description = "包含视频、音频、图像和交互式内容的综合学习材料",
            videoContent = createMockVideoContent(),
            audioContent = createMockAudioContent(),
            imageContent = listOf(createMockImageContent()),
            interactiveContent = createMockInteractiveContent(),
            metadata = createMockContentMetadata()
        )
    }

    private fun createMockVideoContent(): VideoContent {
        return VideoContent(
            url = "https://example.com/video.mp4",
            duration = Duration.parse("PT10M"),
            resolution = "1920x1080",
            format = "mp4",
            fileSize = 50000000L,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
    }

    private fun createMockAudioContent(): AudioContent {
        return AudioContent(
            url = "https://example.com/audio.mp3",
            duration = Duration.parse("PT5M"),
            format = "mp3",
            sampleRate = 44100,
            channels = 2,
            fileSize = 5000000L
        )
    }

    private fun createMockImageContent(): ImageContent {
        return ImageContent(
            url = "https://example.com/image.jpg",
            width = 1920,
            height = 1080,
            format = "jpg",
            fileSize = 500000L,
            altText = "示例图像"
        )
    }

    private fun createMockInteractiveContent(): InteractiveContent {
        return InteractiveContent(
            type = InteractiveType.QUIZ,
            elements = listOf(
                InteractiveElement(
                    id = "element_001",
                    type = ElementType.BUTTON,
                    properties = mapOf("text" to "提交答案", "color" to "blue"),
                    interactions = listOf(
                        InteractionDefinition(
                            trigger = "click",
                            action = "submit",
                            feedback = "答案已提交"
                        )
                    )
                )
            ),
            configuration = InteractiveConfiguration(
                maxAttempts = 3,
                timeLimit = Duration.parse("PT30M"),
                scoringMethod = ScoringMethod.POINTS,
                feedbackMode = FeedbackMode.IMMEDIATE
            )
        )
    }

    private fun createMockContentMetadata(): ContentMetadata {
        return ContentMetadata(
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            author = "测试作者",
            subject = "计算机科学",
            difficulty = DifficultyLevel.INTERMEDIATE,
            tags = listOf("机器学习", "人工智能", "教育技术")
        )
    }

    private fun createMockVideoBasicInfo(): VideoBasicInfo {
        return VideoBasicInfo(
            duration = Duration.parse("PT10M"),
            resolution = "1920x1080",
            format = "mp4",
            fileSize = 50000000L
        )
    }

    private fun createMockVideoContentAnalysis(): VideoContentAnalysis {
        return VideoContentAnalysis(
            identifiedTopics = listOf("机器学习", "深度学习", "神经网络"),
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            educationalValue = 8.5,
            confidence = 0.9
        )
    }

    private fun createMockVideoScenes(): List<VideoScene> {
        return listOf(
            VideoScene(
                startTime = Duration.parse("PT0S"),
                endTime = Duration.parse("PT2M"),
                description = "介绍机器学习基础概念",
                keyObjects = listOf("图表", "文字", "演讲者")
            ),
            VideoScene(
                startTime = Duration.parse("PT2M"),
                endTime = Duration.parse("PT5M"),
                description = "深度学习算法讲解",
                keyObjects = listOf("神经网络图", "代码示例")
            )
        )
    }

    private fun createMockAudioBasicInfo(): AudioBasicInfo {
        return AudioBasicInfo(
            duration = Duration.parse("PT5M"),
            sampleRate = 44100,
            channels = 2,
            format = "mp3"
        )
    }

    private fun createMockSpeechRecognition(): SpeechRecognitionResult {
        return SpeechRecognitionResult(
            transcript = "今天我们来学习机器学习的基础概念，包括监督学习和无监督学习的区别。",
            language = "zh-CN",
            confidence = 0.95,
            speakerCount = 1
        )
    }

    private fun createMockAudioQuality(): AudioQualityAnalysis {
        return AudioQualityAnalysis(
            clarity = 0.9,
            noiseLevel = 0.1,
            volumeConsistency = 0.85
        )
    }

    private fun createMockEmotionAnalysis(): EmotionAnalysis {
        return EmotionAnalysis(
            primaryEmotion = "积极",
            intensity = 0.7,
            stability = 0.8,
            confidence = 0.85
        )
    }

    private fun createMockImageBasicInfo(): ImageBasicInfo {
        return ImageBasicInfo(
            width = 1920,
            height = 1080,
            format = "jpg",
            fileSize = 500000L
        )
    }

    private fun createMockRecognizedObjects(): List<RecognizedObject> {
        return listOf(
            RecognizedObject(
                name = "计算机",
                confidence = 0.95,
                boundingBox = BoundingBox(100, 100, 200, 150)
            ),
            RecognizedObject(
                name = "书籍",
                confidence = 0.88,
                boundingBox = BoundingBox(300, 200, 150, 200)
            )
        )
    }

    private fun createMockTextRecognition(): TextRecognitionResult {
        return TextRecognitionResult(
            text = "机器学习算法原理与应用",
            language = "zh-CN",
            confidence = 0.92,
            regions = listOf(
                TextRegion(
                    text = "机器学习算法原理与应用",
                    boundingBox = BoundingBox(50, 50, 400, 30),
                    confidence = 0.92
                )
            )
        )
    }

    private fun createMockSceneAnalysis(): SceneAnalysis {
        return SceneAnalysis(
            sceneType = "教室",
            context = "学习环境",
            educationalRelevance = 0.9,
            confidence = 0.85
        )
    }

    private fun createMockElementAnalysis(): InteractiveElementAnalysis {
        return InteractiveElementAnalysis(
            elementCount = 5,
            interactionTypes = listOf("点击", "拖拽", "输入"),
            complexityLevel = 0.7
        )
    }

    private fun createMockUXAnalysis(): UXAnalysis {
        return UXAnalysis(
            usabilityScore = 0.85,
            accessibilityScore = 0.8,
            engagementPotential = 0.9,
            confidence = 0.88
        )
    }

    private fun createMockLearningEffectiveness(): LearningEffectivenessAnalysis {
        return LearningEffectivenessAnalysis(
            objectivesAlignment = 0.9,
            cognitiveLoad = 0.6,
            feedbackQuality = 0.85,
            confidence = 0.87
        )
    }

    private fun createMockRealTimePatterns(): List<RealTimePattern> {
        return listOf(
            RealTimePattern(
                patternType = "session_engagement",
                strength = 0.8,
                deviation = 0.1,
                significance = 0.7
            ),
            RealTimePattern(
                patternType = "content_interaction",
                strength = 0.75,
                deviation = 0.15,
                significance = 0.6
            )
        )
    }

    private fun createMockMonitoringSessionData(): MonitoringSessionData {
        return MonitoringSessionData(
            isActive = true,
            metricsCollected = 100,
            alertsTriggered = 2
        )
    }

    private fun createMockImmediateRisks(): List<ImmediateRisk> {
        return listOf(
            ImmediateRisk(
                riskType = "disengagement",
                severity = RiskSeverity.MEDIUM,
                probability = 0.7,
                description = "学习参与度下降",
                recommendedAction = "提供即时反馈和激励"
            )
        )
    }

    private fun createMockRealTimeInterventions(): List<RealTimeIntervention> {
        return listOf(
            RealTimeIntervention(
                interventionType = "engagement_boost",
                message = "检测到学习参与度下降，建议休息片刻或调整学习方式",
                action = "show_encouragement",
                priority = Priority.MEDIUM
            )
        )
    }
}
