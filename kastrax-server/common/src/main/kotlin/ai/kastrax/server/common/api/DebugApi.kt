package ai.kastrax.server.common.api

import ai.kastrax.server.common.model.Breakpoint
import ai.kastrax.server.common.model.DebugSession
import ai.kastrax.server.common.model.Execution
import java.util.concurrent.CompletableFuture

/**
 * 调试API接口
 */
interface DebugApi {
    /**
     * 设置断点
     */
    fun setBreakpoint(workflowId: String, breakpoint: Breakpoint): CompletableFuture<Breakpoint>
    
    /**
     * 获取断点
     */
    fun getBreakpoints(workflowId: String): CompletableFuture<List<Breakpoint>>
    
    /**
     * 删除断点
     */
    fun deleteBreakpoint(workflowId: String, breakpointId: String): CompletableFuture<Boolean>
    
    /**
     * 创建调试会话
     */
    fun createDebugSession(workflowId: String, input: Map<String, Any>): CompletableFuture<DebugSession>
    
    /**
     * 获取调试会话
     */
    fun getDebugSession(id: String): CompletableFuture<DebugSession>
    
    /**
     * 单步执行
     */
    fun stepExecution(sessionId: String): CompletableFuture<Execution>
    
    /**
     * 继续执行
     */
    fun continueExecution(sessionId: String): CompletableFuture<Execution>
    
    /**
     * 停止调试会话
     */
    fun stopDebugSession(id: String): CompletableFuture<Boolean>
}
