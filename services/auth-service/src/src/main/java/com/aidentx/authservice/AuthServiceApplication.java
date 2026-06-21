package com.aidentx.authservice;

import com.aidentx.authservice.config.IdpConfigProperties;
import com.aidentx.authservice.config.PasswordExpirationNotificationConfig;
import com.aidentx.authservice.config.PasswordPolicyExpirationConfig;
import com.aidentx.logging.config.UnifiedLoggingAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {GrpcServerSecurityAutoConfiguration.class})
@EnableConfigurationProperties({
    IdpConfigProperties.class,
    PasswordPolicyExpirationConfig.class,
    PasswordExpirationNotificationConfig.class
})
@Import(UnifiedLoggingAutoConfiguration.class)
@EntityScan(basePackages = {"com.aidentx.authservice.model.entity"})
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {

        System.setProperty("log4j.configurationFile", "log4j2.json");
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
