package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.dto.in.ExecuteScriptRequestDto;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
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
public class ScriptingHandler {

    private final ExecuteScriptTrait executeScriptTrait;
    private final Validator validator;

    public ScriptingHandler(ExecuteScriptTrait executeScriptTrait, Validator validator) {
        this.executeScriptTrait = executeScriptTrait;
        this.validator = validator;
    }

    public Mono<ServerResponse> executeScript(ServerRequest request) {
        return request.bodyToMono(ExecuteScriptRequestDto.class)
            .flatMap(this::validateRequest)
            .flatMap(dto -> executeScriptTrait.executeScript(dto.script()))
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(result))
            .onErrorResume(this::handleError);
    }

    private Mono<ExecuteScriptRequestDto> validateRequest(ExecuteScriptRequestDto request) {
        Set<ConstraintViolation<ExecuteScriptRequestDto>> violations = validator.validate(request);

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
                .bodyValue(new ErrorResponse(
                    "INVALID_REQUEST",
                    throwable.getMessage(),
                    HttpStatus.BAD_REQUEST.value()
                ));
        }

        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new ErrorResponse(
                "INTERNAL_ERROR",
                throwable.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            ));
    }

    public record ErrorResponse(
        String code,
        String message,
        int status
    ) {}
}