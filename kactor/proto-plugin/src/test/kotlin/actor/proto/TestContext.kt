package actor.proto

/**
 * 测试用的Context实现
 * @param self 当前Actor的PID
 * @param message 当前消息
 */
class TestContext(
    override val self: TestPID,
    override var message: Any
) : Context {
    override val actor: Actor
        get() = throw NotImplementedError("Not implemented")
    
    override val sender: TestPID? = null
    
    /**
     * 响应消息
     * @param message 响应消息
     */
    override fun respond(message: Any) {
        // 测试用的空实现
    }
    
    /**
     * 停止Actor
     * @param pid 要停止的Actor的PID
     */
    override fun stop(pid: TestPID) {
        // 测试用的空实现
    }
}
