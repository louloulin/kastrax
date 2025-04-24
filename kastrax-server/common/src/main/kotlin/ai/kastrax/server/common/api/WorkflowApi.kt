package ai.kastrax.server.common.api

import ai.kastrax.server.common.model.Workflow
import java.util.concurrent.CompletableFuture

/**
 * 工作流API接口
 */
interface WorkflowApi {
    /**
     * 创建工作流
     */
    fun createWorkflow(workflow: Workflow): CompletableFuture<Workflow>
    
    /**
     * 获取工作流
     */
    fun getWorkflow(id: String): CompletableFuture<Workflow>
    
    /**
     * 更新工作流
     */
    fun updateWorkflow(id: String, workflow: Workflow): CompletableFuture<Workflow>
    
    /**
     * 删除工作流
     */
    fun deleteWorkflow(id: String): CompletableFuture<Boolean>
    
    /**
     * 获取工作流列表
     */
    fun getWorkflows(page: Int, size: Int, filter: Map<String, String>): CompletableFuture<List<Workflow>>
}
