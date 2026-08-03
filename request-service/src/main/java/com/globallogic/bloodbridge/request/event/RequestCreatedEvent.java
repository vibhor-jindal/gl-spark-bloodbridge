package com.globallogic.bloodbridge.request.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
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
