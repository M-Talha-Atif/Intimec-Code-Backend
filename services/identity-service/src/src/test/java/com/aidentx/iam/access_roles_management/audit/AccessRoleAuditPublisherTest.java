package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAuditPublisherTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleAuditPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAuditPublisher.class), anyString())).thenReturn(logger);
        publisher = new AccessRoleAuditPublisher(kafkaTemplate, objectMapper, loggerFactory);
    }

    @ParameterizedTest
    @CsvSource({
            "low, low",
            "medium, medium",
            "high, high"
    })
    void deletedRoleKafkaAuditRiskComesFromRoleRiskLevel(String roleRiskLevel, String expectedAuditRisk) {
        UUID roleId = UUID.randomUUID();
        AccessRoleAuditEvent event = AccessRoleAuditEvent.roleDeleted(
                UUID.randomUUID().toString(),
                "john.doe",
                TENANT_ID,
                roleId.toString(),
                "Finance",
                "business",
                roleRiskLevel,
                "Finance",
                UUID.randomUUID().toString(),
                objectMapper.createObjectNode().put("riskLevel", roleRiskLevel)
        );

        publisher.publish(event);

        ArgumentCaptor<Object> kafkaEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(
                eq(AppConstants.AUDIT_TOPIC_NAME),
                eq(roleId.toString()),
                kafkaEventCaptor.capture());

        assertThat(kafkaEventCaptor.getValue()).isInstanceOf(AuditLogKafkaEvent.class);
        AuditLogKafkaEvent kafkaEvent = (AuditLogKafkaEvent) kafkaEventCaptor.getValue();
        assertThat(kafkaEvent.getRisk()).isEqualTo(expectedAuditRisk);
        assertThat(kafkaEvent.getDetails().get(AccessRoleAuditConstants.DETAIL_RISK_LEVEL).asText())
                .isEqualTo(roleRiskLevel);
        assertThat(kafkaEvent.getMetadata().get(AccessRoleAuditConstants.METADATA_RISK_LEVEL).asText())
                .isEqualTo(roleRiskLevel);
    }
}

