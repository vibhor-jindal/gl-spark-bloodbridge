package com.globallogic.bloodbridge.inventory.controller;

import com.globallogic.bloodbridge.inventory.dto.*;
import com.globallogic.bloodbridge.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> addStock(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addStock(request));
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<InventoryResponse> getBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(inventoryService.getBatch(batchId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<InventoryResponse>> search(
            @RequestParam String bloodGroup,
            @RequestParam String city) {
        return ResponseEntity.ok(inventoryService.search(bloodGroup, city));
    }

    @PatchMapping("/{batchId}")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable Long batchId,
            @Valid @RequestBody UnitsUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.updateStock(batchId, request));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponse> reserveUnits(@Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.reserveUnits(request));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<LowStockAlert>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }
}
