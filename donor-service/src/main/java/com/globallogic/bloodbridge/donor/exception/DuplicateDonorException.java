package com.globallogic.bloodbridge.donor.exception;

public class DuplicateDonorException extends RuntimeException {
    public DuplicateDonorException(String phone) {
        super("A donor with phone '" + phone + "' already exists");
    }
}
