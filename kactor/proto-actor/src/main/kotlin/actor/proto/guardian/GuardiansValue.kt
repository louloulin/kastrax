package actor.proto.guardian

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.SupervisorStrategy
import java.util.concurrent.ConcurrentHashMap

/**
 * GuardiansValue 管理 Actor 系统中的所有 Guardian
 * @param actorSystem Actor 系统
 */
class GuardiansValue(val actorSystem: ActorSystem) {
    private val guardians = ConcurrentHashMap<SupervisorStrategy, GuardianProcess>()

    /**
     * 获取指定监督策略的 Guardian PID
     * @param strategy 监督策略
     * @return Guardian 的 PID
     */
    fun getGuardianPid(strategy: SupervisorStrategy): PID {
        return guardians.computeIfAbsent(strategy) { s ->
            newGuardian(s)
        }.pid
    }

    /**
     * 创建一个新的 Guardian
     * @param strategy 监督策略
     * @return 新创建的 GuardianProcess
     */
    private fun newGuardian(strategy: SupervisorStrategy): GuardianProcess {
        val ref = GuardianProcess(strategy, this)
        val id = actorSystem.processRegistry().nextId()
        val pid = actorSystem.processRegistry().put("guardian$id", ref)
        ref.pid = pid
        return ref
    }
}
