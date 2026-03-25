package com.bonitasoft.connectors.telegram;

import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryPolicy {
    private static final int MAX_RETRIES = 3;
    private static final long[] FALLBACK_WAITS = {35_000, 70_000, 140_000};

    public <T> T execute(Callable<T> action) throws TelegramException {
        TelegramException lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.call();
            } catch (TelegramException e) {
                lastException = e;
                if (!e.isRetryable() || attempt == MAX_RETRIES) throw e;
                long waitMs = e.getRetryAfterSeconds() != null
                        ? e.getRetryAfterSeconds() * 1000L
                        : FALLBACK_WAITS[Math.min(attempt, FALLBACK_WAITS.length - 1)];
                log.warn("Retryable Telegram error (attempt {}/{}), waiting {}ms: {}", attempt + 1, MAX_RETRIES, waitMs, e.getMessage());
                sleep(waitMs);
            } catch (Exception e) {
                throw new TelegramException("Unexpected error: " + e.getMessage(), e);
            }
        }
        throw lastException;
    }

    void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
