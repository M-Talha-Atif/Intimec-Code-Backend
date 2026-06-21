package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.event.RoleImportJobEventPublisher;
import com.aidentx.iam.access_roles_management.event.RoleImportJobStartedEvent;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobResponse;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJob;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJobItem;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportJobStatus;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobItemRepository;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobRepository;
import com.aidentx.iam.access_roles_management.service.import_jobs.CsvRoleImportParser;
import com.aidentx.iam.access_roles_management.service.import_jobs.JsonRoleImportParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleImportServiceImplTest {

    private static final String ORGANIZATION_ID = "11111111-1111-1111-1111-111111111111";
    private static final String IDENTITY_ID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private RoleImportJobRepository jobRepository;

    @Mock
    private RoleImportJobItemRepository itemRepository;

    @Mock
    private RoleImportJobEventPublisher eventPublisher;

    @Test
    void submitPersistsJobItemsAndPublishesEvent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AccessRoleImportServiceImpl service = service(objectMapper, 100);
        UUID jobId = UUID.randomUUID();

        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> {
            RoleImportJob job = invocation.getArgument(0);
            job.setId(jobId);
            job.setCreatedAt(Instant.now());
            return job;
        });
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "roles.json",
                "application/json",
                """
                        {
                          "roles": [
                            {"externalRef":"IMP-001","name":"Import Role 001","type":"business","businessUnit":"Finance","organization":"%s","requestable":true,"requireApproval":true,"identityIds":["%s"]},
                            {"externalRef":"IMP-002","name":"Import Role 002","type":"technical","businessUnit":"IT","organization":"%s","requestable":false,"requireApproval":true}
                          ]
                        }
                        """.formatted(ORGANIZATION_ID, IDENTITY_ID, ORGANIZATION_ID).getBytes());

        RoleImportJobResponse response = service.submit("tenant_001", "4bb2c7df-5677-4cd2-a0d1-aad3fa8afe8f", "alice.admin", "json", file);

        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getTenantId()).isEqualTo("tenant_001");
        assertThat(response.getStatus()).isEqualTo(RoleImportJobStatus.PENDING);
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getStatusUrl()).isEqualTo("/api/v1/access-roles/import/" + jobId);
        ArgumentCaptor<RoleImportJob> jobCaptor = ArgumentCaptor.forClass(RoleImportJob.class);
        verify(jobRepository, times(2)).save(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues().get(0).getActorId()).isEqualTo("4bb2c7df-5677-4cd2-a0d1-aad3fa8afe8f");
        assertThat(jobCaptor.getAllValues().get(0).getActorUsername()).isEqualTo("alice.admin");

        ArgumentCaptor<RoleImportJobItem> itemCaptor = ArgumentCaptor.forClass(RoleImportJobItem.class);
        verify(itemRepository, times(2)).save(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues().get(0).getPayload().get("identityIds").get(0).asText()).isEqualTo(IDENTITY_ID);
        ArgumentCaptor<RoleImportJobStartedEvent> eventCaptor = ArgumentCaptor.forClass(RoleImportJobStartedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().jobId()).isEqualTo(jobId);
        assertThat(eventCaptor.getValue().tenantId()).isEqualTo("tenant_001");
    }

    @Test
    void submitStoresDuplicateExternalRefAsFriendlyFailedRow() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AccessRoleImportServiceImpl service = service(objectMapper, 100);
        UUID jobId = UUID.randomUUID();
        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> {
            RoleImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobId);
            }
            return job;
        });
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "roles.json",
                "application/json",
                """
                        [
                          {"externalRef":"DUP","name":"Import Role 001","type":"business","businessUnit":"Finance","organization":"%s","requestable":true,"requireApproval":true},
                          {"externalRef":"DUP","name":"Import Role 002","type":"technical","businessUnit":"IT","organization":"%s","requestable":true,"requireApproval":true}
                        ]
                        """.formatted(ORGANIZATION_ID, ORGANIZATION_ID).getBytes());

        RoleImportJobResponse response = service.submit("tenant_001", "tester", "tester", "json", file);

        assertThat(response.getFailedCount()).isEqualTo(1);
        ArgumentCaptor<RoleImportJobItem> itemCaptor = ArgumentCaptor.forClass(RoleImportJobItem.class);
        verify(itemRepository, times(2)).save(itemCaptor.capture());
        RoleImportJobItem failedItem = itemCaptor.getAllValues().get(1);
        assertThat(failedItem.getStatus()).isEqualTo(RoleImportItemStatus.FAILED);
        assertThat(failedItem.getErrorMessage()).contains("Import Role 002", "duplicate externalRef 'DUP'");
    }

    @Test
    void submitQueuesRoleWhenOnlyNameIsProvided() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AccessRoleImportServiceImpl service = service(objectMapper, 100);
        UUID jobId = UUID.randomUUID();
        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> {
            RoleImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobId);
            }
            return job;
        });
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "roles.json",
                "application/json",
                """
                        [
                          {"externalRef":"IMP-001","name":"Import Role 001"}
                        ]
                        """.getBytes());

        RoleImportJobResponse response = service.submit("tenant_001", "tester", "tester", "json", file);

        assertThat(response.getFailedCount()).isZero();
        ArgumentCaptor<RoleImportJobItem> itemCaptor = ArgumentCaptor.forClass(RoleImportJobItem.class);
        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo(RoleImportItemStatus.QUEUED);
        assertThat(itemCaptor.getValue().getErrorMessage()).isNull();
    }

    @Test
    void submitStoresMissingNameAsFriendlyFailedRow() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AccessRoleImportServiceImpl service = service(objectMapper, 100);
        UUID jobId = UUID.randomUUID();
        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> {
            RoleImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobId);
            }
            return job;
        });
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "roles.json",
                "application/json",
                """
                        [
                          {"externalRef":"IMP-001","type":"business","businessUnit":"Finance","organization":"%s","requestable":true,"requireApproval":true}
                        ]
                        """.formatted(ORGANIZATION_ID).getBytes());

        RoleImportJobResponse response = service.submit("tenant_001", "tester", "tester", "json", file);

        assertThat(response.getFailedCount()).isEqualTo(1);
        ArgumentCaptor<RoleImportJobItem> itemCaptor = ArgumentCaptor.forClass(RoleImportJobItem.class);
        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo(RoleImportItemStatus.FAILED);
        assertThat(itemCaptor.getValue().getErrorMessage()).contains("Role name", "name");
    }

    private AccessRoleImportServiceImpl service(ObjectMapper objectMapper, int maxRows) {
        return new AccessRoleImportServiceImpl(
                jobRepository,
                itemRepository,
                new JsonRoleImportParser(objectMapper),
                new CsvRoleImportParser(objectMapper),
                eventPublisher,
                objectMapper,
                maxRows);
    }
}
