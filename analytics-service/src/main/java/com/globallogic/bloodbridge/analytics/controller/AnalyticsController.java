package com.globallogic.bloodbridge.analytics.controller;

import com.globallogic.bloodbridge.analytics.dto.DashboardResponse;
import com.globallogic.bloodbridge.analytics.service.AnalyticsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsDashboardService dashboardService;

    @GetMapping("/api/analytics/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        LocalDateTime rangeStart = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime rangeEnd = to != null ? to : LocalDateTime.now();

        return ResponseEntity.ok(dashboardService.getDashboard(city, rangeStart, rangeEnd));
    }

    @GetMapping("/api/analytics/export")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        LocalDateTime rangeStart = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime rangeEnd = to != null ? to : LocalDateTime.now();

        String csv = dashboardService.exportCsv(city, rangeStart, rangeEnd);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bloodbridge-report.csv\"")
                .body(csv);
    }
}
