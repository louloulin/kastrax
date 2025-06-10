package ai.kastrax.edutech.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 课程实体
 */
@Serializable
data class Course(
    val id: CourseId = CourseId.generate(),
    val title: String,
    val description: String,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val instructorId: String,
    val estimatedDuration: Int = 0, // 分钟
    val learningObjectives: List<String> = emptyList(),
    val prerequisites: List<String> = emptyList(),
    val status: CourseStatus = CourseStatus.ACTIVE,
    val createdAt: Instant = kotlinx.datetime.Clock.System.now(),
    val updatedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 课程状态
 */
enum class CourseStatus {
    DRAFT,      // 草稿
    ACTIVE,     // 活跃
    ARCHIVED,   // 已归档
    DELETED     // 已删除
}

/**
 * 成绩实体
 */
@Serializable
data class Grade(
    val studentId: StudentId,
    val assignmentId: String,
    val courseId: CourseId? = null,
    val score: Double,
    val maxScore: Double,
    val percentage: Double = (score / maxScore) * 100,
    val letterGrade: String? = null,
    val feedback: String? = null,
    val gradedAt: Instant = kotlinx.datetime.Clock.System.now(),
    val gradedBy: String? = null
)
