package ai.kastrax.server.quarkus.repository

import ai.kastrax.server.common.model.Execution
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap

/**
 * 执行存储库
 */
@ApplicationScoped
class ExecutionRepository {
    private val executions = ConcurrentHashMap<String, Execution>()
    
    /**
     * 保存执行记录
     */
    fun save(execution: Execution): Execution {
        executions[execution.id] = execution
        return execution
    }
    
    /**
     * 根据ID查找执行记录
     */
    fun findById(id: String): Execution? {
        return executions[id]
    }
    
    /**
     * 根据工作流ID查找执行记录
     */
    fun findByWorkflowId(workflowId: String, page: Int, size: Int): List<Execution> {
        return executions.values
            .filter { it.workflowId == workflowId }
            .sortedByDescending { it.startTime }
            .drop(page * size)
            .take(size)
            .toList()
    }
}
