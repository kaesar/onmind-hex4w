package co.onmind.hex4w.transverse.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;

/**
 * Builders of {@link OutboundGuard} instances for the tests.
 */
public final class TestGuards {

    private TestGuards() {
    }

    /**
     * A guard that practically never rejects: circuit breaker closed, generous bulkhead
     * and rate limiter.
     *
     * @return a permissive guard
     */
    public static OutboundGuard permissive() {
        return of(1000, 100_000);
    }

    /**
     * A guard with the given limits, rejecting immediately instead of waiting.
     *
     * @param maxConcurrentCalls maximum calls in flight
     * @param limitForPeriod      maximum calls per refresh period
     * @return a guard with the given limits
     */
    public static OutboundGuard of(int maxConcurrentCalls, int limitForPeriod) {
        return new OutboundGuard(
                CircuitBreaker.ofDefaults("test"),
                Bulkhead.of("test", BulkheadConfig.custom()
                        .maxConcurrentCalls(maxConcurrentCalls)
                        .maxWaitDuration(Duration.ZERO)
                        .build()),
                RateLimiter.of("test", RateLimiterConfig.custom()
                        .limitForPeriod(limitForPeriod)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO)
                        .build()));
    }
}
