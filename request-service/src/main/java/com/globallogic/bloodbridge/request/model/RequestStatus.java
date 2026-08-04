package com.globallogic.bloodbridge.request.model;

public enum RequestStatus {
    PENDING,
    MATCHED,
    CONFIRMED,
    BANK_RESERVED,
    OUT_FOR_DELIVERY,
    FULFILLED,
    NO_DONORS_FOUND,
    CANCELLED
}
