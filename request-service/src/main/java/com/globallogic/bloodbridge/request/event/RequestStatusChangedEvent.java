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
public class RequestStatusChangedEvent {
    private Long requestId;
    private String status;
    private Long confirmedDonorId;
    private LocalDateTime changedAt;
}
