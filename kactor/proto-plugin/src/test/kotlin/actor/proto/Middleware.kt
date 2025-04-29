package actor.proto

/**
 * 接收函数接口
 */
fun interface Receive {
    suspend operator fun invoke(ctx: Context)
}

/**
 * 创建Receive实例
 */
fun createReceive(block: suspend (Context) -> Unit): Receive {
    return object : Receive {
        override suspend fun invoke(ctx: Context) {
            block(ctx)
        }
    }
}

/**
 * 接收中间件接口
 */
fun interface ReceiveMiddleware {
    operator fun invoke(next: Receive): Receive
}

/**
 * 创建ReceiveMiddleware实例
 */
fun createReceiveMiddleware(block: (Receive) -> Receive): ReceiveMiddleware {
    return object : ReceiveMiddleware {
        override fun invoke(next: Receive): Receive {
            return block(next)
        }
    }
}

/**
 * 应用中间件
 * @param middleware 中间件
 * @param next 下一个处理函数
 * @return 处理函数
 */
fun applyReceiveMiddleware(middleware: ReceiveMiddleware, next: Receive): Receive {
    return middleware.invoke(next)
}

/**
 * 发送函数类型
 */
typealias Send = suspend (context: SenderContext, target: TestPID, envelope: MessageEnvelope) -> Unit

/**
 * 发送中间件
 */
typealias SenderMiddleware = (next: Send) -> Send

/**
 * 发送者上下文
 */
interface SenderContext

/**
 * 消息信封
 */
data class MessageEnvelope(val message: Any)
