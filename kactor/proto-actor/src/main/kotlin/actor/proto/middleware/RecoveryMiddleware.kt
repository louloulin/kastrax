package actor.proto.middleware

import actor.proto.Context
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 恢复中间件，用于捕获 Actor 处理消息时的异常并恢复
 * @return 接收中间件
 */
fun recovery(): ReceiveMiddleware {
    return { next ->
        { ctx ->
            try {
                next(ctx)
            } catch (e: Exception) {
                logger.error(e) { "Actor ${ctx.self} failed to process message: ${ctx.message}" }
            }
        }
    }
}

/**
 * 恢复中间件，用于捕获 Actor 处理消息时的异常并恢复，带有自定义异常处理函数
 * @param handler 自定义异常处理函数
 * @return 接收中间件
 */
fun recovery(handler: (Context, Exception) -> Unit): ReceiveMiddleware {
    return { next ->
        { ctx ->
            try {
                next(ctx)
            } catch (e: Exception) {
                handler(ctx, e)
            }
        }
    }
}
