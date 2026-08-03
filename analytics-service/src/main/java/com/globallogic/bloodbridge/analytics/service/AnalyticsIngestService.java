package com.globallogic.bloodbridge.analytics.service;

import com.globallogic.bloodbridge.analytics.event.DonorMatchedEvent;
import com.globallogic.bloodbridge.analytics.event.RequestCreatedEvent;
import com.globallogic.bloodbridge.analytics.event.RequestStatusChangedEvent;
import com.globallogic.bloodbridge.analytics.model.RequestMetric;
import com.globallogic.bloodbridge.analytics.repository.RequestMetricRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsIngestService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsIngestService.class);

    private final RequestMetricRepository requestMetricRepository;

    @KafkaListener(topics = "request-created-events", groupId = "analytics-service")
    @Transactional
    public void onRequestCreated(RequestCreatedEvent event) {
        RequestMetric metric = RequestMetric.builder()
                .requestId(event.getRequestId())
                .bloodGroup(event.getBloodGroup())
                .city(event.getCity())
                .urgency(event.getUrgency())
                .unitsNeeded(event.getUnitsNeeded())
                .status("PENDING")
                .requestCreatedAt(event.getCreatedAt())
                .build();

        requestMetricRepository.save(metric);
        log.info("Ingested RequestCreatedEvent requestId={}", event.getRequestId());
    }

    @KafkaListener(topics = "donor-matched-events", groupId = "analytics-service")
    @Transactional
    public void onDonorMatched(DonorMatchedEvent event) {
        requestMetricRepository.findById(event.getRequestId()).ifPresentOrElse(metric -> {
            if (metric.getDonorMatchedAt() == null) {
                metric.setDonorMatchedAt(LocalDateTime.now());
            }
            requestMetricRepository.save(metric);
        }, () -> log.warn("DonorMatchedEvent for unknown requestId={} — RequestCreatedEvent may not have arrived yet", event.getRequestId()));

        log.info("Ingested DonorMatchedEvent requestId={}", event.getRequestId());
    }

    @KafkaListener(topics = "request-status-changed-events", groupId = "analytics-service")
    @Transactional
    public void onStatusChanged(RequestStatusChangedEvent event) {
        requestMetricRepository.findById(event.getRequestId()).ifPresentOrElse(metric -> {
            metric.setStatus(event.getStatus());
            switch (event.getStatus()) {
                case "CONFIRMED" -> metric.setConfirmedAt(event.getChangedAt());
                case "FULFILLED" -> metric.setFulfilledAt(event.getChangedAt());
                case "CANCELLED" -> metric.setCancelledAt(event.getChangedAt());
                default -> { }
            }
            requestMetricRepository.save(metric);
        }, () -> log.warn("RequestStatusChangedEvent for unknown requestId={}", event.getRequestId()));

        log.info("Ingested RequestStatusChangedEvent requestId={} status={}", event.getRequestId(), event.getStatus());
    }
}
