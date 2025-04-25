package ai.kastrax.server.spring.config

import ai.kastrax.server.common.config.JacksonConfig as CommonJacksonConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Jackson 配置
 */
@Configuration
class JacksonConfig {
    /**
     * 配置 ObjectMapper
     */
    @Bean
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(CommonJacksonConfig().createModule())
        return objectMapper
    }
}
