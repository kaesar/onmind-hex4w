package co.onmind.hex4w.application.ports.out;

import reactor.core.publisher.Mono;

public interface LambdaPort {
    Mono<String> invoke(String functionName, String payload);
}
