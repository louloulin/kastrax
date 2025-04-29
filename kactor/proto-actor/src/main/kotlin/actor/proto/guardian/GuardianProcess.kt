package actor.proto.guardian

import actor.proto.ActorSystem
import actor.proto.Failure
import actor.proto.PID
import actor.proto.Process
import actor.proto.Restart
import actor.proto.SupervisorStrategy
import actor.proto.Supervisor
import actor.proto.mailbox.ResumeMailbox
import actor.proto.mailbox.SystemMessage

/**
 * GuardianProcess 是一个特殊的进程，用于监督其他 Actor
 * @param strategy 监督策略
 * @param guardians Guardian 管理器
 */
class GuardianProcess(
    private val strategy: SupervisorStrategy,
    private val guardians: GuardiansValue
) : Process() {
    lateinit var pid: PID

    override fun sendUserMessage(pid: PID, message: Any) {
        throw IllegalStateException("Guardian actor cannot receive any user messages")
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        if (message is Failure) {
            val supervisor = object : Supervisor {
                override fun children(): Set<PID> {
                    throw IllegalStateException("Guardian does not hold its children PIDs")
                }

                override fun escalateFailure(reason: Any, message: Any?) {
                    throw IllegalStateException("Guardian cannot escalate failure")
                }

                override fun restartChildren(vararg pids: PID) {
                    pids.forEach { childPid ->
                        val restart = Restart(Exception("Restarting"))
                        val process = guardians.actorSystem.processRegistry().get(childPid)
                        process.sendSystemMessage(childPid, restart)
                    }
                }

                override fun stopChildren(vararg pids: PID) {
                    pids.forEach { childPid ->
                        guardians.actorSystem.stop(childPid)
                    }
                }

                override fun resumeChildren(vararg pids: PID) {
                    pids.forEach { childPid ->
                        val process = guardians.actorSystem.processRegistry().get(childPid)
                        process.sendSystemMessage(childPid, ResumeMailbox)
                    }
                }
            }

            strategy.handleFailure(
                guardians.actorSystem,
                supervisor,
                message.who,
                message.restartStatistics,
                message.reason,
                message.message
            )
        }
    }

    override fun stop(pid: PID) {
        // Guardian 不能被停止，忽略此操作
    }
}
