package com.globallogic.bloodbridge.donor.service;

import com.globallogic.bloodbridge.donor.dto.DonorRequest;
import com.globallogic.bloodbridge.donor.dto.DonorResponse;
import com.globallogic.bloodbridge.donor.exception.DonorNotFoundException;
import com.globallogic.bloodbridge.donor.exception.DuplicateDonorException;
import com.globallogic.bloodbridge.donor.model.Donor;
import com.globallogic.bloodbridge.donor.repository.DonorRepository;
import com.globallogic.bloodbridge.donor.util.CityClusters;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DonorService {

    private static final Logger log = LoggerFactory.getLogger(DonorService.class);

    private final DonorRepository donorRepository;

    @Transactional
    public DonorResponse registerDonor(Long userId, DonorRequest request) {
        if (donorRepository.existsByUserId(userId)) {
            throw DuplicateDonorException.forUser(userId);
        }

        String phone = blankToNull(request.getPhone());
        if (phone != null && donorRepository.existsByPhone(phone)) {
            throw new DuplicateDonorException(phone);
        }

        Donor donor = Donor.builder()
                .userId(userId)
                .name(request.getName().trim())
                .bloodGroup(request.getBloodGroup().trim())
                .phone(phone)
                .email(blankToNull(request.getEmail()))
                .city(request.getCity().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isAvailable(true)
                .build();

        try {
            Donor saved = donorRepository.save(donor);
            log.info("Registered donor id={} userId={}", saved.getDonorId(), userId);
            return toResponse(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("Donor register constraint failed for userId={}: {}", userId, ex.getMostSpecificCause().getMessage());
            if (donorRepository.existsByUserId(userId)) {
                throw DuplicateDonorException.forUser(userId);
            }
            throw new DuplicateDonorException(phone != null ? phone : "unknown");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public DonorResponse getDonor(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new DonorNotFoundException(donorId));
        return toResponse(donor);
    }

    public DonorResponse getByUserId(Long userId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new DonorNotFoundException(userId));
        return toResponse(donor);
    }

    /** Expand to nearby-cluster peers when same-city available donors are below this count. */
    private static final int MIN_LOCAL_DONORS = 3;

    public List<DonorResponse> search(String bloodGroup, String city) {
        // Feign/HTTP often send "B+" as "B " (+ = space in form encoding) — normalize back.
        String group = normalizeBloodGroup(bloodGroup);
        List<Donor> donors;
        if (city != null && !city.isBlank()) {
            String trimmedCity = city.trim();
            donors = donorRepository.findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue(group, trimmedCity);

            List<String> clusterCities = CityClusters.searchCities(trimmedCity);
            // Prefer same city; if none/too few, also include nearby cities (e.g. Delhi NCR).
            if (clusterCities.size() > 1 && donors.size() < MIN_LOCAL_DONORS) {
                List<Donor> clusterDonors = donorRepository.findAvailableByBloodGroupAndCities(group, clusterCities);
                donors = mergePreferringLocalCity(donors, clusterDonors, trimmedCity);
            }

            // Last resort: same blood group in any city.
            if (donors.isEmpty()) {
                donors = donorRepository.findByBloodGroupAndIsAvailableTrue(group);
            }
        } else {
            donors = donorRepository.findByBloodGroupAndIsAvailableTrue(group);
        }
        return donors.stream()
                .filter(Donor::isEligibleToDonate)
                .map(this::toResponse)
                .toList();
    }

    private static List<Donor> mergePreferringLocalCity(List<Donor> local, List<Donor> nearby, String requestCity) {
        Map<Long, Donor> byId = new LinkedHashMap<>();
        for (Donor d : local) {
            byId.put(d.getDonorId(), d);
        }
        for (Donor d : nearby) {
            byId.putIfAbsent(d.getDonorId(), d);
        }
        List<Donor> merged = new ArrayList<>(byId.values());
        merged.sort(Comparator.comparing((Donor d) -> CityClusters.isSameCity(d.getCity(), requestCity) ? 0 : 1));
        return merged;
    }

    static String normalizeBloodGroup(String bloodGroup) {
        if (bloodGroup == null) {
            return null;
        }
        return bloodGroup.trim().replace(' ', '+').toUpperCase();
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
