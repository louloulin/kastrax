package ai.kastrax.edutech.mobile

import ai.kastrax.core.actor.Message
import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant

/**
 * 移动学习Actor消息定义
 */

// 输入消息
data class RegisterDevice(
    val device: MobileDevice
) : Message

data class StartMobileSession(
    val sessionId: String,
    val studentId: StudentId,
    val location: GeoLocation? = null,
    val batteryLevel: Int? = null
) : Message

data class RecordMobileActivity(
    val activity: MobileLearningActivity
) : Message

data class EndMobileSession(
    val sessionId: String
) : Message

data class SyncMobileData(
    val forceSync: Boolean = false
) : Message

data class UpdatePreferences(
    val preferences: MobileLearningPreferences
) : Message

data class GetMobileStats(
    val studentId: StudentId,
    val timeRange: Pair<Instant, Instant>? = null
) : Message

data class DownloadOfflineContent(
    val packageId: String,
    val priority: DownloadPriority = DownloadPriority.NORMAL
) : Message

data class CheckConnectivity(
    val requestId: String = ""
) : Message

data class DownloadProgress(
    val packageId: String,
    val progress: Float
) : Message

enum class DownloadPriority {
    HIGH,
    NORMAL,
    LOW
}

// 响应消息
data class DeviceRegistered(
    val deviceId: String,
    val success: Boolean,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class MobileSessionStarted(
    val sessionId: String,
    val success: Boolean,
    val session: MobileLearningSession? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class MobileActivityRecorded(
    val activityId: String,
    val success: Boolean,
    val activity: MobileLearningActivity? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class MobileSessionEnded(
    val sessionId: String,
    val success: Boolean,
    val session: MobileLearningSession? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class MobileDataSynced(
    val deviceId: String,
    val success: Boolean,
    val syncResponse: SyncResponse? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class PreferencesUpdated(
    val studentId: StudentId,
    val success: Boolean,
    val preferences: MobileLearningPreferences? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class MobileStatsResponse(
    val studentId: StudentId,
    val success: Boolean,
    val stats: MobileLearningStats? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class OfflineContentDownloaded(
    val packageId: String,
    val success: Boolean,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class ConnectivityStatus(
    val deviceId: String,
    val isOnline: Boolean,
    val networkCondition: NetworkCondition,
    val lastSyncTime: Instant? = null,
    val pendingSyncItems: Int = 0,
    val timestamp: Instant
) : Message

// 通知消息
data class LowBatteryWarning(
    val deviceId: String,
    val batteryLevel: Int,
    val timestamp: Instant
) : Message

data class StorageSpaceWarning(
    val deviceId: String,
    val availableSpace: Long,
    val requiredSpace: Long,
    val timestamp: Instant
) : Message

data class SyncFailureNotification(
    val deviceId: String,
    val failedItems: Int,
    val lastAttempt: Instant,
    val nextRetry: Instant,
    val timestamp: Instant
) : Message

data class OfflineContentExpired(
    val packageId: String,
    val title: String,
    val expiryDate: Instant,
    val timestamp: Instant
) : Message

data class LearningStreakAchievement(
    val studentId: StudentId,
    val streakDays: Int,
    val achievement: MobileAchievement,
    val timestamp: Instant
) : Message

// 系统消息
data class NetworkStateChanged(
    val deviceId: String,
    val previousState: NetworkCondition,
    val currentState: NetworkCondition,
    val timestamp: Instant
) : Message

data class BatteryLevelChanged(
    val deviceId: String,
    val previousLevel: Int,
    val currentLevel: Int,
    val timestamp: Instant
) : Message

data class AppStateChanged(
    val deviceId: String,
    val state: AppState,
    val timestamp: Instant
) : Message

enum class AppState {
    FOREGROUND,
    BACKGROUND,
    SUSPENDED,
    TERMINATED
}

// 错误消息
data class MobileError(
    val errorCode: String,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Instant
) : Message

// 批量操作消息
data class BatchSyncRequest(
    val deviceId: String,
    val items: List<SyncItem>,
    val priority: SyncPriority = SyncPriority.NORMAL
) : Message

data class BatchSyncResponse(
    val deviceId: String,
    val success: Boolean,
    val processedItems: Int,
    val failedItems: Int,
    val errors: List<String> = emptyList(),
    val timestamp: Instant
) : Message

// 内容管理消息
data class RequestOfflinePackages(
    val studentId: StudentId,
    val subject: Subject? = null,
    val maxSize: Long? = null
) : Message

data class OfflinePackagesResponse(
    val studentId: StudentId,
    val packages: List<OfflineLearningPackage>,
    val totalSize: Long,
    val timestamp: Instant
) : Message

data class UpdateContentProgress(
    val contentId: String,
    val progress: Float,
    val completed: Boolean = false
) : Message

data class ContentProgressUpdated(
    val contentId: String,
    val success: Boolean,
    val newProgress: Float,
    val error: String? = null,
    val timestamp: Instant
) : Message

// 分析和报告消息
data class RequestLearningInsights(
    val studentId: StudentId,
    val timeRange: Pair<Instant, Instant>? = null,
    val includeRecommendations: Boolean = true
) : Message

data class LearningInsightsResponse(
    val studentId: StudentId,
    val insights: MobileLearningInsights,
    val recommendations: List<String> = emptyList(),
    val timestamp: Instant
) : Message

data class MobileLearningInsights(
    val totalLearningTime: kotlin.time.Duration,
    val averageDailyTime: kotlin.time.Duration,
    val mostActiveHours: List<Int>,
    val preferredContentTypes: List<MobileContentType>,
    val completionRate: Float,
    val streakDays: Int,
    val improvementAreas: List<String>,
    val strengths: List<String>
)
