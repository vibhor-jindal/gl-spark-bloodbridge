package com.globallogic.bloodbridge.matching.client;

import com.globallogic.bloodbridge.matching.dto.AvailabilityUpdateRequest;
import com.globallogic.bloodbridge.matching.dto.DonorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "donor-service")
public interface DonorServiceClient {

    /**
     * Blood group is a path segment so "+" is not decoded as a space (query-form encoding bug).
     */
    @GetMapping("/api/donors/search/{bloodGroup}")
    List<DonorDto> searchDonors(
            @PathVariable("bloodGroup") String bloodGroup,
            @RequestParam(value = "city", required = false) String city);

    @GetMapping("/api/donors/{donorId}")
    DonorDto getDonor(@PathVariable("donorId") Long donorId);

    @GetMapping("/api/donors/me")
    DonorDto getMyDonorProfile(@RequestHeader("X-User-Id") Long userId);

    @PutMapping("/api/donors/{donorId}/availability")
    DonorDto updateAvailability(@PathVariable("donorId") Long donorId, @RequestBody AvailabilityUpdateRequest request);
}
