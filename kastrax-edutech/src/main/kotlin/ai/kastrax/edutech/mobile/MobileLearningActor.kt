package ai.kastrax.edutech.mobile

import ai.kastrax.core.actor.Actor
import ai.kastrax.core.actor.ActorContext
import ai.kastrax.core.actor.Message
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.analytics.LearningAnalyticsService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * 移动学习Actor
 * 处理单个移动设备的学习会话和活动管理
 */
class MobileLearningActor(
    private val deviceId: String,
    private val analyticsService: LearningAnalyticsService,
    private val syncService: MobileSyncService
) : Actor {

    private var currentSession: MobileLearningSession? = null
    private var deviceInfo: MobileDevice? = null
    private var learningPreferences: MobileLearningPreferences? = null
    private val activityHistory = mutableListOf<MobileLearningActivity>()
    private var lastSyncTime: Instant? = null

    override suspend fun receive(message: Message, context: ActorContext) {
        when (message) {
            is RegisterDevice -> handleRegisterDevice(message, context)
            is StartMobileSession -> handleStartSession(message, context)
            is RecordMobileActivity -> handleRecordActivity(message, context)
            is EndMobileSession -> handleEndSession(message, context)
            is SyncMobileData -> handleSyncData(message, context)
            is UpdatePreferences -> handleUpdatePreferences(message, context)
            is GetMobileStats -> handleGetStats(message, context)
            is DownloadOfflineContent -> handleDownloadContent(message, context)
            is CheckConnectivity -> handleCheckConnectivity(message, context)
        }
    }

    private suspend fun handleRegisterDevice(message: RegisterDevice, context: ActorContext) {
        try {
            deviceInfo = message.device
            
            context.reply(DeviceRegistered(
                deviceId = deviceId,
                success = true,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(DeviceRegistered(
                deviceId = deviceId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleStartSession(message: StartMobileSession, context: ActorContext) {
        try {
            val device = deviceInfo ?: throw IllegalStateException("Device not registered")
            
            currentSession = MobileLearningSession(
                sessionId = message.sessionId,
                studentId = message.studentId,
                deviceId = deviceId,
                startTime = Clock.System.now(),
                location = message.location,
                networkCondition = determineNetworkCondition(device),
                batteryLevel = message.batteryLevel,
                isOfflineSession = !device.isOnline
            )

            context.reply(MobileSessionStarted(
                sessionId = message.sessionId,
                success = true,
                session = currentSession!!,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(MobileSessionStarted(
                sessionId = message.sessionId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleRecordActivity(message: RecordMobileActivity, context: ActorContext) {
        try {
            val session = currentSession ?: throw IllegalStateException("No active session")
            
            val activity = message.activity.copy(
                startTime = Clock.System.now(),
                syncStatus = if (session.isOfflineSession) SyncStatus.PENDING else SyncStatus.SYNCED
            )

            // 添加到活动历史
            activityHistory.add(activity)

            // 更新当前会话
            currentSession = session.copy(
                activities = session.activities + activity
            )

            // 如果在线，立即同步
            if (!session.isOfflineSession) {
                syncService.syncActivity(activity)
            }

            // 更新学习分析
            analyticsService.recordLearningActivity(
                studentId = session.studentId,
                activity = convertToLearningActivity(activity),
                context = mapOf(
                    "deviceType" to deviceId,
                    "networkCondition" to session.networkCondition.name,
                    "batteryLevel" to (session.batteryLevel?.toString() ?: "unknown")
                )
            )

            context.reply(MobileActivityRecorded(
                activityId = activity.activityId,
                success = true,
                activity = activity,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(MobileActivityRecorded(
                activityId = message.activity.activityId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleEndSession(message: EndMobileSession, context: ActorContext) {
        try {
            val session = currentSession ?: throw IllegalStateException("No active session")
            
            val endedSession = session.copy(
                endTime = Clock.System.now()
            )

            // 同步会话数据
            if (!session.isOfflineSession) {
                syncService.syncSession(endedSession)
            }

            // 清理当前会话
            currentSession = null

            context.reply(MobileSessionEnded(
                sessionId = message.sessionId,
                success = true,
                session = endedSession,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(MobileSessionEnded(
                sessionId = message.sessionId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleSyncData(message: SyncMobileData, context: ActorContext) {
        try {
            val syncResponse = syncService.syncOfflineData(deviceId)
            lastSyncTime = Clock.System.now()

            context.reply(MobileDataSynced(
                deviceId = deviceId,
                success = true,
                syncResponse = syncResponse,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(MobileDataSynced(
                deviceId = deviceId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleUpdatePreferences(message: UpdatePreferences, context: ActorContext) {
        try {
            learningPreferences = message.preferences

            // 同步偏好设置
            syncService.syncProgress(LearningProgress(
                studentId = message.preferences.studentId,
                subject = Subject.GENERAL,
                completedActivities = activityHistory.size,
                totalActivities = activityHistory.size,
                averageScore = calculateAverageScore(),
                timeSpent = calculateTotalTimeSpent(),
                lastUpdated = Clock.System.now()
            ))

            context.reply(PreferencesUpdated(
                studentId = message.preferences.studentId,
                success = true,
                preferences = message.preferences,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(PreferencesUpdated(
                studentId = message.preferences.studentId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleGetStats(message: GetMobileStats, context: ActorContext) {
        try {
            val stats = calculateMobileStats(message.studentId)

            context.reply(MobileStatsResponse(
                studentId = message.studentId,
                success = true,
                stats = stats,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(MobileStatsResponse(
                studentId = message.studentId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleDownloadContent(message: DownloadOfflineContent, context: ActorContext) {
        try {
            val result = syncService.downloadOfflineContent(message.packageId) { progress ->
                // 发送下载进度更新
                context.send(context.self, DownloadProgress(message.packageId, progress))
            }

            when (result) {
                is DownloadResult.Success -> {
                    context.reply(OfflineContentDownloaded(
                        packageId = message.packageId,
                        success = true,
                        timestamp = Clock.System.now()
                    ))
                }
                is DownloadResult.Error -> {
                    context.reply(OfflineContentDownloaded(
                        packageId = message.packageId,
                        success = false,
                        error = result.message,
                        timestamp = Clock.System.now()
                    ))
                }
            }
        } catch (e: Exception) {
            context.reply(OfflineContentDownloaded(
                packageId = message.packageId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleCheckConnectivity(message: CheckConnectivity, context: ActorContext) {
        val device = deviceInfo
        val isOnline = device?.isOnline ?: false
        val networkCondition = device?.let { determineNetworkCondition(it) } ?: NetworkCondition.OFFLINE

        context.reply(ConnectivityStatus(
            deviceId = deviceId,
            isOnline = isOnline,
            networkCondition = networkCondition,
            lastSyncTime = lastSyncTime,
            pendingSyncItems = syncService.getPendingSyncCount(deviceId),
            timestamp = Clock.System.now()
        ))
    }

    // 辅助方法
    private fun determineNetworkCondition(device: MobileDevice): NetworkCondition {
        return if (device.isOnline) NetworkCondition.WIFI else NetworkCondition.OFFLINE
    }

    private fun convertToLearningActivity(mobileActivity: MobileLearningActivity): LearningActivity {
        return LearningActivity(
            id = mobileActivity.activityId,
            type = when (mobileActivity.type) {
                MobileActivityType.VIDEO_WATCHING -> ActivityType.VIDEO_WATCHING
                MobileActivityType.INTERACTIVE_QUIZ -> ActivityType.QUIZ
                MobileActivityType.READING_COMPREHENSION -> ActivityType.READING
                MobileActivityType.AUDIO_LISTENING -> ActivityType.LISTENING
                else -> ActivityType.PRACTICE
            },
            contentId = mobileActivity.content.contentId,
            startTime = mobileActivity.startTime,
            endTime = mobileActivity.endTime,
            completed = mobileActivity.isCompleted,
            score = mobileActivity.progress.percentage.toDouble()
        )
    }

    private fun calculateAverageScore(): Double {
        return if (activityHistory.isEmpty()) 0.0
        else activityHistory.map { it.progress.percentage }.average().toDouble()
    }

    private fun calculateTotalTimeSpent(): kotlin.time.Duration {
        return activityHistory.sumOf { it.progress.timeSpent.inWholeMilliseconds }.let {
            kotlin.time.Duration.parse("${it}ms")
        }
    }

    private fun calculateMobileStats(studentId: StudentId): MobileLearningStats {
        return MobileLearningStats(
            studentId = studentId,
            deviceId = deviceId,
            totalSessionTime = calculateTotalTimeSpent(),
            activitiesCompleted = activityHistory.count { it.isCompleted },
            averageSessionDuration = if (activityHistory.isNotEmpty()) 
                calculateTotalTimeSpent() / activityHistory.size else 0.minutes,
            mostUsedContentType = activityHistory
                .groupBy { it.content.contentType }
                .maxByOrNull { it.value.size }?.key ?: MobileContentType.TEXT,
            offlineUsagePercentage = activityHistory.count { it.syncStatus == SyncStatus.PENDING }
                .toFloat() / activityHistory.size.coerceAtLeast(1),
            dailyUsagePattern = mapOf(
                9 to 30.minutes,
                14 to 45.minutes,
                19 to 60.minutes
            ),
            weeklyProgress = emptyList(),
            achievements = emptyList()
        )
    }
}
