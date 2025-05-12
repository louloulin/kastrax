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
 * @property heartbeatInterval 心跳间隔
 * @property workerTimeoutDuration 工作器超时时间
 * @property taskAssignmentBatchSize 任务分配批次大小
 * @property maxRetries 最大重试次数
 * @property initialWorkerCount 初始工作者数量
 * @property taskAssignmentInterval 任务分配间隔
 * @property workerStatusCheckInterval 工作者状态检查间隔
 * @property maxTasksPerWorker 每个工作者的最大任务数
 */
data class IndexCoordinatorConfig(
    val heartbeatInterval: Duration = 10.seconds,
    val workerTimeoutDuration: Duration = 30.seconds,
    val taskAssignmentBatchSize: Int = 10,
    val maxRetries: Int = 3,
    val initialWorkerCount: Int = 4,
    val taskAssignmentInterval: Duration = 1.seconds,
    val workerStatusCheckInterval: Duration = 10.seconds,
    val maxTasksPerWorker: Int = 10
)

/**
 * 索引工作器配置
 *
 * @property capacity 容量
 * @property heartbeatInterval 心跳间隔
 * @property taskTimeout 任务超时时间
 * @property maxConcurrentTasks 最大并发任务数
 */
data class IndexWorkerConfig(
    val capacity: Int = 5,
    val heartbeatInterval: Duration = 5.seconds,
    val taskTimeout: Duration = 60.seconds,
    val maxConcurrentTasks: Int = 3
)

/**
 * 索引分片管理器配置
 *
 * @property shardCount 分片数量
 * @property replicaCount 副本数量
 * @property heartbeatInterval 心跳间隔
 * @property shardTimeoutDuration 分片超时时间
 * @property rebalanceInterval 重新平衡间隔
 */
data class IndexShardManagerConfig(
    val shardCount: Int = 10,
    val replicaCount: Int = 1,
    val heartbeatInterval: Duration = 10.seconds,
    val shardTimeoutDuration: Duration = 30.seconds,
    val rebalanceInterval: Duration = 60.seconds
)
