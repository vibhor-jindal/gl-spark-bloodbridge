package com.globallogic.bloodbridge.analytics.service;

import com.globallogic.bloodbridge.analytics.dto.DashboardResponse;
import com.globallogic.bloodbridge.analytics.model.RequestMetric;
import com.globallogic.bloodbridge.analytics.repository.RequestMetricRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsDashboardServiceTest {

    @Mock
    private RequestMetricRepository requestMetricRepository;

    @InjectMocks
    private AnalyticsDashboardService dashboardService;

    @Test
    @DisplayName("US-009 AC1: Dashboard reports request volume, fulfillment rate, and average match time")
    void testGetDashboard_ComputesMetrics() {
        LocalDateTime created = LocalDateTime.now().minusMinutes(10);
        LocalDateTime matched = created.plusSeconds(30);

        RequestMetric fulfilled = RequestMetric.builder()
                .requestId(1L).bloodGroup("O+").city("Delhi").status("FULFILLED")
                .requestCreatedAt(created).donorMatchedAt(matched).build();
        RequestMetric pending = RequestMetric.builder()
                .requestId(2L).bloodGroup("B+").city("Delhi").status("PENDING")
                .requestCreatedAt(created).build();

        when(requestMetricRepository.findByRequestCreatedAtBetween(any(), any())).thenReturn(List.of(fulfilled, pending));

        DashboardResponse response = dashboardService.getDashboard(null, created.minusDays(1), LocalDateTime.now());

        assertThat(response.getTotalRequests()).isEqualTo(2);
        assertThat(response.getFulfilledOrConfirmedCount()).isEqualTo(1);
        assertThat(response.getFulfillmentRatePercent()).isEqualTo(50.0);
        assertThat(response.getAverageMatchTimeSeconds()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("US-009 AC2: Filtering by city calls the city-scoped repository query")
    void testGetDashboard_FiltersByCity() {
        when(requestMetricRepository.findByCityIgnoreCaseAndRequestCreatedAtBetween(any(), any(), any()))
                .thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard("Mumbai", LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(response.getTotalRequests()).isZero();
    }

    @Test
    @DisplayName("Dashboard with no matched requests reports a null average match time instead of a misleading zero")
    void testGetDashboard_NoMatches_AverageIsNull() {
        RequestMetric pending = RequestMetric.builder()
                .requestId(2L).bloodGroup("B+").city("Delhi").status("PENDING")
                .requestCreatedAt(LocalDateTime.now()).build();

        when(requestMetricRepository.findByRequestCreatedAtBetween(any(), any())).thenReturn(List.of(pending));

        DashboardResponse response = dashboardService.getDashboard(null, LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(response.getAverageMatchTimeSeconds()).isNull();
    }

    @Test
    @DisplayName("US-009 AC4: CSV export includes a header row and one row per request")
    void testExportCsv_IncludesHeaderAndRows() {
        RequestMetric metric = RequestMetric.builder()
                .requestId(1L).bloodGroup("O+").city("Delhi").urgency("CRITICAL").unitsNeeded(2)
                .status("FULFILLED").requestCreatedAt(LocalDateTime.now()).build();

        when(requestMetricRepository.findByRequestCreatedAtBetween(any(), any())).thenReturn(List.of(metric));

        String csv = dashboardService.exportCsv(null, LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(csv).startsWith("requestId,bloodGroup,city");
        assertThat(csv).contains("1,O+,Delhi,CRITICAL,2,FULFILLED");
    }
}
