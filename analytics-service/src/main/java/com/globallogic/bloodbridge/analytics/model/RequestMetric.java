package com.globallogic.bloodbridge.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestMetric {

    @Id
    private Long requestId;

    private String bloodGroup;
    private String city;
    private String urgency;
    private Integer unitsNeeded;

    @Column(nullable = false)
    private String status;

    private LocalDateTime requestCreatedAt;
    private LocalDateTime donorMatchedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime fulfilledAt;
    private LocalDateTime cancelledAt;
}
