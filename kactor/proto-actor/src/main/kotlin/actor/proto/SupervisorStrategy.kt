package actor.proto

import actor.proto.RestartStatistics

/**
 * SupervisorStrategy 定义了如何处理失败的子 Actor
 */
interface SupervisorStrategy {
    /**
     * 处理子 Actor 的失败
     * @param actorSystem Actor 系统
     * @param supervisor 监督者
     * @param child 失败的子 Actor
     * @param restartStatistics 重启统计信息
     * @param reason 失败原因
     * @param message 失败时处理的消息
     */
    fun handleFailure(
        actorSystem: ActorSystem,
        supervisor: Supervisor,
        child: PID,
        restartStatistics: RestartStatistics,
        reason: Any,
        message: Any?
    )
}