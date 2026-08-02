package com.globallogic.bloodbridge.request.exception;

public class RequestNotFoundException extends RuntimeException {
    public RequestNotFoundException(Long requestId) {
        super("Blood request not found with id: " + requestId);
    }
}
