package actor.proto.middleware

import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderMiddleware

/**
 * 日志中间件，用于记录 Actor 接收到的消息
 * @return 接收中间件
 */
fun logReceive(): ReceiveMiddleware {
    return { next ->
        { ctx ->
            ctx.logger.info("Actor received message", "actor" to ctx.self, "message_type" to ctx.message.javaClass.simpleName)
            next(ctx)
        }
    }
}

/**
 * 日志中间件，用于记录 Actor 接收到的消息，带有自定义日志函数
 * @param logFn 自定义日志函数
 * @return 接收中间件
 */
fun logReceive(logFn: (Context) -> Unit): ReceiveMiddleware {
    return { next ->
        { ctx ->
            logFn(ctx)
            next(ctx)
        }
    }
}

/**
 * Middleware for logging messages received by actors
 * @param logMessageContent Whether to log the content of messages (default: false)
 * @param logLevel The log level to use (default: DEBUG)
 * @return A receive middleware function that logs messages
 */
fun loggingReceiveMiddleware(
    logMessageContent: Boolean = false,
    logLevel: LoggingMiddlewareLevel = LoggingMiddlewareLevel.DEBUG
): ReceiveMiddleware = { next ->
    { ctx ->
        val message = ctx.message
        val messageType = message.javaClass.simpleName

        when (logLevel) {
            LoggingMiddlewareLevel.DEBUG -> {
                if (logMessageContent) {
                    ctx.logger.debug(
                        "Received message",
                        "type" to messageType,
                        "message" to message
                    )
                } else {
                    ctx.logger.debug(
                        "Received message",
                        "type" to messageType
                    )
                }
            }
            LoggingMiddlewareLevel.INFO -> {
                if (logMessageContent) {
                    ctx.logger.info(
                        "Received message",
                        "type" to messageType,
                        "message" to message
                    )
                } else {
                    ctx.logger.info(
                        "Received message",
                        "type" to messageType
                    )
                }
            }
        }

        next(ctx)
    }
}

/**
 * Middleware for logging messages sent by actors
 * @param logMessageContent Whether to log the content of messages (default: false)
 * @param logLevel The log level to use (default: DEBUG)
 * @return A sender middleware function that logs messages
 */
fun loggingSenderMiddleware(
    logMessageContent: Boolean = false,
    logLevel: LoggingMiddlewareLevel = LoggingMiddlewareLevel.DEBUG
): SenderMiddleware = { next ->
    { ctx, pid, msg ->
        val message = when (msg) {
            is MessageEnvelope -> msg.message
            else -> msg
        }
        val messageType = message.javaClass.simpleName

        when (logLevel) {
            LoggingMiddlewareLevel.DEBUG -> {
                if (logMessageContent) {
                    ctx.logger.debug(
                        "Sending message",
                        "to" to pid,
                        "type" to messageType,
                        "message" to message
                    )
                } else {
                    ctx.logger.debug(
                        "Sending message",
                        "to" to pid,
                        "type" to messageType
                    )
                }
            }
            LoggingMiddlewareLevel.INFO -> {
                if (logMessageContent) {
                    ctx.logger.info(
                        "Sending message",
                        "to" to pid,
                        "type" to messageType,
                        "message" to message
                    )
                } else {
                    ctx.logger.info(
                        "Sending message",
                        "to" to pid,
                        "type" to messageType
                    )
                }
            }
        }

        next(ctx, pid, msg)
    }
}

/**
 * Log level for logging middleware
 */
enum class LoggingMiddlewareLevel {
    DEBUG,
    INFO
}
