package com.aidentx.access_portal_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI configuration for Access Portal Service
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI accessPortalOpenAPI() {

        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("AidentX Team")
                .email("support@aidentx.com");

        License license = new License()
                .name("Proprietary")
                .url("https://aidentx.com/license");

        Info info = new Info()
                .title("Access Portal Service API")
                .version("1.0.0")
                .description("""
                        IAM Access Portal microservice responsible for managing
                        user access requests and approvals.

                        **Features:**
                        - Submit access requests for applications and roles
                        - Approve or reject access requests
                        - Track access request status
                        - Audit-friendly access lifecycle

                        **Current Phase:** Phase 1 – REST API

                        **Available Endpoints:**
                        - Access request creation
                        - Access request approval / rejection
                        - Access request listing and status tracking

                        **Planned (Next Phases):**
                        - Integration with Identity and Policy services
                        - Event publishing via Kafka
                        - Time-bound access and auto-expiry
                        """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}