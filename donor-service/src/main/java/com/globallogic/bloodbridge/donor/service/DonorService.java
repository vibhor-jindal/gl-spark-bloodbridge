package com.globallogic.bloodbridge.donor.service;

import com.globallogic.bloodbridge.donor.dto.DonorRequest;
import com.globallogic.bloodbridge.donor.dto.DonorResponse;
import com.globallogic.bloodbridge.donor.exception.DonorNotFoundException;
import com.globallogic.bloodbridge.donor.exception.DuplicateDonorException;
import com.globallogic.bloodbridge.donor.model.Donor;
import com.globallogic.bloodbridge.donor.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonorService {

    private static final Logger log = LoggerFactory.getLogger(DonorService.class);

    private final DonorRepository donorRepository;

    @Transactional
    public DonorResponse registerDonor(Long userId, DonorRequest request) {
        if (donorRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateDonorException(request.getPhone());
        }

        Donor donor = Donor.builder()
                .userId(userId)
                .name(request.getName())
                .bloodGroup(request.getBloodGroup())
                .phone(request.getPhone())
                .email(request.getEmail())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isAvailable(true)
                .build();

        Donor saved = donorRepository.save(donor);
        log.info("Registered donor id={} userId={}", saved.getDonorId(), userId);
        return toResponse(saved);
    }

    public DonorResponse getDonor(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new DonorNotFoundException(donorId));
        return toResponse(donor);
    }

    public List<DonorResponse> search(String bloodGroup, String city) {
        return donorRepository.findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue(bloodGroup, city)
                .stream()
                .filter(Donor::isEligibleToDonate)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DonorResponse updateAvailability(Long donorId, boolean isAvailable) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new DonorNotFoundException(donorId));
        donor.setIsAvailable(isAvailable);
        Donor saved = donorRepository.save(donor);
        log.info("Donor id={} availability set to {}", donorId, isAvailable);
        return toResponse(saved);
    }

    @Transactional
    public DonorResponse recordDonation(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new DonorNotFoundException(donorId));
        donor.setLastDonationDate(LocalDate.now());
        donor.setIsAvailable(false);
        Donor saved = donorRepository.save(donor);
        log.info("Donor id={} recorded a donation, cooldown started", donorId);
        return toResponse(saved);
    }

    private DonorResponse toResponse(Donor donor) {
        return DonorResponse.builder()
                .donorId(donor.getDonorId())
                .userId(donor.getUserId())
                .name(donor.getName())
                .bloodGroup(donor.getBloodGroup())
                .phone(donor.getPhone())
                .email(donor.getEmail())
                .city(donor.getCity())
                .latitude(donor.getLatitude())
                .longitude(donor.getLongitude())
                .isAvailable(donor.getIsAvailable())
                .lastDonationDate(donor.getLastDonationDate())
                .eligibleToDonate(donor.isEligibleToDonate())
                .build();
    }
}
