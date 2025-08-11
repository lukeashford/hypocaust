package com.example.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for The Machine API. Provides comprehensive API documentation with proper
 * metadata.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("The Machine API")
            .description(
                "A powerful AI-driven system for brand intelligence and analysis using LangChain4j")
            .version("1.0.0")
            .contact(new Contact()
                .name("The Machine Team")
                .email("support@themachine.com")
                .url("https://github.com/themachine/api"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")))
        .servers(List.of(
            new Server()
                .url("http://localhost:8080")
                .description("Development server"),
            new Server()
                .url("https://api.themachine.com")
                .description("Production server")));
  }
}