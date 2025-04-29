package actor.proto.pidset

import actor.proto.PID
import actor.proto.ActorSystem
import actor.proto.ProcessRegistry

/**
 * PIDSetUtils 提供了一些 PID Set 相关的工具方法
 */
object PIDSetUtils {
    /**
     * 向 PID 集合中的所有 Actor 发送消息
     * @param pidSet PID 集合
     * @param message 要发送的消息
     * @param system Actor 系统
     */
    fun broadcast(pidSet: PIDSet, message: Any, system: ActorSystem) {
        pidSet.forEach { pid ->
            system.send(pid, message)
        }
    }



    /**
     * 停止 PID 集合中的所有 Actor
     * @param pidSet PID 集合
     * @param system Actor 系统
     */
    fun stopAll(pidSet: PIDSet, system: ActorSystem) {
        pidSet.forEach { pid ->
            system.stop(pid)
        }
    }

    /**
     * 检查 PID 集合中的所有 Actor 是否都存在
     * @param pidSet PID 集合
     * @param registry 进程注册表
     * @return 如果所有 Actor 都存在则返回 true，否则返回 false
     */
    fun allExist(pidSet: PIDSet, registry: ProcessRegistry): Boolean {
        for (pid in pidSet.toSet()) {
            val process = registry.get(pid)
            if (!process.isAlive()) {
                return false
            }
        }
        return true
    }

    /**
     * 获取 PID 集合中存在的 Actor
     * @param pidSet PID 集合
     * @param registry 进程注册表
     * @return 存在的 Actor 的 PID 集合
     */
    fun getExisting(pidSet: PIDSet, registry: ProcessRegistry): PIDSet {
        val result = PIDSet()
        for (pid in pidSet.toSet()) {
            val process = registry.get(pid)
            if (process.isAlive()) {
                result.add(pid)
            }
        }
        return result
    }

    /**
     * 获取 PID 集合中不存在的 Actor
     * @param pidSet PID 集合
     * @param registry 进程注册表
     * @return 不存在的 Actor 的 PID 集合
     */
    fun getNonExisting(pidSet: PIDSet, registry: ProcessRegistry): PIDSet {
        val result = PIDSet()
        for (pid in pidSet.toSet()) {
            val process = registry.get(pid)
            if (!process.isAlive()) {
                result.add(pid)
            }
        }
        return result
    }
}
