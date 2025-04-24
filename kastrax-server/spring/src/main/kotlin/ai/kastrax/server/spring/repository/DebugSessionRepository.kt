package ai.kastrax.server.spring.repository

import ai.kastrax.server.common.model.DebugSession
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * 调试会话存储库
 */
@Repository
class DebugSessionRepository {
    private val sessions = ConcurrentHashMap<String, DebugSession>()
    
    /**
     * 保存调试会话
     */
    fun save(session: DebugSession): DebugSession {
        sessions[session.id] = session
        return session
    }
    
    /**
     * 根据ID查找调试会话
     */
    fun findById(id: String): DebugSession? {
        return sessions[id]
    }
    
    /**
     * 根据工作流ID查找调试会话
     */
    fun findByWorkflowId(workflowId: String): List<DebugSession> {
        return sessions.values
            .filter { it.workflowId == workflowId }
            .toList()
    }
}
