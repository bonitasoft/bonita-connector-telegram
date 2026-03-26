package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy() {
        @Override void sleep(long millis) { /* no-op for tests */ }
    };

    // Use real RetryPolicy to cover sleep() method
    private final RetryPolicy realPolicy = new RetryPolicy();

    @Test
    void should_succeed_on_first_attempt() {
        String result = policy.execute(() -> "ok");
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void should_retry_on_retryable_and_succeed() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = policy.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new TelegramException("Too Many Requests", 429, true, 35);
            }
            return "recovered";
        });
        assertThat(result).isEqualTo("recovered");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void should_retry_on_retryable_500_and_succeed() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = policy.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new TelegramException("Internal Server Error", 500, true);
            }
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void should_not_retry_on_non_retryable() {
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new TelegramException("Bad request", 400, false);
        })).isInstanceOf(TelegramException.class)
                .hasMessageContaining("Bad request")
                .satisfies(e -> assertThat(((TelegramException) e).isRetryable()).isFalse());
    }

    @Test
    void should_exhaust_retries_and_throw() {
        AtomicInteger attempts = new AtomicInteger(0);
        assertThatThrownBy(() -> policy.execute(() -> {
            attempts.incrementAndGet();
            throw new TelegramException("Always fails", 500, true);
        })).isInstanceOf(TelegramException.class);
        assertThat(attempts.get()).isEqualTo(4); // initial + 3 retries
    }

    @Test
    void should_use_retry_after_from_exception() {
        AtomicInteger waitMs = new AtomicInteger(0);
        RetryPolicy trackingPolicy = new RetryPolicy() {
            @Override void sleep(long millis) { waitMs.set((int) millis); }
        };
        AtomicInteger attempts = new AtomicInteger(0);
        trackingPolicy.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new TelegramException("Rate limit", 429, true, 60);
            }
            return "ok";
        });
        assertThat(waitMs.get()).isEqualTo(60_000);
    }

    @Test
    void should_use_fallback_when_no_retry_after() {
        AtomicInteger waitMs = new AtomicInteger(0);
        RetryPolicy trackingPolicy = new RetryPolicy() {
            @Override void sleep(long millis) { waitMs.set((int) millis); }
        };
        AtomicInteger attempts = new AtomicInteger(0);
        trackingPolicy.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new TelegramException("Server error", 500, true);
            }
            return "ok";
        });
        assertThat(waitMs.get()).isEqualTo(35_000); // first fallback
    }

    @Test
    void should_not_retry_403() {
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new TelegramException("Forbidden", 403, false);
        })).isInstanceOf(TelegramException.class).hasMessageContaining("Forbidden");
    }

    @Test
    void should_wrap_unexpected_exceptions() {
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(TelegramException.class).hasMessageContaining("Unexpected error");
    }

    @Test
    void should_execute_real_sleep() {
        // Cover the real sleep() method (not overridden) for pitest NO_COVERAGE mutant
        realPolicy.sleep(1); // 1ms sleep, just to cover the method
    }

    @Test
    void should_handle_interrupt_during_sleep() throws Exception {
        // Cover the InterruptedException branch in sleep()
        Thread testThread = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            testThread.interrupt();
        });
        interrupter.start();
        realPolicy.sleep(5000); // Will be interrupted after ~50ms
        // Verify that the interrupt flag was set back
        assertThat(Thread.interrupted()).isTrue(); // clears the flag
        interrupter.join();
    }

    @Test
    void should_retry_multiple_times_with_escalating_waits() {
        java.util.List<Long> waits = new java.util.ArrayList<>();
        RetryPolicy trackingPolicy = new RetryPolicy() {
            @Override void sleep(long millis) { waits.add(millis); }
        };
        AtomicInteger attempts = new AtomicInteger(0);
        trackingPolicy.execute(() -> {
            if (attempts.incrementAndGet() < 4) {
                throw new TelegramException("Server error", 500, true);
            }
            return "ok";
        });
        assertThat(waits).containsExactly(35_000L, 70_000L, 140_000L);
    }
}
