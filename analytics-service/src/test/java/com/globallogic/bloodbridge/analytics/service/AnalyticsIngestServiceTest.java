package com.globallogic.bloodbridge.analytics.service;

import com.globallogic.bloodbridge.analytics.event.DonorMatchedEvent;
import com.globallogic.bloodbridge.analytics.event.RequestCreatedEvent;
import com.globallogic.bloodbridge.analytics.event.RequestStatusChangedEvent;
import com.globallogic.bloodbridge.analytics.model.RequestMetric;
import com.globallogic.bloodbridge.analytics.repository.RequestMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsIngestServiceTest {

    @Mock
    private RequestMetricRepository requestMetricRepository;

    @InjectMocks
    private AnalyticsIngestService ingestService;

    @Test
    @DisplayName("US-009 AC3: A RequestCreatedEvent is ingested as a new RequestMetric row")
    void testOnRequestCreated_CreatesMetric() {
        RequestCreatedEvent event = new RequestCreatedEvent(1L, "B+", "Delhi", "CRITICAL", 2, LocalDateTime.now());

        ingestService.onRequestCreated(event);

        verify(requestMetricRepository, times(1)).save(argThat(m ->
                m.getRequestId().equals(1L) && m.getStatus().equals("PENDING")));
    }

    @Test
    @DisplayName("US-009 AC3: A DonorMatchedEvent updates the existing metric's donorMatchedAt timestamp")
    void testOnDonorMatched_UpdatesExistingMetric() {
        RequestMetric metric = RequestMetric.builder().requestId(1L).status("PENDING").build();
        when(requestMetricRepository.findById(1L)).thenReturn(Optional.of(metric));

        DonorMatchedEvent event = new DonorMatchedEvent(1L, 10L, "Amit", "amit@example.com", "9000000000", "B+", "AIIMS Delhi", 2, "CRITICAL");
        ingestService.onDonorMatched(event);

        assertThat(metric.getDonorMatchedAt()).isNotNull();
        verify(requestMetricRepository, times(1)).save(metric);
    }

    @Test
    @DisplayName("A DonorMatchedEvent for an unknown requestId is logged and skipped without error")
    void testOnDonorMatched_UnknownRequest_DoesNotThrow() {
        when(requestMetricRepository.findById(99L)).thenReturn(Optional.empty());

        DonorMatchedEvent event = new DonorMatchedEvent(99L, 10L, "Amit", "amit@example.com", "9000000000", "B+", "AIIMS Delhi", 2, "CRITICAL");
        ingestService.onDonorMatched(event);

        verify(requestMetricRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-009 AC3: A RequestStatusChangedEvent(FULFILLED) sets status and fulfilledAt")
    void testOnStatusChanged_Fulfilled_UpdatesMetric() {
        RequestMetric metric = RequestMetric.builder().requestId(1L).status("CONFIRMED").build();
        when(requestMetricRepository.findById(1L)).thenReturn(Optional.of(metric));

        LocalDateTime changedAt = LocalDateTime.now();
        ingestService.onStatusChanged(new RequestStatusChangedEvent(1L, "FULFILLED", null, changedAt));

        assertThat(metric.getStatus()).isEqualTo("FULFILLED");
        assertThat(metric.getFulfilledAt()).isEqualTo(changedAt);
    }
}
