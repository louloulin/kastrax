package ai.kastrax.codebase.indexing.distributed

// TODO: 暂时注释掉Actor相关代码，等待kactor依赖问题解决

// 空实现以避免语法错误
class IndexCoordinatorConfig

/*
import kotlin.time.Duration

/**
 * 索引协调者配置
 *
 * @property initialWorkerCount 初始工作者数量
 * @property taskAssignmentInterval 任务分配间隔
 * @property workerStatusCheckInterval 工作者状态检查间隔
 * @property maxTasksPerWorker 每个工作者的最大任务数
 * @property maxRetries 最大重试次数
 * @property taskTimeout 任务超时时间
 */
data class IndexCoordinatorConfig(
    val initialWorkerCount: Int = 4,
    val taskAssignmentInterval: Duration,
    val workerStatusCheckInterval: Duration,
    val maxTasksPerWorker: Int = 10,
    val maxRetries: Int = 3,
    val taskTimeout: Duration = Duration.parse("5m")
)
*/
