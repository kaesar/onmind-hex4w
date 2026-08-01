package co.onmind.hex4w.application.ports.out;

import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import reactor.core.publisher.Mono;

public interface AbcPort {
    Mono<AbcResponse> sheet(String show, String from, String some);
    Mono<AbcResponse> exec(AbcRequest request);
}