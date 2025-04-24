package ai.kastrax.server.quarkus.repository

import ai.kastrax.server.common.model.Breakpoint
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap

/**
 * 断点存储库
 */
@ApplicationScoped
class BreakpointRepository {
    private val breakpoints = ConcurrentHashMap<String, Breakpoint>()
    
    /**
     * 保存断点
     */
    fun save(breakpoint: Breakpoint): Breakpoint {
        breakpoints[breakpoint.id] = breakpoint
        return breakpoint
    }
    
    /**
     * 根据ID查找断点
     */
    fun findById(id: String): Breakpoint? {
        return breakpoints[id]
    }
    
    /**
     * 根据工作流ID查找断点
     */
    fun findByWorkflowId(workflowId: String): List<Breakpoint> {
        return breakpoints.values
            .filter { it.workflowId == workflowId }
            .toList()
    }
    
    /**
     * 删除断点
     */
    fun deleteById(id: String): Boolean {
        return breakpoints.remove(id) != null
    }
}
