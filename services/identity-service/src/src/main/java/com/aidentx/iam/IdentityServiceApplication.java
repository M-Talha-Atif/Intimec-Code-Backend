package com.aidentx.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {
        "com.aidentx.iam",
        "com.aidentx.common",
        "com.aidentx.shared.dgraph",
        "com.aidentx.dal",
        "com.aidentx.iam.role_management.mapper",
        "com.aidentx.iam.entitlement_management.mapper",
        "com.aidentx.iam.policy.legacy.mapper",
        "com.aidentx.iam.identity_management.mapper",
        "com.aidentx.iam.shadow_management.mapper",
        "com.aidentx.iam.access_roles_management.mapper",
})
public class IdentityServiceApplication {
    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "log4j2.json");
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}