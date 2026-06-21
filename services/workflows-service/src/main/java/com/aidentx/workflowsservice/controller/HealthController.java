package com.aidentx.workflowsservice.controller;

import com.aidentx.workflowsservice.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Health Check Controller for Workflows Service
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.application.name:workflows-service}")
    private String serviceName;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put(HEALTH_STATUS, HEALTH_STATUS_UP);
        healthData.put(HEALTH_SERVICE, serviceName);
        healthData.put(HEALTH_VERSION, SERVICE_VERSION);
        
        return ResponseEntity.ok(ApiResponse.success(SUCCESS_SERVICE_HEALTHY, healthData));
    }

    @GetMapping("/workflows/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> workflowsHealth() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put(HEALTH_STATUS, HEALTH_STATUS_UP);
        healthData.put(HEALTH_SERVICE, serviceName);
        healthData.put(HEALTH_VERSION, SERVICE_VERSION);
        
        return ResponseEntity.ok(ApiResponse.success(SUCCESS_SERVICE_HEALTHY, healthData));
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok(PING_RESPONSE);
    }
}
