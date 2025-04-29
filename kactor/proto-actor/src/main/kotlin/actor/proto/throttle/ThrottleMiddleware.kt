package actor.proto.throttle

import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderMiddleware
import java.time.Duration

/**
 * 创建一个接收中间件，用于对接收到的消息进行节流
 *
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param onThrottled 当消息被节流时的回调函数
 * @return 一个接收中间件，用于对接收到的消息进行节流
 */
fun throttleReceiveMiddleware(
    maxEventsInPeriod: Int,
    period: Duration,
    onThrottled: (Int) -> Unit = {}
): ReceiveMiddleware {
    val strategy = CountBasedThrottleStrategy(maxEventsInPeriod, period, onThrottled)

    return { next ->
        { ctx ->
            if (strategy.processMessage(ctx.message)) {
                next(ctx)
            } else {
                // 消息被节流，不处理
                val messageType = when (val msg = ctx.message) {
                    null -> "null"
                    else -> msg.javaClass.simpleName
                }
                ctx.logger.debug("Message throttled", "message_type" to messageType)
            }
        }
    }
}

/**
 * 创建一个发送中间件，用于对发送的消息进行节流
 *
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param onThrottled 当消息被节流时的回调函数
 * @return 一个发送中间件，用于对发送的消息进行节流
 */
fun throttleSenderMiddleware(
    maxEventsInPeriod: Int,
    period: Duration,
    onThrottled: (Int) -> Unit = {}
): SenderMiddleware {
    val strategy = CountBasedThrottleStrategy(maxEventsInPeriod, period, onThrottled)

    return { next ->
        { ctx, pid, msg ->
            val message = when (msg) {
                is MessageEnvelope -> msg.message
                else -> msg
            }

            if (strategy.processMessage(message)) {
                next(ctx, pid, msg)
            } else {
                // 消息被节流，不发送
                val messageType = when (message) {
                    null -> "null"
                    else -> message.javaClass.simpleName
                }
                ctx.logger.debug("Message send throttled",
                    "to" to pid,
                    "message_type" to messageType
                )
            }
        }
    }
}

/**
 * 创建一个接收中间件，用于对特定类型的消息进行节流
 *
 * @param messageType 要节流的消息类型
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param onThrottled 当消息被节流时的回调函数
 * @return 一个接收中间件，用于对特定类型的消息进行节流
 */
inline fun <reified T> throttleMessageTypeMiddleware(
    maxEventsInPeriod: Int,
    period: Duration,
    noinline onThrottled: (Int) -> Unit = {}
): ReceiveMiddleware {
    val strategy = CountBasedThrottleStrategy(maxEventsInPeriod, period, onThrottled)

    return { next ->
        { ctx ->
            val message = ctx.message

            if (message is T) {
                if (strategy.processMessage(message)) {
                    next(ctx)
                } else {
                    // 消息被节流，不处理
                    ctx.logger.debug("Message of type ${T::class.java.simpleName} throttled")
                }
            } else {
                // 不是目标类型的消息，直接处理
                next(ctx)
            }
        }
    }
}
