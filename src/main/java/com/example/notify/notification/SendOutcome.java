package com.example.notify.notification;

/** {@code replayed=true} means an existing request with the same Idempotency-Key was returned
 * instead of creating a new one. */
public record SendOutcome(NotificationRequest request, boolean replayed) {
}
