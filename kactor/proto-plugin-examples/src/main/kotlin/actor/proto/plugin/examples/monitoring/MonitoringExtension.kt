package actor.proto.plugin.examples.monitoring

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.Props
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderContext
import actor.proto.SenderMiddleware
import actor.proto.plugin.ActorDecoratorExtension
import actor.proto.plugin.PropsDecoratorExtension
import actor.proto.plugin.ReceiveMiddlewareExtension
import actor.proto.plugin.SenderMiddlewareExtension
import org.pf4j.Extension
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Actor lifecycle messages
 */
object Started
object Stopped
object Restarting

/**
 * 监控数据
 * 收集Actor系统的指标
 */
object MonitoringData {
    // 消息计数器
    private val messageCounter = ConcurrentHashMap<String, AtomicLong>()

    // 处理时间
    private val processingTime = ConcurrentHashMap<String, AtomicLong>()

    // 错误计数器
    private val errorCounter = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 记录消息
     * @param actorType Actor类型
     * @param messageType 消息类型
     */
    fun recordMessage(actorType: String, messageType: String) {
        val key = "$actorType:$messageType"
        messageCounter.computeIfAbsent(key) { AtomicLong(0) }.incrementAndGet()
    }

    /**
     * 记录处理时间
     * @param actorType Actor类型
     * @param messageType 消息类型
     * @param duration 处理时间
     */
    fun recordProcessingTime(actorType: String, messageType: String, duration: Duration) {
        val key = "$actorType:$messageType"
        processingTime.computeIfAbsent(key) { AtomicLong(0) }.addAndGet(duration.toNanos())
    }

    /**
     * 记录错误
     * @param actorType Actor类型
     * @param errorType 错误类型
     */
    fun recordError(actorType: String, errorType: String) {
        val key = "$actorType:$errorType"
        errorCounter.computeIfAbsent(key) { AtomicLong(0) }.incrementAndGet()
    }

    /**
     * 获取消息计数
     * @param actorType Actor类型
     * @param messageType 消息类型
     * @return 消息计数
     */
    fun getMessageCount(actorType: String, messageType: String): Long {
        val key = "$actorType:$messageType"
        return messageCounter[key]?.get() ?: 0
    }

    /**
     * 获取平均处理时间
     * @param actorType Actor类型
     * @param messageType 消息类型
     * @return 平均处理时间（毫秒）
     */
    fun getAverageProcessingTime(actorType: String, messageType: String): Double {
        val key = "$actorType:$messageType"
        val totalTime = processingTime[key]?.get() ?: 0
        val count = messageCounter[key]?.get() ?: 1
        return totalTime / (count * 1_000_000.0)
    }

    /**
     * 获取错误计数
     * @param actorType Actor类型
     * @param errorType 错误类型
     * @return 错误计数
     */
    fun getErrorCount(actorType: String, errorType: String): Long {
        val key = "$actorType:$errorType"
        return errorCounter[key]?.get() ?: 0
    }

    /**
     * 重置所有计数器
     */
    fun reset() {
        messageCounter.clear()
        processingTime.clear()
        errorCounter.clear()
    }

    /**
     * 获取所有指标
     * @return 所有指标的映射
     */
    fun getAllMetrics(): Map<String, Any> {
        val metrics = mutableMapOf<String, Any>()

        // 添加消息计数
        messageCounter.forEach { (key, count) ->
            metrics["message_count:$key"] = count.get()
        }

        // 添加处理时间
        processingTime.forEach { (key, time) ->
            val count = messageCounter[key]?.get() ?: 1
            metrics["avg_processing_time_ms:$key"] = time.get() / (count * 1_000_000.0)
        }

        // 添加错误计数
        errorCounter.forEach { (key, count) ->
            metrics["error_count:$key"] = count.get()
        }

        return metrics
    }
}

/**
 * 监控接收中间件扩展
 * 用于收集消息处理指标
 */
@Extension
class MonitoringReceiveMiddleware : ReceiveMiddlewareExtension {
    private val logger = LoggerFactory.getLogger(MonitoringReceiveMiddleware::class.java)

    override fun id(): String = "monitoring-receive-middleware"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Monitoring receive middleware initialized")
    }

    override fun shutdown() {
        logger.info("Monitoring receive middleware shutdown")
    }

    override fun getReceiveMiddleware(): ReceiveMiddleware = { next ->
        { ctx ->
            val actorType = ctx.actor.javaClass.simpleName
            val messageType = ctx.message.javaClass.simpleName

            // 记录消息
            MonitoringData.recordMessage(actorType, messageType)

            // 记录处理时间
            val startTime = System.nanoTime()
            try {
                next(ctx)
            } catch (e: Exception) {
                // 记录错误
                MonitoringData.recordError(actorType, e.javaClass.simpleName)
                throw e
            } finally {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                MonitoringData.recordProcessingTime(actorType, messageType, duration)
            }
        }
    }
}

/**
 * 监控发送中间件扩展
 * 用于收集消息发送指标
 */
@Extension
class MonitoringSenderMiddleware : SenderMiddlewareExtension {
    private val logger = LoggerFactory.getLogger(MonitoringSenderMiddleware::class.java)

    override fun id(): String = "monitoring-sender-middleware"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Monitoring sender middleware initialized")
    }

    override fun shutdown() {
        logger.info("Monitoring sender middleware shutdown")
    }

    override fun getSenderMiddleware(): SenderMiddleware = { next ->
        { ctx, pid, envelope ->
            val senderType = ctx.javaClass.simpleName
            val messageType = envelope.message.javaClass.simpleName

            // 记录消息
            MonitoringData.recordMessage("$senderType:send", messageType)

            // 记录处理时间
            val startTime = System.nanoTime()
            try {
                next(ctx, pid, envelope)
            } catch (e: Exception) {
                // 记录错误
                MonitoringData.recordError("$senderType:send", e.javaClass.simpleName)
                throw e
            } finally {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                MonitoringData.recordProcessingTime("$senderType:send", messageType, duration)
            }
        }
    }
}

/**
 * 监控Actor装饰器扩展
 * 用于收集Actor生命周期指标
 */
@Extension
class MonitoringActorDecorator : ActorDecoratorExtension {
    private val logger = LoggerFactory.getLogger(MonitoringActorDecorator::class.java)

    override fun id(): String = "monitoring-actor-decorator"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Monitoring actor decorator initialized")
    }

    override fun shutdown() {
        logger.info("Monitoring actor decorator shutdown")
    }

    override fun decorateActor(actor: Actor): Actor {
        return MonitoredActor(actor)
    }

    /**
     * 被监控的Actor
     * @param actor 原始Actor
     */
    private class MonitoredActor(private val actor: Actor) : Actor {
        override suspend fun Context.receive(msg: Any) {
            val actorType = actor.javaClass.simpleName

            // 记录Actor启动和停止
            when (msg) {
                is Started -> {
                    MonitoringData.recordMessage(actorType, "Started")
                }
                is Stopped -> {
                    MonitoringData.recordMessage(actorType, "Stopped")
                }
                is Restarting -> {
                    MonitoringData.recordMessage(actorType, "Restarting")
                }
            }

            // 调用原始Actor的receive方法
            with(actor) {
                this@receive.receive(msg)
            }
        }
    }
}

/**
 * 监控Props装饰器扩展
 * 用于收集Actor创建指标
 */
@Extension
class MonitoringPropsDecorator : PropsDecoratorExtension {
    private val logger = LoggerFactory.getLogger(MonitoringPropsDecorator::class.java)

    override fun id(): String = "monitoring-props-decorator"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Monitoring props decorator initialized")
    }

    override fun shutdown() {
        logger.info("Monitoring props decorator shutdown")
    }

    override fun decorateProps(props: Props): Props {
        // 记录Actor创建
        val producerType = props.producer?.javaClass?.simpleName ?: "Unknown"
        MonitoringData.recordMessage("ActorSystem", "CreateActor:$producerType")

        return props
    }
}
