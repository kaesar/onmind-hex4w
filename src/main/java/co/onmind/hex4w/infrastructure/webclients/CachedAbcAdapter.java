package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.application.ports.out.CachePort;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Decorator around {@link AbcPort} that caches {@code sheet} responses in Redis.
 * <p>
 * Only read operations ({@link #sheet}) are cached — write operations
 * ({@link #exec}) always delegate to the underlying port.
 * <p>
 * Cache key is derived from the sheet parameters.  On cache miss the response
 * is fetched from the delegate, serialized to JSON, and stored.  Cache errors
 * are swallowed so a Redis outage never breaks the XDB flow.
 */
public class CachedAbcAdapter implements AbcPort {

    private static final Logger logger = LoggerFactory.getLogger(CachedAbcAdapter.class);

    private final AbcPort delegate;
    private final CachePort cachePort;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public CachedAbcAdapter(AbcPort delegate, CachePort cachePort,
                            ObjectMapper objectMapper, Duration ttl) {
        this.delegate = delegate;
        this.cachePort = cachePort;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Mono<AbcResponse> sheet(String show, String from, String some) {
        String key = cacheKey(show, from, some);

        return cachePort.get(key)
                .flatMap(cached -> {
                    if (cached == null || cached.isBlank()) {
                        return Mono.<AbcResponse> empty();
                    }
                    try {
                        AbcResponse cachedResponse = objectMapper.readValue(cached, AbcResponse.class);
                        logger.debug("Cache HIT key={}", key);
                        return Mono.just(cachedResponse);
                    } catch (JsonProcessingException e) {
                        logger.warn("Cache value for key={} could not be deserialized, ignoring", key);
                        return Mono.<AbcResponse> empty();
                    }
                })
                .onErrorResume(e -> {
                    logger.debug("Cache read error for key={}, falling back to delegate", key, e);
                    return Mono.<AbcResponse> empty();
                })
                .switchIfEmpty(Mono.defer(() -> fetchAndCache(show, from, some, key)));
    }

    @Override
    public Mono<AbcResponse> exec(AbcRequest request) {
        return delegate.exec(request);
    }

    private Mono<AbcResponse> fetchAndCache(String show, String from, String some, String key) {
        return delegate.sheet(show, from, some)
                .flatMap(response -> {
                    String json = serialize(response, key);
                    if (json == null) {
                        return Mono.just(response);
                    }
                    return cachePort.set(key, json, ttl)
                            .onErrorResume(e -> {
                                logger.debug("Cache write error for key={}, returning uncached", key, e);
                                return Mono.empty();
                            })
                            .then(Mono.just(response));
                });
    }

    private String serialize(AbcResponse response, String key) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            logger.warn("Unable to serialize AbcResponse for cache at key={}", key, e);
            return null;
        }
    }

    private String cacheKey(String show, String from, String some) {
        return "abc:sheet:%s:%s:%s".formatted(
                show != null ? show : "",
                from != null ? from : "",
                some != null ? some : ""
        );
    }
}
