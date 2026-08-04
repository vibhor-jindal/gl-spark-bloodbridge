package com.globallogic.bloodbridge.donor.repository;

import com.globallogic.bloodbridge.donor.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    Optional<Donor> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByPhone(String phone);
    List<Donor> findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue(String bloodGroup, String city);
    List<Donor> findByBloodGroupAndIsAvailableTrue(String bloodGroup);

    @Query("""
            SELECT d FROM Donor d
            WHERE d.bloodGroup = :bloodGroup
              AND LOWER(TRIM(d.city)) IN :cities
              AND d.isAvailable = true
            """)
    List<Donor> findAvailableByBloodGroupAndCities(
            @Param("bloodGroup") String bloodGroup,
            @Param("cities") Collection<String> cities);
}
