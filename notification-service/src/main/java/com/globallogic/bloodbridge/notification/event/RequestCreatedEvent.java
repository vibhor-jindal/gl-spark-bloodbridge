package com.globallogic.bloodbridge.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreatedEvent {
    private Long requestId;
    private Long requesterId;
    private String patientName;
    private String bloodGroup;
    private String city;
    private String hospitalName;
    private String urgency;
    private Integer unitsNeeded;
    private LocalDateTime createdAt;
}
