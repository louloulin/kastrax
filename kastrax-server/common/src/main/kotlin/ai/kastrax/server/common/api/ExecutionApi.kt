package ai.kastrax.server.common.api

import ai.kastrax.server.common.model.Execution
import java.util.concurrent.CompletableFuture

/**
 * 执行API接口
 */
interface ExecutionApi {
    /**
     * 执行工作流
     */
    fun executeWorkflow(workflowId: String, input: Map<String, Any>): CompletableFuture<Execution>
    
    /**
     * 获取执行状态
     */
    fun getExecution(id: String): CompletableFuture<Execution>
    
    /**
     * 取消执行
     */
    fun cancelExecution(id: String): CompletableFuture<Boolean>
    
    /**
     * 获取执行历史
     */
    fun getExecutionHistory(workflowId: String, page: Int, size: Int): CompletableFuture<List<Execution>>
}
