package com.globallogic.bloodbridge.inventory.repository;

import com.globallogic.bloodbridge.inventory.model.BatchStatus;
import com.globallogic.bloodbridge.inventory.model.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc(String bloodGroup, String city, BatchStatus status);

    @Query("""
            SELECT b FROM InventoryBatch b
            WHERE b.bloodGroup = :bloodGroup
              AND LOWER(TRIM(b.city)) IN :cities
              AND b.status = :status
            ORDER BY b.expiryDate ASC
            """)
    List<InventoryBatch> findActiveByBloodGroupAndCities(
            @Param("bloodGroup") String bloodGroup,
            @Param("cities") Collection<String> cities,
            @Param("status") BatchStatus status);

    List<InventoryBatch> findByExpiryDateBeforeAndStatus(LocalDate date, BatchStatus status);

    List<InventoryBatch> findByStatus(BatchStatus status);

    List<InventoryBatch> findByOwnerUserIdOrderByExpiryDateAsc(Long ownerUserId);
}
