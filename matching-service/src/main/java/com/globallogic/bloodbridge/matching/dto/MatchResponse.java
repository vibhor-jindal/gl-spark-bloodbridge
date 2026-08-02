package com.globallogic.bloodbridge.matching.dto;

import com.globallogic.bloodbridge.matching.model.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {
    private Long matchId;
    private Long requestId;
    private Long donorId;
    private Double matchScore;
    private ResponseStatus responseStatus;
    private LocalDateTime createdAt;
}
