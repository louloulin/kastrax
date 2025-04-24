package ai.kastrax.server.spring.controller

import ai.kastrax.server.common.api.DebugApi
import ai.kastrax.server.common.model.Breakpoint
import ai.kastrax.server.common.model.DebugSession
import ai.kastrax.server.common.model.Execution
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/debug")
class DebugController(private val debugApi: DebugApi) {
    
    @PostMapping("/workflows/{workflowId}/breakpoints")
    fun setBreakpoint(
        @PathVariable workflowId: String,
        @RequestBody breakpoint: Breakpoint
    ): CompletableFuture<ResponseEntity<Breakpoint>> {
        return debugApi.setBreakpoint(workflowId, breakpoint)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.badRequest().build() }
    }
    
    @GetMapping("/workflows/{workflowId}/breakpoints")
    fun getBreakpoints(@PathVariable workflowId: String): CompletableFuture<ResponseEntity<List<Breakpoint>>> {
        return debugApi.getBreakpoints(workflowId)
            .thenApply { ResponseEntity.ok(it) }
    }
    
    @DeleteMapping("/workflows/{workflowId}/breakpoints/{breakpointId}")
    fun deleteBreakpoint(
        @PathVariable workflowId: String,
        @PathVariable breakpointId: String
    ): CompletableFuture<ResponseEntity<Void>> {
        return debugApi.deleteBreakpoint(workflowId, breakpointId)
            .thenApply { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
    }
    
    @PostMapping("/workflows/{workflowId}/sessions")
    fun createDebugSession(
        @PathVariable workflowId: String,
        @RequestBody input: Map<String, Any>
    ): CompletableFuture<ResponseEntity<DebugSession>> {
        return debugApi.createDebugSession(workflowId, input)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.badRequest().build() }
    }
    
    @GetMapping("/sessions/{id}")
    fun getDebugSession(@PathVariable id: String): CompletableFuture<ResponseEntity<DebugSession>> {
        return debugApi.getDebugSession(id)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.notFound().build() }
    }
    
    @PostMapping("/sessions/{id}/step")
    fun stepExecution(@PathVariable id: String): CompletableFuture<ResponseEntity<Execution>> {
        return debugApi.stepExecution(id)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.badRequest().build() }
    }
    
    @PostMapping("/sessions/{id}/continue")
    fun continueExecution(@PathVariable id: String): CompletableFuture<ResponseEntity<Execution>> {
        return debugApi.continueExecution(id)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.badRequest().build() }
    }
    
    @DeleteMapping("/sessions/{id}")
    fun stopDebugSession(@PathVariable id: String): CompletableFuture<ResponseEntity<Void>> {
        return debugApi.stopDebugSession(id)
            .thenApply { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
    }
}
