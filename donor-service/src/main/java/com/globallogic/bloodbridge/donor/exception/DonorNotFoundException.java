package com.globallogic.bloodbridge.donor.exception;

public class DonorNotFoundException extends RuntimeException {
    public DonorNotFoundException(Long donorId) {
        super("Donor not found with id: " + donorId);
    }
}
