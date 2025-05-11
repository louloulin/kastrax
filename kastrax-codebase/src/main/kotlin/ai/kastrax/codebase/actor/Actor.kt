package ai.kastrax.codebase.actor

/**
 * Actor 接口
 */
interface Actor {
    /**
     * 接收消息
     *
     * @param context 上下文
     */
    suspend fun receive(context: Context)
}
