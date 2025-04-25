package ai.kastrax.server.quarkus.config

import ai.kastrax.server.common.config.JacksonConfig
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.jackson.ObjectMapperCustomizer
import jakarta.inject.Singleton

/**
 * Quarkus Jackson 配置提供者
 */
@Singleton
class JacksonConfigProvider : ObjectMapperCustomizer {
    override fun customize(objectMapper: ObjectMapper) {
        objectMapper.registerModule(JacksonConfig().createModule())
    }
}
