package ai.kastrax.edutech.assessment

import ai.kastrax.edutech.models.*
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.*

/**
 * 基础评估服务
 * 
 * 实现ed2.md第二阶段Week 7-8基础评估功能
 * 支持选择题自动批改、简答题评分算法、评估结果分析和反馈生成机制
 */
class AssessmentService(
    private val llmProvider: LlmProvider,
    private val assessmentRepository: AssessmentRepository
) {
    private val gradingHistory = mutableMapOf<AssessmentId, List<GradingResult>>()
    private val mutex = Mutex()
    
    /**
     * 创建评估
     *
     * @param request 评估创建请求
     * @return 创建结果
     */
    suspend fun createAssessment(request: AssessmentCreationRequest): AssessmentCreationResult {
        return try {
            val assessment = Assessment(
                id = AssessmentId.generate(),
                title = request.title,
                description = request.description,
                subject = request.subject,
                difficulty = request.difficulty,
                questions = request.questions,
                timeLimit = request.timeLimit,
                passingScore = request.passingScore,
                createdBy = request.createdBy,
                createdAt = Clock.System.now()
            )
            
            assessmentRepository.saveAssessment(assessment)
            
            AssessmentCreationResult.Success(assessment)
        } catch (e: Exception) {
            AssessmentCreationResult.Failure("创建评估失败: ${e.message}")
        }
    }
    
    /**
     * 提交评估答案
     *
     * @param submission 评估提交
     * @return 提交结果
     */
    suspend fun submitAssessment(submission: AssessmentSubmission): AssessmentSubmissionResult {
        return try {
            val assessment = assessmentRepository.getAssessment(submission.assessmentId)
                ?: return AssessmentSubmissionResult.Failure("评估不存在")
            
            // 自动批改
            val gradingResult = gradeSubmission(assessment, submission)
            
            // 保存提交记录
            assessmentRepository.saveSubmission(submission)
            assessmentRepository.saveGradingResult(gradingResult)
            
            // 记录批改历史
            recordGradingHistory(submission.assessmentId, gradingResult)
            
            AssessmentSubmissionResult.Success(
                submissionId = submission.id,
                gradingResult = gradingResult,
                submittedAt = submission.submittedAt
            )
        } catch (e: Exception) {
            AssessmentSubmissionResult.Failure("提交评估失败: ${e.message}")
        }
    }
    
    /**
     * 批改评估
     *
     * @param assessment 评估
     * @param submission 提交答案
     * @return 批改结果
     */
    suspend fun gradeSubmission(
        assessment: Assessment,
        submission: AssessmentSubmission
    ): GradingResult {
        val questionGrades = mutableListOf<QuestionGrade>()
        var totalScore = 0.0
        var maxScore = 0.0
        
        assessment.questions.forEachIndexed { index, question ->
            val answer = submission.answers.getOrNull(index)
            val grade = gradeQuestion(question, answer)
            questionGrades.add(grade)
            totalScore += grade.score
            maxScore += grade.maxScore
        }
        
        val percentage = if (maxScore > 0) (totalScore / maxScore) * 100 else 0.0
        val passed = percentage >= assessment.passingScore
        
        // 生成详细反馈
        val feedback = generateDetailedFeedback(assessment, questionGrades, percentage, passed)
        
        return GradingResult(
            id = GradingResultId.generate(),
            submissionId = submission.id,
            assessmentId = assessment.id,
            studentId = submission.studentId,
            questionGrades = questionGrades,
            totalScore = totalScore,
            maxScore = maxScore,
            percentage = percentage,
            passed = passed,
            feedback = feedback,
            gradedAt = Clock.System.now()
        )
    }
    
    /**
     * 批改单个问题
     *
     * @param question 问题
     * @param answer 答案
     * @return 问题评分
     */
    suspend fun gradeQuestion(question: Question, answer: Answer?): QuestionGrade {
        return when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> gradeMultipleChoice(question, answer)
            QuestionType.TRUE_FALSE -> gradeTrueFalse(question, answer)
            QuestionType.SHORT_ANSWER -> gradeShortAnswer(question, answer)
            QuestionType.ESSAY -> gradeEssay(question, answer)
            QuestionType.FILL_IN_BLANK -> gradeFillInBlank(question, answer)
        }
    }
    
    /**
     * 获取评估统计
     *
     * @param assessmentId 评估ID
     * @return 统计结果
     */
    suspend fun getAssessmentStatistics(assessmentId: AssessmentId): AssessmentStatisticsResult {
        return try {
            val submissions = assessmentRepository.getSubmissionsByAssessment(assessmentId)
            val gradingResults = submissions.mapNotNull { submission ->
                assessmentRepository.getGradingResult(submission.id)
            }
            
            if (gradingResults.isEmpty()) {
                return AssessmentStatisticsResult.Success(
                    AssessmentStatistics(
                        assessmentId = assessmentId,
                        totalSubmissions = 0,
                        averageScore = 0.0,
                        passRate = 0.0,
                        scoreDistribution = emptyMap(),
                        questionAnalysis = emptyList()
                    )
                )
            }
            
            val statistics = calculateStatistics(assessmentId, gradingResults)
            AssessmentStatisticsResult.Success(statistics)
        } catch (e: Exception) {
            AssessmentStatisticsResult.Failure("获取统计失败: ${e.message}")
        }
    }
    
    /**
     * 生成评估报告
     *
     * @param assessmentId 评估ID
     * @param studentId 学生ID (可选)
     * @return 报告结果
     */
    suspend fun generateAssessmentReport(
        assessmentId: AssessmentId,
        studentId: StudentId? = null
    ): AssessmentReportResult {
        return try {
            val assessment = assessmentRepository.getAssessment(assessmentId)
                ?: return AssessmentReportResult.Failure("评估不存在")
            
            val report = if (studentId != null) {
                generateStudentReport(assessment, studentId)
            } else {
                generateOverallReport(assessment)
            }
            
            AssessmentReportResult.Success(report)
        } catch (e: Exception) {
            AssessmentReportResult.Failure("生成报告失败: ${e.message}")
        }
    }
    
    // 私有方法实现
    
    private suspend fun gradeMultipleChoice(question: Question, answer: Answer?): QuestionGrade {
        val isCorrect = answer?.content == question.correctAnswer
        val score = if (isCorrect) question.points else 0.0
        
        return QuestionGrade(
            questionId = question.id,
            studentAnswer = answer?.content ?: "",
            correctAnswer = question.correctAnswer ?: "",
            score = score,
            maxScore = question.points,
            isCorrect = isCorrect,
            feedback = if (isCorrect) "正确！" else "答案错误，正确答案是：${question.correctAnswer}"
        )
    }
    
    private suspend fun gradeTrueFalse(question: Question, answer: Answer?): QuestionGrade {
        val isCorrect = answer?.content?.lowercase() == question.correctAnswer?.lowercase()
        val score = if (isCorrect) question.points else 0.0
        
        return QuestionGrade(
            questionId = question.id,
            studentAnswer = answer?.content ?: "",
            correctAnswer = question.correctAnswer ?: "",
            score = score,
            maxScore = question.points,
            isCorrect = isCorrect,
            feedback = if (isCorrect) "正确！" else "答案错误，正确答案是：${question.correctAnswer}"
        )
    }
    
    private suspend fun gradeShortAnswer(question: Question, answer: Answer?): QuestionGrade {
        if (answer?.content.isNullOrBlank()) {
            return QuestionGrade(
                questionId = question.id,
                studentAnswer = "",
                correctAnswer = question.correctAnswer ?: "",
                score = 0.0,
                maxScore = question.points,
                isCorrect = false,
                feedback = "未提供答案"
            )
        }
        
        // 使用LLM进行智能评分
        val prompt = """
            请评估以下简答题的答案：
            
            问题：${question.content}
            标准答案：${question.correctAnswer}
            学生答案：${answer!!.content}
            满分：${question.points}
            
            请给出：
            1. 得分（0-${question.points}分）
            2. 是否正确（true/false）
            3. 详细反馈
            
            请以JSON格式回答：
            {
                "score": 分数,
                "isCorrect": 是否正确,
                "feedback": "详细反馈"
            }
        """.trimIndent()
        
        return try {
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = prompt
                )
            )

            val options = LlmOptions(
                maxTokens = 500,
                temperature = 0.3
            )

            val llmResponse = llmProvider.generate(messages, options)
            
            // 简化解析（实际应该使用JSON解析）
            val score = extractScore(llmResponse.content, question.points)
            val isCorrect = score >= question.points * 0.8
            val feedback = extractFeedback(llmResponse.content)
            
            QuestionGrade(
                questionId = question.id,
                studentAnswer = answer.content,
                correctAnswer = question.correctAnswer ?: "",
                score = score,
                maxScore = question.points,
                isCorrect = isCorrect,
                feedback = feedback
            )
        } catch (e: Exception) {
            // 降级到简单匹配
            val similarity = calculateSimilarity(answer.content, question.correctAnswer ?: "")
            val score = question.points * similarity
            val isCorrect = similarity >= 0.8
            
            QuestionGrade(
                questionId = question.id,
                studentAnswer = answer.content,
                correctAnswer = question.correctAnswer ?: "",
                score = score,
                maxScore = question.points,
                isCorrect = isCorrect,
                feedback = if (isCorrect) "答案基本正确" else "答案需要改进，请参考标准答案"
            )
        }
    }
    
    private suspend fun gradeEssay(question: Question, answer: Answer?): QuestionGrade {
        if (answer?.content.isNullOrBlank()) {
            return QuestionGrade(
                questionId = question.id,
                studentAnswer = "",
                correctAnswer = question.correctAnswer ?: "",
                score = 0.0,
                maxScore = question.points,
                isCorrect = false,
                feedback = "未提供答案"
            )
        }
        
        // 使用LLM进行论文评分
        val prompt = """
            请评估以下论文题的答案：
            
            问题：${question.content}
            评分标准：${question.correctAnswer ?: "内容准确性、逻辑性、表达清晰度"}
            学生答案：${answer!!.content}
            满分：${question.points}
            
            请从以下几个方面评分：
            1. 内容准确性 (40%)
            2. 逻辑结构 (30%)
            3. 表达清晰度 (20%)
            4. 创新性 (10%)
            
            请给出总分和详细反馈。
        """.trimIndent()
        
        return try {
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = prompt
                )
            )

            val options = LlmOptions(
                maxTokens = 800,
                temperature = 0.3
            )

            val llmResponse = llmProvider.generate(messages, options)
            
            val score = extractScore(llmResponse.content, question.points)
            val isCorrect = score >= question.points * 0.6 // 论文题60%算及格
            val feedback = llmResponse.content
            
            QuestionGrade(
                questionId = question.id,
                studentAnswer = answer.content,
                correctAnswer = question.correctAnswer ?: "",
                score = score,
                maxScore = question.points,
                isCorrect = isCorrect,
                feedback = feedback
            )
        } catch (e: Exception) {
            // 降级评分
            val wordCount = answer.content.length
            val score = when {
                wordCount < 50 -> question.points * 0.3
                wordCount < 200 -> question.points * 0.6
                else -> question.points * 0.8
            }
            
            QuestionGrade(
                questionId = question.id,
                studentAnswer = answer.content,
                correctAnswer = question.correctAnswer ?: "",
                score = score,
                maxScore = question.points,
                isCorrect = score >= question.points * 0.6,
                feedback = "基于字数和基本结构的评分，建议人工复核"
            )
        }
    }
    
    private suspend fun gradeFillInBlank(question: Question, answer: Answer?): QuestionGrade {
        val studentAnswer = answer?.content?.trim() ?: ""
        val correctAnswer = question.correctAnswer?.trim() ?: ""
        
        val isCorrect = studentAnswer.equals(correctAnswer, ignoreCase = true) ||
                       calculateSimilarity(studentAnswer, correctAnswer) >= 0.9
        
        val score = if (isCorrect) question.points else 0.0
        
        return QuestionGrade(
            questionId = question.id,
            studentAnswer = studentAnswer,
            correctAnswer = correctAnswer,
            score = score,
            maxScore = question.points,
            isCorrect = isCorrect,
            feedback = if (isCorrect) "正确！" else "答案错误，正确答案是：$correctAnswer"
        )
    }
    
    private fun generateDetailedFeedback(
        assessment: Assessment,
        questionGrades: List<QuestionGrade>,
        percentage: Double,
        passed: Boolean
    ): String {
        val correctCount = questionGrades.count { it.isCorrect }
        val totalQuestions = questionGrades.size
        
        val feedback = StringBuilder()
        feedback.append("评估完成！\n\n")
        feedback.append("总体表现：\n")
        feedback.append("- 得分：${String.format("%.1f", percentage)}%\n")
        feedback.append("- 正确题数：$correctCount/$totalQuestions\n")
        feedback.append("- 结果：${if (passed) "通过" else "未通过"}\n\n")
        
        if (!passed) {
            feedback.append("需要改进的地方：\n")
            questionGrades.filter { !it.isCorrect }.forEach { grade ->
                feedback.append("- 题目${grade.questionId.value}：${grade.feedback}\n")
            }
        } else {
            feedback.append("表现优秀！继续保持。\n")
        }
        
        return feedback.toString()
    }
    
    private suspend fun recordGradingHistory(assessmentId: AssessmentId, result: GradingResult) {
        mutex.withLock {
            val history = gradingHistory.getOrPut(assessmentId) { mutableListOf() }.toMutableList()
            history.add(result)
            gradingHistory[assessmentId] = history
        }
    }
    
    private fun calculateStatistics(
        assessmentId: AssessmentId,
        results: List<GradingResult>
    ): AssessmentStatistics {
        val averageScore = results.map { it.percentage }.average()
        val passRate = results.count { it.passed }.toDouble() / results.size * 100
        
        val scoreDistribution = results.groupBy { 
            when {
                it.percentage >= 90 -> "A (90-100%)"
                it.percentage >= 80 -> "B (80-89%)"
                it.percentage >= 70 -> "C (70-79%)"
                it.percentage >= 60 -> "D (60-69%)"
                else -> "F (<60%)"
            }
        }.mapValues { it.value.size }
        
        // 简化的问题分析
        val questionAnalysis = emptyList<QuestionAnalysis>()
        
        return AssessmentStatistics(
            assessmentId = assessmentId,
            totalSubmissions = results.size,
            averageScore = averageScore,
            passRate = passRate,
            scoreDistribution = scoreDistribution,
            questionAnalysis = questionAnalysis
        )
    }
    
    private suspend fun generateStudentReport(
        assessment: Assessment,
        studentId: StudentId
    ): AssessmentReport {
        // 简化实现
        return AssessmentReport(
            id = "report_${UUID.randomUUID()}",
            assessmentId = assessment.id,
            studentId = studentId,
            reportType = ReportType.STUDENT,
            content = "学生个人评估报告",
            generatedAt = Clock.System.now()
        )
    }
    
    private suspend fun generateOverallReport(assessment: Assessment): AssessmentReport {
        // 简化实现
        return AssessmentReport(
            id = "report_${UUID.randomUUID()}",
            assessmentId = assessment.id,
            studentId = null,
            reportType = ReportType.OVERALL,
            content = "整体评估报告",
            generatedAt = Clock.System.now()
        )
    }
    
    // 辅助方法
    private fun extractScore(text: String, maxScore: Double): Double {
        // 简化的分数提取逻辑
        val scoreRegex = Regex("""score["\s]*:\s*(\d+\.?\d*)""")
        val match = scoreRegex.find(text.lowercase())
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.coerceIn(0.0, maxScore) ?: 0.0
    }
    
    private fun extractFeedback(text: String): String {
        // 简化的反馈提取逻辑
        val feedbackRegex = Regex("""feedback["\s]*:\s*["']([^"']+)["']""")
        val match = feedbackRegex.find(text)
        return match?.groupValues?.get(1) ?: "评分完成"
    }
    
    private fun calculateSimilarity(text1: String, text2: String): Double {
        // 简化的相似度计算
        val words1 = text1.lowercase().split(Regex("\\s+")).toSet()
        val words2 = text2.lowercase().split(Regex("\\s+")).toSet()
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return if (union > 0) intersection.toDouble() / union else 0.0
    }
}
