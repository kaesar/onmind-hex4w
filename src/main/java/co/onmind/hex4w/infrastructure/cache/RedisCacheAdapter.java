package co.onmind.hex4w.infrastructure.cache;

import co.onmind.hex4w.application.ports.out.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis implementation of {@link CachePort}.
 * <p>
 * Stores values as plain strings (typically JSON) via a
 * {@link ReactiveRedisTemplate}.  All operations are non-blocking;
 * cache errors propagate to the caller, which can decide to degrade
 * gracefully (e.g. fall through to the underlying service).
 */
@Component
public class RedisCacheAdapter implements CachePort {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheAdapter.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisCacheAdapter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<String> get(String key) {
        return redisTemplate.opsForValue()
                .get(key)
                .doOnSuccess(v -> logger.debug("Redis GET key={} hit={}", key, v != null));
    }

    @Override
    public Mono<Void> set(String key, String value, Duration ttl) {
        return redisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnSuccess(b -> logger.debug("Redis SET key={} ttl={}s ok={}", key, ttl.getSeconds(), b))
                .then();
    }

    @Override
    public Mono<Void> evict(String key) {
        return redisTemplate.opsForValue()
                .getAndDelete(key)
                .doOnNext(v -> logger.debug("Redis EVICT key={} cached={}", key, v != null))
                .then();
    }
}
