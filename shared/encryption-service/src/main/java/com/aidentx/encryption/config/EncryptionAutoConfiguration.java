package com.aidentx.encryption.config;

import com.aidentx.encryption.repository.EncryptionKeyRepository;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.encryption.service.FieldEncryptionService;
import com.aidentx.encryption.service.KeyManagementService;
import com.aidentx.encryption.service.impl.EncryptionServiceImpl;
import com.aidentx.encryption.service.impl.FieldEncryptionServiceImpl;
import com.aidentx.encryption.service.impl.KeyManagementServiceImpl;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration(after = JpaRepositoriesAutoConfiguration.class)
@ConditionalOnClass(EncryptionKeyRepository.class)
@ConditionalOnProperty(prefix = "aidentx.encryption", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EncryptionStarterProperties.class)
@EntityScan("com.aidentx.encryption.model") // Tell consumer's JPA where to find our Entity
@EnableJpaRepositories("com.aidentx.encryption.repository") // Tell consumer where to find our Repository
public class EncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeyManagementService keyManagementService(EncryptionKeyRepository repository,
            EncryptionStarterProperties properties,
            UnifiedLoggerFactory loggerFactory) {
        return new KeyManagementServiceImpl(repository, properties, loggerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public EncryptionService encryptionService(KeyManagementService keyManagementService,
            UnifiedLoggerFactory loggerFactory) {
        return new EncryptionServiceImpl(keyManagementService, loggerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public FieldEncryptionService fieldEncryptionService(EncryptionService encryptionService,
            UnifiedLoggerFactory loggerFactory) {
        return new FieldEncryptionServiceImpl(encryptionService, new ObjectMapper(), loggerFactory);
    }
}