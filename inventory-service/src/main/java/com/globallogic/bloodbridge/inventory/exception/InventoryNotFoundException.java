package com.globallogic.bloodbridge.inventory.exception;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(Long batchId) {
        super("Inventory batch not found with id: " + batchId);
    }
}
