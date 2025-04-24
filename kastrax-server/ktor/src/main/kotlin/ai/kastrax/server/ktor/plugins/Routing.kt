package ai.kastrax.server.ktor.plugins

import ai.kastrax.server.ktor.routes.configureDebugRoutes
import ai.kastrax.server.ktor.routes.configureExecutionRoutes
import ai.kastrax.server.ktor.routes.configureWorkflowRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api") {
            get("/health") {
                call.respondText("OK")
            }
            
            configureWorkflowRoutes()
            configureExecutionRoutes()
            configureDebugRoutes()
        }
    }
}
