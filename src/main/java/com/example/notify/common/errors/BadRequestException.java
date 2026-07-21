package com.example.notify.common.errors;

/** Thrown for request-level validation failures that aren't simple bean-validation violations
 * (e.g. template variable mismatches, malformed schedules). Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
