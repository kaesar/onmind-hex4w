package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.in.SendEmailRequestDto;
import reactor.core.publisher.Mono;

public interface SendEmailTrait {
    Mono<Void> sendEmail(SendEmailRequestDto request);
}
