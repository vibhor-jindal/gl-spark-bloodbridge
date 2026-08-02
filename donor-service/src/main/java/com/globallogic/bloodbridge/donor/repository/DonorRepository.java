package com.globallogic.bloodbridge.donor.repository;

import com.globallogic.bloodbridge.donor.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    Optional<Donor> findByUserId(Long userId);
    boolean existsByPhone(String phone);
    List<Donor> findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue(String bloodGroup, String city);
}
