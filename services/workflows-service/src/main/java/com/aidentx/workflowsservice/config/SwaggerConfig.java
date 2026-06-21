package com.aidentx.workflowsservice.config;

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
 * Swagger/OpenAPI Configuration for Workflows Service
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8098}")
    private String serverPort;

    @Bean
    public OpenAPI workflowsServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("AidentX Team");
        contact.setEmail("support@aidentx.com");

        License license = new License()
                .name("Proprietary")
                .url("https://aidentx.com/license");

        Info info = new Info()
                .title("Workflows Service API")
                .version("1.0.0")
                .description("""
                    Production-grade workflow execution engine with visual workflow designer support.
                    
                    **Features:**
                    - Create and manage workflows with visual graph structure
                    - Execute workflows with context and approval flows
                    - Template-based workflow creation
                    - Version control and drift detection
                    - Risk scoring and compliance tracking
                    
                    **Current Phase:** Phase 2 - CRUD Operations
                    
                    **Available Endpoints:**
                    - Workflow CRUD (Create, Read, Update, Delete)
                    - Workflow duplication
                    - Pagination and filtering
                    
                    **Coming Soon (Phase 3):**
                    - Workflow execution engine
                    - Approval workflows
                    - Real-time monitoring
                    """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
