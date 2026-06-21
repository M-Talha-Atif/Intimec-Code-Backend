package com.aidentx.taskmanagement.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI/Swagger configuration for the Task Management Service.
 *
 * <p>This configuration provides comprehensive API documentation with examples, security
 * definitions, and common request parameters.
 *
 * <p>Access the documentation at:
 *
 * <ul>
 *   <li>Swagger UI: http://localhost:8080/swagger-ui.html
 *   <li>OpenAPI JSON: http://localhost:8080/v3/api-docs
 * </ul>
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI taskManagementOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Task Management Service API")
                .description(
                    "# Task Management Service\n\n"
                        + "This service provides comprehensive task management capabilities including:\n"
                        + "- Task creation, retrieval, update, and deletion (CRUD)\n"
                        + "- Task scheduling and execution\n"
                        + "- Task lifecycle management\n"
                        + "- Application-scoped task organization")
                .version("1.0.0")
                .contact(new Contact().name("Intimec Development Team").email("dev@intimec.com"))
                .license(new License().name("Proprietary").url("https://intimec.com/license")))
        .servers(
            List.of(
                new Server().url("http://localhost:8086").description("Local Development Server")))
        .components(
            new Components()
                // Define reusable security schemes
                .addSecuritySchemes(
                    "tenantId",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Tenant-ID")
                        .description("Tenant identifier for multi-tenancy isolation"))
                .addSecuritySchemes(
                    "userId",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-User-ID")
                        .description("User identifier for audit tracking"))
                // Define reusable parameters
                .addParameters(
                    "tenantIdHeader",
                    new Parameter()
                        .in("header")
                        .name("X-Tenant-ID")
                        .description("Tenant identifier UUID")
                        .required(true)
                        .schema(
                            new StringSchema()
                                .format("uuid")
                                .example("123e4567-e89b-12d3-a456-426614174000")))
                .addParameters(
                    "userIdHeader",
                    new Parameter()
                        .in("header")
                        .name("X-User-ID")
                        .description("User identifier UUID")
                        .required(true)
                        .schema(
                            new StringSchema()
                                .format("uuid")
                                .example("789e4567-e89b-12d3-a456-426614174111")))
                .addParameters(
                    "applicationIdPath",
                    new Parameter()
                        .in("path")
                        .name("applicationId")
                        .description("Application identifier UUID")
                        .required(true)
                        .schema(
                            new StringSchema()
                                .format("uuid")
                                .example("550e8400-e29b-41d4-a716-446655440000")))
                .addParameters(
                    "taskIdPath",
                    new Parameter()
                        .in("path")
                        .name("taskId")
                        .description("Task identifier UUID")
                        .required(true)
                        .schema(
                            new StringSchema()
                                .format("uuid")
                                .example("abc12345-e29b-41d4-a716-446655440001"))));
  }
}
