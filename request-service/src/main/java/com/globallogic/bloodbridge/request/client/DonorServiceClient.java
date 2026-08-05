package com.globallogic.bloodbridge.request.client;

import com.globallogic.bloodbridge.request.dto.DonorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "donor-service")
public interface DonorServiceClient {

    @GetMapping("/api/donors/{donorId}")
    DonorDto getDonor(@PathVariable("donorId") Long donorId);
}
