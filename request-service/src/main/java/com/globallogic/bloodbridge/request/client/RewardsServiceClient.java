package com.globallogic.bloodbridge.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "rewards-service")
public interface RewardsServiceClient {

    @PostMapping("/api/rewards/{donorId}/credit")
    void creditDonation(@PathVariable("donorId") Long donorId);
}
