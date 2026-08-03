package com.globallogic.bloodbridge.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String bloodGroup, String city, int requested, int available) {
        super("Insufficient stock for " + bloodGroup + " in " + city
                + ": requested " + requested + ", only " + available + " available");
    }
}
