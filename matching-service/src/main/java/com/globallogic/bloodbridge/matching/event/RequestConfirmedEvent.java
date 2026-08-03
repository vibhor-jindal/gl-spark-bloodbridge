package com.globallogic.bloodbridge.matching.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestConfirmedEvent {
    private Long requestId;
    private Long requesterId;
    private Long donorId;
    private String donorName;
    private String donorPhone;
}
