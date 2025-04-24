package ai.kastrax.server.quarkus.resource

import ai.kastrax.server.common.api.ExecutionApi
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.concurrent.CompletionStage

@Path("/executions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ExecutionResource {
    
    @Inject
    lateinit var executionApi: ExecutionApi
    
    @GET
    @Path("/{id}")
    fun getExecution(@PathParam("id") id: String): CompletionStage<Response> {
        return executionApi.getExecution(id)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.NOT_FOUND).build() }
    }
    
    @POST
    @Path("/{id}/cancel")
    fun cancelExecution(@PathParam("id") id: String): CompletionStage<Response> {
        return executionApi.cancelExecution(id)
            .thenApply { if (it) Response.noContent().build() else Response.status(Response.Status.NOT_FOUND).build() }
    }
    
    @POST
    @Path("/workflows/{workflowId}/execute")
    fun executeWorkflow(
        @PathParam("workflowId") workflowId: String,
        input: Map<String, Any>
    ): CompletionStage<Response> {
        return executionApi.executeWorkflow(workflowId, input)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.BAD_REQUEST).build() }
    }
    
    @GET
    @Path("/workflows/{workflowId}")
    fun getExecutionHistory(
        @PathParam("workflowId") workflowId: String,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("10") size: Int
    ): CompletionStage<Response> {
        return executionApi.getExecutionHistory(workflowId, page, size)
            .thenApply { Response.ok(it).build() }
    }
}
