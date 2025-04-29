package actor.proto

/**
 * 测试用的Actor接口
 */
interface Actor {
    /**
     * 接收消息
     * @param ctx 上下文
     * @param msg 消息
     */
    suspend fun Context.receive(msg: Any)
}
