package com.globallogic.bloodbridge.request.dto;

import com.globallogic.bloodbridge.request.model.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private RequestStatus status;

    private Long confirmedDonorId;
}
