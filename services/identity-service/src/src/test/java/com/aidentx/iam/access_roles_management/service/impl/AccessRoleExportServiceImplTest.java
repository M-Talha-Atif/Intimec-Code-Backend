package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.service.AccessRoleExportService;
import com.aidentx.iam.access_roles_management.service.AccessRoleService;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportColumn;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportColumnRegistry;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFilters;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFormat;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleExportServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleService roleService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleExportServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleExportServiceImpl.class), anyString())).thenReturn(logger);
        AccessRoleExportColumnRegistry registry = new AccessRoleExportColumnRegistry(List.of(
                column("id", AccessRoleResponse::getId),
                column("name", AccessRoleResponse::getName),
                column("classification", AccessRoleResponse::getClassification),
                column("requestable", AccessRoleResponse::getRequestable)));
        service = new AccessRoleExportServiceImpl(roleService, registry, new ObjectMapper(), loggerFactory);
    }

    @Test
    void csvExportEmitsHeaderAndQuotesFieldsWithReservedCharacters() throws Exception {
        AccessRoleResponse row = role(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Finance, Manager",
                List.of("SOX", "PII"),
                true);
        when(roleService.listRoles(eq(TENANT_ID), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 500), 1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AccessRoleExportService.ExportResult result = service.streamExport(
                TENANT_ID, AccessRoleExportFilters.empty(), AccessRoleExportFormat.CSV, null, out);

        String csv = out.toString();
        assertThat(result.rowCount()).isEqualTo(1L);
        assertThat(result.truncated()).isFalse();
        assertThat(csv).startsWith("id,name,classification,requestable\r\n");
        assertThat(csv).contains("\"Finance, Manager\"");
        assertThat(csv).contains("SOX|PII");
        assertThat(csv).contains("true");
    }

    @Test
    void jsonExportEmitsArrayOfObjectsWithTypedValues() throws Exception {
        AccessRoleResponse row = role(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "Auditor",
                List.of("GDPR"),
                false);
        when(roleService.listRoles(eq(TENANT_ID), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 500), 1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamExport(TENANT_ID, AccessRoleExportFilters.empty(),
                AccessRoleExportFormat.JSON, null, out);

        JsonNode parsed = new ObjectMapper().readTree(out.toByteArray());
        assertThat(parsed.isArray()).isTrue();
        assertThat(parsed).hasSize(1);
        JsonNode first = parsed.get(0);
        assertThat(first.get("name").asText()).isEqualTo("Auditor");
        assertThat(first.get("classification").isArray()).isTrue();
        assertThat(first.get("classification").get(0).asText()).isEqualTo("GDPR");
        assertThat(first.get("requestable").asBoolean()).isFalse();
    }

    @Test
    void columnsParameterRestrictsAndOrdersOutput() throws Exception {
        AccessRoleResponse row = role(UUID.randomUUID(), "Eng", List.of(), true);
        when(roleService.listRoles(eq(TENANT_ID), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 500), 1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamExport(TENANT_ID, AccessRoleExportFilters.empty(),
                AccessRoleExportFormat.CSV, List.of("name", "id"), out);

        String csv = out.toString();
        assertThat(csv.split("\r\n")[0]).isEqualTo("name,id");
    }

    @Test
    void iteratesAllPagesUntilLastPage() throws Exception {
        AccessRoleResponse a = role(UUID.randomUUID(), "A", List.of(), true);
        AccessRoleResponse b = role(UUID.randomUUID(), "B", List.of(), true);
        AccessRoleResponse c = role(UUID.randomUUID(), "C", List.of(), true);
        Page<AccessRoleResponse> page0 = new PageImpl<>(List.of(a, b), PageRequest.of(0, 2), 3);
        Page<AccessRoleResponse> page1 = new PageImpl<>(List.of(c), PageRequest.of(1, 2), 3);
        when(roleService.listRoles(eq(TENANT_ID), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page0, page1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AccessRoleExportService.ExportResult result = service.streamExport(
                TENANT_ID, AccessRoleExportFilters.empty(), AccessRoleExportFormat.CSV, null, out);

        assertThat(result.rowCount()).isEqualTo(3L);
        assertThat(out.toString()).contains(",A,").contains(",B,").contains(",C,");
    }

    private static AccessRoleResponse role(UUID id, String name, List<String> classification, Boolean requestable) {
        AccessRoleResponse r = new AccessRoleResponse();
        r.setId(id);
        r.setTenantId(TENANT_ID);
        r.setName(name);
        r.setClassification(classification);
        r.setRequestable(requestable);
        r.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        return r;
    }

    private static AccessRoleExportColumn column(String key,
                                                 java.util.function.Function<AccessRoleResponse, Object> extractor) {
        return new AccessRoleExportColumn() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public Object extract(AccessRoleResponse role) {
                return extractor.apply(role);
            }
        };
    }
}


