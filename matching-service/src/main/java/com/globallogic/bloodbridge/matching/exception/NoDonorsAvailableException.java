package com.globallogic.bloodbridge.matching.exception;

public class NoDonorsAvailableException extends RuntimeException {
    public NoDonorsAvailableException(String bloodGroup, String city) {
        super("No available donors found for blood group '" + bloodGroup + "' in '" + city + "'");
    }
}
