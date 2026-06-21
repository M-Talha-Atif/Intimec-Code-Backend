package com.aidentx.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@EntityScan(basePackages = {"com.aidentx.configuration.model.entity"})
@ComponentScan(basePackages = "com.aidentx.configuration")
public class ConfigurationServiceApplication {

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "log4j2.json");
        SpringApplication.run(ConfigurationServiceApplication.class, args);
    }
}