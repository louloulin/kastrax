package ai.kastrax.server.quarkus.resource

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Workflow
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.concurrent.CompletionStage

@Path("/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class WorkflowResource {
    
    @Inject
    lateinit var workflowApi: WorkflowApi
    
    @POST
    fun createWorkflow(workflow: Workflow): CompletionStage<Response> {
        return workflowApi.createWorkflow(workflow)
            .thenApply { Response.status(Response.Status.CREATED).entity(it).build() }
    }
    
    @GET
    fun getWorkflows(
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("10") size: Int,
        @QueryParam("filter") filter: Map<String, String>
    ): CompletionStage<Response> {
        return workflowApi.getWorkflows(page, size, filter)
            .thenApply { Response.ok(it).build() }
    }
    
    @GET
    @Path("/{id}")
    fun getWorkflow(@PathParam("id") id: String): CompletionStage<Response> {
        return workflowApi.getWorkflow(id)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.NOT_FOUND).build() }
    }
    
    @PUT
    @Path("/{id}")
    fun updateWorkflow(@PathParam("id") id: String, workflow: Workflow): CompletionStage<Response> {
        return workflowApi.updateWorkflow(id, workflow)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.NOT_FOUND).build() }
    }
    
    @DELETE
    @Path("/{id}")
    fun deleteWorkflow(@PathParam("id") id: String): CompletionStage<Response> {
        return workflowApi.deleteWorkflow(id)
            .thenApply { if (it) Response.noContent().build() else Response.status(Response.Status.NOT_FOUND).build() }
    }
}
