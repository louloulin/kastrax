package actor.proto.plugin.examples.ratelimit

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderMiddleware
import actor.proto.plugin.ReceiveMiddlewareExtension
import actor.proto.plugin.SenderMiddlewareExtension
import org.pf4j.Extension
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 限流器
 * 用于限制消息处理速率
 * @param maxRequests 最大请求数
 * @param period 时间周期
 */
class RateLimiter(
    private val maxRequests: Int,
    private val period: Duration
) {
    private val counter = AtomicInteger(0)
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        // 定期重置计数器
        scheduler.scheduleAtFixedRate(
            { counter.set(0) },
            period.toMillis(),
            period.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * 尝试获取令牌
     * @return 如果获取成功则返回true，否则返回false
     */
    fun tryAcquire(): Boolean {
        val current = counter.get()
        if (current >= maxRequests) {
            return false
        }
        return counter.incrementAndGet() <= maxRequests
    }

    /**
     * 关闭限流器
     */
    fun shutdown() {
        scheduler.shutdown()
    }
}

/**
 * 限流管理器
 * 管理所有限流器
 */
object RateLimitManager {
    private val logger = LoggerFactory.getLogger(RateLimitManager::class.java)
    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()

    /**
     * 获取或创建限流器
     * @param key 限流器键
     * @param maxRequests 最大请求数
     * @param period 时间周期
     * @return 限流器
     */
    fun getRateLimiter(key: String, maxRequests: Int, period: Duration): RateLimiter {
        return rateLimiters.computeIfAbsent(key) { RateLimiter(maxRequests, period) }
    }

    /**
     * 关闭所有限流器
     */
    fun shutdown() {
        rateLimiters.values.forEach { it.shutdown() }
        rateLimiters.clear()
    }
}

/**
 * 限流接收中间件扩展
 * 用于限制Actor的消息处理速率
 */
@Extension
class RateLimitReceiveMiddleware : ReceiveMiddlewareExtension {
    private val logger = LoggerFactory.getLogger(RateLimitReceiveMiddleware::class.java)

    override fun id(): String = "rate-limit-receive-middleware"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Rate limit receive middleware initialized")
    }

    override fun shutdown() {
        logger.info("Rate limit receive middleware shutdown")
    }

    // 默认限流配置
    private val defaultMaxRequests = 100
    private val defaultPeriod = Duration.ofSeconds(1)

    override fun getReceiveMiddleware(): ReceiveMiddleware = { next ->
        { ctx ->
            val actorType = ctx.actor.javaClass.simpleName
            val key = "receive:$actorType"

            // 获取限流器
            val rateLimiter = RateLimitManager.getRateLimiter(key, defaultMaxRequests, defaultPeriod)

            // 尝试获取令牌
            if (rateLimiter.tryAcquire()) {
                // 处理消息
                next(ctx)
            } else {
                // 限流，记录日志
                logger.warn("Rate limit exceeded for actor: {}", actorType)

                // 可以选择丢弃消息或重新排队
                // 这里选择丢弃消息
            }
        }
    }
}

/**
 * 限流发送中间件扩展
 * 用于限制Actor的消息发送速率
 */
@Extension
class RateLimitSenderMiddleware : SenderMiddlewareExtension {
    private val logger = LoggerFactory.getLogger(RateLimitSenderMiddleware::class.java)

    override fun id(): String = "rate-limit-sender-middleware"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Rate limit sender middleware initialized")
    }

    override fun shutdown() {
        logger.info("Rate limit sender middleware shutdown")
    }

    // 默认限流配置
    private val defaultMaxRequests = 100
    private val defaultPeriod = Duration.ofSeconds(1)

    override fun getSenderMiddleware(): SenderMiddleware = { next ->
        { ctx, pid, envelope ->
            val senderType = ctx.javaClass.simpleName
            val key = "send:$senderType"

            // 获取限流器
            val rateLimiter = RateLimitManager.getRateLimiter(key, defaultMaxRequests, defaultPeriod)

            // 尝试获取令牌
            if (rateLimiter.tryAcquire()) {
                // 发送消息
                next(ctx, pid, envelope)
            } else {
                // 限流，记录日志
                logger.warn("Rate limit exceeded for sender: {}", senderType)

                // 可以选择丢弃消息或重新排队
                // 这里选择丢弃消息
            }
        }
    }
}
