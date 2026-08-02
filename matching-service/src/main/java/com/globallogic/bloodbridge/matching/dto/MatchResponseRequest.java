package com.globallogic.bloodbridge.matching.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponseRequest {

    @NotNull(message = "donorId is required")
    private Long donorId;

    @NotNull(message = "accepted is required")
    private Boolean accepted;
}
