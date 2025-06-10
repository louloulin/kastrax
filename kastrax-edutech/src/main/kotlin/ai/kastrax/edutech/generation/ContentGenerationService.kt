package ai.kastrax.edutech.generation

import ai.kastrax.edutech.models.*
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.*

/**
 * 智能内容生成服务
 * 
 * 实现ed2.md第二阶段Week 7-8智能内容生成系统
 * 支持LLM集成优化、内容生成模板、多模态内容支持和内容质量评估
 */
class ContentGenerationService(
    private val llmProvider: LlmProvider,
    private val templateRepository: ContentTemplateRepository,
    private val qualityAssessment: ContentQualityAssessment
) {
    private val generationHistory = mutableMapOf<String, List<GeneratedContent>>()
    private val mutex = Mutex()
    
    /**
     * 生成学习内容
     *
     * @param request 内容生成请求
     * @return 生成结果
     */
    suspend fun generateContent(request: ContentGenerationRequest): ContentGenerationResult {
        return try {
            // 选择合适的模板
            val template = selectTemplate(request)
            
            // 构建LLM提示
            val prompt = buildPrompt(template, request)
            
            // 调用LLM生成内容
            val llmResponse = callLLM(prompt, request.parameters)
            
            // 解析生成的内容
            val generatedContent = parseGeneratedContent(llmResponse, request)
            
            // 质量评估
            val qualityScore = qualityAssessment.assessContent(generatedContent)
            
            // 如果质量不达标，重新生成（但避免无限递归）
            val finalContent = if (qualityScore.overallScore < 0.7 && !request.parameters.containsKey("improvementFeedback")) {
                try {
                    regenerateWithImprovedPrompt(request, qualityScore.feedback)
                } catch (e: Exception) {
                    // 如果重新生成失败，使用原始内容
                    generatedContent
                }
            } else {
                generatedContent
            }
            
            // 记录生成历史
            recordGenerationHistory(request.requestId, finalContent)
            
            ContentGenerationResult.Success(
                content = finalContent,
                qualityScore = qualityScore,
                generationTime = finalContent.generatedAt
            )
        } catch (e: Exception) {
            ContentGenerationResult.Failure("内容生成失败: ${e.message}")
        }
    }
    
    /**
     * 批量生成内容
     *
     * @param requests 批量生成请求
     * @return 批量生成结果
     */
    suspend fun batchGenerateContent(
        requests: List<ContentGenerationRequest>
    ): BatchGenerationResult {
        return try {
            val results = mutableListOf<ContentGenerationResult>()
            var successCount = 0
            var failureCount = 0
            
            requests.forEach { request ->
                val result = generateContent(request)
                results.add(result)
                
                when (result) {
                    is ContentGenerationResult.Success -> successCount++
                    is ContentGenerationResult.Failure -> failureCount++
                }
            }
            
            BatchGenerationResult.Success(
                results = results,
                successCount = successCount,
                failureCount = failureCount,
                totalCount = requests.size
            )
        } catch (e: Exception) {
            BatchGenerationResult.Failure("批量生成失败: ${e.message}")
        }
    }
    
    /**
     * 生成多模态内容
     *
     * @param request 多模态内容生成请求
     * @return 生成结果
     */
    suspend fun generateMultimodalContent(
        request: MultimodalGenerationRequest
    ): MultimodalGenerationResult {
        return try {
            val components = mutableMapOf<ContentModality, GeneratedContent>()
            
            // 生成文本内容
            if (ContentModality.TEXT in request.modalities) {
                val textRequest = ContentGenerationRequest(
                    requestId = "${request.requestId}_text",
                    contentType = request.contentType,
                    subject = request.subject,
                    difficulty = request.difficulty,
                    topic = request.topic,
                    learningObjectives = request.learningObjectives,
                    targetAudience = request.targetAudience,
                    parameters = request.parameters
                )
                
                val textResult = generateContent(textRequest)
                if (textResult is ContentGenerationResult.Success) {
                    components[ContentModality.TEXT] = textResult.content
                }
            }
            
            // 生成图像描述（用于后续图像生成）
            if (ContentModality.IMAGE in request.modalities) {
                val imageDescriptionResult = generateImageDescription(request)
                if (imageDescriptionResult != null) {
                    components[ContentModality.IMAGE] = imageDescriptionResult
                }
            }
            
            // 生成视频脚本
            if (ContentModality.VIDEO in request.modalities) {
                val videoScriptResult = generateVideoScript(request)
                if (videoScriptResult != null) {
                    components[ContentModality.VIDEO] = videoScriptResult
                }
            }
            
            // 生成交互式内容
            if (ContentModality.INTERACTIVE in request.modalities) {
                val interactiveResult = generateInteractiveContent(request)
                if (interactiveResult != null) {
                    components[ContentModality.INTERACTIVE] = interactiveResult
                }
            }
            
            MultimodalGenerationResult.Success(
                components = components,
                generatedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            MultimodalGenerationResult.Failure("多模态内容生成失败: ${e.message}")
        }
    }
    
    /**
     * 获取生成历史
     *
     * @param requestId 请求ID
     * @return 生成历史
     */
    suspend fun getGenerationHistory(requestId: String): List<GeneratedContent> {
        return mutex.withLock {
            generationHistory[requestId] ?: emptyList()
        }
    }
    
    /**
     * 优化内容生成参数
     *
     * @param feedback 用户反馈
     * @return 优化建议
     */
    suspend fun optimizeGenerationParameters(
        feedback: List<GenerationFeedback>
    ): ParameterOptimizationResult {
        return try {
            val analysis = analyzeFeedback(feedback)
            val optimizedParameters = generateOptimizedParameters(analysis)
            
            ParameterOptimizationResult.Success(
                optimizedParameters = optimizedParameters,
                improvementAreas = analysis.improvementAreas,
                confidenceScore = analysis.confidenceScore
            )
        } catch (e: Exception) {
            ParameterOptimizationResult.Failure("参数优化失败: ${e.message}")
        }
    }
    
    // 私有辅助方法
    
    private suspend fun selectTemplate(request: ContentGenerationRequest): ContentTemplate {
        return templateRepository.findBestTemplate(
            contentType = request.contentType,
            subject = request.subject,
            difficulty = request.difficulty
        ) ?: ContentTemplate.getDefault(request.contentType)
    }
    
    private fun buildPrompt(template: ContentTemplate, request: ContentGenerationRequest): String {
        return template.buildPrompt(
            topic = request.topic.value,
            objectives = request.learningObjectives,
            difficulty = request.difficulty.displayName,
            audience = request.targetAudience,
            parameters = request.parameters
        )
    }
    
    private suspend fun callLLM(prompt: String, parameters: Map<String, String>): LlmResponse {
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = parameters["maxTokens"]?.toIntOrNull() ?: 2000,
            temperature = parameters["temperature"]?.toDoubleOrNull() ?: 0.7,
            topP = parameters["topP"]?.toDoubleOrNull() ?: 0.9
        )

        return llmProvider.generate(messages, options)
    }
    
    private fun parseGeneratedContent(
        llmResponse: LlmResponse,
        request: ContentGenerationRequest
    ): GeneratedContent {
        return GeneratedContent(
            id = GeneratedContentId.generate(),
            requestId = request.requestId,
            contentType = request.contentType,
            subject = request.subject,
            difficulty = request.difficulty,
            topic = request.topic,
            title = extractTitle(llmResponse.content),
            content = extractMainContent(llmResponse.content),
            learningObjectives = request.learningObjectives,
            estimatedDuration = estimateDuration(llmResponse.content),
            generatedAt = Clock.System.now(),
            metadata = mapOf(
                "llmModel" to "test-model",
                "tokenCount" to (llmResponse.usage?.totalTokens?.toString() ?: "0"),
                "finishReason" to (llmResponse.finishReason ?: "completed")
            )
        )
    }
    
    private suspend fun regenerateWithImprovedPrompt(
        request: ContentGenerationRequest,
        feedback: String
    ): GeneratedContent {
        val improvedRequest = request.copy(
            parameters = request.parameters + mapOf(
                "improvementFeedback" to feedback,
                "temperature" to "0.5" // 降低随机性以提高质量
            )
        )
        
        val result = generateContent(improvedRequest)
        return when (result) {
            is ContentGenerationResult.Success -> result.content
            is ContentGenerationResult.Failure -> throw Exception("重新生成失败: ${result.error}")
        }
    }
    
    private suspend fun recordGenerationHistory(requestId: String, content: GeneratedContent) {
        mutex.withLock {
            val history = generationHistory.getOrPut(requestId) { mutableListOf() }.toMutableList()
            history.add(content)
            generationHistory[requestId] = history
        }
    }
    
    private suspend fun generateImageDescription(request: MultimodalGenerationRequest): GeneratedContent? {
        val prompt = """
            为以下学习内容生成详细的图像描述，用于后续图像生成：
            主题: ${request.topic.value}
            学科: ${request.subject.displayName}
            难度: ${request.difficulty.displayName}
            学习目标: ${request.learningObjectives.joinToString(", ")}
            
            请生成3-5个具体的图像描述，每个描述应该：
            1. 与学习内容高度相关
            2. 有助于理解概念
            3. 适合目标受众
            4. 具有教育价值
        """.trimIndent()
        
        return try {
            val llmResponse = callLLM(prompt, request.parameters)
            GeneratedContent(
                id = GeneratedContentId.generate(),
                requestId = "${request.requestId}_image_desc",
                contentType = ContentType.IMAGE,
                subject = request.subject,
                difficulty = request.difficulty,
                topic = request.topic,
                title = "图像描述 - ${request.topic.value}",
                content = llmResponse.content,
                learningObjectives = request.learningObjectives,
                estimatedDuration = 0,
                generatedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun generateVideoScript(request: MultimodalGenerationRequest): GeneratedContent? {
        val prompt = """
            为以下学习内容生成视频脚本：
            主题: ${request.topic.value}
            学科: ${request.subject.displayName}
            难度: ${request.difficulty.displayName}
            学习目标: ${request.learningObjectives.joinToString(", ")}
            
            视频脚本应包括：
            1. 开场白 (30秒)
            2. 主要内容讲解 (5-8分钟)
            3. 实例演示 (2-3分钟)
            4. 总结和要点回顾 (1分钟)
            5. 互动问题 (30秒)
            
            请使用清晰、易懂的语言，适合目标受众。
        """.trimIndent()
        
        return try {
            val llmResponse = callLLM(prompt, request.parameters)
            GeneratedContent(
                id = GeneratedContentId.generate(),
                requestId = "${request.requestId}_video_script",
                contentType = ContentType.VIDEO,
                subject = request.subject,
                difficulty = request.difficulty,
                topic = request.topic,
                title = "视频脚本 - ${request.topic.value}",
                content = llmResponse.content,
                learningObjectives = request.learningObjectives,
                estimatedDuration = 600, // 10分钟
                generatedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun generateInteractiveContent(request: MultimodalGenerationRequest): GeneratedContent? {
        val prompt = """
            为以下学习内容生成交互式学习活动：
            主题: ${request.topic.value}
            学科: ${request.subject.displayName}
            难度: ${request.difficulty.displayName}
            学习目标: ${request.learningObjectives.joinToString(", ")}
            
            交互式内容应包括：
            1. 知识点检查问题 (3-5个选择题)
            2. 实践练习活动 (1-2个)
            3. 思考讨论问题 (2-3个)
            4. 自我评估清单
            
            请确保活动具有教育价值且适合目标受众。
        """.trimIndent()
        
        return try {
            val llmResponse = callLLM(prompt, request.parameters)
            GeneratedContent(
                id = GeneratedContentId.generate(),
                requestId = "${request.requestId}_interactive",
                contentType = ContentType.INTERACTIVE,
                subject = request.subject,
                difficulty = request.difficulty,
                topic = request.topic,
                title = "交互式活动 - ${request.topic.value}",
                content = llmResponse.content,
                learningObjectives = request.learningObjectives,
                estimatedDuration = 300, // 5分钟
                generatedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun analyzeFeedback(feedback: List<GenerationFeedback>): FeedbackAnalysis {
        val positiveAspects = mutableListOf<String>()
        val improvementAreas = mutableListOf<String>()
        val averageRating = feedback.map { it.rating }.average()
        
        feedback.forEach { fb ->
            if (fb.rating >= 4) {
                positiveAspects.addAll(fb.positiveAspects)
            } else {
                improvementAreas.addAll(fb.improvementSuggestions)
            }
        }
        
        return FeedbackAnalysis(
            averageRating = averageRating,
            positiveAspects = positiveAspects.distinct(),
            improvementAreas = improvementAreas.distinct(),
            confidenceScore = if (feedback.size >= 10) 0.9 else feedback.size * 0.09
        )
    }
    
    private fun generateOptimizedParameters(analysis: FeedbackAnalysis): Map<String, String> {
        val optimized = mutableMapOf<String, String>()
        
        // 根据反馈调整参数
        if (analysis.averageRating < 3.0) {
            optimized["temperature"] = "0.5" // 降低随机性
            optimized["maxTokens"] = "2500" // 增加内容长度
        } else if (analysis.averageRating > 4.0) {
            optimized["temperature"] = "0.8" // 增加创造性
        }

        // 根据改进建议调整
        if ("内容太简单" in analysis.improvementAreas) {
            optimized["complexityBoost"] = "true"
        }
        if ("需要更多例子" in analysis.improvementAreas) {
            optimized["includeMoreExamples"] = "true"
        }
        
        return optimized
    }
    
    // 简化的辅助方法
    private fun extractTitle(content: String): String {
        val firstLine = content.lines().firstOrNull { it.trim().isNotEmpty() } ?: "生成的内容"
        // 移除Markdown标题符号
        return firstLine.trim().removePrefix("#").trim().take(100)
    }
    
    private fun extractMainContent(content: String): String {
        return content.trim()
    }
    
    private fun estimateDuration(content: String): Int {
        // 简单估算：每200字约1分钟阅读时间
        return (content.length / 200).coerceAtLeast(1)
    }
}
