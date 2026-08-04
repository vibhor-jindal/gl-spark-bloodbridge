package com.globallogic.bloodbridge.matching.exception;

public class DonorProfileNotFoundException extends RuntimeException {
    public DonorProfileNotFoundException(Long userId) {
        super("No donor profile for user " + userId + " — register as a donor first");
    }
}
