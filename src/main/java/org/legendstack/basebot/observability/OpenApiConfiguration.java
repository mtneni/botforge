package org.legendstack.basebot.observability;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for BotForge.
 * Accessible at /swagger-ui.html in dev mode.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI botForgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BotForge API")
                        .description("API for BotForge — an AI chatbot platform with RAG, semantic memory, " +
                                "persona studio, and multi-model orchestration.")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("LegendStack")
                                .url("https://github.com/legendstack/botforge"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
