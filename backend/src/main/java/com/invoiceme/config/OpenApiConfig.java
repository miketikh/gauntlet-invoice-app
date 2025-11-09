package com.invoiceme.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("InvoiceMe API")
                .version("1.0.0")
                .description("RESTful API for ERP invoicing system with CQRS pattern. " +
                    "Separates command operations (POST/PUT/DELETE) from query operations (GET) " +
                    "for improved scalability and performance.")
                .contact(new Contact()
                    .name("InvoiceMe Team")
                    .email("support@invoiceme.com")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT authentication token. Obtain by calling POST /api/auth/login with valid credentials.")));
    }
}
