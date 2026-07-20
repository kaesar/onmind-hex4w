package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.dto.out.StoreItemResponseDto;
import co.onmind.hex4w.application.ports.in.ListStoreTrait;
import jakarta.validation.constraints.NotBlank;
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
public class StoreHandler {

    private final ListStoreTrait listStoreTrait;
    private final Validator validator;

    public StoreHandler(ListStoreTrait listStoreTrait, Validator validator) {
        this.listStoreTrait = listStoreTrait;
        this.validator = validator;
    }

    public Mono<ServerResponse> listItems(ServerRequest request) {
        return Mono.fromCallable(() -> {
                String bucket = request.queryParam("bucket")
                    .orElseThrow(() -> new IllegalArgumentException("Bucket query parameter is required"));
                if (bucket.trim().isEmpty()) {
                    throw new IllegalArgumentException("Bucket query parameter cannot be empty");
                }
                return bucket.trim();
            })
            .flatMap(bucket -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(listStoreTrait.listItems(bucket), StoreItemResponseDto.class))
            .onErrorResume(this::handleError);
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
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            ));
    }

    public record ErrorResponse(
        String code,
        String message,
        int status
    ) {}
}