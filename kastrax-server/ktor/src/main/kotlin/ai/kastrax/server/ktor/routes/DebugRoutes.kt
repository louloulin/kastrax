package ai.kastrax.server.ktor.routes

import ai.kastrax.server.common.api.DebugApi
import ai.kastrax.server.common.model.Breakpoint
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureDebugRoutes() {
    val debugApi by inject<DebugApi>()
    
    route("/debug") {
        route("/workflows/{workflowId}/breakpoints") {
            // 设置断点
            post {
                val workflowId = call.parameters["workflowId"] ?: throw IllegalArgumentException("Missing workflowId")
                val breakpoint = call.receive<Breakpoint>()
                val result = debugApi.setBreakpoint(workflowId, breakpoint).get()
                call.respond(HttpStatusCode.Created, result)
            }
            
            // 获取断点列表
            get {
                val workflowId = call.parameters["workflowId"] ?: throw IllegalArgumentException("Missing workflowId")
                val result = debugApi.getBreakpoints(workflowId).get()
                call.respond(HttpStatusCode.OK, result)
            }
            
            // 删除断点
            delete("/{breakpointId}") {
                val workflowId = call.parameters["workflowId"] ?: throw IllegalArgumentException("Missing workflowId")
                val breakpointId = call.parameters["breakpointId"] ?: throw IllegalArgumentException("Missing breakpointId")
                val result = debugApi.deleteBreakpoint(workflowId, breakpointId).get()
                if (result) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        
        route("/workflows/{workflowId}/sessions") {
            // 创建调试会话
            post {
                val workflowId = call.parameters["workflowId"] ?: throw IllegalArgumentException("Missing workflowId")
                val input = call.receive<Map<String, Any>>()
                val result = debugApi.createDebugSession(workflowId, input).get()
                call.respond(HttpStatusCode.Created, result)
            }
        }
        
        route("/sessions/{id}") {
            // 获取调试会话
            get {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
                val result = debugApi.getDebugSession(id).get()
                call.respond(HttpStatusCode.OK, result)
            }
            
            // 单步执行
            post("/step") {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
                val result = debugApi.stepExecution(id).get()
                call.respond(HttpStatusCode.OK, result)
            }
            
            // 继续执行
            post("/continue") {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
                val result = debugApi.continueExecution(id).get()
                call.respond(HttpStatusCode.OK, result)
            }
            
            // 停止调试会话
            delete {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
                val result = debugApi.stopDebugSession(id).get()
                if (result) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
