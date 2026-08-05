package com.globallogic.bloodbridge.inventory.service;

import com.globallogic.bloodbridge.inventory.dto.*;
import com.globallogic.bloodbridge.inventory.exception.InsufficientStockException;
import com.globallogic.bloodbridge.inventory.exception.InventoryNotFoundException;
import com.globallogic.bloodbridge.inventory.model.BatchStatus;
import com.globallogic.bloodbridge.inventory.model.InventoryBatch;
import com.globallogic.bloodbridge.inventory.repository.InventoryBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryBatch batch;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryService, "safetyThreshold", 5);

        batch = InventoryBatch.builder()
                .batchId(1L)
                .bloodBankName("Red Cross Delhi")
                .city("Delhi")
                .bloodGroup("O+")
                .unitsAvailable(10)
                .collectedDate(LocalDate.now().minusDays(5))
                .expiryDate(LocalDate.now().plusDays(30))
                .status(BatchStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("US-008 AC1: Adding stock reflects the new quantity immediately")
    void testAddStock_Success() {
        InventoryRequest request = new InventoryRequest("Red Cross Delhi", "Delhi", "O+", 10,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenReturn(batch);

        InventoryResponse response = inventoryService.addStock(10L, request);

        assertThat(response.getUnitsAvailable()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo(BatchStatus.ACTIVE);
    }

    @Test
    @DisplayName("US-008 AC1: Updating a batch's units reflects immediately, including transition to DEPLETED at zero")
    void testUpdateStock_ToZero_MarksDepleted() {
        when(inventoryBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenReturn(batch);

        inventoryService.updateStock(1L, new UnitsUpdateRequest(0));

        assertThat(batch.getUnitsAvailable()).isZero();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.DEPLETED);
    }

    @Test
    @DisplayName("Updating units > 0 on a DEPLETED batch restores ACTIVE when not past expiry")
    void testUpdateStock_Depleted_RestoresActive() {
        batch.setUnitsAvailable(0);
        batch.setStatus(BatchStatus.DEPLETED);
        when(inventoryBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenReturn(batch);

        inventoryService.updateStock(1L, new UnitsUpdateRequest(5));

        assertThat(batch.getUnitsAvailable()).isEqualTo(5);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);
    }

    @Test
    @DisplayName("Updating units on a past-expiry batch keeps/sets EXPIRED even if units > 0")
    void testUpdateStock_PastExpiry_StaysExpired() {
        batch.setStatus(BatchStatus.DEPLETED);
        batch.setUnitsAvailable(0);
        batch.setExpiryDate(LocalDate.now().minusDays(1));
        when(inventoryBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenReturn(batch);

        inventoryService.updateStock(1L, new UnitsUpdateRequest(5));

        assertThat(batch.getUnitsAvailable()).isEqualTo(5);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.EXPIRED);
    }

    @Test
    @DisplayName("Updating an unknown batch id throws InventoryNotFoundException")
    void testUpdateStock_NotFound() {
        when(inventoryBatchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateStock(99L, new UnitsUpdateRequest(5)))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    @DisplayName("US-008 AC2: Reserving units decrements available quantity across batches, soonest-expiry first")
    void testReserveUnits_DecrementsAvailableQuantity() {
        when(inventoryBatchRepository.findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc("O+", "Delhi", BatchStatus.ACTIVE))
                .thenReturn(List.of(batch));

        ReserveResponse response = inventoryService.reserveUnits(new ReserveRequest("O+", "Delhi", 4, null));

        assertThat(response.getUnitsReserved()).isEqualTo(4);
        assertThat(response.getRemainingAvailable()).isEqualTo(6);
        assertThat(batch.getUnitsAvailable()).isEqualTo(6);
        verify(inventoryBatchRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("US-008 AC2: Reserving more units than available throws InsufficientStockException")
    void testReserveUnits_InsufficientStock_ThrowsException() {
        when(inventoryBatchRepository.findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc("O+", "Delhi", BatchStatus.ACTIVE))
                .thenReturn(List.of(batch));

        assertThatThrownBy(() -> inventoryService.reserveUnits(new ReserveRequest("O+", "Delhi", 50, null)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("US-008 AC2: Fully reserving a batch's units marks it DEPLETED")
    void testReserveUnits_ExactAmount_MarksDepleted() {
        when(inventoryBatchRepository.findByBloodGroupAndCityIgnoreCaseAndStatusOrderByExpiryDateAsc("O+", "Delhi", BatchStatus.ACTIVE))
                .thenReturn(List.of(batch));

        inventoryService.reserveUnits(new ReserveRequest("O+", "Delhi", 10, null));

        assertThat(batch.getUnitsAvailable()).isZero();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.DEPLETED);
    }

    @Test
    @DisplayName("US-008 AC3: The daily cleanup job marks overdue ACTIVE batches as EXPIRED")
    void testExpireOverdueBatches_MarksExpired() {
        InventoryBatch overdue = InventoryBatch.builder()
                .batchId(2L).bloodGroup("A+").city("Mumbai")
                .unitsAvailable(3).status(BatchStatus.ACTIVE)
                .expiryDate(LocalDate.now().minusDays(1))
                .build();

        when(inventoryBatchRepository.findByExpiryDateBeforeAndStatus(any(LocalDate.class), org.mockito.ArgumentMatchers.eq(BatchStatus.ACTIVE)))
                .thenReturn(List.of(overdue));

        inventoryService.expireOverdueBatches();

        assertThat(overdue.getStatus()).isEqualTo(BatchStatus.EXPIRED);
        verify(inventoryBatchRepository, times(1)).saveAll(List.of(overdue));
    }

    @Test
    @DisplayName("US-008 AC4: Stock below the safety threshold raises a low-stock alert")
    void testGetLowStockAlerts_BelowThreshold_RaisesAlert() {
        InventoryBatch lowStock = InventoryBatch.builder()
                .batchId(3L).bloodGroup("AB-").city("Chennai")
                .unitsAvailable(2).status(BatchStatus.ACTIVE)
                .build();

        when(inventoryBatchRepository.findByStatus(BatchStatus.ACTIVE)).thenReturn(List.of(lowStock));

        List<LowStockAlert> alerts = inventoryService.getLowStockAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getBloodGroup()).isEqualTo("AB-");
        assertThat(alerts.get(0).getAvailableUnits()).isEqualTo(2);
    }

    @Test
    @DisplayName("US-008 AC4: Stock at or above the safety threshold does not raise an alert")
    void testGetLowStockAlerts_AboveThreshold_NoAlert() {
        when(inventoryBatchRepository.findByStatus(BatchStatus.ACTIVE)).thenReturn(List.of(batch));

        List<LowStockAlert> alerts = inventoryService.getLowStockAlerts();

        assertThat(alerts).isEmpty();
    }
}
