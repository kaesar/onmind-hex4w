package co.onmind.hex4w.infrastructure.configuration;

import co.onmind.hex4w.transverse.resilience.OutboundGuard;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience policies for the outbound adapters: circuit breaker, bulkhead and rate limiter.
 * <p>
 * The bulkhead and the rate limiter bound how much traffic reaches a downstream. Without
 * them the only limit is the Reactor Netty connection pool, which queues the excess
 * (up to 256 pending acquires, 45 seconds) instead of rejecting it — so a burst turns
 * into latency instead of into a fast failure.
 */
@Configuration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceConfiguration {

    @Bean
    public CircuitBreaker abcCircuitBreaker() {
        return CircuitBreaker.of("abc", circuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker s3CircuitBreaker() {
        return CircuitBreaker.of("s3", circuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker lambdaCircuitBreaker() {
        return CircuitBreaker.of("lambda", circuitBreakerConfig());
    }

    @Bean
    public OutboundGuard abcGuard(@Qualifier("abcCircuitBreaker") CircuitBreaker abcCircuitBreaker,
                                  ResilienceProperties properties) {
        return guard("abc", abcCircuitBreaker, properties.getAbc());
    }

    @Bean
    public OutboundGuard s3Guard(@Qualifier("s3CircuitBreaker") CircuitBreaker s3CircuitBreaker,
                                 ResilienceProperties properties) {
        return guard("s3", s3CircuitBreaker, properties.getS3());
    }

    @Bean
    public OutboundGuard lambdaGuard(@Qualifier("lambdaCircuitBreaker") CircuitBreaker lambdaCircuitBreaker,
                                     ResilienceProperties properties) {
        return guard("lambda", lambdaCircuitBreaker, properties.getLambda());
    }

    private OutboundGuard guard(String name, CircuitBreaker circuitBreaker,
                                ResilienceProperties.Downstream limits) {
        Bulkhead bulkhead = Bulkhead.of(name, BulkheadConfig.custom()
                .maxConcurrentCalls(Math.max(1, limits.getMaxConcurrentCalls()))
                .maxWaitDuration(Duration.ofMillis(Math.max(0, limits.getMaxWaitMs())))
                .build());

        RateLimiter rateLimiter = RateLimiter.of(name, RateLimiterConfig.custom()
                .limitForPeriod(Math.max(1, limits.getRateLimitPerPeriod()))
                .limitRefreshPeriod(Duration.ofMillis(Math.max(1, limits.getRateLimitRefreshMs())))
                .timeoutDuration(Duration.ofMillis(Math.max(0, limits.getRateLimitTimeoutMs())))
                .build());

        return new OutboundGuard(circuitBreaker, bulkhead, rateLimiter);
    }

    private CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                // A rejection by our own bulkhead or rate limiter is not a downstream
                // failure and must never trip the breaker.
                .ignoreExceptions(
                        io.github.resilience4j.bulkhead.BulkheadFullException.class,
                        io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
                .build();
    }
}
