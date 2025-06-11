package ai.kastrax.edutech.assessment

import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * 评估引擎 - Phase 4 Week 13-14 集成测试支持
 * 
 * 提供评估创建和管理功能
 */
class AssessmentEngine {
    
    /**
     * 创建评估
     * 
     * @param courseId 课程ID
     * @return 评估信息
     */
    fun createAssessment(courseId: String): Map<String, Any> {
        val assessmentId = "assessment_${System.currentTimeMillis()}"
        
        return mapOf(
            "assessmentId" to assessmentId,
            "courseId" to courseId,
            "title" to "Course Assessment for $courseId",
            "questions" to generateQuestions(),
            "createdAt" to Clock.System.now().toString(),
            "duration" to 60, // 分钟
            "totalPoints" to 100
        )
    }
    
    /**
     * 获取评估详情
     * 
     * @param assessmentId 评估ID
     * @return 评估详情
     */
    fun getAssessment(assessmentId: String): Map<String, Any>? {
        return mapOf(
            "assessmentId" to assessmentId,
            "title" to "Sample Assessment",
            "questions" to generateQuestions(),
            "duration" to 60,
            "totalPoints" to 100,
            "status" to "active"
        )
    }
    
    /**
     * 更新评估
     * 
     * @param assessmentId 评估ID
     * @param updates 更新内容
     * @return 更新结果
     */
    fun updateAssessment(assessmentId: String, updates: Map<String, Any>): Map<String, Any> {
        return mapOf(
            "assessmentId" to assessmentId,
            "updated" to true,
            "updatedAt" to Clock.System.now().toString(),
            "changes" to updates.keys.toList()
        )
    }
    
    /**
     * 删除评估
     * 
     * @param assessmentId 评估ID
     * @return 删除结果
     */
    fun deleteAssessment(assessmentId: String): Map<String, Any> {
        return mapOf(
            "assessmentId" to assessmentId,
            "deleted" to true,
            "deletedAt" to Clock.System.now().toString()
        )
    }
    
    /**
     * 获取课程的所有评估
     * 
     * @param courseId 课程ID
     * @return 评估列表
     */
    fun getAssessmentsByCourse(courseId: String): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "assessmentId" to "assessment1",
                "courseId" to courseId,
                "title" to "Midterm Assessment",
                "status" to "active"
            ),
            mapOf(
                "assessmentId" to "assessment2",
                "courseId" to courseId,
                "title" to "Final Assessment",
                "status" to "draft"
            )
        )
    }
    
    /**
     * 提交评估答案
     * 
     * @param assessmentId 评估ID
     * @param studentId 学生ID
     * @param answers 答案
     * @return 提交结果
     */
    fun submitAssessment(
        assessmentId: String,
        studentId: String,
        answers: List<String>
    ): Map<String, Any> {
        val score = (70..95)Random.nextInt()
        
        return mapOf(
            "submissionId" to "submission_${System.currentTimeMillis()}",
            "assessmentId" to assessmentId,
            "studentId" to studentId,
            "score" to score,
            "passed" to (score >= 60),
            "submittedAt" to Clock.System.now().toString(),
            "feedback" to generateFeedback(score)
        )
    }
    
    /**
     * 获取评估结果
     * 
     * @param assessmentId 评估ID
     * @param studentId 学生ID
     * @return 评估结果
     */
    fun getAssessmentResult(assessmentId: String, studentId: String): Map<String, Any>? {
        return mapOf(
            "assessmentId" to assessmentId,
            "studentId" to studentId,
            "score" to 85,
            "passed" to true,
            "completedAt" to Clock.System.now().toString(),
            "timeSpent" to 45 // 分钟
        )
    }
    
    /**
     * 生成问题
     */
    private fun generateQuestions(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "questionId" to "q1",
                "type" to "multiple_choice",
                "question" to "What is the capital of France?",
                "options" to listOf("London", "Berlin", "Paris", "Madrid"),
                "correctAnswer" to "Paris",
                "points" to 10
            ),
            mapOf(
                "questionId" to "q2",
                "type" to "short_answer",
                "question" to "Explain the concept of inheritance in OOP.",
                "points" to 20
            ),
            mapOf(
                "questionId" to "q3",
                "type" to "essay",
                "question" to "Discuss the advantages and disadvantages of cloud computing.",
                "points" to 30
            )
        )
    }
    
    /**
     * 生成反馈
     */
    private fun generateFeedback(score: Int): String {
        return when {
            score >= 90 -> "Excellent work! You have demonstrated mastery of the subject."
            score >= 80 -> "Good job! You have a solid understanding with room for minor improvements."
            score >= 70 -> "Satisfactory performance. Consider reviewing some concepts."
            score >= 60 -> "You passed, but there's significant room for improvement."
            else -> "Please review the material and consider retaking the assessment."
        }
    }
}
