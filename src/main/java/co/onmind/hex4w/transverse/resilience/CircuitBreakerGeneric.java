package co.onmind.hex4w.transverse.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class CircuitBreakerGeneric {

    private CircuitBreakerGeneric() {}

    public static <T> Mono<T> withCircuitBreaker(Mono<T> mono, CircuitBreaker cb) {
        return mono.transformDeferred(CircuitBreakerOperator.of(cb));
    }

    public static <T> Flux<T> withCircuitBreaker(Flux<T> flux, CircuitBreaker cb) {
        return flux.transformDeferred(CircuitBreakerOperator.of(cb));
    }
}
