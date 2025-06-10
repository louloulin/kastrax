package ai.kastrax.edutech.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 学生实体 - 基于ed2.md第3.2节数据模型设计
 * 
 * 实现Kastrax强类型系统的教育数据安全保障
 */
@Serializable
data class Student(
    val id: StudentId,
    val name: String,
    val email: String,
    val gradeLevel: GradeLevel,
    val learningProfile: LearningProfile,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun create(
            name: String,
            email: String,
            gradeLevel: GradeLevel,
            learningProfile: LearningProfile = LearningProfile.default()
        ): Student {
            val now = kotlinx.datetime.Clock.System.now()
            return Student(
                id = StudentId.generate(),
                name = name,
                email = email,
                gradeLevel = gradeLevel,
                learningProfile = learningProfile,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

/**
 * 强类型的学生ID，确保类型安全
 */
@Serializable
@JvmInline
value class StudentId(val value: String) {
    companion object {
        fun generate(): StudentId = StudentId("student_${java.util.UUID.randomUUID()}")
        fun fromString(value: String): StudentId = StudentId(value)
    }
    
    override fun toString(): String = value
}

/**
 * 年级级别枚举
 */
@Serializable
enum class GradeLevel(val displayName: String, val numericValue: Int) {
    KINDERGARTEN("幼儿园", 0),
    GRADE_1("一年级", 1),
    GRADE_2("二年级", 2),
    GRADE_3("三年级", 3),
    GRADE_4("四年级", 4),
    GRADE_5("五年级", 5),
    GRADE_6("六年级", 6),
    GRADE_7("七年级", 7),
    GRADE_8("八年级", 8),
    GRADE_9("九年级", 9),
    GRADE_10("十年级", 10),
    GRADE_11("十一年级", 11),
    GRADE_12("十二年级", 12),
    UNDERGRADUATE("本科", 13),
    GRADUATE("研究生", 14),
    ADULT_EDUCATION("成人教育", 15);
    
    fun isElementary(): Boolean = numericValue in 1..6
    fun isMiddleSchool(): Boolean = numericValue in 7..9
    fun isHighSchool(): Boolean = numericValue in 10..12
    fun isHigherEducation(): Boolean = numericValue >= 13
}

/**
 * 学习档案 - 实现ed2.md第2.2节个性化学习建模
 */
@Serializable
data class LearningProfile(
    val learningStyle: LearningStyle,
    val preferredContentTypes: Set<ContentType>,
    val currentDifficultyLevel: DifficultyLevel,
    val subjectPreferences: Map<Subject, PreferenceLevel>,
    val knowledgeState: Map<Topic, MasteryLevel>,
    val cognitiveAbilities: CognitiveAbilities,
    val motivationFactors: MotivationProfile
) {
    companion object {
        fun default(): LearningProfile = LearningProfile(
            learningStyle = LearningStyle.BALANCED,
            preferredContentTypes = setOf(ContentType.TEXT, ContentType.VIDEO),
            currentDifficultyLevel = DifficultyLevel.BEGINNER,
            subjectPreferences = emptyMap(),
            knowledgeState = emptyMap(),
            cognitiveAbilities = CognitiveAbilities.default(),
            motivationFactors = MotivationProfile.default()
        )
    }
}

/**
 * 学习风格枚举
 */
@Serializable
enum class LearningStyle(val description: String) {
    VISUAL("视觉学习者 - 偏好图表、图像和视觉材料"),
    AUDITORY("听觉学习者 - 偏好音频、讲座和讨论"),
    KINESTHETIC("动觉学习者 - 偏好实践操作和体验学习"),
    READING_WRITING("读写学习者 - 偏好文本阅读和写作练习"),
    BALANCED("平衡型 - 多种学习方式结合");
    
    fun getMultiplierFor(contentType: ContentType): Double = when (this) {
        VISUAL -> when (contentType) {
            ContentType.IMAGE, ContentType.VIDEO, ContentType.INTERACTIVE -> 1.2
            ContentType.AUDIO -> 0.8
            else -> 1.0
        }
        AUDITORY -> when (contentType) {
            ContentType.AUDIO, ContentType.VIDEO -> 1.2
            ContentType.TEXT -> 0.9
            else -> 1.0
        }
        KINESTHETIC -> when (contentType) {
            ContentType.INTERACTIVE, ContentType.SIMULATION -> 1.3
            ContentType.TEXT -> 0.8
            else -> 1.0
        }
        READING_WRITING -> when (contentType) {
            ContentType.TEXT, ContentType.DOCUMENT -> 1.2
            ContentType.VIDEO -> 0.9
            else -> 1.0
        }
        BALANCED -> 1.0
    }
}

/**
 * 内容类型枚举
 */
@Serializable
enum class ContentType(val displayName: String) {
    TEXT("文本"),
    VIDEO("视频"),
    AUDIO("音频"),
    IMAGE("图像"),
    INTERACTIVE("交互式"),
    SIMULATION("模拟"),
    DOCUMENT("文档"),
    QUIZ("测验"),
    GAME("游戏");
}

/**
 * 难度级别
 */
@Serializable
enum class DifficultyLevel(val displayName: String, val numericValue: Int) {
    BEGINNER("初级", 1),
    ELEMENTARY("基础", 2),
    INTERMEDIATE("中级", 3),
    ADVANCED("高级", 4),
    EXPERT("专家", 5);
    
    fun next(): DifficultyLevel? = values().getOrNull(ordinal + 1)
    fun previous(): DifficultyLevel? = values().getOrNull(ordinal - 1)
}

/**
 * 学科枚举
 */
@Serializable
enum class Subject(val displayName: String, val category: SubjectCategory) {
    MATHEMATICS("数学", SubjectCategory.STEM),
    PHYSICS("物理", SubjectCategory.STEM),
    CHEMISTRY("化学", SubjectCategory.STEM),
    BIOLOGY("生物", SubjectCategory.STEM),
    COMPUTER_SCIENCE("计算机科学", SubjectCategory.STEM),
    CHINESE("语文", SubjectCategory.LANGUAGE),
    ENGLISH("英语", SubjectCategory.LANGUAGE),
    HISTORY("历史", SubjectCategory.HUMANITIES),
    GEOGRAPHY("地理", SubjectCategory.HUMANITIES),
    ART("美术", SubjectCategory.ARTS),
    MUSIC("音乐", SubjectCategory.ARTS),
    PHYSICAL_EDUCATION("体育", SubjectCategory.PHYSICAL);
}

@Serializable
enum class SubjectCategory(val displayName: String) {
    STEM("科学技术工程数学"),
    LANGUAGE("语言文学"),
    HUMANITIES("人文社科"),
    ARTS("艺术"),
    PHYSICAL("体育健康")
}

/**
 * 偏好级别
 */
@Serializable
enum class PreferenceLevel(val displayName: String, val weight: Double) {
    DISLIKE("不喜欢", 0.5),
    NEUTRAL("中性", 1.0),
    LIKE("喜欢", 1.5),
    LOVE("热爱", 2.0)
}

/**
 * 主题ID和掌握程度
 */
@Serializable
@JvmInline
value class Topic(val value: String) {
    override fun toString(): String = value
}

@Serializable
enum class MasteryLevel(val displayName: String, val percentage: Int) {
    NOT_STARTED("未开始", 0),
    BEGINNER("初学", 25),
    DEVELOPING("发展中", 50),
    PROFICIENT("熟练", 75),
    MASTERED("精通", 100);
    
    fun getDifficultyAdjustment(): Double = when (this) {
        NOT_STARTED -> 0.5
        BEGINNER -> 0.7
        DEVELOPING -> 1.0
        PROFICIENT -> 1.3
        MASTERED -> 1.5
    }
}

/**
 * 认知能力评估
 */
@Serializable
data class CognitiveAbilities(
    val workingMemoryCapacity: Int, // 工作记忆容量 (1-10)
    val processingSpeed: Int,       // 处理速度 (1-10)
    val attentionSpan: Int,         // 注意力持续时间 (1-10)
    val logicalReasoning: Int,      // 逻辑推理能力 (1-10)
    val spatialAbility: Int         // 空间能力 (1-10)
) {
    companion object {
        fun default(): CognitiveAbilities = CognitiveAbilities(
            workingMemoryCapacity = 5,
            processingSpeed = 5,
            attentionSpan = 5,
            logicalReasoning = 5,
            spatialAbility = 5
        )
    }
    
    fun getOverallScore(): Double = 
        (workingMemoryCapacity + processingSpeed + attentionSpan + logicalReasoning + spatialAbility) / 5.0
}

/**
 * 动机档案
 */
@Serializable
data class MotivationProfile(
    val intrinsicMotivation: Int,    // 内在动机 (1-10)
    val extrinsicMotivation: Int,    // 外在动机 (1-10)
    val goalOrientation: GoalOrientation,
    val competitiveness: Int,        // 竞争性 (1-10)
    val persistenceLevel: Int        // 坚持程度 (1-10)
) {
    companion object {
        fun default(): MotivationProfile = MotivationProfile(
            intrinsicMotivation = 5,
            extrinsicMotivation = 5,
            goalOrientation = GoalOrientation.BALANCED,
            competitiveness = 5,
            persistenceLevel = 5
        )
    }
    
    fun getCurrentBoostFactors(): Map<String, Double> = mapOf(
        "intrinsic" to (intrinsicMotivation / 10.0),
        "extrinsic" to (extrinsicMotivation / 10.0),
        "competitive" to (competitiveness / 10.0),
        "persistent" to (persistenceLevel / 10.0)
    )
}

@Serializable
enum class GoalOrientation(val description: String) {
    MASTERY("掌握导向 - 专注于理解和掌握知识"),
    PERFORMANCE("表现导向 - 专注于成绩和排名"),
    BALANCED("平衡导向 - 兼顾理解和表现")
}
