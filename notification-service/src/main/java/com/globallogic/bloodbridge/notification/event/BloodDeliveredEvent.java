package com.globallogic.bloodbridge.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloodDeliveredEvent {
    private Long requestId;
    private Long requesterId;
    private String patientName;
    private String hospitalName;
    private String bloodGroup;
    private Integer unitsNeeded;
    private String fulfillmentSource;
    private String donorName;
    private LocalDateTime deliveredAt;
}
