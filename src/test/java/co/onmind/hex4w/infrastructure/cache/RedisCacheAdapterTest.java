package co.onmind.hex4w.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheAdapterTest {

    @Mock
    ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    ReactiveValueOperations<String, String> valueOps;

    private RedisCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.<String, String>opsForValue()).thenReturn(valueOps);
        adapter = new RedisCacheAdapter(redisTemplate);
    }

    @Test
    @DisplayName("get returns value when present")
    void getReturnsValue() {
        when(valueOps.get("my-key")).thenReturn(Mono.just("cached-value"));

        StepVerifier.create(adapter.get("my-key"))
            .expectNext("cached-value")
            .verifyComplete();
    }

    @Test
    @DisplayName("get returns empty when key absent")
    void getReturnsEmptyWhenAbsent() {
        when(valueOps.get("missing")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.get("missing"))
            .verifyComplete();
    }

    @Test
    @DisplayName("set writes value with TTL and completes")
    void setWritesWithTtl() {
        when(valueOps.set("key", "value", Duration.ofSeconds(300)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(adapter.set("key", "value", Duration.ofSeconds(300)))
            .verifyComplete();

        verify(valueOps).set(eq("key"), eq("value"), eq(Duration.ofSeconds(300)));
    }

    @Test
    @DisplayName("evict deletes key and completes")
    void evictDeletesKey() {
        when(valueOps.getAndDelete("key")).thenReturn(Mono.just("old-value"));

        StepVerifier.create(adapter.evict("key"))
            .verifyComplete();

        verify(valueOps).getAndDelete("key");
    }
}
