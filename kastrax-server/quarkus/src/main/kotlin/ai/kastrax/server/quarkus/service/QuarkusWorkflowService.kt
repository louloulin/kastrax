package ai.kastrax.server.quarkus.service

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Workflow
import ai.kastrax.server.quarkus.repository.IWorkflowRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class QuarkusWorkflowService : WorkflowApi {

    @Inject
    lateinit var workflowRepository: IWorkflowRepository

    override fun createWorkflow(workflow: Workflow): CompletableFuture<Workflow> {
        val newWorkflow = workflow.copy(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return CompletableFuture.completedFuture(workflowRepository.save(newWorkflow))
    }

    override fun getWorkflow(id: String): CompletableFuture<Workflow> {
        return CompletableFuture.completedFuture(workflowRepository.findById(id)
            ?: throw NoSuchElementException("Workflow not found: $id"))
    }

    override fun updateWorkflow(id: String, workflow: Workflow): CompletableFuture<Workflow> {
        val existingWorkflow = workflowRepository.findById(id)
            ?: throw NoSuchElementException("Workflow not found: $id")

        val updatedWorkflow = workflow.copy(
            id = existingWorkflow.id,
            createdAt = existingWorkflow.createdAt,
            updatedAt = Instant.now()
        )

        return CompletableFuture.completedFuture(workflowRepository.save(updatedWorkflow))
    }

    override fun deleteWorkflow(id: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(workflowRepository.deleteById(id))
    }

    override fun getWorkflows(page: Int, size: Int, filter: Map<String, String>): CompletableFuture<List<Workflow>> {
        return CompletableFuture.completedFuture(workflowRepository.findAll(page, size, filter))
    }
}
