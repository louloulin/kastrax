package ai.kastrax.server.spring.controller

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Workflow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/workflows")
class WorkflowController(private val workflowApi: WorkflowApi) {

    @PostMapping
    fun createWorkflow(@RequestBody workflow: Workflow): CompletableFuture<ResponseEntity<Workflow>> {
        return workflowApi.createWorkflow(workflow)
            .thenApply { ResponseEntity.status(201).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(it) }
    }

    @GetMapping("/{id}")
    fun getWorkflow(@PathVariable id: String): CompletableFuture<ResponseEntity<Workflow>> {
        return workflowApi.getWorkflow(id)
            .thenApply { ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(it) }
            .exceptionally { ResponseEntity.notFound().build() }
    }

    @PutMapping("/{id}")
    fun updateWorkflow(@PathVariable id: String, @RequestBody workflow: Workflow): CompletableFuture<ResponseEntity<Workflow>> {
        return workflowApi.updateWorkflow(id, workflow)
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { ResponseEntity.notFound().build() }
    }

    @DeleteMapping("/{id}")
    fun deleteWorkflow(@PathVariable id: String): CompletableFuture<ResponseEntity<Void>> {
        return workflowApi.deleteWorkflow(id)
            .thenApply { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
    }

    @GetMapping
    fun getWorkflows(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam filter: Map<String, String>
    ): CompletableFuture<ResponseEntity<List<Workflow>>> {
        return workflowApi.getWorkflows(page, size, filter)
            .thenApply { ResponseEntity.ok(it) }
    }
}
