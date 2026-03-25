package com.bonitasoft.connectors.telegram;

public class TelegramException extends RuntimeException {
    private final int statusCode;
    private final boolean retryable;
    private final Integer retryAfterSeconds;

    public TelegramException(String message, int statusCode, boolean retryable) {
        this(message, statusCode, retryable, null);
    }

    public TelegramException(String message, int statusCode, boolean retryable, Integer retryAfterSeconds) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public TelegramException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.retryable = false;
        this.retryAfterSeconds = null;
    }

    public int getStatusCode() { return statusCode; }
    public boolean isRetryable() { return retryable; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
}
