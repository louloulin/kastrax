package actor.proto

/**
 * 测试用的Context类
 */
interface Context {
    /**
     * 当前Actor
     */
    val actor: Actor

    /**
     * 当前消息
     */
    var message: Any

    /**
     * 当前Actor的PID
     */
    val self: TestPID

    /**
     * 发送者的PID
     */
    val sender: TestPID?

    /**
     * 响应消息
     * @param message 响应消息
     */
    fun respond(message: Any)

    /**
     * 停止Actor
     * @param pid 要停止的Actor的PID
     */
    fun stop(pid: TestPID)
}
