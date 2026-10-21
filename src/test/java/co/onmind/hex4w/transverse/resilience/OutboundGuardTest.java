package co.onmind.hex4w.transverse.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboundGuardTest {

    @Test
    @DisplayName("passes the value through when every policy allows it")
    void passesThrough() {
        OutboundGuard guard = TestGuards.permissive();

        StepVerifier.create(guard.transform(Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    @DisplayName("rejects with RequestNotPermitted when the rate limit is exceeded")
    void rejectsWhenRateLimitExceeded() {
        OutboundGuard guard = TestGuards.of(100, 2);

        StepVerifier.create(guard.transform(Mono.just("first"))).expectNext("first").verifyComplete();
        StepVerifier.create(guard.transform(Mono.just("second"))).expectNext("second").verifyComplete();

        StepVerifier.create(guard.transform(Mono.just("third")))
                .expectError(RequestNotPermitted.class)
                .verify();
    }

    @Test
    @DisplayName("rejects with BulkheadFullException when too many calls are in flight")
    void rejectsWhenBulkheadIsFull() {
        OutboundGuard guard = TestGuards.of(1, 1000);

        // The first call occupies the only slot and never completes.
        Mono<String> blocking = guard.transform(Mono.<String>never());
        StepVerifier.create(blocking)
                .then(() -> {
                    StepVerifier.create(guard.transform(Mono.just("second")))
                            .expectError(BulkheadFullException.class)
                            .verify();
                })
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("does not trip the circuit breaker when our own rate limiter rejects")
    void rejectionsDoNotTripTheCircuitBreaker() {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .ignoreExceptions(BulkheadFullException.class, RequestNotPermitted.class)
                .build());

        OutboundGuard guard = new OutboundGuard(
                circuitBreaker,
                Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).build()),
                RateLimiter.of("test", RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO)
                        .build()));

        StepVerifier.create(guard.transform(Mono.just("first"))).expectNext("first").verifyComplete();
        StepVerifier.create(guard.transform(Mono.just("second")))
                .expectError(RequestNotPermitted.class)
                .verify();

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState(),
                "a rate-limit rejection is not a downstream failure and must not open the breaker");
    }

    @Test
    @DisplayName("trips the circuit breaker on real downstream failures")
    void tripsOnDownstreamFailures() {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .build());

        OutboundGuard guard = new OutboundGuard(
                circuitBreaker,
                Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(10).build()),
                RateLimiter.of("test", RateLimiterConfig.custom().limitForPeriod(1000).build()));

        StepVerifier.create(guard.transform(Mono.error(new IllegalStateException("downstream exploded"))))
                .expectError(IllegalStateException.class)
                .verify();
        StepVerifier.create(guard.transform(Mono.error(new IllegalStateException("downstream exploded"))))
                .expectError(IllegalStateException.class)
                .verify();

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("guards a Flux as well")
    void guardsFlux() {
        OutboundGuard guard = TestGuards.permissive();

        StepVerifier.create(guard.transform(Flux.just("a", "b")))
                .expectNext("a", "b")
                .verifyComplete();
    }
}
