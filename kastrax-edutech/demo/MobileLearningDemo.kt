package ai.kastrax.edutech.demo

import ai.kastrax.edutech.mobile.*
import ai.kastrax.edutech.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

/**
 * 移动学习功能演示
 * 展示移动端学习应用的核心功能
 */
fun main() = runBlocking {
    println("🎓 Kastrax移动学习功能演示")
    println("=" * 50)

    // 1. 创建移动设备
    val mobileDevice = createSampleMobileDevice()
    println("📱 创建移动设备: ${mobileDevice.deviceId}")
    println("   设备类型: ${mobileDevice.deviceType}")
    println("   操作系统: ${mobileDevice.osVersion}")
    println("   应用版本: ${mobileDevice.appVersion}")
    println("   屏幕尺寸: ${mobileDevice.screenSize.width}x${mobileDevice.screenSize.height}")
    println("   设备能力: 摄像头=${mobileDevice.capabilities.hasCamera}, 麦克风=${mobileDevice.capabilities.hasMicrophone}")
    println()

    // 2. 创建学习会话
    val learningSession = createSampleLearningSession(mobileDevice.deviceId)
    println("📚 创建学习会话: ${learningSession.sessionId}")
    println("   学生ID: ${learningSession.studentId}")
    println("   设备ID: ${learningSession.deviceId}")
    println("   网络状态: ${learningSession.networkCondition}")
    println("   离线模式: ${learningSession.isOfflineSession}")
    println()

    // 3. 创建学习活动
    val learningActivity = createSampleLearningActivity()
    println("🎯 创建学习活动: ${learningActivity.activityId}")
    println("   活动类型: ${learningActivity.type}")
    println("   内容标题: ${learningActivity.content.title}")
    println("   内容类型: ${learningActivity.content.contentType}")
    println("   学习进度: ${learningActivity.progress.percentage}%")
    println("   完成状态: ${if (learningActivity.isCompleted) "已完成" else "进行中"}")
    println()

    // 4. 创建离线学习包
    val offlinePackage = createSampleOfflinePackage()
    println("📦 创建离线学习包: ${offlinePackage.packageId}")
    println("   包标题: ${offlinePackage.title}")
    println("   学科: ${offlinePackage.subject}")
    println("   年级: ${offlinePackage.gradeLevel}")
    println("   预计时长: ${offlinePackage.estimatedDuration}")
    println("   包大小: ${formatFileSize(offlinePackage.totalSize)}")
    println("   下载状态: ${offlinePackage.downloadStatus}")
    println()

    // 5. 创建学习偏好
    val preferences = createSamplePreferences()
    println("⚙️ 创建学习偏好")
    println("   学生ID: ${preferences.studentId}")
    println("   偏好内容类型: ${preferences.preferredContentTypes.joinToString(", ")}")
    println("   自动下载: ${preferences.autoDownloadEnabled}")
    println("   仅WiFi下载: ${preferences.wifiOnlyDownload}")
    println("   最大存储: ${formatFileSize(preferences.maxStorageUsage)}")
    println("   字体大小: ${preferences.accessibilitySettings.fontSize}")
    println()

    // 6. 创建移动交互
    val interaction = createSampleInteraction()
    println("👆 创建移动交互: ${interaction.interactionId}")
    println("   交互类型: ${interaction.type}")
    println("   交互数据: ${interaction.data}")
    println("   持续时间: ${interaction.duration}")
    println("   准确度: ${interaction.accuracy}")
    println()

    // 7. 创建学习统计
    val stats = createSampleLearningStats()
    println("📊 创建学习统计")
    println("   学生ID: ${stats.studentId}")
    println("   设备ID: ${stats.deviceId}")
    println("   总学习时间: ${stats.totalSessionTime}")
    println("   完成活动数: ${stats.activitiesCompleted}")
    println("   平均会话时长: ${stats.averageSessionDuration}")
    println("   最常用内容类型: ${stats.mostUsedContentType}")
    println("   离线使用比例: ${(stats.offlineUsagePercentage * 100).toInt()}%")
    println("   成就数量: ${stats.achievements.size}")
    println()

    // 8. 创建同步响应
    val syncResponse = createSampleSyncResponse()
    println("🔄 创建同步响应")
    println("   同步成功: ${syncResponse.syncedActivities} 个活动")
    println("   同步失败: ${syncResponse.failedActivities} 个活动")
    println("   上次同步: ${syncResponse.lastSyncTime}")
    println("   下次同步: ${syncResponse.nextSyncTime}")
    println()

    // 9. 演示API响应
    val apiResponse = createSampleApiResponse()
    println("🌐 创建API响应")
    println("   请求成功: ${apiResponse.success}")
    println("   响应数据: ${apiResponse.data}")
    println("   请求ID: ${apiResponse.requestId}")
    println("   时间戳: ${apiResponse.timestamp}")
    println()

    // 10. 演示移动学习流程
    println("🎮 移动学习流程演示")
    println("-" * 30)
    
    // 模拟学习流程
    println("1. 学生打开移动应用")
    println("2. 系统检测设备能力和网络状态")
    println("3. 创建个性化学习会话")
    println("4. 推荐适合的学习内容")
    println("5. 学生开始学习活动")
    println("6. 实时记录学习进度和交互")
    println("7. 自动同步学习数据")
    println("8. 生成学习统计和成就")
    println("9. 提供个性化学习建议")
    println("10. 支持离线学习模式")
    println()

    println("✅ 移动学习功能演示完成！")
    println("🚀 Kastrax移动学习平台为学生提供了:")
    println("   • 跨平台移动学习支持")
    println("   • 智能内容个性化推荐")
    println("   • 离线学习能力")
    println("   • 实时学习数据同步")
    println("   • 多模态交互支持")
    println("   • 详细的学习分析")
    println("   • 成就系统激励")
    println("   • 无障碍功能支持")
}

// 辅助函数
private fun createSampleMobileDevice(): MobileDevice {
    return MobileDevice(
        deviceId = "mobile_demo_001",
        deviceType = DeviceType.ANDROID_PHONE,
        osVersion = "Android 13",
        appVersion = "1.0.0",
        screenSize = ScreenSize(1080, 2340, 3.0f),
        capabilities = DeviceCapabilities(
            hasCamera = true,
            hasMicrophone = true,
            hasGPS = true,
            supportsBiometric = true,
            supportsOfflineMode = true,
            maxStorageSize = 2_000_000_000L // 2GB
        ),
        lastSyncTime = Clock.System.now(),
        isOnline = true
    )
}

private fun createSampleLearningSession(deviceId: String): MobileLearningSession {
    return MobileLearningSession(
        sessionId = "session_demo_001",
        studentId = StudentId("student_demo_001"),
        deviceId = deviceId,
        startTime = Clock.System.now(),
        location = GeoLocation(
            latitude = 39.9042,
            longitude = 116.4074,
            accuracy = 10.0f,
            timestamp = Clock.System.now()
        ),
        networkCondition = NetworkCondition.WIFI,
        batteryLevel = 85,
        isOfflineSession = false
    )
}

private fun createSampleLearningActivity(): MobileLearningActivity {
    return MobileLearningActivity(
        activityId = "activity_demo_001",
        type = MobileActivityType.INTERACTIVE_QUIZ,
        content = MobileContent(
            contentId = "content_demo_001",
            title = "数学基础练习",
            description = "包含加减乘除的基础数学练习题",
            contentType = MobileContentType.INTERACTIVE_MEDIA,
            mediaUrl = "https://example.com/math-quiz.html",
            duration = 20.minutes,
            fileSize = 15_000_000L,
            isDownloaded = true,
            downloadProgress = 1.0f
        ),
        startTime = Clock.System.now(),
        progress = ActivityProgress(
            percentage = 75.0f,
            currentStep = 15,
            totalSteps = 20,
            timeSpent = 15.minutes,
            lastUpdateTime = Clock.System.now()
        ),
        isCompleted = false,
        syncStatus = SyncStatus.SYNCED
    )
}

private fun createSampleOfflinePackage(): OfflineLearningPackage {
    return OfflineLearningPackage(
        packageId = "package_demo_001",
        title = "小学数学基础包",
        description = "包含小学1-6年级数学基础知识和练习题",
        subject = Subject.MATHEMATICS,
        gradeLevel = GradeLevel.GRADE_3,
        estimatedDuration = 3.hours,
        contents = emptyList(),
        activities = emptyList(),
        totalSize = 150_000_000L, // 150MB
        downloadStatus = DownloadStatus.DOWNLOADED
    )
}

private fun createSamplePreferences(): MobileLearningPreferences {
    return MobileLearningPreferences(
        studentId = StudentId("student_demo_001"),
        preferredContentTypes = setOf(
            MobileContentType.VIDEO,
            MobileContentType.INTERACTIVE_MEDIA,
            MobileContentType.AUDIO
        ),
        autoDownloadEnabled = true,
        wifiOnlyDownload = true,
        maxStorageUsage = 1_000_000_000L, // 1GB
        notificationSettings = NotificationSettings(
            enablePushNotifications = true,
            studyReminders = true,
            achievementNotifications = true,
            progressUpdates = true,
            quietHours = TimeRange(22, 0, 7, 0)
        ),
        accessibilitySettings = AccessibilitySettings(
            fontSize = FontSize.LARGE,
            highContrast = false,
            voiceOverEnabled = false,
            subtitlesEnabled = true,
            reducedMotion = false
        )
    )
}

private fun createSampleInteraction(): MobileInteraction {
    return MobileInteraction(
        interactionId = "interaction_demo_001",
        type = MobileInteractionType.TAP,
        timestamp = Clock.System.now(),
        data = """{"x": 540, "y": 960, "target": "answer_button_2", "pressure": 0.8}""",
        duration = kotlin.time.Duration.parse("250ms"),
        accuracy = 0.95f
    )
}

private fun createSampleLearningStats(): MobileLearningStats {
    return MobileLearningStats(
        studentId = StudentId("student_demo_001"),
        deviceId = "mobile_demo_001",
        totalSessionTime = 8.hours,
        activitiesCompleted = 45,
        averageSessionDuration = 25.minutes,
        mostUsedContentType = MobileContentType.VIDEO,
        offlineUsagePercentage = 0.25f,
        dailyUsagePattern = mapOf(
            8 to 30.minutes,
            12 to 20.minutes,
            16 to 45.minutes,
            20 to 35.minutes
        ),
        weeklyProgress = listOf(
            DailyProgress("2024-12-19", 45.minutes, 3, listOf("数学", "语文")),
            DailyProgress("2024-12-20", 60.minutes, 4, listOf("英语", "科学"))
        ),
        achievements = listOf(
            MobileAchievement(
                achievementId = "achievement_demo_001",
                title = "学习达人",
                description = "连续学习7天",
                iconUrl = "/icons/streak_7days.png",
                unlockedAt = Clock.System.now(),
                category = AchievementCategory.CONSISTENCY
            ),
            MobileAchievement(
                achievementId = "achievement_demo_002",
                title = "数学小能手",
                description = "数学练习正确率达到90%",
                iconUrl = "/icons/math_master.png",
                unlockedAt = Clock.System.now(),
                category = AchievementCategory.MASTERY
            )
        )
    )
}

private fun createSampleSyncResponse(): SyncResponse {
    return SyncResponse(
        syncedActivities = 12,
        failedActivities = 1,
        lastSyncTime = Clock.System.now(),
        nextSyncTime = Clock.System.now().plus(10.minutes)
    )
}

private fun createSampleApiResponse(): MobileApiResponse<String> {
    return MobileApiResponse(
        success = true,
        data = "移动学习功能正常运行",
        error = null,
        timestamp = Clock.System.now(),
        requestId = "req_demo_001"
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000}GB"
        bytes >= 1_000_000 -> "${bytes / 1_000_000}MB"
        bytes >= 1_000 -> "${bytes / 1_000}KB"
        else -> "${bytes}B"
    }
}

private operator fun String.times(n: Int): String = this.repeat(n)
