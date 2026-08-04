package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbcAdapterTest {

    @Mock
    AbcWebClient abcWebClient;

    private CircuitBreaker circuitBreaker;
    private AbcAdapter adapter;

    @BeforeEach
    void setUp() {
        circuitBreaker = CircuitBreaker.ofDefaults("abc");
        adapter = new AbcAdapter(abcWebClient, circuitBreaker);
    }

    @Test
    @DisplayName("sheet delegates to AbcWebClient.sheet and returns response")
    void sheetDelegates() {
        AbcResponse mockResponse = new AbcResponse(true, 200, "ok", 1, "{}");
        when(abcWebClient.sheet("show1", "xykit", "sheet1"))
            .thenReturn(Mono.just(mockResponse));

        StepVerifier.create(adapter.sheet("show1", "xykit", "sheet1"))
            .expectNext(mockResponse)
            .verifyComplete();

        verify(abcWebClient).sheet("show1", "xykit", "sheet1");
    }

    @Test
    @DisplayName("exec delegates to AbcWebClient.ask")
    void execDelegates() {
        AbcRequest request = AbcRequest.builder()
            .what("insert")
            .from("xykit")
            .some("sheet1")
            .puts(Map.of("x", 1))
            .build();
        AbcResponse mockResponse = new AbcResponse(true, 200, "ok", 1, "{\"id\":1}");
        when(abcWebClient.ask(request))
            .thenReturn(Mono.just(mockResponse));

        StepVerifier.create(adapter.exec(request))
            .expectNext(mockResponse)
            .verifyComplete();

        verify(abcWebClient).ask(request);
    }

    @Test
    @DisplayName("sheet propagates errors from delegate")
    void sheetPropagatesErrors() {
        when(abcWebClient.sheet(any(), any(), any()))
            .thenReturn(Mono.error(new RuntimeException("WebClient failed")));

        StepVerifier.create(adapter.sheet("show1", "xykit", "sheet1"))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().contains("WebClient failed"))
            .verify();
    }
}
