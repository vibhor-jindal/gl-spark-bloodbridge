package com.globallogic.bloodbridge.matching.exception;

public class MatchNotFoundException extends RuntimeException {
    public MatchNotFoundException(Long requestId, Long donorId) {
        super("No match found for requestId=" + requestId + " and donorId=" + donorId);
    }
}
