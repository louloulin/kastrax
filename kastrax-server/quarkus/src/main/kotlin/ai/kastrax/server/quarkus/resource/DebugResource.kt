package ai.kastrax.server.quarkus.resource

import ai.kastrax.server.common.api.DebugApi
import ai.kastrax.server.common.model.Breakpoint
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.concurrent.CompletionStage

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class DebugResource {
    
    @Inject
    lateinit var debugApi: DebugApi
    
    @POST
    @Path("/workflows/{workflowId}/breakpoints")
    fun setBreakpoint(
        @PathParam("workflowId") workflowId: String,
        breakpoint: Breakpoint
    ): CompletionStage<Response> {
        return debugApi.setBreakpoint(workflowId, breakpoint)
            .thenApply { Response.status(Response.Status.CREATED).entity(it).build() }
            .exceptionally { Response.status(Response.Status.BAD_REQUEST).build() }
    }
    
    @GET
    @Path("/workflows/{workflowId}/breakpoints")
    fun getBreakpoints(@PathParam("workflowId") workflowId: String): CompletionStage<Response> {
        return debugApi.getBreakpoints(workflowId)
            .thenApply { Response.ok(it).build() }
    }
    
    @DELETE
    @Path("/workflows/{workflowId}/breakpoints/{breakpointId}")
    fun deleteBreakpoint(
        @PathParam("workflowId") workflowId: String,
        @PathParam("breakpointId") breakpointId: String
    ): CompletionStage<Response> {
        return debugApi.deleteBreakpoint(workflowId, breakpointId)
            .thenApply { if (it) Response.noContent().build() else Response.status(Response.Status.NOT_FOUND).build() }
    }
    
    @POST
    @Path("/workflows/{workflowId}/sessions")
    fun createDebugSession(
        @PathParam("workflowId") workflowId: String,
        input: Map<String, Any>
    ): CompletionStage<Response> {
        return debugApi.createDebugSession(workflowId, input)
            .thenApply { Response.status(Response.Status.CREATED).entity(it).build() }
            .exceptionally { Response.status(Response.Status.BAD_REQUEST).build() }
    }
    
    @GET
    @Path("/sessions/{id}")
    fun getDebugSession(@PathParam("id") id: String): CompletionStage<Response> {
        return debugApi.getDebugSession(id)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.NOT_FOUND).build() }
    }
    
    @POST
    @Path("/sessions/{id}/step")
    fun stepExecution(@PathParam("id") id: String): CompletionStage<Response> {
        return debugApi.stepExecution(id)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.BAD_REQUEST).build() }
    }
    
    @POST
    @Path("/sessions/{id}/continue")
    fun continueExecution(@PathParam("id") id: String): CompletionStage<Response> {
        return debugApi.continueExecution(id)
            .thenApply { Response.ok(it).build() }
            .exceptionally { Response.status(Response.Status.BAD_REQUEST).build() }
    }
    
    @DELETE
    @Path("/sessions/{id}")
    fun stopDebugSession(@PathParam("id") id: String): CompletionStage<Response> {
        return debugApi.stopDebugSession(id)
            .thenApply { if (it) Response.noContent().build() else Response.status(Response.Status.NOT_FOUND).build() }
    }
}
