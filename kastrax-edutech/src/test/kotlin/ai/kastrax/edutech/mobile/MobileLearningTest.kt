package ai.kastrax.edutech.mobile

import ai.kastrax.edutech.models.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

/**
 * 移动端学习应用测试
 */
class MobileLearningTest {

    @BeforeEach
    fun setUp() {
        // 简化设置，专注于数据模型和基本逻辑测试
    }

    @Test
    fun `ML-001 - 移动设备信息模型测试`() = runTest {
        // Given
        val deviceInfo = createTestDeviceInfo()

        // Then
        assertNotNull(deviceInfo.deviceId)
        assertEquals(DeviceType.ANDROID_PHONE, deviceInfo.deviceType)
        assertTrue(deviceInfo.capabilities.hasCamera)
        assertTrue(deviceInfo.capabilities.hasMicrophone)
        assertTrue(deviceInfo.isOnline)
    }

    @Test
    fun `ML-002 - 移动内容模型测试`() = runTest {
        // Given
        val mobileContent = createTestMobileContent()

        // Then
        assertNotNull(mobileContent.contentId)
        assertNotNull(mobileContent.title)
        assertEquals(MobileContentType.INTERACTIVE_MEDIA, mobileContent.contentType)
        assertEquals(15.minutes, mobileContent.duration)
        assertFalse(mobileContent.isDownloaded)
    }

    @Test
    fun `ML-003 - 移动学习活动模型测试`() = runTest {
        // Given
        val activity = createTestMobileActivity()

        // Then
        assertNotNull(activity.activityId)
        assertEquals(MobileActivityType.INTERACTIVE_QUIZ, activity.type)
        assertNotNull(activity.content)
        assertEquals(0.0f, activity.progress.percentage)
        assertEquals(0, activity.progress.currentStep)
        assertEquals(10, activity.progress.totalSteps)
        assertFalse(activity.isCompleted)
    }

    @Test
    fun `ML-004 - 离线学习包模型测试`() = runTest {
        // Given
        val offlinePackage = createTestOfflinePackage()

        // Then
        assertNotNull(offlinePackage.packageId)
        assertEquals("数学基础离线包", offlinePackage.title)
        assertEquals(Subject.MATHEMATICS, offlinePackage.subject)
        assertEquals(GradeLevel.GRADE_8, offlinePackage.gradeLevel)
        assertEquals(2.hours, offlinePackage.estimatedDuration)
        assertEquals(DownloadStatus.NOT_DOWNLOADED, offlinePackage.downloadStatus)
    }

    @Test
    fun `ML-005 - 同步响应模型测试`() = runTest {
        // Given
        val syncResponse = SyncResponse(
            syncedActivities = 5,
            failedActivities = 1,
            lastSyncTime = Clock.System.now(),
            nextSyncTime = Clock.System.now().plus(30.minutes)
        )

        // Then
        assertEquals(5, syncResponse.syncedActivities)
        assertEquals(1, syncResponse.failedActivities)
        assertNotNull(syncResponse.lastSyncTime)
        assertNotNull(syncResponse.nextSyncTime)
    }

    @Test
    fun `ML-006 - 移动学习统计模型测试`() = runTest {
        // Given
        val stats = createTestMobileLearningStats()

        // Then
        assertNotNull(stats.studentId)
        assertNotNull(stats.deviceId)
        assertTrue(stats.totalSessionTime >= kotlin.time.Duration.ZERO)
        assertTrue(stats.activitiesCompleted >= 0)
        assertTrue(stats.offlineUsagePercentage >= 0.0f && stats.offlineUsagePercentage <= 1.0f)
        assertEquals(MobileContentType.VIDEO, stats.mostUsedContentType)
    }

    @Test
    fun `ML-007 - 移动学习会话模型测试`() = runTest {
        // Given
        val session = MobileLearningSession(
            sessionId = "session_123",
            studentId = StudentId("student_123"),
            deviceId = "device_123",
            startTime = Clock.System.now(),
            networkCondition = NetworkCondition.WIFI,
            isOfflineSession = false
        )

        // Then
        assertNotNull(session.sessionId)
        assertEquals(StudentId("student_123"), session.studentId)
        assertEquals("device_123", session.deviceId)
        assertNotNull(session.startTime)
        assertEquals(NetworkCondition.WIFI, session.networkCondition)
        assertFalse(session.isOfflineSession)
    }

    @Test
    fun `ML-008 - 离线模式设备测试`() = runTest {
        // Given
        val deviceInfo = createTestDeviceInfo().copy(isOnline = false)
        val activity = createTestMobileActivity().copy(
            type = MobileActivityType.OFFLINE_PRACTICE
        )

        // Then
        assertFalse(deviceInfo.isOnline)
        assertEquals(MobileActivityType.OFFLINE_PRACTICE, activity.type)
        assertTrue(deviceInfo.capabilities.supportsOfflineMode)
    }

    @Test
    fun `ML-009 - 设备兼容性测试`() = runTest {
        // Given - 测试不同设备类型
        val deviceTypes = listOf(
            DeviceType.ANDROID_PHONE,
            DeviceType.ANDROID_TABLET,
            DeviceType.IOS_PHONE,
            DeviceType.IOS_TABLET
        )

        // When & Then
        deviceTypes.forEach { deviceType ->
            val deviceInfo = createTestDeviceInfo().copy(deviceType = deviceType)

            // 验证设备类型正确设置
            assertEquals(deviceType, deviceInfo.deviceType)
            assertNotNull(deviceInfo.capabilities)
        }
    }

    @Test
    fun `ML-010 - 移动学习成就模型测试`() = runTest {
        // Given
        val achievement = createTestMobileAchievement()

        // Then
        assertNotNull(achievement.achievementId)
        assertNotNull(achievement.title)
        assertNotNull(achievement.description)
        assertEquals(AchievementCategory.PROGRESS, achievement.category)
        assertNotNull(achievement.unlockedAt)
    }

    // 辅助方法
    private fun createTestDeviceInfo(): MobileDevice {
        return MobileDevice(
            deviceId = "test_device_123",
            deviceType = DeviceType.ANDROID_PHONE,
            osVersion = "Android 12",
            appVersion = "1.0.0",
            screenSize = ScreenSize(1080, 1920, 2.0f),
            capabilities = DeviceCapabilities(
                hasCamera = true,
                hasMicrophone = true,
                hasGPS = true,
                supportsBiometric = true,
                supportsOfflineMode = true,
                maxStorageSize = 1_000_000_000L // 1GB
            ),
            lastSyncTime = Clock.System.now(),
            isOnline = true
        )
    }

    private fun createTestMobileContent(): MobileContent {
        return MobileContent(
            contentId = "content_123",
            title = "测试移动内容",
            description = "这是一个测试移动内容",
            contentType = MobileContentType.INTERACTIVE_MEDIA,
            duration = 15.minutes,
            fileSize = 5_000_000L, // 5MB
            isDownloaded = false
        )
    }

    private fun createTestPreferences(studentId: StudentId): MobileLearningPreferences {
        return MobileLearningPreferences(
            studentId = studentId,
            preferredContentTypes = setOf(
                MobileContentType.VIDEO,
                MobileContentType.INTERACTIVE_MEDIA
            ),
            autoDownloadEnabled = true,
            wifiOnlyDownload = true,
            maxStorageUsage = 500_000_000L, // 500MB
            notificationSettings = NotificationSettings(
                enablePushNotifications = true,
                studyReminders = true,
                achievementNotifications = true,
                progressUpdates = true
            ),
            accessibilitySettings = AccessibilitySettings(
                fontSize = FontSize.MEDIUM,
                highContrast = false,
                voiceOverEnabled = false,
                subtitlesEnabled = false,
                reducedMotion = false
            )
        )
    }

    private fun createTestOfflinePackage(): OfflineLearningPackage {
        return OfflineLearningPackage(
            packageId = "package_math_basic",
            title = "数学基础离线包",
            description = "包含基础数学概念的离线学习内容",
            subject = Subject.MATHEMATICS,
            gradeLevel = GradeLevel.GRADE_8,
            estimatedDuration = 2.hours,
            contents = emptyList(),
            activities = emptyList(),
            totalSize = 25_000_000L, // 25MB
            downloadStatus = DownloadStatus.NOT_DOWNLOADED
        )
    }

    private fun createTestMobileActivity(): MobileLearningActivity {
        return MobileLearningActivity(
            activityId = "activity_123",
            type = MobileActivityType.INTERACTIVE_QUIZ,
            content = createTestMobileContent(),
            startTime = Clock.System.now(),
            progress = ActivityProgress(
                percentage = 0.0f,
                currentStep = 0,
                totalSteps = 10,
                timeSpent = kotlin.time.Duration.ZERO,
                lastUpdateTime = Clock.System.now()
            )
        )
    }

    private fun createTestMobileLearningStats(): MobileLearningStats {
        return MobileLearningStats(
            studentId = StudentId("student_123"),
            deviceId = "device_123",
            totalSessionTime = 2.hours,
            activitiesCompleted = 15,
            averageSessionDuration = 30.minutes,
            mostUsedContentType = MobileContentType.VIDEO,
            offlineUsagePercentage = 0.3f,
            dailyUsagePattern = mapOf(9 to 1.hours, 14 to 30.minutes, 19 to 45.minutes),
            weeklyProgress = emptyList(),
            achievements = listOf(createTestMobileAchievement())
        )
    }

    private fun createTestMobileAchievement(): MobileAchievement {
        return MobileAchievement(
            achievementId = "achievement_123",
            title = "学习新手",
            description = "完成第一个学习活动",
            iconUrl = "https://example.com/icon.png",
            unlockedAt = Clock.System.now(),
            category = AchievementCategory.PROGRESS
        )
    }
}
