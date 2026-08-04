package com.globallogic.bloodbridge.matching.client;

import com.globallogic.bloodbridge.matching.dto.RequestDto;
import com.globallogic.bloodbridge.matching.dto.StatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "request-service")
public interface RequestServiceClient {

    @GetMapping("/api/requests/{requestId}")
    RequestDto getRequest(@PathVariable("requestId") Long requestId);

    @PutMapping("/api/requests/{requestId}/status")
    RequestDto updateStatus(@PathVariable("requestId") Long requestId, @RequestBody StatusUpdateRequest request);
}
