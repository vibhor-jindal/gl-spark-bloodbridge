package com.globallogic.bloodbridge.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestStatusChangedEvent {
    private Long requestId;
    private String status;
    private Long confirmedDonorId;
    private LocalDateTime changedAt;
}
