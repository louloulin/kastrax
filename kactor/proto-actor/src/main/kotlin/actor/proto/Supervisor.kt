package actor.proto

/**
 * Supervisor 是一个用于管理子 Actor 生命周期的接口
 */
interface Supervisor {
    /**
     * 获取所有子 Actor
     * @return 子 Actor 的集合
     */
    fun children(): Set<PID>

    /**
     * 将失败上报给父级
     * @param reason 失败原因
     * @param message 失败时处理的消息
     */
    fun escalateFailure(reason: Any, message: Any?)

    /**
     * 重启指定的子 Actor
     * @param pids 要重启的子 Actor 的 PID
     */
    fun restartChildren(vararg pids: PID)

    /**
     * 停止指定的子 Actor
     * @param pids 要停止的子 Actor 的 PID
     */
    fun stopChildren(vararg pids: PID)

    /**
     * 恢复指定的子 Actor
     * @param pids 要恢复的子 Actor 的 PID
     */
    fun resumeChildren(vararg pids: PID)
}