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
public class BloodDeliveredEvent {
    private Long requestId;
    private Long requesterId;
    private String patientName;
    private String hospitalName;
    private String bloodGroup;
    private Integer unitsNeeded;
    /** DONOR, BLOOD_BANK, or null when unknown. */
    private String fulfillmentSource;
    private String donorName;
    private LocalDateTime deliveredAt;
}
