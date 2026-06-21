package com.aidentx.workflowsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Workflows Service - Production-grade workflow execution engine
 * 
 * Provides visual workflow designer support with:
 * - Stateless workflow execution engine
 * - Node executor plugin system
 * - Multi-stage approval workflows
 * - Integration with identity services
 * - Real-time execution monitoring
 * - Template management and versioning
 * - Event-level deduplication with automatic cleanup
 */
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling // Enable scheduled tasks (e.g., processed events cleanup)
public class WorkflowsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowsServiceApplication.class, args);
    }
}
