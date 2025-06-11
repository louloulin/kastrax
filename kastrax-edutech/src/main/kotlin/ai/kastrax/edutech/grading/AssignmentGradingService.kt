package ai.kastrax.edutech.grading

import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * 智能作业批改服务
 * 支持多种类型作业的自动批改和反馈生成
 */
class AssignmentGradingService(
    private val llmProvider: LlmProvider,
    private val codeExecutor: CodeExecutor,
    private val qualityAssurance: QualityAssuranceService
) {
    
    /**
     * 批改作业提交
     */
    suspend fun gradeAssignment(
        submission: AssignmentSubmission,
        request: GradingRequest
    ): AssignmentGradingResult {
        return try {
            var result: GradingResult? = null
            val gradingTime = measureTime {
                result = when (submission.type.category) {
                    AssignmentCategory.PROGRAMMING -> gradeProgrammingAssignment(submission, request)
                    AssignmentCategory.MATHEMATICS -> gradeMathAssignment(submission, request)
                    AssignmentCategory.WRITING -> gradeWritingAssignment(submission, request)
                    AssignmentCategory.CREATIVE -> gradeCreativeAssignment(submission, request)
                }
            }

            val finalResult = result!!

            // 质量评估
            val qualityAssessment = qualityAssurance.assessGradingQuality(finalResult, submission)

            // 如果质量不达标，标记需要人工审核
            if (qualityAssessment.overallQuality < 0.8) {
                AssignmentGradingResult.NeedsReview(
                    finalResult.copy(needsReview = true, gradingTime = gradingTime),
                    "质量评估不达标: ${qualityAssessment.overallQuality}"
                )
            } else {
                AssignmentGradingResult.Success(finalResult.copy(gradingTime = gradingTime))
            }
        } catch (e: Exception) {
            AssignmentGradingResult.Failure("批改失败: ${e.message}")
        }
    }
    
    /**
     * 批改编程作业
     */
    private suspend fun gradeProgrammingAssignment(
        submission: AssignmentSubmission,
        request: GradingRequest
    ): GradingResult {
        val content = submission.content as AssignmentContent.ProgrammingContent
        
        // 代码执行测试
        val executionResults = if (request.options.enableCodeExecution) {
            codeExecutor.executeCode(content.sourceCode, content.language, content.testCases)
        } else {
            emptyList()
        }
        
        // LLM代码分析
        val codeAnalysis = analyzeCodeWithLLM(content, request.rubric)
        
        // 计算分数
        val scores = calculateProgrammingScores(codeAnalysis, executionResults, request.rubric)
        
        // 生成反馈
        val feedback = generateProgrammingFeedback(content, codeAnalysis, executionResults, scores)
        
        return GradingResult(
            id = GradingResultId.generate(),
            submissionId = submission.id,
            overallScore = scores.values.sum(),
            passed = scores.values.sum() >= request.rubric.passingScore,
            feedback = feedback,
            rubricScores = scores,
            gradedAt = Clock.System.now(),
            gradingTime = Duration.ZERO, // 将在外层设置
            confidence = calculateConfidence(codeAnalysis, executionResults)
        )
    }
    
    /**
     * 批改数学作业
     */
    private suspend fun gradeMathAssignment(
        submission: AssignmentSubmission,
        request: GradingRequest
    ): GradingResult {
        val content = submission.content as AssignmentContent.MathContent
        
        // LLM数学分析
        val mathAnalysis = analyzeMathWithLLM(content, request.rubric)
        
        // 计算分数
        val scores = calculateMathScores(mathAnalysis, request.rubric)
        
        // 生成反馈
        val feedback = generateMathFeedback(content, mathAnalysis, scores)
        
        return GradingResult(
            id = GradingResultId.generate(),
            submissionId = submission.id,
            overallScore = scores.values.sum(),
            passed = scores.values.sum() >= request.rubric.passingScore,
            feedback = feedback,
            rubricScores = scores,
            gradedAt = Clock.System.now(),
            gradingTime = Duration.ZERO,
            confidence = mathAnalysis.confidence
        )
    }
    
    /**
     * 批改写作作业
     */
    private suspend fun gradeWritingAssignment(
        submission: AssignmentSubmission,
        request: GradingRequest
    ): GradingResult {
        val content = submission.content as AssignmentContent.WritingContent
        
        // LLM写作分析
        val writingAnalysis = analyzeWritingWithLLM(content, request.rubric)
        
        // 计算分数
        val scores = calculateWritingScores(writingAnalysis, request.rubric)
        
        // 生成反馈
        val feedback = generateWritingFeedback(content, writingAnalysis, scores)
        
        return GradingResult(
            id = GradingResultId.generate(),
            submissionId = submission.id,
            overallScore = scores.values.sum(),
            passed = scores.values.sum() >= request.rubric.passingScore,
            feedback = feedback,
            rubricScores = scores,
            gradedAt = Clock.System.now(),
            gradingTime = Duration.ZERO,
            confidence = writingAnalysis.confidence
        )
    }
    
    /**
     * 批改创意作业
     */
    private suspend fun gradeCreativeAssignment(
        submission: AssignmentSubmission,
        request: GradingRequest
    ): GradingResult {
        val content = submission.content as AssignmentContent.CreativeContent
        
        // LLM创意分析
        val creativeAnalysis = analyzeCreativeWithLLM(content, request.rubric)
        
        // 计算分数
        val scores = calculateCreativeScores(creativeAnalysis, request.rubric)
        
        // 生成反馈
        val feedback = generateCreativeFeedback(content, creativeAnalysis, scores)
        
        return GradingResult(
            id = GradingResultId.generate(),
            submissionId = submission.id,
            overallScore = scores.values.sum(),
            passed = scores.values.sum() >= request.rubric.passingScore,
            feedback = feedback,
            rubricScores = scores,
            gradedAt = Clock.System.now(),
            gradingTime = Duration.ZERO,
            confidence = creativeAnalysis.confidence
        )
    }
    
    /**
     * 使用LLM分析代码
     */
    private suspend fun analyzeCodeWithLLM(
        content: AssignmentContent.ProgrammingContent,
        rubric: GradingRubric
    ): CodeAnalysis {
        val prompt = buildCodeAnalysisPrompt(content, rubric)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 2000,
            temperature = 0.3 // 较低温度确保一致性
        )

        val response = llmProvider.generate(messages, options)
        return parseCodeAnalysisResponse(response.content)
    }
    
    /**
     * 使用LLM分析数学作业
     */
    private suspend fun analyzeMathWithLLM(
        content: AssignmentContent.MathContent,
        rubric: GradingRubric
    ): MathAnalysis {
        val prompt = buildMathAnalysisPrompt(content, rubric)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 1500,
            temperature = 0.2
        )

        val response = llmProvider.generate(messages, options)
        return parseMathAnalysisResponse(response.content)
    }
    
    /**
     * 使用LLM分析写作作业
     */
    private suspend fun analyzeWritingWithLLM(
        content: AssignmentContent.WritingContent,
        rubric: GradingRubric
    ): WritingAnalysis {
        val prompt = buildWritingAnalysisPrompt(content, rubric)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 2500,
            temperature = 0.4
        )

        val response = llmProvider.generate(messages, options)
        return parseWritingAnalysisResponse(response.content)
    }
    
    /**
     * 使用LLM分析创意作业
     */
    private suspend fun analyzeCreativeWithLLM(
        content: AssignmentContent.CreativeContent,
        rubric: GradingRubric
    ): CreativeAnalysis {
        val prompt = buildCreativeAnalysisPrompt(content, rubric)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )

        val options = LlmOptions(
            maxTokens = 2000,
            temperature = 0.6 // 创意作业允许更高的温度
        )

        val response = llmProvider.generate(messages, options)
        return parseCreativeAnalysisResponse(response.content)
    }
    
    // 辅助方法将在下一部分实现
    private fun buildCodeAnalysisPrompt(content: AssignmentContent.ProgrammingContent, rubric: GradingRubric): String {
        return """
        请分析以下${content.language.displayName}代码，并根据评分标准进行评估：
        
        代码：
        ```${content.language.extension}
        ${content.sourceCode}
        ```
        
        评分标准：
        ${rubric.criteria.joinToString("\n") { "- ${it.name}: ${it.description} (${it.maxPoints}分)" }}
        
        请提供：
        1. 代码质量评估
        2. 功能正确性分析
        3. 代码风格评价
        4. 性能考虑
        5. 具体改进建议
        
        请以JSON格式返回分析结果。
        """.trimIndent()
    }
    
    private fun buildMathAnalysisPrompt(content: AssignmentContent.MathContent, rubric: GradingRubric): String {
        return """
        请分析以下数学解答，并根据评分标准进行评估：
        
        解答：
        ${content.solution}
        
        解题步骤：
        ${content.workingSteps.joinToString("\n") { "- $it" }}
        
        最终答案：${content.finalAnswer}
        
        评分标准：
        ${rubric.criteria.joinToString("\n") { "- ${it.name}: ${it.description} (${it.maxPoints}分)" }}
        
        请提供：
        1. 解题方法正确性
        2. 计算准确性
        3. 步骤完整性
        4. 数学表达规范性
        5. 具体改进建议
        
        请以JSON格式返回分析结果。
        """.trimIndent()
    }
    
    private fun buildWritingAnalysisPrompt(content: AssignmentContent.WritingContent, rubric: GradingRubric): String {
        return """
        请分析以下写作作品，并根据评分标准进行评估：
        
        文本内容：
        ${content.text}
        
        字数：${content.wordCount}
        参考文献：${content.references.joinToString(", ")}
        
        评分标准：
        ${rubric.criteria.joinToString("\n") { "- ${it.name}: ${it.description} (${it.maxPoints}分)" }}
        
        请提供：
        1. 内容质量评估
        2. 结构组织分析
        3. 语言表达评价
        4. 论证逻辑性
        5. 具体改进建议
        
        请以JSON格式返回分析结果。
        """.trimIndent()
    }
    
    private fun buildCreativeAnalysisPrompt(content: AssignmentContent.CreativeContent, rubric: GradingRubric): String {
        return """
        请分析以下创意作品，并根据评分标准进行评估：
        
        作品描述：
        ${content.description}
        
        艺术陈述：
        ${content.artisticStatement}
        
        使用技法：${content.techniques.joinToString(", ")}
        
        评分标准：
        ${rubric.criteria.joinToString("\n") { "- ${it.name}: ${it.description} (${it.maxPoints}分)" }}
        
        请提供：
        1. 创意性评估
        2. 技术执行分析
        3. 艺术表现评价
        4. 概念深度
        5. 具体改进建议
        
        请以JSON格式返回分析结果。
        """.trimIndent()
    }

    // 解析分析结果的方法
    private fun parseCodeAnalysisResponse(response: String): CodeAnalysis {
        // 简化的解析实现，实际应该使用JSON解析
        return CodeAnalysis(
            codeQuality = 0.8,
            functionality = 0.9,
            style = 0.7,
            performance = 0.8,
            suggestions = listOf("改进变量命名", "添加注释", "优化算法复杂度"),
            confidence = 0.85
        )
    }

    private fun parseMathAnalysisResponse(response: String): MathAnalysis {
        return MathAnalysis(
            methodCorrectness = 0.9,
            calculationAccuracy = 0.85,
            stepCompleteness = 0.8,
            presentation = 0.75,
            suggestions = listOf("补充解题步骤", "检查计算错误"),
            confidence = 0.82
        )
    }

    private fun parseWritingAnalysisResponse(response: String): WritingAnalysis {
        return WritingAnalysis(
            contentQuality = 0.8,
            structure = 0.85,
            language = 0.75,
            logic = 0.8,
            suggestions = listOf("加强论证", "改进语言表达", "完善结构"),
            confidence = 0.78
        )
    }

    private fun parseCreativeAnalysisResponse(response: String): CreativeAnalysis {
        return CreativeAnalysis(
            creativity = 0.9,
            technique = 0.8,
            expression = 0.85,
            concept = 0.75,
            suggestions = listOf("深化概念表达", "尝试新技法"),
            confidence = 0.8
        )
    }

    // 评分计算方法
    private fun calculateProgrammingScores(
        analysis: CodeAnalysis,
        executionResults: List<ExecutionResult>,
        rubric: GradingRubric
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()

        rubric.criteria.forEach { criterion ->
            val score = when (criterion.name.lowercase()) {
                "代码质量", "code quality" -> analysis.codeQuality * criterion.maxPoints
                "功能正确性", "functionality" -> {
                    if (executionResults.isNotEmpty()) {
                        val passRate = executionResults.count { it.passed }.toDouble() / executionResults.size
                        passRate * criterion.maxPoints
                    } else {
                        analysis.functionality * criterion.maxPoints
                    }
                }
                "代码风格", "style" -> analysis.style * criterion.maxPoints
                "性能", "performance" -> analysis.performance * criterion.maxPoints
                else -> criterion.maxPoints * 0.8 // 默认分数
            }
            scores[criterion.name] = score
        }

        return scores
    }

    private fun calculateMathScores(
        analysis: MathAnalysis,
        rubric: GradingRubric
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()

        rubric.criteria.forEach { criterion ->
            val score = when (criterion.name.lowercase()) {
                "解题方法", "method" -> analysis.methodCorrectness * criterion.maxPoints
                "计算准确性", "accuracy" -> analysis.calculationAccuracy * criterion.maxPoints
                "步骤完整性", "completeness" -> analysis.stepCompleteness * criterion.maxPoints
                "表达规范", "presentation" -> analysis.presentation * criterion.maxPoints
                else -> criterion.maxPoints * 0.8
            }
            scores[criterion.name] = score
        }

        return scores
    }

    private fun calculateWritingScores(
        analysis: WritingAnalysis,
        rubric: GradingRubric
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()

        rubric.criteria.forEach { criterion ->
            val score = when (criterion.name.lowercase()) {
                "内容质量", "content" -> analysis.contentQuality * criterion.maxPoints
                "结构组织", "structure" -> analysis.structure * criterion.maxPoints
                "语言表达", "language" -> analysis.language * criterion.maxPoints
                "逻辑性", "logic" -> analysis.logic * criterion.maxPoints
                else -> criterion.maxPoints * 0.8
            }
            scores[criterion.name] = score
        }

        return scores
    }

    private fun calculateCreativeScores(
        analysis: CreativeAnalysis,
        rubric: GradingRubric
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()

        rubric.criteria.forEach { criterion ->
            val score = when (criterion.name.lowercase()) {
                "创意性", "creativity" -> analysis.creativity * criterion.maxPoints
                "技术执行", "technique" -> analysis.technique * criterion.maxPoints
                "艺术表现", "expression" -> analysis.expression * criterion.maxPoints
                "概念深度", "concept" -> analysis.concept * criterion.maxPoints
                else -> criterion.maxPoints * 0.8
            }
            scores[criterion.name] = score
        }

        return scores
    }

    // 置信度计算
    private fun calculateConfidence(analysis: CodeAnalysis, executionResults: List<ExecutionResult>): Double {
        val baseConfidence = analysis.confidence
        val executionConfidence = if (executionResults.isNotEmpty()) {
            val passRate = executionResults.count { it.passed }.toDouble() / executionResults.size
            if (passRate > 0.8) 0.9 else if (passRate > 0.5) 0.7 else 0.5
        } else {
            0.7
        }

        return (baseConfidence + executionConfidence) / 2
    }

    // 反馈生成方法
    private fun generateProgrammingFeedback(
        content: AssignmentContent.ProgrammingContent,
        analysis: CodeAnalysis,
        executionResults: List<ExecutionResult>,
        scores: Map<String, Double>
    ): DetailedFeedback {
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val improvements = mutableListOf<ImprovementSuggestion>()
        val resources = mutableListOf<LearningResource>()

        // 分析优势
        if (analysis.codeQuality > 0.8) strengths.add("代码质量良好，结构清晰")
        if (analysis.functionality > 0.8) strengths.add("功能实现正确")
        if (analysis.style > 0.8) strengths.add("代码风格规范")

        // 分析不足
        if (analysis.codeQuality < 0.7) weaknesses.add("代码质量需要改进")
        if (analysis.functionality < 0.7) weaknesses.add("功能实现存在问题")
        if (analysis.style < 0.7) weaknesses.add("代码风格不够规范")

        // 生成改进建议
        analysis.suggestions.forEach { suggestion ->
            improvements.add(
                ImprovementSuggestion(
                    category = "代码改进",
                    description = suggestion,
                    priority = Priority.MEDIUM,
                    actionItems = listOf("重构相关代码", "参考最佳实践")
                )
            )
        }

        // 推荐学习资源
        if (analysis.codeQuality < 0.7) {
            resources.add(
                LearningResource(
                    title = "${content.language.displayName}编程最佳实践",
                    type = ResourceType.TUTORIAL,
                    url = "https://example.com/best-practices",
                    description = "学习${content.language.displayName}编程的最佳实践",
                    relevance = 0.9
                )
            )
        }

        val summary = generateSummary(scores, strengths, weaknesses)

        return DetailedFeedback(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            improvements = improvements,
            resources = resources
        )
    }

    private fun generateMathFeedback(
        content: AssignmentContent.MathContent,
        analysis: MathAnalysis,
        scores: Map<String, Double>
    ): DetailedFeedback {
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val improvements = mutableListOf<ImprovementSuggestion>()
        val resources = mutableListOf<LearningResource>()

        // 分析优势
        if (analysis.methodCorrectness > 0.8) strengths.add("解题方法正确")
        if (analysis.calculationAccuracy > 0.8) strengths.add("计算准确")
        if (analysis.stepCompleteness > 0.8) strengths.add("解题步骤完整")

        // 分析不足
        if (analysis.methodCorrectness < 0.7) weaknesses.add("解题方法需要改进")
        if (analysis.calculationAccuracy < 0.7) weaknesses.add("计算存在错误")
        if (analysis.stepCompleteness < 0.7) weaknesses.add("解题步骤不够完整")

        // 生成改进建议
        analysis.suggestions.forEach { suggestion ->
            improvements.add(
                ImprovementSuggestion(
                    category = "数学解题",
                    description = suggestion,
                    priority = Priority.HIGH,
                    actionItems = listOf("复习相关概念", "练习类似题目")
                )
            )
        }

        val summary = generateSummary(scores, strengths, weaknesses)

        return DetailedFeedback(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            improvements = improvements,
            resources = resources
        )
    }

    private fun generateWritingFeedback(
        content: AssignmentContent.WritingContent,
        analysis: WritingAnalysis,
        scores: Map<String, Double>
    ): DetailedFeedback {
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val improvements = mutableListOf<ImprovementSuggestion>()
        val resources = mutableListOf<LearningResource>()

        // 分析优势
        if (analysis.contentQuality > 0.8) strengths.add("内容质量高")
        if (analysis.structure > 0.8) strengths.add("结构组织良好")
        if (analysis.language > 0.8) strengths.add("语言表达流畅")

        // 分析不足
        if (analysis.contentQuality < 0.7) weaknesses.add("内容质量需要提升")
        if (analysis.structure < 0.7) weaknesses.add("结构组织需要改进")
        if (analysis.language < 0.7) weaknesses.add("语言表达需要加强")

        // 生成改进建议
        analysis.suggestions.forEach { suggestion ->
            improvements.add(
                ImprovementSuggestion(
                    category = "写作技巧",
                    description = suggestion,
                    priority = Priority.MEDIUM,
                    actionItems = listOf("阅读优秀范文", "练习写作技巧")
                )
            )
        }

        val summary = generateSummary(scores, strengths, weaknesses)

        return DetailedFeedback(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            improvements = improvements,
            resources = resources
        )
    }

    private fun generateCreativeFeedback(
        content: AssignmentContent.CreativeContent,
        analysis: CreativeAnalysis,
        scores: Map<String, Double>
    ): DetailedFeedback {
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val improvements = mutableListOf<ImprovementSuggestion>()
        val resources = mutableListOf<LearningResource>()

        // 分析优势
        if (analysis.creativity > 0.8) strengths.add("创意性突出")
        if (analysis.technique > 0.8) strengths.add("技术执行良好")
        if (analysis.expression > 0.8) strengths.add("艺术表现力强")

        // 分析不足
        if (analysis.creativity < 0.7) weaknesses.add("创意性需要加强")
        if (analysis.technique < 0.7) weaknesses.add("技术执行需要改进")
        if (analysis.expression < 0.7) weaknesses.add("艺术表现需要提升")

        // 生成改进建议
        analysis.suggestions.forEach { suggestion ->
            improvements.add(
                ImprovementSuggestion(
                    category = "创意表达",
                    description = suggestion,
                    priority = Priority.MEDIUM,
                    actionItems = listOf("探索新的创意方向", "学习新的表现技法")
                )
            )
        }

        val summary = generateSummary(scores, strengths, weaknesses)

        return DetailedFeedback(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            improvements = improvements,
            resources = resources
        )
    }

    private fun generateSummary(
        scores: Map<String, Double>,
        strengths: List<String>,
        weaknesses: List<String>
    ): String {
        val totalScore = scores.values.sum()
        val maxScore = scores.size * 25.0 // 假设每项25分
        val percentage = (totalScore / maxScore * 100).toInt()

        return buildString {
            append("总分: ${totalScore.toInt()}/${maxScore.toInt()} ($percentage%)\n")
            if (strengths.isNotEmpty()) {
                append("优势: ${strengths.joinToString(", ")}\n")
            }
            if (weaknesses.isNotEmpty()) {
                append("需要改进: ${weaknesses.joinToString(", ")}")
            }
        }
    }
}
