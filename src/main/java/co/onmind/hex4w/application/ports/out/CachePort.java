package co.onmind.hex4w.application.ports.out;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Output port for a reactive key/value cache.
 * <p>
 * The application layer uses this abstraction so the cache implementation
 * (Redis, Caffeine, etc.) can be swapped without touching use cases.
 */
public interface CachePort {

    Mono<String> get(String key);

    Mono<Void> set(String key, String value, Duration ttl);

    Mono<Void> evict(String key);
}
