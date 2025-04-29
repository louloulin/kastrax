package actor.proto.middleware

import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.ReceiveMiddleware
import actor.proto.SenderContext
import actor.proto.SenderMiddleware
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 度量中间件，用于收集 Actor 系统的度量数据
 */
object MetricsMiddleware {
    private val messageCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val processingTimes = ConcurrentHashMap<String, Long>()
    private val messageTypeCount = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 获取已处理的消息总数
     * @return 消息总数
     */
    fun getMessageCount(): Long = messageCount.get()

    /**
     * 获取处理消息时发生的错误总数
     * @return 错误总数
     */
    fun getErrorCount(): Long = errorCount.get()

    /**
     * 获取各类型消息的处理数量
     * @return 消息类型及其处理数量的映射
     */
    fun getMessageTypeCount(): Map<String, Long> = messageTypeCount.mapValues { it.value.get() }

    /**
     * 获取各 Actor 的平均消息处理时间（纳秒）
     * @return Actor ID 及其平均处理时间的映射
     */
    fun getAverageProcessingTimes(): Map<String, Long> = processingTimes.toMap()

    /**
     * 重置所有度量数据
     */
    fun reset() {
        messageCount.set(0)
        errorCount.set(0)
        processingTimes.clear()
        messageTypeCount.clear()
    }

    /**
     * 创建度量接收中间件
     * @return 接收中间件
     */
    fun metricsReceive(): ReceiveMiddleware {
        return { next ->
            { ctx ->
                val start = Instant.now()
                try {
                    messageCount.incrementAndGet()
                    val messageType = ctx.message.javaClass.simpleName
                    messageTypeCount.computeIfAbsent(messageType) { AtomicLong(0) }.incrementAndGet()
                    
                    next(ctx)
                    
                    val end = Instant.now()
                    val duration = Duration.between(start, end).toNanos()
                    processingTimes[ctx.self.id] = duration
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                    throw e
                }
            }
        }
    }

    /**
     * 创建度量发送中间件
     * @return 发送中间件
     */
    fun metricsSend(): SenderMiddleware {
        return { next ->
            { ctx, target, envelope ->
                val messageType = envelope.message.javaClass.simpleName
                messageTypeCount.computeIfAbsent("sent_$messageType") { AtomicLong(0) }.incrementAndGet()
                
                next(ctx, target, envelope)
            }
        }
    }
}
