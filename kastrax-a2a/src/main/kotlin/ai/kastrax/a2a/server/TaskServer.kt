package ai.kastrax.a2a.server

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.model.*
import ai.kastrax.a2a.task.TaskManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
// 注释掉 SSE 相关导入，因为当前版本的 Ktor 可能不支持
// import io.ktor.sse.ServerSentEvent
// import io.ktor.sse.respondSse
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 配置任务服务器路由
 */
fun Route.configureTaskRoutes(agents: Map<String, A2AAgent>, taskManager: TaskManager) {
    route("/a2a/v1/tasks") {
        // 发送任务
        post("/send") {
            try {
                val request = call.receive<SendTaskRequest>()
                val agentId = request.params.metadata?.get("agent_id") ?: "default"
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found: $agentId")

                val task = taskManager.onSendTask(agent, request.params)
                val response = SendTaskResponse(responseId = request.requestId, result = task)

                call.respond(response)
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }

        // 发送任务并订阅更新
        post("/sendSubscribe") {
            try {
                val request = call.receive<SendTaskStreamingRequest>()
                val agentId = request.params.metadata?.get("agent_id") ?: "default"
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found: $agentId")

                val eventFlow = taskManager.onSendTaskSubscribe(agent, request.params)

                // 注释掉 SSE 相关代码，因为当前版本的 Ktor 可能不支持
                // call.respondSse {
                //     eventFlow.map { event ->
                //         val json = Json.encodeToString(event)
                //         ServerSentEvent(data = json)
                //     }.collect { event ->
                //         send(event)
                //     }
                // }

                // 使用普通响应替代
                call.respond(HttpStatusCode.OK, "Streaming not supported in this version")
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }

        // 获取任务
        post("/get") {
            try {
                val request = call.receive<GetTaskRequest>()
                val task = taskManager.getTask(request.params.id)
                    ?: throw NotFoundException("Task not found: ${request.params.id}")

                val response = GetTaskResponse(responseId = request.requestId, result = task)
                call.respond(response)
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }

        // 取消任务
        post("/cancel") {
            try {
                val request = call.receive<CancelTaskRequest>()
                val task = taskManager.cancelTask(request.params.id)
                    ?: throw NotFoundException("Task not found: ${request.params.id}")

                val response = CancelTaskResponse(responseId = request.requestId, result = task)
                call.respond(response)
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }

        // 设置任务推送通知
        post("/pushNotification/set") {
            try {
                val request = call.receive<SetTaskPushNotificationRequest>()
                val config = taskManager.setTaskPushNotification(
                    request.params.id,
                    request.params.pushNotificationConfig
                )

                val response = SetTaskPushNotificationResponse(responseId = request.requestId, result = config)
                call.respond(response)
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }

        // 获取任务推送通知
        post("/pushNotification/get") {
            try {
                val request = call.receive<GetTaskPushNotificationRequest>()
                val config = taskManager.getTaskPushNotification(request.params.id)
                    ?: throw NotFoundException("Push notification config not found for task: ${request.params.id}")

                val response = GetTaskPushNotificationResponse(responseId = request.requestId, result = config)
                call.respond(response)
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }

        // 重新订阅任务
        post("/resubscribe") {
            try {
                val request = call.receive<TaskResubscriptionRequest>()
                val task = taskManager.getTask(request.params.id)
                    ?: throw NotFoundException("Task not found: ${request.params.id}")

                // 创建任务状态更新事件流
                // 注释掉 SSE 相关代码，因为当前版本的 Ktor 可能不支持
                // call.respondSse {
                //     taskManager.taskStatusEvents
                //         .map { event ->
                //             if (event.id == request.params.id) {
                //                 val json = Json.encodeToString(event)
                //                 ServerSentEvent(data = json)
                //             } else {
                //                 null
                //             }
                //         }
                //         .collect { event ->
                //             if (event != null) {
                //                 send(event)
                //             }
                //         }
                // }

                // 使用普通响应替代
                call.respond(HttpStatusCode.OK, "Streaming not supported in this version")
            } catch (e: Exception) {
                val error = when (e) {
                    is NotFoundException -> JSONRPCError(code = JSONRPCErrorCodes.TASK_NOT_FOUND, message = e.message ?: "Task not found")
                    else -> JSONRPCError(code = JSONRPCErrorCodes.INTERNAL_ERROR, message = e.message ?: "Unknown error")
                }

                call.respond(
                    HttpStatusCode.BadRequest,
                    JSONRPCResponse(responseId = null, error = error)
                )
            }
        }
    }
}
