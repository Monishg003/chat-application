package com.encipherhealth.chatapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig
{
    @Bean
    public OpenAPI chatApplicationOpenAPI()
    {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat Application API")
                        .version("1.0")
                        .description("Redis-backed chat rooms, messaging, history, and real-time Pub/Sub updates."));
    }
}
