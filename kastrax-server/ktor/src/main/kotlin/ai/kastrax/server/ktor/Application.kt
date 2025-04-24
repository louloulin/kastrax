package ai.kastrax.server.ktor

import ai.kastrax.server.ktor.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureCORS()
    configureSwagger()
    configureStatusPages()
    configureDependencyInjection()
    configureRouting()
}
