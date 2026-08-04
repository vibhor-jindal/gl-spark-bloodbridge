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
    public ResponseEntity<InventoryResponse> addStock(
            @RequestHeader(value = "X-User-Id", required = false) Long ownerUserId,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addStock(ownerUserId, request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<InventoryResponse>> mine(@RequestHeader("X-User-Id") Long ownerUserId) {
        return ResponseEntity.ok(inventoryService.listMine(ownerUserId));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> listAll() {
        return ResponseEntity.ok(inventoryService.listAll());
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

    @DeleteMapping("/{batchId}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long batchId) {
        inventoryService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponse> reserveUnits(@Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.reserveUnits(request));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<LowStockAlert>> getLowStockAlerts(
            @RequestHeader(value = "X-User-Id", required = false) Long ownerUserId,
            @RequestParam(defaultValue = "false") boolean mineOnly) {
        Long filter = mineOnly ? ownerUserId : null;
        return ResponseEntity.ok(inventoryService.getLowStockAlerts(filter));
    }
}
