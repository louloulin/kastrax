package ai.kastrax.codebase.indexing.distributed

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 分布式索引系统配置
 *
 * @property localWorkerCount 本地工作器数量
 * @property taskSubmitTimeout 任务提交超时时间
 * @property coordinatorConfig 协调器配置
 * @property workerConfig 工作器配置
 * @property shardManagerConfig 分片管理器配置
 */
data class DistributedIndexSystemConfig(
    val localWorkerCount: Int = 2,
    val taskSubmitTimeout: Duration = 30.seconds,
    val coordinatorConfig: IndexCoordinatorConfig = IndexCoordinatorConfig(),
    val workerConfig: IndexWorkerConfig = IndexWorkerConfig(),
    val shardManagerConfig: IndexShardManagerConfig = IndexShardManagerConfig()
)

/**
 * 索引协调器配置
 *
 * @property initialWorkerCount 初始工作器数量
 * @property maxPendingTasks 最大待处理任务数
 * @property taskAssignmentInterval 任务分配间隔
 * @property workerStatusCheckInterval 工作器状态检查间隔
 */
data class IndexCoordinatorConfig(
    val initialWorkerCount: Int = 2,
    val maxPendingTasks: Int = 1000,
    val taskAssignmentInterval: Duration = 1.seconds,
    val workerStatusCheckInterval: Duration = 5.seconds
)

/**
 * 索引工作器配置
 *
 * @property capacity 容量
 * @property taskTimeout 任务超时时间
 */
data class IndexWorkerConfig(
    val capacity: Int = 5,
    val taskTimeout: Duration = 60.seconds
)

/**
 * 索引分片管理器配置
 *
 * @property shardCount 分片数量
 * @property replicaCount 副本数量
 */
data class IndexShardManagerConfig(
    val shardCount: Int = 3,
    val replicaCount: Int = 1
)
