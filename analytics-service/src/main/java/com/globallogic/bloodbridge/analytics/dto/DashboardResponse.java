package com.globallogic.bloodbridge.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalRequests;
    private long fulfilledOrConfirmedCount;
    private double fulfillmentRatePercent;
    private Double averageMatchTimeSeconds;
    private Map<String, Long> requestsByBloodGroup;
    private Map<String, Long> requestsByStatus;
}
