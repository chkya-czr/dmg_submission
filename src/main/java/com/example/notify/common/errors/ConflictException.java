package com.example.notify.common.errors;

/** Thrown for state conflicts, e.g. a duplicate unique key that isn't a plain validation error. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
