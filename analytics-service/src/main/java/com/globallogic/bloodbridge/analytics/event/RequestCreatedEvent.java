package com.globallogic.bloodbridge.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreatedEvent {
    private Long requestId;
    private String bloodGroup;
    private String city;
    private String urgency;
    private Integer unitsNeeded;
    private LocalDateTime createdAt;
}
