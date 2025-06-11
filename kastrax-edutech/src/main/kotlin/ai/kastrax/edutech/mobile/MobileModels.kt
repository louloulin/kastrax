package ai.kastrax.edutech.mobile

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 移动端学习应用数据模型
 */

/**
 * 移动设备信息
 */
@Serializable
data class MobileDevice(
    val deviceId: String,
    val deviceType: DeviceType,
    val osVersion: String,
    val appVersion: String,
    val screenSize: ScreenSize,
    val capabilities: DeviceCapabilities,
    val lastSyncTime: Instant,
    val isOnline: Boolean = true
)

@Serializable
enum class DeviceType {
    ANDROID_PHONE,
    ANDROID_TABLET,
    IOS_PHONE,
    IOS_TABLET
}

@Serializable
data class ScreenSize(
    val width: Int,
    val height: Int,
    val density: Float
)

@Serializable
data class DeviceCapabilities(
    val hasCamera: Boolean,
    val hasMicrophone: Boolean,
    val hasGPS: Boolean,
    val supportsBiometric: Boolean,
    val supportsOfflineMode: Boolean,
    val maxStorageSize: Long // in bytes
)

/**
 * 移动学习会话
 */
@Serializable
data class MobileLearningSession(
    val sessionId: String,
    val studentId: StudentId,
    val deviceId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val activities: List<MobileLearningActivity> = emptyList(),
    val location: GeoLocation? = null,
    val networkCondition: NetworkCondition,
    val batteryLevel: Int? = null,
    val isOfflineSession: Boolean = false
)

@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Instant
)

@Serializable
enum class NetworkCondition {
    WIFI,
    CELLULAR_4G,
    CELLULAR_3G,
    CELLULAR_2G,
    OFFLINE
}

/**
 * 移动学习活动
 */
@Serializable
data class MobileLearningActivity(
    val activityId: String,
    val type: MobileActivityType,
    val content: MobileContent,
    val startTime: Instant,
    val endTime: Instant? = null,
    val interactions: List<MobileInteraction> = emptyList(),
    val progress: ActivityProgress,
    val isCompleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Serializable
enum class MobileActivityType {
    VIDEO_WATCHING,
    INTERACTIVE_QUIZ,
    READING_COMPREHENSION,
    AUDIO_LISTENING,
    VOICE_RECORDING,
    PHOTO_SUBMISSION,
    DRAWING_EXERCISE,
    GESTURE_LEARNING,
    AR_EXPERIENCE,
    OFFLINE_PRACTICE
}

@Serializable
data class MobileContent(
    val contentId: String,
    val title: String,
    val description: String,
    val contentType: MobileContentType,
    val mediaUrl: String? = null,
    val localPath: String? = null, // for offline content
    val duration: kotlin.time.Duration? = null,
    val fileSize: Long? = null,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f
)

@Serializable
enum class MobileContentType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    INTERACTIVE_MEDIA,
    AR_CONTENT,
    DOCUMENT
}

@Serializable
data class ActivityProgress(
    val percentage: Float,
    val currentStep: Int,
    val totalSteps: Int,
    val timeSpent: kotlin.time.Duration,
    val lastUpdateTime: Instant
)

@Serializable
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    OFFLINE_ONLY
}

/**
 * 移动交互
 */
@Serializable
data class MobileInteraction(
    val interactionId: String,
    val type: MobileInteractionType,
    val timestamp: Instant,
    val data: String, // JSON encoded interaction data
    val duration: kotlin.time.Duration? = null,
    val accuracy: Float? = null
)

@Serializable
enum class MobileInteractionType {
    TAP,
    SWIPE,
    PINCH_ZOOM,
    VOICE_INPUT,
    TEXT_INPUT,
    CAMERA_CAPTURE,
    GESTURE_RECOGNITION,
    BIOMETRIC_AUTH,
    SHAKE_GESTURE,
    TILT_GESTURE
}

/**
 * 离线学习包
 */
@Serializable
data class OfflineLearningPackage(
    val packageId: String,
    val title: String,
    val description: String,
    val subject: Subject,
    val gradeLevel: GradeLevel,
    val estimatedDuration: kotlin.time.Duration,
    val contents: List<MobileContent>,
    val activities: List<MobileLearningActivity>,
    val totalSize: Long,
    val downloadStatus: DownloadStatus,
    val expiryDate: Instant? = null,
    val lastAccessTime: Instant? = null
)

@Serializable
enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    EXPIRED,
    CORRUPTED
}

/**
 * 移动学习偏好
 */
@Serializable
data class MobileLearningPreferences(
    val studentId: StudentId,
    val preferredContentTypes: Set<MobileContentType>,
    val autoDownloadEnabled: Boolean = true,
    val wifiOnlyDownload: Boolean = true,
    val maxStorageUsage: Long, // in bytes
    val notificationSettings: NotificationSettings,
    val accessibilitySettings: AccessibilitySettings,
    val parentalControls: ParentalControls? = null
)

@Serializable
data class NotificationSettings(
    val enablePushNotifications: Boolean = true,
    val studyReminders: Boolean = true,
    val achievementNotifications: Boolean = true,
    val progressUpdates: Boolean = true,
    val quietHours: TimeRange? = null
)

@Serializable
data class TimeRange(
    val startHour: Int, // 0-23
    val startMinute: Int, // 0-59
    val endHour: Int,
    val endMinute: Int
)

@Serializable
data class AccessibilitySettings(
    val fontSize: FontSize = FontSize.MEDIUM,
    val highContrast: Boolean = false,
    val voiceOverEnabled: Boolean = false,
    val subtitlesEnabled: Boolean = false,
    val reducedMotion: Boolean = false
)

@Serializable
enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

@Serializable
data class ParentalControls(
    val isEnabled: Boolean = false,
    val allowedUsageHours: List<TimeRange> = emptyList(),
    val maxDailyUsage: kotlin.time.Duration? = null,
    val blockedContentTypes: Set<MobileContentType> = emptySet(),
    val requireApprovalForDownloads: Boolean = false
)

/**
 * 移动学习统计
 */
@Serializable
data class MobileLearningStats(
    val studentId: StudentId,
    val deviceId: String,
    val totalSessionTime: kotlin.time.Duration,
    val activitiesCompleted: Int,
    val averageSessionDuration: kotlin.time.Duration,
    val mostUsedContentType: MobileContentType,
    val offlineUsagePercentage: Float,
    val dailyUsagePattern: Map<Int, kotlin.time.Duration>, // hour -> duration
    val weeklyProgress: List<DailyProgress>,
    val achievements: List<MobileAchievement>
)

@Serializable
data class DailyProgress(
    val date: String, // YYYY-MM-DD format
    val sessionTime: kotlin.time.Duration,
    val activitiesCompleted: Int,
    val topicsStudied: List<String>
)

@Serializable
data class MobileAchievement(
    val achievementId: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val unlockedAt: Instant,
    val category: AchievementCategory
)

@Serializable
enum class AchievementCategory {
    CONSISTENCY,
    PROGRESS,
    MASTERY,
    EXPLORATION,
    COLLABORATION,
    CREATIVITY
}

/**
 * 移动API响应模型
 */
@Serializable
data class MobileApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Instant,
    val requestId: String
)

@Serializable
data class SyncResponse(
    val syncedActivities: Int,
    val failedActivities: Int,
    val lastSyncTime: Instant,
    val nextSyncTime: Instant? = null
)
