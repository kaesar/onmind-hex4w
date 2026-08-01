package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Adapter that implements {@link AbcPort} by delegating to {@link AbcWebClient}.
 * <p>
 * No {@code @Component} — the bean is declared in {@link co.onmind.hex4w.infrastructure.configuration.WebClientConfiguration}
 * to ensure the correct {@link AbcWebClient} (with base URL + auth token) is used.
 */
public class AbcAdapter implements AbcPort {

    private static final Logger logger = LoggerFactory.getLogger(AbcAdapter.class);

    private final AbcWebClient abcWebClient;

    public AbcAdapter(AbcWebClient abcWebClient) {
        this.abcWebClient = abcWebClient;
    }

    @Override
    public Mono<AbcResponse> sheet(String show, String from, String some) {
        logger.debug("AbcAdapter.sheet: show={}", show);
        return abcWebClient.sheet(show, from, some);
    }

    @Override
    public Mono<AbcResponse> exec(AbcRequest request) {
        logger.debug("AbcAdapter.exec: what={}, from={}", request.what(), request.from());
        return abcWebClient.ask(request);
    }
}