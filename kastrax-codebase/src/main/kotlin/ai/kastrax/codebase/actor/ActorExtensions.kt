package ai.kastrax.codebase.actor

import java.io.File

/**
 * Actor 扩展函数
 */
object ActorExtensions {
    /**
     * 获取根目录
     */
    val File.root: File
        get() = this
    
    /**
     * 生成 Actor
     *
     * @param props Actor 属性
     * @param name Actor 名称
     * @return Actor 的 PID
     */
    suspend fun ActorSystem.spawn(props: Props, name: String): PID {
        return PID(name)
    }
    
    /**
     * 发送消息
     *
     * @param target 目标 PID
     * @param message 消息
     */
    suspend fun ActorSystem.send(target: PID, message: Any) {
        // 模拟发送消息
    }
}
