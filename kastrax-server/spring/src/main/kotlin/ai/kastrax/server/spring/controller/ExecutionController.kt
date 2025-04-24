package ai.kastrax.server.spring.controller

import ai.kastrax.server.common.api.ExecutionApi
import ai.kastrax.server.common.model.Execution
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/executions")
class ExecutionController(private val executionApi: ExecutionApi) {
    
    @PostMapping("/workflows/{workflowId}/execute")
    fun executeWorkflow(
        @PathVariable workflowId: String,
        @RequestBody input: Map<String, Any>
    ): CompletableFuture<ResponseEntity<Execution>> {
        return executionApi.executeWorkflow(workflowId, input)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.badRequest().build() }
    }
    
    @GetMapping("/{id}")
    fun getExecution(@PathVariable id: String): CompletableFuture<ResponseEntity<Execution>> {
        return executionApi.getExecution(id)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.notFound().build() }
    }
    
    @PostMapping("/{id}/cancel")
    fun cancelExecution(@PathVariable id: String): CompletableFuture<ResponseEntity<Void>> {
        return executionApi.cancelExecution(id)
            .thenApply { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
    }
    
    @GetMapping("/workflows/{workflowId}")
    fun getExecutionHistory(
        @PathVariable workflowId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): CompletableFuture<ResponseEntity<List<Execution>>> {
        return executionApi.getExecutionHistory(workflowId, page, size)
            .thenApply { ResponseEntity.ok(it) }
    }
}
