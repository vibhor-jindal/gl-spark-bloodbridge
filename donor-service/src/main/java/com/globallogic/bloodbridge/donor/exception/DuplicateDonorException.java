package com.globallogic.bloodbridge.donor.exception;

public class DuplicateDonorException extends RuntimeException {
    public DuplicateDonorException(String phone) {
        super("A donor with phone '" + phone + "' already exists");
    }

    public static DuplicateDonorException forUser(Long userId) {
        return new DuplicateDonorException(userId);
    }

    private DuplicateDonorException(Long userId) {
        super("You already have a donor profile (userId=" + userId + "). Open Match alerts or Profile to continue.");
    }
}
