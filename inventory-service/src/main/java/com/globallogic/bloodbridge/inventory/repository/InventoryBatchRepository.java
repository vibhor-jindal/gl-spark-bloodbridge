package com.globallogic.bloodbridge.inventory.repository;

import com.globallogic.bloodbridge.inventory.model.BatchStatus;
import com.globallogic.bloodbridge.inventory.model.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc(String bloodGroup, String city, BatchStatus status);

    List<InventoryBatch> findByExpiryDateBeforeAndStatus(LocalDate date, BatchStatus status);

    List<InventoryBatch> findByStatus(BatchStatus status);
}
