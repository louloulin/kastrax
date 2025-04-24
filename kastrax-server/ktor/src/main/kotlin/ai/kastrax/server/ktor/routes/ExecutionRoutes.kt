package ai.kastrax.server.ktor.routes

import ai.kastrax.server.common.api.ExecutionApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureExecutionRoutes() {
    val executionApi by inject<ExecutionApi>()
    
    route("/executions") {
        // 获取执行状态
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val result = executionApi.getExecution(id).get()
            call.respond(HttpStatusCode.OK, result)
        }
        
        // 取消执行
        post("/{id}/cancel") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val result = executionApi.cancelExecution(id).get()
            if (result) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    
    route("/workflows/{workflowId}/executions") {
        // 执行工作流
        post {
            val workflowId = call.parameters["workflowId"] ?: throw IllegalArgumentException("Missing workflowId")
            val input = call.receive<Map<String, Any>>()
            val result = executionApi.executeWorkflow(workflowId, input).get()
            call.respond(HttpStatusCode.OK, result)
        }
        
        // 获取执行历史
        get {
            val workflowId = call.parameters["workflowId"] ?: throw IllegalArgumentException("Missing workflowId")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
            
            val result = executionApi.getExecutionHistory(workflowId, page, size).get()
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
