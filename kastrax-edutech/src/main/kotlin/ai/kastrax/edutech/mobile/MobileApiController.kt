package ai.kastrax.edutech.mobile

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 移动学习API控制器
 * 提供移动端学习功能的REST API接口
 */
class MobileApiController(
    private val mobileLearningService: MobileLearningService
) {

    /**
     * 注册移动设备
     * POST /api/mobile/devices/register
     */
    suspend fun registerDevice(request: RegisterDeviceRequest): MobileApiResponse<MobileDevice> {
        val device = MobileDevice(
            deviceId = request.deviceId,
            deviceType = request.deviceType,
            osVersion = request.osVersion,
            appVersion = request.appVersion,
            screenSize = request.screenSize,
            capabilities = request.capabilities,
            lastSyncTime = Clock.System.now(),
            isOnline = true
        )

        return mobileLearningService.registerDevice(device)
    }

    /**
     * 创建学习会话
     * POST /api/mobile/sessions
     */
    suspend fun createSession(request: CreateMobileSessionRequest): MobileApiResponse<MobileLearningSession> {
        return mobileLearningService.createLearningSession(
            studentId = request.studentId,
            deviceId = request.deviceId,
            location = request.location
        )
    }

    /**
     * 记录学习活动
     * POST /api/mobile/sessions/{sessionId}/activities
     */
    suspend fun recordActivity(
        sessionId: String,
        request: RecordActivityRequest
    ): MobileApiResponse<MobileLearningActivity> {
        return mobileLearningService.recordLearningActivity(sessionId, request.activity)
    }

    /**
     * 结束学习会话
     * PUT /api/mobile/sessions/{sessionId}/end
     */
    suspend fun endSession(sessionId: String): MobileApiResponse<MobileLearningSession> {
        return mobileLearningService.endLearningSession(sessionId)
    }

    /**
     * 获取个性化内容
     * GET /api/mobile/content/personalized
     */
    suspend fun getPersonalizedContent(
        studentId: String,
        deviceId: String,
        contentType: String? = null,
        subject: String? = null
    ): MobileApiResponse<List<MobileContent>> {
        val preferences = getDefaultPreferences(StudentId(studentId))
        return mobileLearningService.getPersonalizedContent(
            studentId = StudentId(studentId),
            deviceId = deviceId,
            preferences = preferences
        )
    }

    /**
     * 获取离线学习包
     * GET /api/mobile/offline/packages
     */
    suspend fun getOfflinePackages(
        studentId: String,
        subject: String? = null
    ): MobileApiResponse<List<OfflineLearningPackage>> {
        val subjectEnum = subject?.let { Subject.valueOf(it.uppercase()) }
        return mobileLearningService.getOfflineLearningPackages(
            studentId = StudentId(studentId),
            subject = subjectEnum
        )
    }

    /**
     * 获取学习统计
     * GET /api/mobile/stats
     */
    suspend fun getLearningStats(
        studentId: String,
        deviceId: String,
        startDate: String? = null,
        endDate: String? = null
    ): MobileApiResponse<MobileLearningStats> {
        val timeRange = if (startDate != null && endDate != null) {
            Pair(Instant.parse(startDate), Instant.parse(endDate))
        } else null

        return mobileLearningService.getLearningStats(
            studentId = StudentId(studentId),
            deviceId = deviceId,
            timeRange = timeRange
        )
    }

    /**
     * 同步离线数据
     * POST /api/mobile/sync
     */
    suspend fun syncOfflineData(request: SyncDataRequest): MobileApiResponse<SyncResponse> {
        // 这里应该调用同步服务
        val syncResponse = SyncResponse(
            syncedActivities = request.activities.size,
            failedActivities = 0,
            lastSyncTime = Clock.System.now()
        )

        return MobileApiResponse(
            success = true,
            data = syncResponse,
            timestamp = Clock.System.now(),
            requestId = generateRequestId()
        )
    }

    /**
     * 更新学习偏好
     * PUT /api/mobile/preferences
     */
    suspend fun updatePreferences(request: UpdatePreferencesRequest): MobileApiResponse<MobileLearningPreferences> {
        // 这里应该保存偏好设置
        return MobileApiResponse(
            success = true,
            data = request.preferences,
            timestamp = Clock.System.now(),
            requestId = generateRequestId()
        )
    }

    /**
     * 检查设备连接状态
     * GET /api/mobile/devices/{deviceId}/status
     */
    suspend fun getDeviceStatus(deviceId: String): MobileApiResponse<DeviceStatus> {
        val status = DeviceStatus(
            deviceId = deviceId,
            isOnline = true,
            networkCondition = NetworkCondition.WIFI,
            batteryLevel = 85,
            lastSyncTime = Clock.System.now(),
            pendingSyncItems = 0
        )

        return MobileApiResponse(
            success = true,
            data = status,
            timestamp = Clock.System.now(),
            requestId = generateRequestId()
        )
    }

    /**
     * 下载离线内容
     * POST /api/mobile/offline/download
     */
    suspend fun downloadOfflineContent(request: DownloadContentRequest): MobileApiResponse<DownloadStatus> {
        // 模拟下载过程
        val downloadStatus = DownloadStatus.DOWNLOADED

        return MobileApiResponse(
            success = true,
            data = downloadStatus,
            timestamp = Clock.System.now(),
            requestId = generateRequestId()
        )
    }

    // 辅助方法
    private fun getDefaultPreferences(studentId: StudentId): MobileLearningPreferences {
        return MobileLearningPreferences(
            studentId = studentId,
            preferredContentTypes = setOf(
                MobileContentType.VIDEO,
                MobileContentType.INTERACTIVE_MEDIA,
                MobileContentType.TEXT
            ),
            autoDownloadEnabled = true,
            wifiOnlyDownload = true,
            maxStorageUsage = 1_000_000_000L, // 1GB
            notificationSettings = NotificationSettings(),
            accessibilitySettings = AccessibilitySettings()
        )
    }

    private fun generateRequestId(): String = "req_${Clock.System.now().toEpochMilliseconds()}"
}

// API请求/响应模型
@Serializable
data class RegisterDeviceRequest(
    val deviceId: String,
    val deviceType: DeviceType,
    val osVersion: String,
    val appVersion: String,
    val screenSize: ScreenSize,
    val capabilities: DeviceCapabilities
)

@Serializable
data class CreateMobileSessionRequest(
    val studentId: StudentId,
    val deviceId: String,
    val location: GeoLocation? = null
)

@Serializable
data class RecordActivityRequest(
    val activity: MobileLearningActivity
)

@Serializable
data class SyncDataRequest(
    val deviceId: String,
    val activities: List<MobileLearningActivity>,
    val sessions: List<MobileLearningSession> = emptyList()
)

@Serializable
data class UpdatePreferencesRequest(
    val preferences: MobileLearningPreferences
)

@Serializable
data class DownloadContentRequest(
    val packageId: String,
    val priority: String = "normal"
)

@Serializable
data class DeviceStatus(
    val deviceId: String,
    val isOnline: Boolean,
    val networkCondition: NetworkCondition,
    val batteryLevel: Int,
    val lastSyncTime: Instant,
    val pendingSyncItems: Int
)
