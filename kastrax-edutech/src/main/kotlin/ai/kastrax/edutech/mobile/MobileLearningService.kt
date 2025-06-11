package ai.kastrax.edutech.mobile

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.analytics.LearningAnalyticsService
import ai.kastrax.edutech.content.ContentManagementService
import ai.kastrax.edutech.recommendation.PersonalizationEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * 移动学习服务
 * 提供移动端学习功能的核心业务逻辑
 */
class MobileLearningService(
    private val analyticsService: LearningAnalyticsService,
    private val contentService: ContentManagementService,
    private val personalizationEngine: PersonalizationEngine,
    private val syncService: MobileSyncService
) {
    private val activeSessions = mutableMapOf<String, MobileLearningSession>()
    private val deviceRegistry = mutableMapOf<String, MobileDevice>()
    private val offlinePackages = mutableMapOf<String, OfflineLearningPackage>()

    /**
     * 注册移动设备
     */
    suspend fun registerDevice(device: MobileDevice): MobileApiResponse<MobileDevice> {
        return try {
            deviceRegistry[device.deviceId] = device
            MobileApiResponse(
                success = true,
                data = device,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to register device: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    /**
     * 创建移动学习会话
     */
    suspend fun createLearningSession(
        studentId: StudentId,
        deviceId: String,
        location: GeoLocation? = null
    ): MobileApiResponse<MobileLearningSession> {
        return try {
            val device = deviceRegistry[deviceId]
                ?: throw IllegalArgumentException("Device not registered: $deviceId")

            val session = MobileLearningSession(
                sessionId = generateSessionId(),
                studentId = studentId,
                deviceId = deviceId,
                startTime = Clock.System.now(),
                location = location,
                networkCondition = determineNetworkCondition(device),
                batteryLevel = getCurrentBatteryLevel(device),
                isOfflineSession = !device.isOnline
            )

            activeSessions[session.sessionId] = session

            MobileApiResponse(
                success = true,
                data = session,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to create session: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    /**
     * 获取个性化移动学习内容
     */
    suspend fun getPersonalizedContent(
        studentId: StudentId,
        deviceId: String,
        preferences: MobileLearningPreferences
    ): MobileApiResponse<List<MobileContent>> {
        return try {
            val device = deviceRegistry[deviceId]
                ?: throw IllegalArgumentException("Device not registered: $deviceId")

            // 获取学生学习档案
            val studentProfile = analyticsService.getStudentProfile(studentId)

            // 根据设备能力和偏好筛选内容
            val availableContent = contentService.getAvailableContent()
                .filter { content ->
                    isContentCompatibleWithDevice(content, device) &&
                    isContentMatchingPreferences(content, preferences)
                }

            // 使用个性化引擎推荐内容
            val personalizedContent = personalizationEngine.recommendContent(
                studentProfile = studentProfile,
                availableContent = availableContent,
                context = mapOf(
                    "deviceType" to device.deviceType.name,
                    "networkCondition" to device.isOnline.toString(),
                    "screenSize" to "${device.screenSize.width}x${device.screenSize.height}"
                )
            )

            // 转换为移动内容格式
            val mobileContent = personalizedContent.map { content ->
                convertToMobileContent(content, device)
            }

            MobileApiResponse(
                success = true,
                data = mobileContent,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to get personalized content: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    /**
     * 记录移动学习活动
     */
    suspend fun recordLearningActivity(
        sessionId: String,
        activity: MobileLearningActivity
    ): MobileApiResponse<MobileLearningActivity> {
        return try {
            val session = activeSessions[sessionId]
                ?: throw IllegalArgumentException("Session not found: $sessionId")

            // 更新会话中的活动列表
            val updatedActivities = session.activities + activity
            val updatedSession = session.copy(activities = updatedActivities)
            activeSessions[sessionId] = updatedSession

            // 如果设备在线，立即同步
            if (!session.isOfflineSession) {
                syncService.syncActivity(activity)
            }

            // 更新学习分析
            analyticsService.recordLearningActivity(
                studentId = session.studentId,
                activity = convertToLearningActivity(activity),
                context = mapOf(
                    "deviceType" to session.deviceId,
                    "location" to (session.location?.toString() ?: "unknown"),
                    "networkCondition" to session.networkCondition.name
                )
            )

            MobileApiResponse(
                success = true,
                data = activity,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to record activity: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    /**
     * 结束学习会话
     */
    suspend fun endLearningSession(sessionId: String): MobileApiResponse<MobileLearningSession> {
        return try {
            val session = activeSessions[sessionId]
                ?: throw IllegalArgumentException("Session not found: $sessionId")

            val endedSession = session.copy(
                endTime = Clock.System.now()
            )

            activeSessions.remove(sessionId)

            // 同步会话数据
            if (!session.isOfflineSession) {
                syncService.syncSession(endedSession)
            }

            MobileApiResponse(
                success = true,
                data = endedSession,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to end session: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    /**
     * 获取离线学习包
     */
    suspend fun getOfflineLearningPackages(
        studentId: StudentId,
        subject: Subject? = null
    ): MobileApiResponse<List<OfflineLearningPackage>> {
        return try {
            val studentProfile = analyticsService.getStudentProfile(studentId)
            
            val packages = offlinePackages.values.filter { pkg ->
                (subject == null || pkg.subject == subject) &&
                pkg.gradeLevel == studentProfile.gradeLevel
            }

            MobileApiResponse(
                success = true,
                data = packages,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to get offline packages: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    /**
     * 获取移动学习统计
     */
    suspend fun getLearningStats(
        studentId: StudentId,
        deviceId: String,
        timeRange: Pair<Instant, Instant>? = null
    ): MobileApiResponse<MobileLearningStats> {
        return try {
            val stats = calculateMobileLearningStats(studentId, deviceId, timeRange)

            MobileApiResponse(
                success = true,
                data = stats,
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        } catch (e: Exception) {
            MobileApiResponse(
                success = false,
                error = "Failed to get learning stats: ${e.message}",
                timestamp = Clock.System.now(),
                requestId = generateRequestId()
            )
        }
    }

    // 私有辅助方法
    private fun generateRequestId(): String = "req_${Clock.System.now().toEpochMilliseconds()}"
    private fun generateSessionId(): String = "session_${Clock.System.now().toEpochMilliseconds()}"

    private fun determineNetworkCondition(device: MobileDevice): NetworkCondition {
        return if (device.isOnline) NetworkCondition.WIFI else NetworkCondition.OFFLINE
    }

    private fun getCurrentBatteryLevel(device: MobileDevice): Int? {
        // 模拟电池电量获取
        return (50..100).random()
    }

    private fun isContentCompatibleWithDevice(content: LearningContent, device: MobileDevice): Boolean {
        // 检查内容是否与设备兼容
        return when (content.contentType) {
            ContentType.VIDEO -> device.capabilities.hasCamera || device.screenSize.width >= 720
            ContentType.AUDIO -> device.capabilities.hasMicrophone
            ContentType.INTERACTIVE -> device.deviceType in listOf(DeviceType.ANDROID_TABLET, DeviceType.IOS_TABLET)
            else -> true
        }
    }

    private fun isContentMatchingPreferences(
        content: LearningContent, 
        preferences: MobileLearningPreferences
    ): Boolean {
        val mobileContentType = when (content.contentType) {
            ContentType.TEXT -> MobileContentType.TEXT
            ContentType.VIDEO -> MobileContentType.VIDEO
            ContentType.AUDIO -> MobileContentType.AUDIO
            ContentType.IMAGE -> MobileContentType.IMAGE
            ContentType.INTERACTIVE -> MobileContentType.INTERACTIVE_MEDIA
            ContentType.DOCUMENT -> MobileContentType.DOCUMENT
        }
        return mobileContentType in preferences.preferredContentTypes
    }

    private fun convertToMobileContent(content: LearningContent, device: MobileDevice): MobileContent {
        return MobileContent(
            contentId = content.id,
            title = content.title,
            description = content.description,
            contentType = when (content.contentType) {
                ContentType.TEXT -> MobileContentType.TEXT
                ContentType.VIDEO -> MobileContentType.VIDEO
                ContentType.AUDIO -> MobileContentType.AUDIO
                ContentType.IMAGE -> MobileContentType.IMAGE
                ContentType.INTERACTIVE -> MobileContentType.INTERACTIVE_MEDIA
                ContentType.DOCUMENT -> MobileContentType.DOCUMENT
            },
            mediaUrl = content.url,
            duration = content.estimatedDuration,
            fileSize = estimateFileSize(content),
            isDownloaded = false
        )
    }

    private fun convertToLearningActivity(mobileActivity: MobileLearningActivity): LearningActivity {
        return LearningActivity(
            id = mobileActivity.activityId,
            type = when (mobileActivity.type) {
                MobileActivityType.VIDEO_WATCHING -> ActivityType.VIDEO_WATCHING
                MobileActivityType.INTERACTIVE_QUIZ -> ActivityType.QUIZ
                MobileActivityType.READING_COMPREHENSION -> ActivityType.READING
                else -> ActivityType.PRACTICE
            },
            contentId = mobileActivity.content.contentId,
            startTime = mobileActivity.startTime,
            endTime = mobileActivity.endTime,
            completed = mobileActivity.isCompleted,
            score = mobileActivity.progress.percentage.toDouble()
        )
    }

    private fun estimateFileSize(content: LearningContent): Long {
        return when (content.contentType) {
            ContentType.VIDEO -> 50_000_000L // 50MB
            ContentType.AUDIO -> 5_000_000L  // 5MB
            ContentType.IMAGE -> 1_000_000L  // 1MB
            else -> 100_000L // 100KB
        }
    }

    private suspend fun calculateMobileLearningStats(
        studentId: StudentId,
        deviceId: String,
        timeRange: Pair<Instant, Instant>?
    ): MobileLearningStats {
        // 模拟统计计算
        return MobileLearningStats(
            studentId = studentId,
            deviceId = deviceId,
            totalSessionTime = 5.hours,
            activitiesCompleted = 25,
            averageSessionDuration = 30.minutes,
            mostUsedContentType = MobileContentType.VIDEO,
            offlineUsagePercentage = 0.3f,
            dailyUsagePattern = mapOf(
                9 to 45.minutes,
                14 to 30.minutes,
                19 to 60.minutes
            ),
            weeklyProgress = listOf(
                DailyProgress("2024-12-19", 45.minutes, 3, listOf("数学", "英语")),
                DailyProgress("2024-12-20", 60.minutes, 4, listOf("科学", "历史"))
            ),
            achievements = listOf(
                MobileAchievement(
                    achievementId = "consistency_7days",
                    title = "连续学习7天",
                    description = "坚持连续7天使用移动学习",
                    iconUrl = "/icons/consistency.png",
                    unlockedAt = Clock.System.now(),
                    category = AchievementCategory.CONSISTENCY
                )
            )
        )
    }
}
