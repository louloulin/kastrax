package ai.kastrax.core.agent

import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * 内存中的状态管理器实现
 */
class InMemoryStateManager : StateManager, KastraXBase(component = "STATE_MANAGER", name = "in-memory") {
    private val statesMutex = Mutex()
    private val states = mutableMapOf<String, AgentState>()
    
    override suspend fun saveState(state: AgentState): AgentState {
        val updatedState = state.copy(lastUpdated = Clock.System.now())
        
        statesMutex.withLock {
            states[state.id] = updatedState
        }
        
        logger.debug("保存状态: ${state.id}")
        return updatedState
    }
    
    override suspend fun getState(stateId: String): AgentState? {
        return statesMutex.withLock {
            states[stateId]
        }
    }
    
    override suspend fun getStateByThread(threadId: String): AgentState? {
        return statesMutex.withLock {
            states.values.firstOrNull { it.threadId == threadId }
        }
    }
    
    override suspend fun getStatesByResource(resourceId: String): List<AgentState> {
        return statesMutex.withLock {
            states.values.filter { it.resourceId == resourceId }
        }
    }
    
    override suspend fun deleteState(stateId: String): Boolean {
        statesMutex.withLock {
            states.remove(stateId)
        }
        
        logger.debug("删除状态: $stateId")
        return true
    }
}
