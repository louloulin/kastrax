package ai.kastrax.server.ktor.plugins

import ai.kastrax.server.common.config.JacksonConfig
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import com.fasterxml.jackson.databind.ObjectMapper

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        // 使用 kotlinx.serialization
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })

        // 使用 Jackson
        jackson {
            registerModule(JacksonConfig().createModule())
        }
    }
}
