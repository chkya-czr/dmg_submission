package com.example.notify.channel;

public record SendResult(boolean success, boolean retryable, String errorMessage) {

    public static SendResult ok() {
        return new SendResult(true, false, null);
    }

    public static SendResult retryableFailure(String errorMessage) {
        return new SendResult(false, true, errorMessage);
    }

    public static SendResult permanentFailure(String errorMessage) {
        return new SendResult(false, false, errorMessage);
    }
}
