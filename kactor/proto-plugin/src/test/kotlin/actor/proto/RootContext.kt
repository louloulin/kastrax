package actor.proto

import kotlinx.coroutines.runBlocking

/**
 * 测试用的RootContext类
 */
class RootContext {
    // 存储所有Actor
    private val actors = mutableMapOf<TestPID, Actor>()

    // 存储所有中间件
    private val middlewares = mutableListOf<ReceiveMiddleware>()

    /**
     * 添加中间件
     * @param middleware 中间件
     */
    fun addMiddleware(middleware: ReceiveMiddleware) {
        middlewares.add(middleware)
    }

    /**
     * 发送消息
     * @param pid 目标PID
     * @param message 消息
     */
    fun send(pid: TestPID, message: Any) {
        val actor = actors[pid] ?: return

        // 创建Context
        val context = TestContext(pid, message)

        // 应用中间件
        var receive: Receive = createReceive { ctx ->
            with(actor) {
                ctx.receive(ctx.message)
            }
        }

        // 应用所有中间件
        for (middleware in middlewares.reversed()) {
            receive = applyReceiveMiddleware(middleware, receive)
        }

        // 处理消息
        runBlocking {
            receive(context)
        }
    }

    /**
     * 创建Actor
     * @param props Actor属性
     * @return Actor的PID
     */
    fun spawn(props: Props): TestPID {
        val actor = props.producer?.invoke() ?: return createPID("invalid", "invalid")
        val pid = createPID("test", "test-${actors.size}")
        actors[pid] = actor
        return pid
    }
}
