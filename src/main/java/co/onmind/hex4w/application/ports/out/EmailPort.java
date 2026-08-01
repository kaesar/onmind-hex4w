package co.onmind.hex4w.application.ports.out;

import reactor.core.publisher.Mono;

import java.util.List;

public interface EmailPort {
    Mono<Void> send(String to, String subject, String body);

    Mono<Void> send(String to, String subject, String body, String from, List<String> cc);
}
