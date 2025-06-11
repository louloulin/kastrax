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
 * 简化的移动端学习应用测试
 * 专注于数据模型和基本功能验证
 */
class SimpleMobileLearningTest {

    @BeforeEach
    fun setUp() {
        // 简化设置
    }

    @Test
    fun `SML-001 - 移动设备信息模型测试`() = runTest {
        // Given
        val deviceInfo = createTestDeviceInfo()

        // Then
        assertNotNull(deviceInfo.deviceId)
        assertEquals(DeviceType.ANDROID_PHONE, deviceInfo.deviceType)
        assertEquals("Android 12", deviceInfo.osVersion)
        assertEquals("1.0.0", deviceInfo.appVersion)
        assertTrue(deviceInfo.capabilities.hasCamera)
        assertTrue(deviceInfo.capabilities.hasMicrophone)
        assertTrue(deviceInfo.capabilities.hasGPS)
        assertTrue(deviceInfo.capabilities.supportsBiometric)
        assertTrue(deviceInfo.capabilities.supportsOfflineMode)
        assertTrue(deviceInfo.isOnline)
        assertEquals(1_000_000_000L, deviceInfo.capabilities.maxStorageSize)
    }

    @Test
    fun `SML-002 - 移动内容模型测试`() = runTest {
        // Given
        val mobileContent = createTestMobileContent()

        // Then
        assertNotNull(mobileContent.contentId)
        assertEquals("测试移动内容", mobileContent.title)
        assertEquals("这是一个测试移动内容", mobileContent.description)
        assertEquals(MobileContentType.INTERACTIVE_MEDIA, mobileContent.contentType)
        assertEquals(15.minutes, mobileContent.duration)
        assertEquals(5_000_000L, mobileContent.fileSize)
        assertFalse(mobileContent.isDownloaded)
        assertEquals(0f, mobileContent.downloadProgress)
        assertNull(mobileContent.localPath)
    }

    @Test
    fun `SML-003 - 移动学习活动模型测试`() = runTest {
        // Given
        val activity = createTestMobileActivity()

        // Then
        assertNotNull(activity.activityId)
        assertEquals(MobileActivityType.INTERACTIVE_QUIZ, activity.type)
        assertNotNull(activity.content)
        assertEquals(0.0f, activity.progress.percentage)
        assertEquals(0, activity.progress.currentStep)
        assertEquals(10, activity.progress.totalSteps)
        assertEquals(kotlin.time.Duration.ZERO, activity.progress.timeSpent)
        assertFalse(activity.isCompleted)
        assertEquals(SyncStatus.PENDING, activity.syncStatus)
    }

    @Test
    fun `SML-004 - 离线学习包模型测试`() = runTest {
        // Given
        val offlinePackage = createTestOfflinePackage()

        // Then
        assertNotNull(offlinePackage.packageId)
        assertEquals("数学基础离线包", offlinePackage.title)
        assertEquals("包含基础数学概念的离线学习内容", offlinePackage.description)
        assertEquals(Subject.MATHEMATICS, offlinePackage.subject)
        assertEquals(GradeLevel.GRADE_8, offlinePackage.gradeLevel)
        assertEquals(2.hours, offlinePackage.estimatedDuration)
        assertEquals(25_000_000L, offlinePackage.totalSize)
        assertEquals(DownloadStatus.NOT_DOWNLOADED, offlinePackage.downloadStatus)
        assertTrue(offlinePackage.contents.isEmpty())
        assertTrue(offlinePackage.activities.isEmpty())
    }

    @Test
    fun `SML-005 - 移动学习偏好模型测试`() = runTest {
        // Given
        val preferences = createTestPreferences(StudentId("student_123"))

        // Then
        assertEquals(StudentId("student_123"), preferences.studentId)
        assertTrue(preferences.preferredContentTypes.contains(MobileContentType.VIDEO))
        assertTrue(preferences.preferredContentTypes.contains(MobileContentType.INTERACTIVE_MEDIA))
        assertTrue(preferences.autoDownloadEnabled)
        assertTrue(preferences.wifiOnlyDownload)
        assertEquals(500_000_000L, preferences.maxStorageUsage)
        
        // 验证通知设置
        assertTrue(preferences.notificationSettings.enablePushNotifications)
        assertTrue(preferences.notificationSettings.studyReminders)
        assertTrue(preferences.notificationSettings.achievementNotifications)
        assertTrue(preferences.notificationSettings.progressUpdates)
        
        // 验证无障碍设置
        assertEquals(FontSize.MEDIUM, preferences.accessibilitySettings.fontSize)
        assertFalse(preferences.accessibilitySettings.highContrast)
        assertFalse(preferences.accessibilitySettings.voiceOverEnabled)
        assertFalse(preferences.accessibilitySettings.subtitlesEnabled)
        assertFalse(preferences.accessibilitySettings.reducedMotion)
    }

    @Test
    fun `SML-006 - 移动学习统计模型测试`() = runTest {
        // Given
        val stats = createTestMobileLearningStats()

        // Then
        assertEquals(StudentId("student_123"), stats.studentId)
        assertEquals("device_123", stats.deviceId)
        assertEquals(2.hours, stats.totalSessionTime)
        assertEquals(15, stats.activitiesCompleted)
        assertEquals(30.minutes, stats.averageSessionDuration)
        assertEquals(MobileContentType.VIDEO, stats.mostUsedContentType)
        assertEquals(0.3f, stats.offlineUsagePercentage)
        assertTrue(stats.offlineUsagePercentage >= 0.0f && stats.offlineUsagePercentage <= 1.0f)
        assertNotNull(stats.dailyUsagePattern)
        assertTrue(stats.weeklyProgress.isEmpty())
        assertEquals(1, stats.achievements.size)
    }

    @Test
    fun `SML-007 - 移动成就模型测试`() = runTest {
        // Given
        val achievement = createTestMobileAchievement()

        // Then
        assertNotNull(achievement.achievementId)
        assertEquals("学习新手", achievement.title)
        assertEquals("完成第一个学习活动", achievement.description)
        assertEquals("https://example.com/icon.png", achievement.iconUrl)
        assertEquals(AchievementCategory.PROGRESS, achievement.category)
        assertNotNull(achievement.unlockedAt)
        assertTrue(achievement.unlockedAt <= Clock.System.now())
    }

    @Test
    fun `SML-008 - 移动交互模型测试`() = runTest {
        // Given
        val interaction = createTestMobileInteraction()

        // Then
        assertNotNull(interaction.interactionId)
        assertEquals(MobileInteractionType.TAP, interaction.type)
        assertNotNull(interaction.timestamp)
        assertNotNull(interaction.data)
        assertEquals(5.minutes, interaction.duration)
        assertEquals(0.85f, interaction.accuracy)
    }

    @Test
    fun `SML-009 - 设备兼容性测试`() = runTest {
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
            assertTrue(deviceInfo.capabilities.maxStorageSize > 0)
            
            // 验证不同设备类型的屏幕尺寸适配
            when (deviceType) {
                DeviceType.ANDROID_PHONE, DeviceType.IOS_PHONE -> {
                    // 手机设备通常有较小的屏幕
                    assertTrue(deviceInfo.screenSize.width <= 1200)
                }
                DeviceType.ANDROID_TABLET, DeviceType.IOS_TABLET -> {
                    // 平板设备可以有更大的屏幕
                    assertTrue(deviceInfo.screenSize.width >= 800)
                }
            }
        }
    }

    @Test
    fun `SML-010 - 离线模式功能测试`() = runTest {
        // Given
        val onlineDevice = createTestDeviceInfo().copy(isOnline = true)
        val offlineDevice = createTestDeviceInfo().copy(isOnline = false)
        val offlineActivity = createTestMobileActivity().copy(
            type = MobileActivityType.OFFLINE_PRACTICE,
            syncStatus = SyncStatus.OFFLINE_ONLY
        )

        // Then
        assertTrue(onlineDevice.isOnline)
        assertFalse(offlineDevice.isOnline)
        assertEquals(MobileActivityType.OFFLINE_PRACTICE, offlineActivity.type)
        assertEquals(SyncStatus.OFFLINE_ONLY, offlineActivity.syncStatus)
        
        // 验证离线设备仍然支持离线功能
        assertTrue(offlineDevice.capabilities.supportsOfflineMode)
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

    private fun createTestMobileInteraction(): MobileInteraction {
        return MobileInteraction(
            interactionId = "interaction_123",
            type = MobileInteractionType.TAP,
            timestamp = Clock.System.now(),
            data = """{"x": 100, "y": 200, "target": "button"}""",
            duration = 5.minutes,
            accuracy = 0.85f
        )
    }
}
