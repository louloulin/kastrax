package ai.kastrax.server.spring.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI配置
 */
@Configuration
class OpenApiConfig {
    
    /**
     * 配置OpenAPI
     */
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("KastraX Server API")
                    .description("KastraX Server API Documentation")
                    .version("v1")
            )
    }
}
