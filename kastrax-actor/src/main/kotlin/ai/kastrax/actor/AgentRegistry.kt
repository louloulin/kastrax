package ai.kastrax.actor

import actor.proto.PID
import java.util.concurrent.ConcurrentHashMap

/**
 * Agent 注册表，用于管理和发现 Agent
 */
class AgentRegistry {
    private val agents = ConcurrentHashMap<String, PID>()
    
    /**
     * 注册 Agent
     *
     * @param name Agent 名称
     * @param pid Agent 的 PID
     */
    fun register(name: String, pid: PID) {
        agents[name] = pid
    }
    
    /**
     * 注销 Agent
     *
     * @param name Agent 名称
     */
    fun unregister(name: String) {
        agents.remove(name)
    }
    
    /**
     * 查找 Agent
     *
     * @param name Agent 名称
     * @return Agent 的 PID，如果不存在则返回 null
     */
    fun lookup(name: String): PID? {
        return agents[name]
    }
    
    /**
     * 获取所有注册的 Agent
     *
     * @return 所有注册的 Agent 的映射表
     */
    fun getAllAgents(): Map<String, PID> {
        return agents.toMap()
    }
}
