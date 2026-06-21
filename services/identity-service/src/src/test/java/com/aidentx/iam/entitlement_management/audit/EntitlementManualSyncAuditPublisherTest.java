package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementManualSyncAuditPublisherTest {

	private static final String TENANT_ID = "tenant-1";

	@Mock private KafkaTemplate<String, Object> kafkaTemplate;
	@Mock private UnifiedLoggerFactory loggerFactory;
	@Mock(answer = Answers.RETURNS_DEEP_STUBS) private IUnifiedLogger logger;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private EntitlementManualSyncAuditPublisher publisher;

	@BeforeEach
	void setUp() {
		when(loggerFactory.getLogger(eq(EntitlementManualSyncAuditPublisher.class), anyString())).thenReturn(logger);
		publisher = new EntitlementManualSyncAuditPublisher(kafkaTemplate, objectMapper, loggerFactory);
	}

	@Test
	void publishSendsFriendlyAuditLogEventWithMembersAndRiskFromEntitlement() {
		UUID entitlementId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		EntitlementManualSyncAuditEvent event = EntitlementManualSyncAuditEvent.summary(
				actorId.toString(), "Alice Admin", TENANT_ID, entitlementId.toString(), "Entitlement Hello",
				UUID.randomUUID().toString(), "high", 1, 1, 1, 1, 2,
				List.of(new EntitlementManualSyncAuditEvent.MemberAuditDetail(UUID.randomUUID().toString(), "John Doe", "EMPLOYEE",
						List.of(new EntitlementManualSyncAuditEvent.AssignmentAuditDetail(assignmentId.toString(), null, "PENDING", "CREATED")))),
				List.of(new EntitlementManualSyncAuditEvent.MemberAuditDetail(UUID.randomUUID().toString(), "Jane Doe", "EMPLOYEE", List.of())),
				List.of(new EntitlementManualSyncAuditEvent.MemberAuditDetail(UUID.randomUUID().toString(), "Sam Skip", "EMPLOYEE", List.of())),
				List.of(new EntitlementManualSyncAuditEvent.FailedMemberAuditDetail(UUID.randomUUID().toString(), "Fay Fail", "EMPLOYEE", "Identity not found")),
				objectMapper.createObjectNode().put(EntitlementAuditConstants.DETAIL_RISK_LEVEL, "high"));

		publisher.publish(event);

		ArgumentCaptor<Object> kafkaEventCaptor = ArgumentCaptor.forClass(Object.class);
		verify(kafkaTemplate).send(eq(AppConstants.AUDIT_TOPIC_NAME), eq(entitlementId.toString()), kafkaEventCaptor.capture());

		assertThat(kafkaEventCaptor.getValue()).isInstanceOf(AuditLogKafkaEvent.class);
		AuditLogKafkaEvent kafkaEvent = (AuditLogKafkaEvent) kafkaEventCaptor.getValue();
		assertThat(kafkaEvent.getActorId()).isEqualTo(actorId);
		assertThat(kafkaEvent.getActorName()).isEqualTo("Alice Admin");
		assertThat(kafkaEvent.getObjectId()).isEqualTo(entitlementId);
		assertThat(kafkaEvent.getObjectType()).isEqualTo("Entitlement");
		assertThat(kafkaEvent.getEvent()).isEqualTo(EntitlementAuditConstants.ACTIVITY_MEMBERS_MANUALLY_SYNCED);
		assertThat(kafkaEvent.getRisk()).isEqualTo(AppConstants.AUDIT_RISK_HIGH);
		assertThat(kafkaEvent.getDetails().get(EntitlementAuditConstants.DETAIL_RISK_LEVEL).asText()).isEqualTo("high");
		assertThat(kafkaEvent.getDetails().get(EntitlementAuditConstants.DETAIL_ADDED_MEMBERS).get(0).get("identityName").asText()).isEqualTo("John Doe");
		assertThat(kafkaEvent.getDetails().get(EntitlementAuditConstants.DETAIL_ADDED_MEMBERS).get(0).get("assignments").get(0).get("assignmentId").asText()).isEqualTo(assignmentId.toString());
		assertThat(kafkaEvent.getDetails().get(EntitlementAuditConstants.DETAIL_REMOVED_MEMBERS)).hasSize(1);
		assertThat(kafkaEvent.getDetails().get(EntitlementAuditConstants.DETAIL_SKIPPED_MEMBERS)).hasSize(1);
		assertThat(kafkaEvent.getDetails().get(EntitlementAuditConstants.DETAIL_FAILED_MEMBERS).get(0).get("reason").asText()).isEqualTo("Identity not found");
		assertThat(kafkaEvent.getTargetObject().get(AppConstants.AUDIT_TARGET_OBJECT_NAME).asText()).isEqualTo("Entitlement Hello");
	}
}
