package actor.proto.throttle

import actor.proto.logging.Logger
import java.time.Duration

/**
 * 节流策略接口，定义了不同的节流行为
 */
interface ThrottleStrategy {
    /**
     * 处理消息，根据策略决定是否允许消息通过
     *
     * @param message 要处理的消息
     * @return 如果消息被允许通过，则返回true；否则返回false
     */
    fun <T> processMessage(message: T): Boolean
}

/**
 * 基于计数的节流策略，在指定时间段内限制消息数量
 *
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param onThrottled 当消息被节流时的回调函数
 */
class CountBasedThrottleStrategy(
    maxEventsInPeriod: Int,
    period: Duration,
    private val onThrottled: (Int) -> Unit = {}
) : ThrottleStrategy {
    private val throttle = newThrottle(maxEventsInPeriod, period, onThrottled)

    override fun <T> processMessage(message: T): Boolean {
        return when (throttle()) {
            Valve.OPEN, Valve.CLOSING -> true
            Valve.CLOSED -> false
        }
    }
}

/**
 * 带有日志记录的基于计数的节流策略
 *
 * @param logger 用于记录日志的Logger实例
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param onThrottled 当消息被节流时的回调函数
 */
class LoggingCountBasedThrottleStrategy(
    private val logger: Logger,
    maxEventsInPeriod: Int,
    period: Duration,
    private val onThrottled: (Logger, Int) -> Unit = { logger, count ->
        logger.warning("Throttled $count messages in the last ${period.seconds} seconds")
    }
) : ThrottleStrategy {
    private val throttle = newThrottleWithLogger(logger, maxEventsInPeriod, period, onThrottled)

    override fun <T> processMessage(message: T): Boolean {
        val valve = throttle()
        val messageType = when (message) {
            null -> "null"
            else -> message.javaClass.simpleName
        }

        return when (valve) {
            Valve.OPEN -> {
                logger.debug("Message allowed through throttle", "message_type" to messageType)
                true
            }
            Valve.CLOSING -> {
                logger.debug("Message allowed through throttle (closing)", "message_type" to messageType)
                true
            }
            Valve.CLOSED -> {
                logger.debug("Message throttled", "message_type" to messageType)
                false
            }
        }
    }
}

/**
 * 创建一个消息处理器，应用节流策略
 *
 * @param strategy 要应用的节流策略
 * @param handler 当消息被允许通过时的处理函数
 * @return 一个应用了节流策略的消息处理函数
 */
fun <T> throttle(strategy: ThrottleStrategy, handler: (T) -> Unit): (T) -> Unit {
    return { message ->
        if (strategy.processMessage(message)) {
            handler(message)
        }
    }
}
