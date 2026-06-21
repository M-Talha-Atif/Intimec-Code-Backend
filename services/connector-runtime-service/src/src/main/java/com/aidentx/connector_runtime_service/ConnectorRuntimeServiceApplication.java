package com.aidentx.connector_runtime_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aidentx.connector_runtime_service", "com.aidentx.dal.dal_core"})
@ConfigurationPropertiesScan("com.aidentx.connector_runtime_service.config")
@EnableScheduling
public class ConnectorRuntimeServiceApplication {
    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "log4j2.json");
        SpringApplication.run(ConnectorRuntimeServiceApplication.class, args);
    }
}