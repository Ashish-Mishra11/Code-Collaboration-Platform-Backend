package com.codesync.apigateway.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized Swagger / OpenAPI configuration for the API Gateway.
 *
 * The Gateway hosts a single Swagger UI at:
 *   http://localhost:8081/swagger-ui.html
 *
 * It aggregates the /v3/api-docs from every downstream microservice
 * via the gateway routes defined in application.properties.
 *
 * Each service's docs are available under:
 *   /v3/api-docs/auth-service
 *   /v3/api-docs/project-service
 *   /v3/api-docs/editor-service
 *   /v3/api-docs/collab-service
 *   /v3/api-docs/version-service
 *   /v3/api-docs/execution-service
 *   /v3/api-docs/payment-service
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeSync – Centralized API Documentation")
                        .description(
                                "Aggregated API documentation for all CodeSync microservices. " +
                                "Use the top-right dropdown in Swagger UI to switch between services. " +
                                "All endpoints require a JWT Bearer token (except auth & public routes). " +
                                "Click 'Authorize' and enter:  Bearer <your_token>")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CodeSync Team")
                                .email("support@codesync.dev"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. Example: eyJhbGciOiJ...")));
    }
}
