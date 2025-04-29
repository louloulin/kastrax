package actor.proto.metrics

import actor.proto.*
import java.time.Duration

/**
 * 创建接收消息的度量中间件
 * @param registry 度量注册表
 * @return 接收中间件
 */
fun metricsReceiveMiddleware(registry: MetricsRegistry): ReceiveMiddleware {
    return { next ->
        { ctx ->
            val startTime = System.nanoTime()
            val actorType = ctx.actor?.javaClass?.simpleName ?: "Unknown"

            try {
                next(ctx)
            } finally {
                val duration = System.nanoTime() - startTime
                val durationMs = duration / 1_000_000.0

                // 记录消息处理计数
                registry.counter("actor.messages.processed", mapOf(
                    "actor" to actorType,
                    "address" to ctx.self.address
                )).inc()

                // 记录消息处理时间
                registry.histogram("actor.messages.processing.time", mapOf(
                    "actor" to actorType,
                    "address" to ctx.self.address
                )).observe(durationMs)
            }
        }
    }
}

/**
 * 创建发送消息的度量中间件
 * @param registry 度量注册表
 * @return 发送中间件
 */
fun metricsSenderMiddleware(registry: MetricsRegistry): SenderMiddleware {
    return { next ->
        { ctx, target, envelope ->
            val startTime = System.nanoTime()

            try {
                next(ctx, target, envelope)
            } finally {
                val duration = System.nanoTime() - startTime
                val durationMs = duration / 1_000_000.0

                // 记录消息发送计数
                registry.counter("actor.messages.sent", mapOf(
                    "sender" to ctx.self.id,
                    "target" to target.id
                )).inc()

                // 记录消息发送时间
                registry.histogram("actor.messages.sending.time", mapOf(
                    "sender" to ctx.self.id,
                    "target" to target.id
                )).observe(durationMs)
            }
        }
    }
}
