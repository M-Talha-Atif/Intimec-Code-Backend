package com.aidentx.iam.access_roles_management.controller;

import com.aidentx.iam.access_management.config.constants.AccessKpiConstants;
import com.aidentx.iam.access_management.model.dto.response.AccessKpiMetricDto;
import com.aidentx.iam.access_management.model.dto.response.AccessKpiResponseDto;
import com.aidentx.iam.access_management.service.AccessKpiService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessKpiControllerTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessKpiService accessKpiService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessKpiController controller;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(AccessKpiController.class, "access-management")).thenReturn(logger);
        controller = new AccessKpiController(accessKpiService, loggerFactory);
    }

    @Test
    void getAccessKpisDelegatesToCrossDomainServiceAndWrapsResponse() {
        AccessKpiResponseDto serviceResponse = new AccessKpiResponseDto();
        serviceResponse.setRoleCoverageRatio(metric("73.00", AccessKpiConstants.UNIT_PERCENT));
        serviceResponse.setDirectAccessExposure(metric("142", AccessKpiConstants.UNIT_COUNT));
        serviceResponse.setRoleComplexityIndex(metric("8.40", AccessKpiConstants.UNIT_AVERAGE));
        serviceResponse.setOrphanedAccessObjects(metric("31", AccessKpiConstants.UNIT_COUNT));
        serviceResponse.setAccessReuseScore(metric("64.00", AccessKpiConstants.UNIT_PERCENT));
        when(accessKpiService.getAccessKpis(TENANT_ID)).thenReturn(serviceResponse);

        var response = controller.getAccessKpis(TENANT_ID);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(AccessKpiConstants.MESSAGE_ACCESS_KPIS_FETCHED);
        assertThat(response.getData()).isSameAs(serviceResponse);
        verify(accessKpiService).getAccessKpis(TENANT_ID);
    }

    private AccessKpiMetricDto metric(String value, String unit) {
        return new AccessKpiMetricDto(
                new BigDecimal(value),
                unit,
                null,
                AccessKpiConstants.DELTA_DIRECTION_NONE,
                AccessKpiConstants.PERIOD_LABEL_CURRENT);
    }
}
