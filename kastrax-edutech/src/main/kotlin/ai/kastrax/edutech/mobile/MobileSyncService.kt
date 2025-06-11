package ai.kastrax.edutech.mobile

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 移动端数据同步服务
 * 处理在线/离线数据同步，确保数据一致性
 */
class MobileSyncService {
    private val pendingSyncItems = mutableListOf<SyncItem>()
    private val syncQueue = mutableMapOf<String, MutableList<SyncItem>>()
    private var isSyncing = false

    /**
     * 同步移动学习活动
     */
    suspend fun syncActivity(activity: MobileLearningActivity): SyncResult {
        return try {
            val syncItem = SyncItem(
                id = generateSyncId(),
                type = SyncItemType.ACTIVITY,
                data = activity,
                timestamp = Clock.System.now(),
                retryCount = 0
            )

            addToSyncQueue(syncItem)
            processSyncQueue()

            SyncResult.Success(syncItem.id)
        } catch (e: Exception) {
            SyncResult.Error("Failed to sync activity: ${e.message}")
        }
    }

    /**
     * 同步学习会话
     */
    suspend fun syncSession(session: MobileLearningSession): SyncResult {
        return try {
            val syncItem = SyncItem(
                id = generateSyncId(),
                type = SyncItemType.SESSION,
                data = session,
                timestamp = Clock.System.now(),
                retryCount = 0
            )

            addToSyncQueue(syncItem)
            processSyncQueue()

            SyncResult.Success(syncItem.id)
        } catch (e: Exception) {
            SyncResult.Error("Failed to sync session: ${e.message}")
        }
    }

    /**
     * 同步学习进度
     */
    suspend fun syncProgress(progress: LearningProgress): SyncResult {
        return try {
            val syncItem = SyncItem(
                id = generateSyncId(),
                type = SyncItemType.PROGRESS,
                data = progress,
                timestamp = Clock.System.now(),
                retryCount = 0
            )

            addToSyncQueue(syncItem)
            processSyncQueue()

            SyncResult.Success(syncItem.id)
        } catch (e: Exception) {
            SyncResult.Error("Failed to sync progress: ${e.message}")
        }
    }

    /**
     * 批量同步离线数据
     */
    suspend fun syncOfflineData(deviceId: String): SyncResponse {
        return try {
            val deviceSyncItems = syncQueue[deviceId] ?: emptyList()
            var syncedCount = 0
            var failedCount = 0

            for (item in deviceSyncItems) {
                when (syncSingleItem(item)) {
                    is SyncResult.Success -> {
                        syncedCount++
                        removeSyncItem(deviceId, item.id)
                    }
                    is SyncResult.Error -> {
                        failedCount++
                        item.retryCount++
                        if (item.retryCount >= MAX_RETRY_COUNT) {
                            removeSyncItem(deviceId, item.id)
                        }
                    }
                }
            }

            SyncResponse(
                syncedActivities = syncedCount,
                failedActivities = failedCount,
                lastSyncTime = Clock.System.now(),
                nextSyncTime = calculateNextSyncTime()
            )
        } catch (e: Exception) {
            SyncResponse(
                syncedActivities = 0,
                failedActivities = syncQueue[deviceId]?.size ?: 0,
                lastSyncTime = Clock.System.now(),
                nextSyncTime = calculateNextSyncTime()
            )
        }
    }

    /**
     * 下载离线内容
     */
    suspend fun downloadOfflineContent(
        packageId: String,
        onProgress: (Float) -> Unit = {}
    ): DownloadResult {
        return try {
            // 模拟下载过程
            for (progress in 0..100 step 10) {
                delay(100) // 模拟下载时间
                onProgress(progress / 100f)
            }

            DownloadResult.Success(packageId)
        } catch (e: Exception) {
            DownloadResult.Error("Failed to download content: ${e.message}")
        }
    }

    /**
     * 检查同步状态
     */
    fun getSyncStatus(deviceId: String): SyncStatus {
        val deviceItems = syncQueue[deviceId] ?: emptyList()
        return when {
            deviceItems.isEmpty() -> SyncStatus.SYNCED
            isSyncing -> SyncStatus.SYNCING
            else -> SyncStatus.PENDING
        }
    }

    /**
     * 获取待同步项目数量
     */
    fun getPendingSyncCount(deviceId: String): Int {
        return syncQueue[deviceId]?.size ?: 0
    }

    /**
     * 强制同步
     */
    suspend fun forcSync(deviceId: String): SyncResponse {
        isSyncing = true
        return try {
            syncOfflineData(deviceId)
        } finally {
            isSyncing = false
        }
    }

    // 私有方法
    private fun addToSyncQueue(item: SyncItem) {
        val deviceId = extractDeviceId(item)
        syncQueue.getOrPut(deviceId) { mutableListOf() }.add(item)
        pendingSyncItems.add(item)
    }

    private suspend fun processSyncQueue() {
        if (isSyncing) return

        isSyncing = true
        try {
            // 处理高优先级同步项目
            val highPriorityItems = pendingSyncItems.filter { it.priority == SyncPriority.HIGH }
            for (item in highPriorityItems) {
                syncSingleItem(item)
                pendingSyncItems.remove(item)
            }

            // 批量处理普通优先级项目
            val normalItems = pendingSyncItems.filter { it.priority == SyncPriority.NORMAL }
            if (normalItems.isNotEmpty()) {
                processBatchSync(normalItems.take(BATCH_SIZE))
            }
        } finally {
            isSyncing = false
        }
    }

    private suspend fun syncSingleItem(item: SyncItem): SyncResult {
        return try {
            when (item.type) {
                SyncItemType.ACTIVITY -> syncActivityToServer(item.data as MobileLearningActivity)
                SyncItemType.SESSION -> syncSessionToServer(item.data as MobileLearningSession)
                SyncItemType.PROGRESS -> syncProgressToServer(item.data as LearningProgress)
                SyncItemType.PREFERENCES -> syncPreferencesToServer(item.data as MobileLearningPreferences)
            }
            SyncResult.Success(item.id)
        } catch (e: Exception) {
            SyncResult.Error("Sync failed: ${e.message}")
        }
    }

    private suspend fun processBatchSync(items: List<SyncItem>) {
        // 模拟批量同步
        delay(1.seconds)
        items.forEach { item ->
            when (syncSingleItem(item)) {
                is SyncResult.Success -> pendingSyncItems.remove(item)
                is SyncResult.Error -> item.retryCount++
            }
        }
    }

    private suspend fun syncActivityToServer(activity: MobileLearningActivity) {
        // 模拟网络请求
        delay(100)
        // 实际实现中会调用服务器API
    }

    private suspend fun syncSessionToServer(session: MobileLearningSession) {
        // 模拟网络请求
        delay(100)
        // 实际实现中会调用服务器API
    }

    private suspend fun syncProgressToServer(progress: LearningProgress) {
        // 模拟网络请求
        delay(100)
        // 实际实现中会调用服务器API
    }

    private suspend fun syncPreferencesToServer(preferences: MobileLearningPreferences) {
        // 模拟网络请求
        delay(100)
        // 实际实现中会调用服务器API
    }

    private fun extractDeviceId(item: SyncItem): String {
        return when (val data = item.data) {
            is MobileLearningActivity -> "device_${data.activityId.take(8)}"
            is MobileLearningSession -> data.deviceId
            is MobileLearningPreferences -> "device_${data.studentId.value.take(8)}"
            else -> "unknown_device"
        }
    }

    private fun removeSyncItem(deviceId: String, itemId: String) {
        syncQueue[deviceId]?.removeIf { it.id == itemId }
        pendingSyncItems.removeIf { it.id == itemId }
    }

    private fun calculateNextSyncTime(): Instant {
        return Clock.System.now().plus(5.minutes)
    }

    private fun generateSyncId(): String = "sync_${Clock.System.now().toEpochMilliseconds()}"

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val BATCH_SIZE = 10
    }
}

/**
 * 同步项目数据类
 */
data class SyncItem(
    val id: String,
    val type: SyncItemType,
    val data: Any,
    val timestamp: Instant,
    var retryCount: Int = 0,
    val priority: SyncPriority = SyncPriority.NORMAL
)

enum class SyncItemType {
    ACTIVITY,
    SESSION,
    PROGRESS,
    PREFERENCES
}

enum class SyncPriority {
    HIGH,
    NORMAL,
    LOW
}

/**
 * 同步结果
 */
sealed class SyncResult {
    data class Success(val itemId: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * 下载结果
 */
sealed class DownloadResult {
    data class Success(val packageId: String) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}
