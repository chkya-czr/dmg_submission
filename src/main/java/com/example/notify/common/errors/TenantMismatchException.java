package com.example.notify.common.errors;

/**
 * Thrown when an authenticated tenant admin attempts to act on a tenant other than their own.
 * Platform admins are exempt from this check.
 */
public class TenantMismatchException extends RuntimeException {

    public TenantMismatchException(String message) {
        super(message);
    }
}
