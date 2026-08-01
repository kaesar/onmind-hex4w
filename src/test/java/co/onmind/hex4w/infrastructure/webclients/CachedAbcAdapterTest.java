package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.application.ports.out.CachePort;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachedAbcAdapterTest {

    @Mock
    private AbcPort delegate;

    @Mock
    private CachePort cachePort;

    private ObjectMapper objectMapper;
    private CachedAbcAdapter cachedAbcAdapter;

    private static final AbcResponse SAMPLE_RESPONSE = new AbcResponse(
            true, 200, "OK", 1, List.of(Map.of("id", "1"))
    );

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cachedAbcAdapter = new CachedAbcAdapter(delegate, cachePort,
                objectMapper, Duration.ofSeconds(300));
    }

    @Test
    @DisplayName("Cache hit: returns cached response without calling delegate")
    void cacheHitReturnsCachedResponse() throws Exception {
        String json = objectMapper.writeValueAsString(SAMPLE_RESPONSE);

        when(cachePort.get(anyString())).thenReturn(Mono.just(json));

        StepVerifier.create(cachedAbcAdapter.sheet("show", "from", "some"))
                .expectNext(SAMPLE_RESPONSE)
                .verifyComplete();

        verify(delegate, never()).sheet(any(), any(), any());
        verify(cachePort, never()).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Cache miss: calls delegate, caches result, returns response")
    void cacheMissCallsDelegateAndCaches() throws Exception {
        String json = objectMapper.writeValueAsString(SAMPLE_RESPONSE);

        when(cachePort.get("abc:sheet:show:from:some")).thenReturn(Mono.empty());
        when(delegate.sheet("show", "from", "some")).thenReturn(Mono.just(SAMPLE_RESPONSE));
        when(cachePort.set(anyString(), eq(json), any())).thenReturn(Mono.empty());

        StepVerifier.create(cachedAbcAdapter.sheet("show", "from", "some"))
                .expectNext(SAMPLE_RESPONSE)
                .verifyComplete();

        verify(delegate).sheet("show", "from", "some");
        verify(cachePort).set(anyString(), eq(json), any());
    }

    @Test
    @DisplayName("Cache read error: falls back to delegate")
    void cacheReadErrorFallsBackToDelegate() {
        when(cachePort.get(anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));
        when(delegate.sheet("show", "from", "some")).thenReturn(Mono.just(SAMPLE_RESPONSE));
        when(cachePort.set(anyString(), anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(cachedAbcAdapter.sheet("show", "from", "some"))
                .expectNext(SAMPLE_RESPONSE)
                .verifyComplete();

        verify(delegate).sheet("show", "from", "some");
    }

    @Test
    @DisplayName("Cache write error: still returns delegate response")
    void cacheWriteErrorReturnsResponse() {
        when(cachePort.get(anyString())).thenReturn(Mono.empty());
        when(delegate.sheet("show", "from", "some")).thenReturn(Mono.just(SAMPLE_RESPONSE));
        when(cachePort.set(anyString(), anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Redis down")));

        StepVerifier.create(cachedAbcAdapter.sheet("show", "from", "some"))
                .expectNext(SAMPLE_RESPONSE)
                .verifyComplete();
    }

    @Test
    @DisplayName("Corrupted cache entry: deserialization error falls back to delegate")
    void corruptedCacheEntryFallsBackToDelegate() {
        when(cachePort.get(anyString())).thenReturn(Mono.just("{bad-json"));
        when(delegate.sheet("show", "from", "some")).thenReturn(Mono.just(SAMPLE_RESPONSE));
        when(cachePort.set(anyString(), anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(cachedAbcAdapter.sheet("show", "from", "some"))
                .expectNext(SAMPLE_RESPONSE)
                .verifyComplete();

        verify(delegate).sheet("show", "from", "some");
    }

    @Test
    @DisplayName("exec: never caches, always delegates")
    void execNeverCaches() {
        AbcRequest request = AbcRequest.builder().what("insert").from("test").build();

        when(delegate.exec(request)).thenReturn(Mono.just(SAMPLE_RESPONSE));

        StepVerifier.create(cachedAbcAdapter.exec(request))
                .expectNext(SAMPLE_RESPONSE)
                .verifyComplete();

        verify(delegate).exec(request);
        verify(cachePort, never()).get(anyString());
        verify(cachePort, never()).set(anyString(), anyString(), any());
    }
}
