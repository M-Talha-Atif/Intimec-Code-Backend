package com.aidentx.applicationpermissions.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Application Permissions Service.
 * 
 * <p>Configures Swagger UI documentation with JWT authentication support
 * and comprehensive API information for role management endpoints.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8095}")
    private String serverPort;

    @Value("${swagger.server.url:http://localhost}")
    private String serverUrl;

    /**
     * Configures OpenAPI documentation with security schemes and API information.
     * 
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI applicationPermissionsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Application Permissions Service API")
                        .description("REST API for managing roles and application permissions with multi-tenant support")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AidentX Development Team")
                                .email("dev@aidentx.com")
                                .url("https://aidentx.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://aidentx.com/license")))
                .servers(List.of(
                        new Server()
                                .url(serverUrl + ":" + serverPort)
                                .description("Application Permissions Service - Direct Access"),
                        new Server()
                                .url("http://localhost:8181")
                                .description("Application Permissions Service - API Gateway")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. " +
                                                    "Example: \"Authorization: Bearer {token}\"")));
    }
}