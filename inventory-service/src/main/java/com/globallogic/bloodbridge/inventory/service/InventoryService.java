package com.globallogic.bloodbridge.inventory.service;

import com.globallogic.bloodbridge.inventory.dto.*;
import com.globallogic.bloodbridge.inventory.exception.InsufficientStockException;
import com.globallogic.bloodbridge.inventory.exception.InventoryNotFoundException;
import com.globallogic.bloodbridge.inventory.model.BatchStatus;
import com.globallogic.bloodbridge.inventory.model.InventoryBatch;
import com.globallogic.bloodbridge.inventory.repository.InventoryBatchRepository;
import com.globallogic.bloodbridge.inventory.util.CityClusters;
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
    public InventoryResponse addStock(Long ownerUserId, InventoryRequest request) {
        InventoryBatch batch = InventoryBatch.builder()
                .bloodBankName(request.getBloodBankName())
                .ownerUserId(ownerUserId)
                .city(normalizeCity(request.getCity()))
                .bloodGroup(normalizeBloodGroup(request.getBloodGroup()))
                .unitsAvailable(request.getUnitsAvailable())
                .collectedDate(request.getCollectedDate())
                .expiryDate(request.getExpiryDate())
                .status(BatchStatus.ACTIVE)
                .build();

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        log.info("Added inventory batch id={} bloodGroup={} city={} units={} owner={}",
                saved.getBatchId(), saved.getBloodGroup(), saved.getCity(), saved.getUnitsAvailable(), ownerUserId);
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse updateStock(Long batchId, UnitsUpdateRequest request) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new InventoryNotFoundException(batchId));

        int units = request.getUnitsAvailable();
        batch.setUnitsAvailable(units);

        // 0 units → DEPLETED. units > 0 and not past expiry → ACTIVE (reactivates DEPLETED).
        // Past-expiry batches stay/become EXPIRED even if units are restored.
        if (units == 0) {
            batch.setStatus(BatchStatus.DEPLETED);
        } else if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
            batch.setStatus(BatchStatus.EXPIRED);
        } else {
            batch.setStatus(BatchStatus.ACTIVE);
        }

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        log.info("Updated inventory batch id={} to {} units status={}", batchId, units, saved.getStatus());
        return toResponse(saved);
    }

    public InventoryResponse getBatch(Long batchId) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new InventoryNotFoundException(batchId));
        return toResponse(batch);
    }

    public List<InventoryResponse> search(String bloodGroup, String city) {
        String group = normalizeBloodGroup(bloodGroup);
        String normalizedCity = normalizeCity(city);
        List<InventoryBatch> batches = inventoryBatchRepository
                .findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc(group, normalizedCity, BatchStatus.ACTIVE);
        // If no exact-city stock, include nearby metro cities (Delhi NCR).
        if (batches.isEmpty() && normalizedCity != null) {
            List<String> cluster = CityClusters.searchCities(normalizedCity);
            if (cluster.size() > 1) {
                batches = inventoryBatchRepository.findActiveByBloodGroupAndCities(group, cluster, BatchStatus.ACTIVE);
            }
        }
        return batches.stream().map(this::toResponse).toList();
    }

    static String normalizeBloodGroup(String bloodGroup) {
        if (bloodGroup == null) {
            return null;
        }
        return bloodGroup.trim().replace(' ', '+').toUpperCase();
    }

    static String normalizeCity(String city) {
        if (city == null) {
            return null;
        }
        return city.trim();
    }

    public List<InventoryResponse> listMine(Long ownerUserId) {
        return inventoryBatchRepository.findByOwnerUserIdOrderByExpiryDateAsc(ownerUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InventoryResponse> listAll() {
        return inventoryBatchRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteBatch(Long batchId) {
        if (!inventoryBatchRepository.existsById(batchId)) {
            throw new InventoryNotFoundException(batchId);
        }
        inventoryBatchRepository.deleteById(batchId);
        log.info("Deleted inventory batch id={}", batchId);
    }

    @Transactional
    public ReserveResponse reserveUnits(ReserveRequest request) {
        String bloodGroup = normalizeBloodGroup(request.getBloodGroup());
        String city = normalizeCity(request.getCity());
        List<InventoryBatch> batches;
        if (request.getBatchId() != null) {
            InventoryBatch batch = inventoryBatchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new InventoryNotFoundException(request.getBatchId()));
            batches = List.of(batch);
        } else {
            batches = inventoryBatchRepository
                    .findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc(
                            bloodGroup, city, BatchStatus.ACTIVE);
            if (batches.isEmpty() && city != null) {
                List<String> cluster = CityClusters.searchCities(city);
                if (cluster.size() > 1) {
                    batches = inventoryBatchRepository.findActiveByBloodGroupAndCities(
                            bloodGroup, cluster, BatchStatus.ACTIVE);
                }
            }
        }

        int totalAvailable = batches.stream().mapToInt(InventoryBatch::getUnitsAvailable).sum();
        if (totalAvailable < request.getUnitsNeeded()) {
            throw new InsufficientStockException(bloodGroup, city, request.getUnitsNeeded(), totalAvailable);
        }

        int remainingToReserve = request.getUnitsNeeded();
        List<InventoryBatch> updated = new ArrayList<>();
        Long firstBatchId = null;

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
            if (firstBatchId == null) {
                firstBatchId = batch.getBatchId();
            }
        }

        inventoryBatchRepository.saveAll(updated);

        int remaining = totalAvailable - request.getUnitsNeeded();
        log.info("Reserved {} unit(s) of {} in {}. Remaining: {}",
                request.getUnitsNeeded(), bloodGroup, city, remaining);

        return ReserveResponse.builder()
                .bloodGroup(bloodGroup)
                .city(city)
                .unitsReserved(request.getUnitsNeeded())
                .remainingAvailable(remaining)
                .batchId(firstBatchId)
                .ownerUserId(updated.isEmpty() ? null : updated.get(0).getOwnerUserId())
                .bloodBankName(updated.isEmpty() ? null : updated.get(0).getBloodBankName())
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
        return getLowStockAlerts(null);
    }

    public List<LowStockAlert> getLowStockAlerts(Long ownerUserId) {
        Map<String, List<InventoryBatch>> grouped = inventoryBatchRepository.findByStatus(BatchStatus.ACTIVE)
                .stream()
                .filter(b -> ownerUserId == null || ownerUserId.equals(b.getOwnerUserId()))
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
                .ownerUserId(batch.getOwnerUserId())
                .city(batch.getCity())
                .bloodGroup(batch.getBloodGroup())
                .unitsAvailable(batch.getUnitsAvailable())
                .collectedDate(batch.getCollectedDate())
                .expiryDate(batch.getExpiryDate())
                .status(batch.getStatus())
                .build();
    }
}
