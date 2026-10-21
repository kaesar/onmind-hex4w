package co.onmind.hex4w.transverse.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Composes the resilience policies applied to an outbound call: rate limiter, bulkhead
 * and circuit breaker.
 * <p>
 * The order matters and it is the reason the three policies are composed here instead of
 * being applied one by one at each call site:
 * </p>
 * <ol>
 *   <li><b>Rate limiter</b> (outermost) — rejects excess calls before they occupy any
 *       concurrency slot, and before they can reach the downstream at all.</li>
 *   <li><b>Bulkhead</b> — caps how many calls are <em>subscribed</em> at the same time.
 *       This is the policy that the Reactor Netty connection pool cannot provide: the
 *       pool queues the excess for up to 45 seconds instead of rejecting it.</li>
 *   <li><b>Circuit breaker</b> (innermost) — only ever observes real downstream
 *       failures. A bulkhead rejection or a rate-limit rejection is not a downstream
 *       failure and must never trip the breaker.</li>
 * </ol>
 * <p>
 * All three operators are applied with {@code transformDeferred}, so a call is only
 * counted when it is actually subscribed — not when the chain is assembled.
 *
 * @param circuitBreaker the circuit breaker protecting the downstream
 * @param bulkhead       the bulkhead limiting the concurrent calls
 * @param rateLimiter    the rate limiter limiting the call rate
 */
public record OutboundGuard(
        CircuitBreaker circuitBreaker,
        Bulkhead bulkhead,
        RateLimiter rateLimiter) {

    /**
     * Applies the composed policies to a {@link Mono}.
     * <p>
     * {@code transformDeferred} <em>wraps</em> the publisher, so the operator applied
     * last ends up being the outermost one. The chain is therefore written in reverse
     * subscription order: the circuit breaker is applied first (innermost) and the rate
     * limiter last (outermost), which yields the intended subscription order
     * {@code RateLimiter → Bulkhead → CircuitBreaker → call}.
     *
     * @param mono the outbound call
     * @param <T>  the result type
     * @return the guarded stream
     */
    public <T> Mono<T> transform(Mono<T> mono) {
        return mono.transformDeferred(CircuitBreakerOperator.<T>of(circuitBreaker))
                .transformDeferred(BulkheadOperator.<T>of(bulkhead))
                .transformDeferred(RateLimiterOperator.<T>of(rateLimiter));
    }

    /**
     * Applies the composed policies to a {@link Flux}.
     * <p>
     * See {@link #transform(Mono)} for the ordering rationale.
     *
     * @param flux the outbound call
     * @param <T>  the element type
     * @return the guarded stream
     */
    public <T> Flux<T> transform(Flux<T> flux) {
        return flux.transformDeferred(CircuitBreakerOperator.<T>of(circuitBreaker))
                .transformDeferred(BulkheadOperator.<T>of(bulkhead))
                .transformDeferred(RateLimiterOperator.<T>of(rateLimiter));
    }
}
