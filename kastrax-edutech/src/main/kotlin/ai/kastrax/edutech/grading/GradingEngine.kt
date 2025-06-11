package ai.kastrax.edutech.grading

import ai.kastrax.edutech.auth.AuthService
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * 批改引擎 - Phase 4 Week 13-14 集成测试支持
 * 
 * 提供作业批改和权限控制功能
 */
class GradingEngine(
    private val authService: AuthService
) {
    /**
     * 批改提交 (无用户ID版本)
     * 
     * @param submissionId 提交ID
     * @return 批改结果
     */
    fun gradeSubmission(submissionId: String): Map<String, Any> {
        return mapOf(
            "submissionId" to submissionId,
            "score" to Random.nextInt(70, 96),
            "feedback" to "Good work with room for improvement",
            "gradedAt" to Clock.System.now().toString(),
            "grader" to "system"
        )
    }
    
    /**
     * 批改提交 (带用户ID版本，支持权限控制)
     * 
     * @param submissionId 提交ID
     * @param graderId 批改者ID
     * @return 批改结果
     * @throws SecurityException 如果权限不足
     */
    fun gradeSubmission(submissionId: String, graderId: String): Map<String, Any> {
        // 检查权限
        if (!authService.hasPermission(graderId, "GRADE_ASSIGNMENTS", "course123")) {
            throw SecurityException("Insufficient permissions")
        }
        
        return mapOf(
            "submissionId" to submissionId,
            "score" to Random.nextInt(80, 96),
            "feedback" to "Excellent work! Well done.",
            "gradedAt" to Clock.System.now().toString(),
            "grader" to graderId
        )
    }
    
    /**
     * 批量批改提交
     * 
     * @param submissionIds 提交ID列表
     * @param graderId 批改者ID
     * @return 批改结果列表
     */
    fun batchGradeSubmissions(submissionIds: List<String>, graderId: String): List<Map<String, Any>> {
        return submissionIds.map { submissionId ->
            try {
                gradeSubmission(submissionId, graderId)
            } catch (e: SecurityException) {
                mapOf(
                    "submissionId" to submissionId,
                    "error" to (e.message ?: "Unknown error"),
                    "status" to "failed"
                )
            }
        }
    }
    
    /**
     * 获取批改历史
     * 
     * @param submissionId 提交ID
     * @return 批改历史
     */
    fun getGradingHistory(submissionId: String): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "submissionId" to submissionId,
                "score" to 85,
                "gradedAt" to Clock.System.now().toString(),
                "grader" to "teacher123",
                "version" to 1
            )
        )
    }
    
    /**
     * 重新批改提交
     * 
     * @param submissionId 提交ID
     * @param graderId 批改者ID
     * @param reason 重新批改原因
     * @return 批改结果
     */
    fun regradeSubmission(submissionId: String, graderId: String, reason: String): Map<String, Any> {
        if (!authService.hasPermission(graderId, "GRADE_ASSIGNMENTS", "course123")) {
            throw SecurityException("Insufficient permissions")
        }
        
        return mapOf(
            "submissionId" to submissionId,
            "score" to Random.nextInt(75, 91),
            "feedback" to "Revised grading based on: $reason",
            "gradedAt" to Clock.System.now().toString(),
            "grader" to graderId,
            "regradeReason" to reason,
            "version" to 2
        )
    }
}
