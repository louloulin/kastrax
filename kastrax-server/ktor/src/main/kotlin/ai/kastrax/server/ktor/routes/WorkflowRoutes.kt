package ai.kastrax.server.ktor.routes

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Workflow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureWorkflowRoutes() {
    val workflowApi by inject<WorkflowApi>()
    
    route("/workflows") {
        // 创建工作流
        post {
            val workflow = call.receive<Workflow>()
            val result = workflowApi.createWorkflow(workflow).get()
            call.respond(HttpStatusCode.Created, result)
        }
        
        // 获取工作流列表
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
            val filter = call.request.queryParameters.entries()
                .filter { it.key != "page" && it.key != "size" }
                .associate { it.key to it.value.first() }
            
            val result = workflowApi.getWorkflows(page, size, filter).get()
            call.respond(HttpStatusCode.OK, result)
        }
        
        // 获取工作流
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val result = workflowApi.getWorkflow(id).get()
            call.respond(HttpStatusCode.OK, result)
        }
        
        // 更新工作流
        put("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val workflow = call.receive<Workflow>()
            val result = workflowApi.updateWorkflow(id, workflow).get()
            call.respond(HttpStatusCode.OK, result)
        }
        
        // 删除工作流
        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val result = workflowApi.deleteWorkflow(id).get()
            if (result) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
