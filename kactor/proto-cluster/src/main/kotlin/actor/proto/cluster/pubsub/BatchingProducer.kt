package actor.proto.cluster.pubsub

import actor.proto.PID
import actor.proto.cluster.Cluster
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * BatchingProducerConfig 表示批处理生产者的配置
 * @param maxBatchSize 最大批处理大小
 * @param flushInterval 刷新间隔
 * @param errorHandler 错误处理器
 */
data class BatchingProducerConfig(
    val maxBatchSize: Int = 100,
    val flushInterval: Duration = Duration.ofMillis(100),
    val errorHandler: PublishingErrorHandler? = null
)

/**
 * PublishingErrorDecision 表示发布错误时的决策
 */
sealed class PublishingErrorDecision {
    /**
     * 失败批处理并停止
     */
    object FailBatchAndStop : PublishingErrorDecision()

    /**
     * 失败批处理并继续
     */
    object FailBatchAndContinue : PublishingErrorDecision()

    /**
     * 在延迟后重试批处理
     * @param delay 延迟时间
     */
    data class RetryBatchAfter(val delay: Duration) : PublishingErrorDecision()

    /**
     * 立即重试批处理
     */
    object RetryBatchImmediately : PublishingErrorDecision()
}

/**
 * PublishingErrorHandler 表示发布错误处理器
 */
typealias PublishingErrorHandler = suspend (retries: Int, exception: Exception, batch: PubSubBatch) -> PublishingErrorDecision

/**
 * PendingMessage 表示待处理的消息
 * @param message 消息
 * @param deferred 延迟对象，用于完成或失败
 */
private data class PendingMessage(
    val message: Any,
    val deferred: CompletableDeferred<PubSubBatchResponse>
)

/**
 * BatchingProducer 表示批处理生产者
 * @param cluster 集群
 * @param topic 主题
 * @param config 配置
 */
class BatchingProducer(
    private val cluster: Cluster,
    private val topic: String,
    private val config: BatchingProducerConfig = BatchingProducerConfig()
) : Closeable {
    private val queue = ConcurrentLinkedQueue<PendingMessage>()
    private val isRunning = AtomicBoolean(true)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    init {
        job = scope.launch {
            processBatches()
        }
    }

    /**
     * 处理批处理
     */
    private suspend fun processBatches() {
        while (isRunning.get() && scope.isActive) {
            try {
                val batch = collectBatch()
                if (batch.isNotEmpty()) {
                    publishBatch(batch)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error processing batch" }
            }

            delay(config.flushInterval.toMillis())
        }
    }

    /**
     * 收集批处理
     * @return 收集的批处理
     */
    private fun collectBatch(): List<PendingMessage> {
        val batch = mutableListOf<PendingMessage>()
        var count = 0

        while (count < config.maxBatchSize) {
            val message = queue.poll() ?: break
            batch.add(message)
            count++
        }

        return batch
    }

    /**
     * 发布批处理
     * @param pendingMessages 待处理的消息列表
     */
    private suspend fun publishBatch(pendingMessages: List<PendingMessage>) {
        if (pendingMessages.isEmpty()) return

        val pubSubBatch = PubSubBatch(topic)
        pendingMessages.forEach { pubSubBatch.add(it.message) }

        try {
            // 发布批处理
            val pubSubActor = cluster.pubSub.pid
            val response = cluster.actorSystem.root.requestAwait<PubSubBatchResponse>(pubSubActor, BatchPublishRequest(pubSubBatch, null))

            // 完成所有待处理的消息
            pendingMessages.forEach { it.deferred.complete(response) }
        } catch (e: Exception) {
            handlePublishingError(pendingMessages, pubSubBatch, e)
        }
    }

    /**
     * 处理发布错误
     * @param pendingMessages 待处理的消息列表
     * @param batch 批处理
     * @param exception 异常
     */
    private suspend fun handlePublishingError(pendingMessages: List<PendingMessage>, batch: PubSubBatch, exception: Exception) {
        val errorHandler = config.errorHandler
        if (errorHandler == null) {
            // 默认行为：失败批处理并停止
            val response = PubSubBatchResponse(false, exception.message)
            pendingMessages.forEach { it.deferred.complete(response) }
            isRunning.set(false)
            return
        }

        var retries = 0
        var currentException = exception

        while (true) {
            val decision = errorHandler(retries, currentException, batch)

            when (decision) {
                is PublishingErrorDecision.FailBatchAndStop -> {
                    val response = PubSubBatchResponse(false, currentException.message)
                    pendingMessages.forEach { it.deferred.complete(response) }
                    isRunning.set(false)
                    return
                }
                is PublishingErrorDecision.FailBatchAndContinue -> {
                    val response = PubSubBatchResponse(false, currentException.message)
                    pendingMessages.forEach { it.deferred.complete(response) }
                    return
                }
                is PublishingErrorDecision.RetryBatchAfter -> {
                    delay(decision.delay.toMillis())
                    try {
                        val pubSubActor = cluster.pubSub.pid
                        val response = cluster.actorSystem.root.requestAwait<PubSubBatchResponse>(pubSubActor, BatchPublishRequest(batch, null))
                        pendingMessages.forEach { it.deferred.complete(response) }
                        return
                    } catch (e: Exception) {
                        currentException = e
                        retries++
                    }
                }
                is PublishingErrorDecision.RetryBatchImmediately -> {
                    try {
                        val pubSubActor = cluster.pubSub.pid
                        val response = cluster.actorSystem.root.requestAwait<PubSubBatchResponse>(pubSubActor, BatchPublishRequest(batch, null))
                        pendingMessages.forEach { it.deferred.complete(response) }
                        return
                    } catch (e: Exception) {
                        currentException = e
                        retries++
                    }
                }
            }
        }
    }

    /**
     * 生产消息
     * @param message 消息
     * @return 发布响应的延迟对象
     */
    suspend fun produce(message: Any): PubSubBatchResponse {
        val deferred = CompletableDeferred<PubSubBatchResponse>()

        if (!isRunning.get()) {
            deferred.complete(PubSubBatchResponse(false, "Producer is stopped"))
            return deferred.await()
        }

        queue.add(PendingMessage(message, deferred))
        return deferred.await()
    }

    /**
     * 生产多个消息
     * @param messages 消息列表
     * @return 发布响应的延迟对象列表
     */
    suspend fun produceAll(messages: Collection<Any>): List<PubSubBatchResponse> {
        val deferreds = messages.map {
            val deferred = CompletableDeferred<PubSubBatchResponse>()
            if (!isRunning.get()) {
                deferred.complete(PubSubBatchResponse(false, "Producer is stopped"))
            } else {
                queue.add(PendingMessage(it, deferred))
            }
            deferred
        }

        return deferreds.map { it.await() }
    }

    /**
     * 刷新队列中的所有消息
     */
    suspend fun flush() {
        val batch = collectBatch()
        if (batch.isNotEmpty()) {
            publishBatch(batch)
        }
    }

    /**
     * 关闭生产者
     */
    override fun close() {
        isRunning.set(false)
        job?.cancel()
    }
}
