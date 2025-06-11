package ai.kastrax.edutech.grading

import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.edutech.models.*
// import ai.kastrax.edutech.recommendation.PersonalizedRecommendationEngine
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 高级反馈生成引擎
 * 提供详细的错误分析、改进建议生成、学习资源推荐和个性化指导
 */
class FeedbackGenerationEngine(
    private val llmProvider: LlmProvider
) {
    
    /**
     * 生成详细反馈
     */
    suspend fun generateDetailedFeedback(
        submission: AssignmentSubmission,
        gradingResult: GradingResult,
        studentProfile: StudentProfile? = null
    ): EnhancedFeedback {
        
        // 错误分析
        val errorAnalysis = analyzeErrors(submission, gradingResult)
        
        // 改进建议生成
        val improvementPlan = generateImprovementPlan(submission, gradingResult, errorAnalysis)
        
        // 学习资源推荐
        val resourceRecommendations = recommendLearningResources(
            submission, gradingResult, studentProfile
        )
        
        // 个性化指导
        val personalizedGuidance = generatePersonalizedGuidance(
            submission, gradingResult, studentProfile, errorAnalysis
        )
        
        // 下一步行动计划
        val actionPlan = generateActionPlan(improvementPlan, resourceRecommendations)
        
        return EnhancedFeedback(
            originalFeedback = gradingResult.feedback,
            errorAnalysis = errorAnalysis,
            improvementPlan = improvementPlan,
            resourceRecommendations = resourceRecommendations,
            personalizedGuidance = personalizedGuidance,
            actionPlan = actionPlan,
            generatedAt = Clock.System.now()
        )
    }
    
    /**
     * 详细错误分析
     */
    private suspend fun analyzeErrors(
        submission: AssignmentSubmission,
        gradingResult: GradingResult
    ): ErrorAnalysis {
        val prompt = buildErrorAnalysisPrompt(submission, gradingResult)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 1500,
            temperature = 0.3
        )

        val response = llmProvider.generate(messages, options)
        return parseErrorAnalysisResponse(response.content, submission.type)
    }
    
    /**
     * 生成改进计划
     */
    private suspend fun generateImprovementPlan(
        submission: AssignmentSubmission,
        gradingResult: GradingResult,
        errorAnalysis: ErrorAnalysis
    ): ImprovementPlan {
        val prompt = buildImprovementPlanPrompt(submission, gradingResult, errorAnalysis)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 2000,
            temperature = 0.4
        )

        val response = llmProvider.generate(messages, options)
        return parseImprovementPlanResponse(response.content)
    }
    
    /**
     * 推荐学习资源
     */
    private suspend fun recommendLearningResources(
        submission: AssignmentSubmission,
        gradingResult: GradingResult,
        studentProfile: StudentProfile?
    ): List<LearningResource> {
        val weakAreas = identifyWeakAreas(gradingResult)
        val resources = mutableListOf<LearningResource>()
        
        // 基于弱项推荐资源
        weakAreas.forEach { area ->
            val areaResources = when (submission.type.category) {
                AssignmentCategory.PROGRAMMING -> recommendProgrammingResources(area, submission)
                AssignmentCategory.MATHEMATICS -> recommendMathResources(area)
                AssignmentCategory.WRITING -> recommendWritingResources(area)
                AssignmentCategory.CREATIVE -> recommendCreativeResources(area)
            }
            resources.addAll(areaResources)
        }
        
        // 个性化推荐（简化实现）
        if (studentProfile != null) {
            // 基于学生档案生成推荐资源
            val personalizedResources = generatePersonalizedResources(studentProfile, weakAreas)
            resources.addAll(personalizedResources)
        }
        
        return resources.distinctBy { it.url }.take(10) // 去重并限制数量
    }
    
    /**
     * 生成个性化指导
     */
    private suspend fun generatePersonalizedGuidance(
        submission: AssignmentSubmission,
        gradingResult: GradingResult,
        studentProfile: StudentProfile?,
        errorAnalysis: ErrorAnalysis
    ): PersonalizedGuidance {
        val prompt = buildPersonalizedGuidancePrompt(
            submission, gradingResult, studentProfile, errorAnalysis
        )
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 1800,
            temperature = 0.5
        )

        val response = llmProvider.generate(messages, options)
        return parsePersonalizedGuidanceResponse(response.content, studentProfile)
    }

    /**
     * 生成个性化资源推荐
     */
    private fun generatePersonalizedResources(
        studentProfile: StudentProfile,
        weakAreas: List<String>
    ): List<LearningResource> {
        val resources = mutableListOf<LearningResource>()

        // 基于学习风格推荐资源
        when (studentProfile.learningStyle) {
            LearningStyle.VISUAL -> {
                resources.add(
                    LearningResource(
                        title = "可视化学习资源",
                        type = ResourceType.VIDEO,
                        url = "https://example.com/visual-learning",
                        description = "适合视觉学习者的图表和视频资源",
                        relevance = 0.9
                    )
                )
            }
            LearningStyle.AUDITORY -> {
                resources.add(
                    LearningResource(
                        title = "音频学习资源",
                        type = ResourceType.TUTORIAL,
                        url = "https://example.com/audio-learning",
                        description = "适合听觉学习者的音频讲解",
                        relevance = 0.9
                    )
                )
            }
            LearningStyle.KINESTHETIC -> {
                resources.add(
                    LearningResource(
                        title = "实践练习资源",
                        type = ResourceType.PRACTICE,
                        url = "https://example.com/hands-on-practice",
                        description = "适合动觉学习者的实践练习",
                        relevance = 0.9
                    )
                )
            }
            else -> {
                resources.add(
                    LearningResource(
                        title = "综合学习资源",
                        type = ResourceType.TUTORIAL,
                        url = "https://example.com/comprehensive-learning",
                        description = "综合性学习资源",
                        relevance = 0.8
                    )
                )
            }
        }

        // 基于弱项推荐针对性资源
        weakAreas.forEach { area ->
            resources.add(
                LearningResource(
                    title = "${area}专项提升",
                    type = ResourceType.PRACTICE,
                    url = "https://example.com/improvement-${area}",
                    description = "针对${area}的专项提升资源",
                    relevance = 0.85
                )
            )
        }

        return resources.take(5) // 限制推荐数量
    }
    
    /**
     * 生成行动计划
     */
    private fun generateActionPlan(
        improvementPlan: ImprovementPlan,
        resources: List<LearningResource>
    ): ActionPlan {
        val steps = mutableListOf<ActionStep>()
        
        // 基于改进计划生成步骤
        improvementPlan.shortTermGoals.forEachIndexed { index, goal ->
            steps.add(
                ActionStep(
                    stepNumber = index + 1,
                    title = goal.title,
                    description = goal.description,
                    estimatedTime = goal.estimatedTime,
                    resources = resources.filter { it.relevance > 0.7 }.take(3),
                    priority = goal.priority
                )
            )
        }
        
        return ActionPlan(
            steps = steps,
            estimatedTotalTime = steps.sumOf { it.estimatedTime.inWholeMinutes }.toInt(),
            difficulty = calculatePlanDifficulty(steps)
        )
    }
    
    // 辅助方法
    private fun buildErrorAnalysisPrompt(
        submission: AssignmentSubmission,
        gradingResult: GradingResult
    ): String {
        return """
        请对以下作业提交进行详细的错误分析：
        
        作业类型：${submission.type.displayName}
        评分结果：${gradingResult.overallScore}/100
        
        作业内容：
        ${getContentSummary(submission.content)}
        
        评分反馈：
        ${gradingResult.feedback.summary}
        
        弱项：${gradingResult.feedback.weaknesses.joinToString(", ")}
        
        请提供：
        1. 具体错误类型分类
        2. 错误根本原因分析
        3. 错误严重程度评估
        4. 常见错误模式识别
        5. 预防措施建议
        
        请以JSON格式返回分析结果。
        """.trimIndent()
    }
    
    private fun buildImprovementPlanPrompt(
        submission: AssignmentSubmission,
        gradingResult: GradingResult,
        errorAnalysis: ErrorAnalysis
    ): String {
        return """
        基于错误分析结果，请制定详细的改进计划：
        
        当前水平：${gradingResult.overallScore}/100
        主要错误：${errorAnalysis.majorErrors.joinToString(", ") { it.type }}
        
        请提供：
        1. 短期改进目标（1-2周）
        2. 中期发展目标（1-2个月）
        3. 长期学习目标（3-6个月）
        4. 具体学习路径
        5. 里程碑设置
        
        请以JSON格式返回改进计划。
        """.trimIndent()
    }
    
    private fun buildPersonalizedGuidancePrompt(
        submission: AssignmentSubmission,
        gradingResult: GradingResult,
        studentProfile: StudentProfile?,
        errorAnalysis: ErrorAnalysis
    ): String {
        val profileInfo = studentProfile?.let {
            """
            学生档案：
            - 学习风格：${it.learningStyle}
            - 技能水平：${it.skillLevels}
            - 学习偏好：${it.preferences}
            """
        } ?: "无学生档案信息"
        
        return """
        请为学生提供个性化的学习指导：
        
        $profileInfo
        
        当前作业表现：${gradingResult.overallScore}/100
        主要问题：${errorAnalysis.majorErrors.joinToString(", ") { it.description }}
        
        请提供：
        1. 个性化学习建议
        2. 适合的学习方法
        3. 激励和鼓励
        4. 学习习惯建议
        5. 心理支持
        
        请以JSON格式返回指导内容。
        """.trimIndent()
    }
    
    private fun getContentSummary(content: AssignmentContent): String {
        return when (content) {
            is AssignmentContent.ProgrammingContent -> 
                "编程语言：${content.language.displayName}\n代码长度：${content.sourceCode.length}字符"
            is AssignmentContent.MathContent -> 
                "解答长度：${content.solution.length}字符\n步骤数：${content.workingSteps.size}"
            is AssignmentContent.WritingContent -> 
                "字数：${content.wordCount}\n参考文献：${content.references.size}个"
            is AssignmentContent.CreativeContent -> 
                "描述长度：${content.description.length}字符\n媒体文件：${content.mediaFiles.size}个"
        }
    }
    
    private fun identifyWeakAreas(gradingResult: GradingResult): List<String> {
        return gradingResult.rubricScores
            .filter { it.value < 15.0 } // 假设满分25分，低于15分为弱项
            .keys.toList()
    }
    
    private fun recommendProgrammingResources(area: String, submission: AssignmentSubmission): List<LearningResource> {
        val content = submission.content as AssignmentContent.ProgrammingContent
        val language = content.language.displayName
        
        return listOf(
            LearningResource(
                title = "${language} ${area} 教程",
                type = ResourceType.TUTORIAL,
                url = "https://example.com/${language}-${area}",
                description = "学习${language}中的${area}相关知识",
                relevance = 0.9
            ),
            LearningResource(
                title = "${language} 最佳实践",
                type = ResourceType.DOCUMENTATION,
                url = "https://example.com/${language}-best-practices",
                description = "${language}编程最佳实践指南",
                relevance = 0.8
            )
        )
    }
    
    private fun recommendMathResources(area: String): List<LearningResource> {
        return listOf(
            LearningResource(
                title = "${area} 基础教程",
                type = ResourceType.TUTORIAL,
                url = "https://example.com/math-${area}",
                description = "数学${area}的基础知识和解题方法",
                relevance = 0.9
            )
        )
    }

    private fun recommendWritingResources(area: String): List<LearningResource> {
        return listOf(
            LearningResource(
                title = "${area} 写作指南",
                type = ResourceType.TUTORIAL,
                url = "https://example.com/writing-${area}",
                description = "提升${area}写作技能的指南",
                relevance = 0.9
            )
        )
    }

    private fun recommendCreativeResources(area: String): List<LearningResource> {
        return listOf(
            LearningResource(
                title = "${area} 创意技法",
                type = ResourceType.TUTORIAL,
                url = "https://example.com/creative-${area}",
                description = "学习${area}相关的创意表现技法",
                relevance = 0.9
            )
        )
    }
    
    private fun calculatePlanDifficulty(steps: List<ActionStep>): DifficultyLevel {
        val avgPriority = steps.map { 
            when (it.priority) {
                Priority.HIGH -> 3
                Priority.MEDIUM -> 2
                Priority.LOW -> 1
            }
        }.average()
        
        return when {
            avgPriority >= 2.5 -> DifficultyLevel.ADVANCED
            avgPriority >= 1.5 -> DifficultyLevel.INTERMEDIATE
            else -> DifficultyLevel.BEGINNER
        }
    }
    
    // 解析方法（简化实现）
    private fun parseErrorAnalysisResponse(response: String, type: AssignmentType): ErrorAnalysis {
        // 实际应该解析JSON响应
        return ErrorAnalysis(
            majorErrors = listOf(
                ErrorDetail("逻辑错误", "算法逻辑不正确", Severity.MAJOR, "重新理解问题需求"),
                ErrorDetail("语法错误", "代码语法有误", Severity.MINOR, "检查语法规则")
            ),
            minorErrors = listOf(
                ErrorDetail("命名规范", "变量命名不规范", Severity.MINOR, "使用有意义的变量名")
            ),
            errorPatterns = listOf("常见的边界条件处理错误"),
            rootCauses = listOf("对问题理解不够深入", "基础语法掌握不牢固")
        )
    }
    
    private fun parseImprovementPlanResponse(response: String): ImprovementPlan {
        return ImprovementPlan(
            shortTermGoals = listOf(
                LearningGoal(
                    title = "修复语法错误",
                    description = "学习和练习基础语法",
                    estimatedTime = kotlin.time.Duration.parse("PT2H"),
                    priority = Priority.HIGH
                )
            ),
            mediumTermGoals = listOf(
                LearningGoal(
                    title = "提升算法思维",
                    description = "练习算法设计和分析",
                    estimatedTime = kotlin.time.Duration.parse("PT20H"),
                    priority = Priority.MEDIUM
                )
            ),
            longTermGoals = listOf(
                LearningGoal(
                    title = "掌握高级编程技巧",
                    description = "学习设计模式和最佳实践",
                    estimatedTime = kotlin.time.Duration.parse("PT100H"),
                    priority = Priority.MEDIUM
                )
            )
        )
    }
    
    private fun parsePersonalizedGuidanceResponse(
        response: String, 
        studentProfile: StudentProfile?
    ): PersonalizedGuidance {
        return PersonalizedGuidance(
            learningAdvice = "建议采用循序渐进的学习方法，先掌握基础概念再进行实践",
            studyMethods = listOf("代码阅读", "实践练习", "同伴讨论"),
            motivation = "你已经展现出良好的学习态度，继续保持！",
            habitSuggestions = listOf("每天练习30分钟", "定期复习已学内容"),
            psychologicalSupport = "遇到困难是正常的，重要的是坚持学习和不断改进"
        )
    }
}
