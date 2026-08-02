package com.globallogic.bloodbridge.matching.service;

import com.globallogic.bloodbridge.matching.client.DonorServiceClient;
import com.globallogic.bloodbridge.matching.dto.DonorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DonorSearchService {

    private final DonorServiceClient donorServiceClient;

    @Cacheable(value = "donorSearch", key = "#bloodGroup + '-' + #city")
    public List<DonorDto> searchEligibleDonors(String bloodGroup, String city) {
        return donorServiceClient.searchDonors(bloodGroup, city);
    }
}
