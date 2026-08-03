package com.globallogic.bloodbridge.inventory.service;

import com.globallogic.bloodbridge.inventory.dto.*;
import com.globallogic.bloodbridge.inventory.exception.InsufficientStockException;
import com.globallogic.bloodbridge.inventory.exception.InventoryNotFoundException;
import com.globallogic.bloodbridge.inventory.model.BatchStatus;
import com.globallogic.bloodbridge.inventory.model.InventoryBatch;
import com.globallogic.bloodbridge.inventory.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryBatchRepository inventoryBatchRepository;

    @Value("${bloodbridge.inventory.safety-threshold:5}")
    private int safetyThreshold;

    @Transactional
    public InventoryResponse addStock(InventoryRequest request) {
        InventoryBatch batch = InventoryBatch.builder()
                .bloodBankName(request.getBloodBankName())
                .city(request.getCity())
                .bloodGroup(request.getBloodGroup())
                .unitsAvailable(request.getUnitsAvailable())
                .collectedDate(request.getCollectedDate())
                .expiryDate(request.getExpiryDate())
                .status(BatchStatus.ACTIVE)
                .build();

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        log.info("Added inventory batch id={} bloodGroup={} city={} units={}",
                saved.getBatchId(), saved.getBloodGroup(), saved.getCity(), saved.getUnitsAvailable());
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse updateStock(Long batchId, UnitsUpdateRequest request) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new InventoryNotFoundException(batchId));

        batch.setUnitsAvailable(request.getUnitsAvailable());
        if (request.getUnitsAvailable() == 0 && batch.getStatus() == BatchStatus.ACTIVE) {
            batch.setStatus(BatchStatus.DEPLETED);
        }

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        log.info("Updated inventory batch id={} to {} units", batchId, request.getUnitsAvailable());
        return toResponse(saved);
    }

    public InventoryResponse getBatch(Long batchId) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new InventoryNotFoundException(batchId));
        return toResponse(batch);
    }

    public List<InventoryResponse> search(String bloodGroup, String city) {
        return inventoryBatchRepository
                .findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc(bloodGroup, city, BatchStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReserveResponse reserveUnits(ReserveRequest request) {
        List<InventoryBatch> batches = inventoryBatchRepository
                .findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc(
                        request.getBloodGroup(), request.getCity(), BatchStatus.ACTIVE);

        int totalAvailable = batches.stream().mapToInt(InventoryBatch::getUnitsAvailable).sum();
        if (totalAvailable < request.getUnitsNeeded()) {
            throw new InsufficientStockException(request.getBloodGroup(), request.getCity(), request.getUnitsNeeded(), totalAvailable);
        }

        int remainingToReserve = request.getUnitsNeeded();
        List<InventoryBatch> updated = new ArrayList<>();

        for (InventoryBatch batch : batches) {
            if (remainingToReserve <= 0) {
                break;
            }
            int deduction = Math.min(remainingToReserve, batch.getUnitsAvailable());
            batch.setUnitsAvailable(batch.getUnitsAvailable() - deduction);
            if (batch.getUnitsAvailable() == 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            }
            remainingToReserve -= deduction;
            updated.add(batch);
        }

        inventoryBatchRepository.saveAll(updated);

        int remaining = totalAvailable - request.getUnitsNeeded();
        log.info("Reserved {} unit(s) of {} in {}. Remaining: {}",
                request.getUnitsNeeded(), request.getBloodGroup(), request.getCity(), remaining);

        return ReserveResponse.builder()
                .bloodGroup(request.getBloodGroup())
                .city(request.getCity())
                .unitsReserved(request.getUnitsNeeded())
                .remainingAvailable(remaining)
                .build();
    }

    @Transactional
    @Scheduled(cron = "${bloodbridge.inventory.expiry-cleanup-cron:0 0 2 * * *}")
    public void expireOverdueBatches() {
        List<InventoryBatch> overdue = inventoryBatchRepository
                .findByExpiryDateBeforeAndStatus(LocalDate.now(), BatchStatus.ACTIVE);

        overdue.forEach(batch -> batch.setStatus(BatchStatus.EXPIRED));
        inventoryBatchRepository.saveAll(overdue);

        if (!overdue.isEmpty()) {
            log.info("Expiry cleanup: marked {} batch(es) as EXPIRED", overdue.size());
        }
    }

    public List<LowStockAlert> getLowStockAlerts() {
        Map<String, List<InventoryBatch>> grouped = inventoryBatchRepository.findByStatus(BatchStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(b -> b.getBloodGroup() + "|" + b.getCity()));

        List<LowStockAlert> alerts = new ArrayList<>();
        grouped.forEach((key, batches) -> {
            int total = batches.stream().mapToInt(InventoryBatch::getUnitsAvailable).sum();
            if (total < safetyThreshold) {
                String[] parts = key.split("\\|");
                alerts.add(LowStockAlert.builder()
                        .bloodGroup(parts[0])
                        .city(parts[1])
                        .availableUnits(total)
                        .threshold(safetyThreshold)
                        .build());
                log.warn("LOW STOCK ALERT: {} in {} has only {} unit(s) (threshold {})", parts[0], parts[1], total, safetyThreshold);
            }
        });

        return alerts;
    }

    private InventoryResponse toResponse(InventoryBatch batch) {
        return InventoryResponse.builder()
                .batchId(batch.getBatchId())
                .bloodBankName(batch.getBloodBankName())
                .city(batch.getCity())
                .bloodGroup(batch.getBloodGroup())
                .unitsAvailable(batch.getUnitsAvailable())
                .collectedDate(batch.getCollectedDate())
                .expiryDate(batch.getExpiryDate())
                .status(batch.getStatus())
                .build();
    }
}
