package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.dto.in.SendEmailRequestDto;
import co.onmind.hex4w.application.ports.in.SendEmailTrait;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EmailHandler {

    private final SendEmailTrait sendEmailTrait;
    private final Validator validator;

    public EmailHandler(SendEmailTrait sendEmailTrait, Validator validator) {
        this.sendEmailTrait = sendEmailTrait;
        this.validator = validator;
    }

    public Mono<ServerResponse> sendEmail(ServerRequest request) {
        return request.bodyToMono(SendEmailRequestDto.class)
                .flatMap(this::validateRequest)
                .flatMap(sendEmailTrait::sendEmail)
                .then(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new EmailResponse("Email queued successfully")))
                .onErrorResume(this::handleError);
    }

    private Mono<SendEmailRequestDto> validateRequest(SendEmailRequestDto request) {
        Set<ConstraintViolation<SendEmailRequestDto>> violations = validator.validate(request);

        if (violations.isEmpty()) {
            return Mono.just(request);
        }

        String errorMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return Mono.error(new IllegalArgumentException("Validation failed: " + errorMessage));
    }

    private Mono<ServerResponse> handleError(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new EmailErrorResponse(
                            "INVALID_REQUEST",
                            throwable.getMessage(),
                            HttpStatus.BAD_REQUEST.value()));
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new EmailErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    public record EmailResponse(String message) {}

    public record EmailErrorResponse(
            String code,
            String message,
            int status
    ) {}
}
