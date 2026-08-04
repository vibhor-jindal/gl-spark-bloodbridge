package com.globallogic.bloodbridge.rewards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private Long donorId;
    private String donorName;
    private String city;
    private Integer totalPoints;
    private Integer donationCount;
    private int rank;
}
