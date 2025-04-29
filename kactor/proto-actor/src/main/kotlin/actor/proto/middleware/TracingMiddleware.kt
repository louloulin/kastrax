package actor.proto.middleware

import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.MessageHeader
import actor.proto.PID
import actor.proto.ReceiveMiddleware
import actor.proto.SenderContext
import actor.proto.SenderMiddleware
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 跟踪中间件，用于跟踪消息的发送和接收
 * @return 发送中间件
 */
fun traceSender(): SenderMiddleware {
    return { next ->
        { ctx, target, envelope ->
            val traceId = UUID.randomUUID().toString()
            val newHeader = MessageHeader()
            if (envelope.header != null) {
                envelope.header!!.keys().forEach { key ->
                    envelope.header!!.get(key)?.let { value ->
                        newHeader.set(key, value)
                    }
                }
            }
            newHeader.set("trace-id", traceId)
            val newEnvelope = MessageEnvelope(envelope.message, envelope.sender, newHeader)

            logger.debug { "Sending message with trace-id: $traceId from ${ctx.self} to $target" }
            next(ctx, target, newEnvelope)
        }
    }
}

/**
 * 跟踪中间件，用于跟踪消息的发送和接收
 * @return 接收中间件
 */
fun traceReceiver(): ReceiveMiddleware {
    return { next ->
        { ctx ->
            val traceId = ctx.headers?.get("trace-id")
            if (traceId != null) {
                logger.debug { "Received message with trace-id: $traceId at ${ctx.self}" }
            }
            next(ctx)
        }
    }
}
