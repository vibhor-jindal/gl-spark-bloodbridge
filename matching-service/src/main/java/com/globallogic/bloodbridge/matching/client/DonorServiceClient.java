package com.globallogic.bloodbridge.matching.client;

import com.globallogic.bloodbridge.matching.dto.AvailabilityUpdateRequest;
import com.globallogic.bloodbridge.matching.dto.DonorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "donor-service")
public interface DonorServiceClient {

    @GetMapping("/api/donors/search")
    List<DonorDto> searchDonors(@RequestParam("bloodGroup") String bloodGroup, @RequestParam("city") String city);

    @GetMapping("/api/donors/{donorId}")
    DonorDto getDonor(@PathVariable("donorId") Long donorId);

    @PatchMapping("/api/donors/{donorId}/availability")
    DonorDto updateAvailability(@PathVariable("donorId") Long donorId, @RequestBody AvailabilityUpdateRequest request);
}
