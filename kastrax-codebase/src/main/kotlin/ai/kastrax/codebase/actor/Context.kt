package ai.kastrax.codebase.actor

/**
 * Actor 上下文
 */
interface Context {
    /**
     * 获取 Actor 的 PID
     */
    val self: PID
    
    /**
     * 获取发送者的 PID
     */
    val sender: PID?
    
    /**
     * 发送消息
     *
     * @param target 目标 PID
     * @param message 消息
     */
    suspend fun send(target: PID, message: Any)
    
    /**
     * 回复消息
     *
     * @param message 消息
     */
    suspend fun respond(message: Any)
    
    /**
     * 请求并等待响应
     *
     * @param target 目标 PID
     * @param message 消息
     * @return 响应
     */
    suspend fun requestAwait(target: PID, message: Any): Any
    
    /**
     * 监视 Actor
     *
     * @param target 目标 PID
     */
    suspend fun watch(target: PID)
    
    /**
     * 取消监视 Actor
     *
     * @param target 目标 PID
     */
    suspend fun unwatch(target: PID)
    
    /**
     * 生成子 Actor
     *
     * @param props Actor 属性
     * @param name Actor 名称
     * @return 子 Actor 的 PID
     */
    suspend fun spawnChild(props: Props, name: String): PID
}
