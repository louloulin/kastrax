package ai.kastrax.codebase.indexing.distributed

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 索引任务 Actor 配置
 *
 * @property taskTimeout 任务超时时间
 * @property maxRetries 最大重试次数
 */
data class IndexTaskActorConfig(
    val taskTimeout: Duration = 60.seconds,
    val maxRetries: Int = 3
)
