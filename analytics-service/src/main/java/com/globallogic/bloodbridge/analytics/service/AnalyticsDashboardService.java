package com.globallogic.bloodbridge.analytics.service;

import com.globallogic.bloodbridge.analytics.dto.DashboardResponse;
import com.globallogic.bloodbridge.analytics.model.RequestMetric;
import com.globallogic.bloodbridge.analytics.repository.RequestMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private static final Set<String> FULFILLED_STATUSES = Set.of("CONFIRMED", "FULFILLED");

    private final RequestMetricRepository requestMetricRepository;

    public DashboardResponse getDashboard(String city, LocalDateTime from, LocalDateTime to) {
        List<RequestMetric> metrics = fetch(city, from, to);

        long total = metrics.size();
        long fulfilledOrConfirmed = metrics.stream().filter(m -> FULFILLED_STATUSES.contains(m.getStatus())).count();
        double fulfillmentRate = total == 0 ? 0.0 : (fulfilledOrConfirmed * 100.0) / total;

        List<Long> matchTimes = metrics.stream()
                .filter(m -> m.getRequestCreatedAt() != null && m.getDonorMatchedAt() != null)
                .map(m -> Duration.between(m.getRequestCreatedAt(), m.getDonorMatchedAt()).getSeconds())
                .toList();
        Double avgMatchTime = matchTimes.isEmpty() ? null
                : matchTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        return DashboardResponse.builder()
                .totalRequests(total)
                .fulfilledOrConfirmedCount(fulfilledOrConfirmed)
                .fulfillmentRatePercent(Math.round(fulfillmentRate * 100.0) / 100.0)
                .averageMatchTimeSeconds(avgMatchTime)
                .requestsByBloodGroup(metrics.stream()
                        .collect(Collectors.groupingBy(RequestMetric::getBloodGroup, Collectors.counting())))
                .requestsByStatus(metrics.stream()
                        .collect(Collectors.groupingBy(RequestMetric::getStatus, Collectors.counting())))
                .build();
    }

    public String exportCsv(String city, LocalDateTime from, LocalDateTime to) {
        List<RequestMetric> metrics = fetch(city, from, to);

        StringBuilder csv = new StringBuilder("requestId,bloodGroup,city,urgency,unitsNeeded,status,createdAt,donorMatchedAt,confirmedAt,fulfilledAt,cancelledAt\n");
        for (RequestMetric m : metrics) {
            csv.append(m.getRequestId()).append(',')
                    .append(nullSafe(m.getBloodGroup())).append(',')
                    .append(nullSafe(m.getCity())).append(',')
                    .append(nullSafe(m.getUrgency())).append(',')
                    .append(m.getUnitsNeeded()).append(',')
                    .append(nullSafe(m.getStatus())).append(',')
                    .append(nullSafe(m.getRequestCreatedAt())).append(',')
                    .append(nullSafe(m.getDonorMatchedAt())).append(',')
                    .append(nullSafe(m.getConfirmedAt())).append(',')
                    .append(nullSafe(m.getFulfilledAt())).append(',')
                    .append(nullSafe(m.getCancelledAt())).append('\n');
        }
        return csv.toString();
    }

    private List<RequestMetric> fetch(String city, LocalDateTime from, LocalDateTime to) {
        if (city == null || city.isBlank()) {
            return requestMetricRepository.findByRequestCreatedAtBetween(from, to);
        }
        return requestMetricRepository.findByCityIgnoreCaseAndRequestCreatedAtBetween(city, from, to);
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
